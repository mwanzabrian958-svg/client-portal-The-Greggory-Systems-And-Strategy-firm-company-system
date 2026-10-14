# 🛡️ OWASP Security Review — Greggory Client Portal

**Full-stack assessment:** API Security Top 10 · Web Top 10 · Mobile Top 10

| | |
|---|---|
| **Date** | 14 October 2026 |
| **Reviewer** | Cline (automated OWASP review + live probing) |
| **Method** | Static source analysis (all middleware, routes, Gradle, manifest) **+ live penetration probes against the production release backend** |
| **Overall posture** | Core authn/authz solid; several defense-in-depth and configuration gaps (details below) |

---

## 📦 Scope

| Component | Location | Deployment |
|---|---|---|
| React/Vite backend (main API) | `w-The-Greggory-Systems-And-Strategy-firm/…/website/` | `https://w-the-greggory-systems-and-strategy-firm-1vf9.onrender.com` (Render) |
| Android gateway backend | `client-portal-…/server.js` | Render (secondary gateway) |
| Download site | `docs/index.html` | `https://greggory-client-portal-downloads.onrender.com` |
| Android APK (release) | `app/` (`BASE_URL` at `app/build.gradle:75`) | GitHub raw + download site |

---

## ✅ Live probe results (release backend)

| # | Probe | Expected | Actual | Verdict |
|---|---|---|---|---|
| 1 | No token → `GET /api/users/client-dashboard` | 401 | **401** | ✅ Pass |
| 2 | SQLi tautology on login (`admin' OR '1'='1@x.com`) | 401 | **401** `{"error":"Invalid credentials"}` | ✅ Pass |
| 3 | CORS hostile origin `https://evil.example.com` | Blocked | **Blocked** (no ACAO header) — but returns a sloppy **500** | ⚠️ Pass w/ issue |
| 4 | 12× rapid failed logins | 429 eventually | **401 ×12** (global 100/15min; per-IP DB auth limit = 50/min, not tripped) | ⚠️ Loose |
| 5 | Valid token (user 54) + mismatched `X-Greggory-Client-ID: 53` | 403 | **200 — but data still scoped to user 54 only** | ⚠️ See API-1 |
| 6 | API security headers (`/health`) | helmet set | HSTS, X-Frame-Options, nosniff, Referrer-Policy present; **no CSP** | ⚠️ See API-6 |

> **BOLA bottom line:** No cross-tenant data leak was achieved. All responses remained scoped to the
> token owner. The 403 routing-integrity check exists in code but is bypassed by a duplicate
> middleware (see **API-1**).

---

# 1️⃣ OWASP API Security Top 10

## API-1 · BOLA/BFLA — header-integrity check bypassed by duplicate middleware — 🟠 MEDIUM

**Root cause (confirmed):**

- `backend/middleware/clientAuth.js:114-123` correctly enforces:
  `X-Greggory-Client-ID` ≠ token user → **403 Routing Integrity Breach**.
- But `server.js:145` defines its **own inline `authenticateUser`** that contains **zero**
  references to `greggory-client-id` (verified via grep over `server.js`).
- The route `GET /api/users/client-dashboard` is defined **inline at `server.js:2078`** using that
  weaker copy, and registers **before** the modular `usersRouter` mount at `server.js:7048`.
  Express matches routes in registration order → **the inline route wins** and the hardened check
  in `clientAuth.js` never executes for this endpoint.

**Why it's not Critical:** authorization derives from the JWT/session token; every query is
scoped `WHERE user_id = token id`. The live probe returned only user 54's data despite the
mismatched header. This is a **defense-in-depth failure, not an exploit**.

**Note:** the companion gateway (`client-portal-…/server.js:110-116`) *does* enforce the check
correctly (`routingId !== Number(decoded.id) → 403`).

**Fix (choose one):**
1. Delete the inline route at `server.js:2078` and let `usersRouter` serve it, **or**
2. Replace the inline `authenticateUser` at `server.js:145` with
   `require("./backend/middleware/clientAuth")`.
