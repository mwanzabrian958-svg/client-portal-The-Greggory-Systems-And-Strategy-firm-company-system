package com.greggory.portal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greggory.portal.data.api.SearchResults

@Composable
fun SearchScreen(query: String, results: SearchResults?, isLoading: Boolean) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (query.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Start typing to search projects, invoices, or tasks.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val hasResults = results?.let {
        it.projects?.isNotEmpty() == true || it.invoices?.isNotEmpty() == true || it.tasks?.isNotEmpty() == true || it.documents?.isNotEmpty() == true
    } ?: false

    if (!hasResults) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No results found for '$query'", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        results?.projects?.takeIf { it.isNotEmpty() }?.let { projects ->
            item { Text("PROJECTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            items(projects) { project ->
                ProjectCard(project, onViewRoadmap = {})
            }
        }

        results?.invoices?.takeIf { it.isNotEmpty() }?.let { invoices ->
            item { Text("INVOICES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            items(invoices) { invoice ->
                InvoiceCard(invoice, onPayClick = {}, onDownloadClick = {}, onReportClick = {})
            }
        }

        results?.tasks?.takeIf { it.isNotEmpty() }?.let { tasks ->
            item { Text("TASKS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
            items(tasks) { task ->
                TaskCard(task)
            }
        }
    }
}
