-- AI Investor — initial schema (PostgreSQL, Flyway V1).
-- Conventions: all timestamps TIMESTAMPTZ in UTC; money/prices/quantities NUMERIC (BigDecimal), never float;
-- every money value has a currency; enums are TEXT + CHECK (broker/provider types stay open-ended).
-- No credentials are ever stored here — they live in .env (see docs/configuration.md).

-- ============================================================================
-- Accounts & app state
-- ============================================================================

-- Broker accounts. Defined in application.yml + .env, upserted at startup so other tables can reference them.
CREATE TABLE broker_accounts (
    id              TEXT PRIMARY KEY,                       -- alias, e.g. 'ibkr-paper', 'etoro-demo'
    broker          TEXT NOT NULL,                          -- 'IBKR', 'ETORO', … (open-ended)
    display_name    TEXT NOT NULL,
    trading_mode    TEXT NOT NULL CHECK (trading_mode IN ('PAPER', 'LIVE')),
    base_currency   CHAR(3) NOT NULL,
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
    max_position_value  NUMERIC(24, 8) NOT NULL CHECK (max_position_value >= 0),
    max_order_value     NUMERIC(24, 8) NOT NULL CHECK (max_order_value >= 0),
    max_daily_loss      NUMERIC(24, 8) NOT NULL CHECK (max_daily_loss >= 0),
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
    max_cost_per_month      NUMERIC(24, 8),
    cost_currency           CHAR(3) NOT NULL DEFAULT 'USD',
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
    cost                NUMERIC(24, 8),
    cost_currency       CHAR(3),
    prompt              TEXT,                               -- what we sent (no secrets)
    raw_response        TEXT,                               -- what the AI answered, unparsed
    error_message       TEXT
);
CREATE INDEX ai_calls_provider_idx ON ai_calls (provider_id, started_at DESC);
CREATE INDEX ai_calls_run_idx ON ai_calls (analysis_run_id);

-- ============================================================================
-- Instruments & market data
-- ============================================================================

-- Our symbol -> broker's own instrument ID (IBKR conId, eToro instrumentId, …).
CREATE TABLE instrument_mappings (
    id                      BIGSERIAL PRIMARY KEY,
    broker                  TEXT NOT NULL,
    symbol                  TEXT NOT NULL,
    exchange                TEXT NOT NULL DEFAULT '',
    currency                CHAR(3) NOT NULL,
    asset_class             TEXT NOT NULL CHECK (asset_class IN ('STOCK', 'ETF', 'CRYPTO', 'FX', 'COMMODITY', 'INDEX', 'OTHER')),
    broker_instrument_id    TEXT NOT NULL,
    resolved_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (broker, symbol, exchange, currency)
);

-- Price bars used for AI analysis.
CREATE TABLE market_data (
    symbol      TEXT NOT NULL,
    bar_size    TEXT NOT NULL,                               -- e.g. '1d', '1h', '5m'
    bar_time    TIMESTAMPTZ NOT NULL,                        -- bar start, UTC
    open        NUMERIC(24, 8) NOT NULL,
    high        NUMERIC(24, 8) NOT NULL,
    low         NUMERIC(24, 8) NOT NULL,
    close       NUMERIC(24, 8) NOT NULL,
    volume      NUMERIC(24, 8),
    currency    CHAR(3) NOT NULL,
    source      TEXT NOT NULL,                               -- broker or data provider name
    PRIMARY KEY (symbol, bar_size, bar_time, source)
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
    size_value          NUMERIC(24, 8) CHECK (size_value > 0),
    confidence          NUMERIC(5, 4) NOT NULL CHECK (confidence BETWEEN 0 AND 1),
    reasoning           TEXT NOT NULL,                       -- why the AI wants to buy/sell/hold
    status              TEXT NOT NULL CHECK (status IN ('PENDING', 'EXECUTED', 'REJECTED', 'HOLD')),
    rejection_reason    TEXT,                                -- risk check / validation reason when REJECTED
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((size_type IS NULL) = (size_value IS NULL))
);
CREATE INDEX signals_account_idx ON signals (account_id, created_at DESC);

