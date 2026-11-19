package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.CourseSearchResult
import com.example.model.FlashcardDeck
import com.example.model.PluginDrawerTab
import com.example.model.StudyLanguage
import com.example.model.StudyStats
import com.example.ui.PomodoroPhase
import com.example.ui.StudyWorkspaceState
import com.example.ui.components.plugins.CourseFinderTab
import com.example.ui.components.plugins.FlashcardsTab
import com.example.ui.components.plugins.PomodoroTab
import com.example.ui.components.plugins.QuizTab
import com.example.ui.theme.LocalLuminaColors

@Composable
fun PluginDrawerContent(
  state: StudyWorkspaceState,
  decks: List<FlashcardDeck>,
  stats: StudyStats,
  onSelectTab: (PluginDrawerTab) -> Unit,
  onClose: () -> Unit,
  onCourseQueryChange: (String) -> Unit,
  onCourseSubjectChange: (String) -> Unit,
  onCourseLevelChange: (String) -> Unit,
  onCourseCostChange: (String) -> Unit,
  onCourseLanguageChange: (StudyLanguage) -> Unit,
  onSearchCourses: () -> Unit,
  onSaveCourse: (CourseSearchResult) -> Unit,
  onFlashcardTopicChange: (String) -> Unit,
  onGenerateFlashcards: () -> Unit,
  onSelectDeck: (String) -> Unit,
  onFlipCard: () -> Unit,
  onStepCard: (Int, Int) -> Unit,
  onMarkCardStatus: (FlashcardDeck, Int, String) -> Unit,
  onQuizTopicChange: (String) -> Unit,
  onQuizCountChange: (Int) -> Unit,
  onGenerateQuiz: () -> Unit,
  onSelectQuizOption: (Int, Int) -> Unit,
  onSubmitQuiz: () -> Unit,
  onResetQuiz: () -> Unit,
  onToggleTimer: () -> Unit,
  onSwitchPomodoroPhase: (PomodoroPhase) -> Unit,
  onResetPomodoro: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val luminaColors = LocalLuminaColors.current

  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "Study Plugins",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      IconButton(onClick = onClose) {
        Icon(Icons.Default.Close, contentDescription = "Close plugin drawer", tint = luminaColors.textSecondary)
      }
    }

    TabRow(
      selectedTabIndex = state.activePluginTab.ordinal,
      containerColor = luminaColors.elevatedCard,
      contentColor = luminaColors.brandCyan,
    ) {
      PluginDrawerTab.entries.forEach { tab ->
        Tab(
          selected = state.activePluginTab == tab,
          onClick = { onSelectTab(tab) },
          text = { Text(tab.title, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
        )
      }
    }
    HorizontalDivider(color = luminaColors.borderSubtle)

    when (state.activePluginTab) {
      PluginDrawerTab.COURSES -> CourseFinderTab(
        state = state,
        onQueryChange = onCourseQueryChange,
        onSubjectChange = onCourseSubjectChange,
        onLevelChange = onCourseLevelChange,
        onCostChange = onCourseCostChange,
        onLanguageChange = onCourseLanguageChange,
        onSearch = onSearchCourses,
        onSaveCourse = onSaveCourse,
      )
      PluginDrawerTab.FLASHCARDS -> FlashcardsTab(
        state = state,
        decks = decks,
        onTopicChange = onFlashcardTopicChange,
        onGenerateDeck = onGenerateFlashcards,
        onSelectDeck = onSelectDeck,
        onFlipCard = onFlipCard,
        onStepCard = onStepCard,
        onMarkCardStatus = onMarkCardStatus,
      )
      PluginDrawerTab.QUIZ -> QuizTab(
        state = state,
        onTopicChange = onQuizTopicChange,
        onCountChange = onQuizCountChange,
        onGenerateQuiz = onGenerateQuiz,
        onSelectOption = onSelectQuizOption,
        onSubmitQuiz = onSubmitQuiz,
        onResetAttempt = onResetQuiz,
      )
      PluginDrawerTab.TIMER -> PomodoroTab(
        state = state,
        stats = stats,
        onToggleTimer = onToggleTimer,
        onSwitchPhase = onSwitchPomodoroPhase,
        onResetTimer = onResetPomodoro,
      )
    }
  }
}
