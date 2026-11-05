# AI Investor

Automated trading app. An AI model (any of several configured providers — free/local or paid, switched automatically) analyses market data and produces buy/sell signals; the backend turns those signals into orders on the user's brokerage account. Supported brokers: **Interactive Brokers (IBKR)**, **eToro** or something else. Several accounts can be configured, but **only one is active at a time** (traded and shown in the UI); switching between them is a single action in the UI header (see `docs/rules/brokers.md`). More brokers and AI providers will be added later, so the code must be broker- and AI-agnostic. Every signal, order and fill is stored in a database so history can be reviewed in the frontend.

**This app moves real money — the safety rules in `docs/rules/safety.md` always apply.**

## Project layout

```
ai-investor/
├── backend/    # API server, broker adapters (IBKR, eToro, …), AI providers + router, trade execution, scheduler
├── frontend/   # Web UI: dashboard, positions, trade history, AI decisions, settings
├── docs/       # rules/ (constraints code must follow), skills/ (reference: design, schema, API, roadmap); all imported below
└── CLAUDE.md
```

The frontend is set up (see Commands). The backend is a skeleton: it applies the database schema through Flyway and serves accounts, account switching, the kill switch, settings and stored signals/orders to the frontend, and its packages and interfaces are in place (see `docs/skills/architecture.md`), but there are no broker adapters, AI providers or trading logic yet (see `docs/skills/backend-roadmap.md`). Unfinished pipeline code refuses by design. Update these files when code is added (commands, structure, env vars).

## Detailed docs

Each file covers one topic; update the matching file instead of growing this one.

| File | Contents |
|---|---|
| `docs/rules/safety.md` | Safety rules |
| `docs/rules/configuration.md` | `.env` secrets file, switching accounts, single-user / no login, local-only access |
| `docs/skills/architecture.md` | Tech stack, data flow |
| `docs/skills/database.md` | Database schema: what each table stores (source: `backend/src/main/resources/db/migration/`) |
| `docs/rules/brokers.md` | Multi-broker architecture (IBKR, eToro, future brokers) |
| `docs/rules/ai-providers.md` | Multi-AI-provider architecture, priority, failover, quota reset handling |
| `docs/skills/api-contract.md` | REST API endpoints and generic API models |
| `docs/rules/conventions.md` | Code conventions |
| `docs/skills/backend-roadmap.md` | What the backend must do and in which order to build it (checklist) |

@docs/rules/safety.md
@docs/rules/configuration.md
@docs/skills/architecture.md
@docs/skills/database.md
@docs/rules/brokers.md
@docs/rules/ai-providers.md
@docs/skills/api-contract.md
@docs/rules/conventions.md
@docs/skills/backend-roadmap.md

## Commands

Frontend (run in `frontend/`):
- `npm install` — install dependencies
- `npm run dev` — dev server on http://localhost:5173 (proxies `/api` to the backend on :8080)
- `npm run build` — type-check + production build

Secrets: copy `.env.example` to `.env` in the project root and fill in your values (git-ignored). Restart the backend after editing it.

`frontend/.env.development` sets `VITE_USE_MOCK=false`, so the UI calls the backend, which must be running. Set it to `true` to use in-memory mock data (`src/mockData.ts`) with no backend.

Backend (run in `backend/`, needs JDK 21 and the PostgreSQL database from `.env`; Maven is downloaded by the wrapper):
- `./mvnw spring-boot:run` — start on http://127.0.0.1:8080; applies pending Flyway migrations to the database
- `./mvnw test` — run tests; the integration test needs Docker running (Testcontainers) and is skipped without it
