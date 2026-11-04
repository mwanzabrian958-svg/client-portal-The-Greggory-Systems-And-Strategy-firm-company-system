-- ============================================================================
-- The Greggory Client Portal - Aiven MySQL Database Schema (defaultdb)
-- 100% Parity with Android App Local Room Database & API Models
-- Host: mysql-57dd84d-the-greggory-systems-and-strategy-firm.i.aivencloud.com:27809
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS `users` (
    `email` VARCHAR(255) NOT NULL PRIMARY KEY,
    `id` INT NOT NULL,
    `firstName` VARCHAR(100) NOT NULL,
    `lastName` VARCHAR(100) DEFAULT NULL,
    `displayName` VARCHAR(150) DEFAULT NULL,
    `phone` VARCHAR(50) DEFAULT NULL,
    `primaryRole` VARCHAR(50) DEFAULT 'user',
    `profilePhotoData` LONGTEXT DEFAULT NULL,
    `missionBriefing` TEXT DEFAULT NULL,
    `authToken` TEXT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Projects Table
CREATE TABLE IF NOT EXISTS `projects` (
    `id` INT NOT NULL PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `status` VARCHAR(100) NOT NULL,
    `progress` INT NOT NULL,
    `client_id` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Invoices Table
CREATE TABLE IF NOT EXISTS `invoices` (
    `id` INT NOT NULL PRIMARY KEY,
    `amount` DECIMAL(12,2) NOT NULL,
    `status` VARCHAR(100) NOT NULL,
    `client_id` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Reports Table
CREATE TABLE IF NOT EXISTS `reports` (
    `id` INT NOT NULL PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `summary` TEXT DEFAULT NULL,
    `file_type` VARCHAR(50) NOT NULL,
    `file_size` BIGINT NOT NULL,
    `report_date` VARCHAR(50) NOT NULL,
    `project_name` VARCHAR(255) NOT NULL,
    `client_id` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Messages Cache Table
CREATE TABLE IF NOT EXISTS `messages` (
    `id` VARCHAR(100) NOT NULL PRIMARY KEY,
    `sender` VARCHAR(150) NOT NULL,
    `subject` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `time` VARCHAR(50) NOT NULL,
    `unread` BOOLEAN NOT NULL,
    `feedback` BOOLEAN NOT NULL,
    `attachmentUrl` TEXT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Strategy Chat Messages Table
CREATE TABLE IF NOT EXISTS `chat_messages` (
    `localId` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `id` VARCHAR(100) NOT NULL,
    `senderId` INT NOT NULL,
    `senderName` VARCHAR(150) NOT NULL,
    `message` TEXT NOT NULL,
    `timestamp` BIGINT NOT NULL,
    `isFromMe` BOOLEAN NOT NULL,
    `attachmentUrl` TEXT DEFAULT NULL,
    `attachmentType` VARCHAR(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Client Agreements Table
CREATE TABLE IF NOT EXISTS `client_agreements` (
    `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `clientName` VARCHAR(150) NOT NULL,
    `companyName` VARCHAR(200) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `phoneNumber` VARCHAR(50) NOT NULL,
    `serviceCategory` VARCHAR(150) NOT NULL,
    `declarationAccepted` BOOLEAN NOT NULL,
    `signatureName` VARCHAR(150) NOT NULL,
    `timestamp` BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Quotes Table
CREATE TABLE IF NOT EXISTS `quotes` (
    `id` INT NOT NULL PRIMARY KEY,
    `project_id` INT NOT NULL,
    `project_name` VARCHAR(255) NOT NULL,
    `amount` DECIMAL(12,2) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `status` VARCHAR(100) NOT NULL,
    `client_id` INT NOT NULL,
    `created_at` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. Feedback Table
CREATE TABLE IF NOT EXISTS `feedback` (
    `id` INT NOT NULL PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `type` VARCHAR(50) NOT NULL,
    `rating` INT NOT NULL,
    `priority` VARCHAR(50) NOT NULL,
    `author` VARCHAR(150) NOT NULL,
    `created_at` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Change Requests Table
CREATE TABLE IF NOT EXISTS `change_requests` (
    `id` INT NOT NULL PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `status` VARCHAR(100) NOT NULL,
    `client_id` INT NOT NULL,
    `created_at` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. Signature Requests Table
CREATE TABLE IF NOT EXISTS `signature_requests` (
    `id` INT NOT NULL PRIMARY KEY,
    `document_name` VARCHAR(255) NOT NULL,
    `project_name` VARCHAR(255) NOT NULL,
    `status` VARCHAR(100) NOT NULL,
    `client_id` INT NOT NULL,
    `created_at` VARCHAR(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. Notifications Table
CREATE TABLE IF NOT EXISTS `notifications` (
    `id` INT NOT NULL PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `message` TEXT NOT NULL,
    `timestamp` VARCHAR(50) NOT NULL,
    `read` BOOLEAN NOT NULL,
    `client_id` INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
