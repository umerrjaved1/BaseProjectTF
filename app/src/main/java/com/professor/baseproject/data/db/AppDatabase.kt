package com.professor.baseproject.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.professor.baseproject.model.DataModel

/**
 * MIGRATIONS: this template is provisioned with `fallbackToDestructiveMigration()`
 * in [com.professor.baseproject.di.DiModule.DatabaseModule.provideDatabase], which WIPES
 * user data on a version bump. That is acceptable while a fork is pre-release.
 * Before shipping a version bump on a live app, remove that call and supply a real
 * `Migration` — otherwise every updating user loses their local data silently.
 */
@Database(entities = [DataModel::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataModelDao(): DataModelDao
}
