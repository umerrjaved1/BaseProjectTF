package com.tf.phonecleaner.booster.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_name")
data class DataModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val sequence: String,
    var dummyData: String)
