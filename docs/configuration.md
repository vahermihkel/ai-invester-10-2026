# Configuration, secrets & access — always follow

## One secrets file: `.env`

All credentials and account settings live in **one file: `.env` in the project root** (git-ignored). `.env.example` is the committed template with placeholders; keep it in sync whenever a variable is added, renamed or removed.

- **What goes in `.env`:** broker credentials/connection settings for every account, `ACTIVE_ACCOUNT`, `ALLOW_LIVE_TRADING`, AI API keys, database credentials. Nothing else needs editing to change accounts or credentials.
- **Loading:** the backend reads `.env` at startup (e.g. `spring.config.import=optional:file:../.env[.properties]` or an equivalent loader) and binds values through `@ConfigurationProperties`. `application.yml` only references variables (`${IBKR_PAPER_PORT}`), never contains real values. Changing `.env` requires a backend restart; no hot reload.
- **Account definitions:** the fixed list of known accounts (alias, broker type, PAPER/LIVE) is in `application.yml`; their credentials come from `.env`. Naming: `<BROKER>_<ENV>_<SETTING>`, e.g. `IBKR_PAPER_PORT`, `ETORO_DEMO_API_KEY`. Adding an account = add it to `application.yml` + its variables to `.env.example`.
- **Account availability from `.env`:** an account is enabled only when all its required variables are filled in; if any are empty it is disabled (shown as disabled in the UI) with a startup warning naming the missing variable — never a crash, never printing values.
- **Active account:**
  - `ACTIVE_ACCOUNT` in `.env` is the default.
  - The UI can switch to another enabled account at runtime; the choice is stored in the DB and survives restarts.
  - If `ACTIVE_ACCOUNT` in `.env` was changed since the last start (the last-seen value is stored in the DB), the `.env` value wins at startup. So editing `.env` + restart always works, and the UI dropdown is the quick way.
- **Live trading switch:** `ALLOW_LIVE_TRADING=false` by default. While false, LIVE accounts can't be made active (neither from `.env` nor UI) — the backend starts with no active account and a clear error if `ACTIVE_ACCOUNT` points to a LIVE account.
- **Broker specifics:**
  - IBKR: the app needs host/port/client ID/account ID only. The IBKR username and password are entered in TWS / IB Gateway, never stored by this app.
  - eToro: API key + user key per environment (demo / real).
- **AI keys:** one variable per provider entry; empty key = provider disabled with a warning.
- **Never** hardcode credentials, print them in logs/errors/API responses, or send them to the frontend. Account IDs shown in the UI are masked; the app refers to accounts by alias.
- `.gitignore` must include `.env`, `.env.*` (except `.env.example` and `frontend/.env.development`), database files, logs, `node_modules/`, `target/`, `build/`, `.gradle/`, `.idea/`.

## Access: single user, no login

The app is used by one person on their own computer. **There is no login/logout and no user accounts** — don't add authentication, user tables or sessions. Anyone else who wants to use it installs their own copy with their own `.env`.

Because the API has no login and controls real money, access is limited to the local machine instead:

- Backend binds to `127.0.0.1` only (`server.address=127.0.0.1`), never `0.0.0.0`.
- CORS allows only the local frontend (`http://localhost:5173` in dev); no wildcard origins.
- Never expose the backend port to the network or internet (no port forwarding, no public hosting) without first adding authentication — note this in the README if the app is shared.
