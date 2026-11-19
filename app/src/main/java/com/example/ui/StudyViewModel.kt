package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiStudyRouter
import com.example.data.ai.StreamChunkEvent
import com.example.data.local.SettingsDataStore
import com.example.data.local.StudyCacheDao
import com.example.data.local.UserStudyPreferences
import com.example.data.firebase.StudyFirestoreRepository
import com.example.model.ChatMessage
import com.example.model.ComposerAttachment
import com.example.model.CourseSearchResult
import com.example.model.FlashcardDeck
import com.example.model.FlashcardItem
import com.example.model.PluginDrawerTab
import com.example.model.QuizQuestion
import com.example.model.SavedCourse
import com.example.model.SidebarFolder
import com.example.model.StudyLanguage
import com.example.model.StudyModelMode
import com.example.model.StudyNote
import com.example.model.StudyPluginPill
import com.example.model.StudySession
import com.example.model.StudyStats
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "StudyViewModel"

sealed interface DataStreamState<out T> {
  data object Loading : DataStreamState<Nothing>
  data class Success<T>(val data: T) : DataStreamState<T>
  data class Error(val message: String) : DataStreamState<Nothing>
}

enum class PomodoroPhase(val label: String, val defaultSeconds: Int) {
  FOCUS("Focus Session", 25 * 60),
  BREAK("Short Break", 5 * 60),
}

