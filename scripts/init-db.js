const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

async function initDb() {
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

    const connection = await mysql.createConnection(dbConfig);

    console.log('Connected to Aiven defaultdb. Creating essential tables...');

    // 1. users table
    await connection.query(`
        CREATE TABLE IF NOT EXISTS users (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            email VARCHAR(255) NOT NULL UNIQUE,
            password_hash VARCHAR(255) DEFAULT NULL,
            first_name VARCHAR(100) NOT NULL,
            last_name VARCHAR(100) NOT NULL,
            display_name VARCHAR(200) DEFAULT NULL,
            phone_number VARCHAR(50),
            primary_role VARCHAR(50) DEFAULT 'user',
            profile_photo_blob LONGBLOB NULL DEFAULT NULL,
            profile_photo_mime_type VARCHAR(100) NULL DEFAULT NULL,
            profile_photo_file_name VARCHAR(255) NULL DEFAULT NULL,
            mission_briefing TEXT DEFAULT NULL,
            whatsapp_verified BOOLEAN DEFAULT FALSE,
            is_active BOOLEAN DEFAULT TRUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    `);
    console.log('✓ Table `users` verified/created.');

    // 2. user_projects table
    await connection.query(`
        CREATE TABLE IF NOT EXISTS user_projects (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            project_name VARCHAR(255) NOT NULL,
            status VARCHAR(50) DEFAULT 'active',
            progress_percentage INT DEFAULT 0,
            priority VARCHAR(50) DEFAULT 'Medium',
            manager_name VARCHAR(100) DEFAULT 'Team Lead',
            end_date DATE DEFAULT NULL,
            estimated_budget DECIMAL(15,2) DEFAULT 0.00,
            actual_budget DECIMAL(15,2) DEFAULT 0.00,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    `);
    console.log('✓ Table `user_projects` verified/created.');

    // 3. notifications table
    await connection.query(`
        CREATE TABLE IF NOT EXISTS notifications (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            title VARCHAR(255) NOT NULL,
            message TEXT NOT NULL,
            priority VARCHAR(50) DEFAULT 'normal',
            status VARCHAR(50) DEFAULT 'unread',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    `);
    console.log('✓ Table `notifications` verified/created.');

    // 4. project_reports table
    await connection.query(`
        CREATE TABLE IF NOT EXISTS project_reports (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            project_name VARCHAR(255) NOT NULL,
            title VARCHAR(255) NOT NULL,
            summary TEXT NOT NULL,
            file_type VARCHAR(50) DEFAULT 'pdf',
            file_size BIGINT DEFAULT 0,
            report_date DATE DEFAULT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    `);
    console.log('✓ Table `project_reports` verified/clean.');

    // 5. invoices table
    await connection.query(`
        CREATE TABLE IF NOT EXISTS invoices (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            user_id BIGINT NOT NULL,
            invoice_number VARCHAR(100) NOT NULL,
            project_name VARCHAR(255) NOT NULL,
            amount DECIMAL(15,2) NOT NULL,
            status VARCHAR(50) DEFAULT 'pending',
            due_date DATE DEFAULT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    `);
    console.log('✓ Table `invoices` verified/created.');

    console.log('SUCCESS: All core tables initialized successfully in Aiven defaultdb!');
    await connection.end();
}

initDb().catch(err => {
    console.error('Initialization error:', err);
    process.exit(1);
});
