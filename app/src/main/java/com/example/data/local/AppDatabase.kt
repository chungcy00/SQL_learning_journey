package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KpProgressEntity::class,
        QuestionAttemptEntity::class,
        WrongRecordEntity::class,
        FlashcardStateEntity::class,
        FlashcardEventEntity::class,
        FlashcardSessionEntity::class,
        UserFlashcardEntity::class,
        FavoriteEntity::class,
        NoteEntity::class,
        SqlDraftEntity::class,
        QueryHistoryEntity::class,
        PersonalDatasetEntity::class,
        StudyDailyLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kpProgressDao(): KpProgressDao
    abstract fun questionAttemptDao(): QuestionAttemptDao
    abstract fun wrongRecordDao(): WrongRecordDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun userAssetsDao(): UserAssetsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sql_learning_user_v1.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
