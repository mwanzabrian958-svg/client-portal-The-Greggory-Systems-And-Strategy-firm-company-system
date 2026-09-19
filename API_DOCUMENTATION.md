# API Reference Documentation

**Base URL:** `https://the-greggory-systems-and-strategy-firm-jz7i.onrender.com/`

---

## 1. Authentication & User Management

### Register
*   **Endpoint:** `POST /api/users/register`
*   **Description:** Creates a new client account. Supports Base64 profile photo uploads.
*   **Payload:** `RegisterRequest` (first_name, last_name, email, phone, password, profile_photo_base64, profile_photo_mime_type, profile_photo_file_name)

### Login
*   **Endpoint:** `POST /api/users/login`
*   **Description:** Authenticates user and returns a terminal-specific JWT token.
*   **Headers Required:** None.

### Forgot Password
*   **Endpoint:** `POST /api/users/forgot-password`
*   **Description:** Triggers a password reset email for the provided account.

### Change Password
*   **Endpoint:** `POST /api/users/change-password`
*   **Description:** Allows authenticated users to update their credentials.

### Profile Update
*   **Endpoint:** `PUT /api/users/profile`
*   **Description:** Updates client display name and phone number.

### Upload Profile Photo
*   **Endpoint:** `POST /api/users/profile-photo` (Multipart)
*   **Description:** Uploads a new profile image directly to the secure storage.

---

## 2. Client Dashboard & Projects

### Get Dashboard Data
*   **Endpoint:** `GET /api/users/client-dashboard`
*   **Description:** Retrieves summary statistics, recent projects, active invoices, tasks, and KPI metrics.
*   **Routing:** Automatically filtered by `X-Greggory-Client-ID`.

### Get Project Ledger
*   **Endpoint:** `GET /api/user-projects`
*   **Description:** Returns a full list of all projects (active and archived) for the client.

### Global Search
*   **Endpoint:** `GET /api/users/search?q={query}`
*   **Description:** Searches across projects, tasks, invoices, and documents for the authenticated client.

---

## 3. Financial Hub (M-Pesa)

### Initiate STK Push
*   **Endpoint:** `POST /api/mpesa/stkpush`
*   **Description:** Triggers a payment request on the client's phone.
*   **Payload:** `MpesaStkPushRequest` (amount, phoneNumber, accountReference, description)

### Check Payment Status
*   **Endpoint:** `GET /api/mpesa/status/{checkoutRequestId}`
*   **Description:** Polls for the final result of an initiated M-Pesa transaction.

### Report Manual Payment
*   **Endpoint:** `POST /api/mpesa/report-payment`
*   **Description:** Used by the automated interceptor to sync M-Pesa SMS records to the central accounting table.
*   **Payload:** `PaymentReportRequest` (invoiceId, mpesaMessage)

---

## 4. Document & Asset Vault

### List Reports
*   **Endpoint:** `GET /api/users/my-reports`
*   **Description:** Lists all project reports and PDF documents available for download.

### Download Report
*   **Endpoint:** `GET /api/users/my-reports/{id}/download`
*   **Description:** Streams the PDF/Docx file content.
*   **Response Type:** `application/octet-stream`.

### Download Invoice PDF
*   **Endpoint:** `GET /api/users/my-invoices/{id}/pdf`
*   **Description:** Generates and streams a PDF version of the specified invoice.

### Upload Asset
*   **Endpoint:** `POST /api/reports/upload-asset` (Multipart)
*   **Description:** Allows uploading project-related assets (images/logs).

---

## 5. Communication & Feedback

### Submit Feedback / AI Telemetry
*   **Endpoint:** `POST /api/users/client-feedback`
*   **Description:** Transmits automated AI issue reports or manual user feedback.
*   **Payload:** `FeedbackRequest` (title, message, type, rating, priority)

### Get Feedback History
*   **Endpoint:** `GET /api/users/client-feedback`
*   **Description:** Retrieves past feedback and reported system anomalies for the client.

---

## 6. Strategic Requests (Quotes & Signatures)

### Quotes
*   **Endpoint:** `GET /api/users/my-quotes`
*   **Action:** `POST /api/users/my-quotes/{id}/decision` (accept/reject)

### Signature Requests
*   **Endpoint:** `GET /api/users/my-signature-requests`
*   **Action:** `POST /api/users/my-signature-requests/{id}/decision`

### Change Requests
*   **Endpoint:** `GET /api/users/my-change-requests`
*   **Submission:** `POST /api/users/my-change-requests`

---

## 7. Notifications & Infrastructure

### Get Notifications
*   **Endpoint:** `GET /api/users/notifications/me`
*   **Description:** Fetches all unread and archived alerts.

### Mark as Read
*   **Endpoint:** `PUT /api/users/notifications/{id}/read`
*   **Endpoint:** `PUT /api/users/notifications/read-all/me`

### Update Push Token
*   **Endpoint:** `POST /api/users/push-token`
*   **Description:** Updates the Firebase FCM token for the device.
