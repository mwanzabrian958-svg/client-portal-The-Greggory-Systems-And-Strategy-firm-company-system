# 🖱️ Complete Inventory of Clickables & Interactive Elements

This document provides a comprehensive list of every interactive element, button, form input, dialog, picker, and navigation trigger across all 12 core screens of **The Greggory Client Portal**.

---

## 1. Login Screen (`LoginScreen.kt`)
*   **Email Address Field:** Text input for client credentials.
*   **Password Field:** Secure password input (masked).
*   **Sign In Button:** Authenticates credentials against the backend API and stores secure session tokens.
*   **Biometric Authentication Button:** Triggers hardware biometric prompt (Fingerprint / Face ID).
*   **Forgot Password Link:** Navigates to password recovery modal/screen.
*   **Signup Navigation Link:** Redirects new users to the account creation screen.

## 2. Signup Screen (`SignupScreen.kt`)
*   **First Name & Last Name Fields:** Text inputs.
*   **Email & Phone Number Fields:** Contact detail inputs.
*   **Password Field:** Secure password input.
*   **Profile Photo Picker Launcher:** Tapping camera icon opens system image gallery to select an initial profile picture.
*   **Register Button:** Submits registration payload (including base64 profile photo) to backend.
*   **Login Redirect Link:** Returns existing users to the login screen.

## 3. Portal / Dashboard Screen (`PortalScreen.kt`)
*   **Navigation Drawer Hamburger Icon:** Opens side menu (Drawer).
*   **Drawer Header Profile Photo & Name:** Displays live synchronized user avatar and credentials.
*   **KPI Metric Cards:** Interactive summary cards displaying active projects, invoices, and financial standing.
*   **Quick Action Grid:** Shortcut buttons to Billing, Reports, Requests, and Search.
*   **Direct Strategy Support Card (1-Tap Channels):**
    *   *Chat Button:* Opens in-app Strategy Chat.
    *   *WhatsApp Button:* Launches direct WhatsApp advisory channel.
    *   *SMS Button:* Opens native SMS messaging.
    *   *Phone Call Button:* Initiates direct call to lead strategist.
*   **Bottom Navigation / Screen Selector:** Switches between Portal Home, Messages, Billing, and Settings.

## 4. Profile Screen (`ProfileScreen.kt`)
*   **Profile Avatar & Camera Overlay:** Tapping opens system image picker (`image/*`) to update profile photo.
*   **Full Name Text Field:** Editable input field.
*   **Email Address Field:** Secured read-only display.
*   **Phone Number Text Field:** Editable input field.
*   **Save Changes Button:** Sends `ProfileUpdateRequest` to backend and updates local SQLite Room cache & `PreferencesManager`.
*   **Change Password Button:** Opens `ChangePasswordDialog` modal.
*   **Change Password Modal:** Old password, new password fields, and submit/cancel buttons.

## 5. Strategy Chat Screen (`StrategyChatScreen.kt`)
*   **Back Navigation Button:** Returns to previous screen.
*   **Message Input Field:** Multi-line text field for typing strategy messages.
*   **Attach File Button (`📎`):** Launches system media picker supporting both **images** (`image/*`) and **videos** (`video/*`).
*   **Media Attachment Preview Card:** Displays selected image/video thumbnail preview before sending, with a **Remove (`✕`)** button.
*   **Send Floating Action Button (FAB):** Encodes attachment to base64, submits `SendChatRequest` to backend, and triggers optimistic UI update + automated strategist reply.
*   **Chat Bubbles:** Display text, embedded preview images, and video player play-icon badges with timestamps.

## 6. Billing & Invoices Screen (`BillingScreen.kt`)
*   **Invoice List Items:** Expandable invoice details.
*   **Download PDF Button:** Streams and downloads official invoice PDF receipts.
*   **Pay Now Button:** Triggers M-Pesa STK Push payment dialog.
*   **M-Pesa Payment Dialog:** Phone number input, amount display, and STK push confirmation / status polling.
*   **Report Payment Issue Button:** Submits transaction dispute / discrepancy reports.

## 7. Reports Screen (`ReportsScreen.kt`)
*   **Report List Items:** Selectable enterprise reports and deliverables.
*   **Download Report Button:** Streams secure file downloads via download manager.
*   **Upload Asset Button:** Opens file picker to upload supporting project documents or assets.

## 8. Requests Screen (`RequestsScreen.kt`)
*   **Status Tabs:** Switch between Pending, Active, and Completed service requests.
*   **Request Action Buttons:** Approve, Reject, or Decline requests.
*   **Electronic Signature Confirmation Dialog:** Modal prompting user to sign/confirm agreements.
*   **New Request Floating Action Button (FAB):** Opens request creation dialog.
*   **Service Type & Priority Dropdowns:** Selectors for configuring new service requests.

## 9. Job Services Screen (`JobServicesScreen.kt`)
*   **Service Catalog Cards:** Expandable service offerings.
*   **Engagement / Booking Buttons:** Request new job services or consultations.
*   **Project Progress Timeline:** Interactive progress indicators.

## 10. Settings Screen (`SettingsScreen.kt`)
*   **Biometric Security Toggle:** Enable/disable biometric login protection.
*   **Theme / Appearance Selector:** Switch between light and dark modes.
*   **Clear Local Cache Button:** Clears Room database cache and temporary files.
*   **Logout Button:** Triggers nuclear token purge (wipes SQLite tokens and encrypted prefs) and redirects to login.

## 11. Notifications Screen (`NotificationsScreen.kt`)
*   **Notification Items:** Tap to view alert details or mark as read.
*   **Clear All Button:** Dismisses all system notifications.

## 12. Team Directory Screen (`TeamScreen.kt`)
*   **Team Member Cards:** Tap to view advisory roles and background.
*   **Direct Contact Links:** 1-tap email and messaging triggers for individual strategists.
