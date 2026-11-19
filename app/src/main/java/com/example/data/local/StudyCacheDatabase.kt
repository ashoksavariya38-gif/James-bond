package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "cached_ai_responses")
data class CachedAiResponse(
  @PrimaryKey val cacheKey: String,
  val responseText: String,
  val modelLabel: String,
  val sourceUrlsJson: String,
  val cachedAtMillis: Long,
)

@Entity(tableName = "cached_course_searches")
data class CachedCourseSearch(
  @PrimaryKey val filterKey: String,
  val coursesJson: String,
  val cachedAtMillis: Long,
)

@Dao
interface StudyCacheDao {
  @Query("SELECT * FROM cached_ai_responses WHERE cacheKey = :key LIMIT 1")
  suspend fun getCachedResponse(key: String): CachedAiResponse?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putCachedResponse(entry: CachedAiResponse)

  @Query("SELECT * FROM cached_course_searches WHERE filterKey = :key LIMIT 1")
  suspend fun getCachedCourses(key: String): CachedCourseSearch?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun putCachedCourses(entry: CachedCourseSearch)

  @Query("DELETE FROM cached_ai_responses")
  suspend fun clearResponses()

  @Query("DELETE FROM cached_course_searches")
  suspend fun clearCourses()

  @Query("SELECT COUNT(*) FROM cached_ai_responses")
  suspend fun countCachedResponses(): Int
}

@Database(
  entities = [CachedAiResponse::class, CachedCourseSearch::class],
  version = 1,
  exportSchema = false,
)
abstract class StudyCacheDatabase : RoomDatabase() {
  abstract fun studyCacheDao(): StudyCacheDao

  companion object {
    @Volatile
    private var INSTANCE: StudyCacheDatabase? = null

    fun getInstance(context: Context): StudyCacheDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          StudyCacheDatabase::class.java,
          "luminastudy_cache.db",
        ).build().also { INSTANCE = it }
      }
  }
}
