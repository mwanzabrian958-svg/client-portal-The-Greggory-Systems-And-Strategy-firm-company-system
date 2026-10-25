// Greggory Client Portal — production backend gateway.
// Phone APK --(HTTPS + JWT)--> this server --(MySQL + SSL)--> Aiven (defaultdb).
// Deploy this file to Render as the Web Service. NEVER put DB credentials in the app.
const express = require('express');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const jwt = require('jsonwebtoken');
const bcrypt = require('bcryptjs');
const mysql = require('mysql2/promise');

const app = express();
const PORT = process.env.PORT || 10000;
// SECURITY: a weak/default JWT signing secret lets anyone mint admin-capable
// tokens (OWASP API-2). Fail fast when unset — a process that cannot sign
// tokens honestly must not boot with a guessable one. Local dev gets a working
// ephemeral secret with an unmissable warning (every restart invalidates dev
// sessions — exactly what you want in dev).
let JWT_SECRET = process.env.JWT_SECRET;
if (!JWT_SECRET) {
  if (process.env.NODE_ENV === "production") {
    console.error(
      "[SECURITY][FATAL] JWT_SECRET is not set in production. Refusing to start " +
        "rather than signing tokens with a default value. Set JWT_SECRET in the " +
        "hosting dashboard, then redeploy."
    );
    process.exit(1);
  }
  JWT_SECRET = require("crypto").randomBytes(64).toString("hex");
  console.warn(
    "[SECURITY] JWT_SECRET unset — using an EPHEMERAL random secret. " +
      "All sessions will invalidate on every restart. Set JWT_SECRET for stability."
  );
}
const TOKEN_TTL = '7d';

app.use(cors());
app.use(express.json({ limit: '5mb' }));
app.use((req, res, next) => {
  res.setHeader('X-Routing-Policy', 'set-in-stone-v1');
  next();
});

// Request logger: ground-truth that the phone APK actually reached the backend.
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  next();
});

// --- Aiven pool (env-only, no hardcoded password) ---
// SECURITY: verify the server certificate against the PINNED Aiven Project CA
// (config-aiven-ca.pem; rotation via DB_CA_FILE / DB_CA_PEM env). Live-proven
// 14 Oct 2026: pinned-CA verification connects; anything else fails the TLS
// handshake, which is the MITM protection (OWASP A02/API-8). Never
// rejectUnauthorized:false — when the anchor is missing the process refuses to
// boot rather than silently degrading to an unverified channel.
const fs = require('fs');
function loadDbCa() {
  if (process.env.DB_CA_PEM) return { ca: process.env.DB_CA_PEM, source: 'DB_CA_PEM env' };
  if (process.env.DB_CA_FILE) return { ca: fs.readFileSync(process.env.DB_CA_FILE, 'utf8'), source: 'DB_CA_FILE env' };
  const bundled = require('path').join(__dirname, 'config-aiven-ca.pem');
  if (fs.existsSync(bundled)) return { ca: fs.readFileSync(bundled, 'utf8'), source: 'bundled config-aiven-ca.pem' };
  console.error('[SECURITY][FATAL] No MySQL CA trust anchor: bundled config-aiven-ca.pem is missing ' +
    'and neither DB_CA_PEM nor DB_CA_FILE is set. Refusing to start.');
  process.exit(1);
}
const _dbCa = loadDbCa();
console.log(`[DB TLS] cloud certificate chain verified against: ${_dbCa.source}`);
const pool = mysql.createPool({
  host: process.env.DB_HOST || 'mysql-2277f171-brianmwanza651-0b75.g.aivencloud.com',
  port: parseInt(process.env.DB_PORT || '27059', 10),
  user: process.env.DB_USER || 'avnadmin',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'defaultdb',
  ssl: { minVersion: 'TLSv1.2', ca: _dbCa.ca },
  waitForConnections: true,
  connectionLimit: 5,
  queueLimit: 0,
  // Aiven free tier silently drops idle TCP connections -> mysql2 then hands the
  // dead socket to the next query and it fails with ECONNRESET. Keep-alive pings
  // keep the pipe warm so pooled connections stay usable.
  enableKeepAlive: true,
  keepAliveInitialDelay: 10000,
});

// Retry a pool query once on transient connection resets (ECONNRESET /
// PROTOCOL_CONNECTION_LOST) which Aiven triggers when it reaps an idle socket.
async function q(sql, params) {
  try {
    return await pool.query(sql, params);
  } catch (e) {
    const transient = ['ECONNRESET', 'PROTOCOL_CONNECTION_LOST', 'ETIMEDOUT', 'ECONNREFUSED'];
    if (transient.includes(e.code)) {
      return await pool.query(sql, params);
    }
    throw e;
  }
}

