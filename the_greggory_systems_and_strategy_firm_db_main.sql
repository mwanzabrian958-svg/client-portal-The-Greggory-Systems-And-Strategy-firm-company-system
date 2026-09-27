-- =====================================================
-- MASTER DATABASE SCHEMA: the_greggory_systems_and_strategy_firm_db_main
-- =====================================================

-- FILE: AUTH_ENDPOINTS_SCHEMA.sql
-- =====================================================
-- AUTH PLATFORM ENDPOINTS - LOCKED MAPPING TABLE
-- =====================================================
-- This table locks in the platform-to-table associations
-- NO changes allowed after initialization
-- Database: the_greggory_systems_and_strategy_firm_db_main
-- =====================================================

USE the_greggory_systems_and_strategy_firm_db_main;

-- =============================================
-- Table: auth_platform_mapping
-- Locks platform → table → endpoint associations
-- =============================================
CREATE TABLE IF NOT EXISTS auth_platform_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_name VARCHAR(50) NOT NULL UNIQUE,
    table_name VARCHAR(100) NOT NULL UNIQUE,
    register_endpoint VARCHAR(255) NOT NULL,
    login_endpoint VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    locked_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by VARCHAR(100) DEFAULT 'SYSTEM',
    INDEX idx_platform_name (platform_name),
    INDEX idx_table_name (table_name),
    INDEX idx_is_locked (is_locked),
    CONSTRAINT check_platform_name CHECK (platform_name IN ('user', 'admin', 'developer')),
    CONSTRAINT check_table_name CHECK (table_name IN ('users', 'admin_users', 'developer_users'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert locked platform mappings
DELETE FROM auth_platform_mapping; -- Clear any existing entries

INSERT INTO auth_platform_mapping (
    platform_name, 
    table_name, 
    register_endpoint, 
    login_endpoint, 
    description, 
    is_active, 
    is_locked,
    locked_by
) VALUES 
(
    'user',
    'users',
    'POST /api/users/register',
    'POST /api/users/login',
    'Regular user authentication - public user accounts, donors, beneficiaries',
    TRUE,
    TRUE,
    'SYSTEM'
),
(
    'admin',
    'admin_users',
    'POST /api/admin/create (admin-create via users.js)',
    'POST /api/admin-verification/authenticate-enhanced',
    'Administrative staff - super admins, admins, moderators',
    TRUE,
    TRUE,
    'SYSTEM'
),
(
    'developer',
    'developer_users',
    'POST /api/admin/developer-create (admin-create via users.js)',
    'POST /api/developer-verification/authenticate',
    'Development team - senior, mid, junior, lead level developers',
    TRUE,
    TRUE,
    'SYSTEM'
);

-- =============================================
-- Table: auth_request_log
-- Logs all authentication requests for audit
-- =============================================
CREATE TABLE IF NOT EXISTS auth_request_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(100) NOT NULL UNIQUE,
    platform VARCHAR(50) NOT NULL,
    table_name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    ip_address VARCHAR(45),
    request_method VARCHAR(10),
    request_body_hash VARCHAR(64),
    response_status INT,
    response_message VARCHAR(255),
    error_message VARCHAR(500),
    execution_time_ms INT,
    is_success BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_platform (platform),
    INDEX idx_table_name (table_name),
    INDEX idx_endpoint (endpoint),
    INDEX idx_email (email),
    INDEX idx_created_at (created_at),
    INDEX idx_is_success (is_success)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: auth_validation_rules
-- Defines strict validation rules per platform
-- =============================================
CREATE TABLE IF NOT EXISTS auth_validation_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform VARCHAR(50) NOT NULL,
    rule_name VARCHAR(100) NOT NULL,
    rule_type ENUM('required_field', 'table_isolation', 'cross_check', 'password_policy', 'rate_limit') DEFAULT 'required_field',
    rule_value VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    enforcement_level ENUM('strict', 'warning', 'info') DEFAULT 'strict',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_platform_rule (platform, rule_name),
    INDEX idx_platform (platform),
    INDEX idx_rule_type (rule_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert validation rules
DELETE FROM auth_validation_rules;

INSERT INTO auth_validation_rules (
    platform,
    rule_name,
    rule_type,
    rule_value,
    description,
    enforcement_level
) VALUES
-- User platform rules
('user', 'email_required', 'required_field', 'email', 'Email field is mandatory for user registration', 'strict'),
('user', 'password_required', 'required_field', 'password', 'Password field is mandatory for user registration', 'strict'),
('user', 'first_name_required', 'required_field', 'first_name', 'First name field is mandatory for user registration', 'strict'),
('user', 'last_name_required', 'required_field', 'last_name', 'Last name field is mandatory for user registration', 'strict'),
('user', 'only_users_table', 'table_isolation', 'users', 'User auth MUST ONLY reference users table', 'strict'),
('user', 'no_admin_check', 'cross_check', 'admin_users', 'NEVER check admin_users table in user auth flow', 'strict'),
('user', 'no_developer_check', 'cross_check', 'developer_users', 'NEVER check developer_users table in user auth flow', 'strict'),
('user', 'password_min_length', 'password_policy', '8', 'Password must be minimum 8 characters', 'strict'),
-- Admin platform rules
('admin', 'email_required', 'required_field', 'email', 'Email field is mandatory for admin registration', 'strict'),
('admin', 'password_required', 'required_field', 'password', 'Password field is mandatory for admin registration', 'strict'),
('admin', 'first_name_required', 'required_field', 'first_name', 'First name field is mandatory for admin registration', 'strict'),
('admin', 'last_name_required', 'required_field', 'last_name', 'Last name field is mandatory for admin registration', 'strict'),
('admin', 'role_required', 'required_field', 'role', 'Role field is mandatory for admin registration', 'strict'),
('admin', 'only_admin_users_table', 'table_isolation', 'admin_users', 'Admin auth MUST ONLY reference admin_users table', 'strict'),
('admin', 'no_users_check', 'cross_check', 'users', 'NEVER check users table in admin auth flow', 'strict'),
('admin', 'no_developer_check', 'cross_check', 'developer_users', 'NEVER check developer_users table in admin auth flow', 'strict'),
('admin', 'password_min_length', 'password_policy', '8', 'Password must be minimum 8 characters', 'strict'),
-- Developer platform rules
('developer', 'email_required', 'required_field', 'email', 'Email field is mandatory for developer registration', 'strict'),
('developer', 'password_required', 'required_field', 'password', 'Password field is mandatory for developer registration', 'strict'),
('developer', 'first_name_required', 'required_field', 'first_name', 'First name field is mandatory for developer registration', 'strict'),
('developer', 'last_name_required', 'required_field', 'last_name', 'Last name field is mandatory for developer registration', 'strict'),
('developer', 'role_required', 'required_field', 'role', 'Role field is mandatory for developer registration', 'strict'),
('developer', 'only_developer_users_table', 'table_isolation', 'developer_users', 'Developer auth MUST ONLY reference developer_users table', 'strict'),
('developer', 'no_users_check', 'cross_check', 'users', 'NEVER check users table in developer auth flow', 'strict'),
('developer', 'no_admin_check', 'cross_check', 'admin_users', 'NEVER check admin_users table in developer auth flow', 'strict'),
('developer', 'password_min_length', 'password_policy', '8', 'Password must be minimum 8 characters', 'strict');

-- =============================================
-- VIEWS for Auth Platform Monitoring
-- =============================================

-- View: Active Auth Platforms
CREATE OR REPLACE VIEW v_active_auth_platforms AS
SELECT 
    platform_name,
    table_name,
    register_endpoint,
    login_endpoint,
    description,
    is_active,
    is_locked,
    locked_at,
    locked_by
FROM auth_platform_mapping
WHERE is_active = TRUE AND is_locked = TRUE
ORDER BY platform_name;

-- View: Auth Request Success Rate
CREATE OR REPLACE VIEW v_auth_request_stats AS
SELECT 
    platform,
    table_name,
    endpoint,
    COUNT(*) as total_requests,
    SUM(CASE WHEN is_success = TRUE THEN 1 ELSE 0 END) as successful_requests,
    SUM(CASE WHEN is_success = FALSE THEN 1 ELSE 0 END) as failed_requests,
    ROUND(
        (SUM(CASE WHEN is_success = TRUE THEN 1 ELSE 0 END) / COUNT(*)) * 100, 2
    ) as success_rate_percentage,
    ROUND(AVG(execution_time_ms), 2) as avg_execution_time_ms,
    MIN(created_at) as first_request_at,
    MAX(created_at) as last_request_at
FROM auth_request_log
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY platform, table_name, endpoint
ORDER BY platform, endpoint;

-- =============================================
-- Sample Audit Query
-- =============================================
-- SELECT 
--     platform_name,
--     table_name,
--     register_endpoint,
--     login_endpoint,
--     is_locked,
--     locked_at,
--     locked_by
-- FROM auth_platform_mapping
-- WHERE is_locked = TRUE;


-- FILE: check_users_table.sql
-- Check the structure of the users table
SHOW CREATE TABLE users;

-- Check what columns exist in the users table
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_KEY, EXTRA
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'the_greggory_systems_and_strategy_firm_db' 
AND TABLE_NAME = 'users';

-- View existing users (if any)
SELECT * FROM users LIMIT 5;

-- Example INSERT statement (update column names as needed based on the actual table structure)
-- INSERT INTO users (email, password_hash, name, role, is_active, created_at)
-- VALUES (
--   'test@example.com',
--   'hashed_password_here',  -- You'll need to generate this
--   'Test User',
--   'admin',
--   1,
--   NOW()
-- );

-- To generate a password hash in MySQL:
-- SELECT MD5('your_password_here');  -- For MD5 hash (not recommended for production)
-- OR using SHA2 (better):
-- SELECT SHA2('your_password_here', 256);


-- FILE: add_role_column_to_users.sql
-- ============================================================
-- Migration: Add 'role' column to users table
-- Reason: server.js fallback queries at L2666/L2967 try
--   SELECT id FROM users WHERE role = 'admin' before falling
--   back to primary_role. The 'role' column didn't exist,
--   causing ER_BAD_FIELD_ERROR on every accounting/finance
--   write that needs a created_by admin user.
--   This ALTER is idempotent: IF NOT EXISTS avoids errors
--   when the column is already present.
-- ============================================================

ALTER TABLE users 
  ADD COLUMN IF NOT EXISTS `role` VARCHAR(50) DEFAULT 'user'
    AFTER `primary_role`;


-- FILE: fix-missing-brian-mwanza.sql
-- =====================================================
-- FIX: Insert Brian Mwanza into company_personnel table
-- This script fixes the "Person not found" error on the About page
-- =====================================================

USE the_greggory_systems_and_strategy_firm_db_main;

-- Insert Brian Mwanza if he doesn't already exist
INSERT INTO company_personnel (name, position, bio, image_url, sort_order, is_active)
SELECT 'Brian Mwanza', 'Founder & Managing Director',
 '<p>Brian Mwanza is the visionary force behind The-Greggory-Systems-And-Strategy-firm. With over a decade of experience in systemic design and business strategy, he has guided some of the most ambitious organizations through complex digital and operational transformations.</p><p>His philosophy is rooted in the belief that &quot;Strategy is not a document; it''s a pulse.&quot; Under his leadership, the firm has evolved from a boutique advisory to a global architect of business resonance, known for its uncompromising commitment to clarity and human-centric systems.</p>',
 '/images/brian-mwanza-ceo.jpg',
 0, TRUE
WHERE NOT EXISTS (SELECT 1 FROM company_personnel WHERE name = 'Brian Mwanza');

-- Verify the record exists
SELECT id, name, position, is_active FROM company_personnel WHERE name = 'Brian Mwanza';


-- FILE: portal-sync-schema.sql
-- ── PORTAL SYNC SCHEMA ──
-- Ensures all tables required for the Client Portal are present and synchronized with the backend.

USE the_greggory_systems_and_strategy_firm_db_main;

-- 1. User Projects (Main Engagement Nodes)
CREATE TABLE IF NOT EXISTS user_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    project_description TEXT,
    project_type VARCHAR(100) DEFAULT 'consulting',
    status ENUM('planning', 'in-progress', 'completed', 'on-hold', 'cancelled') DEFAULT 'planning',
    priority ENUM('Low', 'Medium', 'High', 'Critical') DEFAULT 'Medium',
    progress_percentage INT DEFAULT 0,
    start_date DATE,
    end_date DATE,
    estimated_budget DECIMAL(15,2) DEFAULT 0.00,
    actual_budget DECIMAL(15,2) DEFAULT 0.00,
    project_manager_id BIGINT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 2. Project Tasks (Execution Board)
CREATE TABLE IF NOT EXISTS project_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    task_description TEXT,
    assigned_to BIGINT,
    status ENUM('planned', 'in-progress', 'blocked', 'completed') DEFAULT 'planned',
    priority ENUM('Low', 'Medium', 'High', 'Critical') DEFAULT 'Medium',
    due_date DATETIME,
    progress_percentage INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Project Activities (Live Data Relay)
CREATE TABLE IF NOT EXISTS project_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(100) DEFAULT 'update',
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Project Invoices (Financial Ledger)
CREATE TABLE IF NOT EXISTS project_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    invoice_number VARCHAR(100) UNIQUE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('draft', 'pending', 'paid', 'overdue', 'cancelled') DEFAULT 'draft',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. Project Documents (The Vault)
CREATE TABLE IF NOT EXISTS project_docs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(100) DEFAULT 'General',
    file_path VARCHAR(512),
    file_size BIGINT DEFAULT 0,
    version VARCHAR(20) DEFAULT 'v1.0',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. User Feedback (Satisfaction Relays)
CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_id BIGINT DEFAULT NULL,
    title VARCHAR(255),
    message TEXT NOT NULL,
    feedback_type VARCHAR(100) DEFAULT 'general',
    rating INT DEFAULT 5,
    status ENUM('new', 'reviewed', 'responded', 'closed') DEFAULT 'new',
    priority ENUM('Low', 'Medium', 'High', 'Urgent') DEFAULT 'Medium',
    source VARCHAR(100) DEFAULT 'portal',
    contact_name VARCHAR(255) DEFAULT NULL,
    contact_email VARCHAR(255) DEFAULT NULL,
    contact_phone VARCHAR(50) DEFAULT NULL,
    admin_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 7. Notifications (System Relays)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(100) DEFAULT 'system',
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status ENUM('unread', 'read', 'archived') DEFAULT 'unread',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. Client Project Summary (Aggregated Telemetry)
CREATE TABLE IF NOT EXISTS client_project_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_projects INT DEFAULT 0,
    active_projects INT DEFAULT 0,
    completed_projects INT DEFAULT 0,
    total_budget DECIMAL(15,2) DEFAULT 0.00,
    total_spent DECIMAL(15,2) DEFAULT 0.00,
    client_rating DECIMAL(3,2) DEFAULT 5.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 9. Project Reports (The PDF Tables)
CREATE TABLE IF NOT EXISTS project_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    file_data LONGBLOB,
    file_type VARCHAR(100) DEFAULT 'application/pdf',
    file_size BIGINT DEFAULT 0,
    report_date DATE NOT NULL,
    status ENUM('draft', 'review', 'final') DEFAULT 'final',
    admin_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB;


-- FILE: the-greggory-systems-and-strategy-firm-db-main.sql
-- =====================================================
-- Complete Database Schema for The-Greggory-Systems-And-Strategy-firm
-- Database Name: the_greggory_systems_and_strategy_firm_db_main
-- =====================================================

-- Drop and create database
DROP DATABASE IF EXISTS the_greggory_systems_and_strategy_firm_db_main;
CREATE DATABASE the_greggory_systems_and_strategy_firm_db_main CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE the_greggory_systems_and_strategy_firm_db_main;

-- Enable strict mode
SET SQL_MODE = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- =====================================================
-- SECTION 1: BASE TABLES (No Foreign Key Dependencies)
-- =====================================================

-- =============================================
-- Table: images
-- Centralized image storage - MUST BE FIRST
-- =============================================
CREATE TABLE IF NOT EXISTS images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512),
    file_type VARCHAR(100),
    file_size BIGINT,
    content_type VARCHAR(100),
    data LONGBLOB,
    alt_text VARCHAR(255),
    title VARCHAR(255),
    width INT,
    height INT,
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_images_filename (file_name),
    INDEX idx_images_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: roles
-- User roles and permissions
-- =============================================
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT NULL,
    is_system_role BOOLEAN DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_roles_name (name),
    INDEX idx_roles_system (is_system_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default roles (ONCE - not duplicated)
INSERT INTO roles (id, name, description, is_system_role) VALUES 
(1, 'admin', 'Administrator account', 1),
(2, 'user', 'Regular user account', 1),
(3, 'developer', 'Developer account', 1);

-- =============================================
-- Table: team_members
-- Job/role definitions for users - REQUIRED by backend
-- =============================================
CREATE TABLE IF NOT EXISTS team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_team_members_name (name),
    INDEX idx_team_members_role (role),
    INDEX idx_team_members_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default team member roles
INSERT INTO team_members (name, role, department) VALUES
('Project Manager', 'manager', 'Projects'),
('Site Supervisor', 'supervisor', 'Operations'),
('Engineer', 'engineer', 'Technical'),
('Consultant', 'consultant', 'Consulting'),
('Field Worker', 'field_worker', 'Operations'),
('Administrator', 'admin', 'Administration'),
('Developer', 'developer', 'Technology');

-- =============================================
-- Table: website_content
-- =============================================
CREATE TABLE IF NOT EXISTS website_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_key VARCHAR(100) NOT NULL UNIQUE,
    content_value LONGTEXT,
    content_type ENUM('text', 'html', 'json', 'image_url') DEFAULT 'text',
    section VARCHAR(100),
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default website content
INSERT INTO website_content (content_key, content_value, content_type, section, description) VALUES
('hero_title', 'THE-GREGGORY-SYSTEMS-AND-STRATEGY-FIRM', 'text', 'hero', 'Main landing page title'),
('hero_motto', 'Strategic Project Development for all clients', 'text', 'hero', 'Main landing page motto'),
('intro_title', 'Empowering Your Success Through Comprehensive Solutions', 'text', 'intro', 'Introduction section title'),
('intro_description', 'At The-Greggory-Systems-And-Strategy-firm, we believe that every business challenge-from systems design to strategic planning-can be solved with excellence.', 'text', 'intro', 'Introduction section description');

-- =============================================
-- Table: users
-- Regular user accounts
-- =============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    whatsapp_verified BOOLEAN DEFAULT FALSE,
    whatsapp_auth_key VARCHAR(10) DEFAULT NULL,
    email_verification_token VARCHAR(255) DEFAULT NULL,
    email_verification_expires DATETIME DEFAULT NULL,
    password_hash VARCHAR(255) DEFAULT NULL,
    password_reset_token VARCHAR(255) DEFAULT NULL,
    password_reset_expires DATETIME DEFAULT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) DEFAULT NULL,
    phone_number VARCHAR(50),
    profile_photo_id BIGINT DEFAULT NULL,
    profile_photo_blob LONGBLOB NULL DEFAULT NULL,
    profile_photo_mime_type VARCHAR(100) NULL DEFAULT NULL,
    profile_photo_file_name VARCHAR(255) NULL DEFAULT NULL,
    job_id BIGINT DEFAULT NULL,
    primary_role VARCHAR(50) DEFAULT 'user',
    role VARCHAR(50) DEFAULT 'user',
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    last_login_ip VARCHAR(45) DEFAULT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (profile_photo_id) REFERENCES images(id) ON DELETE SET NULL,
    FOREIGN KEY (job_id) REFERENCES team_members(id) ON DELETE SET NULL,
    INDEX idx_users_email (email),
    INDEX idx_users_active (is_active, deleted_at),
    INDEX idx_users_name (first_name, last_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: admin_users
-- Admin authentication (per AUTH_PROTOCOL.md)
-- =============================================
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    whatsapp_verified BOOLEAN DEFAULT FALSE,
    whatsapp_auth_key VARCHAR(10) DEFAULT NULL,
    email_verification_token VARCHAR(255) DEFAULT NULL,
    email_verification_expires DATETIME DEFAULT NULL,
    password_hash VARCHAR(255) DEFAULT NULL,
    password_reset_token VARCHAR(255) DEFAULT NULL,
    password_reset_expires DATETIME DEFAULT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) DEFAULT NULL,
    phone_number VARCHAR(50),
    profile_photo_id BIGINT DEFAULT NULL,
    profile_photo_blob LONGBLOB NULL DEFAULT NULL,
    profile_photo_mime_type VARCHAR(100) NULL DEFAULT NULL,
    profile_photo_file_name VARCHAR(255) NULL DEFAULT NULL,
    profile_image_id BIGINT DEFAULT NULL,
    admin_level ENUM('super_admin', 'admin', 'moderator') DEFAULT 'admin',
    admin_permissions JSON,
    access_level ENUM('full', 'limited', 'read_only') DEFAULT 'full',
    department VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    last_login_ip VARCHAR(45) DEFAULT NULL,
    failed_login_attempts INT DEFAULT 0,
    account_locked_until TIMESTAMP NULL DEFAULT NULL,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255) DEFAULT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (profile_photo_id) REFERENCES images(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    INDEX idx_admin_users_email (email),
    INDEX idx_admin_users_active (is_active, deleted_at),
    INDEX idx_admin_users_level (admin_level),
    INDEX idx_admin_users_access (access_level),
    INDEX idx_admin_users_department (department),
    INDEX idx_admin_users_login (last_login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: developer_users
-- Developer authentication (per AUTH_PROTOCOL.md)
-- =============================================
CREATE TABLE IF NOT EXISTS developer_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    whatsapp_verified BOOLEAN DEFAULT FALSE,
    whatsapp_auth_key VARCHAR(10) DEFAULT NULL,
    email_verification_token VARCHAR(255) DEFAULT NULL,
    email_verification_expires DATETIME DEFAULT NULL,
    password_hash VARCHAR(255) DEFAULT NULL,
    password_reset_token VARCHAR(255) DEFAULT NULL,
    password_reset_expires DATETIME DEFAULT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200) DEFAULT NULL,
    phone_number VARCHAR(50),
    profile_photo_id BIGINT DEFAULT NULL,
    profile_photo_blob LONGBLOB NULL DEFAULT NULL,
    profile_photo_mime_type VARCHAR(100) NULL DEFAULT NULL,
    profile_photo_file_name VARCHAR(255) NULL DEFAULT NULL,
    profile_image_id BIGINT DEFAULT NULL,
    developer_level ENUM('senior', 'mid', 'junior', 'lead') DEFAULT 'mid',
    tech_stack JSON,
    specialization VARCHAR(100),
    access_level ENUM('full', 'limited', 'read_only') DEFAULT 'limited',
    team_id BIGINT,
    github_username VARCHAR(100),
    linkedin_url VARCHAR(512),
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    last_login_ip VARCHAR(45) DEFAULT NULL,
    failed_login_attempts INT DEFAULT 0,
    account_locked_until TIMESTAMP NULL DEFAULT NULL,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255) DEFAULT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    locale VARCHAR(10) DEFAULT 'en-US',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (profile_photo_id) REFERENCES images(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    INDEX idx_developer_users_email (email),
    INDEX idx_developer_users_active (is_active, deleted_at),
    INDEX idx_developer_users_level (developer_level),
    INDEX idx_developer_users_stack (specialization),
    INDEX idx_developer_users_team (team_id),
    INDEX idx_developer_users_login (last_login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: companies
-- =============================================
CREATE TABLE IF NOT EXISTS companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    logo_id BIGINT,
    website_url VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (logo_id) REFERENCES images(id) ON DELETE SET NULL,
    INDEX idx_companies_slug (slug),
    INDEX idx_companies_active (is_active, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 2: USER-RELATED TABLES (Reference users)
-- =====================================================

-- =============================================
-- Table: user_roles
-- =============================================
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_user_role (user_id, role_id),
    INDEX idx_user_roles_user (user_id),
    INDEX idx_user_roles_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: contact_forms
-- =============================================
CREATE TABLE IF NOT EXISTS contact_forms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    company VARCHAR(255),
    subject VARCHAR(255),
    message LONGTEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contact_forms_email (email, created_at),
    INDEX idx_contact_forms_read (is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: user_projects
-- =============================================
CREATE TABLE IF NOT EXISTS user_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    project_description LONGTEXT,
    project_type ENUM('consulting', 'development', 'design', 'marketing', 'management', 'other') DEFAULT 'consulting',
    status ENUM('planning', 'in_progress', 'completed', 'on_hold', 'cancelled') DEFAULT 'planning',
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    start_date DATE,
    end_date DATE,
    estimated_budget DECIMAL(12,2),
    actual_budget DECIMAL(12,2),
    client_id BIGINT,
    client_name VARCHAR(255),
    client_email VARCHAR(255),
    client_phone VARCHAR(50),
    project_manager_id BIGINT,
    team_members JSON,
    deliverables JSON,
    milestones JSON,
    documents JSON,
    progress_percentage INT DEFAULT 0,
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_manager_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_user_projects_user (user_id, status),
    INDEX idx_user_projects_client_id (client_id),
    INDEX idx_user_projects_client (client_name),
    INDEX idx_user_projects_status (status, priority),
    INDEX idx_user_projects_dates (start_date, end_date),
    INDEX idx_user_projects_active (is_active, deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_tasks (user_projects version)
-- =============================================
CREATE TABLE IF NOT EXISTS project_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    task_description TEXT,
    assigned_to BIGINT,
    status ENUM('not_started', 'in_progress', 'completed', 'blocked', 'cancelled') DEFAULT 'not_started',
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    due_date DATETIME,
    completed_at TIMESTAMP NULL DEFAULT NULL,
    estimated_hours DECIMAL(5,2),
    actual_hours DECIMAL(5,2),
    dependencies JSON,
    attachments JSON,
    comments TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_project_tasks_project (project_id, status),
    INDEX idx_project_tasks_assigned (assigned_to, status),
    INDEX idx_project_tasks_due (due_date, status),
    INDEX idx_project_tasks_priority (priority, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_documents (user_projects version)
-- =============================================
CREATE TABLE IF NOT EXISTS project_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_type ENUM('contract', 'proposal', 'report', 'invoice', 'image', 'video', 'document', 'other') DEFAULT 'document',
    file_path VARCHAR(512),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_by BIGINT,
    is_public BOOLEAN DEFAULT FALSE,
    download_count INT DEFAULT 0,
    last_downloaded_at TIMESTAMP NULL DEFAULT NULL,
    description TEXT,
    tags JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (project_id) REFERENCES user_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_project_documents_project (project_id, document_type),
    INDEX idx_project_documents_public (is_public, document_type),
    INDEX idx_project_documents_uploaded (uploaded_by, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_project_summary
-- =============================================
CREATE TABLE IF NOT EXISTS client_project_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_projects INT DEFAULT 0,
    active_projects INT DEFAULT 0,
    completed_projects INT DEFAULT 0,
    total_budget DECIMAL(15,2) DEFAULT 0,
    total_spent DECIMAL(15,2) DEFAULT 0,
    average_project_duration INT DEFAULT 0,
    last_project_date TIMESTAMP NULL DEFAULT NULL,
    client_rating DECIMAL(3,2) DEFAULT 0,
    notes TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_client_summary_user (user_id),
    INDEX idx_client_summary_active (active_projects),
    INDEX idx_client_summary_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: user_feedback
-- =============================================
CREATE TABLE IF NOT EXISTS user_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NULL,
    user_id BIGINT,
    feedback_type ENUM('project_review', 'service_feedback', 'complaint', 'suggestion', 'testimonial', 'bug_report') DEFAULT 'project_review',
    rating INT CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    message TEXT NOT NULL,
    status ENUM('new', 'reviewed', 'responded', 'resolved', 'closed') DEFAULT 'new',
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    admin_response TEXT,
    responded_by BIGINT NULL,
    responded_at TIMESTAMP NULL,
    contact_name VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(20),
    source ENUM('website', 'email', 'phone', 'in_person', 'social_media') DEFAULT 'website',
    ip_address VARCHAR(45),
    user_agent TEXT,
    attachment_url VARCHAR(512),
    attachment_type VARCHAR(100),
    internal_notes TEXT,
    assigned_to BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (responded_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_feedback_user (user_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_feedback_status (status),
    INDEX idx_feedback_priority (priority),
    INDEX idx_feedback_rating (rating),
    INDEX idx_feedback_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: mpesa_transactions
-- =============================================
CREATE TABLE IF NOT EXISTS mpesa_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NULL,
    project_id BIGINT NULL,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    merchant_request_id VARCHAR(100),
    checkout_request_id VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'KES',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    amount_kes DECIMAL(15,2) GENERATED ALWAYS AS (amount * exchange_rate) STORED,
    phone_number VARCHAR(20) NOT NULL,
    status ENUM('pending', 'completed', 'failed', 'cancelled', 'reversed') DEFAULT 'pending',
    result_code INT,
    result_desc VARCHAR(255),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completion_time TIMESTAMP NULL,
    response_data JSON,
    payment_method ENUM('paybill', 'till_number', 'buy_goods') DEFAULT 'paybill',
    business_number VARCHAR(20) DEFAULT '174379',
    account_reference VARCHAR(255),
    client_id BIGINT,
    client_name VARCHAR(255),
    client_email VARCHAR(255),
    reconciled BOOLEAN DEFAULT FALSE,
    reconciled_at TIMESTAMP NULL,
    reconciled_by BIGINT NULL,
    reconciliation_notes TEXT,
    is_refund BOOLEAN DEFAULT FALSE,
    original_transaction_id VARCHAR(100),
    refund_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (reconciled_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_mpesa_transaction_id (transaction_id),
    INDEX idx_mpesa_status (status),
    INDEX idx_mpesa_phone (phone_number),
    INDEX idx_mpesa_client (client_id),
    INDEX idx_mpesa_date (transaction_date),
    INDEX idx_mpesa_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 3: ADMIN/CONTENT TABLES
-- =====================================================

-- =============================================
-- Table: blog_articles
-- =============================================
CREATE TABLE IF NOT EXISTS blog_articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    excerpt TEXT,
    content LONGTEXT NOT NULL,
    author VARCHAR(100),
    read_time VARCHAR(50),
    category VARCHAR(100),
    image_url VARCHAR(512),
    image_blob LONGBLOB,
    image_mime_type VARCHAR(100),
    image_file_name VARCHAR(255),
    image_id BIGINT,
    icon_class VARCHAR(100),
    is_published BOOLEAN DEFAULT FALSE,
    published_date TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (image_id) REFERENCES images(id) ON DELETE SET NULL,
    INDEX idx_blog_articles_published (is_published, published_date),
    INDEX idx_blog_articles_category (category),
    INDEX idx_blog_articles_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: blog_subscriptions
-- =============================================
CREATE TABLE IF NOT EXISTS blog_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    status ENUM('active', 'unsubscribed', 'pending') DEFAULT 'active',
    source VARCHAR(100) DEFAULT 'website_blog',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_blog_subs_email (email),
    INDEX idx_blog_subs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: case_studies
-- =============================================
CREATE TABLE IF NOT EXISTS case_studies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    client VARCHAR(255),
    industry VARCHAR(255),
    challenge TEXT,
    solution LONGTEXT,
    results LONGTEXT,
    duration VARCHAR(100),
    image_url VARCHAR(512),
    image_blob LONGBLOB,
    image_mime_type VARCHAR(100),
    image_file_name VARCHAR(255),
    image_urls JSON,
    is_featured BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_case_studies_featured (is_featured),
    INDEX idx_case_studies_industry (industry),
    INDEX idx_case_studies_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: videos
-- =============================================
CREATE TABLE IF NOT EXISTS videos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    video_blob LONGBLOB NULL DEFAULT NULL,
    video_mime_type VARCHAR(100) NULL DEFAULT NULL,
    video_file_name VARCHAR(255) NULL DEFAULT NULL,
    video_size BIGINT NULL DEFAULT NULL,
    thumbnail_blob LONGBLOB NULL DEFAULT NULL,
    thumbnail_mime_type VARCHAR(100) NULL DEFAULT NULL,
    thumbnail_file_name VARCHAR(255) NULL DEFAULT NULL,
    video_url VARCHAR(512),
    thumbnail_url VARCHAR(512),
    is_active BOOLEAN DEFAULT TRUE,
    is_featured BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_videos_active (is_active, display_order),
    INDEX idx_videos_featured (is_featured, display_order),
    INDEX idx_videos_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: admin_navbar_items
-- =============================================
CREATE TABLE IF NOT EXISTS admin_navbar_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_name VARCHAR(255) NOT NULL,
    item_type ENUM('link', 'dropdown', 'button', 'separator') DEFAULT 'link',
    display_text VARCHAR(255) NOT NULL,
    url VARCHAR(512),
    icon_class VARCHAR(100),
    parent_id BIGINT NULL DEFAULT NULL,
    sort_order INT DEFAULT 0,
    is_visible BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    target_blank BOOLEAN DEFAULT FALSE,
    css_class VARCHAR(255),
    requires_auth BOOLEAN DEFAULT FALSE,
    required_role VARCHAR(50),
    mobile_only BOOLEAN DEFAULT FALSE,
    desktop_only BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    FOREIGN KEY (parent_id) REFERENCES admin_navbar_items(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_admin_navbar_parent (parent_id, sort_order),
    INDEX idx_admin_navbar_visible (is_visible, is_active, sort_order),
    INDEX idx_admin_navbar_type (item_type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: admin_website_settings
-- =============================================
CREATE TABLE IF NOT EXISTS admin_website_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value LONGTEXT,
    setting_type ENUM('text', 'textarea', 'number', 'boolean', 'json', 'file') DEFAULT 'text',
    display_name VARCHAR(255),
    description TEXT,
    category VARCHAR(100) DEFAULT 'general',
    is_public BOOLEAN DEFAULT FALSE,
    sort_order INT DEFAULT 0,
    validation_rules JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_admin_settings_key (setting_key),
    INDEX idx_admin_settings_category (category, sort_order),
    INDEX idx_admin_settings_public (is_public, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: admin_activity_logs
-- =============================================
CREATE TABLE IF NOT EXISTS admin_activity_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    action_description TEXT,
    affected_table VARCHAR(100),
    affected_record_id BIGINT,
    old_values JSON,
    new_values JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_admin_activity_admin (admin_user_id, created_at),
    INDEX idx_admin_activity_action (action_type, created_at),
    INDEX idx_admin_activity_table (affected_table, created_at),
    INDEX idx_admin_activity_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 4: PROJECT MANAGEMENT TABLES
-- =====================================================

-- =============================================
-- Table: projects (main project management)
-- =============================================
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('active', 'completed', 'pending', 'on_hold', 'cancelled') DEFAULT 'active',
    progress INT DEFAULT 0 CHECK (progress >= 0 AND progress <= 100),
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    start_date DATE NOT NULL,
    expected_completion DATE NOT NULL,
    actual_completion DATE NULL,
    client_id BIGINT,
    client_name VARCHAR(255) NOT NULL,
    client_contact VARCHAR(255),
    location VARCHAR(500),
    project_type VARCHAR(100),
    budget DECIMAL(12,2) DEFAULT 0.00,
    spent DECIMAL(12,2) DEFAULT 0.00,
    remaining DECIMAL(12,2) GENERATED ALWAYS AS (budget - spent) STORED,
    currency VARCHAR(3) DEFAULT 'USD',
    project_manager_id BIGINT,
    team_lead_id BIGINT,
    team_size INT DEFAULT 0,
    main_photo_data LONGBLOB NULL,
    main_photo_name VARCHAR(255) NULL,
    main_photo_type VARCHAR(100) NULL,
    main_photo_size BIGINT NULL,
    cover_photo_data LONGBLOB NULL,
    cover_photo_name VARCHAR(255) NULL,
    cover_photo_type VARCHAR(100) NULL,
    cover_photo_size BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_projects_status (status),
    INDEX idx_projects_client_id (client_id),
    INDEX idx_projects_client (client_name),
    INDEX idx_projects_manager (project_manager_id),
    INDEX idx_projects_dates (start_date, expected_completion),
    INDEX idx_projects_created (created_at),
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_manager_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (team_lead_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_team_members
-- =============================================
CREATE TABLE IF NOT EXISTS project_team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(100) NOT NULL DEFAULT 'team_member',
    duties TEXT,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT NOT NULL,
    removed_at TIMESTAMP NULL DEFAULT NULL,
    removed_by BIGINT DEFAULT NULL,
    INDEX idx_project_team_project (project_id),
    INDEX idx_project_team_user (user_id),
    INDEX idx_project_team_role (role),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (removed_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY unique_project_user (project_id, user_id, removed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_photos
-- =============================================
CREATE TABLE IF NOT EXISTS project_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    photo_data LONGBLOB NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    photo_type ENUM('main', 'cover', 'progress', 'team', 'site', 'completion', 'screenshot', 'document') DEFAULT 'progress',
    title VARCHAR(255),
    description TEXT,
    display_order INT DEFAULT 0,
    is_featured BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    INDEX idx_project_photos_project (project_id),
    INDEX idx_project_photos_type (photo_type),
    INDEX idx_project_photos_featured (is_featured),
    INDEX idx_project_photos_order (display_order),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_activities
-- =============================================
CREATE TABLE IF NOT EXISTS project_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    activity_type ENUM('update', 'milestone', 'alert', 'note', 'status_change', 'team_change', 'photo_added', 'document_uploaded') DEFAULT 'update',
    message TEXT NOT NULL,
    details JSON NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    INDEX idx_project_activities_project (project_id),
    INDEX idx_project_activities_user (user_id),
    INDEX idx_project_activities_type (activity_type),
    INDEX idx_project_activities_created (created_at),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_expenses
-- =============================================
CREATE TABLE IF NOT EXISTS project_expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    expense_date DATE NOT NULL,
    vendor VARCHAR(255),
    receipt_number VARCHAR(100),
    receipt_image_id BIGINT NULL,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    INDEX idx_project_expenses_project (project_id),
    INDEX idx_project_expenses_category (category),
    INDEX idx_project_expenses_date (expense_date),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (receipt_image_id) REFERENCES images(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_invoices
-- =============================================
CREATE TABLE IF NOT EXISTS project_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    client_id BIGINT,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    client_name VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    amount_kes DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    currency_code VARCHAR(3) NOT NULL DEFAULT 'KES',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status ENUM('draft', 'sent', 'paid', 'overdue', 'cancelled') DEFAULT 'draft',
    description TEXT,
    terms TEXT,
    notes TEXT,
    paid_amount DECIMAL(12,2) DEFAULT 0.00,
    paid_amount_kes DECIMAL(12,2) DEFAULT 0.00,
    paid_date DATE NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_project_invoices_project (project_id),
    INDEX idx_project_invoices_client (client_id),
    INDEX idx_project_invoices_number (invoice_number),
    INDEX idx_project_invoices_status (status),
    INDEX idx_project_invoices_dates (issue_date, due_date),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_documents (projects version)
-- =============================================
CREATE TABLE IF NOT EXISTS project_docs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    category VARCHAR(100) DEFAULT 'general',
    file_path VARCHAR(512) NOT NULL,
    file_data LONGBLOB NULL,
    description TEXT,
    version VARCHAR(20) DEFAULT '1.0',
    is_public BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_project_docs_project (project_id),
    INDEX idx_project_docs_category (category),
    INDEX idx_project_docs_type (file_type),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_reports
-- =============================================
CREATE TABLE IF NOT EXISTS project_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    report_type ENUM('progress', 'financial', 'inspection', 'milestone', 'final', 'custom') DEFAULT 'progress',
    content LONGTEXT,
    summary TEXT,
    file_path VARCHAR(512),
    file_data LONGBLOB,
    file_type VARCHAR(50),
    file_size BIGINT,
    report_date DATE NOT NULL,
    period_start DATE,
    period_end DATE,
    download_count INT DEFAULT 0,
    generated_by BIGINT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    template_version VARCHAR(20) DEFAULT '1.0',
    status ENUM('draft', 'final', 'archived') DEFAULT 'draft',
    is_public BOOLEAN DEFAULT TRUE,
    export_format ENUM('pdf', 'excel', 'csv', 'json') DEFAULT 'pdf',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_project_reports_project (project_id),
    INDEX idx_project_reports_type (report_type),
    INDEX idx_project_reports_date (report_date),
    INDEX idx_project_reports_status (status),
    INDEX idx_project_reports_generated (generated_at),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 5: FINANCIAL TABLES
-- =====================================================

-- =============================================
-- Table: accounting_entries
-- =============================================
CREATE TABLE IF NOT EXISTS accounting_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    entry_type ENUM('income', 'expense', 'budget_allocation', 'budget_adjustment', 'invoice_payment', 'refund') NOT NULL,
    category VARCHAR(100) NOT NULL,
    subcategory VARCHAR(100),
    amount DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) GENERATED ALWAYS AS (amount + tax_amount) STORED,
    currency VARCHAR(3) DEFAULT 'USD',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    amount_usd DECIMAL(15,2) GENERATED ALWAYS AS (total_amount * exchange_rate) STORED,
    transaction_date DATE NOT NULL,
    transaction_reference VARCHAR(255),
    payment_method ENUM('cash', 'bank_transfer', 'credit_card', 'debit_card', 'check', 'online_payment', 'other') DEFAULT 'bank_transfer',
    payment_status ENUM('pending', 'completed', 'failed', 'refunded', 'partially_refunded') DEFAULT 'completed',
    description TEXT NOT NULL,
    notes TEXT,
    internal_notes TEXT,
    invoice_id BIGINT NULL,
    receipt_id BIGINT NULL,
    contract_id BIGINT NULL,
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    approval_status ENUM('pending', 'approved', 'rejected', 'needs_revision') DEFAULT 'approved',
    rejection_reason TEXT,
    budget_category VARCHAR(100),
    budget_period VARCHAR(50),
    is_billable BOOLEAN DEFAULT TRUE,
    billable_percentage DECIMAL(5,2) DEFAULT 100.00,
    tax_rate DECIMAL(5,4) DEFAULT 0.0000,
    tax_exempt BOOLEAN DEFAULT FALSE,
    tax_region VARCHAR(100),
    reconciled BOOLEAN DEFAULT FALSE,
    reconciled_by BIGINT NULL,
    reconciled_at TIMESTAMP NULL,
    reconciliation_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_accounting_project (project_id),
    INDEX idx_accounting_type (entry_type),
    INDEX idx_accounting_category (category),
    INDEX idx_accounting_date (transaction_date),
    INDEX idx_accounting_status (payment_status),
    INDEX idx_accounting_approval (approval_status),
    INDEX idx_accounting_reconciled (reconciled),
    INDEX idx_accounting_created (created_at),
    INDEX idx_accounting_budget_period (budget_period),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (reconciled_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: accounting_categories
-- =============================================
CREATE TABLE IF NOT EXISTS accounting_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category_type ENUM('income', 'expense', 'both') DEFAULT 'expense',
    default_budget_percentage DECIMAL(5,2) DEFAULT 0.00,
    is_tax_deductible BOOLEAN DEFAULT FALSE,
    requires_approval BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0,
    color_code VARCHAR(7) DEFAULT '#000000',
    icon VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_accounting_categories_type (category_type),
    INDEX idx_accounting_categories_active (is_active),
    INDEX idx_accounting_categories_order (display_order),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: accounting_periods
-- =============================================
CREATE TABLE IF NOT EXISTS accounting_periods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    period_name VARCHAR(100) NOT NULL,
    period_type ENUM('monthly', 'quarterly', 'yearly', 'custom') DEFAULT 'monthly',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_budget DECIMAL(15,2) DEFAULT 0.00,
    allocated_budget DECIMAL(15,2) DEFAULT 0.00,
    spent_budget DECIMAL(15,2) DEFAULT 0.00,
    remaining_budget DECIMAL(15,2) GENERATED ALWAYS AS (allocated_budget - spent_budget) STORED,
    status ENUM('planning', 'active', 'closed', 'archived') DEFAULT 'planning',
    locked BOOLEAN DEFAULT FALSE,
    description TEXT,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_accounting_periods_project (project_id),
    INDEX idx_accounting_periods_dates (start_date, end_date),
    INDEX idx_accounting_periods_status (status),
    INDEX idx_accounting_periods_type (period_type),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: financial_reports
-- =============================================
CREATE TABLE IF NOT EXISTS financial_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    report_name VARCHAR(255) NOT NULL,
    report_type ENUM('profit_loss', 'balance_sheet', 'cash_flow', 'budget_variance', 'expense_breakdown', 'income_statement', 'custom') DEFAULT 'profit_loss',
    report_data LONGTEXT,
    summary TEXT,
    insights TEXT,
    report_date DATE NOT NULL,
    period_start DATE,
    period_end DATE,
    generated_by BIGINT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    template_version VARCHAR(20) DEFAULT '1.0',
    status ENUM('draft', 'final', 'archived') DEFAULT 'draft',
    is_public BOOLEAN DEFAULT TRUE,
    shared_with JSON,
    download_count INT DEFAULT 0,
    last_downloaded_at TIMESTAMP NULL,
    export_format ENUM('pdf', 'excel', 'csv', 'json') DEFAULT 'pdf',
    file_path VARCHAR(512),
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_financial_reports_project (project_id),
    INDEX idx_financial_reports_type (report_type),
    INDEX idx_financial_reports_date (report_date),
    INDEX idx_financial_reports_status (status),
    INDEX idx_financial_reports_generated (generated_at),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (generated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: invoices
-- =============================================
CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    client_id BIGINT,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    invoice_type ENUM('project_fee', 'milestone', 'expense', 'retainer', 'custom') DEFAULT 'project_fee',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subtotal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    tax_rate DECIMAL(5,4) DEFAULT 0.0000,
    tax_amount DECIMAL(15,2) GENERATED ALWAYS AS (subtotal * tax_rate) STORED,
    total_amount DECIMAL(15,2) GENERATED ALWAYS AS (subtotal + tax_amount) STORED,
    currency VARCHAR(3) DEFAULT 'KES',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    total_amount_kes DECIMAL(15,2) GENERATED ALWAYS AS (total_amount * exchange_rate) STORED,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE NULL,
    status ENUM('draft', 'sent', 'viewed', 'partial', 'paid', 'overdue', 'cancelled') DEFAULT 'draft',
    payment_status ENUM('pending', 'partial', 'paid', 'failed') DEFAULT 'pending',
    payment_method ENUM('mpesa', 'bank_transfer', 'cash', 'check', 'online_payment', 'other') DEFAULT 'mpesa',
    payment_phone VARCHAR(20) DEFAULT '+254799789956',
    payment_reference VARCHAR(255),
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255),
    client_phone VARCHAR(20),
    client_address TEXT,
    items JSON,
    notes TEXT,
    payment_terms TEXT,
    terms_conditions TEXT,
    pdf_file_path VARCHAR(512),
    pdf_generated BOOLEAN DEFAULT FALSE,
    pdf_generated_at TIMESTAMP NULL,
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_at TIMESTAMP NULL,
    email_opened BOOLEAN DEFAULT FALSE,
    email_opened_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_invoices_project (project_id),
    INDEX idx_invoices_client (client_id),
    INDEX idx_invoices_number (invoice_number),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_payment_status (payment_status),
    INDEX idx_invoices_dates (issue_date, due_date),
    INDEX idx_invoices_created (created_at),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: quotes
-- =============================================
CREATE TABLE IF NOT EXISTS quotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NULL,
    client_id BIGINT NULL,
    quote_number VARCHAR(50) NOT NULL UNIQUE,
    quote_type ENUM('project_estimate', 'service_quote', 'product_quote', 'consultation', 'custom') DEFAULT 'project_estimate',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subtotal DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    tax_rate DECIMAL(5,4) DEFAULT 0.0000,
    tax_amount DECIMAL(15,2) GENERATED ALWAYS AS (subtotal * tax_rate) STORED,
    total_amount DECIMAL(15,2) GENERATED ALWAYS AS (subtotal + tax_amount) STORED,
    currency VARCHAR(3) DEFAULT 'KES',
    exchange_rate DECIMAL(10,6) DEFAULT 1.000000,
    total_amount_kes DECIMAL(15,2) GENERATED ALWAYS AS (total_amount * exchange_rate) STORED,
    issue_date DATE NOT NULL,
    valid_until DATE NOT NULL,
    accepted_date DATE NULL,
    rejected_date DATE NULL,
    status ENUM('draft', 'sent', 'viewed', 'accepted', 'rejected', 'expired', 'converted') DEFAULT 'draft',
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    approved_by BIGINT NULL,
    approved_at TIMESTAMP NULL,
    approval_status ENUM('pending', 'approved', 'rejected', 'needs_revision') DEFAULT 'approved',
    rejection_reason TEXT,
    client_name VARCHAR(255) NOT NULL,
    client_email VARCHAR(255),
    client_phone VARCHAR(20),
    client_address TEXT,
    client_company VARCHAR(255),
    items JSON,
    notes TEXT,
    payment_terms TEXT,
    terms_conditions TEXT,
    delivery_timeline TEXT,
    converted_to_invoice_id BIGINT NULL,
    converted_at TIMESTAMP NULL,
    conversion_notes TEXT,
    pdf_file_path VARCHAR(512),
    pdf_generated BOOLEAN DEFAULT FALSE,
    pdf_generated_at TIMESTAMP NULL,
    email_sent BOOLEAN DEFAULT FALSE,
    email_sent_at TIMESTAMP NULL,
    email_opened BOOLEAN DEFAULT FALSE,
    email_opened_at TIMESTAMP NULL,
    follow_up_required BOOLEAN DEFAULT TRUE,
    follow_up_date DATE NULL,
    follow_up_count INT DEFAULT 0,
    last_follow_up_at TIMESTAMP NULL,
    discount_type ENUM('percentage', 'fixed', 'none') DEFAULT 'none',
    discount_value DECIMAL(15,2) DEFAULT 0.00,
    discount_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_quotes_project (project_id),
    INDEX idx_quotes_client (client_id),
    INDEX idx_quotes_number (quote_number),
    INDEX idx_quotes_status (status),
    INDEX idx_quotes_priority (priority),
    INDEX idx_quotes_dates (issue_date, valid_until),
    INDEX idx_quotes_created (created_at),
    INDEX idx_quotes_follow_up (follow_up_date),
    INDEX idx_quotes_conversion (converted_to_invoice_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (converted_to_invoice_id) REFERENCES invoices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: quote_items
-- =============================================
CREATE TABLE IF NOT EXISTS quote_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    item_description TEXT,
    item_type ENUM('service', 'product', 'labor', 'material', 'fee', 'custom') DEFAULT 'service',
    unit_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    line_total DECIMAL(15,2) GENERATED ALWAYS AS (unit_price * quantity * (1 - discount_percentage/100)) STORED,
    unit VARCHAR(50) DEFAULT 'unit',
    sku VARCHAR(100),
    category VARCHAR(100),
    notes TEXT,
    display_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by BIGINT,
    INDEX idx_quote_items_quote (quote_id),
    INDEX idx_quote_items_category (category),
    INDEX idx_quote_items_order (display_order),
    FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: quote_activities
-- =============================================
CREATE TABLE IF NOT EXISTS quote_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quote_id BIGINT NOT NULL,
    activity_type ENUM('created', 'sent', 'viewed', 'accepted', 'rejected', 'expired', 'converted', 'follow_up', 'modified') NOT NULL,
    description TEXT NOT NULL,
    user_id BIGINT NULL,
    user_type ENUM('client', 'admin', 'system') DEFAULT 'system',
    activity_data JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_quote_activities_quote (quote_id),
    INDEX idx_quote_activities_type (activity_type),
    INDEX idx_quote_activities_user (user_id),
    INDEX idx_quote_activities_created (created_at),
    FOREIGN KEY (quote_id) REFERENCES quotes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 6: DEFAULT DATA INSERTS
-- =====================================================

-- Insert default navbar items
INSERT INTO admin_navbar_items (item_name, item_type, display_text, url, sort_order, is_visible) VALUES
('home', 'link', 'Home', '/', 1, TRUE),
('about', 'link', 'About Us', '/about', 2, TRUE),
('services', 'link', 'Our Services', '/services', 3, TRUE),
('projects', 'link', 'Projects & Activities', '/projects', 4, TRUE),
('companies', 'dropdown', 'Subsidiaries', '#', 5, TRUE),
('case_studies', 'link', 'Case Studies', '/case-studies', 6, TRUE),
('blog', 'link', 'Blog', '/blog', 7, TRUE),
('contact', 'link', 'Contact', '/contact', 8, TRUE);

-- Insert default website settings
INSERT INTO admin_website_settings (setting_key, setting_value, setting_type, display_name, description, category, is_public) VALUES
('site_title', 'The The-Greggory-Systems-And-Strategy-firm', 'text', 'Site Title', 'Main title of the website', 'general', TRUE),
('site_description', 'Strategic Project Development for all clients. Your Vision Delivered with Trust.', 'textarea', 'Site Description', 'Meta description for SEO', 'general', TRUE),
('contact_email', 'thegreggorysystemsandstrategyf@gmail.com', 'text', 'Contact Email', 'Main contact email address', 'contact', TRUE),
('contact_phone', '+254115525854', 'text', 'Contact Phone', 'Main contact phone number', 'contact', TRUE),
('company_address', 'rafiki kabarak, kabarak', 'textarea', 'Company Address', 'Physical office address', 'contact', TRUE),
('maintenance_mode', 'false', 'boolean', 'Maintenance Mode', 'Put site in maintenance mode', 'system', FALSE),
('allow_registration', 'true', 'boolean', 'Allow Registration', 'Enable user registration', 'auth', FALSE),
('deep_space_mode', 'true', 'boolean', 'Deep Space Mode', 'Master theme switch. ON (true) = Deep Space Dark Protocol, OFF (false) = Light Mode Protocol.', 'appearance', TRUE);

-- =====================================================
-- SECTION 7: CLIENT PORTAL - PROJECTS & ACTIVITIES
-- =====================================================

-- =============================================
-- Table: client_projects
-- Main projects table for client portal
-- =============================================
CREATE TABLE IF NOT EXISTS client_projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(50) UNIQUE NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    description TEXT,
    client_id BIGINT,
    project_manager_id BIGINT,
    start_date DATE,
    end_date DATE,
    status ENUM('planning', 'in_progress', 'on_hold', 'completed', 'cancelled') DEFAULT 'planning',
    priority ENUM('low', 'medium', 'high', 'critical') DEFAULT 'medium',
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    budget_allocated DECIMAL(15,2) DEFAULT 0.00,
    budget_spent DECIMAL(15,2) DEFAULT 0.00,
    project_type VARCHAR(100),
    industry_sector VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_client_projects_code (project_code),
    INDEX idx_client_projects_client (client_id),
    INDEX idx_client_projects_manager (project_manager_id),
    INDEX idx_client_projects_status (status),
    INDEX idx_client_projects_priority (priority),
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_manager_id) REFERENCES team_members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_milestones
-- Milestone tracking for projects
-- =============================================
CREATE TABLE IF NOT EXISTS project_milestones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    milestone_name VARCHAR(255) NOT NULL,
    description TEXT,
    milestone_date DATE NOT NULL,
    status ENUM('pending', 'in_progress', 'completed', 'delayed', 'cancelled') DEFAULT 'pending',
    completion_percentage DECIMAL(5,2) DEFAULT 0.00,
    actual_completion_date DATE,
    deliverables TEXT,
    dependencies TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_milestones_project (project_id),
    INDEX idx_milestones_status (status),
    INDEX idx_milestones_date (milestone_date),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_tasks
-- Task management for projects
-- =============================================
CREATE TABLE IF NOT EXISTS project_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    assigned_to BIGINT,
    task_type VARCHAR(100),
    priority ENUM('low', 'medium', 'high', 'critical') DEFAULT 'medium',
    status ENUM('to_do', 'in_progress', 'in_review', 'completed', 'cancelled') DEFAULT 'to_do',
    due_date DATE,
    start_date DATE,
    completion_date DATE,
    estimated_hours DECIMAL(8,2),
    actual_hours DECIMAL(8,2),
    parent_task_id BIGINT,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    tags VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_tasks_project (project_id),
    INDEX idx_tasks_assigned (assigned_to),
    INDEX idx_tasks_status (status),
    INDEX idx_tasks_priority (priority),
    INDEX idx_tasks_due_date (due_date),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES team_members(id) ON DELETE SET NULL,
    FOREIGN KEY (parent_task_id) REFERENCES project_tasks(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_resources
-- Resource allocation for projects
-- =============================================
CREATE TABLE IF NOT EXISTS project_resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    resource_type ENUM('personnel', 'equipment', 'material', 'software', 'other') NOT NULL,
    resource_name VARCHAR(255) NOT NULL,
    resource_id BIGINT,
    allocated_quantity DECIMAL(10,2) DEFAULT 1.00,
    used_quantity DECIMAL(10,2) DEFAULT 0.00,
    unit VARCHAR(50),
    cost_per_unit DECIMAL(10,2) DEFAULT 0.00,
    total_cost DECIMAL(15,2) DEFAULT 0.00,
    allocation_date DATE,
    availability_status ENUM('available', 'in_use', 'unavailable', 'reserved') DEFAULT 'available',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_resources_project (project_id),
    INDEX idx_resources_type (resource_type),
    INDEX idx_resources_status (availability_status),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_budgets
-- Budget tracking for projects
-- =============================================
CREATE TABLE IF NOT EXISTS project_budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    budget_category VARCHAR(100) NOT NULL,
    budget_name VARCHAR(255) NOT NULL,
    allocated_amount DECIMAL(15,2) NOT NULL,
    spent_amount DECIMAL(15,2) DEFAULT 0.00,
    remaining_amount DECIMAL(15,2) GENERATED ALWAYS AS (allocated_amount - spent_amount) STORED,
    fiscal_year INT,
    quarter ENUM('Q1', 'Q2', 'Q3', 'Q4'),
    approval_status ENUM('pending', 'approved', 'rejected', 'revised') DEFAULT 'pending',
    approved_by BIGINT,
    approval_date DATE,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_budgets_project (project_id),
    INDEX idx_budgets_category (budget_category),
    INDEX idx_budgets_status (approval_status),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (approved_by) REFERENCES admin_users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_expenses
-- Expense tracking for projects
-- =============================================
CREATE TABLE IF NOT EXISTS project_expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    expense_category VARCHAR(100) NOT NULL,
    description TEXT,
    amount DECIMAL(15,2) NOT NULL,
    expense_date DATE NOT NULL,
    incurred_by BIGINT,
    approved_by BIGINT,
    approval_status ENUM('pending', 'approved', 'rejected') DEFAULT 'pending',
    receipt_image_id BIGINT,
    invoice_number VARCHAR(100),
    vendor VARCHAR(255),
    payment_method VARCHAR(100),
    is_reimbursable BOOLEAN DEFAULT FALSE,
    reimbursement_status ENUM('not_applicable', 'pending', 'approved', 'paid') DEFAULT 'not_applicable',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_expenses_project (project_id),
    INDEX idx_expenses_category (expense_category),
    INDEX idx_expenses_date (expense_date),
    INDEX idx_expenses_status (approval_status),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (incurred_by) REFERENCES team_members(id) ON DELETE SET NULL,
    FOREIGN KEY (approved_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    FOREIGN KEY (receipt_image_id) REFERENCES images(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_invoices
-- Invoice management for clients
-- =============================================
CREATE TABLE IF NOT EXISTS client_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    project_id BIGINT,
    client_id BIGINT NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE NOT NULL,
    subtotal DECIMAL(15,2) NOT NULL,
    tax_amount DECIMAL(15,2) DEFAULT 0.00,
    discount_amount DECIMAL(15,2) DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status ENUM('draft', 'sent', 'viewed', 'partial', 'paid', 'overdue', 'cancelled') DEFAULT 'draft',
    payment_terms VARCHAR(255),
    notes TEXT,
    sent_date DATE,
    viewed_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_invoices_number (invoice_number),
    INDEX idx_invoices_project (project_id),
    INDEX idx_invoices_client (client_id),
    INDEX idx_invoices_status (status),
    INDEX idx_invoices_due_date (due_date),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE SET NULL,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: invoice_line_items
-- Line items for invoices
-- =============================================
CREATE TABLE IF NOT EXISTS invoice_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    item_description TEXT NOT NULL,
    quantity DECIMAL(10,2) NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    discount_percentage DECIMAL(5,2) DEFAULT 0.00,
    tax_percentage DECIMAL(5,2) DEFAULT 0.00,
    line_total DECIMAL(15,2) GENERATED ALWAYS AS (quantity * unit_price * (1 - discount_percentage/100) * (1 + tax_percentage/100)) STORED,
    item_type VARCHAR(100),
    service_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_line_items_invoice (invoice_id),
    FOREIGN KEY (invoice_id) REFERENCES client_invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_payments
-- Payment tracking for invoices
-- =============================================
CREATE TABLE IF NOT EXISTS client_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    client_id BIGINT,
    payment_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_method ENUM('bank_transfer', 'credit_card', 'paypal', 'check', 'cash', 'other') NOT NULL,
    payment_reference VARCHAR(255),
    status ENUM('pending', 'completed', 'failed', 'refunded', 'partial_refund') DEFAULT 'pending',
    processed_by BIGINT,
    notes TEXT,
    receipt_image_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_payments_invoice (invoice_id),
    INDEX idx_payments_client (client_id),
    INDEX idx_payments_date (payment_date),
    INDEX idx_payments_status (status),
    FOREIGN KEY (invoice_id) REFERENCES client_invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (processed_by) REFERENCES admin_users(id) ON DELETE SET NULL,
    FOREIGN KEY (receipt_image_id) REFERENCES images(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_documents
-- Document management for clients
-- =============================================
CREATE TABLE IF NOT EXISTS client_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    client_id BIGINT,
    document_name VARCHAR(255) NOT NULL,
    document_type ENUM('contract', 'proposal', 'report', 'invoice', 'deliverable', 'legal', 'technical', 'other') NOT NULL,
    category VARCHAR(100),
    description TEXT,
    file_path VARCHAR(512),
    file_size BIGINT,
    file_type VARCHAR(100),
    version_number INT DEFAULT 1,
    is_current_version BOOLEAN DEFAULT TRUE,
    parent_document_id BIGINT,
    status ENUM('draft', 'review', 'approved', 'rejected', 'archived') DEFAULT 'draft',
    access_level ENUM('public', 'private', 'confidential') DEFAULT 'private',
    expiry_date DATE,
    tags VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_documents_project (project_id),
    INDEX idx_documents_client (client_id),
    INDEX idx_documents_type (document_type),
    INDEX idx_documents_status (status),
    INDEX idx_documents_version (parent_document_id),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE SET NULL,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_document_id) REFERENCES client_documents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: document_signatures
-- eSignature tracking for documents
-- =============================================
CREATE TABLE IF NOT EXISTS document_signatures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    signer_id BIGINT NOT NULL,
    signer_name VARCHAR(255) NOT NULL,
    signer_email VARCHAR(255) NOT NULL,
    signature_status ENUM('pending', 'signed', 'declined', 'expired') DEFAULT 'pending',
    signature_date TIMESTAMP NULL,
    signature_image_id BIGINT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    signature_hash VARCHAR(255),
    expires_at TIMESTAMP NULL,
    reminder_sent BOOLEAN DEFAULT FALSE,
    reminder_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_signatures_document (document_id),
    INDEX idx_signatures_signer (signer_id),
    INDEX idx_signatures_status (signature_status),
    FOREIGN KEY (document_id) REFERENCES client_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_messages
-- Communication hub for clients
-- =============================================
CREATE TABLE IF NOT EXISTS client_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    client_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    subject VARCHAR(255),
    message_body TEXT NOT NULL,
    message_type ENUM('email', 'sms', 'whatsapp', 'in_app', 'other') DEFAULT 'in_app',
    priority ENUM('low', 'normal', 'high', 'urgent') DEFAULT 'normal',
    status ENUM('draft', 'sent', 'delivered', 'read', 'replied', 'failed') DEFAULT 'draft',
    is_unread BOOLEAN DEFAULT TRUE,
    parent_message_id BIGINT,
    attachments TEXT,
    sent_at TIMESTAMP NULL,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_messages_project (project_id),
    INDEX idx_messages_client (client_id),
    INDEX idx_messages_sender (sender_id),
    INDEX idx_messages_recipient (recipient_id),
    INDEX idx_messages_status (status),
    INDEX idx_messages_unread (is_unread),
    INDEX idx_messages_date (created_at),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE SET NULL,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (recipient_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_message_id) REFERENCES client_messages(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_risks
-- Risk assessment and management
-- =============================================
CREATE TABLE IF NOT EXISTS project_risks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    risk_title VARCHAR(255) NOT NULL,
    risk_description TEXT,
    risk_category VARCHAR(100),
    probability ENUM('very_low', 'low', 'medium', 'high', 'very_high') DEFAULT 'medium',
    impact ENUM('very_low', 'low', 'medium', 'high', 'very_high') DEFAULT 'medium',
    risk_score DECIMAL(5,2) GENERATED ALWAYS AS (
        CASE probability
            WHEN 'very_low' THEN 1
            WHEN 'low' THEN 2
            WHEN 'medium' THEN 3
            WHEN 'high' THEN 4
            WHEN 'very_high' THEN 5
        END *
        CASE impact
            WHEN 'very_low' THEN 1
            WHEN 'low' THEN 2
            WHEN 'medium' THEN 3
            WHEN 'high' THEN 4
            WHEN 'very_high' THEN 5
        END
    ) STORED,
    risk_level ENUM('low', 'medium', 'high', 'critical') GENERATED ALWAYS AS (
        CASE
            WHEN risk_score <= 4 THEN 'low'
            WHEN risk_score <= 9 THEN 'medium'
            WHEN risk_score <= 16 THEN 'high'
            ELSE 'critical'
        END
    ) STORED,
    mitigation_strategy TEXT,
    owner_id BIGINT,
    status ENUM('open', 'mitigating', 'mitigated', 'closed', 'occurred') DEFAULT 'open',
    identified_date DATE,
    target_mitigation_date DATE,
    actual_mitigation_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_risks_project (project_id),
    INDEX idx_risks_level (risk_level),
    INDEX idx_risks_status (status),
    INDEX idx_risks_owner (owner_id),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (owner_id) REFERENCES team_members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: quality_assurance
-- QA checkpoint tracking
-- =============================================
CREATE TABLE IF NOT EXISTS quality_assurance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    checkpoint_name VARCHAR(255) NOT NULL,
    checkpoint_type VARCHAR(100),
    description TEXT,
    qa_date DATE NOT NULL,
    status ENUM('pending', 'in_progress', 'passed', 'failed', 'deferred') DEFAULT 'pending',
    tester_id BIGINT,
    test_results TEXT,
    issues_found INT DEFAULT 0,
    issues_resolved INT DEFAULT 0,
    pass_rate DECIMAL(5,2),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_qa_project (project_id),
    INDEX idx_qa_status (status),
    INDEX idx_qa_date (qa_date),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (tester_id) REFERENCES team_members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: performance_metrics
-- KPI and performance tracking
-- =============================================
CREATE TABLE IF NOT EXISTS performance_metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    metric_name VARCHAR(255) NOT NULL,
    metric_category VARCHAR(100),
    metric_type ENUM('numeric', 'percentage', 'currency', 'boolean', 'text') DEFAULT 'numeric',
    value DECIMAL(20,4),
    target_value DECIMAL(20,4),
    unit VARCHAR(50),
    measurement_date DATE NOT NULL,
    frequency ENUM('daily', 'weekly', 'monthly', 'quarterly', 'yearly', 'one_time') DEFAULT 'monthly',
    status ENUM('on_track', 'at_risk', 'behind', 'ahead') DEFAULT 'on_track',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_metrics_project (project_id),
    INDEX idx_metrics_category (metric_category),
    INDEX idx_metrics_date (measurement_date),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: audit_logs
-- Comprehensive activity tracking
-- =============================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    user_type ENUM('admin', 'developer', 'user', 'system') NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    old_values TEXT,
    new_values TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(255),
    request_method VARCHAR(10),
    request_url TEXT,
    status ENUM('success', 'failure', 'warning') DEFAULT 'success',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_date (created_at),
    INDEX idx_audit_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_feedback
-- Client feedback and surveys
-- =============================================
CREATE TABLE IF NOT EXISTS client_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT,
    client_id BIGINT NOT NULL,
    feedback_type ENUM('satisfaction', 'bug_report', 'feature_request', 'complaint', 'compliment', 'other') NOT NULL,
    subject VARCHAR(255),
    feedback_body TEXT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    priority ENUM('low', 'medium', 'high', 'urgent') DEFAULT 'medium',
    status ENUM('received', 'in_review', 'addressed', 'resolved', 'closed') DEFAULT 'received',
    assigned_to BIGINT,
    response_text TEXT,
    response_date TIMESTAMP NULL,
    response_time_hours DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_feedback_project (project_id),
    INDEX idx_feedback_client (client_id),
    INDEX idx_feedback_type (feedback_type),
    INDEX idx_feedback_status (status),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE SET NULL,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES team_members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: project_timeline
-- Gantt chart and timeline data
-- =============================================
CREATE TABLE IF NOT EXISTS project_timeline (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    milestone_id BIGINT,
    item_name VARCHAR(255) NOT NULL,
    item_type ENUM('task', 'milestone', 'phase', 'deliverable') NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    duration_days INT,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    dependencies TEXT,
    color VARCHAR(7),
    is_critical BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_timeline_project (project_id),
    INDEX idx_timeline_task (task_id),
    INDEX idx_timeline_milestone (milestone_id),
    INDEX idx_timeline_dates (start_date, end_date),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (task_id) REFERENCES project_tasks(id) ON DELETE SET NULL,
    FOREIGN KEY (milestone_id) REFERENCES project_milestones(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: notifications
-- Notification system for clients
-- =============================================
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    notification_type ENUM('project_update', 'task_assigned', 'milestone_complete', 'invoice_sent', 'payment_received', 'message', 'risk_alert', 'qa_result', 'system') NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority ENUM('low', 'normal', 'high', 'urgent') DEFAULT 'normal',
    status ENUM('unread', 'read', 'archived') DEFAULT 'unread',
    action_url VARCHAR(512),
    related_entity_type VARCHAR(100),
    related_entity_id BIGINT,
    sent_via_email BOOLEAN DEFAULT FALSE,
    sent_via_sms BOOLEAN DEFAULT FALSE,
    sent_via_push BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    INDEX idx_notifications_user (user_id),
    INDEX idx_notifications_type (notification_type),
    INDEX idx_notifications_status (status),
    INDEX idx_notifications_date (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: change_requests
-- Change order management
-- =============================================
CREATE TABLE IF NOT EXISTS change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    request_number VARCHAR(50) UNIQUE NOT NULL,
    requested_by BIGINT NOT NULL,
    change_description TEXT NOT NULL,
    reason_for_change TEXT,
    impact_assessment TEXT,
    estimated_cost_impact DECIMAL(15,2) DEFAULT 0.00,
    estimated_time_impact_days INT DEFAULT 0,
    status ENUM('draft', 'submitted', 'under_review', 'approved', 'rejected', 'implemented', 'cancelled') DEFAULT 'draft',
    reviewed_by BIGINT,
    review_date DATE,
    approval_date DATE,
    implementation_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    deleted_by BIGINT DEFAULT NULL,
    INDEX idx_change_requests_project (project_id),
    INDEX idx_change_requests_number (request_number),
    INDEX idx_change_requests_status (status),
    FOREIGN KEY (project_id) REFERENCES client_projects(id) ON DELETE CASCADE,
    FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES admin_users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: data_classifications
-- Data sensitivity levels for client data safety
-- =============================================
CREATE TABLE IF NOT EXISTS data_classifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    sensitivity_level ENUM('public', 'internal', 'confidential', 'restricted') DEFAULT 'internal',
    encryption_required BOOLEAN DEFAULT FALSE,
    access_log_required BOOLEAN DEFAULT TRUE,
    retention_days INT DEFAULT 365,
    auto_delete BOOLEAN DEFAULT FALSE,
    allowed_roles JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_data_classifications_level (sensitivity_level),
    INDEX idx_data_classifications_encryption (encryption_required)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO data_classifications (name, description, sensitivity_level, encryption_required, access_log_required, retention_days, allowed_roles) VALUES
('public', 'Publicly available information', 'public', FALSE, FALSE, 365, '["admin", "developer", "user"]'),
('internal', 'Internal business data', 'internal', FALSE, TRUE, 730, '["admin", "developer"]'),
('confidential', 'Sensitive client data', 'confidential', TRUE, TRUE, 1825, '["admin"]'),
('restricted', 'Highly sensitive data', 'restricted', TRUE, TRUE, 3650, '["super_admin"]');

-- =============================================
-- Table: data_access_logs
-- Comprehensive data access tracking for compliance
-- =============================================
CREATE TABLE IF NOT EXISTS data_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_type ENUM('admin', 'developer', 'user', 'system') NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action ENUM('view', 'create', 'update', 'delete', 'export', 'download', 'share') NOT NULL,
    data_classification_id BIGINT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    session_id VARCHAR(255),
    request_method VARCHAR(10),
    request_url TEXT,
    query_params JSON,
    response_status INT,
    error_message TEXT,
    access_granted BOOLEAN DEFAULT TRUE,
    denial_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_data_access_user (user_id, created_at),
    INDEX idx_data_access_entity (entity_type, entity_id, created_at),
    INDEX idx_data_access_action (action, created_at),
    INDEX idx_data_access_classification (data_classification_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (data_classification_id) REFERENCES data_classifications(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: client_data_consent
-- Track client consent for data processing
-- =============================================
CREATE TABLE IF NOT EXISTS client_data_consent (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    consent_type ENUM('data_processing', 'marketing', 'third_party_sharing', 'analytics', 'communication') NOT NULL,
    consent_given BOOLEAN DEFAULT FALSE,
    consent_method ENUM('explicit', 'implied', 'opt_in', 'opt_out') DEFAULT 'explicit',
    consent_text TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    consent_date TIMESTAMP NULL DEFAULT NULL,
    withdrawal_date TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_consent_user (user_id, consent_type),
    INDEX idx_consent_given (consent_given),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_consent (user_id, consent_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- Table: data_retention_policies
-- Define data retention rules for compliance
-- =============================================
CREATE TABLE IF NOT EXISTS data_retention_policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    retention_period_days INT NOT NULL,
    auto_archive BOOLEAN DEFAULT FALSE,
    auto_delete BOOLEAN DEFAULT FALSE,
    archive_location VARCHAR(512),
    legal_basis VARCHAR(255),
    compliance_framework VARCHAR(100),
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_retention_entity (entity_type),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO data_retention_policies (entity_type, retention_period_days, auto_archive, auto_delete, legal_basis, compliance_framework) VALUES
('users', 2555, TRUE, FALSE, 'Contract performance', 'GDPR'),
('user_projects', 2555, TRUE, FALSE, 'Contract performance', 'GDPR'),
('project_tasks', 2555, TRUE, FALSE, 'Contract performance', 'GDPR'),
('project_documents', 2555, TRUE, FALSE, 'Contract performance', 'GDPR'),
('audit_logs', 3650, TRUE, TRUE, 'Legal obligation', 'GDPR'),
('data_access_logs', 3650, TRUE, TRUE, 'Legal obligation', 'GDPR'),
('client_feedback', 1825, TRUE, FALSE, 'Legitimate interest', 'GDPR'),
('mpesa_transactions', 3650, TRUE, FALSE, 'Legal obligation', 'KRA'),
('invoices', 3650, TRUE, FALSE, 'Legal obligation', 'KRA');

-- =============================================
-- Table: data_encryption_keys
-- Track encryption keys for sensitive data
-- =============================================
CREATE TABLE IF NOT EXISTS data_encryption_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    key_name VARCHAR(255) NOT NULL,
    key_type ENUM('symmetric', 'asymmetric', 'hash') DEFAULT 'symmetric',
    algorithm VARCHAR(100) NOT NULL,
    key_version INT DEFAULT 1,
    encrypted_key LONGBLOB NOT NULL,
    iv LONGBLOB,
    expires_at TIMESTAMP NULL DEFAULT NULL,
    rotated_at TIMESTAMP NULL DEFAULT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_encryption_keys_active (is_active, expires_at),
    INDEX idx_encryption_keys_type (key_type),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SECTION 7: CORPORATE PROTOCOLS
-- =====================================================

-- Show finalization as a proper result set
SELECT
    'The-Greggory-Systems-And-Strategy-firm' as Corporate_Entity,
    'Active' as Protocol_Status,
    NOW() as Deployment_Timestamp,
    'Strategic Systems & Business Solutions' as Mission_Profile,
    'Deep Space (Dark)' as Theme_Protocol_Status;

-- =====================================================
-- SUCCESS MESSAGE
-- =====================================================
SELECT 'Complete database schema created successfully with zero ghosts and total strategic alignment!' as message;
-- =============================================
-- Table: company_personnel
-- Company personnel profiles - admin-managed like blog articles,
-- displayed on the About page (image + name + position; click opens profile).
-- =============================================
CREATE TABLE IF NOT EXISTS company_personnel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    position VARCHAR(150) NOT NULL,
    bio LONGTEXT,
    image_url VARCHAR(512) DEFAULT NULL,
    image_blob LONGBLOB,
    image_mime_type VARCHAR(100) DEFAULT NULL,
    image_file_name VARCHAR(255) DEFAULT NULL,
    sort_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    INDEX idx_company_personnel_active (is_active),
    INDEX idx_company_personnel_sort (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed founding personnel (Brian Mwanza)
INSERT INTO company_personnel (name, position, bio, image_url, sort_order, is_active)
SELECT 'Brian Mwanza', 'Founder & Managing Director',
'<p>Brian Mwanza is the visionary force behind The-Greggory-Systems-And-Strategy-firm. With over a decade of experience in systemic designand business strategy, he has guided some of the most ambitious organizations through complex digitaland operational transformations.</p><p>His philosophy is rooted in the belief that &quot;Strategy is not a document; it''s a pulse.&quot; Under his leadership,the firm has evolved from a boutique advisory to a global architect of business resonance,known for its uncompromising commitment to clarity and human-centric systems.</p>',
'/images/brian-mwanza-ceo.jpg',
0, TRUE
WHERE NOT EXISTS (SELECT 1 FROM company_personnel WHERE name = 'Brian Mwanza');

-- FILE: portal-sync-schema.sql
-- The Greggory Systems & Strategy Firm
-- Database Schema for Client Portal Synchronization
-- Target: Aiven Cloud MySQL / MariaDB

CREATE DATABASE IF NOT EXISTS the_greggory_systems_and_strategy_firm_db_main;
USE the_greggory_systems_and_strategy_firm_db_main;

-- 1. Projects Table
CREATE TABLE IF NOT EXISTS projects (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'Active',
    progress INT DEFAULT 0,
    client_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Invoices Table
CREATE TABLE IF NOT EXISTS invoices (
    id INT AUTO_INCREMENT PRIMARY KEY,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending',
    client_id INT NOT NULL,
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Project Reports (PDF/Document Vault)
-- Contains LONGBLOB for actual file storage as requested in Deployment Guide
CREATE TABLE IF NOT EXISTS project_reports (
    id INT AUTO_INCREMENT PRIMARY KEY,
    project_id INT NOT NULL,
    client_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    file_content LONGBLOB NOT NULL,
    file_type VARCHAR(100) DEFAULT 'application/pdf',
    file_size BIGINT,
    report_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- 4. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    priority VARCHAR(20) DEFAULT 'normal',
    status VARCHAR(20) DEFAULT 'unread',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

