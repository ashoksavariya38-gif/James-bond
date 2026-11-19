package com.example.ui.components.plugins

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudyStats
import com.example.ui.PomodoroPhase
import com.example.ui.StudyWorkspaceState
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LuminaBrandGradient
import java.util.Locale

@Composable
fun PomodoroTab(
  state: StudyWorkspaceState,
  stats: StudyStats,
  onToggleTimer: () -> Unit,
  onSwitchPhase: (PomodoroPhase) -> Unit,
  onResetTimer: () -> Unit,
) {
  val luminaColors = LocalLuminaColors.current
  val totalSec = state.pomodoroPhase.defaultSeconds.coerceAtLeast(1)
  val progress = state.remainingSeconds.toFloat() / totalSec.toFloat()
  val minutes = state.remainingSeconds / 60
  val seconds = state.remainingSeconds % 60
  val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Text(
      text = "Pomodoro Study Timer",
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChipPill(
        label = "Focus (25m)",
        selected = state.pomodoroPhase == PomodoroPhase.FOCUS,
        onClick = { onSwitchPhase(PomodoroPhase.FOCUS) },
      )
      FilterChipPill(
        label = "Break (5m)",
        selected = state.pomodoroPhase == PomodoroPhase.BREAK,
        onClick = { onSwitchPhase(PomodoroPhase.BREAK) },
      )
    }

    Box(
      modifier = Modifier.size(210.dp),
      contentAlignment = Alignment.Center,
    ) {
      val trackColor = luminaColors.borderSubtle
      Canvas(modifier = Modifier.size(200.dp)) {
        val strokeWidth = 14.dp.toPx()
        drawArc(
          color = trackColor,
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
          brush = LuminaBrandGradient,
          startAngle = -90f,
          sweepAngle = 360f * progress,
          useCenter = false,
          style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = state.pomodoroPhase.label,
          style = MaterialTheme.typography.labelMedium,
          color = luminaColors.brandCyan,
        )
        Text(
          text = timeFormatted,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 38.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onResetTimer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1f).testTag("timer_reset_button"),
      ) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text("Reset")
      }
      Button(
        onClick = onToggleTimer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.weight(1.4f).testTag("timer_toggle_button"),
      ) {
        Icon(
          imageVector = if (state.isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(6.dp))
        Text(if (state.isTimerRunning) "Pause" else "Start")
      }
    }

    Surface(
      color = MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
      shape = RoundedCornerShape(16.dp),
      border = BorderStroke(1.dp, luminaColors.borderSubtle),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Timer, contentDescription = null, tint = luminaColors.brandPurple, modifier = Modifier.size(20.dp))
          Text(
            text = "${stats.completedPomodoros}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text("Completed Sessions", style = MaterialTheme.typography.labelSmall, color = luminaColors.textSecondary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Timer, contentDescription = null, tint = luminaColors.brandCyan, modifier = Modifier.size(20.dp))
          Text(
            text = "${stats.totalFocusMinutes}m",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text("Focus Time", style = MaterialTheme.typography.labelSmall, color = luminaColors.textSecondary)
        }
      }
    }
  }
}
