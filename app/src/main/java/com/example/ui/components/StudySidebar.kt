package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SidebarFolder
import com.example.model.StudySession
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LuminaBrandGradient

@Composable
fun StudySidebarContent(
  sessions: List<StudySession>,
  activeSessionId: String?,
  searchQuery: String,
  onSearchQueryChange: (String) -> Unit,
  onNewSession: () -> Unit,
  onSelectSession: (StudySession) -> Unit,
  onDeleteSession: (String) -> Unit,
  savedCoursesCount: Int,
  flashcardDecksCount: Int,
  studyNotesCount: Int,
  onOpenFolder: (SidebarFolder) -> Unit,
  onOpenSettings: () -> Unit,
  userDisplayName: String,
  modifier: Modifier = Modifier,
) {
  val luminaColors = LocalLuminaColors.current
  val filteredSessions = if (searchQuery.isBlank()) {
    sessions
  } else {
    sessions.filter {
      it.title.contains(searchQuery, ignoreCase = true) ||
        it.preview.contains(searchQuery, ignoreCase = true)
    }
  }

  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(luminaColors.sidebar)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(14.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.padding(bottom = 14.dp, start = 4.dp),
    ) {
      LuminaSparkleLogo(size = 28.dp)
      Text(
        text = "LuminaStudy AI",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    // New Study Session Gradient Button
    Button(
      onClick = onNewSession,
      shape = RoundedCornerShape(14.dp),
      contentPadding = PaddingValues(0.dp),
      colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
      modifier = Modifier
        .fillMaxWidth()
        .height(46.dp)
        .testTag("new_study_session_button"),
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(LuminaBrandGradient, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "New Study Session",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
          )
        }
      }
    }

    Spacer(Modifier.height(12.dp))

    // Search Past Chats
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChange,
      placeholder = {
        Text("Search study chats...", style = MaterialTheme.typography.bodySmall)
      },
      leadingIcon = {
        Icon(
          imageVector = Icons.Default.Search,
          contentDescription = null,
          tint = luminaColors.textSecondary,
          modifier = Modifier.size(18.dp),
        )
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = luminaColors.brandBlue,
        unfocusedBorderColor = luminaColors.borderSubtle,
        focusedContainerColor = luminaColors.elevatedCard,
        unfocusedContainerColor = luminaColors.elevatedCard,
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("sidebar_search_input"),
    )

    Spacer(Modifier.height(14.dp))

    Text(
      text = "STUDY LIBRARY",
      style = MaterialTheme.typography.labelSmall,
      color = luminaColors.textSecondary,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )

    SidebarFolderRow(
      icon = Icons.Default.Bookmark,
      title = "Saved Courses",
      count = savedCoursesCount,
      accent = luminaColors.brandBlue,
      onClick = { onOpenFolder(SidebarFolder.SAVED_COURSES) },
      modifier = Modifier.testTag("folder_saved_courses"),
    )
    SidebarFolderRow(
      icon = Icons.Default.Style,
      title = "Flashcard Decks",
      count = flashcardDecksCount,
      accent = luminaColors.brandPurple,
      onClick = { onOpenFolder(SidebarFolder.FLASHCARD_DECKS) },
      modifier = Modifier.testTag("folder_flashcard_decks"),
    )
    SidebarFolderRow(
      icon = Icons.Default.Description,
      title = "Study Notes",
      count = studyNotesCount,
      accent = luminaColors.brandCyan,
      onClick = { onOpenFolder(SidebarFolder.STUDY_NOTES) },
      modifier = Modifier.testTag("folder_study_notes"),
    )

    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = luminaColors.borderSubtle)
    Spacer(Modifier.height(10.dp))

    Text(
      text = "RECENT SESSIONS",
      style = MaterialTheme.typography.labelSmall,
      color = luminaColors.textSecondary,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )

    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      if (filteredSessions.isEmpty()) {
        item {
          Text(
            text = if (searchQuery.isBlank()) "Start a conversation to save your study history." else "No matching chats found.",
            style = MaterialTheme.typography.bodySmall,
            color = luminaColors.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
          )
        }
      } else {
        items(filteredSessions, key = { it.id }) { session ->
          val isSelected = session.id == activeSessionId
          Surface(
            color = if (isSelected) luminaColors.elevatedCard else Color.Transparent,
            shape = RoundedCornerShape(10.dp),
            border = if (isSelected) BorderStroke(1.dp, luminaColors.borderSubtle) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectSession(session) },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = if (isSelected) luminaColors.brandCyan else luminaColors.textSecondary,
                modifier = Modifier.size(16.dp),
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = session.title,
                  style = MaterialTheme.typography.labelLarge,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                if (session.preview.isNotBlank()) {
                  Text(
                    text = session.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = luminaColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              IconButton(
                onClick = { onDeleteSession(session.id) },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete session",
                  tint = luminaColors.textSecondary,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }
        }
      }
    }

    HorizontalDivider(color = luminaColors.borderSubtle)
    Spacer(Modifier.height(10.dp))

    // Bottom Settings & Plugins Button
    Surface(
      color = luminaColors.elevatedCard,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(1.dp, luminaColors.borderSubtle),
      modifier = Modifier
        .fillMaxWidth()
        .clickable { onOpenSettings() }
        .testTag("settings_plugins_button"),
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(LuminaBrandGradient),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = userDisplayName.take(1).uppercase().ifBlank { "S" },
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Settings & Plugins",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = userDisplayName.ifBlank { "Student Account" },
            style = MaterialTheme.typography.bodySmall,
            color = luminaColors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = null,
          tint = luminaColors.textSecondary,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}

@Composable
private fun SidebarFolderRow(
  icon: ImageVector,
  title: String,
  count: Int,
  accent: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val luminaColors = LocalLuminaColors.current
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 9.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = accent,
      modifier = Modifier.size(18.dp),
    )
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
    Surface(
      color = luminaColors.elevatedCard,
      shape = RoundedCornerShape(8.dp),
      border = BorderStroke(1.dp, luminaColors.borderSubtle),
    ) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelSmall,
        color = luminaColors.textSecondary,
        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
      )
    }
  }
}