-- Bids/orders sent (or attempted) to the broker, from an AI signal or placed manually.
CREATE TABLE orders (
    id                  BIGSERIAL PRIMARY KEY,
    account_id          TEXT NOT NULL REFERENCES broker_accounts (id),
    signal_id           BIGINT REFERENCES signals (id),      -- NULL for manual orders
    source              TEXT NOT NULL CHECK (source IN ('AI', 'MANUAL')),
    reasoning           TEXT,                                -- copied from the signal, or the user's note for manual orders
    client_order_id     TEXT NOT NULL UNIQUE,                -- our idempotency key, stored before sending
    broker_order_id     TEXT,                                -- assigned by the broker once acknowledged
    symbol              TEXT NOT NULL,
    asset_class         TEXT NOT NULL CHECK (asset_class IN ('STOCK', 'ETF', 'CRYPTO', 'FX', 'COMMODITY', 'INDEX', 'OTHER')),
    side                TEXT NOT NULL CHECK (side IN ('BUY', 'SELL')),
    order_type          TEXT NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT')),
    size_type           TEXT NOT NULL CHECK (size_type IN ('UNITS', 'AMOUNT')),
    size_value          NUMERIC(24, 8) NOT NULL CHECK (size_value > 0),
    limit_price         NUMERIC(24, 8),
    leverage            NUMERIC(10, 4),                      -- NULL = no leverage
    currency            CHAR(3) NOT NULL,
    status              TEXT NOT NULL CHECK (status IN
                            ('CREATED', 'SUBMITTED', 'PARTIALLY_FILLED', 'FILLED', 'CANCELLED', 'REJECTED', 'UNKNOWN')),
    filled_quantity     NUMERIC(24, 8) NOT NULL DEFAULT 0,
    avg_fill_price      NUMERIC(24, 8),
    commission          NUMERIC(24, 8) NOT NULL DEFAULT 0,
    -- Lifecycle timestamps (full history in order_events)
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),  -- bid created
    submitted_at        TIMESTAMPTZ,                         -- sent to broker
    filled_at           TIMESTAMPTZ,                         -- fully filled
    cancelled_at        TIMESTAMPTZ,
    rejected_at         TIMESTAMPTZ,
    close_reason        TEXT,                                -- why cancelled/rejected (by us, the user or the broker)
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (order_type <> 'LIMIT' OR limit_price IS NOT NULL),
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
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders (id),
    broker_fill_id  TEXT NOT NULL,
    quantity        NUMERIC(24, 8) NOT NULL CHECK (quantity > 0),
    price           NUMERIC(24, 8) NOT NULL CHECK (price > 0),
    commission      NUMERIC(24, 8) NOT NULL DEFAULT 0,
    currency        CHAR(3) NOT NULL,
    filled_at       TIMESTAMPTZ NOT NULL,
    UNIQUE (order_id, broker_fill_id)
);

-- Completed (or still open) round-trip trades: entry -> exit, with prices, profit and both reasonings.
CREATE TABLE trades (
    id                  BIGSERIAL PRIMARY KEY,
    account_id          TEXT NOT NULL REFERENCES broker_accounts (id),
    symbol              TEXT NOT NULL,
    asset_class         TEXT NOT NULL CHECK (asset_class IN ('STOCK', 'ETF', 'CRYPTO', 'FX', 'COMMODITY', 'INDEX', 'OTHER')),
    side                TEXT NOT NULL CHECK (side IN ('LONG', 'SHORT')),   -- LONG: buy then sell; SHORT: sell then buy
    status              TEXT NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    quantity            NUMERIC(24, 8) NOT NULL CHECK (quantity > 0),     -- units opened
    entry_price         NUMERIC(24, 8) NOT NULL,                          -- avg start price
    exit_price          NUMERIC(24, 8),                                   -- avg end price (NULL while open)
    entry_signal_id     BIGINT REFERENCES signals (id),                   -- why we bought (LONG) / sold (SHORT)
    exit_signal_id      BIGINT REFERENCES signals (id),                   -- why we sold (LONG) / bought back (SHORT)
    entry_note          TEXT,                                             -- reason for manual entries
    exit_note           TEXT,                                             -- reason for manual exits
    fees                NUMERIC(24, 8) NOT NULL DEFAULT 0,
    realized_pnl        NUMERIC(24, 8),                                   -- profit/loss after fees (NULL while open)
    realized_pnl_pct    NUMERIC(12, 6),                                   -- relative to entry value
    currency            CHAR(3) NOT NULL,
    opened_at           TIMESTAMPTZ NOT NULL,
    closed_at           TIMESTAMPTZ,
    CHECK ((status = 'CLOSED') = (closed_at IS NOT NULL AND exit_price IS NOT NULL AND realized_pnl IS NOT NULL))
);
CREATE INDEX trades_account_idx ON trades (account_id, opened_at DESC);
CREATE UNIQUE INDEX trades_one_open_idx ON trades (account_id, symbol, side) WHERE status = 'OPEN';

