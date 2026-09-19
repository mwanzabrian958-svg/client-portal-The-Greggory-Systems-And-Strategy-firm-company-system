package com.greggory.portal.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.greggory.portal.data.api.RetrofitClient
import com.greggory.portal.data.api.TeamMember
import com.greggory.portal.ui.components.ContactExpertDialog
import com.greggory.portal.ui.components.TeamMemberCard

@Composable
fun TeamScreen(members: List<TeamMember>) {
    val groupedMembers = members.groupBy { it.projectName ?: "General Support" }
    var selectedMember by remember { mutableStateOf<TeamMember?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Project Team",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "The dedicated professionals handling your deliverables.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (members.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No team members assigned yet.")
                    }
                }
            }

            groupedMembers.forEach { (projectName, team) ->
                item {
                    Text(
                        text = projectName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(team) { member ->
                    TeamMemberCard(member, onClick = { selectedMember = member })
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        if (selectedMember != null) {
            ContactExpertDialog(
                expert = selectedMember!!,
                onDismiss = { selectedMember = null }
            )
        }
    }
}