data class StudyWorkspaceState(
  val activeSessionId: String? = null,
  val sessionSearchQuery: String = "",
  val composerText: String = "",
  val pendingAttachment: ComposerAttachment? = null,
  val activePlugins: Set<StudyPluginPill> = setOf(StudyPluginPill.STEP_BY_STEP),
  val isGenerating: Boolean = false,
  val streamingText: String = "",
  val streamingModelLabel: String = "",
  val retryStatusMessage: String? = null,
  val statusNotice: String? = null,
  val openAiPromptNeeded: Boolean = false,
  val isPluginSheetOpen: Boolean = false,
  val activePluginTab: PluginDrawerTab = PluginDrawerTab.COURSES,
  val activeFolderOverlay: SidebarFolder? = null,
  val isSettingsOpen: Boolean = false,
  val isVoiceListening: Boolean = false,
  val speakingMessageId: String? = null,
  // Course Finder state
  val courseQuery: String = "",
  val courseSubject: String = "",
  val courseLevel: String = "All Levels",
  val courseCost: String = "All",
  val courseLanguage: StudyLanguage = StudyLanguage.AUTO,
  val isSearchingCourses: Boolean = false,
  val courseResults: List<CourseSearchResult> = emptyList(),
  val coursesFromCache: Boolean = false,
  val courseError: String? = null,
  // Flashcards state
  val flashcardTopicInput: String = "",
  val isGeneratingFlashcards: Boolean = false,
  val activeDeckId: String? = null,
  val activeCardIndex: Int = 0,
  val isCardFlipped: Boolean = false,
  val flashcardError: String? = null,
  // Quiz state
  val quizTopicInput: String = "",
  val quizQuestionCount: Int = 5,
  val isGeneratingQuiz: Boolean = false,
  val quizQuestions: List<QuizQuestion> = emptyList(),
  val quizAnswers: Map<Int, Int> = emptyMap(),
  val isQuizSubmitted: Boolean = false,
  val quizError: String? = null,
  // Pomodoro timer state
  val pomodoroPhase: PomodoroPhase = PomodoroPhase.FOCUS,
  val remainingSeconds: Int = PomodoroPhase.FOCUS.defaultSeconds,
  val isTimerRunning: Boolean = false,
  val cachedResponseCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModel(
  private val repository: StudyFirestoreRepository,
  private val cacheDao: StudyCacheDao,
  private val settingsDataStore: SettingsDataStore,
  private val aiRouter: AiStudyRouter,
  private val currentUserId: String,
) : ViewModel() {

  private val _uiState = MutableStateFlow(StudyWorkspaceState())
  val uiState: StateFlow<StudyWorkspaceState> = _uiState.asStateFlow()

  val preferences: StateFlow<UserStudyPreferences> = settingsDataStore.preferencesFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = UserStudyPreferences(),
    )

  val sessionsState: StateFlow<DataStreamState<List<StudySession>>> =
    repository.observeSessions(currentUserId)
      .toDataStreamState("Failed to load study sessions")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataStreamState.Loading)

  private val activeSessionIdFlow = _uiState.map { it.activeSessionId }

  val messagesState: StateFlow<DataStreamState<List<ChatMessage>>> =
    activeSessionIdFlow.flatMapLatest { sessionId ->
      if (sessionId.isNullOrBlank()) {
        flowOf(DataStreamState.Success(emptyList()))
      } else {
        repository.observeMessages(currentUserId, sessionId)
          .toDataStreamState("Failed to load messages")
      }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataStreamState.Success(emptyList()))

  val notesState: StateFlow<DataStreamState<List<StudyNote>>> =
    repository.observeNotes(currentUserId)
      .toDataStreamState("Failed to load study notes")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataStreamState.Loading)

  val flashcardDecksState: StateFlow<DataStreamState<List<FlashcardDeck>>> =
    repository.observeFlashcardDecks(currentUserId)
      .toDataStreamState("Failed to load flashcard decks")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataStreamState.Loading)

  val savedCoursesState: StateFlow<DataStreamState<List<SavedCourse>>> =
    repository.observeSavedCourses(currentUserId)
      .toDataStreamState("Failed to load saved courses")
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DataStreamState.Loading)

  val studyStatsState: StateFlow<StudyStats> =
    repository.observeStudyStats(currentUserId)
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w(TAG, "Error observing study stats", firestoreError ?: error)
        emit(StudyStats(id = "stats", userId = currentUserId))
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        StudyStats(id = "stats", userId = currentUserId),
      )

  private var generationJob: Job? = null
  private var timerJob: Job? = null

  init {
    refreshCacheCount()
  }

  private fun <T> kotlinx.coroutines.flow.Flow<T>.toDataStreamState(fallbackError: String) =
    this.map<T, DataStreamState<T>> { DataStreamState.Success(it) }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, fallbackError, root)
        emit(DataStreamState.Error(root.localizedMessage ?: fallbackError))
      }

  fun refreshCacheCount() {
    viewModelScope.launch {
      _uiState.update { it.copy(cachedResponseCount = cacheDao.countCachedResponses()) }
    }
  }

  fun clearLocalCache() {
    viewModelScope.launch {
      cacheDao.clearResponses()
      cacheDao.clearCourses()
      refreshCacheCount()
      showNotice("Local AI & course search cache cleared.")
    }
  }

  fun setDarkMode(enabled: Boolean) {
    viewModelScope.launch { settingsDataStore.setDarkMode(enabled) }
  }

  fun setModelMode(mode: StudyModelMode) {
    viewModelScope.launch {
      settingsDataStore.setModelMode(mode)
      val sessionId = _uiState.value.activeSessionId
      if (!sessionId.isNullOrBlank()) {
        repository.updateSession(currentUserId, sessionId, modelMode = mode)
      }
      if ((mode == StudyModelMode.CHATGPT || mode == StudyModelMode.DUAL_COMPARE) &&
        preferences.value.openAiApiKey.isBlank()
      ) {
        _uiState.update { it.copy(openAiPromptNeeded = true) }
      } else {
        _uiState.update { it.copy(openAiPromptNeeded = false) }
      }
    }
  }

  fun setLanguage(language: StudyLanguage) {
    viewModelScope.launch {
      settingsDataStore.setLanguage(language)
      val sessionId = _uiState.value.activeSessionId
      if (!sessionId.isNullOrBlank()) {
        repository.updateSession(currentUserId, sessionId, language = language)
      }
    }
  }

  fun saveOpenAiApiKey(apiKey: String) {
    viewModelScope.launch {
      settingsDataStore.setOpenAiApiKey(apiKey)
      _uiState.update { it.copy(openAiPromptNeeded = apiKey.isBlank()) }
      showNotice(if (apiKey.isBlank()) "OpenAI key removed." else "OpenAI key saved for ChatGPT & Dual Compare.")
    }
  }

  fun startNewSession() {
    generationJob?.cancel()
    _uiState.update {
      it.copy(
        activeSessionId = null,
        isGenerating = false,
        streamingText = "",
        retryStatusMessage = null,
        activeFolderOverlay = null,
        isSettingsOpen = false,
      )
    }
  }

  fun selectSession(session: StudySession) {
    generationJob?.cancel()
    _uiState.update {
      it.copy(
        activeSessionId = session.id,
        isGenerating = false,
        streamingText = "",
        retryStatusMessage = null,
        activeFolderOverlay = null,
        isSettingsOpen = false,
      )
    }
    viewModelScope.launch {
      settingsDataStore.setModelMode(StudyModelMode.fromWire(session.modelMode))
      settingsDataStore.setLanguage(StudyLanguage.fromCode(session.languageCode))
    }
  }

  fun deleteSession(sessionId: String) {
    viewModelScope.launch {
      repository.deleteSession(currentUserId, sessionId)
      if (_uiState.value.activeSessionId == sessionId) {
        _uiState.update { it.copy(activeSessionId = null) }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(sessionSearchQuery = query) }
  }

  fun updateComposerText(text: String) {
    _uiState.update { it.copy(composerText = text) }
  }

  fun appendVoiceText(spokenText: String) {
    if (spokenText.isBlank()) return
    _uiState.update { state ->
      val combined = if (state.composerText.isBlank()) spokenText.trim() else "${state.composerText.trimEnd()} ${spokenText.trim()}"
      state.copy(composerText = combined, isVoiceListening = false)
    }
  }

  fun setVoiceListening(listening: Boolean) {
    _uiState.update { it.copy(isVoiceListening = listening) }
  }

  fun setAttachment(attachment: ComposerAttachment?) {
    _uiState.update { it.copy(pendingAttachment = attachment) }
  }

  fun togglePluginPill(pill: StudyPluginPill) {
    _uiState.update { state ->
      val next = if (state.activePlugins.contains(pill)) {
        state.activePlugins - pill
      } else {
        state.activePlugins + pill
      }
      state.copy(activePlugins = next)
    }
  }

  fun openPluginDrawer(tab: PluginDrawerTab = _uiState.value.activePluginTab) {
    _uiState.update { it.copy(isPluginSheetOpen = true, activePluginTab = tab) }
  }

  fun closePluginDrawer() {
    _uiState.update { it.copy(isPluginSheetOpen = false) }
  }

  fun selectPluginTab(tab: PluginDrawerTab) {
    _uiState.update { it.copy(activePluginTab = tab) }
  }

  fun openFolderOverlay(folder: SidebarFolder?) {
    _uiState.update { it.copy(activeFolderOverlay = folder, isSettingsOpen = false) }
  }

  fun setSettingsOpen(open: Boolean) {
    _uiState.update { it.copy(isSettingsOpen = open, activeFolderOverlay = null) }
  }

  fun setSpeakingMessageId(messageId: String?) {
    _uiState.update { it.copy(speakingMessageId = messageId) }
  }

  fun dismissNotice() {
    _uiState.update { it.copy(statusNotice = null) }
  }

  fun showNotice(message: String) {
    _uiState.update { it.copy(statusNotice = message) }
  }

  fun stopGeneration() {
    generationJob?.cancel()
    _uiState.update {
      it.copy(
        isGenerating = false,
        streamingText = "",
        retryStatusMessage = null,
      )
    }
  }

  fun applyQuickStartPrompt(cardIndex: Int) {
    when (cardIndex) {
      0 -> {
        _uiState.update {
          it.copy(
            composerText = "Explain the chain rule in calculus step-by-step with an intuitive analogy and 2 worked examples.",
            activePlugins = setOf(StudyPluginPill.STEP_BY_STEP),
          )
        }
      }
      1 -> {
        openPluginDrawer(PluginDrawerTab.COURSES)
      }
      2 -> {
        _uiState.update {
          it.copy(
            composerText = "Create key study flashcards and a short practice quiz on cellular respiration.",
            activePlugins = setOf(StudyPluginPill.FLASHCARDS, StudyPluginPill.QUIZ_PREP),
          )
        }
      }
      3 -> {
        _uiState.update {
          it.copy(
            composerText = "Summarize the attached notes or PDF into structured bullet points, key formulas, and action items.",
            activePlugins = setOf(StudyPluginPill.STEP_BY_STEP),
          )
        }
      }
    }
  }

  fun sendMessage(overridePrompt: String? = null) {
    val state = _uiState.value
    val promptText = (overridePrompt ?: state.composerText).trim()
    val attachment = state.pendingAttachment
    if (promptText.isBlank() && attachment == null) return
    if (state.isGenerating) return

    val effectivePrompt = promptText.ifBlank { "Please analyze the attached ${attachment?.displayName ?: "study material"} step-by-step." }
    val currentPrefs = preferences.value
    val modelMode = currentPrefs.modelMode
    val language = currentPrefs.language
    val openAiKey = currentPrefs.openAiApiKey
    val activePlugins = state.activePlugins

    if ((modelMode == StudyModelMode.CHATGPT || modelMode == StudyModelMode.DUAL_COMPARE) && openAiKey.isBlank()) {
      _uiState.update { it.copy(openAiPromptNeeded = true) }
      if (modelMode == StudyModelMode.CHATGPT) {
        showNotice("Enter your optional OpenAI API key in Settings to use ChatGPT Plugin, or switch to Gemini.")
        return
      }
    }

    val historyPairs = (messagesState.value as? DataStreamState.Success)?.data
      ?.map { it.role to it.content }
      .orEmpty()

    _uiState.update {
      it.copy(
        composerText = if (overridePrompt == null) "" else it.composerText,
        pendingAttachment = null,
        isGenerating = true,
        streamingText = "",
        streamingModelLabel = modelMode.shortLabel,
        retryStatusMessage = null,
      )
    }

    generationJob = viewModelScope.launch {
      try {
        // Ensure Firestore session exists
        var sessionId = _uiState.value.activeSessionId
        if (sessionId.isNullOrBlank()) {
          val created = repository.createSession(
            userId = currentUserId,
            title = effectivePrompt.take(48),
            preview = effectivePrompt.take(120),
            modelMode = modelMode,
            language = language,
          )
          sessionId = created.getOrElse { err ->
            showNotice(err.localizedMessage ?: "Failed to create study session.")
            _uiState.update { it.copy(isGenerating = false) }
            return@launch
          }
          _uiState.update { it.copy(activeSessionId = sessionId) }
        }

        // Persist user message
        repository.addMessage(
          userId = currentUserId,
          sessionId = sessionId,
          role = "user",
          content = effectivePrompt,
          modelLabel = "Student",
          attachmentLabel = attachment?.displayName,
        )

        when (modelMode) {
          StudyModelMode.GEMINI_FAST, StudyModelMode.GEMINI_DEEP -> {
            var finalText = ""
            var finalLabel = modelMode.shortLabel
            var urls = emptyList<String>()

            aiRouter.streamGeminiTutorReply(
              prompt = effectivePrompt,
              language = language,
              activePlugins = activePlugins,
              mode = modelMode,
              history = historyPairs,
              attachment = attachment,
            ).collect { event ->
              when (event) {
                is StreamChunkEvent.RetryStatus -> {
                  _uiState.update { it.copy(retryStatusMessage = event.message) }
                }
                is StreamChunkEvent.Delta -> {
                  _uiState.update {
                    it.copy(
                      streamingText = event.accumulatedText,
                      streamingModelLabel = event.modelLabel,
                      retryStatusMessage = null,
                    )
                  }
                }
                is StreamChunkEvent.Completed -> {
                  finalText = event.text
                  finalLabel = event.modelLabel
                  urls = event.sourceUrls
                }
              }
            }

            repository.addMessage(
              userId = currentUserId,
              sessionId = sessionId,
              role = "assistant",
              content = finalText,
              modelLabel = finalLabel,
              sourceUrls = urls,
            )
            repository.updateSession(currentUserId, sessionId, preview = finalText.take(140))
          }

          StudyModelMode.CHATGPT -> {
            val reply = aiRouter.queryOpenAiReply(
              apiKey = openAiKey,
              prompt = effectivePrompt,
              language = language,
              activePlugins = activePlugins,
              history = historyPairs,
            ).getOrThrow()

            repository.addMessage(
              userId = currentUserId,
              sessionId = sessionId,
              role = "assistant",
              content = reply,
              modelLabel = "ChatGPT (OpenAI)",
            )
            repository.updateSession(currentUserId, sessionId, preview = reply.take(140))
          }

          StudyModelMode.DUAL_COMPARE -> {
            val geminiDeferred = async {
              var gText = ""
              var gLabel = "Gemini Fast"
              var gUrls = emptyList<String>()
              aiRouter.streamGeminiTutorReply(
                prompt = effectivePrompt,
                language = language,
                activePlugins = activePlugins,
                mode = StudyModelMode.GEMINI_FAST,
                history = historyPairs,
                attachment = attachment,
              ).collect { event ->
                when (event) {
                  is StreamChunkEvent.RetryStatus -> {
                    _uiState.update { it.copy(retryStatusMessage = event.message) }
                  }
                  is StreamChunkEvent.Delta -> {
                    _uiState.update {
                      it.copy(
                        streamingText = event.accumulatedText,
                        streamingModelLabel = "Gemini + ChatGPT Comparing...",
                        retryStatusMessage = null,
                      )
                    }
                  }
                  is StreamChunkEvent.Completed -> {
                    gText = event.text
                    gLabel = event.modelLabel
                    gUrls = event.sourceUrls
                  }
                }
              }
              Triple(gText, gLabel, gUrls)
            }

            val openAiDeferred = async {
              if (openAiKey.isBlank()) {
                Result.failure(IllegalStateException("Add an optional OpenAI API key in Settings to compare ChatGPT side-by-side."))
              } else {
                aiRouter.queryOpenAiReply(
                  apiKey = openAiKey,
                  prompt = effectivePrompt,
                  language = language,
                  activePlugins = activePlugins,
                  history = historyPairs,
                )
              }
            }

            val (gText, gLabel, gUrls) = geminiDeferred.await()
            val openAiRes = openAiDeferred.await()
            val secondaryText = openAiRes.fold(
              onSuccess = { it },
              onFailure = { "OpenAI unavailable: ${it.localizedMessage}" },
            )

            repository.addMessage(
              userId = currentUserId,
              sessionId = sessionId,
              role = "assistant",
              content = gText,
              modelLabel = gLabel,
              secondaryContent = secondaryText,
              secondaryModelLabel = "ChatGPT (OpenAI)",
              sourceUrls = gUrls,
            )
            repository.updateSession(currentUserId, sessionId, preview = gText.take(140))
          }
        }
        refreshCacheCount()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Message generation failed", e)
        showNotice(e.localizedMessage ?: "Failed to generate study response.")
      } finally {
        _uiState.update {
          it.copy(
            isGenerating = false,
            streamingText = "",
            retryStatusMessage = null,
          )
        }
      }
    }
  }

  fun requestSecondOpinion(message: ChatMessage) {
    val sessionId = _uiState.value.activeSessionId ?: return
    val messages = (messagesState.value as? DataStreamState.Success)?.data.orEmpty()
    val msgIndex = messages.indexOfFirst { it.id == message.id }
    val userPrompt = messages.take(msgIndex.coerceAtLeast(0))
      .lastOrNull { it.role == "user" }
      ?.content
      ?: "Provide a second perspective and verify the following explanation:\n\n${message.content.take(2000)}"

    val prefs = preferences.value
    val isCurrentlyChatGpt = message.modelLabel.contains("ChatGPT", ignoreCase = true)

    viewModelScope.launch {
      _uiState.update { it.copy(isGenerating = true, retryStatusMessage = "Asking second opinion...") }
      try {
        if (isCurrentlyChatGpt) {
          var geminiReply = ""
          aiRouter.streamGeminiTutorReply(
            prompt = userPrompt,
            language = prefs.language,
            activePlugins = _uiState.value.activePlugins,
            mode = StudyModelMode.GEMINI_DEEP,
            history = emptyList(),
            attachment = null,
          ).collect { event ->
            if (event is StreamChunkEvent.Completed) geminiReply = event.text
          }
          repository.updateMessageSecondOpinion(
            userId = currentUserId,
            sessionId = sessionId,
            messageId = message.id,
            secondaryContent = geminiReply,
            secondaryModelLabel = "Gemini Second Opinion",
          )
        } else {
          if (prefs.openAiApiKey.isBlank()) {
            _uiState.update { it.copy(openAiPromptNeeded = true) }
            showNotice("Add an optional OpenAI API key in Settings to request a ChatGPT second opinion.")
            return@launch
          }
          val openAiRes = aiRouter.queryOpenAiReply(
            apiKey = prefs.openAiApiKey,
            prompt = userPrompt,
            language = prefs.language,
            activePlugins = _uiState.value.activePlugins,
            history = emptyList(),
          )
          val text = openAiRes.getOrThrow()
          repository.updateMessageSecondOpinion(
            userId = currentUserId,
            sessionId = sessionId,
            messageId = message.id,
            secondaryContent = text,
            secondaryModelLabel = "ChatGPT Second Opinion",
          )
        }
        showNotice("Second opinion added below the response.")
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        showNotice(e.localizedMessage ?: "Failed to fetch second opinion.")
      } finally {
        _uiState.update { it.copy(isGenerating = false, retryStatusMessage = null) }
      }
    }
  }

  fun saveResponseToNotes(message: ChatMessage) {
    viewModelScope.launch {
      val firstLine = message.content.lines()
        .firstOrNull { it.trim().isNotBlank() }
        ?.replace("#", "")
        ?.trim()
        ?.take(80)
        ?: "Study Note"
      val res = repository.saveNote(
        userId = currentUserId,
        title = firstLine,
        content = message.content,
        language = preferences.value.language,
        tags = listOf(message.modelLabel.take(24)),
        sourceSessionId = _uiState.value.activeSessionId,
      )
      if (res.isSuccess) {
        showNotice("Saved to Study Notes.")
      } else {
        showNotice(res.exceptionOrNull()?.localizedMessage ?: "Could not save note.")
      }
    }
  }

  fun saveCustomNote(title: String, content: String) {
    viewModelScope.launch {
      val res = repository.saveNote(
        userId = currentUserId,
        title = title,
        content = content,
        language = preferences.value.language,
        tags = listOf("Personal"),
      )
      if (res.isSuccess) showNotice("Note saved.")
      else showNotice(res.exceptionOrNull()?.localizedMessage ?: "Failed to save note.")
    }
  }

  fun deleteNote(noteId: String) {
    viewModelScope.launch { repository.deleteNote(currentUserId, noteId) }
  }

  fun convertMessageToFlashcards(message: ChatMessage) {
    _uiState.update {
      it.copy(
        isPluginSheetOpen = true,
        activePluginTab = PluginDrawerTab.FLASHCARDS,
        flashcardTopicInput = message.content.take(500),
      )
    }
    generateFlashcardsFromTopic(message.content)
  }

  // --- Course Finder ---
  fun updateCourseQuery(value: String) { _uiState.update { it.copy(courseQuery = value) } }
  fun updateCourseSubject(value: String) { _uiState.update { it.copy(courseSubject = value) } }
  fun updateCourseLevel(value: String) { _uiState.update { it.copy(courseLevel = value) } }
  fun updateCourseCost(value: String) { _uiState.update { it.copy(courseCost = value) } }
  fun updateCourseLanguage(lang: StudyLanguage) { _uiState.update { it.copy(courseLanguage = lang) } }

  fun searchCourses() {
    val state = _uiState.value
    val q = state.courseQuery.trim()
    val subj = state.courseSubject.trim()
    if (q.isBlank() && subj.isBlank()) {
      _uiState.update { it.copy(courseError = "Enter a topic or subject to search worldwide courses.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isSearchingCourses = true, courseError = null) }
      val res = aiRouter.findWorldwideCourses(
        query = q,
        language = state.courseLanguage,
        subject = subj,
        level = state.courseLevel,
        cost = state.courseCost,
      )
      res.fold(
        onSuccess = { (courses, cached) ->
          _uiState.update {
            it.copy(
              isSearchingCourses = false,
              courseResults = courses,
              coursesFromCache = cached,
            )
          }
          refreshCacheCount()
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isSearchingCourses = false,
              courseError = err.localizedMessage ?: "Course search failed.",
            )
          }
        },
      )
    }
  }

  fun saveCourse(course: CourseSearchResult) {
    viewModelScope.launch {
      val res = repository.saveCourse(
        userId = currentUserId,
        title = course.title,
        provider = course.provider,
        language = course.language,
        level = course.level,
        duration = course.duration,
        cost = course.cost,
        url = course.url,
        summary = course.summary,
      )
      if (res.isSuccess) showNotice("Course saved to Saved Courses.")
      else showNotice(res.exceptionOrNull()?.localizedMessage ?: "Could not save course.")
    }
  }

  fun deleteSavedCourse(courseId: String) {
    viewModelScope.launch { repository.deleteSavedCourse(currentUserId, courseId) }
  }

  // --- Flashcards ---
  fun updateFlashcardTopic(value: String) { _uiState.update { it.copy(flashcardTopicInput = value) } }

  fun generateFlashcardsFromTopic(overrideInput: String? = null) {
    val input = (overrideInput ?: _uiState.value.flashcardTopicInput).trim()
    if (input.isBlank()) {
      _uiState.update { it.copy(flashcardError = "Enter a topic or paste study notes first.") }
      return
    }
    viewModelScope.launch {
      _uiState.update { it.copy(isGeneratingFlashcards = true, flashcardError = null) }
      val res = aiRouter.generateFlashcards(input, preferences.value.language)
      res.fold(
        onSuccess = { (deckTitle, cards) ->
          val saveRes = repository.saveFlashcardDeck(
            userId = currentUserId,
            title = deckTitle,
            topic = input.take(160),
            language = preferences.value.language,
            cards = cards,
          )
          _uiState.update {
            it.copy(
              isGeneratingFlashcards = false,
              activeDeckId = saveRes.getOrNull() ?: it.activeDeckId,
              activeCardIndex = 0,
              isCardFlipped = false,
            )
          }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isGeneratingFlashcards = false,
              flashcardError = err.localizedMessage ?: "Failed to generate flashcards.",
            )
          }
        },
      )
    }
  }

  fun selectFlashcardDeck(deckId: String) {
    _uiState.update {
      it.copy(
        activeDeckId = deckId,
        activeCardIndex = 0,
        isCardFlipped = false,
        activeFolderOverlay = null,
        isPluginSheetOpen = true,
        activePluginTab = PluginDrawerTab.FLASHCARDS,
      )
    }
  }

  fun flipActiveCard() {
    _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
  }

  fun stepFlashcard(delta: Int, deckSize: Int) {
    if (deckSize <= 0) return
    _uiState.update {
      val next = (it.activeCardIndex + delta).coerceIn(0, deckSize - 1)
      it.copy(activeCardIndex = next, isCardFlipped = false)
    }
  }

  fun markFlashcardStatus(deck: FlashcardDeck, cardIndex: Int, newStatus: String) {
    val cards = deck.cards.toMutableList()
    if (cardIndex !in cards.indices) return
    cards[cardIndex] = cards[cardIndex].copy(status = newStatus)
    viewModelScope.launch {
      repository.updateFlashcardDeckCards(currentUserId, deck.id, cards)
      val nextIndex = if (cardIndex + 1 < cards.size) cardIndex + 1 else 0
      _uiState.update { it.copy(activeCardIndex = nextIndex, isCardFlipped = false) }
    }
  }

  fun deleteFlashcardDeck(deckId: String) {
    viewModelScope.launch {
      repository.deleteFlashcardDeck(currentUserId, deckId)
      if (_uiState.value.activeDeckId == deckId) {
        _uiState.update { it.copy(activeDeckId = null, activeCardIndex = 0) }
      }
    }
  }

  // --- Quiz & Mock Test ---
  fun updateQuizTopic(value: String) { _uiState.update { it.copy(quizTopicInput = value) } }
  fun updateQuizCount(count: Int) { _uiState.update { it.copy(quizQuestionCount = count.coerceIn(5, 10)) } }

  fun generateQuiz() {
    val state = _uiState.value
    val topic = state.quizTopicInput.trim()
    if (topic.isBlank()) {
      _uiState.update { it.copy(quizError = "Enter a subject or topic to generate a quiz.") }
      return
    }
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          isGeneratingQuiz = true,
          quizError = null,
          quizQuestions = emptyList(),
          quizAnswers = emptyMap(),
          isQuizSubmitted = false,
        )
      }
      val res = aiRouter.generateQuiz(topic, state.quizQuestionCount, preferences.value.language)
      res.fold(
        onSuccess = { questions ->
          _uiState.update { it.copy(isGeneratingQuiz = false, quizQuestions = questions) }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isGeneratingQuiz = false,
              quizError = err.localizedMessage ?: "Failed to generate quiz.",
            )
          }
        },
      )
    }
  }

  fun selectQuizOption(questionId: Int, optionIndex: Int) {
    if (_uiState.value.isQuizSubmitted) return
    _uiState.update { state ->
      state.copy(quizAnswers = state.quizAnswers + (questionId to optionIndex))
    }
  }

  fun submitQuiz() {
    _uiState.update { it.copy(isQuizSubmitted = true) }
  }

  fun resetQuizAttempt() {
    _uiState.update { it.copy(quizAnswers = emptyMap(), isQuizSubmitted = false) }
  }

  // --- Pomodoro Study Timer ---
  fun toggleTimer() {
    if (_uiState.value.isTimerRunning) {
      timerJob?.cancel()
      _uiState.update { it.copy(isTimerRunning = false) }
    } else {
      _uiState.update { it.copy(isTimerRunning = true) }
      timerJob = viewModelScope.launch {
        while (_uiState.value.isTimerRunning && _uiState.value.remainingSeconds > 0) {
          delay(1000L)
          _uiState.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)) }
        }
        if (_uiState.value.remainingSeconds == 0 && _uiState.value.isTimerRunning) {
          val completedPhase = _uiState.value.pomodoroPhase
          if (completedPhase == PomodoroPhase.FOCUS) {
            repository.incrementPomodoroStats(currentUserId, 25L)
            showNotice("Focus session complete! Time for a 5-minute break.")
            _uiState.update {
              it.copy(
                isTimerRunning = false,
                pomodoroPhase = PomodoroPhase.BREAK,
                remainingSeconds = PomodoroPhase.BREAK.defaultSeconds,
              )
            }
          } else {
            showNotice("Break finished! Ready for your next focus session.")
            _uiState.update {
              it.copy(
                isTimerRunning = false,
                pomodoroPhase = PomodoroPhase.FOCUS,
                remainingSeconds = PomodoroPhase.FOCUS.defaultSeconds,
              )
            }
          }
        }
      }
    }
  }

  fun switchPomodoroPhase(phase: PomodoroPhase) {
    timerJob?.cancel()
    _uiState.update {
      it.copy(
        isTimerRunning = false,
        pomodoroPhase = phase,
        remainingSeconds = phase.defaultSeconds,
      )
    }
  }

  fun resetPomodoro() {
    timerJob?.cancel()
    val phase = _uiState.value.pomodoroPhase
    _uiState.update {
      it.copy(
        isTimerRunning = false,
        remainingSeconds = phase.defaultSeconds,
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    generationJob?.cancel()
    timerJob?.cancel()
  }
}
