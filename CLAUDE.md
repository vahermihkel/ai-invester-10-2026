# AI Investor

Automated trading app. An AI model (any of several configured providers — free/local or paid, switched automatically) analyses market data and produces buy/sell signals; the backend turns those signals into orders on the user's brokerage account. Supported brokers: **Interactive Brokers (IBKR)** and **eToro**. Several accounts can be configured, but **only one is active at a time** (traded and shown in the UI); switching between them is a single action in the UI header (see `docs/brokers.md`). More brokers and AI providers will be added later, so the code must be broker- and AI-agnostic. Every signal, order and fill is stored in a database so history can be reviewed in the frontend.

**This app moves real money — the safety rules in `docs/safety.md` always apply.**

## Project layout

```
ai-investor/
├── backend/    # API server, broker adapters (IBKR, eToro, …), AI providers + router, trade execution, scheduler
├── frontend/   # Web UI: dashboard, positions, trade history, AI decisions, settings
├── docs/       # Project rules, imported below
└── CLAUDE.md
```

The frontend is set up (see Commands); the backend folder is still empty. Update these files when the stack is chosen and code is added (commands, structure, env vars).

## Detailed docs

Each file covers one topic; update the matching file instead of growing this one.

| File | Contents |
|---|---|
| `docs/safety.md` | Safety rules |
| `docs/configuration.md` | `.env` secrets file, switching accounts, single-user / no login, local-only access |
| `docs/architecture.md` | Tech stack, data flow, database tables |
| `docs/brokers.md` | Multi-broker architecture (IBKR, eToro, future brokers) |
| `docs/ai-providers.md` | Multi-AI-provider architecture, priority, failover, quota reset handling |
| `docs/api-contract.md` | REST API endpoints and generic API models |
| `docs/conventions.md` | Code conventions |
| `docs/backend-roadmap.md` | What the backend must do and in which order to build it (checklist) |

@docs/safety.md
@docs/configuration.md
@docs/architecture.md
@docs/brokers.md
@docs/ai-providers.md
@docs/api-contract.md
@docs/conventions.md
@docs/backend-roadmap.md

## Commands

Frontend (run in `frontend/`):
- `npm install` — install dependencies
- `npm run dev` — dev server on http://localhost:5173 (proxies `/api` to the backend on :8080)
- `npm run build` — type-check + production build

Secrets: copy `.env.example` to `.env` in the project root and fill in your values (git-ignored). Restart the backend after editing it.

`frontend/.env.development` sets `VITE_USE_MOCK=true`, so the UI uses in-memory mock data (`src/mockData.ts`). Set it to `false` once the backend is running.

Backend: _to be filled in once the Maven project is set up._
