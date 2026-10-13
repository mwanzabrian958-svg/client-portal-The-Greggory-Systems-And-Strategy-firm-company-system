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
const JWT_SECRET = process.env.JWT_SECRET || 'change-me-in-render-env';
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
const pool = mysql.createPool({
  host: process.env.DB_HOST || 'mysql-2277f171-brianmwanza651-0b75.g.aivencloud.com',
  port: parseInt(process.env.DB_PORT || '27059', 10),
  user: process.env.DB_USER || 'avnadmin',
  password: process.env.DB_PASSWORD || '',
  database: process.env.DB_NAME || 'defaultdb',
  ssl: { rejectUnauthorized: false },
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
    whatsapp_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`);
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
  };
}

// --- Health (Render + app probe use these) ---
app.get('/', (req, res) => res.json({ success: true, service: 'greggory-portal-backend', time: new Date().toISOString() }));
app.get('/health', (req, res) => res.json({ success: true, status: 'ok' }));
app.get('/health/db', async (req, res) => {
  try {
    await q('SELECT 1');
    res.json({ success: true, db: 'reachable' });
  } catch (e) {
    res.status(503).json({ success: false, db: 'unreachable', message: e.message });
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
    const { first_name, last_name, email, phone, password } = req.body || {};
    if (!first_name || !last_name || !email || !phone || !password) {
      return res.status(400).json({ success: false, message: 'first_name, last_name, email, phone, password required' });
    }
    const [existing] = await q('SELECT id FROM users WHERE email = ?', [email]);
    if (existing.length > 0) return res.json({ success: false, message: 'Account exists — please log in', loginInstead: true });
    const hash = await bcrypt.hash(password, 10);
    const display = `${first_name} ${last_name}`.trim();
    await q(
      'INSERT INTO users (first_name, last_name, display_name, email, phone_number, password_hash) VALUES (?,?,?,?,?,?)',
      [first_name, last_name, display, email, phone, hash]
    );
    res.json({ success: true, message: 'Account created — please log in' });
  } catch (e) {
    res.status(500).json({ success: false, message: e.message });
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
      'SELECT id, amount, status, user_id AS client_id FROM invoices WHERE user_id = ?', [uid]);
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
  const [rows] = await q('SELECT * FROM project_reports WHERE user_id = ?', [req.user.id]);
  res.json({ success: true, reports: rows });
});

app.get('/api/users/notifications/me', authenticateUser, async (req, res) => {
  const [rows] = await q('SELECT * FROM notifications WHERE user_id = ? ORDER BY id DESC', [req.user.id]);
  res.json({ success: true, notifications: rows });
});

ensureSchema().catch((e) => console.error('[db] schema check failed:', e.message));
app.listen(PORT, () => console.log(`Greggory backend live on :${PORT}`));
