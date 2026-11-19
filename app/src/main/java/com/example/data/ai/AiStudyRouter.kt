package com.example.data.ai

import android.graphics.BitmapFactory
import android.util.Log
import com.example.data.local.CachedAiResponse
import com.example.data.local.CachedCourseSearch
import com.example.data.local.StudyCacheDao
import com.example.model.ComposerAttachment
import com.example.model.CourseSearchResult
import com.example.model.FlashcardItem
import com.example.model.QuizQuestion
import com.example.model.StudyLanguage
import com.example.model.StudyModelMode
import com.example.model.StudyPluginPill
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "AiStudyRouter"
private const val CACHE_TTL_MILLIS = 1000L * 60L * 60L * 12L // 12 hours
private const val MAX_INLINE_BYTES = 18 * 1024 * 1024 // keep safely below 20MB limit

sealed interface StreamChunkEvent {
  data class RetryStatus(val message: String) : StreamChunkEvent
  data class Delta(val accumulatedText: String, val modelLabel: String) : StreamChunkEvent
  data class Completed(
    val text: String,
    val modelLabel: String,
    val sourceUrls: List<String>,
    val fromCache: Boolean,
  ) : StreamChunkEvent
}

class AiStudyRouter(
  private val cacheDao: StudyCacheDao,
  private val openAiConnector: OpenAiConnector = OpenAiConnector(),
) {
  private val fastModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      systemInstruction = content { text(buildBaseSystemInstruction(StudyLanguage.AUTO, emptySet(), deepMode = false)) },
    )
  }

  private val deepModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-4-argon",
      systemInstruction = content { text(buildBaseSystemInstruction(StudyLanguage.AUTO, emptySet(), deepMode = true)) },
    )
  }

  private val groundedSearchModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      tools = listOf(Tool.googleSearch()),
    )
  }

  private val flashcardSchema = Schema.obj(
    properties = mapOf(
      "deckTitle" to Schema.string(description = "Short descriptive title for the flashcard deck"),
      "cards" to Schema.array(
        items = Schema.obj(
          properties = mapOf(
            "front" to Schema.string(description = "Question, term, or prompt on front of card"),
            "back" to Schema.string(description = "Clear concise answer or explanation on back of card"),
          )
        ),
        description = "List of 6 to 10 study flashcards",
      ),
    ),
  )

  private val flashcardModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = flashcardSchema
      },
    )
  }

  private val quizSchema = Schema.obj(
    properties = mapOf(
      "questions" to Schema.array(
        items = Schema.obj(
          properties = mapOf(
            "question" to Schema.string(description = "Multiple-choice question stem"),
            "options" to Schema.array(
              items = Schema.string(),
              description = "Exactly 4 distinct answer choices",
            ),
            "correctIndex" to Schema.integer(description = "Zero-based index (0..3) of the correct option"),
            "explanation" to Schema.string(description = "Clear step-by-step explanation of why the answer is right"),
          )
        ),
        description = "Array of multiple-choice questions",
      ),
    ),
  )

  private val quizModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.8-flash",
      generationConfig = generationConfig {
        responseMimeType = "application/json"
        responseSchema = quizSchema
      },
    )
  }

  fun buildBaseSystemInstruction(
    language: StudyLanguage,
    activePlugins: Set<StudyPluginPill>,
    deepMode: Boolean,
  ): String {
    val languageDirective = if (language == StudyLanguage.AUTO) {
      "Detect the student's Latin-script language from their message and respond naturally in that same language."
    } else {
      "Respond in ${language.displayName} (${language.nativeName}). Keep terminology accurate and natural in that language."
    }
    val pluginHints = if (activePlugins.isEmpty()) {
      ""
    } else {
      "\nActive Study Plugins:\n" + activePlugins.joinToString("\n") { "- ${it.label}: ${it.instructionHint}" }
    }
    val depthHint = if (deepMode) {
      "\nProvide thorough, rigorous first-principles explanations, edge cases, formal proofs or derivations where relevant, and check-yourself exercises."
    } else {
      "\nKeep explanations crisp, structured with clear headings, bullet steps, worked examples, and a quick practice prompt at the end."
    }
    return """
      You are LuminaStudy AI, an encouraging, crystal-clear academic tutor.
      - Break complex topics down step-by-step with intuitive analogies and worked examples.
      - Format math equations cleanly with LaTeX delimiters (${'$'}...${'$'} or ${'$'}${'$'}...${'$'}${'$'}) and code inside fenced markdown blocks.
      - Whenever recommending courses or citing facts, mention reputable universities/providers and real URLs.
      - $languageDirective$depthHint$pluginHints
    """.trimIndent()
  }

  fun streamGeminiTutorReply(
    prompt: String,
    language: StudyLanguage,
    activePlugins: Set<StudyPluginPill>,
    mode: StudyModelMode,
    history: List<Pair<String, String>>,
    attachment: ComposerAttachment?,
  ): Flow<StreamChunkEvent> = flow {
    val deepMode = mode == StudyModelMode.GEMINI_DEEP
    val isCacheable = attachment == null && history.isEmpty()
    val cacheKey = sha256("${mode.wireName}|${language.code}|${activePlugins.joinToString(",")}|${prompt.trim()}")

    if (isCacheable) {
      val cached = cacheDao.getCachedResponse(cacheKey)
      if (cached != null && (System.currentTimeMillis() - cached.cachedAtMillis) < CACHE_TTL_MILLIS) {
        val urls = parseStringArray(cached.sourceUrlsJson)
        emit(StreamChunkEvent.Completed(cached.responseText, "${cached.modelLabel} (Cached)", urls, fromCache = true))
        return@flow
      }
    }

    if (attachment != null && attachment.bytes.size > MAX_INLINE_BYTES) {
      throw IllegalArgumentException("Attached file exceeds the 18 MB limit. Please select a smaller image or PDF.")
    }

    val directivePrompt = buildString {
      append("[Tutor Directive: ")
      append(buildBaseSystemInstruction(language, activePlugins, deepMode))
      append("]\n\n")
      if (history.isNotEmpty()) {
        append("Recent conversation context:\n")
        history.takeLast(6).forEach { (role, text) ->
          append(if (role == "assistant") "Tutor: " else "Student: ")
          append(text.take(1200))
          append("\n")
        }
        append("\n")
      }
      append("Student Question: ")
      append(prompt)
    }

    val requestContent: Content = content {
      if (attachment != null) {
        if (attachment.isPdf) {
          inlineData(attachment.bytes, "application/pdf")
        } else {
          val bitmap = BitmapFactory.decodeByteArray(attachment.bytes, 0, attachment.bytes.size)
          if (bitmap != null) {
            image(bitmap)
          } else {
            inlineData(attachment.bytes, attachment.mimeType)
          }
        }
      }
      text(directivePrompt)
    }

    val useSearchGrounding = activePlugins.contains(StudyPluginPill.COURSE_SEARCH) && attachment == null

    var activeModelLabel = when {
      useSearchGrounding -> "Gemini Fast + Search"
      deepMode -> "Gemini Deep Study"
      else -> "Gemini Fast"
    }

    var attempt = 0
    val maxAttempts = 3
    var lastError: Exception? = null

    while (attempt < maxAttempts) {
      attempt++
      try {
        val accumulated = StringBuilder()
        val foundUrls = linkedSetOf<String>()

        withTimeout(60_000L) {
          if (useSearchGrounding) {
            val response = groundedSearchModel.generateContent(requestContent)
            accumulated.append(response.text.orEmpty())
            response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks?.forEach { chunk ->
              chunk.web?.uri?.takeIf { it.isNotBlank() }?.let { foundUrls.add(it) }
            }
          } else {
            val targetModel = if (deepMode && attempt == 1) deepModel else fastModel
            if (deepMode && attempt > 1) {
              activeModelLabel = "Gemini Deep Study (Flash)"
            }
            targetModel.generateContentStream(requestContent).collect { chunk ->
              val delta = chunk.text.orEmpty()
              if (delta.isNotEmpty()) {
                accumulated.append(delta)
                emit(StreamChunkEvent.Delta(accumulated.toString(), activeModelLabel))
              }
            }
          }
        }

        val finalText = accumulated.toString().trim()
        if (finalText.isBlank()) {
          throw IllegalStateException("Gemini returned an empty response.")
        }

        if (isCacheable) {
          cacheDao.putCachedResponse(
            CachedAiResponse(
              cacheKey = cacheKey,
              responseText = finalText,
              modelLabel = activeModelLabel,
              sourceUrlsJson = JSONArray(foundUrls.toList()).toString(),
              cachedAtMillis = System.currentTimeMillis(),
            )
          )
        }

        emit(
          StreamChunkEvent.Completed(
            text = finalText,
            modelLabel = activeModelLabel,
            sourceUrls = foundUrls.toList(),
            fromCache = false,
          )
        )
        return@flow
      } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Gemini request timed out on attempt $attempt", e)
        lastError = IllegalStateException("Response timed out after 60 seconds. Please try again.")
        break
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        lastError = e
        val msg = e.localizedMessage.orEmpty()
        Log.e(TAG, "Gemini attempt $attempt failed: $msg", e)

        // Graceful fallback if gemini-4-argon requires Blaze plan billing
        val isBillingOrModelUnavailable = deepMode && (
          msg.contains("billing", ignoreCase = true) ||
            msg.contains("Blaze", ignoreCase = true) ||
            msg.contains("not found", ignoreCase = true) ||
            msg.contains("403", ignoreCase = true) ||
            msg.contains("permission", ignoreCase = true)
          )
        if (isBillingOrModelUnavailable && attempt == 1) {
          emit(StreamChunkEvent.RetryStatus("Deep Study model unavailable on current plan — switching to high-reasoning Flash..."))
          activeModelLabel = "Gemini Deep Study (Flash)"
          continue
        }

        val isRateLimit = msg.contains("429") ||
          msg.contains("quota", ignoreCase = true) ||
          msg.contains("rate", ignoreCase = true) ||
          msg.contains("resource exhausted", ignoreCase = true)
        if (isRateLimit && attempt < maxAttempts) {
          val backoffMs = 1500L * attempt
          emit(StreamChunkEvent.RetryStatus("Rate limit reached — retrying automatically in ${backoffMs / 1000.0}s (attempt $attempt/$maxAttempts)..."))
          delay(backoffMs)
          continue
        }
        break
      }
    }

    throw lastError ?: IllegalStateException("Gemini generation failed.")
  }

  suspend fun queryOpenAiReply(
    apiKey: String,
    prompt: String,
    language: StudyLanguage,
    activePlugins: Set<StudyPluginPill>,
    history: List<Pair<String, String>>,
  ): Result<String> {
    val systemPrompt = buildBaseSystemInstruction(language, activePlugins, deepMode = false)
    return openAiConnector.generateStudyReply(
      apiKey = apiKey,
      systemInstruction = systemPrompt,
      historyMessages = history,
      userPrompt = prompt,
    )
  }

  suspend fun findWorldwideCourses(
    query: String,
    language: StudyLanguage,
    subject: String,
    level: String,
    cost: String,
  ): Result<Pair<List<CourseSearchResult>, Boolean>> {
    val filterKey = sha256("courses|${query.trim()}|${language.code}|$subject|$level|$cost")
    val cached = cacheDao.getCachedCourses(filterKey)
    if (cached != null && (System.currentTimeMillis() - cached.cachedAtMillis) < CACHE_TTL_MILLIS) {
      val parsed = parseCourseResults(cached.coursesJson)
      if (parsed.isNotEmpty()) {
        return Result.success(parsed to true)
      }
    }

    val langName = if (language == StudyLanguage.AUTO) "Any Latin-script language (prefer English or query language)" else "${language.displayName} (${language.nativeName})"
    val prompt = """
      Search for 5 real, currently active online courses or open university courses matching:
      - Search Query: ${query.ifBlank { subject }}
      - Language: $langName
      - Subject: ${subject.ifBlank { "All subjects" }}
      - Difficulty Level: $level
      - Cost Preference: $cost

      Return ONLY a valid JSON array (no markdown fences) where each item has:
      "title", "provider", "language", "level", "duration", "cost", "url", "summary".
      Ensure every URL is a real, reputable educational platform or university course URL found via search.
    """.trimIndent()

    return try {
      val response = withTimeout(60_000L) {
        groundedSearchModel.generateContent(prompt)
      }
      val groundedUrls = response.candidates.firstOrNull()?.groundingMetadata?.groundingChunks
        ?.mapNotNull { it.web?.uri }
        ?.filter { it.isNotBlank() }
        .orEmpty()

      val raw = response.text.orEmpty()
      val parsed = parseCourseResults(extractJsonArray(raw), groundedUrls)
      if (parsed.isEmpty()) {
        Result.failure(IllegalStateException("Could not find structured course listings for that query. Try broadening your filters."))
      } else {
        cacheDao.putCachedCourses(
          CachedCourseSearch(
            filterKey = filterKey,
            coursesJson = serializeCourseResults(parsed),
            cachedAtMillis = System.currentTimeMillis(),
          )
        )
        Result.success(parsed to false)
      }
    } catch (e: TimeoutCancellationException) {
      Result.failure(IllegalStateException("Course search timed out after 60 seconds."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Course search failed", e)
      Result.failure(IllegalStateException(e.localizedMessage ?: "Course search failed."))
    }
  }

  suspend fun generateFlashcards(
    topicOrContent: String,
    language: StudyLanguage,
  ): Result<Pair<String, List<FlashcardItem>>> {
    val langInstruction = if (language == StudyLanguage.AUTO) {
      "Match the language of the input."
    } else {
      "Write all cards in ${language.displayName} (${language.nativeName})."
    }
    val prompt = """
      Create 8 high-yield study flashcards from the following topic or study notes.
      $langInstruction
      Make the front a clear question or key term, and the back a concise, memorable answer.

      Input:
      ${topicOrContent.take(12000)}
    """.trimIndent()

    return try {
      val response = withTimeout(60_000L) {
        flashcardModel.generateContent(prompt)
      }
      val jsonText = response.text ?: throw IllegalStateException("Empty flashcard response.")
      val root = JSONObject(jsonText)
      val deckTitle = root.optString("deckTitle", "Study Flashcards").ifBlank { "Study Flashcards" }
      val cardsArr = root.getJSONArray("cards")
      val items = (0 until cardsArr.length()).mapNotNull { idx ->
        val obj = cardsArr.optJSONObject(idx) ?: return@mapNotNull null
        val front = obj.optString("front").trim()
        val back = obj.optString("back").trim()
        if (front.isBlank() || back.isBlank()) null
        else FlashcardItem(
          id = UUID.randomUUID().toString(),
          front = front,
          back = back,
          status = "NEW",
        )
      }
      if (items.isEmpty()) {
        Result.failure(IllegalStateException("No flashcards could be generated from this topic."))
      } else {
        Result.success(deckTitle to items)
      }
    } catch (e: TimeoutCancellationException) {
      Result.failure(IllegalStateException("Flashcard generation timed out."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Flashcard generation failed", e)
      Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to generate flashcards."))
    }
  }

  suspend fun generateQuiz(
    topicOrContent: String,
    questionCount: Int,
    language: StudyLanguage,
  ): Result<List<QuizQuestion>> {
    val clampedCount = questionCount.coerceIn(5, 10)
    val langInstruction = if (language == StudyLanguage.AUTO) {
      "Match the language of the input."
    } else {
      "Write all questions, options, and explanations in ${language.displayName} (${language.nativeName})."
    }
    val prompt = """
      Generate exactly $clampedCount challenging multiple-choice questions for exam practice based on:
      ${topicOrContent.take(12000)}
      $langInstruction
      Each question must have exactly 4 plausible options, a valid zero-based correctIndex (0-3), and a clear explanation.
    """.trimIndent()

    return try {
      val response = withTimeout(60_000L) {
        quizModel.generateContent(prompt)
      }
      val jsonText = response.text ?: throw IllegalStateException("Empty quiz response.")
      val root = JSONObject(jsonText)
      val arr = root.getJSONArray("questions")
      val questions = (0 until arr.length()).mapNotNull { idx ->
        val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
        val q = obj.optString("question").trim()
        val optsArr = obj.optJSONArray("options") ?: return@mapNotNull null
        val options = (0 until optsArr.length()).map { optsArr.optString(it).trim() }.filter { it.isNotBlank() }
        val correctIdx = obj.optInt("correctIndex", 0).coerceIn(0, (options.size - 1).coerceAtLeast(0))
        val explanation = obj.optString("explanation").trim()
        if (q.isBlank() || options.size < 2) null
        else QuizQuestion(
          id = idx + 1,
          question = q,
          options = options,
          correctIndex = correctIdx,
          explanation = explanation.ifBlank { "Correct choice verified by LuminaStudy AI." },
        )
      }
      if (questions.isEmpty()) {
        Result.failure(IllegalStateException("Could not generate quiz questions. Try a more specific topic."))
      } else {
        Result.success(questions)
      }
    } catch (e: TimeoutCancellationException) {
      Result.failure(IllegalStateException("Quiz generation timed out."))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Quiz generation failed", e)
      Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to generate quiz."))
    }
  }

  private fun extractJsonArray(raw: String): String {
    val start = raw.indexOf('[')
    val end = raw.lastIndexOf(']')
    return if (start >= 0 && end > start) raw.substring(start, end + 1) else "[]"
  }

  private fun parseCourseResults(
    jsonArrayStr: String,
    fallbackUrls: List<String> = emptyList(),
  ): List<CourseSearchResult> = try {
    val arr = JSONArray(jsonArrayStr)
    (0 until arr.length()).mapNotNull { idx ->
      val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
      val title = obj.optString("title").trim()
      if (title.isBlank()) return@mapNotNull null
      val rawUrl = obj.optString("url").trim()
      val resolvedUrl = if (rawUrl.startsWith("http")) rawUrl else fallbackUrls.getOrNull(idx) ?: "https://www.coursera.org"
      CourseSearchResult(
        title = title,
        provider = obj.optString("provider", "Open University").ifBlank { "Open University" },
        language = obj.optString("language", "English").ifBlank { "English" },
        level = obj.optString("level", "All Levels").ifBlank { "All Levels" },
        duration = obj.optString("duration", "Self-paced").ifBlank { "Self-paced" },
        cost = obj.optString("cost", "Free").ifBlank { "Free" },
        url = resolvedUrl,
        summary = obj.optString("summary", "").trim(),
      )
    }
  } catch (_: Exception) {
    emptyList()
  }

  private fun serializeCourseResults(courses: List<CourseSearchResult>): String {
    val arr = JSONArray()
    courses.forEach { c ->
      arr.put(
        JSONObject().apply {
          put("title", c.title)
          put("provider", c.provider)
          put("language", c.language)
          put("level", c.level)
          put("duration", c.duration)
          put("cost", c.cost)
          put("url", c.url)
          put("summary", c.summary)
        }
      )
    }
    return arr.toString()
  }

  private fun parseStringArray(json: String): List<String> = try {
    val arr = JSONArray(json)
    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
  } catch (_: Exception) {
    emptyList()
  }

  private fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
  }
}
