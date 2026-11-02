# The Greggory Client Portal - App Description

### What the App Is
The Greggory Client Portal is a comprehensive enterprise client engagement and management application built for The Greggory Systems and Strategy Firm. It acts as a secure digital hub where registered clients can seamlessly interact with the firm, oversee active consulting and technical projects, track milestones and tasks, review financial billing and invoices, submit service requests, access documents and reports, and communicate directly through an integrated strategy chat interface.

### How the App Works
Under the hood, the application is engineered using modern Android development standards, leveraging Jetpack Compose for a declarative and responsive user interface. It communicates securely with backend services via Retrofit (with custom OkHttpClient interceptors attaching authentication tokens) and caches data locally using Room and SharedPreferences. Core utility services—such as Firebase Messaging for push notifications, Crashlytics for error monitoring, Coil for authenticated image loading, and biometric authentication helpers—work in concert to provide a reliable, secure, and production-grade client experience.

### How MainActivity Works
The entry point [`MainActivity`](file:///app/src/main/java/com/greggory/portal/MainActivity.kt) extends `FragmentActivity` and orchestrates the application's lifecycle and global state. Upon creation (`onCreate`), it installs the native Android splash screen, initializes persistent preference managers, and listens for theme adjustments (`dark` or `light` mode). It also acts as a security gateway by purging stale or default demo tokens to ensure only authenticated users retain session access, requests push notification permissions, and sets up the root Compose content tree anchored by `GreggoryPortalTheme`.

### Navigation and Flow
Within `MainActivity`, the app evaluates the client's current session state to determine the appropriate starting destination—routing first-time users to `Onboarding`, unauthenticated users to `Login`, and active clients directly to the main `Portal`. The navigation flow is managed by Jetpack Navigation Compose (`AppNavigation`), which links authentication screens, password recovery, OTP verification, PDF document viewing, and the central `PortalScreen` dashboard together while continuously monitoring network connectivity and handling unauthorized session timeouts globally via `SessionEventBus`.
