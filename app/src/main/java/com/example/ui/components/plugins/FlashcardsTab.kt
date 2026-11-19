package com.example.ui.components.plugins

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.FlashcardDeck
import com.example.ui.StudyWorkspaceState
import com.example.ui.theme.LocalLuminaColors

@Composable
fun FlashcardsTab(
  state: StudyWorkspaceState,
  decks: List<FlashcardDeck>,
  onTopicChange: (String) -> Unit,
  onGenerateDeck: () -> Unit,
  onSelectDeck: (String) -> Unit,
  onFlipCard: () -> Unit,
  onStepCard: (Int, Int) -> Unit,
  onMarkCardStatus: (FlashcardDeck, Int, String) -> Unit,
) {
  val luminaColors = LocalLuminaColors.current
  val activeDeck = decks.firstOrNull { it.id == state.activeDeckId } ?: decks.firstOrNull()

  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "Flashcards Studio",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "Generate flip cards from any topic or pasted notes and track recall with spaced repetition.",
          style = MaterialTheme.typography.bodySmall,
          color = luminaColors.textSecondary,
        )

        OutlinedTextField(
          value = state.flashcardTopicInput,
          onValueChange = onTopicChange,
          label = { Text("Topic or study notes") },
          placeholder = { Text("e.g., Organic chemistry functional groups, French irregular verbs...") },
          maxLines = 4,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("flashcard_topic_input"),
        )

        Button(
          onClick = onGenerateDeck,
          enabled = !state.isGeneratingFlashcards,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("generate_flashcards_button"),
        ) {
          if (state.isGeneratingFlashcards) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Generate Flashcard Deck")
          }
        }

        if (!state.flashcardError.isNullOrBlank()) {
          Text(
            text = state.flashcardError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    if (decks.isNotEmpty()) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Your Decks (${decks.size})",
            style = MaterialTheme.typography.labelMedium,
            color = luminaColors.textSecondary,
          )
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            decks.forEach { deck ->
              FilterChipPill(
                label = "${deck.title.take(24)} (${deck.cards.size})",
                selected = activeDeck?.id == deck.id,
                onClick = { onSelectDeck(deck.id) },
              )
            }
          }
        }
      }
    }

    if (activeDeck != null && activeDeck.cards.isNotEmpty()) {
      item {
        val safeIdx = state.activeCardIndex.coerceIn(0, activeDeck.cards.lastIndex)
        val card = activeDeck.cards[safeIdx]
        val masteredCount = activeDeck.cards.count { it.status == "KNOW_IT" }
        val progress = masteredCount.toFloat() / activeDeck.cards.size.coerceAtLeast(1)

        val rotation by animateFloatAsState(
          targetValue = if (state.isCardFlipped) 180f else 0f,
          animationSpec = spring(stiffness = 380f),
          label = "flashcard_flip",
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Card ${safeIdx + 1} of ${activeDeck.cards.size}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = "$masteredCount/${activeDeck.cards.size} Mastered",
              style = MaterialTheme.typography.labelMedium,
              color = luminaColors.success,
            )
          }

          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = luminaColors.success,
            trackColor = luminaColors.borderSubtle,
          )

          // 3D Flip Card
          Surface(
            color = if (rotation > 90f) luminaColors.mathSurface else MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
              1.5.dp,
              if (rotation > 90f) luminaColors.brandCyan else luminaColors.brandPurple.copy(alpha = 0.6f),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp)
              .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
              }
              .clickable { onFlipCard() }
              .testTag("flashcard_flip_surface"),
          ) {
            Box(
              modifier = Modifier
                .padding(20.dp)
                .graphicsLayer {
                  if (rotation > 90f) rotationY = 180f
                },
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Text(
                  text = if (rotation > 90f) "ANSWER • TAP TO FLIP" else "QUESTION • TAP TO FLIP",
                  style = MaterialTheme.typography.labelSmall,
                  color = if (rotation > 90f) luminaColors.brandCyan else luminaColors.brandPurple,
                )
                Text(
                  text = if (rotation > 90f) card.back else card.front,
                  style = MaterialTheme.typography.titleMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                  textAlign = TextAlign.Center,
                )
              }
            }
          }

          // Spaced repetition buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = { onMarkCardStatus(activeDeck, safeIdx, "REVIEW_AGAIN") },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("flashcard_review_again"),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = luminaColors.warning, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text("Review Again")
            }
            Button(
              onClick = { onMarkCardStatus(activeDeck, safeIdx, "KNOW_IT") },
              colors = ButtonDefaults.buttonColors(containerColor = luminaColors.success),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("flashcard_know_it"),
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.size(6.dp))
              Text("Know It")
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = { onStepCard(-1, activeDeck.cards.size) }) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous card")
            }
            Text(
              text = "Status: ${card.status.replace('_', ' ')}",
              style = MaterialTheme.typography.labelSmall,
              color = luminaColors.textSecondary,
            )
            IconButton(onClick = { onStepCard(1, activeDeck.cards.size) }) {
              Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next card")
            }
          }
        }
      }
    } else if (!state.isGeneratingFlashcards) {
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
            Icon(Icons.Default.Style, contentDescription = null, tint = luminaColors.brandPurple, modifier = Modifier.size(28.dp))
            Text(
              "No flashcard deck selected yet. Generate one above or convert any chat explanation!",
              style = MaterialTheme.typography.bodySmall,
              color = luminaColors.textSecondary,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}
