package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalLuminaColors

private data class QuickStartItem(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val accentIndex: Int,
  val tag: String,
)

@Composable
fun WelcomeQuickStart(
  onQuickStartClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val luminaColors = LocalLuminaColors.current
  val items = listOf(
    QuickStartItem(
      title = "Explain a Concept Step-by-Step",
      subtitle = "Break down tough STEM, humanities, or coding topics with examples",
      icon = Icons.Default.AutoAwesome,
      accentIndex = 0,
      tag = "quick_start_explain",
    ),
    QuickStartItem(
      title = "Find Worldwide Courses",
      subtitle = "Search free & paid courses in 10 Latin-script languages with web grounding",
      icon = Icons.Default.Public,
      accentIndex = 1,
      tag = "quick_start_courses",
    ),
    QuickStartItem(
      title = "Generate Quiz & Flashcards",
      subtitle = "Turn any subject into active-recall flip cards and 5–10 question tests",
      icon = Icons.Default.Quiz,
      accentIndex = 2,
      tag = "quick_start_quiz",
    ),
    QuickStartItem(
      title = "Summarize Notes or PDF",
      subtitle = "Attach lecture slides, PDFs, or problem photos for instant breakdowns",
      icon = Icons.Default.Description,
      accentIndex = 3,
      tag = "quick_start_summarize",
    ),
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .widthIn(max = 720.dp)
      .padding(horizontal = 20.dp, vertical = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .background(
          Brush.radialGradient(
            colors = listOf(
              luminaColors.brandPurple.copy(alpha = 0.25f),
              Color.Transparent,
            )
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      LuminaSparkleLogo(size = 52.dp)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "What are we mastering today?",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = "Ask a question, upload a PDF or photo, or launch a study plugin below.",
        style = MaterialTheme.typography.bodyMedium,
        color = luminaColors.textSecondary,
        textAlign = TextAlign.Center,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      items.chunked(2).forEach { rowItems ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rowItems.forEach { card ->
            val accentColor = when (card.accentIndex) {
              0 -> luminaColors.brandBlue
              1 -> luminaColors.brandCyan
              2 -> luminaColors.brandPurple
              else -> luminaColors.brandBlue
            }
            Surface(
              color = luminaColors.elevatedCard,
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(1.dp, luminaColors.borderSubtle),
              modifier = Modifier
                .weight(1f)
                .clickable { onQuickStartClick(card.accentIndex) }
                .testTag(card.tag),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  imageVector = card.icon,
                  contentDescription = null,
                  tint = accentColor,
                  modifier = Modifier.size(22.dp),
                )
                Text(
                  text = card.title,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = card.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = luminaColors.textSecondary,
                )
              }
            }
          }
        }
      }
    }
  }
}
