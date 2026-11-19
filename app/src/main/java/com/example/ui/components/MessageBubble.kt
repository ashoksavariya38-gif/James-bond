package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.ChatMessage
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LuminaBrandGradient

@Composable
fun MessageBubble(
  message: ChatMessage,
  isSpeaking: Boolean,
  onCopy: (String) -> Unit,
  onReadAloud: (ChatMessage) -> Unit,
  onSaveToNotes: (ChatMessage) -> Unit,
  onConvertToFlashcards: (ChatMessage) -> Unit,
  onSecondOpinion: (ChatMessage) -> Unit,
  modifier: Modifier = Modifier,
) {
  val luminaColors = LocalLuminaColors.current

  if (message.role == "user") {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
      horizontalArrangement = Arrangement.End,
    ) {
      Surface(
        color = luminaColors.elevatedCard,
        shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
        border = BorderStroke(1.dp, luminaColors.borderSubtle),
        modifier = Modifier.widthIn(max = 560.dp),
      ) {
        Column(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (!message.attachmentLabel.isNullOrBlank()) {
            Surface(
              color = luminaColors.brandBlue.copy(alpha = 0.16f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.AttachFile,
                  contentDescription = null,
                  tint = luminaColors.brandCyan,
                  modifier = Modifier.size(13.dp),
                )
                Text(
                  text = message.attachmentLabel,
                  style = MaterialTheme.typography.labelSmall,
                  color = luminaColors.brandCyan,
                )
              }
            }
          }
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  } else {
    Surface(
      color = luminaColors.elevatedCard,
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, luminaColors.borderSubtle),
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
    ) {
      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Model badge header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier = Modifier
              .size(22.dp)
              .background(LuminaBrandGradient, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              tint = androidx.compose.ui.graphics.Color.White,
              modifier = Modifier.size(14.dp),
            )
          }
          Text(
            text = message.modelLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          if (!message.secondaryContent.isNullOrBlank()) {
            Surface(
              color = luminaColors.brandPurple.copy(alpha = 0.2f),
              shape = RoundedCornerShape(8.dp),
            ) {
              Text(
                text = "Dual Compare",
                style = MaterialTheme.typography.labelSmall,
                color = luminaColors.brandCyan,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
              )
            }
          }
        }

        // Primary content or Dual Compare view
        if (message.secondaryContent.isNullOrBlank()) {
          MarkdownMathText(
            content = message.content,
            sourceUrls = message.sourceUrls,
          )
        } else {
          DualCompareSection(
            primaryLabel = message.modelLabel,
            primaryContent = message.content,
            primaryUrls = message.sourceUrls,
            secondaryLabel = message.secondaryModelLabel ?: "Second Opinion",
            secondaryContent = message.secondaryContent,
          )
        }

        HorizontalDivider(color = luminaColors.borderSubtle)

        // 5 Required Action Buttons
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          ResponseActionChip(
            icon = Icons.Default.ContentCopy,
            label = "Copy",
            onClick = { onCopy(message.content) },
            modifier = Modifier.testTag("action_copy"),
          )
          ResponseActionChip(
            icon = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
            label = if (isSpeaking) "Stop" else "Read Aloud",
            highlighted = isSpeaking,
            onClick = { onReadAloud(message) },
            modifier = Modifier.testTag("action_read_aloud"),
          )
          ResponseActionChip(
            icon = Icons.Default.BookmarkAdd,
            label = "Save to Notes",
            onClick = { onSaveToNotes(message) },
            modifier = Modifier.testTag("action_save_notes"),
          )
          ResponseActionChip(
            icon = Icons.Default.Style,
            label = "Convert to Flashcards",
            onClick = { onConvertToFlashcards(message) },
            modifier = Modifier.testTag("action_convert_flashcards"),
          )
          ResponseActionChip(
            icon = Icons.Default.CompareArrows,
            label = "Second Opinion",
            onClick = { onSecondOpinion(message) },
            modifier = Modifier.testTag("action_second_opinion"),
          )
        }
      }
    }
  }
}

@Composable
private fun DualCompareSection(
  primaryLabel: String,
  primaryContent: String,
  primaryUrls: List<String>,
  secondaryLabel: String,
  secondaryContent: String,
) {
  val luminaColors = LocalLuminaColors.current
  var selectedPane by remember { mutableIntStateOf(0) }

  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    if (maxWidth >= 600.dp) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Surface(
          color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, luminaColors.brandBlue.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(primaryLabel, style = MaterialTheme.typography.labelLarge, color = luminaColors.brandBlue)
            MarkdownMathText(content = primaryContent, sourceUrls = primaryUrls)
          }
        }
        Surface(
          color = MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, luminaColors.brandPurple.copy(alpha = 0.4f)),
          modifier = Modifier.weight(1f),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(secondaryLabel, style = MaterialTheme.typography.labelLarge, color = luminaColors.brandPurple)
            MarkdownMathText(content = secondaryContent)
          }
        }
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(primaryLabel, secondaryLabel).forEachIndexed { idx, label ->
            val selected = selectedPane == idx
            Surface(
              color = if (selected) luminaColors.brandBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, if (selected) luminaColors.brandBlue else luminaColors.borderSubtle),
              modifier = Modifier.clickable { selectedPane = idx },
            ) {
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onSurface else luminaColors.textSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
        if (selectedPane == 0) {
          MarkdownMathText(content = primaryContent, sourceUrls = primaryUrls)
        } else {
          MarkdownMathText(content = secondaryContent)
        }
      }
    }
  }
}

@Composable
private fun ResponseActionChip(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  highlighted: Boolean = false,
) {
  val luminaColors = LocalLuminaColors.current
  Surface(
    color = if (highlighted) luminaColors.brandCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(1.dp, if (highlighted) luminaColors.brandCyan else luminaColors.borderSubtle),
    modifier = modifier.clickable(onClick = onClick),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = if (highlighted) luminaColors.brandCyan else luminaColors.textSecondary,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (highlighted) luminaColors.brandCyan else MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
