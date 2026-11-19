package com.example.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.firebase.signOutUser
import com.example.model.ChatMessage
import com.example.ui.DataStreamState
import com.example.ui.StudyViewModel
import com.example.ui.components.MarkdownMathText
import com.example.ui.components.MessageBubble
import com.example.ui.components.PluginDrawerContent
import com.example.ui.components.StudyComposer
import com.example.ui.components.StudySidebarContent
import com.example.ui.components.StudyTopBar
import com.example.ui.components.WelcomeQuickStart
import com.example.ui.theme.LocalLuminaColors
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LuminaStudyWorkspace(viewModel: StudyViewModel) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val clipboard = LocalClipboardManager.current
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val luminaColors = LocalLuminaColors.current

  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val preferences by viewModel.preferences.collectAsStateWithLifecycle()
  val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
  val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
  val notesState by viewModel.notesState.collectAsStateWithLifecycle()
  val decksState by viewModel.flashcardDecksState.collectAsStateWithLifecycle()
  val coursesState by viewModel.savedCoursesState.collectAsStateWithLifecycle()
  val stats by viewModel.studyStatsState.collectAsStateWithLifecycle()

  val sessions = (sessionsState as? DataStreamState.Success)?.data.orEmpty()
  val messages = (messagesState as? DataStreamState.Success)?.data.orEmpty()
  val notes = (notesState as? DataStreamState.Success)?.data.orEmpty()
  val decks = (decksState as? DataStreamState.Success)?.data.orEmpty()
  val savedCourses = (coursesState as? DataStreamState.Success)?.data.orEmpty()

  val currentUser = Firebase.auth.currentUser
  val userName = currentUser?.displayName ?: currentUser?.email ?: "Student"
  val userEmail = currentUser?.email.orEmpty()

  val snackbarHostState = remember { SnackbarHostState() }
  val drawerState = rememberDrawerState(DrawerValue.Closed)

  // Android TextToSpeech controller for Read Aloud
  var ttsReady by remember { mutableStateOf(false) }
  val tts = remember(context) {
    TextToSpeech(context.applicationContext) { status ->
      ttsReady = (status == TextToSpeech.SUCCESS)
    }
  }
  DisposableEffect(tts) {
    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {}
      override fun onDone(utteranceId: String?) {
        viewModel.setSpeakingMessageId(null)
      }
      @Deprecated("Deprecated in Java")
      override fun onError(utteranceId: String?) {
        viewModel.setSpeakingMessageId(null)
      }
    })
    onDispose {
      tts.stop()
      tts.shutdown()
    }
  }

  fun handleReadAloud(message: ChatMessage) {
    if (uiState.speakingMessageId == message.id) {
      tts.stop()
      viewModel.setSpeakingMessageId(null)
    } else if (ttsReady) {
      tts.language = preferences.language.toLocale()
      viewModel.setSpeakingMessageId(message.id)
      val plainText = message.content
        .replace(Regex("""```[\s\S]*?```"""), " Code snippet. ")
        .replace(Regex("""[*#`$]"""), "")
      tts.speak(plainText.take(3800), TextToSpeech.QUEUE_FLUSH, null, message.id)
    } else {
      viewModel.showNotice("Text-to-Speech engine is initializing.")
    }
  }

  LaunchedEffect(uiState.statusNotice) {
    val msg = uiState.statusNotice
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissNotice()
    }
  }

  val sidebarComposable: @Composable () -> Unit = {
    StudySidebarContent(
      sessions = sessions,
      activeSessionId = uiState.activeSessionId,
      searchQuery = uiState.sessionSearchQuery,
      onSearchQueryChange = viewModel::updateSearchQuery,
      onNewSession = {
        viewModel.startNewSession()
        scope.launch { drawerState.close() }
      },
      onSelectSession = { session ->
        viewModel.selectSession(session)
        scope.launch { drawerState.close() }
      },
      onDeleteSession = viewModel::deleteSession,
      savedCoursesCount = savedCourses.size,
      flashcardDecksCount = decks.size,
      studyNotesCount = notes.size,
      onOpenFolder = { folder ->
        viewModel.openFolderOverlay(folder)
        scope.launch { drawerState.close() }
      },
      onOpenSettings = {
        viewModel.setSettingsOpen(true)
        scope.launch { drawerState.close() }
      },
      userDisplayName = userName,
    )
  }

  val pluginDrawerBody: @Composable () -> Unit = {
    PluginDrawerContent(
      state = uiState,
      decks = decks,
      stats = stats,
      onSelectTab = viewModel::selectPluginTab,
      onClose = viewModel::closePluginDrawer,
      onCourseQueryChange = viewModel::updateCourseQuery,
      onCourseSubjectChange = viewModel::updateCourseSubject,
      onCourseLevelChange = viewModel::updateCourseLevel,
      onCourseCostChange = viewModel::updateCourseCost,
      onCourseLanguageChange = viewModel::updateCourseLanguage,
      onSearchCourses = viewModel::searchCourses,
      onSaveCourse = viewModel::saveCourse,
      onFlashcardTopicChange = viewModel::updateFlashcardTopic,
      onGenerateFlashcards = { viewModel.generateFlashcardsFromTopic() },
      onSelectDeck = viewModel::selectFlashcardDeck,
      onFlipCard = viewModel::flipActiveCard,
      onStepCard = viewModel::stepFlashcard,
      onMarkCardStatus = viewModel::markFlashcardStatus,
      onQuizTopicChange = viewModel::updateQuizTopic,
      onQuizCountChange = viewModel::updateQuizCount,
      onGenerateQuiz = viewModel::generateQuiz,
      onSelectQuizOption = viewModel::selectQuizOption,
      onSubmitQuiz = viewModel::submitQuiz,
      onResetQuiz = viewModel::resetQuizAttempt,
      onToggleTimer = viewModel::toggleTimer,
      onSwitchPomodoroPhase = viewModel::switchPomodoroPhase,
      onResetPomodoro = viewModel::resetPomodoro,
    )
  }

  val mainContent: @Composable (Boolean) -> Unit = { showMenuToggle ->
    Scaffold(
      containerColor = MaterialTheme.colorScheme.background,
      snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { _ ->
      when {
        uiState.isSettingsOpen -> {
          SettingsAndPluginsScreen(
            preferences = preferences,
            cachedResponseCount = uiState.cachedResponseCount,
            userEmail = userEmail,
            onBack = { viewModel.setSettingsOpen(false) },
            onToggleDarkMode = viewModel::setDarkMode,
            onSaveOpenAiKey = viewModel::saveOpenAiApiKey,
            onClearCache = viewModel::clearLocalCache,
            onSignOut = {
              tts.stop()
              signOutUser(credentialManager, scope)
            },
          )
        }

        uiState.activeFolderOverlay != null -> {
          FolderDetailScreen(
            folder = uiState.activeFolderOverlay!!,
            savedCourses = savedCourses,
            flashcardDecks = decks,
            studyNotes = notes,
            onBack = { viewModel.openFolderOverlay(null) },
            onDeleteCourse = viewModel::deleteSavedCourse,
            onOpenDeck = viewModel::selectFlashcardDeck,
            onDeleteDeck = viewModel::deleteFlashcardDeck,
            onCreateNote = viewModel::saveCustomNote,
            onDeleteNote = viewModel::deleteNote,
          )
        }

        else -> {
          Column(modifier = Modifier.fillMaxSize()) {
            StudyTopBar(
              showDrawerToggle = showMenuToggle,
              onOpenDrawer = { scope.launch { drawerState.open() } },
              selectedMode = preferences.modelMode,
              onSelectMode = viewModel::setModelMode,
              selectedLanguage = preferences.language,
              onSelectLanguage = viewModel::setLanguage,
              darkMode = preferences.darkMode,
              onToggleTheme = { viewModel.setDarkMode(!preferences.darkMode) },
              isPluginDrawerOpen = uiState.isPluginSheetOpen,
              onTogglePluginDrawer = {
                if (uiState.isPluginSheetOpen) viewModel.closePluginDrawer()
                else viewModel.openPluginDrawer()
              },
            )

            // Optional OpenAI Prompt Banner when ChatGPT/Dual mode is selected without key
            AnimatedVisibility(visible = uiState.openAiPromptNeeded) {
              Surface(
                color = luminaColors.brandPurple.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, luminaColors.brandPurple.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = luminaColors.brandCyan, modifier = Modifier.size(18.dp))
                  Text(
                    text = "Add your optional OpenAI key in Settings to enable ChatGPT answers. All Gemini modes remain active.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                  )
                  TextButton(onClick = { viewModel.setSettingsOpen(true) }) {
                    Text("Settings")
                  }
                }
              }
            }

            // Friendly Rate-Limit Retry Status Banner
            AnimatedVisibility(visible = !uiState.retryStatusMessage.isNullOrBlank()) {
              Surface(
                color = luminaColors.warning.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, luminaColors.warning.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
              ) {
                Text(
                  text = uiState.retryStatusMessage.orEmpty(),
                  style = MaterialTheme.typography.labelMedium,
                  color = luminaColors.warning,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
              }
            }

            // Center Chat Stream / Welcome View
            val listState = rememberLazyListState()
            LaunchedEffect(messages.size, uiState.streamingText) {
              if (messages.isNotEmpty() || uiState.streamingText.isNotEmpty()) {
                val targetIndex = (messages.size - 1).coerceAtLeast(0)
                runCatching { listState.animateScrollToItem(targetIndex) }
              }
            }

            Box(
              modifier = Modifier.weight(1f).fillMaxWidth(),
              contentAlignment = Alignment.TopCenter,
            ) {
              if (messages.isEmpty() && !uiState.isGenerating) {
                Box(
                  modifier = Modifier.fillMaxSize(),
                  contentAlignment = Alignment.Center,
                ) {
                  WelcomeQuickStart(
                    onQuickStartClick = viewModel::applyQuickStartPrompt,
                  )
                }
              } else {
                LazyColumn(
                  state = listState,
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
                ) {
                  items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                      message = msg,
                      isSpeaking = uiState.speakingMessageId == msg.id,
                      onCopy = { text ->
                        clipboard.setText(AnnotatedString(text))
                        viewModel.showNotice("Copied to clipboard.")
                      },
                      onReadAloud = { handleReadAloud(it) },
                      onSaveToNotes = viewModel::saveResponseToNotes,
                      onConvertToFlashcards = viewModel::convertMessageToFlashcards,
                      onSecondOpinion = viewModel::requestSecondOpinion,
                    )
                  }

                  if (uiState.isGenerating) {
                    item(key = "streaming_indicator") {
                      Surface(
                        color = luminaColors.elevatedCard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, luminaColors.brandBlue.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                      ) {
                        Column(
                          modifier = Modifier.padding(14.dp),
                          verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                          ) {
                            CircularProgressIndicator(
                              modifier = Modifier.size(16.dp),
                              strokeWidth = 2.dp,
                              color = luminaColors.brandCyan,
                            )
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = luminaColors.brandBlue, modifier = Modifier.size(16.dp))
                            Text(
                              text = uiState.streamingModelLabel.ifBlank { "Thinking..." },
                              style = MaterialTheme.typography.labelLarge,
                              color = luminaColors.brandCyan,
                            )
                          }
                          if (uiState.streamingText.isNotBlank()) {
                            MarkdownMathText(content = uiState.streamingText)
                          }
                        }
                      }
                    }
                  }
                }
              }
            }

            // Bottom Composer
            StudyComposer(
              text = uiState.composerText,
              onTextChange = viewModel::updateComposerText,
              pendingAttachment = uiState.pendingAttachment,
              onAttachmentChange = viewModel::setAttachment,
              activePlugins = uiState.activePlugins,
              onTogglePlugin = viewModel::togglePluginPill,
              selectedLanguage = preferences.language,
              isGenerating = uiState.isGenerating,
              isVoiceListening = uiState.isVoiceListening,
              onVoiceListeningChange = viewModel::setVoiceListening,
              onVoiceResult = viewModel::appendVoiceText,
              onErrorNotice = viewModel::showNotice,
              onSend = { viewModel.sendMessage() },
              onStop = viewModel::stopGeneration,
            )
          }
        }
      }
    }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpandedScreen = maxWidth >= 840.dp

    if (isExpandedScreen) {
      PermanentNavigationDrawer(
        drawerContent = {
          PermanentDrawerSheet(
            drawerContainerColor = luminaColors.sidebar,
            modifier = Modifier.width(290.dp),
          ) {
            sidebarComposable()
          }
        },
      ) {
        Row(modifier = Modifier.fillMaxSize()) {
          Box(modifier = Modifier.weight(1f)) {
            mainContent(false)
          }
          if (uiState.isPluginSheetOpen) {
            Row(modifier = Modifier.width(380.dp).fillMaxHeight()) {
              Box(
                modifier = Modifier
                  .width(1.dp)
                  .fillMaxHeight()
                  .background(luminaColors.borderSubtle),
              )
              Surface(
                color = luminaColors.elevatedCard,
                modifier = Modifier.fillMaxSize(),
              ) {
                pluginDrawerBody()
              }
            }
          }
        }
      }
    } else {
      ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
          ModalDrawerSheet(
            drawerContainerColor = luminaColors.sidebar,
            modifier = Modifier.width(304.dp),
          ) {
            sidebarComposable()
          }
        },
      ) {
        mainContent(true)
      }

      if (uiState.isPluginSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
          onDismissRequest = viewModel::closePluginDrawer,
          sheetState = sheetState,
          containerColor = luminaColors.elevatedCard,
        ) {
          Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.88f)) {
            pluginDrawerBody()
          }
        }
      }
    }
  }
}