3. Ultimately: **one middleware, one route definition** — remove the divergence.

---

## API-2 · Broken Authentication — weak `JWT_SECRET` fallback — 🔴 HIGH (conditional)

```js
// client-portal-…/server.js:13
const JWT_SECRET = process.env.JWT_SECRET || 'change-me-in-render-env';
```

If the Render env var is ever unset/renamed, every token is signed with a **public default** →
tokens become forgeable by anyone who reads the repo.

**Fix:** fail fast at startup instead of defaulting:

```js
if (!process.env.JWT_SECRET) {
  console.error("FATAL: JWT_SECRET is not set");
  process.exit(1);
}
const JWT_SECRET = process.env.JWT_SECRET;
```

**Positive:** admin-session tokens use HMAC-SHA256 with `crypto.timingSafeEqual` verification
(`server.js:237-256`) — constant-time, exp-bound. ✅

---

## API-3 · Injection (SQLi / command) — 🟢 PASSED

- Parameterized queries everywhere (`?` placeholders).
- Repo-wide grep found **zero** template-literal SQL interpolating `req.body` / `req.params`.
- Live tautology probe → **401**, no auth bypass.

**Minor note:** DB pool sets `multipleStatements: true` — increases blast radius *if* an
injection were ever introduced. Consider disabling unless strictly needed by the import scripts.

---

## API-4 · Mass assignment — 🟢 PASSED

No `req.body.role` / `is_active` / `primary_role` reaching INSERT/UPDATE (grep clean).
Register whitelists columns explicitly:
`INSERT INTO users (first_name, last_name, display_name, email, phone_number, password_hash)`.

---

## API-5 · Resource limits / brute force — 🟡 LOW–MEDIUM

Existing controls:
- Global limiter: **100 req / 15 min** (live-confirmed: `x-ratelimit-limit: 100`).
- `express-rate-limit` on admin login.
- `authEndpointValidator.js:80-89`: **DB-driven per-IP auth rate limit**, default
  **50 attempts/min** (`auth_request_log` table, dynamic rule from `auth_validation_rules`).

12 rapid bad logins all returned 401 — under every threshold. Functional but **50/min password
guessing is generous** for a login endpoint.

**Fix:** tighten login to ~5–10/min per IP+email with progressive lockout/backoff.

---

## API-6 · Security misconfiguration — 🟠 MEDIUM

| Issue | Evidence |
|---|---|
| **CSP explicitly disabled** | `server.js:551` → `contentSecurityPolicy: false` |
| `/health/db` public DB diagnostics | Returns `dbHost`, `dbPort`, `dbName`, `dbUser` unauthenticated (no password, but recon-friendly) |
| CORS deny returns **500** | `callback(new Error("CORS: origin … not allowed"))` leaks error-handling behavior |
| CORS allowlist permits **any private RFC1918 origin + localhost** | `server.js:500-504`, with `credentials: true` — fine for dev, risky if this ever serves cookie auth |

**Confirmed-good headers (live, `/health`):**

```
strict-transport-security: max-age=31536000; includeSubDomains
x-frame-options: SAMEORIGIN
x-content-type-options: nosniff
referrer-policy: no-referrer
x-dns-prefetch-control: off
x-ratelimit-limit: 100
```

---

## API-7 · Inventory & documentation — 🟢 PASSED

`API_DOCUMENTATION.md`, `SECURITY.md`, `TECHNICAL_DOCUMENTATION.md` maintained and accurate
(routing-integrity behavior documented, matches intent — if not current wiring).

---

# 2️⃣ OWASP Web Top 10

