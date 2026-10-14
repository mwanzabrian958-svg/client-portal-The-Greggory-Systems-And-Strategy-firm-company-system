const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

async function importDatabase() {
    const dbConfig = {
        host: process.env.DB_HOST || 'mysql-2277f171-brianmwanza651-0b75.g.aivencloud.com',
        port: parseInt(process.env.DB_PORT || '27059', 10),
        user: process.env.DB_USER || 'avnadmin',
        password: process.env.DB_PASSWORD || '',
        database: process.env.DB_NAME || 'defaultdb',
        // SECURITY: verify the chain against the PINNED Aiven Project CA
        // (../config-aiven-ca.pem; override via DB_CA_FILE / DB_CA_PEM).
        ssl: (function () {
            if (process.env.DB_CA_PEM) return { minVersion: 'TLSv1.2', ca: process.env.DB_CA_PEM };
            if (process.env.DB_CA_FILE) return { minVersion: 'TLSv1.2', ca: fs.readFileSync(process.env.DB_CA_FILE, 'utf8') };
            return { minVersion: 'TLSv1.2', ca: fs.readFileSync(path.join(__dirname, '..', 'config-aiven-ca.pem'), 'utf8') };
        })(),
        multipleStatements: true
    };

    if (!dbConfig.password) {
        console.error('ERROR: DB_PASSWORD environment variable is required.');
        process.exit(1);
    }

    console.log('Connecting to Aiven MySQL database...');
    let connection;
    try {
        connection = await mysql.createConnection(dbConfig);
        console.log('Connected successfully to Aiven database (defaultdb)!');

        const sqlFilePath = path.join(__dirname, '..', 'the_greggory_systems_and_strategy_firm_db_main.sql');
        console.log(`Reading SQL schema file from ${sqlFilePath}...`);

        if (!fs.existsSync(sqlFilePath)) {
            console.error('SQL file not found at:', sqlFilePath);
            process.exit(1);
        }

        let sqlContent = fs.readFileSync(sqlFilePath, 'utf8');
        sqlContent = sqlContent.replace(/USE\s+[^;]+;/gi, 'USE defaultdb;');

        console.log('Executing master schema statements on defaultdb...');
        await connection.query(sqlContent);
        console.log('SUCCESS: Master database schema imported successfully!');

    } catch (err) {
        console.error('ERROR during database import:', err);
        process.exit(1);
    } finally {
        if (connection) {
            await connection.end();
        }
    }
}

importDatabase();
