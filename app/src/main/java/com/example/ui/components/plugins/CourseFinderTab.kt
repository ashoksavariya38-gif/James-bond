package com.example.ui.components.plugins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.model.CourseSearchResult
import com.example.model.StudyLanguage
import com.example.ui.StudyWorkspaceState
import com.example.ui.theme.LocalLuminaColors

@Composable
fun CourseFinderTab(
  state: StudyWorkspaceState,
  onQueryChange: (String) -> Unit,
  onSubjectChange: (String) -> Unit,
  onLevelChange: (String) -> Unit,
  onCostChange: (String) -> Unit,
  onLanguageChange: (StudyLanguage) -> Unit,
  onSearch: () -> Unit,
  onSaveCourse: (CourseSearchResult) -> Unit,
) {
  val luminaColors = LocalLuminaColors.current
  val uriHandler = LocalUriHandler.current
  var langExpanded by remember { mutableStateOf(false) }

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Global Course Finder",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Discover real worldwide courses in any Latin-script language using Gemini + Google Search grounding.",
          style = MaterialTheme.typography.bodySmall,
          color = luminaColors.textSecondary,
        )

        OutlinedTextField(
          value = state.courseQuery,
          onValueChange = onQueryChange,
          label = { Text("Course topic or keyword") },
          placeholder = { Text("e.g., Linear Algebra, Machine Learning, Derecho") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("course_query_input"),
        )

        OutlinedTextField(
          value = state.courseSubject,
          onValueChange = onSubjectChange,
          label = { Text("Subject area (optional)") },
          placeholder = { Text("Computer Science, Medicine, Languages...") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        // Language dropdown
        Box {
          Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, luminaColors.borderSubtle),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { langExpanded = true },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(Icons.Default.Language, contentDescription = null, tint = luminaColors.brandCyan, modifier = Modifier.size(18.dp))
              Text(
                text = "Language: ${state.courseLanguage.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
              )
              Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = luminaColors.textSecondary)
            }
          }
          DropdownMenu(
            expanded = langExpanded,
            onDismissRequest = { langExpanded = false },
          ) {
            StudyLanguage.entries.forEach { lang ->
              DropdownMenuItem(
                text = { Text("${lang.displayName} (${lang.nativeName})") },
                onClick = {
                  langExpanded = false
                  onLanguageChange(lang)
                },
              )
            }
          }
        }

        // Level filter pills
        Text("Level", style = MaterialTheme.typography.labelMedium, color = luminaColors.textSecondary)
        Row(
          modifier = Modifier.horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("All Levels", "Beginner", "Intermediate", "Advanced").forEach { lvl ->
            val selected = state.courseLevel == lvl
            FilterChipPill(label = lvl, selected = selected, onClick = { onLevelChange(lvl) })
          }
        }

        // Cost filter pills
        Text("Cost", style = MaterialTheme.typography.labelMedium, color = luminaColors.textSecondary)
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          listOf("All", "Free", "Paid").forEach { cost ->
            val selected = state.courseCost == cost
            FilterChipPill(label = cost, selected = selected, onClick = { onCostChange(cost) })
          }
        }

        Button(
          onClick = onSearch,
          enabled = !state.isSearchingCourses,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("search_courses_button"),
        ) {
          if (state.isSearchingCourses) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Search Worldwide Courses")
          }
        }

        if (!state.courseError.isNullOrBlank()) {
          Text(
            text = state.courseError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    if (state.coursesFromCache && state.courseResults.isNotEmpty()) {
      item {
        Text(
          text = "Loaded from offline cache to save your API quota.",
          style = MaterialTheme.typography.labelSmall,
          color = luminaColors.brandCyan,
        )
      }
    }

    if (state.courseResults.isEmpty() && !state.isSearchingCourses) {
      item {
        Surface(
          color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, luminaColors.borderSubtle),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Default.Public, contentDescription = null, tint = luminaColors.brandBlue, modifier = Modifier.size(28.dp))
            Text("Search MIT OCW, Coursera, edX, Fun-MOOC, Miríadax & more", style = MaterialTheme.typography.bodySmall, color = luminaColors.textSecondary)
          }
        }
      }
    }

    items(state.courseResults) { course ->
      Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Surface(
              color = luminaColors.brandBlue.copy(alpha = 0.16f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = course.cost,
                style = MaterialTheme.typography.labelSmall,
                color = luminaColors.brandCyan,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
            Text(
              text = course.level,
              style = MaterialTheme.typography.labelSmall,
              color = luminaColors.textSecondary,
            )
          }

          Text(
            text = course.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.School, contentDescription = null, tint = luminaColors.brandPurple, modifier = Modifier.size(14.dp))
              Text(course.provider, style = MaterialTheme.typography.labelSmall, color = luminaColors.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = luminaColors.brandCyan, modifier = Modifier.size(14.dp))
              Text(course.duration, style = MaterialTheme.typography.labelSmall, color = luminaColors.textSecondary)
            }
          }

          Text(
            text = "Language: ${course.language}",
            style = MaterialTheme.typography.labelSmall,
            color = luminaColors.textSecondary,
          )

          if (course.summary.isNotBlank()) {
            Text(
              text = course.summary,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = { onSaveCourse(course) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text("Save")
            }
            Button(
              onClick = { runCatching { uriHandler.openUri(course.url) } },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text("Open")
            }
          }
        }
      }
    }
  }
}

@Composable
internal fun FilterChipPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val luminaColors = LocalLuminaColors.current
  Surface(
    color = if (selected) luminaColors.brandPurple.copy(alpha = 0.22f) else MaterialTheme.colorScheme.background,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, if (selected) luminaColors.brandPurple else luminaColors.borderSubtle),
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = if (selected) MaterialTheme.colorScheme.onSurface else luminaColors.textSecondary,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
  }
}
