const http = require('https');

const BASE_URL = 'https://w-the-greggory-systems-and-strategy-firm-1vf9.onrender.com';

const endpoints = [
    { name: 'WhatsApp Auth Status', method: 'GET', path: '/api/auth/whatsapp/status' },
    { name: 'Request WhatsApp OTP', method: 'POST', path: '/api/auth/whatsapp/request-code', body: { phone: '+254712345678' } },
    { name: 'User Login', method: 'POST', path: '/api/users/login', body: { email: 'test@greggory.com', password: 'wrong' } },
    { name: 'User Register', method: 'POST', path: '/api/users/register', body: { first_name: 'Test', last_name: 'User', email: 'test@greggory.com', phone: '0712345678', password: 'pass' } },
    { name: 'Client Dashboard', method: 'GET', path: '/api/users/client-dashboard' },
    { name: 'User Projects Ledger', method: 'GET', path: '/api/user-projects' },
    { name: 'My Reports', method: 'GET', path: '/api/users/my-reports' },
    { name: 'FCM Register Token', method: 'POST', path: '/api/fcm/register-token', body: { fcmToken: 'test_token' } },
    { name: 'Global Search', method: 'GET', path: '/api/users/search?q=test' },
    { name: 'Report Payment', method: 'POST', path: '/api/mpesa/report-payment', body: { invoiceId: '1', mpesaMessage: 'test' } },
    { name: 'Client Feedback', method: 'GET', path: '/api/users/client-feedback' },
    { name: 'My Quotes', method: 'GET', path: '/api/users/my-quotes' },
    { name: 'Signature Requests', method: 'GET', path: '/api/users/my-signature-requests' },
    { name: 'Change Requests', method: 'GET', path: '/api/users/my-change-requests' },
    { name: 'Chat History', method: 'GET', path: '/api/chat/history' }
];

async function testEndpoint(ep) {
    return new Promise((resolve) => {
        const url = new URL(ep.path, BASE_URL);
        const data = ep.body ? JSON.stringify(ep.body) : null;

        const options = {
            hostname: url.hostname,
            port: 443,
            path: url.pathname + url.search,
            method: ep.method,
            headers: {
                'User-Agent': 'GreggoryClientPortalTest/1.0',
                'X-Routing-Policy': 'set-in-stone-v1',
                ...(data ? {
                    'Content-Type': 'application/json',
                    'Content-Length': Buffer.byteLength(data)
                } : {})
            },
            timeout: 10000
        };

        const req = http.request(options, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                resolve({
                    ...ep,
                    status: res.statusCode,
                    success: res.statusCode === 200 || res.statusCode === 400 || res.statusCode === 401 || res.statusCode === 403
                });
            });
        });

        req.on('error', (e) => {
            resolve({
                ...ep,
                status: 'ERR',
                success: false
            });
        });

        req.on('timeout', () => {
            req.destroy();
            resolve({
                ...ep,
                status: 'TIMEOUT',
                success: false
            });
        });

        if (data) req.write(data);
        req.end();
    });
}

async function run() {
    console.log(`Testing API Endpoints against ${BASE_URL}...\n`);
    const results = [];
    for (const ep of endpoints) {
        const res = await testEndpoint(ep);
        results.push(res);
    }

    console.log('| Endpoint Name | Method | Path | Status Code | Result |');
    console.log('|---|---|---|---|---|');
    for (const r of results) {
        let resultIcon = '❌ (404/Error)';
        if (r.status === 200) {
            resultIcon = '✅ 200 (Working)';
        } else if (r.status === 400 || r.status === 401 || r.status === 403) {
            resultIcon = `🟢 ${r.status} (Reachable)`;
        } else if (r.status === 404) {
            resultIcon = '❌ 404 (Failing)';
        } else {
            resultIcon = `❌ ${r.status} (Failing)`;
        }
        console.log(`| ${r.name} | ${r.method} | ${r.path} | ${r.status} | ${resultIcon} |`);
    }
}

run();
