# Database schema

PostgreSQL, managed by Flyway. Schema source of truth: `backend/src/main/resources/db/migration/` (`V1__initial_schema.sql` is the initial schema). **Never edit an applied migration** — add a new `V<n>__description.sql`, and update this file to match.

Rules: timestamps `TIMESTAMPTZ` in UTC; money/prices/quantities `NUMERIC(24,8)` ↔ `BigDecimal`; every money value has a currency; enums are `TEXT` + `CHECK` (broker and AI provider types stay open-ended, no CHECK); no credentials in the DB (they're in `.env`).

## Accounts & app state

| Table | Purpose |
|---|---|
| `broker_accounts` | Configured accounts (alias, broker, PAPER/LIVE, base currency, enabled). Upserted from `application.yml` + `.env` at startup. |
| `app_settings` | Single row: `active_account_id`, `last_seen_env_active_account`, `trading_enabled` (kill switch), `analysis_interval_minutes`, `ai_router_strategy`. |
| `active_account_changes` | History of broker switches (from, to, source UI/ENV/STARTUP, time). |
| `risk_limits` | Limits per account (max position/order value, max daily loss, max trades/day, allowed symbols). |

## AI models

| Table | Purpose |
|---|---|
| `ai_providers` | List of AI models: type, model, FREE/PAID, **`priority`** (1 = first; order TBD), `enabled`, **`unavailable_reason` + `unavailable_until`** (tokens used up → date+time it's usable again), own budgets. |
| `ai_quota_events` | Every "tokens used / quota / rate limit" error: when detected, **`reset_at`**, whether the reset time came from the provider or was estimated. |
| `analysis_runs` | One analysis cycle for the active account (scheduled/manual, status, start/end). |
| `ai_calls` | Every AI request incl. failed ones: provider, model, attempt number (fallback order), outcome, tokens, cost, latency, prompt, raw response. |
| `v_ai_provider_status` (view) | Models in priority order with `available` (now) and `available_again_at`. |

A provider is available when `enabled AND (unavailable_until IS NULL OR unavailable_until <= now())`. On a quota error the router sets `unavailable_until` and inserts an `ai_quota_events` row in the same transaction (see [ai-providers.md](ai-providers.md)).

## Trading

| Table | Purpose |
|---|---|
| `signals` | Every AI decision: BUY/SELL/HOLD, size, confidence, **reasoning** (why), which AI model + fallbacks, status, risk-check rejection reason. |
| `orders` | Bids/orders, from AI (`signal_id`) or manual (`source`). **Reasoning**, our `client_order_id` (idempotency), broker order ID, side, type, size, limit price, status, fill summary, and lifecycle times: `created_at`, `submitted_at`, `filled_at`, `cancelled_at`, `rejected_at`, `close_reason`. |
| `order_events` | Full status history of each order with time and message (when it was submitted, partially filled, cancelled, rejected, …). |
| `fills` | Executions from the broker (partial fills supported). |
| `trades` | Round trips: entry → exit. **Start price** (`entry_price`), **end price** (`exit_price`), quantity, fees, **profit** (`realized_pnl`, `realized_pnl_pct`), times, and links to the signals with the **reasoning for buying and selling** (`entry_signal_id`, `exit_signal_id`; `entry_note`/`exit_note` for manual). |
| `trade_fills` | Which fills opened/closed which trade (partial entries/exits). |
| `v_trade_history` (view) | Trades with entry/exit reasoning, profit and `successful` (= `realized_pnl > 0`). |

LONG trade = buy then sell; SHORT trade = sell then buy back. Only one OPEN trade per account + symbol + side.

## Balance & portfolio history

| Table | Purpose |
|---|---|
| `account_snapshots` | Account money over time, taken on a schedule and after every fill: **`total_value`** (all money in the broker), **`invested_value`** (positions' market value), **`reserved_for_orders`** (money under open bids), **`free_cash`** (available), `cash`, unrealized/realized/daily P&L. `reason` (SCHEDULED / AFTER_FILL / ANALYSIS_RUN / END_OF_DAY / MANUAL) and `source` (LIVE / BACKFILL); at most one END_OF_DAY per account per UTC day. Schedule and retention: [brokers.md](brokers.md). |
| `position_snapshots` | Positions belonging to each account snapshot. |

Relations: `free_cash = cash - reserved_for_orders`; `total_value ≈ cash + invested_value`. Store the broker's reported values; if the broker doesn't report `reserved_for_orders`, compute it from our open BUY orders.

## Other

| Table | Purpose |
|---|---|
| `instrument_mappings` | Our symbol → broker's instrument ID, per broker. |
| `market_data` | Price bars (OHLCV) used for AI analysis. |
| `audit_log` | Every other decision/event/error (kill switch, reconnects, startup checks, …) with UTC time. |
