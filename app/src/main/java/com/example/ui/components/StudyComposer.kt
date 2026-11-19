package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.ComposerAttachment
import com.example.model.StudyLanguage
import com.example.model.StudyPluginPill
import com.example.ui.theme.LocalLuminaColors
import com.example.ui.theme.LuminaBrandGradient

@Composable
fun StudyComposer(
  text: String,
  onTextChange: (String) -> Unit,
  pendingAttachment: ComposerAttachment?,
  onAttachmentChange: (ComposerAttachment?) -> Unit,
  activePlugins: Set<StudyPluginPill>,
  onTogglePlugin: (StudyPluginPill) -> Unit,
  selectedLanguage: StudyLanguage,
  isGenerating: Boolean,
  isVoiceListening: Boolean,
  onVoiceListeningChange: (Boolean) -> Unit,
  onVoiceResult: (String) -> Unit,
  onErrorNotice: (String) -> Unit,
  onSend: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val luminaColors = LocalLuminaColors.current
  var attachMenuExpanded by remember { mutableStateOf(false) }

  // Zero-permission Photo Picker for images
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val bytes = stream.readBytes()
          if (bytes.size > 18 * 1024 * 1024) {
            onErrorNotice("Image exceeds 18 MB limit.")
          } else {
            onAttachmentChange(
              ComposerAttachment(
                displayName = queryFileName(context, uri) ?: "Study Image",
                mimeType = context.contentResolver.getType(uri) ?: "image/jpeg",
                bytes = bytes,
                isPdf = false,
              )
            )
          }
        }
      }.onFailure {
        onErrorNotice("Could not read selected image.")
      }
    }
  }

  // Zero-permission Storage Access Framework for PDFs
  val pdfPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument(),
  ) { uri ->
    if (uri != null) {
      runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          val bytes = stream.readBytes()
          if (bytes.size > 18 * 1024 * 1024) {
            onErrorNotice("PDF exceeds 18 MB limit.")
          } else {
            onAttachmentChange(
              ComposerAttachment(
                displayName = queryFileName(context, uri) ?: "Document.pdf",
                mimeType = "application/pdf",
                bytes = bytes,
                isPdf = true,
              )
            )
          }
        }
      }.onFailure {
        onErrorNotice("Could not read selected PDF.")
      }
    }
  }

  val speechRecognizer = remember(context) {
    if (SpeechRecognizer.isRecognitionAvailable(context)) {
      SpeechRecognizer.createSpeechRecognizer(context)
    } else {
      null
    }
  }

  DisposableEffect(speechRecognizer) {
    onDispose { speechRecognizer?.destroy() }
  }

  fun startDictation() {
    if (speechRecognizer == null) {
      onErrorNotice("Voice dictation is not available on this device.")
      return
    }
    speechRecognizer.setRecognitionListener(object : RecognitionListener {
      override fun onReadyForSpeech(params: Bundle?) { onVoiceListeningChange(true) }
      override fun onBeginningOfSpeech() {}
      override fun onRmsChanged(rmsdB: Float) {}
      override fun onBufferReceived(buffer: ByteArray?) {}
      override fun onEndOfSpeech() { onVoiceListeningChange(false) }
      override fun onError(error: Int) {
        onVoiceListeningChange(false)
        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_CLIENT) {
          onErrorNotice("Voice input interrupted (code $error). Try speaking again.")
        }
      }
      override fun onResults(results: Bundle?) {
        onVoiceListeningChange(false)
        val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (!spoken.isNullOrBlank()) onVoiceResult(spoken)
      }
      override fun onPartialResults(partialResults: Bundle?) {}
      override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
      if (selectedLanguage != StudyLanguage.AUTO) {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage.ttsLocaleTag)
      }
    }
    onVoiceListeningChange(true)
    speechRecognizer.startListening(intent)
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) {
      startDictation()
    } else {
      onErrorNotice("Microphone permission is required for voice-to-text dictation.")
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // Toggleable Study Plugin Pills
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      StudyPluginPill.entries.forEach { pill ->
        val active = activePlugins.contains(pill)
        Surface(
          color = if (active) luminaColors.brandPurple.copy(alpha = 0.22f) else luminaColors.elevatedCard,
          shape = RoundedCornerShape(20.dp),
          border = BorderStroke(
            1.dp,
            if (active) luminaColors.brandCyan else luminaColors.borderSubtle,
          ),
          modifier = Modifier
            .clickable { onTogglePlugin(pill) }
            .testTag("plugin_pill_${pill.name.lowercase()}"),
        ) {
          Text(
            text = pill.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.onSurface else luminaColors.textSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          )
        }
      }
    }

    // Main Elevated Composer Container
    Surface(
      color = luminaColors.elevatedCard,
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.dp, luminaColors.borderSubtle),
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 720.dp),
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        AnimatedVisibility(visible = pendingAttachment != null) {
          pendingAttachment?.let { att ->
            Surface(
              color = luminaColors.brandBlue.copy(alpha = 0.16f),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, luminaColors.brandBlue.copy(alpha = 0.4f)),
              modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = if (att.isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                  contentDescription = null,
                  tint = luminaColors.brandCyan,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = att.displayName,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(
                  onClick = { onAttachmentChange(null) },
                  modifier = Modifier.size(20.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove attachment",
                    tint = luminaColors.textSecondary,
                    modifier = Modifier.size(14.dp),
                  )
                }
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          // Attach Image or PDF button
          Box {
            IconButton(
              onClick = { attachMenuExpanded = true },
              modifier = Modifier.testTag("composer_attach_button"),
            ) {
              Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = "Attach PDF or image",
                tint = luminaColors.textSecondary,
              )
            }
            DropdownMenu(
              expanded = attachMenuExpanded,
              onDismissRequest = { attachMenuExpanded = false },
            ) {
              DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, tint = luminaColors.brandCyan) },
                text = { Text("Upload Image (Photo Picker)") },
                onClick = {
                  attachMenuExpanded = false
                  photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                  )
                },
              )
              DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = luminaColors.brandPurple) },
                text = { Text("Upload PDF Document") },
                onClick = {
                  attachMenuExpanded = false
                  pdfPickerLauncher.launch(arrayOf("application/pdf"))
                },
              )
            }
          }

          // Multi-line Text Input
          Box(
            modifier = Modifier
              .weight(1f)
              .padding(vertical = 10.dp),
          ) {
            if (text.isEmpty()) {
              Text(
                text = if (isVoiceListening) "Listening..." else "Ask anything, paste notes, or attach a PDF...",
                style = MaterialTheme.typography.bodyLarge,
                color = if (isVoiceListening) luminaColors.brandCyan else luminaColors.textSecondary,
              )
            }
            BasicTextField(
              value = text,
              onValueChange = onTextChange,
              textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
              ),
              cursorBrush = SolidColor(luminaColors.brandCyan),
              maxLines = 6,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("composer_text_input"),
            )
          }

          // Voice-to-Text Mic Button
          IconButton(
            onClick = {
              if (isVoiceListening) {
                speechRecognizer?.stopListening()
                onVoiceListeningChange(false)
              } else {
                val granted = ContextCompat.checkSelfPermission(
                  context,
                  Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) startDictation()
                else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              }
            },
            modifier = Modifier.testTag("composer_mic_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Mic,
              contentDescription = "Voice to text input",
              tint = if (isVoiceListening) luminaColors.brandCyan else luminaColors.textSecondary,
            )
          }

          // Gradient Send / Stop Button
          val canSend = text.isNotBlank() || pendingAttachment != null
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(
                if (isGenerating || canSend) LuminaBrandGradient
                else SolidColor(luminaColors.borderSubtle),
              )
              .clickable(enabled = isGenerating || canSend) {
                if (isGenerating) onStop() else onSend()
              }
              .testTag("composer_send_button"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
              contentDescription = if (isGenerating) "Stop generating" else "Send message",
              tint = Color.White,
              modifier = Modifier.size(19.dp),
            )
          }
        }
      }
    }
  }
}

private fun queryFileName(context: android.content.Context, uri: android.net.Uri): String? {
  return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    ?.use { cursor ->
      if (cursor.moveToFirst()) cursor.getString(0) else null
    }
}
