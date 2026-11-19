package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.UserStudyPreferences
import com.example.model.FlashcardDeck
import com.example.model.SavedCourse
import com.example.model.SidebarFolder
import com.example.model.StudyNote
import com.example.ui.components.MarkdownMathText
import com.example.ui.theme.LocalLuminaColors

@Composable
fun FolderDetailScreen(
  folder: SidebarFolder,
  savedCourses: List<SavedCourse>,
  flashcardDecks: List<FlashcardDeck>,
  studyNotes: List<StudyNote>,
  onBack: () -> Unit,
  onDeleteCourse: (String) -> Unit,
  onOpenDeck: (String) -> Unit,
  onDeleteDeck: (String) -> Unit,
  onCreateNote: (String, String) -> Unit,
  onDeleteNote: (String) -> Unit,
) {
  BackHandler(onBack = onBack)
  val luminaColors = LocalLuminaColors.current
  val uriHandler = LocalUriHandler.current

  var showNewNoteForm by remember { mutableStateOf(false) }
  var newNoteTitle by remember { mutableStateOf("") }
  var newNoteBody by remember { mutableStateOf("") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("folder_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chat")
      }
      Text(
        text = folder.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.weight(1f),
      )
      if (folder == SidebarFolder.STUDY_NOTES) {
        Button(
          onClick = { showNewNoteForm = !showNewNoteForm },
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.size(4.dp))
          Text(if (showNewNoteForm) "Close" else "New Note")
        }
      }
    }
    HorizontalDivider(color = luminaColors.borderSubtle)

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
    ) {
      when (folder) {
        SidebarFolder.SAVED_COURSES -> {
          if (savedCourses.isEmpty()) {
            item {
              EmptyFolderCard("No saved courses yet. Use the Global Course Finder plugin to search and bookmark courses.")
            }
          } else {
            items(savedCourses, key = { it.id }) { course ->
              Surface(
                color = luminaColors.elevatedCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, luminaColors.borderSubtle),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text(course.title, style = MaterialTheme.typography.titleMedium)
                  Text(
                    "${course.provider} • ${course.language} • ${course.level} • ${course.cost}",
                    style = MaterialTheme.typography.labelMedium,
                    color = luminaColors.brandCyan,
                  )
                  if (course.summary.isNotBlank()) {
                    Text(course.summary, style = MaterialTheme.typography.bodySmall, color = luminaColors.textSecondary)
                  }
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = { runCatching { uriHandler.openUri(course.url) } },
                      shape = RoundedCornerShape(10.dp),
                    ) {
                      Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.size(6.dp))
                      Text("Open Course")
                    }
                    OutlinedButton(
                      onClick = { onDeleteCourse(course.id) },
                      shape = RoundedCornerShape(10.dp),
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(Modifier.size(4.dp))
                      Text("Remove")
                    }
                  }
                }
              }
            }
          }
        }

        SidebarFolder.FLASHCARD_DECKS -> {
          if (flashcardDecks.isEmpty()) {
            item {
              EmptyFolderCard("No flashcard decks saved yet. Generate one in the Flashcards plugin or convert any chat response!")
            }
          } else {
            items(flashcardDecks, key = { it.id }) { deck ->
              val mastered = deck.cards.count { it.status == "KNOW_IT" }
              Surface(
                color = luminaColors.elevatedCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, luminaColors.borderSubtle),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(deck.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                      "${deck.cards.size} cards • $mastered mastered",
                      style = MaterialTheme.typography.labelMedium,
                      color = luminaColors.brandPurple,
                    )
                  }
                  Button(onClick = { onOpenDeck(deck.id) }, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Study")
                  }
                  IconButton(onClick = { onDeleteDeck(deck.id) }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete deck", tint = luminaColors.textSecondary)
                  }
                }
              }
            }
          }
        }

        SidebarFolder.STUDY_NOTES -> {
          if (showNewNoteForm) {
            item {
              Surface(
                color = luminaColors.elevatedCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, luminaColors.brandBlue.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  OutlinedTextField(
                    value = newNoteTitle,
                    onValueChange = { newNoteTitle = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                  )
                  OutlinedTextField(
                    value = newNoteBody,
                    onValueChange = { newNoteBody = it },
                    label = { Text("Note Content (Markdown supported)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                  )
                  Button(
                    onClick = {
                      if (newNoteBody.isNotBlank()) {
                        onCreateNote(newNoteTitle.ifBlank { "Study Note" }, newNoteBody)
                        newNoteTitle = ""
                        newNoteBody = ""
                        showNewNoteForm = false
                      }
                    },
                    shape = RoundedCornerShape(10.dp),
                  ) {
                    Text("Save Note")
                  }
                }
              }
            }
          }

          if (studyNotes.isEmpty() && !showNewNoteForm) {
            item {
              EmptyFolderCard("No study notes saved yet. Tap 'Save to Notes' on any AI response or create a note above.")
            }
          } else {
            items(studyNotes, key = { it.id }) { note ->
              Surface(
                color = luminaColors.elevatedCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, luminaColors.borderSubtle),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteNote(note.id) }) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "Delete note", tint = luminaColors.textSecondary)
                    }
                  }
                  MarkdownMathText(content = note.content)
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyFolderCard(message: String) {
  val luminaColors = LocalLuminaColors.current
  Surface(
    color = luminaColors.elevatedCard,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, luminaColors.borderSubtle),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = luminaColors.textSecondary,
      modifier = Modifier.padding(20.dp),
    )
  }
}

