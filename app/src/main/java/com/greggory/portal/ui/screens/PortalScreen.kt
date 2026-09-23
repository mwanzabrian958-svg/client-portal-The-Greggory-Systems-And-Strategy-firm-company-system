package com.greggory.portal.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.greggory.portal.R
import com.greggory.portal.data.api.*
import com.greggory.portal.data.local.*
import com.greggory.portal.ui.components.CustomBackground
import com.greggory.portal.ui.components.SectionHeader
import com.greggory.portal.utils.DataRouter
import com.greggory.portal.utils.UpdateInfo
import com.greggory.portal.utils.UpdateManager
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(
    onLogout: () -> Unit, 
    onViewPdf: (String, String) -> Unit, 
    onViewProject: (Int) -> Unit,
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val preferencesManager = remember { PreferencesManager.getInstance(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val localProjects by database.projectDao().getAllProjects().collectAsState(initial = emptyList())
    val localInvoices by database.invoiceDao().getAllInvoices().collectAsState(initial = emptyList())
    val localReports by database.reportDao().getAllReports().collectAsState(initial = emptyList())

    val clientEmail = preferencesManager.getUserEmail() ?: ""
    val userEntity by database.userDao().getUserByEmail(clientEmail).collectAsState(initial = null)

    var dashboardData by remember { mutableStateOf<DashboardResponse?>(null) }
    var notificationsData by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var reportsData by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var currentView by remember { mutableStateOf("Home") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<SearchResults?>(null) }
    var isSearchLoading by remember { mutableStateOf(false) }
    
    var selectedProjectId by remember { mutableStateOf<Int?>(null) }

    // Continuous retry loop: If client's profile photo is missing in DB for clientEmail, keep calling API until one is available
    LaunchedEffect(clientEmail, userEntity?.profilePhotoData) {
        if (clientEmail.isNotEmpty()) {
            val photoData = userEntity?.profilePhotoData ?: preferencesManager.getUserPhotoData()
            if (photoData.isNullOrBlank()) {
                while (isActive) {
                    try {
                        val response = RetrofitClient.instance.getDashboard()
                        if (response.isSuccessful && response.body()?.success == true) {
                            val userObj = response.body()?.dashboard?.user
                            if (userObj != null) {
                                database.userDao().insertUser(userObj.toEntity())
                                preferencesManager.saveUserInfo(
                                    userObj.id,
                                    userObj.email,
                                    userObj.displayName ?: "${userObj.firstName} ${userObj.lastName ?: ""}".trim(),
                                    userObj.phone ?: "",
                                    userObj.primaryRole,
                                    userObj.missionBriefing,
                                    userObj.profilePhotoData
                                )
                                if (!userObj.profilePhotoData.isNullOrBlank()) {
                                    break // Profile photo successfully fetched!
                                }
                            }
                        }
                    } catch (ignored: Exception) {}
                    delay(4000)
                }
            }
        }
    }

    // Update States
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isDownloadingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val info = UpdateManager.checkForUpdates()
        if (info != null) {
            updateInfo = info
        }
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { if (!isDownloadingUpdate) updateInfo = null },
            title = { Text("Update Available") },
            text = {
                Column {
                    Text("A new version (${updateInfo!!.versionName}) of GSSF-client portal is available. This update includes:")
                    Spacer(modifier = Modifier.height(8.dp))
                    updateInfo!!.features.forEach { feature ->
                        Text("• $feature", style = MaterialTheme.typography.bodySmall)
                    }
                    if (isDownloadingUpdate) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { downloadProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Downloading update: ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDownloadingUpdate = true
                        scope.launch {
                            val success = UpdateManager.downloadAndInstall(context, updateInfo!!.url) { progress ->
                                downloadProgress = progress
                            }
                            if (!success) {
                                isDownloadingUpdate = false
                                Toast.makeText(context, "Update failed to download", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isDownloadingUpdate
                ) {
                    Text("INSTALL UPDATE")
                }
            },
            dismissButton = {
                if (!isDownloadingUpdate) {
                    TextButton(onClick = { updateInfo = null }) {
                        Text("LATER")
                    }
                }
            }
        )
    }

    BackHandler(enabled = isSearching || currentView != "Home" || selectedProjectId != null) {
        if (selectedProjectId != null) {
            selectedProjectId = null
        } else if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else {
            currentView = "Home"
        }
    }

    fun performSearch(query: String) {
        if (query.length < 2) {
            searchResults = null
            return
        }
        isSearchLoading = true
        scope.launch {
            try {
                val response = RetrofitClient.instance.search(query)
                if (response.isSuccessful) {
                    val results = response.body()?.results
                    val userId = preferencesManager.getUserId()
                    
                    // Validate search results integrity
                    val isSearchValid = 
                        (results?.projects?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true) &&
                        (results?.invoices?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true) &&
                        (results?.documents?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true)

                    if (isSearchValid) {
                        searchResults = results
                    } else {
                        errorMessage = "Search Security Breach: Results blocked."
                        scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                    }
                }
            } catch (ignored: Exception) {
                // handle error
            } finally {
                isSearchLoading = false
            }
        }
    }

    fun refreshData(isManual: Boolean = false) {
        val token = preferencesManager.getToken()
        if (token == null) {
            scope.launch {
                try { database.userDao().purgeAllTokens() } catch (ignored: Exception) {}
                try { database.clearAllTables() } catch (ignored: Exception) {}
                preferencesManager.clearToken()
                preferencesManager.clear()
                onLogout()
            }
            return
        }
        if (isManual) isRefreshing = true else isLoading = true
        scope.launch {
            try {
                val userId = preferencesManager.getUserId()
                
                kotlinx.coroutines.supervisorScope {
                    // Fetch all data in parallel (Dashboard, Reports, Notifications, and messages for offline reconciliation)
                    val dashDeferred = async { RetrofitClient.instance.getDashboard() }
                    val reportsDeferred = async { RetrofitClient.instance.getReports() }
                    val notifsDeferred = async { RetrofitClient.instance.getNotifications() }
                    val messagesDeferred = async { RetrofitClient.instance.getFeedback() } // Serves as sync fallback

                    val dashResponse = try { dashDeferred.await() } catch (ignored: Exception) { null }
                    val reportsResponse = try { reportsDeferred.await() } catch (ignored: Exception) { null }
                    val notifsResponse = try { notifsDeferred.await() } catch (ignored: Exception) { null }
                    val messagesResponse = try { messagesDeferred.await() } catch (ignored: Exception) { null }

                    var sessionExpired = false

                    // 1. Process Dashboard Response and sync with Local Cache
                    if (dashResponse?.isSuccessful == true && dashResponse.body()?.success == true) {
                        val body = dashResponse.body()?.dashboard
                        val isDashIntegrityValid = 
                            (body?.projects?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true) &&
                            (body?.invoices?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true)

                        if (isDashIntegrityValid) {
                            dashboardData = dashResponse.body()
                            errorMessage = null // Clear any previous breach error if dashboard is now valid
                            body?.user?.let { u ->
                                try {
                                    database.userDao().insertUser(u.toEntity())
                                } catch (ignored: Exception) {}
                            }
                            body?.projects?.let { projects ->
                                try {
                                    database.projectDao().clearProjects()
                                    database.projectDao().insertProjects(projects.map { it.toEntity() })
                                } catch (ignored: Exception) {}
                            }
                            body?.invoices?.let { invoices ->
                                try {
                                    database.invoiceDao().clearInvoices()
                                    database.invoiceDao().insertInvoices(invoices.map { it.toEntity() })
                                } catch (ignored: Exception) {}
                            }
                            // Reconcile and Sync missing messages/receipt items into Room Cache
                            body?.messages?.let { apiMessages ->
                                try {
                                    val messageEntities = apiMessages.map { msg ->
                                        MessageCacheEntity(
                                            id = msg.id,
                                            sender = msg.sender,
                                            subject = msg.subject,
                                            message = msg.message,
                                            time = msg.time,
                                            unread = msg.unread,
                                            feedback = msg.feedback ?: false,
                                            attachmentUrl = null
                                        )
                                    }
                                    database.messageCacheDao().clearMessages()
                                    database.messageCacheDao().insertMessages(messageEntities)
                                } catch (ignored: Exception) {}
                            }
                        } else {
                            errorMessage = "Security: Dashboard routing mismatch for User $userId"
                        }
                    }

                    // 2. Process Reports Response
                    if (reportsResponse?.isSuccessful == true && reportsResponse.body()?.success == true) {
                        val reports = reportsResponse.body()?.reports ?: emptyList()
                        val isReportsIntegrityValid = reports.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) }
                        if (isReportsIntegrityValid) {
                            reportsData = reports
                            try {
                                database.reportDao().clearReports()
                                database.reportDao().insertReports(reports.map { it.toEntity() })
                            } catch (ignored: Exception) {}
                        } else {
                            errorMessage = "Security: Reports routing mismatch for User $userId"
                        }
                    }

                    // 3. Process Notifications Response
                    if (notifsResponse?.isSuccessful == true) {
                        notificationsData = notifsResponse.body()?.notifications ?: emptyList()
                    }

                    // Global validation: only treat token as fully expired if all attempts uniformly return 401
                    if (dashResponse?.code() == 401 && reportsResponse?.code() == 401) {
                        sessionExpired = true
                    }

                    if (sessionExpired) {
                        errorMessage = "Session expired. Please log in again."
                        scope.launch { 
                            snackbarHostState.showSnackbar(errorMessage!!)
                            kotlinx.coroutines.delay(1000)
                            try { database.userDao().purgeAllTokens() } catch (ignored: Exception) {}
                            try { database.clearAllTables() } catch (ignored: Exception) {}
                            preferencesManager.clearToken()
                            preferencesManager.clear()
                            onLogout()
                        }
                    } else if (errorMessage != null) {
                        scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.localizedMessage}"
                scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(key1 = true) {
        delay(100) // Small buffer to ensure Prefs Singleton is settled after login navigation
        refreshData()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    DrawerHeader(
                        user = dashboardData?.dashboard?.user ?: userEntity?.toUserInfo(),
                        profilePhotoData = userEntity?.profilePhotoData ?: preferencesManager.getUserPhotoData()
                    )
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    DrawerItem("Logout", Icons.AutoMirrored.Filled.Logout, false) {
                        scope.launch {
                            drawerState.close()
                            try { database.userDao().purgeAllTokens() } catch (ignored: Exception) {}
                            try { database.clearAllTables() } catch (ignored: Exception) {}
                            preferencesManager.clearToken()
                            preferencesManager.clear()
                            RetrofitClient.initialize(context)
                            onLogout()
                        }
                    }
                    
                    Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                    DrawerItem("My Profile", Icons.Default.Person, currentView == "Profile") {
                        currentView = "Profile"; scope.launch { drawerState.close() }
                    }
                    DrawerItem("App Settings", Icons.Default.Settings, currentView == "Settings") {
                        currentView = "Settings"; scope.launch { drawerState.close() }
                    }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                                isSearching = query.isNotEmpty()
                                performSearch(query)
                            },
                            placeholder = { Text("Search...", style = MaterialTheme.typography.bodyMedium) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        isSearching = false
                                        searchResults = null 
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 8.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        val userName = userEntity?.displayName ?: dashboardData?.dashboard?.user?.displayName ?: preferencesManager.getUserName() ?: "Client"
                        val initials = userName.split(" ")
                            .filter { it.isNotEmpty() }
                            .mapNotNull { it.firstOrNull()?.uppercase() }
                            .take(2)
                            .joinToString("")
                        var imageError by remember { mutableStateOf(false) }
                        val cachedPhotoData = userEntity?.profilePhotoData ?: preferencesManager.getUserPhotoData()

                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { currentView = "Profile" },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!imageError) {
                                val photoModel = when {
                                    !cachedPhotoData.isNullOrEmpty() -> {
                                        if (cachedPhotoData.startsWith("http") || cachedPhotoData.startsWith("data:")) cachedPhotoData else "${RetrofitClient.BASE_URL}$cachedPhotoData"
                                    }
                                    else -> "${RetrofitClient.BASE_URL}api/users/profile-photo/me"
                                }
                                val imageRequest = remember(photoModel, preferencesManager.getToken()) {
                                    val builder = coil.request.ImageRequest.Builder(context)
                                        .data(photoModel)
                                        .crossfade(true)
                                    if (preferencesManager.getToken() != null) {
                                        builder.addHeader("Authorization", "Bearer ${preferencesManager.getToken()}")
                                    }
                                    builder.build()
                                }
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onError = { imageError = true },
                                    onSuccess = { imageError = false }
                                )
                            }
                            
                            // Only show initials if the image failed or is still loading
                            if (imageError || initials.isEmpty()) {
                                Text(
                                    text = initials.ifEmpty { "G" },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                CustomBackground()

                if (isSearching) {
                    SearchScreen(
                        query = searchQuery,
                        results = searchResults,
                        isLoading = isSearchLoading,
                        onViewPdf = onViewPdf,
                        onNavigate = { currentView = it }
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { refreshData(isManual = true) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (currentView) {
                                "Home" -> HomeScreen(
                                    isLoading = isLoading, 
                                    dashboardData = dashboardData, 
                                    localProjects = localProjects, 
                                    localInvoices = localInvoices, 
                                    notifications = notificationsData,
                                    onNavigate = { currentView = it },
                                    onViewProject = { selectedProjectId = it },
                                    onNavigateToChat = onNavigateToChat
                                )
                                "Projects" -> ProjectListScreen(
                                    projects = dashboardData?.dashboard?.projects ?: localProjects.map { it.toApi() },
                                    onViewPdf = onViewPdf,
                                    onViewDetails = { selectedProjectId = it.id }
                                )
                                "Team" -> TeamScreen(dashboardData?.dashboard?.teamMembers ?: emptyList())
                                "Tasks" -> TasksScreen(dashboardData?.dashboard?.tasks ?: emptyList())
                                "Billing" -> BillingScreen(
                                    invoices = dashboardData?.dashboard?.invoices ?: localInvoices.map { it.toApi() },
                                    onViewPdf = onViewPdf
                                )
                                "Documents" -> ReportsScreen(
                                    reports = reportsData.ifEmpty { localReports.map { it.toApi() } },
                                    onViewPdf = onViewPdf
                                )
                                "Services" -> JobServicesScreen()
                                "Requests" -> RequestsScreen()
                                "Messages" -> ChatScreen(
                                    messages = dashboardData?.dashboard?.messages ?: emptyList(),
                                    onSendMessage = { text ->
                                        Toast.makeText(context, "Message Sent: $text", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                "Feedback" -> FeedbackScreen()
                                "Notifications" -> NotificationsScreen(notificationsData)
                                "Profile" -> ProfileScreen(dashboardData)
                                "Settings" -> SettingsScreen(
                                    onLogout = onLogout,
                                    onNavigateToProfile = { currentView = "Profile" },
                                    onNavigateToChat = onNavigateToChat,
                                    dashboardData = dashboardData
                                )
                                else -> Text("Section: $currentView", modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }

                    if (selectedProjectId != null) {
                        val project = dashboardData?.dashboard?.projects?.find { it.id == selectedProjectId }
                        ProjectDetailsScreen(
                            project = project,
                            team = dashboardData?.dashboard?.teamMembers ?: emptyList(),
                            onBack = { selectedProjectId = null }
                        )
                    }
                }
                
                if (isLoading && currentView == "Home" && dashboardData == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }

                if (errorMessage != null && dashboardData == null && !isLoading) {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(errorMessage!!, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { refreshData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry Sync")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerHeader(user: UserInfo?, profilePhotoData: String? = null) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    val initials = user?.let {
        (it.displayName ?: "${it.firstName} ${it.lastName ?: ""}").split(" ")
            .filter { part -> part.isNotEmpty() }
            .mapNotNull { part -> part.firstOrNull()?.uppercase() }
            .take(2)
            .joinToString("")
    } ?: "G"

    Column(modifier = Modifier.padding(28.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            var imageError by remember { mutableStateOf(false) }
            val photoData = profilePhotoData ?: user?.profilePhotoData
            val photoModel = when {
                !photoData.isNullOrBlank() -> {
                    if (photoData.startsWith("http") || photoData.startsWith("data:")) photoData else "${RetrofitClient.BASE_URL}$photoData"
                }
                else -> "${RetrofitClient.BASE_URL}api/users/profile-photo/me"
            }
            if (!imageError) {
                val imageRequest = remember(photoModel, prefs.getToken()) {
                    val builder = coil.request.ImageRequest.Builder(context)
                        .data(photoModel)
                        .crossfade(true)
                    if (prefs.getToken() != null) {
                        builder.addHeader("Authorization", "Bearer ${prefs.getToken()}")
                    }
                    builder.build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Profile Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageError = true },
                    onSuccess = { imageError = false }
                )
            }
            if (imageError || initials.isEmpty()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (user != null) {
            Text(
                text = user.displayName ?: user.firstName,
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = user.email, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "THE GREGGORY", 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Systems & Strategy Firm", 
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun HomeScreen(
    isLoading: Boolean,
    dashboardData: DashboardResponse?,
    localProjects: List<ProjectEntity>,
    localInvoices: List<InvoiceEntity>,
    notifications: List<Notification>,
    onNavigate: (String) -> Unit,
    onViewProject: (Int) -> Unit,
    onNavigateToChat: () -> Unit
) {
    val displayProjects = dashboardData?.dashboard?.projects ?: localProjects.map { it.toApi() }
    val displayInvoices = dashboardData?.dashboard?.invoices ?: localInvoices.map { it.toApi() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        if (isLoading && dashboardData == null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }
        
        // Mission Briefing Section
        dashboardData?.dashboard?.user?.missionBriefing?.let { briefing ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MISSION BRIEFING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(briefing, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        HomeKpiSection(dashboardData, onNavigate, onNavigateToChat)

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Operations Hub", Icons.Default.Apps)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("Active Projects", Icons.Default.BusinessCenter, Modifier.weight(1f)) { onNavigate("Projects") }
                QuickActionCard("Project Roster", Icons.Default.Groups, Modifier.weight(1f)) { onNavigate("Team") }
                QuickActionCard("Milestones", Icons.AutoMirrored.Filled.Assignment, Modifier.weight(1f)) { onNavigate("Tasks") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("Financials", Icons.Default.Payments, Modifier.weight(1f)) { onNavigate("Billing") }
                QuickActionCard("Document Vault", Icons.Default.Folder, Modifier.weight(1f)) { onNavigate("Documents") }
                QuickActionCard("Requests", Icons.Default.Assessment, Modifier.weight(1f)) { onNavigate("Requests") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionCard("Direct Strategy Support", Icons.AutoMirrored.Filled.Chat, Modifier.weight(1f)) { onNavigateToChat() }
                QuickActionCard("Request Job", Icons.Default.AddCircle, Modifier.weight(1f)) { onNavigate("Services") }
                QuickActionCard("Feedback", Icons.Default.RateReview, Modifier.weight(1f)) { onNavigate("Feedback") }
            }
        }
        
        // Budget & Financial Forecast
        dashboardData?.dashboard?.budgetOverview?.let { budget ->
            Spacer(modifier = Modifier.height(24.dp))
            SectionHeader("Financial Forecast", Icons.AutoMirrored.Filled.TrendingUp)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("PLANNED", style = MaterialTheme.typography.labelSmall)
                            Text("KSH ${budget.planned.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("SPENT", style = MaterialTheme.typography.labelSmall)
                            Text("KSH ${budget.spent.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Simple Visual Bar Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxVal = maxOf(budget.planned, budget.spent).coerceAtLeast(1.0)
                        val plannedRatio = (budget.planned / maxVal).toFloat()
                        val spentRatio = (budget.spent / maxVal).toFloat()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(plannedRatio)
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(spentRatio)
                                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("PLANNED", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SPENT", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (budget.spent / budget.planned.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (budget.variance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Variance: ${budget.variance}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Active Projects", Icons.Default.BusinessCenter)
        ProjectsSummaryList(displayProjects.take(3), onViewProject)
        
        // Section Header "Milestone Tasks" with AutoMirrored icon
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Milestone Tasks", Icons.AutoMirrored.Filled.Assignment)
        TasksSummaryList(dashboardData?.dashboard?.tasks?.take(3) ?: emptyList(), onNavigate)

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Recent Invoices", Icons.Default.Payments)
        InvoicesSummaryList(displayInvoices.take(3), onNavigate)
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Latest Updates", Icons.Default.Notifications)
        
        // Show Live Feed (Messages)
        dashboardData?.dashboard?.messages?.take(3)?.forEach { message ->
            MessageFeedItem(message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notifications.isEmpty() && (dashboardData?.dashboard?.messages).isNullOrEmpty()) {
            Text("No recent updates", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MessageFeedItem(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (message.unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(message.sender, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.weight(1f))
                Text(message.time, style = MaterialTheme.typography.labelSmall)
            }
            Text(message.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(message.message, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
fun DirectStrategySupportCard(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.SupportAgent, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DIRECT STRATEGY SUPPORT", 
                    style = MaterialTheme.typography.labelLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SupportChannelItem(Icons.Default.Chat, "Chat", MaterialTheme.colorScheme.primary) {
                    onNavigateToChat()
                }
                SupportChannelItem(Icons.Default.Phone, "Call", Color(0xFF4CAF50)) {
                    try { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+254115525854"))) } catch (e: Exception) { Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show() }
                }
                SupportChannelItem(Icons.Default.ChatBubble, "WhatsApp", Color(0xFF25D366)) {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=254115525854"))) } catch (e: Exception) { Toast.makeText(context, "WhatsApp not found", Toast.LENGTH_SHORT).show() }
                }
                SupportChannelItem(Icons.Default.Sms, "SMS", Color(0xFF2196F3)) {
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("sms:+254115525854"))) } catch (e: Exception) { Toast.makeText(context, "SMS app not found", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
}

@Composable
fun SupportChannelItem(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.padding(14.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HomeKpiSection(
    dashboardData: DashboardResponse?, 
    onNavigate: (String) -> Unit,
    onNavigateToChat: () -> Unit
) {
    val summary = dashboardData?.dashboard?.businessSummary
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiCard("Active Projects", summary?.activeProjects?.toString() ?: "0", Modifier.weight(1f).clickable { onNavigate("Projects") })
        KpiCard("Open Invoices", summary?.openInvoices?.toString() ?: "0", Modifier.weight(1f).clickable { onNavigate("Billing") })
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiCard("Direct Strategy Support", "24/7 Active Lead", Modifier.weight(1f).clickable { onNavigateToChat() })
        KpiCard("Next Milestone", summary?.nextMilestone ?: "Syncing...", Modifier.weight(1f).clickable { onNavigate("Tasks") })
    }

    DirectStrategySupportCard(onNavigateToChat = onNavigateToChat)

    dashboardData?.dashboard?.kpiMetrics?.forEach { metric ->
        val label = metric.label
        if (!label.equals("On-time Delivery", ignoreCase = true) && !label.equals("On Time Delivery", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(8.dp))
            KpiCard(label, metric.value, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ProjectsSummaryList(
    projects: List<Project>, 
    onViewProject: (Int) -> Unit
) {
    if (projects.isEmpty()) {
        Text("No active projects", style = MaterialTheme.typography.bodySmall)
    } else {
        projects.forEach { project ->
            Card(
                onClick = { onViewProject(project.id) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(project.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(project.status, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${project.progress}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun TasksSummaryList(tasks: List<Task>, onNavigate: (String) -> Unit) {
    if (tasks.isEmpty()) {
        Text("No active milestones", style = MaterialTheme.typography.bodySmall)
    } else {
        tasks.forEach { task ->
            Card(
                onClick = { onNavigate("Tasks") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(task.project, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("${task.progress}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun InvoicesSummaryList(invoices: List<Invoice>, onNavigate: (String) -> Unit) {
    if (invoices.isEmpty()) {
        Text("No pending invoices", style = MaterialTheme.typography.bodySmall)
    } else {
        invoices.forEach { invoice ->
            Card(
                onClick = { onNavigate("Billing") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invoice #${invoice.id}", style = MaterialTheme.typography.bodyLarge)
                        InvoiceStatusBadge(invoice.status)
                    }
                    Text("KSH ${invoice.amount.toInt()}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(notification.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(notification.message, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
fun QuickActionCard(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