| # | Risk | Finding | Severity |
|---|---|---|---|
| A01 | Broken access control | See API-1. Admin sessions: constant-time HMAC ✅ | 🟠 Med |
| A02 | Cryptographic failure | Weak `JWT_SECRET` fallback (API-2); **DB pool `ssl: { rejectUnauthorized: false }` in ALL server files** — disables TLS certificate validation to Aiven | 🔴 High |
| A03 | Injection | Clean (API-3) | 🟢 — |
| A04 | Insecure design | Strengths: signed-token logout, `user_sessions` table (per-device revocation), `auth_platform_mapping` table-isolation protocol, audit logging | 🟢 — |
| A05 | Security misconfiguration | **No CSP anywhere** (web + download site). Download site serves `nosniff` but no CSP/X-Frame-Options | 🟠 Med |
| A06 | Vulnerable components | Not assessed (no `npm audit` run this session) | ⚪ N/A |
| A07 | Authentication failures | Rate limits present but loose (API-5) | 🟡 Low |
| A08 | Data integrity failures | `version.json` update manifest unsigned (see M5) | 🟠 Med |
| A09 | Logging & monitoring | `auth_request_log` audit trail with body **hash** (never raw passwords) ✅; global logger records method+URL only ✅ | 🟢 — |
| A10 | SSRF | Server-side fetches constrained to fixed providers (M-Pesa / FCM / WhatsApp) | 🟡 Low |

**XSS note:** React escapes by default, so the practical XSS risk is the **missing CSP** — add a
CSP once the production build's external resources (Google Sign-In, fonts) are inventoried.

---

# 3️⃣ OWASP Mobile Top 10

## M1 · Improper platform usage — token storage — 🟠 MEDIUM

`PreferencesManager.kt:9-25` correctly uses **`EncryptedSharedPreferences`**
(AES256-SIV keys / AES256-GCM values, Android Keystore MasterKey) — ✅

**But** on *any* exception it **silently falls back to plain `SharedPreferences`**
(`GreggoryPrefsSecure`), where the auth token would sit in cleartext app XML. The fallback is
never logged, so a Keystore failure would go unnoticed.

**Fix:** log the downgrade, warn the user, or hard-fail (force re-login) rather than downgrade.

---

## M2 · Data storage — 🟡 LOW

- Auth token encrypted ✅.
- Local Room cache (projects / reports / invoices) unencrypted — acceptable given Android
  FBE at rest; note for rooted-device threat model.
- **No `.env` and no `keystore.jks` in git** ✅ (only `.env.example` tracked — verified via
  `git ls-files`).

---

## M3 · Insecure communication — 🟠 MEDIUM

- **No certificate pinning**: no `CertificatePinner` anywhere; plain `OkHttpClient()` in both
  `RetrofitClient.kt` and `UpdateManager.kt`. TLS + server-side HSTS ✅, but custom-CA/MITM on a
  compromised device would succeed.
- `network_security_config.xml` correctly restricts cleartext to `10.0.2.2` (debug/emulator
  only) ✅.

**Fix (if required by threat model):**
```kotlin
CertificatePinner.Builder()
    .add("w-the-greggory-systems-and-strategy-firm-1vf9.onrender.com",
         "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=") // real pin
    .build()
```
> ⚠️ Pins must be rotated before the server certificate — Render/Cloudflare rotation can break
> the app. Weigh cost vs. benefit for this portal.

---

## M4 · Insufficient authentication — 🟡 LOW

7-day tokens (`TOKEN_TTL = '7d'`), bcrypt cost 10, per-device `user_sessions` revocation ✅,
optional biometric gate ✅. Positive state.

---

## M5 · Insecure update mechanism (supply chain) — 🔴 HIGH — *top mobile finding*

`UpdateManager.kt:25-80`:

1. Fetches **unsigned `version.json` from GitHub raw** (`UPDATE_URL`, line 25).
2. Downloads **whatever APK URL that JSON lists** —
   `Request.Builder().url(updateUrl)` (line 48) accepts **any host, no allowlist**.
3. **No SHA-256 checksum, no metadata signature, no min-versionCode floor** before
   `installApk()` (line 74).

**Mitigating factor:** Android's PackageInstaller still enforces the update APK is signed with
the **same key as the installed app**, so a third party cannot replace the app outright.
A tampered `version.json` (repo compromise / poisoned `main` branch) can nevertheless trigger
**downgrade attacks** and arbitrary-URL fetches.

