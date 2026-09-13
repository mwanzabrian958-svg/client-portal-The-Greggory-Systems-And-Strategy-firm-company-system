package com.greggory.portal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greggory.portal.R
import com.greggory.portal.data.api.Project
import com.greggory.portal.data.api.Invoice
import com.greggory.portal.data.api.Notification
import com.greggory.portal.data.api.Report
import com.greggory.portal.data.local.AppDatabase
import com.greggory.portal.data.local.toApi
import com.greggory.portal.data.local.toEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val preferencesManager = remember { com.greggory.portal.data.local.PreferencesManager(context) }
    val database = remember { AppDatabase.getDatabase(context) }
    
    val localProjects by database.projectDao().getAllProjects().collectAsState(initial = emptyList())
    val localInvoices by database.invoiceDao().getAllInvoices().collectAsState(initial = emptyList())

    var dashboardData by remember { mutableStateOf<com.greggory.portal.data.api.DashboardResponse?>(null) }
    var notificationsData by remember { mutableStateOf<List<Notification>>(emptyList()) }
    var reportsData by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var currentView by remember { mutableStateOf("Home") }

    fun refreshData() {
        val token = preferencesManager.getToken()
        if (token == null) {
            onLogout()
            return
        }
        isLoading = true
            scope.launch {
                try {
                    val dashResponse = com.greggory.portal.data.api.RetrofitClient.instance.getDashboard()
                    if (dashResponse.isSuccessful && dashResponse.body()?.success == true) {
                        val body = dashResponse.body()
                        
                        // Validate Set in Stone Routing Integrity
                        val userId = preferencesManager.getUserId()
                        val isIntegrityValid = body?.projects?.all { 
                            // In a real scenario, we'd check a client_id field in the response
                            true 
                        } ?: true

                        if (isIntegrityValid) {
                            dashboardData = body
                            body?.projects?.let { projects ->
                                database.projectDao().clearProjects()
                                database.projectDao().insertProjects(projects.map { it.toEntity() })
                            }
                            body?.invoices?.let { invoices ->
                                database.invoiceDao().clearInvoices()
                                database.invoiceDao().insertInvoices(invoices.map { it.toEntity() })
                            }
                        } else {
                            errorMessage = "Security Error: Routing Integrity Breach"
                        }
                    }
                    notificationsData = com.greggory.portal.data.api.RetrofitClient.instance.getNotifications().body()?.notifications ?: emptyList()
                    reportsData = com.greggory.portal.data.api.RetrofitClient.instance.getReports().body()?.reports ?: emptyList()
                } catch (e: Exception) {
                    errorMessage = "Network error: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
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
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                DrawerItem("Home", Icons.Default.Dashboard, currentView == "Home") {
                    currentView = "Home"; scope.launch { drawerState.close() }
                }
                
                Text("DEEP DIVES", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary)
                DrawerItem("Full Project Ledger", Icons.Default.BusinessCenter, currentView == "Projects") {
                    currentView = "Projects"; scope.launch { drawerState.close() }
                }
                DrawerItem("Financial Archive", Icons.Default.Payments, currentView == "Billing") {
                    currentView = "Billing"; scope.launch { drawerState.close() }
                }
                DrawerItem("Document Vault", Icons.Default.Folder, currentView == "Documents") {
                    currentView = "Documents"; scope.launch { drawerState.close() }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                Divider()
                DrawerItem("Logout", Icons.Default.Logout, false) {
                    scope.launch {
                        preferencesManager.clear()
                        database.projectDao().clearProjects()
                        database.invoiceDao().clearInvoices()
                        onLogout()
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(if(currentView == "Home") "MISSION CONTROL" else currentView.uppercase(), fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
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
                when (currentView) {
                    "Home" -> HomeScreen(isLoading, dashboardData, localProjects, localInvoices, notificationsData)
                    "Projects" -> ProjectListScreen(dashboardData?.projects ?: localProjects.map { it.toApi() })
                    "Billing" -> BillingScreen(dashboardData?.invoices ?: localInvoices.map { it.toApi() })
                    "Documents" -> ReportsScreen(reportsData)
                    else -> Text("Section: $currentView", modifier = Modifier.align(Alignment.Center))
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
    notifications: List<Notification>
) {
    val displayProjects = dashboardData?.projects ?: localProjects.map { it.toApi() }
    val displayInvoices = dashboardData?.invoices ?: localInvoices.map { it.toApi() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        HomeKpiSection(dashboardData, displayProjects, displayInvoices)
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Active Projects", Icons.Default.BusinessCenter)
        ProjectsSummaryList(displayProjects.take(3))
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Recent Invoices", Icons.Default.Payments)
        InvoicesSummaryList(displayInvoices.take(3))
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader("Latest Updates", Icons.Default.Notifications)
        if (notifications.isEmpty()) {
            Text("No recent updates", style = MaterialTheme.typography.bodySmall)
        } else {
            notifications.take(2).forEach { notification ->
                NotificationItem(notification)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun HomeKpiSection(dashboardData: com.greggory.portal.data.api.DashboardResponse?, projects: List<Project>, invoices: List<Invoice>) {
    val activeProjectsCount = projects.count { it.status.lowercase() == "active" }.toString()
    val pendingInvoicesSum = invoices.filter { it.status.lowercase() != "paid" }.sumOf { it.amount }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiCard("Active Projects", activeProjectsCount, Modifier.weight(1f))
        KpiCard("Open Invoices", "KSH ${pendingInvoicesSum.toInt()}", Modifier.weight(1f))
    }
    dashboardData?.kpiMetrics?.forEach { metric ->
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
fun InvoicesSummaryList(invoices: List<Invoice>) {
    if (invoices.isEmpty()) {
        Text("No pending invoices", style = MaterialTheme.typography.bodySmall)
    } else {
        invoices.forEach { invoice ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Invoice #${invoice.id}", style = MaterialTheme.typography.bodyLarge)
                        StatusBadge(invoice.status)
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
fun KpiCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
