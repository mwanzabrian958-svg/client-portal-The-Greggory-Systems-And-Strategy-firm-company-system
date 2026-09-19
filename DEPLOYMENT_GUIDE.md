# 🚀 Final Deployment & Go-Live Guide
**The Greggory Systems & Strategy Firm - Client Portal**

This guide outlines the final manual steps required to move the system from development/sandbox to a live business environment.

---

## 1. M-Pesa Production Switch (Financial Hub)
**Note:** Clients can also pay manually outside the app by dialing `*334#` and selecting **Send Money** to the company number **07115525854**.

### A. Get Production Credentials
1. Log in to the [Safaricom Developer Portal (Daraja)](https://developer.safaricom.co.ke/).
2. Go to **My Apps** and select your production app (or create a new one with "Lipa na M-Pesa Online" checked).
3. Click **Go-Live** and follow the instructions to link your Business Shortcode (Paybill/Till).
4. Safaricom will send you the **Production Passkey** via email.

### B. Update Render Environment Variables
1. Open your [Render Dashboard](https://dashboard.render.com/).
2. Select your **Backend Service**.
3. Go to **Environment** settings and update the following:
   - `MPESA_CONSUMER_KEY`: Your Production Key.
   - `MPESA_CONSUMER_SECRET`: Your Production Secret.
   - `MPESA_PASSKEY`: The Passkey emailed to you.
   - `MPESA_SHORTCODE`: Your actual Paybill/Till number (e.g., 174379).
   - `NODE_ENV`: Change from `development` to `production`.
4. Save changes. Render will automatically redeploy the backend.

---

## 2. Google Services (Firebase & Cloud)
To enable Push Notifications, Analytics, and Crashlytics:

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Select your project: **Greggory Portal**.
3. Click the Gear icon ⚙️ > **Project settings**.
4. In the "Your apps" section, download the `google-services.json` file.
5. **Placement:** Open your project folder in Windows Explorer and move the file here:
   `...\client-portal-The-Greggory-Systems-And-Strategy-firm-company-system\app\google-services.json`
6. Rebuild the app in Android Studio (**Build > Clean Project** then **Build > Rebuild Project**).

---

## 3. App Signing & Production APK
To prepare the app for installation on client phones or the Play Store:

### A. Generate Keystore (.jks)
1. In Android Studio, go to **Build > Generate Signed Bundle / APK...**
2. Select **APK** and click **Next**.
3. Under **Key store path**, click **Create new...**
4. Choose a path (e.g., your Desktop) and name it `greggory_production_key.jks`.
5. Enter a strong password for both the Keystore and the Key.
6. Set the **Alias** to `greggory_alias`.
7. **IMPORTANT:** Keep this file and these passwords safe! If you lose them, you cannot update the app in the future.

### B. Configure GitHub Secrets (For Automated Deployment)
If you use the GitHub Actions workflow defined in the `README.md`, you need to add these secrets to your GitHub Repository:
1. Go to your repo on GitHub > **Settings > Secrets and variables > Actions**.
2. Add **New repository secret**:
   - `RELEASE_KEYSTORE_BASE64`: Open a terminal, run `[Convert]::ToBase64String([IO.File]::ReadAllBytes("path\to\your\keystore.jks"))` in PowerShell, and paste the output.
   - `KEYSTORE_PASSWORD`: The password you chose.
   - `KEY_ALIAS`: `greggory_alias`.
   - `KEY_PASSWORD`: The key password you chose.

---

## 4. Database Table Sync (PDF Tables)
The `project_reports` table definition is located in `database/portal-sync-schema.sql`.
1. Log in to your **Aiven Console** or use **HeidiSQL**.
2. Run the SQL in `database/portal-sync-schema.sql` to ensure the "PDF Tables" exist in the cloud.
3. This is where real project-specific PDFs will be stored (as LONGBLOB).

## 5. SSL Pinning Security (Critical for Release)
In `RetrofitClient.kt`, I added a security feature called SSL Pinning.
1. Currently, it has a placeholder: `sha256/AAAAAAAAAAAAAAAA...`
2. **Action Required:** You must replace this with the real fingerprint of your Render certificate.
3. **How to get it:** Open a terminal on your computer and run:
   ```bash
   openssl s_client -connect the-greggory-systems-and-strategy-firm-jz7i.onrender.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
   ```
4. Paste the output string into `RetrofitClient.kt` replacing the `AAAAA...` string.

---
*Developed for The Greggory Systems & Strategy Firm.*