**Fix:**
- Allowlist the download host (`raw.githubusercontent.com/mwanzabrian958-svg/...`).
- Embed `sha256` in `version.json`; verify the digest of the downloaded APK before install.
- Reject `versionCode <= BuildConfig.VERSION_CODE` at install time to block downgrades.

```jsonc
// version.json (target format)
{
  "versionCode": 12,
  "versionName": "1.2.0",
  "url": "https://raw.githubusercontent.com/.../client-portal.apk",
  "sha256": "ab12…",
  "features": ["…"]
}
```

---

## M6 · Privacy — data leakage in logs — 🟠 LOW–MEDIUM

```kotlin
// FirebaseMessagingService.kt:19
Log.d("FCM", "New token generated: $token")
```

- The **FCM token is written to logcat**, and `proguard-rules.pro` contains **no
  `-assumenosideeffects` Log-stripping rules** → this survives in the **release** APK.
- Backend/`UpdateManager` logs contain only error messages — no auth tokens ✅.

**Fix:** gate on `BuildConfig.DEBUG`, remove the log line, **or** add:

```proguard
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
```

---

## M7 · Code quality / platform hygiene — 🟡 LOW

Release build (`app/build.gradle:64-78`) is in good shape:

| Control | State |
|---|---|
| `minifyEnabled true` | ✅ |
| `shrinkResources true` | ✅ |
| Signed with release keystore (env or fallback file) | ✅ |
| `debuggable` in manifest | ✅ Absent (verified) |
| `debug` buildType cleartext limited to `10.0.2.2` | ✅ |
| `lint { checkReleaseBuilds false; abortOnError false }` | ⚠️ Release lint gates disabled — minor |

Broad `-keep` rules for `data.api.**` (required for Gson) and Firebase/Room — acceptable.

---

## M8 · Reverse engineering — 🟡 LOW

No pinning + broad keep-rules → traffic/models inspectable via MITM by a device owner.
Acceptable for this threat model once M3/M5 are addressed.

---

## M9 · Mobile anti-automation / tamper detection — ⚪ INFO

No root/hook detection (e.g. RootBeer, Play Integrity). Acceptable for a B2B client portal;
residual risk on rooted devices only.

---

## M10 · Resilience — ⚪ INFO

Updater failure only logs (`UpdateManager.kt:77`) and the app continues on the old version ✅.

---

# 🎯 Priority remediation roadmap

| Priority | Item | Effort |
|---|---|---|
| 🔴 **1** | Remove `ssl: { rejectUnauthorized: false }` (or pin the Aiven CA) in **all** DB pools; fail-fast on missing `JWT_SECRET` | S |
| 🔴 **2** | Updater (M5): SHA-256 verification + host allowlist in `version.json` flow | M |
| 🟠 **3** | API-1: delete inline `client-dashboard` route / unify auth middleware so the 403 check actually runs | S |
| 🟠 **4** | M1: stop silent plaintext-SharedPreferences downgrade | S |
| 🟠 **5** | M6: remove FCM token `Log.d` or strip logs in ProGuard | S |
| 🟠 **6** | API-6/A05: enable CSP (web + download site); return non-500 on CORS deny | M |
| 🟡 **7** | API-5: tighten login rate limit (~5–10/min per IP+email) | S |
| 🟡 **8** | Hide `/health/db` diagnostics behind auth | S |

*S = small (<1 hr) · M = medium (1–4 hr)*

---

# ✅ Clean bill (verified secure)

- **SQL injection** — parameterized queries, live probe passed
- **Mass assignment** — no role/status over-posting
- **No-auth access** — 401 on protected routes
- **BOLA data scoping** — token-derived `WHERE user_id = token id`, no cross-tenant leak achieved
- **CORS allowlist** — hostile origins blocked
- **Security headers** — helmet + HSTS + X-Frame-Options + nosniff live-confirmed
- **Secrets out of git** — no `.env`, no keystore tracked
- **Token storage** — EncryptedSharedPreferences (primary path)
- **Release build** — signed, R8 minify + resource shrink, not debuggable
- **Admin sessions** — constant-time HMAC comparison, expiry-bound

