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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.greggory.portal.data.local.PreferencesManager
import com.greggory.portal.utils.FileDownloadHelper

data class ServiceItem(
    val title: String,
    val category: String,
    val description: String,
    val fullScope: String,
    val clientBenefits: String,
    val methodology: String,
    val deliverableItems: List<String>,
    val imageUrl: String,
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
            "Accelerate strategic clarity, eliminate operational redundancies, and align your entire enterprise around high-yield growth milestones. Our advisory models yield measurable improvements in capital allocation, operational agility, and executive decision-making.",
            "Phase 1: Comprehensive Enterprise Diagnostic & Stakeholder Interviews\nPhase 2: Strategic Blueprint & Governance Framework Formulation\nPhase 3: Board-Level Execution & Change Management Oversight\nPhase 4: Continuous Performance Monitoring & Quarterly Strategic Review",
            listOf(
                "Corporate Strategic Masterplan & 5-Year Roadmap",
                "Executive Decision Matrix & Governance Frameworks",
                "Market Entry, Positioning & Expansion Strategy",
                "Risk Assessment, Mitigation & Crisis Protocols"
            ),
            "https://images.unsplash.com/photo-1552664730-d307ca884978?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.TrendingUp
        ),
        ServiceItem(
            "Financial Engineering & M-Pesa Integration",
            "Financial Consultancy",
            "Capital optimization, automated financial telemetry, and seamless M-Pesa billing integration.",
            "Our financial engineering practice structures robust fiscal architectures for modern enterprises. We implement automated billing pipelines, real-time cashflow telemetry, and enterprise-grade payment gateway integrations (including M-Pesa Daraja) to guarantee financial transparency and frictionless transactions.",
            "Eliminate billing friction, automate reconciliation, and achieve total visibility over enterprise cashflow streams with secure cloud-backed ledgers and automated audit trails.",
            "Phase 1: Fiscal Audit & Payment Gateway Architecture Design\nPhase 2: M-Pesa Daraja & Banking API Pipeline Development\nPhase 3: Automated Ledger Reconciliation & Security Hardening\nPhase 4: Live Telemetry Deployment & Staff Training",
            listOf(
                "Automated Billing & Revenue Collection Pipelines",
                "M-Pesa Daraja & Banking API Gateway Integration",
                "Fiscal Modeling, Budgeting & Capital Optimization",
                "Real-time Financial Telemetry & Audit Dashboards"
            ),
            "https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.AccountBalance
        ),
        ServiceItem(
            "Software & Enterprise Cloud Applications",
            "Technology Consultancy",
            "Custom SAAS platforms, scalable web portals, and native mobile applications (Android/iOS).",
            "We engineer robust, high-performance software systems tailored to your exact operational workflows. From secure enterprise web portals to native mobile applications built with Jetpack Compose, our engineering team delivers scalable codebases engineered for zero downtime and military-grade security.",
            "Empower your workforce and clients with lightning-fast digital tools built with robust architecture, encrypted storage, and seamless interoperability across platforms.",
            "Phase 1: System Architecture & UX/UI Wireframing\nPhase 2: Agile Engineering & Secure Cloud Deployment\nPhase 3: Rigorous QA Testing, Penetration & Load Testing\nPhase 4: Production Handover & Ongoing Maintenance",
            listOf(
                "Custom SAAS & Enterprise Management Portals",
                "Native Android (Compose) & iOS Mobile Apps",
                "Scalable Cloud Architecture & RESTful API Development",
                "High-Performance Web Applications & ERP Systems"
            ),
            "https://images.unsplash.com/photo-1555066931-4365d14bab8c?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.AppShortcut
        ),
        ServiceItem(
            "Digital Transformation & Growth Strategy",
            "Growth Consultancy",
            "Digital branding, client acquisition funnels, and online market positioning.",
            "We guide traditional enterprises through comprehensive digital transformation initiatives. By modernizing client acquisition funnels, optimizing digital brand positioning, and deploying data-driven marketing strategies, we expand your market reach and elevate brand equity.",
            "Convert digital touchpoints into predictable revenue streams and outpace market competitors with cutting-edge positioning and automated customer journeys.",
            "Phase 1: Digital Footprint Audit & Competitor Benchmarking\nPhase 2: Funnel Architecture & Brand Identity Refinement\nPhase 3: Campaign Execution & Lead Generation Automation\nPhase 4: Conversion Rate Optimization (CRO) & Analytics",
            listOf(
                "Comprehensive Digital Transformation Roadmap",
                "High-Conversion Client Acquisition Funnels",
                "Brand Positioning & Executive Authority Building",
                "Omnichannel Growth & Marketing Automation"
            ),
            "https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.Campaign
        ),
        ServiceItem(
            "Data Intelligence, Analytics & AI Telemetry",
            "Data & AI Consultancy",
            "Predictive business analytics, custom AI telemetry, and automated KPI reporting.",
            "Transform raw operational data into actionable strategic intelligence. We deploy advanced analytics pipelines, custom AI agents, and real-time telemetry dashboards that provide executive leadership with deep insights into firm performance and predictive forecasting.",
            "Make data-backed decisions instantly with automated telemetry, real-time visual dashboards, and AI-driven forecasting models.",
            "Phase 1: Data Source Integration & Pipeline Architecture\nPhase 2: BI Dashboard Design & Predictive Model Training\nPhase 3: AI Agent Deployment & Automated Alerting\nPhase 4: Executive Insights Briefings & Fine-Tuning",
            listOf(
                "Predictive Business Intelligence & Forecasting Models",
                "Custom AI Agent & Agentic Workflow Integration",
                "Automated Executive KPI & Telemetry Dashboards",
                "Enterprise Data Governance & Secure Warehousing"
            ),
            "https://images.unsplash.com/photo-1504868584819-f8e8b4b6d7e3?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.Analytics
        ),
        ServiceItem(
            "Operational Efficiency & Workflow Optimization",
            "Operations Consultancy",
            "SOP development, workflow automation, and organizational bottleneck elimination.",
            "Operational friction eats into profit margins. Our operations consultancy audits your firm's workflows, designs streamlined Standard Operating Procedures (SOPs), and automates repetitive tasks to maximize team throughput and resource utilization.",
            "Achieve higher output with fewer bottlenecks, reducing operational overhead while scaling organizational capacity exponentially.",
            "Phase 1: Operational Workflow Time & Motion Audit\nPhase 2: Bottleneck Identification & Process Re-engineering\nPhase 3: SOP Documentation & Workflow Automation\nPhase 4: Productivity Tracking & Continuous Improvement",
            listOf(
                "Comprehensive Operational Workflow Audit",
                "Standard Operating Procedures (SOPs) Manual",
                "Process Automation & Task Routing Frameworks",
                "Resource Allocation, Capacity & Efficiency Tuning"
            ),
            "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.PrecisionManufacturing
        ),
        ServiceItem(
            "Legal, Governance & Regulatory Compliance",
            "Legal Consultancy",
            "Client agreements, regulatory audit readiness, and intellectual property protection.",
            "Protect your enterprise assets and maintain absolute regulatory compliance. We assist in drafting bulletproof client agreements, preparing for regulatory audits, and safeguarding your firm's intellectual property rights across domestic and international jurisdictions.",
            "Mitigate legal exposure and establish unassailable contracts that protect your business interests, vendor relationships, and client data privacy.",
            "Phase 1: Legal Risk Assessment & Compliance Audit\nPhase 2: Master Contract & SOW Template Drafting\nPhase 3: Regulatory Filing & IP Protection Strategy\nPhase 4: Ongoing Legal Advisory & Compliance Monitoring",
            listOf(
                "Master Client Engagement & Vendor Agreements",
                "Regulatory Audit Readiness Reviews & Checklists",
                "Intellectual Property (IP) Protection Frameworks",
                "Corporate Governance & Data Privacy Advisory"
            ),
            "https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=1000&q=80",
            Icons.Default.Gavel
        ),
        ServiceItem(
            "Cybersecurity & Data Vault Systems",
            "Security Consultancy",
            "Military-grade endpoint isolation, hardware biometrics, and encrypted storage schemes.",
            "In an era of sophisticated digital threats, data security is paramount. We implement defense-in-depth cybersecurity protocols, AES-256 encrypted storage vaults, and biometric authentication mechanisms to safeguard sensitive client assets and corporate intelligence.",
            "Ensure total confidentiality, integrity, and availability of your firm's most sensitive data assets against unauthorized access or breaches.",
            "Phase 1: Security Posture Assessment & Threat Modeling\nPhase 2: AES-256 Encryption & Biometric Vault Setup\nPhase 3: Penetration Testing & Vulnerability Hardening\nPhase 4: Security Awareness Training & Incident Response",
            listOf(
                "AES-256 Encrypted Enterprise Data Vaults",
                "Biometric Authentication & Zero-Trust Integration",
                "Endpoint Security & Hardware Isolation Protocols",
                "Vulnerability Assessment & Penetration Testing"
            ),
            "https://images.unsplash.com/photo-1563986768609-322da13575f3?auto=format&fit=crop&w=1000&q=80",
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
                text = "The Greggory Systems & Strategy Firm - Consultancy & Job Requests",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Browse our consultancy packages and request professional services. Tap any service block for full-page details, contract documents, and direct job request submission.",
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
        Column {
            AsyncImage(
                model = service.imageUrl,
                contentDescription = service.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                // Header Image Banner
                AsyncImage(
                    model = service.imageUrl,
                    contentDescription = service.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.large),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = service.category.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Executive Overview & Full Scope",
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
                    text = "Advisory & Implementation Methodology",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = service.methodology,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Key Strategic Deliverables",
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

                // Official Job Request Button
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:thegreggorysystemsandstrategyf@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Official Job Request & Engagement: ${service.title} (${prefs.getUserName()})")
                                putExtra(Intent.EXTRA_TEXT, "Hello Greggory Strategy Team,\n\nI would like to officially submit a Job & Engagement Request for '${service.title}'. Please review my account details and send the formal contract agreement.\n\nClient Name: ${prefs.getUserName()}\nEmail: ${prefs.getUserEmail()}\nPhone: ${prefs.getUserPhone()}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A9D8F))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUBMIT OFFICIAL JOB REQUEST", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Direct Email Firm Desk Button
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:thegreggorysystemsandstrategyf@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Consultation Inquiry: ${service.title} (${prefs.getUserName()})")
                                putExtra(Intent.EXTRA_TEXT, "Hello Greggory Strategy Team,\n\nI have reviewed the consultancy package for '${service.title}' and would like to connect with a strategist.\n\nClient Name: ${prefs.getUserName()}\nEmail: ${prefs.getUserEmail()}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Firm Desk Directly")
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