-- Which fills opened/closed which trade (supports partial entries and exits).
CREATE TABLE trade_fills (
    trade_id    BIGINT NOT NULL REFERENCES trades (id),
    fill_id     BIGINT NOT NULL REFERENCES fills (id),
    role        TEXT NOT NULL CHECK (role IN ('ENTRY', 'EXIT')),
    quantity    NUMERIC(24, 8) NOT NULL CHECK (quantity > 0),   -- part of the fill used for this trade
    PRIMARY KEY (trade_id, fill_id, role)
);

-- ============================================================================
-- Balance & portfolio history
-- ============================================================================

-- Periodic snapshot of the account's money (taken on a schedule and after every fill).
-- total_value = cash + invested_value (+ unrealized P&L already in market value);
-- free_cash   = cash - reserved_for_orders.
CREATE TABLE account_snapshots (
    id                      BIGSERIAL PRIMARY KEY,
    account_id              TEXT NOT NULL REFERENCES broker_accounts (id),
    taken_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    reason                  TEXT NOT NULL CHECK (reason IN
                                ('SCHEDULED', 'AFTER_FILL', 'ANALYSIS_RUN', 'END_OF_DAY', 'MANUAL')),  -- see docs/brokers.md
    source                  TEXT NOT NULL DEFAULT 'LIVE' CHECK (source IN ('LIVE', 'BACKFILL')),  -- BACKFILL = from broker history (e.g. IBKR Flex)
    currency                CHAR(3) NOT NULL,
    total_value            NUMERIC(24, 8) NOT NULL,         -- all money in the broker (equity / net liquidation)
    invested_value          NUMERIC(24, 8) NOT NULL,         -- market value of open positions
    reserved_for_orders     NUMERIC(24, 8) NOT NULL,         -- money tied up in open bids/orders
    free_cash               NUMERIC(24, 8) NOT NULL,         -- money available for new orders
    cash                    NUMERIC(24, 8) NOT NULL,         -- total cash (free + reserved)
    unrealized_pnl          NUMERIC(24, 8) NOT NULL,
    realized_pnl_today      NUMERIC(24, 8) NOT NULL DEFAULT 0,
    daily_pnl               NUMERIC(24, 8) NOT NULL DEFAULT 0
);
CREATE INDEX account_snapshots_account_idx ON account_snapshots (account_id, taken_at DESC);
CREATE UNIQUE INDEX account_snapshots_one_eod_idx ON account_snapshots (account_id, ((taken_at AT TIME ZONE 'UTC')::date))
    WHERE reason = 'END_OF_DAY';

-- Positions at the time of each account snapshot.
CREATE TABLE position_snapshots (
    id                  BIGSERIAL PRIMARY KEY,
    account_snapshot_id BIGINT NOT NULL REFERENCES account_snapshots (id) ON DELETE CASCADE,
    symbol              TEXT NOT NULL,
    asset_class         TEXT NOT NULL CHECK (asset_class IN ('STOCK', 'ETF', 'CRYPTO', 'FX', 'COMMODITY', 'INDEX', 'OTHER')),
    side                TEXT NOT NULL CHECK (side IN ('LONG', 'SHORT')),
    quantity            NUMERIC(24, 8) NOT NULL CHECK (quantity > 0),
    avg_price           NUMERIC(24, 8) NOT NULL,
    market_price        NUMERIC(24, 8) NOT NULL,
    market_value        NUMERIC(24, 8) NOT NULL,
    unrealized_pnl      NUMERIC(24, 8) NOT NULL,
    leverage            NUMERIC(10, 4),
    currency            CHAR(3) NOT NULL
);
CREATE INDEX position_snapshots_snapshot_idx ON position_snapshots (account_snapshot_id);

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
       t.symbol,
       t.side,
       t.status,
       t.quantity,
       t.entry_price,
       t.exit_price,
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
LEFT JOIN signals es ON es.id = t.entry_signal_id
LEFT JOIN signals xs ON xs.id = t.exit_signal_id;
