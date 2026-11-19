package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.StudyLanguage
import com.example.model.StudyModelMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyDataStore by preferencesDataStore(name = "lumina_study_prefs")

data class UserStudyPreferences(
  val darkMode: Boolean = true,
  val modelMode: StudyModelMode = StudyModelMode.GEMINI_FAST,
  val language: StudyLanguage = StudyLanguage.AUTO,
  val openAiApiKey: String = "",
)

class SettingsDataStore(private val context: Context) {
  private val keyDarkMode = booleanPreferencesKey("dark_mode")
  private val keyModelMode = stringPreferencesKey("model_mode")
  private val keyLanguageCode = stringPreferencesKey("language_code")
  private val keyOpenAiApiKey = stringPreferencesKey("openai_api_key")

  val preferencesFlow: Flow<UserStudyPreferences> = context.studyDataStore.data.map { prefs ->
    UserStudyPreferences(
      darkMode = prefs[keyDarkMode] ?: true,
      modelMode = StudyModelMode.fromWire(prefs[keyModelMode]),
      language = StudyLanguage.fromCode(prefs[keyLanguageCode]),
      openAiApiKey = prefs[keyOpenAiApiKey].orEmpty(),
    )
  }

  suspend fun setDarkMode(enabled: Boolean) {
    context.studyDataStore.edit { it[keyDarkMode] = enabled }
  }

  suspend fun setModelMode(mode: StudyModelMode) {
    context.studyDataStore.edit { it[keyModelMode] = mode.wireName }
  }

  suspend fun setLanguage(language: StudyLanguage) {
    context.studyDataStore.edit { it[keyLanguageCode] = language.code }
  }

  suspend fun setOpenAiApiKey(apiKey: String) {
    context.studyDataStore.edit { it[keyOpenAiApiKey] = apiKey.trim() }
  }
}