---

# 📋 Appendix A — Live probe commands (reproducible)

```powershell
$b = 'https://w-the-greggory-systems-and-strategy-firm-1vf9.onrender.com'

# 1. Login → get token
[IO.File]::WriteAllText("$env:TEMP\login.json",
  '{"email":"<user>","password":"<pass>"}', [Text.UTF8Encoding]::new($false))
curl.exe -s -X POST "$b/api/users/login" -H 'Content-Type: application/json' `
  --data-binary "@$env:TEMP\login.json" -o "$env:TEMP\tok.json"
$tok = (Get-Content "$env:TEMP\tok.json" -Raw | ConvertFrom-Json).token

# 2. No-auth probe → expect 401
curl.exe -s -o NUL -w '%{http_code}' "$b/api/users/client-dashboard"

# 3. BOLA / routing-integrity probe → expect 403 (currently returns 200, see API-1)
curl.exe -s -o NUL -w '%{http_code}' "$b/api/users/client-dashboard" `
  -H "Authorization: Bearer $tok" -H 'X-Greggory-Client-ID: 53'

# 4. SQLi tautology on login → expect 401
[IO.File]::WriteAllText("$env:TEMP\sqli.json",
  '{"email":"admin'' OR ''1''=''1@x.com","password":"x"}', [Text.UTF8Encoding]::new($false))
curl.exe -s -X POST "$b/api/users/login" -H 'Content-Type: application/json' `
  --data-binary "@$env:TEMP\sqli.json"

# 5. CORS hostile origin → expect NO access-control-allow-origin
curl.exe -s -I -H 'Origin: https://evil.example.com' "$b/health"

# 6. Security headers
curl.exe -s -D - -o NUL "$b/health"
```

> **PowerShell gotcha:** passing JSON inline via `--data-raw '{...}'` gets mangled by
> PowerShell's argument quoting and yields spurious 500s — always write the body to a file and
> use `--data-binary "@file"`.

---

# 📋 Appendix B — Key evidence map

| Finding | File:Line |
|---|---|
| BOLA bypass root cause (inline auth, no header check) | `w-…/website/server.js:145` |
| Inline dashboard route (wins over router) | `w-…/website/server.js:2078` |
| Modular router mount (too late) | `w-…/website/server.js:7048` |
| Correct routing-integrity check (unused for this route) | `w-…/website/backend/middleware/clientAuth.js:114-123` |
| Working check in companion gateway (reference impl) | `client-portal-…/server.js:110-116` |
| Weak `JWT_SECRET` fallback | `client-portal-…/server.js:13` |
| DB TLS cert validation disabled | all `server.js` / `scripts/*.js` pools → `rejectUnauthorized: false` |
| CSP disabled | `w-…/website/server.js:551` |
| CORS deny → 500 | `w-…/website/server.js:506` / `540` |
| CORS allowlist (localhost + RFC1918) | `w-…/website/server.js:500-504` |
| Per-IP auth rate limit (50/min default) | `w-…/website/backend/middleware/authEndpointValidator.js:80-89` |
| Encrypted prefs + plaintext fallback | `app/src/main/java/com/greggory/portal/data/local/PreferencesManager.kt:9-25` |
| Unsigned updater, no checksum | `app/src/main/java/com/greggory/portal/utils/UpdateManager.kt:25,48,74` |
| FCM token logged | `app/src/main/java/com/greggory/portal/utils/FirebaseMessagingService.kt:19` |
| No Log-stripping rules | `app/proguard-rules.pro` (absent `-assumenosideeffects`) |
| No cert pinning | `app/src/main/java/com/greggory/portal/data/api/RetrofitClient.kt` (no `CertificatePinner`) |
| Release build hardening OK | `app/build.gradle:64-78` |
| Debug cleartext restricted to emulator | `app/src/main/res/xml/network_security_config.xml` |

---

*Findings are based on source state and live probes as of 14 Oct 2026. Re-run Appendix A
probes after remediation to confirm: the API-1 fix should turn probe #3 → **403**.*






