# Technical Documentation: The Greggory Client Portal

## 1. System Architecture
The application follows a modern **Clean Architecture** pattern using **Jetpack Compose** for the UI layer and **Retrofit/Room** for the data layer.

### Layers:
*   **UI Layer (`ui/`):** Built entirely with Declarative UI (Compose). It uses a state-driven approach where screens observe data from the repository/database.
*   **Data Layer (`data/`):**
    *   **Remote (`api/`):** Retrofit 2 handles all cloud communication with the Render backend.
    *   **Local (`local/`):** Room Database provides offline caching for Projects, Invoices, and Reports.
*   **Domain/Utility (`utils/`):** Contains the "Set in Stone" routing logic, Biometric handlers, and Network interceptors.

---

## 2. Core Security Implementation
Security is the backbone of this project, defined by the "Set in Stone" routing policy.

### Data Isolation (Routing)
*   **Header Injection:** The `RetrofitClient` uses an `Interceptor` to inject `X-Greggory-Client-ID` and `X-Routing-Policy` into every request.
*   **Integrity Check:** The `DataRouter` utility validates that incoming data packets match the authenticated user's ID to prevent cross-account leakage.

### Storage Encryption
*   **Jetpack Security:** Sensitive data (JWT tokens) is stored in `EncryptedSharedPreferences` using AES-256 GCM.
*   **Database:** Local SQLite (Room) data is siloed to the application's private storage.

### Network Security
*   **SSL Pinning:** The app enforces certificate pinning against the Render production certificate to eliminate Man-in-the-Middle (MITM) risks.

---

## 3. Tech Stack Details
| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose (Material 3) - BOM 2025.01.00 |
| **Networking** | Retrofit 2 + OkHttp 4 |
| **JSON Parsing** | GSON |
| **Image Loading** | Coil |
| **Local Database** | Room Persistence Library |
| **Security** | Android Biometric + Jetpack Crypto |
| **Push Notifications** | Firebase Cloud Messaging (FCM) |

---

## 4. Build Configurations
The project uses two primary build variants:
1.  **Debug:** Used for development. SSL Pinning is disabled here to allow for proxy debugging if necessary.
2.  **Release:** Hardened for production. SSL Pinning is mandatory. ProGuard/R8 is enabled to obfuscate the routing logic.

## 5. Directory Structure
```text
com.greggory.portal/
├── data/
│   ├── api/          # Retrofit Interfaces & Request/Response Models
│   ├── local/        # Room Database, DAOs, & Preferences Manager
│   └── repository/   # Offline Sync Repository (Source of Truth)
├── ui/
│   ├── components/   # Shared UI components (Network Bar, etc.)
│   ├── navigation/   # Compose Navigation Graphs & Routes
│   ├── screens/      # Individual UI Screens (Compose)
│   └── theme/        # Material 3 Color Schemes & Typography
└── utils/            # AI Monitoring, M-Pesa Interceptor, & Security
```

---

## 6. Advanced Enterprise Features (Finalized)

### Legal & Compliance Integration
*   **Annotated UI Strings:** The `SignupScreen` uses `buildAnnotatedString` to provide inline clickable links for legal documents.
*   **Scoped Dialogs:** Terms & Privacy content is delivered via a full-screen `Dialog` component to maintain state continuity in the registration flow.
*   **Privacy Transparency:** Explicit documentation of AI Telemetry and Data Isolation is now provided to the client during onboarding.

### Autonomous AI Telemetry (`AiIssueMonitor`)
*   **Global Exception Hook:** Intercepts uncaught crashes across all threads.
*   **Heuristic Priority Engine:** Analyzes stack traces to assign `high/medium/low` priority.
*   **PII Scrubbing:** Automatically redacts tokens, passwords, and secrets from logs before cloud transmission.

### Triple-Redundancy Accounting
*   **M-Pesa AutoReconciler:** A `NotificationListenerService` that intercepts successful payment confirmations and pushes them to the backend instantly.
*   **Clipboard Integration:** Automatically copies company billing details for the user during STK Push triggers.
*   **Firebase Receipt Caching:** Automated downstream delivery of downloadable receipts into the local `messages` Room table.

### Data Resiliency & Privacy
*   **Offline Repository Pattern:** UI observes Room Flows; database updates silently in the background.
*   **Network Status Banner:** Real-time UI indicator for offline/cache-only modes.
*   **Nuclear Wipe:** All local caches (Projects, Invoices, Messages) are programmatically purged on logout or session expiry to ensure zero forensic footprint.

