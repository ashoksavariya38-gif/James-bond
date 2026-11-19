package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiStudyRouter
import com.example.data.firebase.StudyFirestoreRepository
import com.example.data.firebase.authStateFlow
import com.example.data.firebase.configureAppCheck
import com.example.data.local.SettingsDataStore
import com.example.data.local.StudyCacheDatabase
import com.example.data.local.UserStudyPreferences
import com.example.ui.StudyViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.LuminaStudyWorkspace
import com.example.ui.theme.LuminaStudyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Configure Firebase App Check before UI, ViewModels, or Firebase operations
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      AppNavigation()
    }
  }
}

@Composable
fun AppNavigation() {
  val context = LocalContext.current
  val settingsDataStore = remember(context) { SettingsDataStore(context.applicationContext) }
  val prefs by settingsDataStore.preferencesFlow.collectAsStateWithLifecycle(
    initialValue = UserStudyPreferences()
  )
  val currentUser by Firebase.auth.authStateFlow().collectAsStateWithLifecycle(
    initialValue = Firebase.auth.currentUser
  )

  LuminaStudyTheme(darkTheme = prefs.darkMode) {
    val user = currentUser
    if (user == null) {
      AuthScreen(onAuthSuccess = {})
    } else {
      key(user.uid) {
        val studyViewModel: StudyViewModel = viewModel(
          key = user.uid,
          factory = viewModelFactory {
            initializer {
              val app = checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
              val databaseId = app.getString(R.string.firestore_database_id)
              val db = FirebaseFirestore.getInstance(databaseId)
              val cacheDao = StudyCacheDatabase.getInstance(app).studyCacheDao()
              StudyViewModel(
                repository = StudyFirestoreRepository(db),
                cacheDao = cacheDao,
                settingsDataStore = SettingsDataStore(app),
                aiRouter = AiStudyRouter(cacheDao),
                currentUserId = user.uid,
              )
            }
          },
        )
        LuminaStudyWorkspace(viewModel = studyViewModel)
      }
    }
  }
}
