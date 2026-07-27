package com.professor.baseproject.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.professor.baseproject.model.DataModel
import kotlinx.coroutines.flow.Flow

/**
 * Template DAO. The `Flow` query is the one worth copying — it keeps the UI in sync
 * without manual refresh calls.
 */
@Dao
interface DataModelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: DataModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<DataModel>)

    @Upsert
    suspend fun upsert(data: DataModel)

    @Query("SELECT * FROM data_model ORDER BY id DESC")
    fun observeAll(): Flow<List<DataModel>>

    @Query("SELECT * FROM data_model ORDER BY id DESC")
    suspend fun getAll(): List<DataModel>

    @Query("SELECT * FROM data_model WHERE id = :id")
    suspend fun getById(id: Long): DataModel?

    @Delete
    suspend fun delete(data: DataModel)

    @Query("DELETE FROM data_model")
    suspend fun deleteAll()
}
