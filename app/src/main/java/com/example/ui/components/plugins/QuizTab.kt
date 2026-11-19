package com.example.ui.components.plugins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.StudyWorkspaceState
import com.example.ui.theme.LocalLuminaColors

@Composable
fun QuizTab(
  state: StudyWorkspaceState,
  onTopicChange: (String) -> Unit,
  onCountChange: (Int) -> Unit,
  onGenerateQuiz: () -> Unit,
  onSelectOption: (Int, Int) -> Unit,
  onSubmitQuiz: () -> Unit,
  onResetAttempt: () -> Unit,
) {
  val luminaColors = LocalLuminaColors.current

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Quiz & Mock Test",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Create 5-to-10 question interactive MCQs with instant grading and detailed explanations.",
          style = MaterialTheme.typography.bodySmall,
          color = luminaColors.textSecondary,
        )

        OutlinedTextField(
          value = state.quizTopicInput,
          onValueChange = onTopicChange,
          label = { Text("Exam topic or concept") },
          placeholder = { Text("e.g., Microeconomics elasticity, World War II, Python OOP") },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("quiz_topic_input"),
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text("Questions:", style = MaterialTheme.typography.labelMedium, color = luminaColors.textSecondary)
          listOf(5, 7, 10).forEach { count ->
            FilterChipPill(
              label = "$count Qs",
              selected = state.quizQuestionCount == count,
              onClick = { onCountChange(count) },
            )
          }
        }

        Button(
          onClick = onGenerateQuiz,
          enabled = !state.isGeneratingQuiz,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("generate_quiz_button"),
        ) {
          if (state.isGeneratingQuiz) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Generate Mock Test")
          }
        }

        if (!state.quizError.isNullOrBlank()) {
          Text(
            text = state.quizError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    if (state.isQuizSubmitted && state.quizQuestions.isNotEmpty()) {
      item {
        val score = state.quizQuestions.count { q -> state.quizAnswers[q.id] == q.correctIndex }
        val pct = (score * 100) / state.quizQuestions.size
        Surface(
          color = luminaColors.brandBlue.copy(alpha = 0.14f),
          shape = RoundedCornerShape(16.dp),
          border = BorderStroke(1.dp, luminaColors.brandBlue.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column {
              Text("Score Summary", style = MaterialTheme.typography.labelMedium, color = luminaColors.brandCyan)
              Text(
                text = "$score / ${state.quizQuestions.size} Correct ($pct%)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
            OutlinedButton(onClick = onResetAttempt, shape = RoundedCornerShape(10.dp)) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(4.dp))
              Text("Retake")
            }
          }
        }
      }
    }

    items(state.quizQuestions, key = { it.id }) { q ->
      val selectedIdx = state.quizAnswers[q.id]
      Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = "${q.id}. ${q.question}",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
          )

          q.options.forEachIndexed { optIdx, optionText ->
            val isChosen = selectedIdx == optIdx
            val isCorrect = optIdx == q.correctIndex
            val borderColor = when {
              state.isQuizSubmitted && isCorrect -> luminaColors.success
              state.isQuizSubmitted && isChosen && !isCorrect -> MaterialTheme.colorScheme.error
              isChosen -> luminaColors.brandBlue
              else -> luminaColors.borderSubtle
            }
            Surface(
              color = when {
                state.isQuizSubmitted && isCorrect -> luminaColors.success.copy(alpha = 0.14f)
                state.isQuizSubmitted && isChosen && !isCorrect -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                isChosen -> luminaColors.brandBlue.copy(alpha = 0.12f)
                else -> luminaColors.elevatedCard
              },
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, borderColor),
              modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !state.isQuizSubmitted) { onSelectOption(q.id, optIdx) },
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                RadioButton(
                  selected = isChosen,
                  onClick = { if (!state.isQuizSubmitted) onSelectOption(q.id, optIdx) },
                )
                Text(
                  text = optionText,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }

          if (state.isQuizSubmitted) {
            Surface(
              color = luminaColors.mathSurface,
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = "Explanation: ${q.explanation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(10.dp),
              )
            }
          }
        }
      }
    }

    if (state.quizQuestions.isNotEmpty() && !state.isQuizSubmitted) {
      item {
        Button(
          onClick = onSubmitQuiz,
          enabled = state.quizAnswers.isNotEmpty(),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("submit_quiz_button"),
        ) {
          Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.size(8.dp))
          Text("Grade Quiz (${state.quizAnswers.size}/${state.quizQuestions.size} answered)")
        }
      }
    }
  }
}
