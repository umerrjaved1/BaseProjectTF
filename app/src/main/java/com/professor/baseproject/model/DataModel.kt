package com.professor.baseproject.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Template Room entity. Replace these fields with whatever the forked app persists.
 *
 * When you change this shape after the app has shipped, bump the version in
 * [com.professor.baseproject.data.db.AppDatabase] and add a real Migration — see the
 * note in that file.
 */
@Keep
@Entity(tableName = "data_model")
data class DataModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Example scalar column. */
    val label: String = "",

    /** Example column persisted through [com.professor.baseproject.data.db.Converters]. */
    val tags: List<String> = emptyList()
)
