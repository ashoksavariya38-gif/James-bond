package com.example.data.firebase

import android.content.Context
import com.example.R
import com.example.model.ChatMessage
import com.example.model.FlashcardDeck
import com.example.model.FlashcardItem
import com.example.model.SavedCourse
import com.example.model.StudyLanguage
import com.example.model.StudyModelMode
import com.example.model.StudyNote
import com.example.model.StudySession
import com.example.model.StudyStats
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyFirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private fun requireUserId(expectedUserId: String): String {
    val uid = auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user Firestore access." }
    return uid
  }

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  fun observeSessions(userId: String): Flow<List<StudySession>> = flow {
    val uid = requireUserId(userId)
    val path = "users/$uid/sessions"
    emitAll(
      userDoc(uid).collection("sessions")
        .orderBy("updatedAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(StudySession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logAndRethrowIfFirestore(error, OperationType.LIST, path, uid)
        }
    )
  }

  fun observeMessages(userId: String, sessionId: String): Flow<List<ChatMessage>> = flow {
    val uid = requireUserId(userId)
    require(sessionId.isNotBlank()) { "Session ID must not be blank." }
    val path = "users/$uid/sessions/$sessionId/messages"
    emitAll(
      userDoc(uid).collection("sessions").document(sessionId).collection("messages")
        .orderBy("createdAt", Query.Direction.ASCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logAndRethrowIfFirestore(error, OperationType.LIST, path, uid)
        }
    )
  }

  fun observeNotes(userId: String): Flow<List<StudyNote>> = flow {
    val uid = requireUserId(userId)
    val path = "users/$uid/notes"
    emitAll(
      userDoc(uid).collection("notes")
        .orderBy("updatedAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(StudyNote::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logAndRethrowIfFirestore(error, OperationType.LIST, path, uid)
        }
    )
  }

  fun observeFlashcardDecks(userId: String): Flow<List<FlashcardDeck>> = flow {
    val uid = requireUserId(userId)
    val path = "users/$uid/flashcardDecks"
    emitAll(
      userDoc(uid).collection("flashcardDecks")
        .orderBy("updatedAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(FlashcardDeck::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logAndRethrowIfFirestore(error, OperationType.LIST, path, uid)
        }
    )
  }

  fun observeSavedCourses(userId: String): Flow<List<SavedCourse>> = flow {
    val uid = requireUserId(userId)
    val path = "users/$uid/savedCourses"
    emitAll(
      userDoc(uid).collection("savedCourses")
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(SavedCourse::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          logAndRethrowIfFirestore(error, OperationType.LIST, path, uid)
        }
    )
  }

  fun observeStudyStats(userId: String): Flow<StudyStats> = flow {
    val uid = requireUserId(userId)
    val path = "users/$uid/profile/stats"
    emitAll(
      userDoc(uid).collection("profile").document("stats")
        .snapshots()
        .map { snapshot ->
          snapshot.toObject(StudyStats::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            ?: StudyStats(id = "stats", userId = uid)
        }
        .catch { error ->
          logAndRethrowIfFirestore(error, OperationType.GET, path, uid)
        }
    )
  }

  suspend fun createSession(
    userId: String,
    title: String,
    preview: String,
    modelMode: StudyModelMode,
    language: StudyLanguage,
  ): Result<String> {
    var docPath = "users/$userId/sessions"
    return try {
      val uid = requireUserId(userId)
      val docRef = userDoc(uid).collection("sessions").document()
      docPath = docRef.path
      val safeTitle = title.trim().ifBlank { "New Study Session" }.take(160)
      val safePreview = preview.trim().take(400)
      val payload = mapOf(
        "userId" to uid,
        "title" to safeTitle,
        "preview" to safePreview,
        "modelMode" to modelMode.wireName,
        "languageCode" to language.code,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateSession(
    userId: String,
    sessionId: String,
    title: String? = null,
    preview: String? = null,
    modelMode: StudyModelMode? = null,
    language: StudyLanguage? = null,
  ): Result<Unit> {
    val docPath = "users/$userId/sessions/$sessionId"
    return try {
      val uid = requireUserId(userId)
      require(sessionId.isNotBlank()) { "Session ID must not be blank." }
      val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
      if (!title.isNullOrBlank()) updates["title"] = title.trim().take(160)
      if (preview != null) updates["preview"] = preview.trim().take(400)
      if (modelMode != null) updates["modelMode"] = modelMode.wireName
      if (language != null) updates["languageCode"] = language.code
      userDoc(uid).collection("sessions").document(sessionId).update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
    val docPath = "users/$userId/sessions/$sessionId"
    return try {
      val uid = requireUserId(userId)
      require(sessionId.isNotBlank()) { "Session ID must not be blank." }
      val sessionRef = userDoc(uid).collection("sessions").document(sessionId)
      val msgs = sessionRef.collection("messages").get().await()
      for (doc in msgs.documents) {
        doc.reference.delete().await()
      }
      sessionRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun addMessage(
    userId: String,
    sessionId: String,
    role: String,
    content: String,
    modelLabel: String,
    secondaryContent: String? = null,
    secondaryModelLabel: String? = null,
    attachmentLabel: String? = null,
    sourceUrls: List<String> = emptyList(),
  ): Result<String> {
    var docPath = "users/$userId/sessions/$sessionId/messages"
    return try {
      val uid = requireUserId(userId)
      require(sessionId.isNotBlank()) { "Session ID must not be blank." }
      val docRef = userDoc(uid).collection("sessions").document(sessionId)
        .collection("messages").document()
      docPath = docRef.path
      val safeUrls = sourceUrls.filter { it.isNotBlank() }.take(15).map { it.take(500) }
      val rawPayload = mapOf<String, Any?>(
        "userId" to uid,
        "sessionId" to sessionId,
        "role" to if (role == "assistant") "assistant" else "user",
        "content" to content.take(60000),
        "secondaryContent" to secondaryContent?.take(60000),
        "modelLabel" to modelLabel.take(80),
        "secondaryModelLabel" to secondaryModelLabel?.take(80),
        "attachmentLabel" to attachmentLabel?.take(200),
        "sourceUrls" to safeUrls,
        "createdAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      docRef.set(rawPayload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateMessageSecondOpinion(
    userId: String,
    sessionId: String,
    messageId: String,
    secondaryContent: String,
    secondaryModelLabel: String,
  ): Result<Unit> {
    val docPath = "users/$userId/sessions/$sessionId/messages/$messageId"
    return try {
      val uid = requireUserId(userId)
      require(sessionId.isNotBlank() && messageId.isNotBlank()) { "IDs must not be blank." }
      userDoc(uid).collection("sessions").document(sessionId)
        .collection("messages").document(messageId)
        .update(
          mapOf(
            "secondaryContent" to secondaryContent.take(60000),
            "secondaryModelLabel" to secondaryModelLabel.take(80),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun saveNote(
    userId: String,
    title: String,
    content: String,
    language: StudyLanguage,
    tags: List<String> = emptyList(),
    sourceSessionId: String? = null,
  ): Result<String> {
    var docPath = "users/$userId/notes"
    return try {
      val uid = requireUserId(userId)
      val safeTitle = title.trim().ifBlank { "Study Note" }.take(160)
      val safeContent = content.trim().ifBlank { return Result.failure(IllegalArgumentException("Note content cannot be empty.")) }.take(60000)
      val docRef = userDoc(uid).collection("notes").document()
      docPath = docRef.path
      val payload = mapOf<String, Any?>(
        "userId" to uid,
        "title" to safeTitle,
        "content" to safeContent,
        "languageCode" to language.code,
        "tags" to tags.take(10).map { it.take(40) },
        "sourceSessionId" to sourceSessionId?.takeIf { it.isNotBlank() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ).filterValues { it != null }
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
    val docPath = "users/$userId/notes/$noteId"
    return try {
      val uid = requireUserId(userId)
      userDoc(uid).collection("notes").document(noteId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun saveFlashcardDeck(
    userId: String,
    title: String,
    topic: String,
    language: StudyLanguage,
    cards: List<FlashcardItem>,
  ): Result<String> {
    var docPath = "users/$userId/flashcardDecks"
    return try {
      val uid = requireUserId(userId)
      val docRef = userDoc(uid).collection("flashcardDecks").document()
      docPath = docRef.path
      val safeCards = cards.take(60).map { card ->
        mapOf(
          "id" to card.id.ifBlank { java.util.UUID.randomUUID().toString() },
          "front" to card.front.take(500),
          "back" to card.back.take(1200),
          "status" to card.status,
        )
      }
      val payload = mapOf(
        "userId" to uid,
        "title" to title.trim().ifBlank { "Flashcard Deck" }.take(160),
        "topic" to topic.trim().ifBlank { "General Study" }.take(240),
        "languageCode" to language.code,
        "cards" to safeCards,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun updateFlashcardDeckCards(
    userId: String,
    deckId: String,
    cards: List<FlashcardItem>,
  ): Result<Unit> {
    val docPath = "users/$userId/flashcardDecks/$deckId"
    return try {
      val uid = requireUserId(userId)
      require(deckId.isNotBlank()) { "Deck ID must not be blank." }
      val safeCards = cards.take(60).map { card ->
        mapOf(
          "id" to card.id,
          "front" to card.front.take(500),
          "back" to card.back.take(1200),
          "status" to card.status,
        )
      }
      userDoc(uid).collection("flashcardDecks").document(deckId)
        .update(
          mapOf(
            "cards" to safeCards,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteFlashcardDeck(userId: String, deckId: String): Result<Unit> {
    val docPath = "users/$userId/flashcardDecks/$deckId"
    return try {
      val uid = requireUserId(userId)
      userDoc(uid).collection("flashcardDecks").document(deckId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun saveCourse(
    userId: String,
    title: String,
    provider: String,
    language: String,
    level: String,
    duration: String,
    cost: String,
    url: String,
    summary: String,
  ): Result<String> {
    var docPath = "users/$userId/savedCourses"
    return try {
      val uid = requireUserId(userId)
      val docRef = userDoc(uid).collection("savedCourses").document()
      docPath = docRef.path
      val payload = mapOf(
        "userId" to uid,
        "title" to title.trim().ifBlank { "Untitled Course" }.take(240),
        "provider" to provider.trim().ifBlank { "Open University" }.take(160),
        "language" to language.trim().ifBlank { "English" }.take(80),
        "level" to level.trim().ifBlank { "All Levels" }.take(60),
        "duration" to duration.trim().ifBlank { "Self-paced" }.take(100),
        "cost" to cost.trim().ifBlank { "Free" }.take(60),
        "url" to url.trim().ifBlank { "https://www.coursera.org" }.take(1000),
        "summary" to summary.trim().take(800),
        "createdAt" to FieldValue.serverTimestamp(),
      )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docPath)
      Result.failure(e)
    }
  }

  suspend fun deleteSavedCourse(userId: String, courseId: String): Result<Unit> {
    val docPath = "users/$userId/savedCourses/$courseId"
    return try {
      val uid = requireUserId(userId)
      userDoc(uid).collection("savedCourses").document(courseId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docPath)
      Result.failure(e)
    }
  }

  suspend fun incrementPomodoroStats(userId: String, focusMinutes: Long): Result<Unit> {
    val docPath = "users/$userId/profile/stats"
    return try {
      val uid = requireUserId(userId)
      val docRef = userDoc(uid).collection("profile").document("stats")
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        docRef.set(
          mapOf(
            "userId" to uid,
            "completedPomodoros" to 1L,
            "totalFocusMinutes" to focusMinutes.coerceAtLeast(1L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        docRef.update(
          mapOf(
            "completedPomodoros" to FieldValue.increment(1L),
            "totalFocusMinutes" to FieldValue.increment(focusMinutes.coerceAtLeast(1L)),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docPath)
      Result.failure(e)
    }
  }

  private fun logAndRethrowIfFirestore(
    error: Throwable,
    op: OperationType,
    path: String,
    uid: String,
  ) {
    val firestoreError = generateSequence(error) { it.cause }
      .filterIsInstance<FirebaseFirestoreException>()
      .firstOrNull()
    if (firestoreError == null && error is CancellationException) throw error
    val exceptionToLog = firestoreError ?: (error as? Exception)
    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
      handleFirestoreError(exceptionToLog, op, path)
    }
    throw error
  }
}