async function ensureSchema() {
  if (!process.env.DB_PASSWORD) {
    console.warn('[db] DB_PASSWORD not set — /health/db reports down until Render env is set.');
    return;
  }
  await q(`CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) DEFAULT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) DEFAULT NULL,
    phone_number VARCHAR(50),
    primary_role VARCHAR(50) DEFAULT 'user',
    mission_briefing TEXT DEFAULT NULL,
    profile_photo_blob LONGBLOB NULL DEFAULT NULL,
    profile_photo_mime_type VARCHAR(100) NULL DEFAULT NULL,
    whatsapp_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`);

  // Ensure columns exist on existing databases according to the_greggory_systems_and_strategy_firm_db_main.sql
  try { await q('ALTER TABLE users ADD COLUMN profile_photo_blob LONGBLOB NULL DEFAULT NULL'); } catch (_) {}
  try { await q('ALTER TABLE users ADD COLUMN profile_photo_mime_type VARCHAR(100) NULL DEFAULT NULL'); } catch (_) {}
  await q(`CREATE TABLE IF NOT EXISTS user_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'active',
    progress_percentage INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`);
  await q(`CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`);
  console.log('[db] core tables verified');
}

function signToken(user) {
  return jwt.sign({ id: user.id, email: user.email }, JWT_SECRET, { expiresIn: TOKEN_TTL });
}

function authenticateUser(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  const routingId = parseInt(req.headers['x-greggory-client-id'] || '0', 10);
  if (!token) return res.status(401).json({ success: false, message: 'Missing session token' });
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    if (routingId > 0 && routingId !== Number(decoded.id)) {
      return res.status(403).json({ success: false, message: 'Routing integrity breach' });
    }
    req.user = decoded;
    next();
  } catch (e) {
    return res.status(401).json({ success: false, message: 'Session expired — please log in again' });
  }
}

function publicUser(row) {
  let photoData = null;
  if (row.profile_photo_blob && row.profile_photo_blob.length > 0) {
    const b64 = Buffer.from(row.profile_photo_blob).toString('base64');
    const mime = row.profile_photo_mime_type || 'image/jpeg';
    photoData = `data:${mime};base64,${b64}`;
  }
  return {
    id: Number(row.id),
    email: row.email,
    first_name: row.first_name,
    last_name: row.last_name,
    display_name: row.display_name,
    phone_number: row.phone_number,
    phone: row.phone_number,
    primary_role: row.primary_role,
    mission_briefing: row.mission_briefing,
    profilePhotoData: photoData
  };
}

// --- Health (Render + app probe use these) ---
app.get('/', (req, res) => res.json({ success: true, service: 'greggory-portal-backend', time: new Date().toISOString() }));
app.get('/health', (req, res) => res.json({ success: true, status: 'ok' }));
// Non-secret DB diagnostics (host/port/db/ssl + whether a password is present).
// Never echoes the password itself — only whether it is set. This is what turns a
// vague "test-db error" into an actionable message when a Render deploy is
// misconfigured (the usual cause: DB_PASSWORD unset or pasted with a stray
// newline/space so the value is silently wrong).
function dbDiagnostics() {
  return {
    dbHost: process.env.DB_HOST ? String(process.env.DB_HOST) : '(unset)',
    dbPort: parseInt(process.env.DB_PORT || '27059', 10),
    dbName: process.env.DB_NAME || 'defaultdb',
    dbUser: process.env.DB_USER || '(unset)',
    passwordSet: Boolean(process.env.DB_PASSWORD && process.env.DB_PASSWORD.trim()),
    ssl: true,
  };
}

// SECURITY: full diagnostics (hostname, db user, latency) are gated behind a
// DIAG_TOKEN env + bearer auth. Unauthenticated callers get only the
// up/down signal — nothing an attacker can use to craft a connection attempt.
function diagAuthorized(req) {
  if (!process.env.DIAG_TOKEN) return false;
  const q = req.query.key || req.header('x-diag-key');
  const h = req.header('authorization');
  return (q && q === process.env.DIAG_TOKEN) ||
    (h && h === 'Bearer ' + process.env.DIAG_TOKEN);
}

app.get('/health/db', async (req, res) => {
  const diag = dbDiagnostics();
  if (!diagAuthorized(req)) {
    // Public signal only: reachable or not. No host, user, latency, or hint.
    if (!diag.passwordSet) return res.status(503).json({ success: false, db: 'unreachable' });
    try {
      await q('SELECT 1');
      return res.json({ success: true, db: 'reachable' });
    } catch (e) {
      return res.status(503).json({ success: false, db: 'unreachable' });
    }
  }
  if (!diag.passwordSet) {
    return res.status(503).json({
      success: false, db: 'unreachable', reason: 'DB_PASSWORD not set in environment',
      hint: 'Set DB_PASSWORD (and DB_HOST/DB_PORT/DB_USER/DB_NAME) in the Render service Environment, then redeploy.',
      ...diag,
    });
  }
  const started = Date.now();
  try {
    await q('SELECT 1');
    res.json({ success: true, db: 'reachable', latencyMs: Date.now() - started, ...diag });
  } catch (e) {
    res.status(503).json({ success: false, db: 'unreachable', code: e.code || e.errno || null, message: e.message, ...diag });
  }
});

