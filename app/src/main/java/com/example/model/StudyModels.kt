package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class StudyModelMode(
  val wireName: String,
  val label: String,
  val shortLabel: String,
  val badge: String,
) {
  GEMINI_FAST("GEMINI_FAST", "Gemini Fast (High Limit)", "Gemini Fast", "High Limit"),
  GEMINI_DEEP("GEMINI_DEEP", "Gemini Deep Study", "Deep Study", "Reasoning"),
  CHATGPT("CHATGPT", "ChatGPT Plugin (OpenAI)", "ChatGPT", "OpenAI"),
  DUAL_COMPARE("DUAL_COMPARE", "Dual AI Compare (Gemini + ChatGPT)", "Dual Compare", "Side-by-Side");

  companion object {
    fun fromWire(raw: String?): StudyModelMode =
      entries.firstOrNull { it.wireName == raw } ?: GEMINI_FAST
  }
}

enum class StudyLanguage(
  val code: String,
  val displayName: String,
  val nativeName: String,
  val ttsLocaleTag: String,
) {
  AUTO("auto", "Auto-Detect", "Auto-detect language", "en"),
  ENGLISH("en", "English", "English", "en-US"),
  SPANISH("es", "Spanish", "Español", "es-ES"),
  FRENCH("fr", "French", "Français", "fr-FR"),
  PORTUGUESE("pt", "Portuguese", "Português", "pt-PT"),
  ITALIAN("it", "Italian", "Italiano", "it-IT"),
  ROMANIAN("ro", "Romanian", "Română", "ro-RO"),
  GERMAN("de", "German", "Deutsch", "de-DE"),
  DUTCH("nl", "Dutch", "Nederlands", "nl-NL"),
  POLISH("pl", "Polish", "Polski", "pl-PL"),
  LATIN("la", "Latin", "Latina", "it-IT");

  fun toLocale(): Locale = Locale.forLanguageTag(ttsLocaleTag)

  companion object {
    fun fromCode(code: String?): StudyLanguage =
      entries.firstOrNull { it.code == code } ?: AUTO
  }
}

enum class StudyPluginPill(val label: String, val instructionHint: String) {
  STEP_BY_STEP("Step-by-Step", "Break the concept down step-by-step with clear numbered reasoning and examples."),
  COURSE_SEARCH("Course Finder", "Include real, reputable worldwide courses or open university resources with direct URLs."),
  FLASHCARDS("Flashcard Prep", "Highlight key terms and Q&A pairs suitable for flashcards at the end."),
  QUIZ_PREP("Practice Quiz", "End with 2 quick practice questions and hidden/inline answers to check understanding."),
  EXAM_PROOF("Deep Proof", "Provide formal mathematical/logical rigor, edge cases, and derivation steps.");
}

enum class PluginDrawerTab(val title: String) {
  COURSES("Courses"),
  FLASHCARDS("Flashcards"),
  QUIZ("Quiz"),
  TIMER("Timer"),
}

enum class SidebarFolder(val title: String) {
  SAVED_COURSES("Saved Courses"),
  FLASHCARD_DECKS("Flashcard Decks"),
  STUDY_NOTES("Study Notes"),
}

@Keep
@IgnoreExtraProperties
data class StudySession(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "New Study Session",
  val preview: String = "",
  val modelMode: String = StudyModelMode.GEMINI_FAST.wireName,
  val languageCode: String = StudyLanguage.AUTO.code,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val userId: String = "",
  val sessionId: String = "",
  val role: String = "user",
  val content: String = "",
  val secondaryContent: String? = null,
  val modelLabel: String = "Gemini Fast",
  val secondaryModelLabel: String? = null,
  val attachmentLabel: String? = null,
  val sourceUrls: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudyNote(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val content: String = "",
  val languageCode: String = StudyLanguage.AUTO.code,
  val tags: List<String> = emptyList(),
  val sourceSessionId: String? = null,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FlashcardItem(
  val id: String = "",
  val front: String = "",
  val back: String = "",
  val status: String = "NEW", // NEW, KNOW_IT, REVIEW_AGAIN
)

@Keep
@IgnoreExtraProperties
data class FlashcardDeck(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val topic: String = "",
  val languageCode: String = StudyLanguage.AUTO.code,
  val cards: List<FlashcardItem> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class SavedCourse(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val provider: String = "",
  val language: String = "English",
  val level: String = "Beginner",
  val duration: String = "Self-paced",
  val cost: String = "Free",
  val url: String = "",
  val summary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class StudyStats(
  @DocumentId val id: String = "",
  val userId: String = "",
  val completedPomodoros: Long = 0L,
  val totalFocusMinutes: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class CourseSearchResult(
  val title: String,
  val provider: String,
  val language: String,
  val level: String,
  val duration: String,
  val cost: String,
  val url: String,
  val summary: String,
)

data class QuizQuestion(
  val id: Int,
  val question: String,
  val options: List<String>,
  val correctIndex: Int,
  val explanation: String,
)

data class ComposerAttachment(
  val displayName: String,
  val mimeType: String,
  val bytes: ByteArray,
  val isPdf: Boolean,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ComposerAttachment) return false
    return displayName == other.displayName && mimeType == other.mimeType && bytes.contentEquals(other.bytes)
  }

  override fun hashCode(): Int {
    var result = displayName.hashCode()
    result = 31 * result + mimeType.hashCode()
    result = 31 * result + bytes.contentHashCode()
    return result
  }
}
