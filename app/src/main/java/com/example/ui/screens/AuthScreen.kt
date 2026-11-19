package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.firebase.attemptAutoSignIn
import com.example.data.firebase.onGoogleSignInClicked
import com.example.ui.components.LuminaSparkleLogo
import com.example.ui.theme.GoogleBrandBlue
import com.example.ui.theme.GoogleBrandGreen
import com.example.ui.theme.GoogleBrandRed
import com.example.ui.theme.GoogleBrandYellow
import com.example.ui.theme.LightTextPrimary
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LuminaStudyTheme

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
  LuminaStudyTheme(darkTheme = true) {
    val luminaColors = LocalLuminaColors.current
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAutoSignIn by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
      attemptAutoSignIn(
        context = context,
        credentialManager = credentialManager,
        onAuthSuccess = {
          isCheckingAutoSignIn = false
          onAuthSuccess()
        },
        onUnauthenticated = {
          isCheckingAutoSignIn = false
        },
        scope = scope,
      )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lumina_aura")
    val auraPulse by infiniteTransition.animateFloat(
      initialValue = 0.92f,
      targetValue = 1.08f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "aura_scale",
    )
    val auraAlpha by infiniteTransition.animateFloat(
      initialValue = 0.22f,
      targetValue = 0.38f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
      ),
      label = "aura_alpha",
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(colorScheme.background)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 24.dp, vertical = 20.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 420.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(modifier = Modifier.weight(1.1f))

        // Floating Sparkle-Gradient Hero
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Box(
            modifier = Modifier.size(156.dp),
            contentAlignment = Alignment.Center,
          ) {
            Box(
              modifier = Modifier
                .size(156.dp)
                .scale(auraPulse)
                .alpha(auraAlpha)
                .background(
                  brush = Brush.radialGradient(
                    colors = listOf(
                      luminaColors.brandPurple.copy(alpha = 0.75f),
                      luminaColors.brandBlue.copy(alpha = 0.45f),
                      luminaColors.brandCyan.copy(alpha = 0.20f),
                      Color.Transparent,
                    )
                  ),
                  shape = CircleShape,
                )
            )
            LuminaSparkleLogo(size = 82.dp)
          }

          Text(
            text = "LuminaStudy AI",
            style = MaterialTheme.typography.displaySmall,
            color = colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )

          Text(
            text = "Your multilingual Gemini study companion.",
            style = MaterialTheme.typography.bodyMedium,
            color = luminaColors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
          )
        }

        Spacer(modifier = Modifier.weight(1.3f))

        // Anchored Bottom Action Region
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
            Surface(
              color = colorScheme.error.copy(alpha = 0.14f),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, colorScheme.error.copy(alpha = 0.45f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = errorMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              )
            }
          }

          Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
          ) {
            // Subtle gradient aura halo behind crisp white Google button
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .background(
                  brush = Brush.horizontalGradient(
                    colors = luminaColors.sparkleGradientStops.map { it.copy(alpha = 0.26f) }
                  ),
                  shape = RoundedCornerShape(31.dp),
                )
            )

            Button(
              onClick = {
                errorMessage = null
                isSigningIn = true
                onGoogleSignInClicked(
                  context = context,
                  credentialManager = credentialManager,
                  onAuthSuccess = {
                    isSigningIn = false
                    onAuthSuccess()
                  },
                  onAuthError = { msg ->
                    isSigningIn = false
                    errorMessage = msg
                  },
                  scope = scope,
                  onAuthCancelled = {
                    isSigningIn = false
                  },
                )
              },
              enabled = !isSigningIn && !isCheckingAutoSignIn,
              shape = RoundedCornerShape(27.dp),
              border = BorderStroke(1.25.dp, luminaColors.brandGradient),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = LightTextPrimary,
                disabledContainerColor = Color.White.copy(alpha = 0.92f),
                disabledContentColor = LightTextPrimary.copy(alpha = 0.7f),
              ),
              contentPadding = PaddingValues(horizontal = 20.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("google_sign_in_button"),
            ) {
              if (isSigningIn || isCheckingAutoSignIn) {
                CircularProgressIndicator(
                  modifier = Modifier.size(20.dp),
                  color = luminaColors.brandBlue,
                  strokeWidth = 2.5.dp,
                )
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                  GoogleGMark(modifier = Modifier.size(20.dp))
                  Text(
                    text = "Sign in with Google",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = LightTextPrimary,
                  )
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
private fun GoogleGMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val stroke = size.minDimension * 0.20f
    val inset = stroke / 2f
    val arcSize = Size(size.width - stroke, size.height - stroke)
    val topLeft = Offset(inset, inset)

    drawArc(
      color = GoogleBrandRed,
      startAngle = -140f,
      sweepAngle = 100f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = GoogleBrandYellow,
      startAngle = 140f,
      sweepAngle = 80f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = GoogleBrandGreen,
      startAngle = 45f,
      sweepAngle = 95f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    drawArc(
      color = GoogleBrandBlue,
      startAngle = -8f,
      sweepAngle = 55f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = stroke, cap = StrokeCap.Butt),
    )
    val cy = size.height / 2f
    drawLine(
      color = GoogleBrandBlue,
      start = Offset(size.width * 0.50f, cy),
      end = Offset(size.width - inset * 0.6f, cy),
      strokeWidth = stroke,
      cap = StrokeCap.Butt,
    )
  }
}
