package com.greggory.portal.ui.screens

import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
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
import com.greggory.portal.utils.DataRouter
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(onLogout: () -> Unit, onViewPdf: (String, String) -> Unit, onViewProject: (Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val preferencesManager = remember { PreferencesManager.getInstance(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val localProjects by database.projectDao().getAllProjects().collectAsState(initial = emptyList())
    val localInvoices by database.invoiceDao().getAllInvoices().collectAsState(initial = emptyList())
    val localReports by database.reportDao().getAllReports().collectAsState(initial = emptyList())

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
            onLogout()
            return
        }
        if (isManual) isRefreshing = true else isLoading = true
        scope.launch {
            try {
                val userId = preferencesManager.getUserId()
                
                kotlinx.coroutines.supervisorScope {
                    // Fetch all data in parallel
                    val dashDeferred = async { RetrofitClient.instance.getDashboard() }
                    val reportsDeferred = async { RetrofitClient.instance.getReports() }
                    val notifsDeferred = async { RetrofitClient.instance.getNotifications() }

                    val dashResponse = try { dashDeferred.await() } catch (ignored: Exception) { null }
                    val reportsResponse = try { reportsDeferred.await() } catch (ignored: Exception) { null }
                    val notifsResponse = try { notifsDeferred.await() } catch (ignored: Exception) { null }

                    var sessionExpired = false

                    // 1. Process Dashboard Response
                    if (dashResponse?.isSuccessful == true && dashResponse.body()?.success == true) {
                        val body = dashResponse.body()?.dashboard
                        val isDashIntegrityValid = 
                            (body?.projects?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true) &&
                            (body?.invoices?.all { DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true)

                        if (isDashIntegrityValid) {
                            dashboardData = dashResponse.body()
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
                        } else {
                            errorMessage = "Security Error: Dashboard Routing Integrity Breach Detected"
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
                            errorMessage = "Security Error: Reports Routing Integrity Breach Detected"
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
                DrawerHeader()
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                DrawerItem("Home", Icons.Default.Dashboard, currentView == "Home") {
                    currentView = "Home"; scope.launch { drawerState.close() }
                }
                
                Text("DEEP DIVES", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                DrawerItem("Active Projects", Icons.Default.BusinessCenter, currentView == "Projects") {
                    currentView = "Projects"; scope.launch { drawerState.close() }
                }
                DrawerItem("Project Roster", Icons.Default.Groups, currentView == "Team") {
                    currentView = "Team"; scope.launch { drawerState.close() }
                }
                DrawerItem("Milestone Tasks", Icons.AutoMirrored.Filled.Assignment, currentView == "Tasks") {
                    currentView = "Tasks"; scope.launch { drawerState.close() }
                }
                DrawerItem("Financial Ledger", Icons.Default.Payments, currentView == "Billing") {
                    currentView = "Billing"; scope.launch { drawerState.close() }
                }
                DrawerItem("Document Vault", Icons.Default.Folder, currentView == "Documents") {
                    currentView = "Documents"; scope.launch { drawerState.close() }
                }
                DrawerItem("Requests & Quotes", Icons.Default.Assessment, currentView == "Requests") {
                    currentView = "Requests"; scope.launch { drawerState.close() }
                }
                DrawerItem("Support Inbox", Icons.Default.Email, currentView == "Messages") {
                    currentView = "Messages"; scope.launch { drawerState.close() }
                }
                
                Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                DrawerItem("My Profile", Icons.Default.Person, currentView == "Profile") {
                    currentView = "Profile"; scope.launch { drawerState.close() }
                }
                DrawerItem("App Settings", Icons.Default.Settings, currentView == "Settings") {
                    currentView = "Settings"; scope.launch { drawerState.close() }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider()
                DrawerItem("Logout", Icons.AutoMirrored.Filled.Logout, false) {
                    scope.launch {
                        preferencesManager.clear()
                        database.projectDao().clearProjects()
                        database.invoiceDao().clearInvoices()
                        database.reportDao().clearReports()
                        onLogout()
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
                        val userName = preferencesManager.getUserName() ?: "U"
                        val initials = userName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString("")
                        
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { currentView = "Profile" },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "${RetrofitClient.BASE_URL}api/users/profile-photo/me",
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = null // Fallback to text below
                            )
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
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
                        when (currentView) {
                            "Home" -> HomeScreen(
                        isLoading = isLoading, 
                        dashboardData = dashboardData, 
                        localProjects = localProjects, 
                        localInvoices = localInvoices, 
                        notifications = notificationsData,
                        onNavigate = { currentView = it },
                        onViewProject = { selectedProjectId = it }
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
                                reports = if (reportsData.isNotEmpty()) reportsData else localReports.map { it.toApi() },
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
                            "Profile" -> ProfileScreen()
                            "Settings" -> SettingsScreen(
                                onLogout = onLogout,
                                onNavigateToProfile = { currentView = "Profile" }
                            )
                            else -> Text("Section: $currentView", modifier = Modifier.align(Alignment.Center))
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
fun DrawerHeader() {
    Column(modifier = Modifier.padding(28.dp)) {
        Image(painter = painterResource(id = R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("THE GREGGORY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Systems & Strategy Firm", style = MaterialTheme.typography.labelSmall)
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
    onViewProject: (Int) -> Unit
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

        HomeKpiSection(dashboardData, onNavigate)

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Quick Actions", Icons.Default.FlashOn)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val modifier = Modifier.weight(1f)
            QuickActionCard("Request Job", Icons.Default.AddCircle, modifier) { onNavigate("Services") }
            QuickActionCard("Pay Invoice", Icons.Default.Payment, modifier) { onNavigate("Billing") }
            QuickActionCard("Feedback", Icons.Default.RateReview, modifier) { onNavigate("Feedback") }
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
        ProjectsSummaryList(displayProjects.take(3), onNavigate, onViewProject)
        
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
fun HomeKpiSection(dashboardData: DashboardResponse?, onNavigate: (String) -> Unit) {
    val summary = dashboardData?.dashboard?.businessSummary
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val kpiModifier = Modifier.weight(1f)
        KpiCard("Active Projects", summary?.activeProjects?.toString() ?: "0", kpiModifier.clickable { onNavigate("Projects") })
        KpiCard("Open Invoices", summary?.openInvoices?.toString() ?: "0", kpiModifier.clickable { onNavigate("Billing") })
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val kpiModifier = Modifier.weight(1f)
        KpiCard("Open Messages", summary?.openMessages?.toString() ?: "0", kpiModifier)
        KpiCard("Next Milestone", summary?.nextMilestone ?: "Syncing...", kpiModifier.clickable { onNavigate("Tasks") })
    }

    dashboardData?.dashboard?.kpiMetrics?.forEach { metric ->
        Spacer(modifier = Modifier.height(8.dp))
        KpiCard(metric.label, metric.value, Modifier.fillMaxWidth())
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProjectsSummaryList(
    projects: List<Project>, 
    onNavigate: (String) -> Unit,
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
                    val contentModifier = Modifier.weight(1f)
                    Column(modifier = contentModifier) {
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
                    val contentModifier = Modifier.weight(1f)
                    Column(modifier = contentModifier) {
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
                    val contentModifier = Modifier.weight(1f)
                    Column(modifier = contentModifier) {
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
