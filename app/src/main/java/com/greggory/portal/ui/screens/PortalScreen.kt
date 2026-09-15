package com.greggory.portal.ui.screens

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.R
import com.greggory.portal.data.api.Project
import com.greggory.portal.data.api.Invoice
import com.greggory.portal.data.api.Notification
import com.greggory.portal.data.api.Report
import com.greggory.portal.data.api.Message
import com.greggory.portal.data.local.AppDatabase
import com.greggory.portal.data.local.toApi
import com.greggory.portal.data.local.toEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val preferencesManager = remember { com.greggory.portal.data.local.PreferencesManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val localProjects by database.projectDao().getAllProjects().collectAsState(initial = emptyList())
    val localInvoices by database.invoiceDao().getAllInvoices().collectAsState(initial = emptyList())
    val localReports by database.reportDao().getAllReports().collectAsState(initial = emptyList())

    var dashboardData by remember { mutableStateOf<com.greggory.portal.data.api.DashboardResponse?>(null) }
    var notificationsData by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var reportsData by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var currentView by remember { mutableStateOf("Home") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<com.greggory.portal.data.api.SearchResults?>(null) }
    var isSearchLoading by remember { mutableStateOf(false) }

    fun performSearch(query: String) {
        if (query.length < 2) {
            searchResults = null
            return
        }
        isSearchLoading = true
        scope.launch {
            try {
                val response = com.greggory.portal.data.api.RetrofitClient.instance.search(query)
                if (response.isSuccessful) {
                    searchResults = response.body()?.results
                }
            } catch (e: Exception) {
                // handle error
            } finally {
                isSearchLoading = false
            }
        }
    }

    fun refreshData() {
        val token = preferencesManager.getToken()
        if (token == null) {
            onLogout()
            return
        }
        isLoading = true
        scope.launch {
            try {
                val userId = preferencesManager.getUserId()
                
                // Fetch all data in parallel
                val dashDeferred = async { com.greggory.portal.data.api.RetrofitClient.instance.getDashboard() }
                val reportsDeferred = async { com.greggory.portal.data.api.RetrofitClient.instance.getReports() }
                val notifsDeferred = async { com.greggory.portal.data.api.RetrofitClient.instance.getNotifications() }

                val dashResponse = dashDeferred.await()
                val reportsResponse = reportsDeferred.await()
                val notifsResponse = notifsDeferred.await()

                if (dashResponse.isSuccessful && dashResponse.body()?.success == true) {
                    val body = dashResponse.body()?.dashboard
                    val reports = reportsResponse.body()?.reports ?: emptyList()
                    
                    // Validate Set in Stone Routing Integrity for all incoming data
                    val isIntegrityValid = 
                        (body?.projects?.all { com.greggory.portal.utils.DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true) &&
                        (body?.invoices?.all { com.greggory.portal.utils.DataRouter.verifyRoutingIntegrity(it.clientId, userId) } ?: true) &&
                        (reports.all { com.greggory.portal.utils.DataRouter.verifyRoutingIntegrity(it.clientId, userId) })

                    if (isIntegrityValid) {
                        dashboardData = dashResponse.body()
                        reportsData = reports
                        notificationsData = notifsResponse.body()?.notifications ?: emptyList()
                        
                        // Sync Local DB
                        body?.projects?.let { projects ->
                            database.projectDao().clearProjects()
                            database.projectDao().insertProjects(projects.map { it.toEntity() })
                        }
                        body?.invoices?.let { invoices ->
                            database.invoiceDao().clearInvoices()
                            database.invoiceDao().insertInvoices(invoices.map { it.toEntity() })
                        }
                        if (reports.isNotEmpty()) {
                            database.reportDao().clearReports()
                            database.reportDao().insertReports(reports.map { it.toEntity() })
                        }
                    } else {
                        errorMessage = "Security Error: Routing Integrity Breach Detected"
                        scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                    }
                } else if (dashResponse.code() == 401 || dashResponse.code() == 403) {
                    errorMessage = "Session Expired"
                    scope.launch { 
                        preferencesManager.clear()
                        onLogout()
                    }
                } else if (!dashResponse.isSuccessful) {
                    errorMessage = "Server error: ${dashResponse.code()}"
                    scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
                }
            } catch (e: Exception) {
                errorMessage = "Network error: ${e.localizedMessage}"
                scope.launch { snackbarHostState.showSnackbar(errorMessage!!) }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(key1 = true) {
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
                DrawerItem("Full Project Ledger", Icons.Default.BusinessCenter, currentView == "Projects") {
                    currentView = "Projects"; scope.launch { drawerState.close() }
                }
                DrawerItem("Project Roster", Icons.Default.Groups, currentView == "Team") {
                    currentView = "Team"; scope.launch { drawerState.close() }
                }
                DrawerItem("Operational Tasks", Icons.Default.AssignmentTurnedIn, currentView == "Tasks") {
                    currentView = "Tasks"; scope.launch { drawerState.close() }
                }
                DrawerItem("Financial Archive", Icons.Default.Payments, currentView == "Billing") {
                    currentView = "Billing"; scope.launch { drawerState.close() }
                }
                DrawerItem("Document Vault", Icons.Default.Folder, currentView == "Documents") {
                    currentView = "Documents"; scope.launch { drawerState.close() }
                }
                DrawerItem("Explore Services", Icons.Default.AddBusiness, currentView == "Services") {
                    currentView = "Services"; scope.launch { drawerState.close() }
                }
                DrawerItem("Requests & Quotes", Icons.AutoMirrored.Filled.Assignment, currentView == "Requests") {
                    currentView = "Requests"; scope.launch { drawerState.close() }
                }
                DrawerItem("Client Feedback", Icons.Default.Feedback, currentView == "Feedback") {
                    currentView = "Feedback"; scope.launch { drawerState.close() }
                }
                DrawerItem("Notifications", Icons.Default.Notifications, currentView == "Notifications") {
                    currentView = "Notifications"; scope.launch { drawerState.close() }
                }
                
                Text("ACCOUNT", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                DrawerItem("My Profile", Icons.Default.Person, currentView == "Profile") {
                    currentView = "Profile"; scope.launch { drawerState.close() }
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
            topBar = {
                TopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    performSearch(it)
                                },
                                placeholder = { Text("Search projects, invoices...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(if(currentView == "Home") "MISSION CONTROL" else currentView.uppercase(), fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = !isSearching; if(!isSearching) searchQuery = "" }) {
                            Icon(if(isSearching) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                        }
                        if (currentView == "Home") {
                            IconButton(onClick = { refreshData() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (isSearching) {
                    SearchScreen(searchQuery, searchResults, isSearchLoading)
                } else {
                    when (currentView) {
                        "Home" -> HomeScreen(
                        isLoading = isLoading, 
                        dashboardData = dashboardData, 
                        localProjects = localProjects, 
                        localInvoices = localInvoices, 
                        notifications = notificationsData,
                        onNavigate = { currentView = it }
                    )
                        "Projects" -> ProjectListScreen(dashboardData?.dashboard?.projects ?: localProjects.map { it.toApi() })
                        "Team" -> TeamScreen(dashboardData?.dashboard?.teamMembers ?: emptyList())
                        "Tasks" -> TasksScreen(dashboardData?.dashboard?.tasks ?: emptyList())
                        "Billing" -> BillingScreen(dashboardData?.dashboard?.invoices ?: localInvoices.map { it.toApi() })
                        "Documents" -> ReportsScreen(if (reportsData.isNotEmpty()) reportsData else localReports.map { it.toApi() })
                        "Services" -> JobServicesScreen()
                        "Requests" -> RequestsScreen()
                        "Feedback" -> FeedbackScreen()
                        "Notifications" -> NotificationsScreen(notificationsData)
                        "Profile" -> ProfileScreen()
                        else -> Text("Section: $currentView", modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                if (isLoading && currentView == "Home" && dashboardData == null) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
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
    dashboardData: com.greggory.portal.data.api.DashboardResponse?,
    localProjects: List<com.greggory.portal.data.local.ProjectEntity>,
    localInvoices: List<com.greggory.portal.data.local.InvoiceEntity>,
    notifications: List<Notification>,
    onNavigate: (String) -> Unit
) {
    val displayProjects = dashboardData?.dashboard?.projects ?: localProjects.map { it.toApi() }
    val displayInvoices = dashboardData?.dashboard?.invoices ?: localInvoices.map { it.toApi() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
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

        HomeKpiSection(dashboardData, displayProjects, displayInvoices)

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Quick Actions", Icons.Default.FlashOn)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionCard("Request Job", Icons.Default.AddCircle, Modifier.weight(1f)) { onNavigate("Services") }
            QuickActionCard("Pay Invoice", Icons.Default.Payment, Modifier.weight(1f)) { onNavigate("Billing") }
            QuickActionCard("Feedback", Icons.Default.RateReview, Modifier.weight(1f)) { onNavigate("Feedback") }
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
                        val maxVal = maxOf(budget.planned, budget.spent)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight((budget.planned / maxVal).toFloat())
                                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight((budget.spent / maxVal).toFloat())
                                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraSmall)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("PLANNED", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SPENT", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (budget.spent / budget.planned).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (budget.variance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text("Variance: ${budget.variance}%", style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.End))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Active Projects", Icons.Default.BusinessCenter)
        ProjectsSummaryList(displayProjects.take(3))
        
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Milestone Tasks", Icons.Default.Assignment)
        TasksSummaryList(dashboardData?.dashboard?.tasks?.take(3) ?: emptyList())

        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Recent Invoices", Icons.Default.Payments)
        InvoicesSummaryList(displayInvoices.take(3))
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Latest Updates", Icons.Default.Notifications)
        
        // Show Live Feed (Messages)
        dashboardData?.dashboard?.messages?.take(3)?.forEach { message ->
            MessageFeedItem(message)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (notifications.isEmpty() && (dashboardData?.dashboard?.messages.isNullOrEmpty())) {
            Text("No recent updates", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun MessageFeedItem(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (message.unread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
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
fun HomeKpiSection(dashboardData: com.greggory.portal.data.api.DashboardResponse?, projects: List<Project>, invoices: List<Invoice>) {
    val summary = dashboardData?.dashboard?.businessSummary
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiCard("Active Projects", summary?.activeProjects?.toString() ?: "0", Modifier.weight(1f))
        KpiCard("Open Invoices", summary?.openInvoices?.toString() ?: "0", Modifier.weight(1f))
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiCard("Open Messages", summary?.openMessages?.toString() ?: "0", Modifier.weight(1f))
        KpiCard("Next Milestone", summary?.nextMilestone ?: "Syncing...", Modifier.weight(1f))
    }

    dashboardData?.dashboard?.kpiMetrics?.forEach { metric ->
        Spacer(modifier = Modifier.height(8.dp)); KpiCard(metric.label, metric.value, Modifier.fillMaxWidth())
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
fun ProjectsSummaryList(projects: List<Project>) {
    if (projects.isEmpty()) {
        Text("No active projects", style = MaterialTheme.typography.bodySmall)
    } else {
        projects.forEach { project ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
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
fun TasksSummaryList(tasks: List<com.greggory.portal.data.api.Task>) {
    if (tasks.isEmpty()) {
        Text("No active milestones", style = MaterialTheme.typography.bodySmall)
    } else {
        tasks.forEach { task ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
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
fun InvoicesSummaryList(invoices: List<Invoice>) {
    if (invoices.isEmpty()) {
        Text("No pending invoices", style = MaterialTheme.typography.bodySmall)
    } else {
        invoices.forEach { invoice ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
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
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
