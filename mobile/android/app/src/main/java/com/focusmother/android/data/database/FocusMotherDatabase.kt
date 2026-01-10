package com.focusmother.android.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focusmother.android.data.dao.AgreementDao
import com.focusmother.android.data.dao.AppCategoryDao
import com.focusmother.android.data.dao.AvatarDao
import com.focusmother.android.data.dao.ConversationDao
import com.focusmother.android.data.entity.Agreement
import com.focusmother.android.data.entity.AppCategoryMapping
import com.focusmother.android.data.entity.AvatarConfig
import com.focusmother.android.data.entity.ConversationMessage

/**
 * Room database for FocusMother app.
 *
 * Manages all local data storage including:
 * - Time agreements between user and AI avatar
 * - Conversation history with the AI
 * - App category mappings and blocking rules
 * - 3D avatar configuration
 *
 * This class implements the singleton pattern to ensure only one database instance
 * exists throughout the application lifecycle, preventing memory leaks and ensuring
 * data consistency.
 *
 * Database version: 1
 * Export schema: false (set to true in production for migrations)
 */
@Database(
    entities = [
        Agreement::class,
        ConversationMessage::class,
        AppCategoryMapping::class,
        AvatarConfig::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FocusMotherDatabase : RoomDatabase() {
    
    /**
     * Provides access to Agreement data operations.
     */
    abstract fun agreementDao(): AgreementDao
    
    /**
     * Provides access to ConversationMessage data operations.
     */
    abstract fun conversationDao(): ConversationDao
    
    /**
     * Provides access to AppCategoryMapping data operations.
     */
    abstract fun appCategoryDao(): AppCategoryDao
    
    /**
     * Provides access to AvatarConfig data operations.
     */
    abstract fun avatarDao(): AvatarDao
    
    companion object {
        /**
         * Singleton instance of the database.
         * Volatile ensures that changes to INSTANCE are immediately visible to all threads.
         */
        @Volatile
        private var INSTANCE: FocusMotherDatabase? = null
        
        /**
         * Gets the singleton database instance.
         *
         * Uses double-checked locking pattern for thread-safe lazy initialization.
         * The synchronized block ensures only one thread can create the database instance.
         *
         * @param context Application context
         * @return Singleton database instance
         */
        fun getDatabase(context: Context): FocusMotherDatabase {
            // Return existing instance if available (first check)
            return INSTANCE ?: synchronized(this) {
                // Double-check inside synchronized block
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusMotherDatabase::class.java,
                    "focus_mother_database"
                )
                    .fallbackToDestructiveMigration() // For development - remove in production
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Creates an in-memory database for testing.
         *
         * In-memory databases are cleared when the process is killed and are ideal for testing
         * as they don't persist data between test runs.
         *
         * @param context Application context (use ApplicationProvider.getApplicationContext() in tests)
         * @return In-memory database instance
         */
        fun getInMemoryDatabase(context: Context): FocusMotherDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                FocusMotherDatabase::class.java
            )
                .allowMainThreadQueries() // Only for testing
                .build()
        }
    }
}
