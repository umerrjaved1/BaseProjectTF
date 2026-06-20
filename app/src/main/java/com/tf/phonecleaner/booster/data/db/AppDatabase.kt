package com.tf.phonecleaner.booster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tf.phonecleaner.booster.model.DataModel

@Database(entities = [DataModel::class], version = 3)
//@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): DataModelDao
}
