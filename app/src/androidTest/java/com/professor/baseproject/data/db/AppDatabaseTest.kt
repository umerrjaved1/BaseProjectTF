package com.professor.baseproject.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.professor.baseproject.model.DataModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is the test that proves the Room setup actually works.
 *
 * `room-ktx` was previously declared without `room-compiler`, so `AppDatabase_Impl` was
 * never generated and the very first injection of AppDatabase would have thrown
 * `RuntimeException: cannot find implementation for AppDatabase`. Nothing caught it
 * because nothing in the app injected the database. Merely opening the DB here fails the
 * build if the KSP processor is not wired up.
 *
 * It also exercises the @TypeConverters path, which only works if DataModel is a real
 * @Entity with a @PrimaryKey.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DataModelDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.dataModelDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun databaseImplementationIsGenerated() {
        // Reaching here at all means AppDatabase_Impl exists and the DAO resolved.
        assertNotNull(db)
        assertNotNull(dao)
    }

    @Test
    fun insertAndReadBack() = runTest {
        val id = dao.insert(DataModel(label = "first", tags = listOf("a", "b")))
        assertTrue("autoGenerate should hand back a positive row id", id > 0)

        val loaded = dao.getById(id)
        assertNotNull(loaded)
        assertEquals("first", loaded!!.label)
        assertEquals(listOf("a", "b"), loaded.tags)
    }

    @Test
    fun tagsSurviveTheTypeConverter() = runTest {
        val tags = listOf("one", "two,with-comma", "三")
        val id = dao.insert(DataModel(label = "converted", tags = tags))
        assertEquals(tags, dao.getById(id)?.tags)
    }

    @Test
    fun emptyTagListRoundTrips() = runTest {
        val id = dao.insert(DataModel(label = "no tags", tags = emptyList()))
        assertEquals(emptyList<String>(), dao.getById(id)?.tags)
    }

    @Test
    fun observeAllEmitsCurrentContents() = runTest {
        dao.insertAll(
            listOf(
                DataModel(label = "a"),
                DataModel(label = "b")
            )
        )
        val rows = dao.observeAll().first()
        assertEquals(2, rows.size)
        // observeAll() orders by id DESC
        assertEquals("b", rows.first().label)
    }

    @Test
    fun deleteAllEmptiesTheTable() = runTest {
        dao.insert(DataModel(label = "temp"))
        dao.deleteAll()
        assertEquals(emptyList<DataModel>(), dao.getAll())
    }

    @Test
    fun getByIdReturnsNullForMissingRow() = runTest {
        assertNull(dao.getById(999_999L))
    }
}
