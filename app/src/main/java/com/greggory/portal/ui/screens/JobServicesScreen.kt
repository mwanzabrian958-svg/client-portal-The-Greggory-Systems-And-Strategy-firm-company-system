package com.greggory.portal.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.utils.FileDownloadHelper

data class ServiceItem(
    val title: String,
    val category: String,
    val description: String,
    val fullScope: String,
    val clientBenefits: String,
    val deliverableItems: List<String>,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobServicesScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }
    var selectedService by remember { mutableStateOf<ServiceItem?>(null) }

    val consultancyServices = listOf(
        ServiceItem(
            "Executive Business Advisory & Corporate Strategy",
            "Business Consultancy",
            "High-level strategic planning, organizational restructuring, and executive decision frameworks.",
            "As a premier business consultancy firm, The Greggory Systems & Strategy Firm provides comprehensive executive advisory services designed to navigate complex market environments. We collaborate directly with your leadership team to define long-term vision, optimize corporate governance, and position your enterprise for sustainable market leadership.",
            "Accelerate strategic clarity, eliminate operational redundancies, and align your entire enterprise around high-yield growth milestones.",
            listOf("Corporate Strategic Blueprint & Roadmap", "Executive Decision & Governance Frameworks", "Market Entry & Expansion Strategy", "Risk Assessment & Mitigation Planning"),
            Icons.Default.TrendingUp
        ),
        ServiceItem(
            "Financial Engineering & M-Pesa Integration",
            "Financial Consultancy",
            "Capital optimization, automated financial telemetry, and seamless M-Pesa billing integration.",
            "Our financial engineering practice structures robust fiscal architectures for modern enterprises. We implement automated billing pipelines, real-time cashflow telemetry, and enterprise-grade payment gateway integrations (including M-Pesa Daraja) to guarantee financial transparency and frictionless transactions.",
            "Eliminate billing friction, automate reconciliation, and achieve total visibility over enterprise cashflow streams.",
            listOf("Automated Billing & Revenue Pipelines", "M-Pesa Daraja Gateway Integration", "Fiscal Modeling & Capital Optimization", "Real-time Financial Telemetry Dashboards"),
            Icons.Default.AccountBalance
        ),
        ServiceItem(
            "Software & Enterprise Cloud Applications",
            "Technology Consultancy",
            "Custom SAAS platforms, scalable web portals, and native mobile applications (Android/iOS).",
            "We engineer robust, high-performance software systems tailored to your exact operational workflows. From secure enterprise web portals to native mobile applications built with Jetpack Compose, our engineering team delivers scalable codebases engineered for zero downtime.",
            "Empower your workforce and clients with lightning-fast digital tools built with military-grade security and modern architecture.",
            listOf("Custom SAAS & Enterprise Portals", "Native Android & iOS Mobile Apps", "Cloud Architecture & API Development", "High-Performance Web Applications"),
            Icons.Default.AppShortcut
        ),
        ServiceItem(
            "Digital Transformation & Growth Strategy",
            "Growth Consultancy",
            "Digital branding, client acquisition funnels, and online market positioning.",
            "We guide traditional enterprises through comprehensive digital transformation initiatives. By modernizing client acquisition funnels, optimizing digital brand positioning, and deploying data-driven marketing strategies, we expand your market reach and elevate brand equity.",
            "Convert digital touchpoints into predictable revenue streams and outpace market competitors with modern positioning.",
            listOf("Digital Transformation Roadmap", "High-Conversion Acquisition Funnels", "Brand Positioning & Authority Building", "Omnichannel Growth Strategy"),
            Icons.Default.Campaign
        ),
        ServiceItem(
            "Data Intelligence, Analytics & AI Telemetry",
            "Data & AI Consultancy",
            "Predictive business analytics, custom AI telemetry, and automated KPI reporting.",
            "Transform raw operational data into actionable strategic intelligence. We deploy advanced analytics pipelines, custom AI agents, and real-time telemetry dashboards that provide executive leadership with deep insights into firm performance.",
            "Make data-backed decisions instantly with automated telemetry and predictive forecasting models.",
            listOf("Predictive Business Intelligence Models", "Custom AI Agent Integration", "Automated Executive KPI Dashboards", "Data Governance & Warehousing"),
            Icons.Default.Analytics
        ),
        ServiceItem(
            "Operational Efficiency & Workflow Optimization",
            "Operations Consultancy",
            "SOP development, workflow automation, and organizational bottleneck elimination.",
            "Operational friction eats into profit margins. Our operations consultancy audits your firm's workflows, designs streamlined Standard Operating Procedures (SOPs), and automates repetitive tasks to maximize team throughput.",
            "Achieve higher output with fewer bottlenecks, reducing operational overhead while scaling capacity.",
            listOf("Comprehensive Workflow Audit", "Standard Operating Procedures (SOPs)", "Process Automation Frameworks", "Resource Allocation & Efficiency Tuning"),
            Icons.Default.PrecisionManufacturing
        ),
        ServiceItem(
            "Legal, Governance & Regulatory Compliance",
            "Legal Consultancy",
            "Client agreements, regulatory audit readiness, and intellectual property protection.",
            "Protect your enterprise assets and maintain absolute regulatory compliance. We assist in drafting bulletproof client agreements, preparing for regulatory audits, and safeguarding your firm's intellectual property rights across jurisdictions.",
            "Mitigate legal exposure and establish unassailable contracts that protect your business interests.",
            listOf("Master Client Engagement Agreements", "Regulatory Audit Readiness Reviews", "Intellectual Property Protection", "Corporate Governance Advisory"),
            Icons.Default.Gavel
        ),
        ServiceItem(
            "Cybersecurity & Data Vault Systems",
            "Security Consultancy",
            "Military-grade endpoint isolation, hardware biometrics, and encrypted storage schemes.",
            "In an era of sophisticated digital threats, data security is paramount. We implement defense-in-depth cybersecurity protocols, AES-256 encrypted storage vaults, and biometric authentication mechanisms to safeguard sensitive client assets.",
            "Ensure total confidentiality, integrity, and availability of your firm's most sensitive data assets.",
            listOf("AES-256 Encrypted Data Vaults", "Biometric Authentication Frameworks", "Endpoint Security & Isolation", "Vulnerability Assessment & Hardening"),
            Icons.Default.Security
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "The Greggory Firm - Consultancy & Services",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Tap any consultancy or service block below to view full scope, deliverables, contract documents, and direct communication channels.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(consultancyServices) { service ->
            ServiceCard(service, onClick = { selectedService = service })
        }
        
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Custom Consultancy & Project Engagement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Require a customized advisory package or specialized corporate engagement? Connect directly with our strategy desk.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { 
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:thegreggorysystemsandstrategyf@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Consultation Inquiry: ${prefs.getUserName()}")
                                    putExtra(Intent.EXTRA_TEXT, "Hello Greggory Strategy Team,\n\nI would like to request a formal consultation regarding a new strategic engagement for my organization.")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                            }
                        }, 
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Email Strategy Desk")
                    }
                }
            }
        }
    }

    selectedService?.let { service ->
        FullServiceDetailsDialog(service = service, onDismiss = { selectedService = null })
    }
}

