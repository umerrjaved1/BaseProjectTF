package com.professor.baseproject.data.repository

import com.professor.baseproject.data.db.DataModelDao
import com.professor.baseproject.model.DataModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataModelRepository @Inject constructor(
    private val dataModelDao: DataModelDao
) {
    suspend fun addFavorite(model: DataModel) = dataModelDao.insertData(model)
}