@Composable
fun SettingsAndPluginsScreen(
  preferences: UserStudyPreferences,
  cachedResponseCount: Int,
  userEmail: String,
  onBack: () -> Unit,
  onToggleDarkMode: (Boolean) -> Unit,
  onSaveOpenAiKey: (String) -> Unit,
  onClearCache: () -> Unit,
  onSignOut: () -> Unit,
) {
  BackHandler(onBack = onBack)
  val luminaColors = LocalLuminaColors.current
  var openAiKeyInput by remember(preferences.openAiApiKey) { mutableStateOf(preferences.openAiApiKey) }
  var keyVisible by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to chat")
      }
      Text(
        text = "Settings & Plugins",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
      )
    }
    HorizontalDivider(color = luminaColors.borderSubtle)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
        .widthIn(max = 680.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      // Theme Card
      Surface(
        color = luminaColors.elevatedCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Dark Slate Mode", style = MaterialTheme.typography.titleMedium)
            Text(
              "Deep slate study theme (#0F1117) or toggle to crisp Light Mode.",
              style = MaterialTheme.typography.bodySmall,
              color = luminaColors.textSecondary,
            )
          }
          Switch(
            checked = preferences.darkMode,
            onCheckedChange = onToggleDarkMode,
            modifier = Modifier.testTag("settings_dark_mode_switch"),
          )
        }
      }

      // Optional OpenAI Connector Card
      Surface(
        color = luminaColors.elevatedCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Key, contentDescription = null, tint = luminaColors.brandCyan, modifier = Modifier.size(20.dp))
            Text("ChatGPT Plugin (Optional OpenAI Key)", style = MaterialTheme.typography.titleMedium)
          }
          Text(
            "All Gemini Fast, Deep Study, Course Finder, Flashcard, and Quiz features work with zero API key via Firebase AI Logic. Optionally add your OpenAI API key below to unlock ChatGPT Plugin and Dual AI Compare side-by-side.",
            style = MaterialTheme.typography.bodySmall,
            color = luminaColors.textSecondary,
          )
          OutlinedTextField(
            value = openAiKeyInput,
            onValueChange = { openAiKeyInput = it },
            label = { Text("OPENAI_API_KEY (Optional)") },
            placeholder = { Text("sk-...") },
            singleLine = true,
            visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { keyVisible = !keyVisible }) {
                Icon(
                  imageVector = if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle key visibility",
                )
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("openai_key_input"),
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onSaveOpenAiKey(openAiKeyInput) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("save_openai_key_button"),
            ) {
              Text("Save Key")
            }
            if (preferences.openAiApiKey.isNotBlank()) {
              OutlinedButton(
                onClick = {
                  openAiKeyInput = ""
                  onSaveOpenAiKey("")
                },
                shape = RoundedCornerShape(10.dp),
              ) {
                Text("Clear Key")
              }
            }
          }
        }
      }

      // Local Cache Management
      Surface(
        color = luminaColors.elevatedCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Quota-Saving Local Cache", style = MaterialTheme.typography.titleMedium)
            Text(
              "$cachedResponseCount repeated study answers cached locally for instant offline recall.",
              style = MaterialTheme.typography.bodySmall,
              color = luminaColors.textSecondary,
            )
          }
          OutlinedButton(onClick = onClearCache, shape = RoundedCornerShape(10.dp)) {
            Text("Clear")
          }
        }
      }

      // Account & Sign Out
      Surface(
        color = luminaColors.elevatedCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Google Account Sync", style = MaterialTheme.typography.titleMedium)
          Text(
            text = userEmail.ifBlank { "Signed in with Google" },
            style = MaterialTheme.typography.bodySmall,
            color = luminaColors.textSecondary,
          )
          Spacer(Modifier.height(4.dp))
          Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("sign_out_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Sign Out")
          }
        }
      }
    }
  }
}
