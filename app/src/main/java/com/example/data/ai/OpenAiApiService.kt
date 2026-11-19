package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiConnector(
  private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build(),
) {
  suspend fun generateStudyReply(
    apiKey: String,
    systemInstruction: String,
    historyMessages: List<Pair<String, String>>,
    userPrompt: String,
    model: String = "gpt-4o-mini",
  ): Result<String> = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) {
      return@withContext Result.failure(
        IllegalStateException("OpenAI API key is not configured. Add an optional key in Settings to use ChatGPT Plugin or Dual AI Compare.")
      )
    }

    try {
      val messagesArray = JSONArray().apply {
        put(
          JSONObject().apply {
            put("role", "system")
            put("content", systemInstruction)
          }
        )
        historyMessages.takeLast(8).forEach { (role, content) ->
          put(
            JSONObject().apply {
              put("role", if (role == "assistant") "assistant" else "user")
              put("content", content)
            }
          )
        }
        put(
          JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
          }
        )
      }

      val bodyJson = JSONObject().apply {
        put("model", model)
        put("messages", messagesArray)
        put("temperature", 0.4)
      }

      val request = Request.Builder()
        .url("https://api.openai.com/v1/chat/completions")
        .addHeader("Authorization", "Bearer ${apiKey.trim()}")
        .addHeader("Content-Type", "application/json")
        .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

      client.newCall(request).execute().use { response ->
        val rawBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
          val errorMsg = try {
            JSONObject(rawBody).optJSONObject("error")?.optString("message")
          } catch (_: Exception) {
            null
          }
          return@withContext Result.failure(
            IllegalStateException(
              errorMsg?.takeIf { it.isNotBlank() }
                ?: "OpenAI request failed (HTTP ${response.code}). Check your OpenAI API key in Settings."
            )
          )
        }
        val parsed = JSONObject(rawBody)
        val content = parsed
          .getJSONArray("choices")
          .getJSONObject(0)
          .getJSONObject("message")
          .getString("content")
          .trim()
        if (content.isBlank()) {
          Result.failure(IllegalStateException("OpenAI returned an empty response."))
        } else {
          Result.success(content)
        }
      }
    } catch (e: Exception) {
      Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to connect to OpenAI."))
    }
  }
}
