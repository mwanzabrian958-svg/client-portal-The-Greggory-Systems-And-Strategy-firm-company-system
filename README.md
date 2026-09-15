# The Greggory Client Portal - Mobile Client

A modern Android application built with Jetpack Compose, designed to provide clients of The Greggory Systems & Strategy Firm with a seamless, secure, and unified portal for project management, billing, and communication.

## 📖 Project Documentation
For detailed information, please refer to the following guides:
*   [**Technical Documentation**](TECHNICAL_DOCUMENTATION.md) - Architecture, Tech Stack, & Directory Structure.
*   [**API Reference**](API_DOCUMENTATION.md) - Endpoints, Payloads, & Routing.
*   [**Security Policy**](SECURITY.md) - "Set in Stone" Routing & Encryption details.
*   [**Deployment Guide**](DEPLOYMENT_GUIDE.md) - Go-Live instructions & M-Pesa setup.
*   [**User Manual**](USER_MANUAL.md) - Client-facing guide for using the app.
*   [**Database Schema**](database/portal-sync-schema.sql) - SQL definitions for cloud sync.

## 🚀 Key Features

### 👤 Profile Management
*   **Dynamic Photo Uploader:** An interactive profile photo uploader on the Sign-Up screen. Users can tap the company logo or the `+` button to select a personal photo, which dynamically replaces the branding to personalize the experience.
*   **Secure Authentication:** Integrated with the firm's central authentication system.

### 🔐 Security & Data Routing (Set in Stone)
*   **Encrypted Storage:** All sensitive tokens and client identifiers are stored using AES-256 GCM `EncryptedSharedPreferences`.
*   **SSL Pinning:** Network traffic is locked to the production Render backend to prevent man-in-the-middle attacks.
*   **Global Token Interceptor:** The app uses a low-level network interceptor to automatically attach DB-generated auth tokens to every request.
*   **Unified Data Routing:** Ensures that all client-specific data (Projects, Invoices, KPIs) is routed correctly to the unique user ID with client-side integrity validation.
*   **Biometric Shield:** Integrated biometric authentication (Fingerprint/Face/PIN) with a secure routing state to protect the dashboard.

### 📊 Mission Control (Dashboard)
*   **Live KPI Tracking:** Real-time visibility into "Active Projects" and "Open Invoices" pulled directly from the cloud database.
*   **Project Ledger:** Detailed lists of active and completed projects with progress tracking.
*   **Financial Hub:** Comprehensive billing section showing invoice status (Paid/Pending) with integrated payment triggers.

## 🛠 Tech Stack
*   **UI:** Jetpack Compose (Modern Declarative UI)
*   **Networking:** Retrofit 2 & OkHttp (Global Interceptors)
*   **Image Loading:** Coil (Asynchronous Image Processing)
*   **Navigation:** Jetpack Navigation Compose
*   **Database Integration:** Fully wired to Aiven Cloud MySQL via the Render secure gateway.
*   **Infrastructure:** Hosted on Render with global edge routing.

## 🏗 Setup & Build

### Prerequisites
*   **Android Studio:** Latest version (Hedgehog or newer).
*   **Java:** JDK 17 (Included in Android Studio JBR).
*   **Emulator:** A running Android Virtual Device (AVD).

### Build Instructions
To build the debug APK:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
./gradlew assembleDebug
```

### 🚀 CI/CD & Deployment
The project is configured with GitHub Actions for automated builds. To enable production builds, add the following secrets to your GitHub repository:
*   `RELEASE_KEYSTORE_BASE64`: The base64-encoded string of your `.jks` file.
*   `KEYSTORE_PASSWORD`: Password for the keystore.
*   `KEY_ALIAS`: Your key alias.
*   `KEY_PASSWORD`: Password for the specific key.

To install on the active emulator:
```powershell
adb install -r app/build_final_v2/outputs/apk/debug/app-debug.apk
```

## 🌐 Backend Integration
The app is fully synchronized with the Greggory Firm production environment using a dual-node failover architecture:
*   **Production API:** `https://w-the-greggory-systems-and-strategy-firm-vik4.onrender.com`
*   **Primary DB (Cloud):** Aiven Cloud MySQL (`mysql-3ab0daba-thegreggorysystemsandstrategyfirm-dd1d.j.aivencloud.com:28067`)
*   **Secondary DB (Local):** XAMPP MariaDB (`127.0.0.1:3306`)
*   **Database Schema:** `the_greggory_systems_and_strategy_firm_db_main` (Default: `defaultdb`)
*   **Auth Protocol:** Locked database-generated terminal tokens.
*   **Financial Gateway:** M-Pesa Daraja STK Integration (Shortcode: 174379).

---
*Developed for The Greggory Systems & Strategy Firm.*