@Composable
fun ServiceCard(service: ServiceItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = service.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = service.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullServiceDetailsDialog(service: ServiceItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager.getInstance(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(service.title, style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = service.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = service.category,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = service.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Full Consultancy Scope",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.fullScope,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "What We Can Do for Your Business",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.clientBenefits,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Key Deliverables",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                service.deliverableItems.forEach { item ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2A9D8F),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = item, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // Contract Documents & Agreements Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Contract & Engagement Documents",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Access standardized Master Service Agreements, Statement of Work (SOW) templates, and legal contract documentation for this consultancy service.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = {
                                FileDownloadHelper.downloadFile(
                                    context = context,
                                    url = FileDownloadHelper.getReportUrl(-1),
                                    fileName = "Greggory_Firm_Agreement_${service.title.replace(" ", "_")}.pdf",
                                    description = "Downloading Contract Agreement"
                                )
                                Toast.makeText(context, "Contract agreement download started...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download Contract Document (PDF/DOCX)")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Direct Communication Action
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:thegreggorysystemsandstrategyf@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Consultation & Contract Inquiry: ${service.title} (${prefs.getUserName()})")
                                putExtra(Intent.EXTRA_TEXT, "Hello Greggory Strategy Team,\n\nI have reviewed the consultancy package for '${service.title}' and would like to proceed with a formal consultation and contract engagement for my organization.\n\nClient Name: ${prefs.getUserName()}\nEmail: ${prefs.getUserEmail()}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Firm Desk (Direct)", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
