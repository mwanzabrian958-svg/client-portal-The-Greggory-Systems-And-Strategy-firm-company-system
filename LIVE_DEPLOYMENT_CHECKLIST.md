# ✅ Final Deployment Checklist: The Greggory Client Portal

Follow these steps in order to move the app from development to a live production environment.

---

## 🟢 PHASE 1: Database & Backend (Cloud)
- [x] **1. Run SQL Schema:**
    *   Executed via automation script.
    *   *Tables Created:* projects, invoices, project_reports, notifications, user_feedback, mpesa_transactions.
    *   *Status:* Verified by AI Assistant.
- [ ] **2. Update Render Environment Variables:**
    *   Login to [Render Dashboard](https://dashboard.render.com/).
    *   Navigate to your Backend Service > **Environment**.
    *   Update the following to **Production** values:
        *   `MPESA_SHORTCODE`: Your actual Paybill/Till.
        *   `MPESA_PASSKEY`: From Safaricom email.
        *   `MPESA_CONSUMER_KEY`: From Daraja App.
        *   `MPESA_CONSUMER_SECRET`: From Daraja App.
        *   `NODE_ENV`: Set to `production`.
- [ ] **3. Verify Backend Connectivity:**
    *   Ensure the backend service is redeployed and showing "Live" on Render.

---

## 🟡 PHASE 2: App Security & Config
- [x] **4. SSL Fingerprint Update:**
    *   *Fingerprint Fetched:* `fizfE9JVlzlRplEx7epXfqW9enrbLvwF/LU26XTPEG4=`
    *   *Status:* Updated in `RetrofitClient.kt` by AI Assistant.
- [x] **5. Firebase Check:**
    *   *Status:* `google-services.json` verified and installed in `app/` folder.

---

## 🔴 PHASE 3: Production Build
- [ ] **6. Create Local Signing Properties:**
    *   Copy `signing.properties.template` to a new file: `signing.properties`.
    *   Open `signing.properties` and enter your real keystore path and passwords.
- [ ] **7. Generate Release APK:**
    *   Open the terminal in Android Studio.
    *   Run: `./gradlew assembleRelease`.
    *   *Result:* Your production-ready file will be at `app/build/outputs/apk/release/app-release.apk`.

---

## 🚀 PHASE 4: Go-Live
- [ ] **8. Smoke Test:**
    *   Install the `app-release.apk` on a physical device.
    *   Log in and verify that the "Set in Stone" routing successfully fetches your live project data from Aiven Cloud.
- [ ] **9. Client Handover:**
    *   Distribute the APK to the first client!
