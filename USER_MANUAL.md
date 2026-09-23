# The Greggory Client Portal - User Manual

Welcome to your mission control. This application is designed to give you complete transparency and control over your partnership with **The Greggory Systems & Strategy Firm**.

## 📥 App Download Links
* **Web Download Page:** [https://mwanzabrian958-svg.github.io/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/](https://mwanzabrian958-svg.github.io/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/)
* **Direct APK Download:** [GSSF-client-portal.apk](https://github.com/mwanzabrian958-svg/client-portal-The-Greggory-Systems-And-Strategy-firm-company-system/raw/main/docs/GSSF-client-portal.apk)

---

## 🏁 Getting Started

### 1. Secure Login & Registration
*   **Account Creation:** Fill in your details on the Sign-Up screen.
*   **Legal Agreement:** You must review and agree to our **Terms of Use and Privacy Policy**. Tap the highlighted link above the "Sign Up" button to read the full document.
*   **Biometric Activation:** On your first successful login, you will be prompted to enable Fingerprint or Face ID. This ensures your project data remains private even if your phone is unlocked.

### 2. The Dashboard (Mission Control)
*   **Active Projects:** See real-time progress percentages of your ongoing initiatives.
*   **Open Invoices:** A quick glance at any outstanding balances.
*   **Direct Strategy Support:** Instant 1-tap channels (System Chat, Call, WhatsApp, SMS) located directly on your Home Screen.

---

## 🎯 Direct Strategy Support Channels Directive

| Channel | Directive & Purpose | Technical Execution |
| :--- | :--- | :--- |
| 💬 **Direct System Chat ("Chat")** | In-app, encrypted messaging stream directly with Lead Strategists. | Launches **Strategy Chat Screen** (`/api/chat/send`), persisted locally in Room DB for offline resilience. |
| 📞 **Direct Phone Call ("Call")** | Direct voice hotline connection (`+254 115 525 854`). | Launches Android Phone Dialer via `ACTION_DIAL` (`tel:+254115525854`). |
| 💬 **WhatsApp Stream ("WhatsApp")** | Encrypted instant messaging and document sharing via official WhatsApp business desk. | Opens WhatsApp conversation window (`https://api.whatsapp.com/send?phone=254115525854`). |
| ✉️ **Direct SMS ("SMS")** | Low-bandwidth or offline cellular SMS messaging line. | Opens native Android SMS app via `ACTION_VIEW` (`sms:+254115525854`). |

---

## 📊 Managing Your Projects
*   Navigate to **Full Project Ledger** from the side menu.
*   Tap on any project to see its current status (Active, On Hold, or Completed).
*   Progress bars indicate how close each phase is to completion.

---

## 💸 Payments & Billing
We have simplified the payment process using M-Pesa integration.

### Quick Pay (STK Push)
1. Go to **Financial Archive**.
2. Tap the **PAY** button on any pending invoice.
3. Enter your M-Pesa phone number. The app will automatically copy the company transfer number to your clipboard as a backup.
4. You will receive a prompt on your phone to enter your M-Pesa PIN.
5. **Auto-Verification:** Once Safaricom sends you the confirmation message, the app will automatically detect it and sync the record to the firm's database. No manual input is required.
6. A downloadable digital receipt will appear in your **Support Inbox** shortly after.

### Manual Payment
*   If you prefer manual payment, the app provides the firm's Paybill/Number details directly in the payment dialog. You can also manually submit a transaction code if the auto-interceptor is disabled on your device.

---

## 📶 Connectivity & Offline Mode
The portal is designed for high-availability.
*   **Offline Mode:** If you lose internet access, a red status bar will appear. You can still view all your project details and financial history, which are stored in a secure local vault.
*   **Automatic Sync:** The app will silently refresh and reconcile all records as soon as you are back online.


---

## 📂 The Document Vault
Access all official project reports and deliverables.
*   Go to **Document Vault**.
*   Tap the **Download** icon on any report.
*   Documents are saved to your phone's "Downloads" folder as PDFs or Word files, ready for printing or sharing.

---

## ⚙️ Profile & Support
*   **Photo Uploader:** Personalize your portal by uploading a profile photo in the settings.
*   **Security:** To log out securely, use the logout button at the bottom of the side menu. This will clear all local session data for your protection.

---
*For support, contact The Greggory Systems & Strategy Firm technical department.*
