-- AI Investor — initial schema (PostgreSQL, Flyway V1).
-- Conventions: all timestamps TIMESTAMPTZ in UTC; money/prices/quantities NUMERIC(38, 18) (BigDecimal), never float;
-- every money value has a currency (VARCHAR(12): ISO codes and crypto codes such as USDT).
-- Broker neutrality: values a broker supplies (broker, asset class, IDs) are open-ended, no CHECK.
-- Our own vocabulary that core code branches on (order status, side, …) is TEXT + CHECK; adapters map to it.
-- A new broker must never need a migration; broker-specific extras go in the JSONB columns.
-- No credentials are ever stored here — they live in .env (see docs/configuration.md).

-- ============================================================================
-- Accounts & app state
-- ============================================================================

-- Broker accounts. Defined in application.yml + .env, upserted at startup so other tables can reference them.
-- An alias keeps its broker and trading_mode forever: history hangs off it. A different one needs a new alias.
CREATE TABLE broker_accounts (
    id              TEXT PRIMARY KEY,                       -- alias, e.g. 'ibkr-paper', 'etoro-demo'
    broker          TEXT NOT NULL,                          -- 'IBKR', 'ETORO', … (open-ended)
    display_name    TEXT NOT NULL,
    trading_mode    TEXT NOT NULL CHECK (trading_mode IN ('PAPER', 'LIVE')),
    base_currency   VARCHAR(12) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,         -- true only when all its .env values are filled in
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Single-row app state: active account, kill switch, analysis settings.
CREATE TABLE app_settings (
    id                          SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    active_account_id           TEXT REFERENCES broker_accounts (id),     -- NULL = nothing is traded
    last_seen_env_active_account TEXT,                                    -- ACTIVE_ACCOUNT from .env at last start
    trading_enabled             BOOLEAN NOT NULL DEFAULT FALSE,           -- kill switch; FALSE = stopped
    analysis_interval_minutes   INT NOT NULL DEFAULT 15 CHECK (analysis_interval_minutes > 0),
    ai_router_strategy          TEXT NOT NULL DEFAULT 'PRIORITY'
                                CHECK (ai_router_strategy IN ('PRIORITY', 'CHEAPEST_FIRST', 'ROUND_ROBIN')),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO app_settings (id) VALUES (1);

-- History of active-account switches.
CREATE TABLE active_account_changes (
    id              BIGSERIAL PRIMARY KEY,
    from_account_id TEXT REFERENCES broker_accounts (id),
    to_account_id   TEXT REFERENCES broker_accounts (id),
    source          TEXT NOT NULL CHECK (source IN ('UI', 'ENV', 'STARTUP')),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Risk limits per account (each broker keeps its own limits when switching).
CREATE TABLE risk_limits (
    account_id          TEXT PRIMARY KEY REFERENCES broker_accounts (id),
    max_position_value  NUMERIC(38, 18) NOT NULL CHECK (max_position_value >= 0),
    max_order_value     NUMERIC(38, 18) NOT NULL CHECK (max_order_value >= 0),
    max_daily_loss      NUMERIC(38, 18) NOT NULL CHECK (max_daily_loss >= 0),
    max_trades_per_day  INT NOT NULL CHECK (max_trades_per_day >= 0),
    allowed_symbols     TEXT[] NOT NULL DEFAULT '{}',
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================================
-- AI models: list, priority, availability (quota) and every call
-- ============================================================================

-- AI providers/models. Defined in application.yml + .env (API key), upserted at startup;
-- priority/enabled/budgets can be changed at runtime and are kept here.
CREATE TABLE ai_providers (
    id                  TEXT PRIMARY KEY,                   -- alias, e.g. 'claude-main', 'gemini-free'
    provider_type       TEXT NOT NULL,                      -- 'OPENAI', 'GEMINI', 'GROK', 'CLAUDE', 'OLLAMA', … (open-ended)
    model               TEXT NOT NULL,                      -- e.g. model name/version used for calls
    tier                TEXT NOT NULL CHECK (tier IN ('FREE', 'PAID')),
    priority            INT NOT NULL DEFAULT 100,           -- 1 = most preferred; order TBD, change any time
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,      -- user switch; also FALSE when the .env key is missing
    -- Availability: unavailable while unavailable_until is in the future (e.g. tokens used up until reset).
    unavailable_reason  TEXT CHECK (unavailable_reason IN
                            ('QUOTA_EXHAUSTED', 'RATE_LIMITED', 'BUDGET_REACHED', 'AUTH_ERROR', 'SERVER_ERROR', 'TIMEOUT', 'CONNECTION_ERROR')),
    unavailable_until   TIMESTAMPTZ,                        -- date+time it becomes available again (UTC)
    unavailable_since   TIMESTAMPTZ,
    -- Own budgets (NULL = no limit), checked before calling so we switch before the provider errors.
    max_tokens_per_day      BIGINT,
    max_tokens_per_month    BIGINT,
    max_requests_per_day    INT,
    max_cost_per_month      NUMERIC(38, 18),
    cost_currency           VARCHAR(12) NOT NULL DEFAULT 'USD',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ai_providers_priority_idx ON ai_providers (priority, id);

-- Every "tokens used / quota / rate limit" error, with the reset time the provider gave (or our estimate).
CREATE TABLE ai_quota_events (
    id              BIGSERIAL PRIMARY KEY,
    provider_id     TEXT NOT NULL REFERENCES ai_providers (id),
    model           TEXT NOT NULL,
    reason          TEXT NOT NULL CHECK (reason IN ('QUOTA_EXHAUSTED', 'RATE_LIMITED', 'BUDGET_REACHED')),
    detected_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    reset_at        TIMESTAMPTZ NOT NULL,                   -- when the provider is usable again
    reset_source    TEXT NOT NULL CHECK (reset_source IN ('PROVIDER', 'ESTIMATED')),
    error_message   TEXT                                    -- provider error text, secrets stripped
);
CREATE INDEX ai_quota_events_provider_idx ON ai_quota_events (provider_id, detected_at DESC);

-- One scheduled (or manual) analysis cycle for the active account.
CREATE TABLE analysis_runs (
    id              BIGSERIAL PRIMARY KEY,
    account_id      TEXT NOT NULL REFERENCES broker_accounts (id),   -- fixed at start of the run
    trigger         TEXT NOT NULL CHECK (trigger IN ('SCHEDULED', 'MANUAL')),
    status          TEXT NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'NO_AI_AVAILABLE', 'FAILED', 'SKIPPED_MARKET_CLOSED', 'SKIPPED_KILL_SWITCH')),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at     TIMESTAMPTZ,
    error_message   TEXT
);
CREATE INDEX analysis_runs_account_idx ON analysis_runs (account_id, started_at DESC);

-- Every request to an AI provider (successful or not): usage, cost, raw prompt/response.
CREATE TABLE ai_calls (
    id                  BIGSERIAL PRIMARY KEY,
    analysis_run_id     BIGINT REFERENCES analysis_runs (id),
    provider_id         TEXT NOT NULL REFERENCES ai_providers (id),
    model               TEXT NOT NULL,
    attempt             INT NOT NULL DEFAULT 1,             -- 1 = first provider tried in this run, 2 = first fallback, …
    started_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at         TIMESTAMPTZ,
    latency_ms          INT,
    outcome             TEXT NOT NULL CHECK (outcome IN
                            ('SUCCESS', 'INVALID_RESPONSE', 'QUOTA_EXHAUSTED', 'RATE_LIMITED', 'BUDGET_REACHED',
                             'AUTH_ERROR', 'SERVER_ERROR', 'TIMEOUT', 'CONNECTION_ERROR')),
    input_tokens        INT,
    output_tokens       INT,
    cost                NUMERIC(38, 18),
    cost_currency       VARCHAR(12),
    prompt              TEXT,                               -- what we sent (no secrets)
    raw_response        TEXT,                               -- what the AI answered, unparsed
    error_message       TEXT
);
CREATE INDEX ai_calls_provider_idx ON ai_calls (provider_id, started_at DESC);
CREATE INDEX ai_calls_run_idx ON ai_calls (analysis_run_id);

-- ============================================================================
-- Instruments & market data
-- ============================================================================

-- Our symbol -> broker's own instrument ID (IBKR conId, eToro instrumentId, …) and its trading rules.
-- Orders, trades and positions point here, so the same symbol on two exchanges/currencies stays separate.
CREATE TABLE instrument_mappings (
    id                      BIGSERIAL PRIMARY KEY,
    broker                  TEXT NOT NULL,
    symbol                  TEXT NOT NULL,
    exchange                TEXT NOT NULL DEFAULT '',
    currency                VARCHAR(12) NOT NULL,
    asset_class             TEXT NOT NULL,                  -- 'STOCK', 'ETF', 'CRYPTO', … (open-ended; 'OTHER' = unclassified)
    broker_instrument_id    TEXT NOT NULL,
    multiplier              NUMERIC(38, 18) NOT NULL DEFAULT 1 CHECK (multiplier > 0),  -- value of 1 unit per 1 price point
    min_quantity            NUMERIC(38, 18),                -- NULL = not reported by the broker
    quantity_step           NUMERIC(38, 18),
    price_tick              NUMERIC(38, 18),
    details                 JSONB,                          -- whatever else the adapter needs (no secrets)
    resolved_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (broker, symbol, exchange, currency)
);

-- Price bars used for AI analysis.
CREATE TABLE market_data (
    symbol      TEXT NOT NULL,
    exchange    TEXT NOT NULL DEFAULT '',
    bar_size    TEXT NOT NULL,                               -- e.g. '1d', '1h', '5m'
    bar_time    TIMESTAMPTZ NOT NULL,                        -- bar start, UTC
    open        NUMERIC(38, 18) NOT NULL,
    high        NUMERIC(38, 18) NOT NULL,
    low         NUMERIC(38, 18) NOT NULL,
    close       NUMERIC(38, 18) NOT NULL,
    volume      NUMERIC(38, 18),
    currency    VARCHAR(12) NOT NULL,
    source      TEXT NOT NULL,                               -- broker or data provider name
    PRIMARY KEY (symbol, exchange, bar_size, bar_time, source)
);

-- ============================================================================
-- Trading: AI decisions (signals), bids (orders) and their history, fills, trades
-- ============================================================================

-- What the AI decided and why (BUY/SELL/HOLD), plus the risk-check outcome.
CREATE TABLE signals (
    id                  BIGSERIAL PRIMARY KEY,
    analysis_run_id     BIGINT REFERENCES analysis_runs (id),
    ai_call_id          BIGINT REFERENCES ai_calls (id),     -- the accepted AI response
    account_id          TEXT NOT NULL REFERENCES broker_accounts (id),
    ai_provider_id      TEXT REFERENCES ai_providers (id),
    ai_model            TEXT,
    fallback_from       JSONB,                               -- providers skipped and why: [{provider, reason}]
    symbol              TEXT NOT NULL,
    action              TEXT NOT NULL CHECK (action IN ('BUY', 'SELL', 'HOLD')),
    size_type           TEXT CHECK (size_type IN ('UNITS', 'AMOUNT')),
    size_value          NUMERIC(38, 18) CHECK (size_value > 0),
    confidence          NUMERIC(5, 4) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    reasoning           TEXT NOT NULL,                       -- why the AI wants to buy/sell/hold
    status              TEXT NOT NULL CHECK (status IN ('PENDING', 'EXECUTED', 'REJECTED', 'HOLD')),
    rejection_reason    TEXT,                                -- risk check / validation reason when REJECTED
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((size_type IS NULL) = (size_value IS NULL))
);
CREATE INDEX signals_account_idx ON signals (account_id, created_at DESC);

-- Bids/orders sent (or attempted) to the broker, from an AI signal or placed manually in our UI.
-- EXTERNAL = found at the broker but not sent by us (broker's own app, stop-out, liquidation), so it has no client_order_id.
CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    account_id          TEXT NOT NULL REFERENCES broker_accounts (id),
    signal_id           BIGINT REFERENCES signals (id),      -- NULL for manual and external orders
    source              TEXT NOT NULL CHECK (source IN ('AI', 'MANUAL', 'EXTERNAL')),
    reasoning           TEXT,                                -- copied from the signal, or the user's note for manual orders
    client_order_id     TEXT UNIQUE,                         -- our idempotency key, stored before sending
    broker_order_id     TEXT,                                -- assigned by the broker once acknowledged
    instrument_mapping_id BIGINT NOT NULL REFERENCES instrument_mappings (id),  -- exactly what was traded
    symbol              TEXT NOT NULL,
    asset_class         TEXT NOT NULL,
    side                TEXT NOT NULL CHECK (side IN ('BUY', 'SELL')),
    -- For brokers that keep separate positions (BrokerCapabilities): open a new one or close broker_position_id.
    -- NULL = the broker nets BUY/SELL into a single position by itself.
    position_effect     TEXT CHECK (position_effect IN ('OPEN', 'CLOSE')),
    broker_position_id  TEXT,
    order_type          TEXT NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT', 'OTHER')),  -- OTHER = external only (stop, …)
    size_type           TEXT NOT NULL CHECK (size_type IN ('UNITS', 'AMOUNT')),
    size_value          NUMERIC(38, 18) NOT NULL CHECK (size_value > 0),
    limit_price         NUMERIC(38, 18),
    leverage            NUMERIC(10, 4),                      -- NULL = no leverage
    currency            VARCHAR(12) NOT NULL,
    status              TEXT NOT NULL CHECK (status IN
                            ('CREATED', 'SUBMITTED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'UNKNOWN')),
    filled_quantity     NUMERIC(38, 18) NOT NULL DEFAULT 0,
    avg_fill_price      NUMERIC(38, 18),
    commission          NUMERIC(38, 18) NOT NULL DEFAULT 0,  -- sum of fills, converted to the order currency
    -- Lifecycle timestamps (full history in order_events)
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),  -- bid created
    submitted_at        TIMESTAMPTZ,                         -- sent to broker
    filled_at           TIMESTAMPTZ,                         -- fully filled
    cancelled_at        TIMESTAMPTZ,
    rejected_at         TIMESTAMPTZ,
    close_reason        TEXT,                                -- why cancelled/rejected (by us, the user or the broker)
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (order_type <> 'LIMIT' OR limit_price IS NOT NULL),
    CHECK (order_type <> 'OTHER' OR source = 'EXTERNAL'),
    CHECK (CASE WHEN source = 'EXTERNAL' THEN broker_order_id IS NOT NULL ELSE client_order_id IS NOT NULL END),
    UNIQUE (account_id, broker_order_id)
);
CREATE INDEX orders_account_idx ON orders (account_id, created_at DESC);
CREATE INDEX orders_open_idx ON orders (account_id, status) WHERE status IN ('CREATED', 'SUBMITTED', 'PARTIALLY_FILLED', 'UNKNOWN');

-- Every status change of an order, with time and reason.
CREATE TABLE order_events (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders (id),
    status      TEXT NOT NULL CHECK (status IN
                    ('CREATED', 'SUBMITTED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'UNKNOWN')),
    event_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    message     TEXT,                                        -- reason / broker message
    raw         JSONB                                        -- broker payload (no secrets)
);
CREATE INDEX order_events_order_idx ON order_events (order_id, event_at);

-- Executions reported by the broker (an order can have several partial fills).
CREATE TABLE fills (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT NOT NULL REFERENCES orders (id),
    broker_fill_id      TEXT NOT NULL,                       -- broker's ID, or a stable one the adapter derives if it has none
    quantity            NUMERIC(38, 18) NOT NULL CHECK (quantity > 0),
    price               NUMERIC(38, 18) NOT NULL CHECK (price > 0),
    commission          NUMERIC(38, 18) NOT NULL DEFAULT 0,  -- negative = rebate
    commission_currency VARCHAR(12) NOT NULL,                -- can differ from the fill currency
    currency            VARCHAR(12) NOT NULL,
    filled_at           TIMESTAMPTZ NOT NULL,
    source              TEXT NOT NULL DEFAULT 'LIVE' CHECK (source IN ('LIVE', 'BACKFILL')),
    raw                 JSONB,                               -- broker payload (no secrets)
    UNIQUE (order_id, broker_fill_id)
);

-- Completed (or still open) round-trip trades: entry -> exit, with prices, profit and both reasonings.
CREATE TABLE trades (
    id                  BIGSERIAL PRIMARY KEY,
    account_id          TEXT NOT NULL REFERENCES broker_accounts (id),
    instrument_mapping_id BIGINT NOT NULL REFERENCES instrument_mappings (id),
    symbol              TEXT NOT NULL,
    asset_class         TEXT NOT NULL,
    side                TEXT NOT NULL CHECK (side IN ('LONG', 'SHORT')),   -- LONG: buy then sell; SHORT: sell then buy
    broker_position_id  TEXT,                                              -- NULL on netting brokers
    status              TEXT NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    quantity            NUMERIC(38, 18) NOT NULL CHECK (quantity > 0),     -- units opened
    entry_price         NUMERIC(38, 18) NOT NULL,                          -- avg start price
    exit_price          NUMERIC(38, 18),                                   -- avg end price (NULL while open)
    entry_signal_id     BIGINT REFERENCES signals (id),                    -- why we bought (LONG) / sold (SHORT)
    exit_signal_id      BIGINT REFERENCES signals (id),                    -- why we sold (LONG) / bought back (SHORT)
    entry_note          TEXT,                                              -- reason for manual entries
    exit_note           TEXT,                                              -- reason for manual exits
    fees                NUMERIC(38, 18) NOT NULL DEFAULT 0,                -- converted to the trade currency
    realized_pnl        NUMERIC(38, 18),                                   -- profit/loss after fees (NULL while open)
    realized_pnl_pct    NUMERIC(12, 6),                                    -- relative to entry value
    currency            VARCHAR(12) NOT NULL,
    opened_at           TIMESTAMPTZ NOT NULL,
    closed_at           TIMESTAMPTZ,
    CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL AND exit_price IS NOT NULL AND realized_pnl IS NOT NULL))
);
CREATE INDEX trades_account_idx ON trades (account_id, opened_at DESC);
CREATE UNIQUE INDEX trades_one_open_idx ON trades (account_id, instrument_mapping_id, side, COALESCE(broker_position_id, ''))
    WHERE status = 'OPEN';

-- Which fills opened/closed which trade (supports partial entries and exits).
CREATE TABLE trade_fills (
    trade_id    BIGINT NOT NULL REFERENCES trades (id),
    fill_id     BIGINT NOT NULL REFERENCES fills (id),
    role        TEXT NOT NULL CHECK (role IN ('ENTRY', 'EXIT')),
    quantity    NUMERIC(38, 18) NOT NULL CHECK (quantity > 0),   -- part of the fill used for this trade
    PRIMARY KEY (trade_id, fill_id, role)
);

-- ============================================================================
-- Balance & portfolio history
-- ============================================================================

-- Periodic snapshot of the account's money (taken on a schedule and after every fill).
-- total_value = cash + invested_value (+ unrealized P&L already in market value);
-- free_cash   = cash - reserved_for_orders.
-- Broker history often only gives total value, so BACKFILL rows may leave the breakdown NULL; never store a made-up 0.
CREATE TABLE account_snapshots (
    id                      BIGSERIAL PRIMARY KEY,
    account_id              TEXT NOT NULL REFERENCES broker_accounts (id),
    taken_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    reason                  TEXT NOT NULL CHECK (reason IN
                                ('SCHEDULED', 'AFTER_FILL', 'ANALYSIS_RUN', 'END_OF_DAY', 'MANUAL')),  -- see docs/brokers.md
    source                  TEXT NOT NULL DEFAULT 'LIVE' CHECK (source IN ('LIVE', 'BACKFILL')),  -- BACKFILL = from broker history (e.g. IBKR Flex)
    currency                VARCHAR(12) NOT NULL,
    total_value             NUMERIC(38, 18) NOT NULL,        -- all money in the broker (equity / net liquidation)
    invested_value          NUMERIC(38, 18),                 -- market value of open positions
    reserved_for_orders     NUMERIC(38, 18),                 -- money tied up in open bids/orders
    free_cash               NUMERIC(38, 18),                 -- money available for new orders
    cash                    NUMERIC(38, 18),                 -- total cash (free + reserved)
    unrealized_pnl          NUMERIC(38, 18),
    realized_pnl_today      NUMERIC(38, 18),                 -- NULL = not reported by the broker
    daily_pnl               NUMERIC(38, 18),                 -- NULL = not reported by the broker
    CHECK (source = 'BACKFILL' OR (invested_value IS NOT NULL AND reserved_for_orders IS NOT NULL
                                   AND free_cash IS NOT NULL AND cash IS NOT NULL AND unrealized_pnl IS NOT NULL))
);
CREATE INDEX account_snapshots_account_idx ON account_snapshots (account_id, taken_at DESC);
CREATE UNIQUE INDEX account_snapshots_one_eod_idx ON account_snapshots (account_id, ((taken_at AT TIME ZONE 'UTC')::date))
    WHERE reason = 'END_OF_DAY';

-- Positions at the time of each account snapshot.
CREATE TABLE position_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    account_snapshot_id BIGINT NOT NULL REFERENCES account_snapshots (id) ON DELETE CASCADE,
    instrument_mapping_id BIGINT NOT NULL REFERENCES instrument_mappings (id),
    symbol              TEXT NOT NULL,
    asset_class         TEXT NOT NULL,
    side                TEXT NOT NULL CHECK (side IN ('LONG', 'SHORT')),
    broker_position_id  TEXT,                                -- NULL on netting brokers
    quantity            NUMERIC(38, 18) NOT NULL CHECK (quantity > 0),
    avg_price           NUMERIC(38, 18) NOT NULL,
    market_price        NUMERIC(38, 18) NOT NULL,
    market_value        NUMERIC(38, 18) NOT NULL,
    unrealized_pnl      NUMERIC(38, 18) NOT NULL,
    leverage            NUMERIC(10, 4),
    currency            VARCHAR(12) NOT NULL
);
CREATE INDEX position_snapshots_snapshot_idx ON position_snapshots (account_snapshot_id);

-- Money moving in or out of the account other than through fills. Needed so a deposit doesn't look like profit
-- in the balance history, and so dividends, interest and financing fees are recorded.
CREATE TABLE cash_transactions (
    id                      BIGSERIAL PRIMARY KEY,
    account_id              TEXT NOT NULL REFERENCES broker_accounts (id),
    broker_transaction_id   TEXT NOT NULL,                   -- broker's ID, or a stable one the adapter derives if it has none
    type                    TEXT NOT NULL CHECK (type IN
                                ('DEPOSIT', 'WITHDRAWAL', 'DIVIDEND', 'INTEREST', 'FEE', 'FINANCING', 'TAX', 'FX', 'ADJUSTMENT', 'OTHER')),
    amount                  NUMERIC(38, 18) NOT NULL,        -- positive = money in, negative = money out
    currency                VARCHAR(12) NOT NULL,
    symbol                  TEXT,                            -- e.g. the stock that paid a dividend
    description             TEXT,
    occurred_at             TIMESTAMPTZ NOT NULL,
    source                  TEXT NOT NULL DEFAULT 'LIVE' CHECK (source IN ('LIVE', 'BACKFILL')),
    raw                     JSONB,                           -- broker payload (no secrets)
    UNIQUE (account_id, broker_transaction_id)
);
CREATE INDEX cash_transactions_account_idx ON cash_transactions (account_id, occurred_at DESC);

-- ============================================================================
-- Audit log
-- ============================================================================

-- Every decision and error that isn't already a row above (kill switch, reconnects, startup checks, …).
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    event_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    level       TEXT NOT NULL CHECK (level IN ('INFO', 'WARN', 'ERROR')),
    event_type  TEXT NOT NULL,                               -- e.g. 'KILL_SWITCH_STOP', 'BROKER_DISCONNECTED'
    account_id  TEXT REFERENCES broker_accounts (id),
    message     TEXT NOT NULL,
    details     JSONB                                        -- no secrets
);
CREATE INDEX audit_log_time_idx ON audit_log (event_at DESC);

-- ============================================================================
-- Views for the UI / reporting
-- ============================================================================

-- AI models in priority order: is it available now, and if not, when again.
CREATE VIEW v_ai_provider_status AS
SELECT p.id,
       p.provider_type,
       p.model,
       p.tier,
       p.priority,
       p.enabled,
       p.enabled AND (p.unavailable_until IS NULL OR p.unavailable_until <= now()) AS available,
       CASE WHEN p.unavailable_until > now() THEN p.unavailable_reason END        AS unavailable_reason,
       CASE WHEN p.unavailable_until > now() THEN p.unavailable_until END         AS available_again_at
FROM ai_providers p
ORDER BY p.priority, p.id;

-- Trades with start/end price, profit and why we bought and sold.
CREATE VIEW v_trade_history AS
SELECT t.id,
       t.account_id,
       a.broker,
       t.symbol,
       t.asset_class,
       t.side,
       t.status,
       t.quantity,
       t.entry_price,
       t.exit_price,
       t.fees,
       t.realized_pnl,
       t.realized_pnl_pct,
       t.realized_pnl > 0                          AS successful,
       t.currency,
       t.opened_at,
       t.closed_at,
       COALESCE(es.reasoning, t.entry_note)        AS entry_reasoning,
       COALESCE(xs.reasoning, t.exit_note)         AS exit_reasoning,
       es.ai_provider_id                           AS entry_ai_provider,
       xs.ai_provider_id                           AS exit_ai_provider
FROM trades t
JOIN broker_accounts a ON a.id = t.account_id
LEFT JOIN signals es ON es.id = t.entry_signal_id
LEFT JOIN signals xs ON xs.id = t.exit_signal_id;
