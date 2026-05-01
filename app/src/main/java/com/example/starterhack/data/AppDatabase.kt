package com.example.starterhack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.starterhack.data.dao.CategoryDao
import com.example.starterhack.data.dao.DocumentDao
import com.example.starterhack.data.dao.TextSnippetDao
import com.example.starterhack.data.dao.VersionDao
import com.example.starterhack.data.entities.Document
import com.example.starterhack.data.entities.DocumentCategory
import com.example.starterhack.data.entities.DocumentVersion
import com.example.starterhack.data.entities.TextSnippet

@Database(
    entities = [Document::class, DocumentVersion::class, DocumentCategory::class, TextSnippet::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun versionDao(): VersionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun textSnippetDao(): TextSnippetDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "redacto.db",
                ).build().also { INSTANCE = it }
            }
    }
}
