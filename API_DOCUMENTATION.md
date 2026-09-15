# API Reference Documentation

**Base URL:** `https://w-the-greggory-systems-and-strategy-firm-vik4.onrender.com/`

## 1. Authentication

### Register
*   **Endpoint:** `POST /api/users/register`
*   **Description:** Creates a new client account. Supports Base64 profile photo uploads.
*   **Payload:** `RegisterRequest` (first_name, last_name, email, phone, password, profile_photo_base64)

### Login
*   **Endpoint:** `POST /api/users/login`
*   **Description:** Authenticates user and returns a terminal-specific JWT token.
*   **Headers Required:** None.

---

## 2. Client Dashboard & Projects

### Get Dashboard Data
*   **Endpoint:** `GET /api/users/client-dashboard`
*   **Description:** Retrieves summary statistics, recent projects, and active invoices.
*   **Routing:** Automatically filtered by `X-Greggory-Client-ID`.

### Get Project Ledger
*   **Endpoint:** `GET /api/user-projects`
*   **Description:** Returns a full list of all projects (active and archived) for the client.

---

## 3. Financial Hub (M-Pesa)

### Initiate STK Push
*   **Endpoint:** `POST /api/mpesa/stkpush`
*   **Description:** Triggers a payment request on the client's phone.
*   **Payload:** `MpesaStkPushRequest` (amount, phoneNumber, accountReference)

### Check Payment Status
*   **Endpoint:** `GET /api/mpesa/status/{checkoutRequestId}`
*   **Description:** Polls for the final result of an initiated M-Pesa transaction.

---

## 4. Document Vault

### List Reports
*   **Endpoint:** `GET /api/users/my-reports`
*   **Description:** Lists all project reports and PDF documents available for download.

### Download Report
*   **Endpoint:** `GET /api/users/my-reports/{id}/download`
*   **Description:** Streams the PDF/Docx file content.
*   **Response Type:** `application/octet-stream` (LONGBLOB from DB).

---

## 5. Security & System

### Update Push Token
*   **Endpoint:** `POST /api/users/push-token`
*   **Description:** Updates the Firebase FCM token for push notifications.

### Profile Update
*   **Endpoint:** `PUT /api/users/profile`
*   **Description:** Updates client display name and phone number.
