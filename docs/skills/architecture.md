# Architecture

## Tech stack (planned — update when decided)

- **Backend:** Java 21 + Spring Boot 4 (Spring Web, Spring Data JPA, Bean Validation)
- **Broker connections:** each broker is an adapter behind our own `BrokerClient` interface (see [brokers.md](../rules/brokers.md))
  - **IBKR:** official IBKR TWS API Java client (`EClient`/`EWrapper`) talking to TWS or IB Gateway; the adapter turns callbacks into `CompletableFuture`s and handles reconnects
  - **eToro:** eToro public REST API (API key + user key, separate demo/virtual and real environments) called via `RestClient`. Check the current eToro API docs for endpoints, auth headers, rate limits and order semantics before implementing — don't guess them
  - **....** any broker can be added later without any problems / major work
- **AI models:** multiple providers behind our own `AiProvider` interface, with automatic failover (see [ai-providers.md](../rules/ai-providers.md)):
  - **Paid APIs:** OpenAI (ChatGPT models), Google Gemini, xAI Grok, Anthropic Claude — called via their HTTP APIs with `RestClient` (or Spring AI where it supports the provider)
  - **Free:** local models via Ollama, and free tiers of hosted APIs (e.g. Gemini free tier, OpenRouter/Groq free models)
  - **....** any provider/model can be added later as a new adapter + config
  - Note: consumer chat subscriptions (ChatGPT Plus, Claude Pro, Gemini Advanced, SuperGrok …) do **not** include API access; each provider needs an API key from its developer platform, billed separately
- **Database:** PostgreSQL; Flyway migrations; Testcontainers (or H2) for tests
- **Frontend:** React + Vite + TypeScript
- **Scheduling:** Spring `@Scheduled` for periodic analysis runs during market hours
- **Build:** Maven

## Backend packages

Under `backend/src/main/java/com/aiinvestor/`:

| Package | Contents |
|---|---|
| `account` | Broker accounts, `ActiveAccountService` (the one place to ask for the active account) |
| `ai` | `AiProvider`, `AiRouter`, failure types; each provider adapter goes in its own subpackage (`ai/openai`, …) |
| `api` | REST controllers and API DTOs ([api-contract.md](api-contract.md)) |
| `audit` | `AuditService` and the audit log |
| `broker` | `BrokerClient`, `BrokerCapabilities`, `BrokerRegistry`; each broker adapter goes in its own subpackage (`broker/ibkr`, `broker/etoro`, …) |
| `config` | `@ConfigurationProperties` classes, web/CORS setup |
| `domain` | Broker-neutral records and enums (`OrderRequest`, `Order`, `Fill`, `Position`, …) |
| `execution` | Turning approved signals into orders, client order IDs |
| `risk` | `RiskEngine`, limits per account |
| `scheduler` | Periodic runs (`AnalysisScheduler`) |
| `settings` | App state: active account, kill switch |
| `signal` | Stored signals, `SignalParser` (strict AI output validation) |

**Unfinished pipeline code refuses instead of pretending to work,** so nothing can trade by accident: `AiRouter` returns no response, `SignalParser` always returns HOLD, `RiskEngine` rejects everything, `ExecutionService` throws, `AnalysisScheduler` isn't scheduled, and unbuilt endpoints return 501 with a reason. `NothingCanTradeYetTest` and `UnbuiltEndpointsTest` pin this; replace those tests as the real code is built.

## Data flow

1. **Market data** — fetch prices/history (from a broker's market data via its adapter, or another free source behind a `MarketDataProvider` interface).
2. **AI analysis** — the `AiRouter` picks the first available AI provider from the configured chain, sends the data, and gets back a structured, broker-neutral signal: `symbol`, `action` (BUY/SELL/HOLD), `quantity` or size hint, `confidence`, `reasoning`. If a provider fails technically (quota, rate limit, error, timeout), the router falls back to the next one. The AI never knows or chooses which broker is used.
3. **Risk check** — validate the signal against risk rules (see [safety.md](../rules/safety.md)), using the active account's limits. Rejected signals are still stored, with the rejection reason.
4. **Execution** — the execution service maps the signal to a broker-neutral `OrderRequest` and sends it to the active account's `BrokerClient` adapter (the active account is read once at the start of each analysis run, so a switch mid-run can't split a run across brokers).
5. **Persistence** — store signals, orders, fills, positions snapshots and errors, each tagged with `broker` and `account_id` (internal alias).
6. **Frontend** — reads everything from the backend REST API ([api-contract.md](api-contract.md)); never talks to a broker or the AI model directly.

## Database

Schema and table descriptions: [database.md](database.md).
