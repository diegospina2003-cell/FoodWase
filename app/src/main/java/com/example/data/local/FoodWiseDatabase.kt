package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        FoodItemEntity::class,
        InventoryMovementEntity::class,
        AgentConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FoodWiseDatabase : RoomDatabase() {

    abstract fun foodWiseDao(): FoodWiseDao

    companion object {
        @Volatile
        private var INSTANCE: FoodWiseDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): FoodWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodWiseDatabase::class.java,
                    "foodwise_database.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateInitialData(database.foodWiseDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: FoodWiseDao) {
            val sampleItems = InitialDataSeeder.getSampleItems()
            dao.insertItems(sampleItems)
            val movements = InitialDataSeeder.getInitialMovements(sampleItems)
            movements.forEach { dao.insertMovement(it) }
            dao.saveAgentConfig(AgentConfigEntity())
        }
    }
}
