# Security Architecture - Greggory Client Portal

## "Set in Stone" Routing & Data Security

The mobile client implements the "Set in Stone" routing policy, ensuring that sensitive client data is siloed and protected by multi-layered hardware and network security.

### 1. Hardware-Backed Encryption
All authentication tokens and sensitive client identifiers are stored using **Android Jetpack Security**.
*   **EncryptedSharedPreferences:** Tokens are encrypted using **AES-256 GCM** before being written to disk.
*   **Master Key:** The encryption keys are managed by the Android KeyStore, ensuring that even on rooted devices, the raw token is inaccessible to unauthorized actors.

### 2. Network Integrity (SSL Pinning)
To prevent Man-in-the-Middle (MITM) attacks, the app implements **Certificate Pinning** within the `RetrofitClient`. 
*   The app only trusts connections signed by the specific SHA-256 hash of the firm's production Render certificate.
*   This ensures that data routing to the Aiven Cloud MySQL database cannot be intercepted by proxies or malicious network nodes.

### 3. Biometric Shield
Access to the dashboard is protected by a mandatory biometric gateway.
*   **Implementation:** Uses the `androidx.biometric` library to verify Fingerprint, Face, or Device Credential.
*   **Routing Lock:** The UI remains in a blank "Secured" state until authentication is successful, preventing screen-scraping of project data during app transitions.

### 4. Global Network Interception & Routing
We use a centralized `OkHttp` interceptor within the `RetrofitClient` architecture:
*   **Automatic Injection:** Every outgoing request is intercepted before transmission to add the `Authorization` and `X-Greggory-Client-ID` headers.
*   **Server-Side Silos:** The backend uses these headers to partition queries, ensuring a client only ever sees data mapped to their specific ID in the `the_greggory_systems_and_strategy_firm_db_main` schema.

### 5. Code Hardening (ProGuard)
The app is built with **R8/ProGuard** enabled in release mode.
*   **Obfuscation:** Internal routing logic and API endpoint structures are obfuscated to prevent reverse engineering.
*   **Shrinking:** Unused code and resources are removed to reduce the attack surface.

## Backend Infrastructure
*   **Production API:** `https://w-the-greggory-systems-and-strategy-firm-vik4.onrender.com`
*   **Encryption:** Mandatory TLS 1.3 for all data in transit.
*   **Database:** Aiven Cloud MySQL with enforced SSL connections.
*   **Persistence:** Tokens follow a strict `7-day` rotation policy.