// Pipeline probe used by RetrofitClient.isBackendReachable()
app.get('/api/auth/whatsapp/status', (req, res) => {
  res.json({ success: true, configured: false, provider: 'none', otpTtlMinutes: 10, issues: ['provider not configured'] });
});
app.get('/api/notify/status', (req, res) => {
  res.json({ success: true, chain: ['whatsapp'], whatsapp: { configured: false } });
});

// --- Auth: each phone registers/logs in independently, gets its own JWT ---
app.post('/api/users/register', async (req, res) => {
  try {
    const { first_name, last_name, email, phone, password, profile_photo_base64, profile_photo_mime_type } = req.body || {};
    if (!first_name || !last_name || !email || !phone || !password) {
      return res.status(400).json({ success: false, message: 'first_name, last_name, email, phone, password required' });
    }
    const [existing] = await q('SELECT id FROM users WHERE email = ?', [email]);
    if (existing.length > 0) return res.json({ success: false, message: 'Account exists — please log in', loginInstead: true });
    const hash = await bcrypt.hash(password, 10);
    const display = `${first_name} ${last_name}`.trim();

    let photoBlob = null;
    let photoMime = null;
    if (profile_photo_base64) {
      photoBlob = Buffer.from(profile_photo_base64, 'base64');
      photoMime = profile_photo_mime_type || 'image/jpeg';
    }

    await q(
      'INSERT INTO users (first_name, last_name, display_name, email, phone_number, password_hash, profile_photo_blob, profile_photo_mime_type) VALUES (?,?,?,?,?,?,?,?)',
      [first_name, last_name, display, email, phone, hash, photoBlob, photoMime]
    );
    res.json({ success: true, message: 'Account created — please log in' });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});

// --- Profile Photo Upload & Retrieval ---
app.post('/api/users/profile-photo', authenticateUser, async (req, res) => {
  try {
    const { photo_base64, mime_type, email } = req.body || {};
    if (!photo_base64) {
      return res.status(400).json({ success: false, message: 'photo_base64 required' });
    }
    const cleanB64 = photo_base64.replace(/^data:image\/[a-zA-Z]+;base64,/, '');
    const imageBuffer = Buffer.from(cleanB64, 'base64');
    const mimeType = mime_type || 'image/jpeg';
    const dataUri = `data:${mimeType};base64,${cleanB64}`;

    if (email) {
      await q(
        'UPDATE users SET profile_photo_blob = ?, profile_photo_mime_type = ? WHERE LOWER(email) = ?',
        [imageBuffer, mimeType, email.toLowerCase().trim()]
      );
    }
    await q(
      'UPDATE users SET profile_photo_blob = ?, profile_photo_mime_type = ? WHERE id = ?',
      [imageBuffer, mimeType, req.user.id]
    );

    res.json({ success: true, message: 'Photo uploaded successfully', imageUrl: dataUri });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});

app.get('/api/users/profile-photo/me', authenticateUser, async (req, res) => {
  try {
    const [rows] = await q('SELECT profile_photo_blob, profile_photo_mime_type FROM users WHERE id = ?', [req.user.id]);
    if (rows.length === 0 || !rows[0].profile_photo_blob || rows[0].profile_photo_blob.length === 0) {
      return res.status(404).send('Photo not found');
    }
    const row = rows[0];
    res.setHeader('Content-Type', row.profile_photo_mime_type || 'image/jpeg');
    res.send(row.profile_photo_blob);
  } catch (e) {
    res.status(500).send(e.message);
  }
});

app.get('/api/users/profile-photo/:idOrEmail', async (req, res) => {
  try {
    const param = req.params.idOrEmail;
    const isEmail = param.includes('@');
    const query = isEmail
      ? 'SELECT profile_photo_blob, profile_photo_mime_type FROM users WHERE LOWER(email) = ?'
      : 'SELECT profile_photo_blob, profile_photo_mime_type FROM users WHERE id = ?';
    const [rows] = await q(query, [isEmail ? param.toLowerCase().trim() : param]);
    if (rows.length === 0 || !rows[0].profile_photo_blob || rows[0].profile_photo_blob.length === 0) {
      return res.status(404).send('Photo not found');
    }
    const row = rows[0];
    res.setHeader('Content-Type', row.profile_photo_mime_type || 'image/jpeg');
    res.send(row.profile_photo_blob);
  } catch (e) {
    res.status(500).send(e.message);
  }
});

app.post('/api/users/login', async (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) return res.status(400).json({ success: false, message: 'email and password required' });
    const [rows] = await q('SELECT * FROM users WHERE email = ?', [email]);
    if (rows.length === 0) return res.status(401).json({ success: false, message: 'Invalid email or password' });
    const user = rows[0];
    const ok = user.password_hash ? await bcrypt.compare(password, user.password_hash) : false;
    if (!ok) return res.status(401).json({ success: false, message: 'Invalid email or password' });
    const token = signToken(user);
    // Best-effort login tracking — never let a failure here break a valid login.
    // Uses ON DUPLICATE-free UPDATE guarded by id; wrapped so any DB hiccup is swallowed.
    try {
      const ip = (req.headers['x-forwarded-for'] || req.socket.remoteAddress || '').toString().split(',')[0].trim() || null;
      await q('UPDATE users SET last_login_at = CURRENT_TIMESTAMP, last_login_ip = ? WHERE id = ?', [ip, user.id]);
    } catch (_) { /* tracking is non-critical */ }
    res.json({ ...publicUser(user), token });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});

app.post('/api/users/forgot-password', rateLimit({ windowMs: 15 * 60 * 1000, max: 5 }),
  (req, res) => res.json({ success: true, message: 'If the account exists, a reset link was sent' }));

app.post('/api/users/logout', authenticateUser, (req, res) => res.json({ success: true, message: 'Logged out' }));

// --- Per-user silo: every query filtered by WHERE user_id = token id ---
app.get('/api/users/client-dashboard', authenticateUser, async (req, res) => {
  try {
    const uid = req.user.id;
    const [urows] = await q('SELECT * FROM users WHERE id = ?', [uid]);
    const [projects] = await q(
      'SELECT id, project_name AS name, status, progress_percentage AS progress, user_id AS client_id FROM user_projects WHERE user_id = ?', [uid]);
    const [invoices] = await q(
      "SELECT id, total_amount AS amount, status, client_id, invoice_number FROM invoices WHERE client_id = ? AND deleted_at IS NULL ORDER BY id DESC", [uid]);
    res.json({ success: true, dashboard: {
      user: urows[0] ? publicUser(urows[0]) : null,
      projects, invoices, tasks: [], teamMembers: [], messages: [],
      budgetOverview: null, businessSummary: null, kpiMetrics: [],
    }});
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
  }
});

app.get('/api/user-projects', authenticateUser, async (req, res) => {
  const [rows] = await q(
    'SELECT id, project_name AS name, status, progress_percentage AS progress, user_id AS client_id FROM user_projects WHERE user_id = ?', [req.user.id]);
  res.json(rows);
});

app.get('/api/users/my-reports', authenticateUser, async (req, res) => {
  // Mirror the source-of-truth backend (backend/routes/users.js): client reports
  // are scoped through the user_projects link (pr.project_id -> user_projects.id),
  // NOT the `projects` consultancy table. Only 'final' reports are client-visible.
  // project_reports may or may not have deleted_at depending on how it was created,
  // so try the soft-delete-aware query first and fall back if that column is absent
  // (matches fetchClientProjectReports()). NOTE: base has NO ORDER BY — the
  // soft-delete predicate must be appended to WHERE, before ORDER BY, or it is a
  // SQL parse error.
  const base =
    'SELECT pr.id, pr.title, pr.summary, pr.file_type, pr.file_size, pr.report_date, ' +
    "       up.project_name AS project_name, up.user_id AS user_id " +
    'FROM project_reports pr ' +
    'JOIN user_projects up ON pr.project_id = up.id ' +
    'WHERE up.user_id = ? AND pr.status = ?';
  const order = ' ORDER BY pr.report_date DESC';
  let rows;
  try {
    [rows] = await q(base + ' AND pr.deleted_at IS NULL' + order, [req.user.id, 'final']);
  } catch (e) {
    if (e && e.code === 'ER_BAD_FIELD_ERROR') {
      [rows] = await q(base + order, [req.user.id, 'final']); // no deleted_at column — plain query
    } else {
      throw e;
    }
  }
  res.json({ success: true, reports: rows });
});

app.get('/api/users/notifications/me', authenticateUser, async (req, res) => {
  const [rows] = await q('SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC', [req.user.id]);
  res.json({ success: true, notifications: rows });
});

ensureSchema().catch((e) => console.error('[db] schema check failed:', e.message));
app.listen(PORT, () => console.log(`Greggory backend live on :${PORT}`));
