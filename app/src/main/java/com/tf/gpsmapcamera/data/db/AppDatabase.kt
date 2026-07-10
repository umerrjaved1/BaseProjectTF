package com.tf.gpsmapcamera.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tf.gpsmapcamera.model.DataModel

@Database(entities = [DataModel::class], version = 1)
//@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): DataModelDao
}
