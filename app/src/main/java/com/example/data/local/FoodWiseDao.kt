package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodWiseDao {

    // Alimentos
    @Query("SELECT * FROM food_items WHERE isArchived = 0 ORDER BY id DESC")
    fun getAllActiveItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FoodItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<FoodItemEntity>)

    @Update
    suspend fun updateItem(item: FoodItemEntity)

    @Delete
    suspend fun deleteItem(item: FoodItemEntity)

    @Query("UPDATE food_items SET isArchived = 1 WHERE id = :id")
    suspend fun archiveItem(id: Long)

    @Query("DELETE FROM food_items")
    suspend fun clearAllItems()

    // Movimientos de trazabilidad
    @Query("SELECT * FROM inventory_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<InventoryMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryMovementEntity): Long

    @Query("DELETE FROM inventory_movements")
    suspend fun clearAllMovements()

    // Configuración del Agente de Utilidad
    @Query("SELECT * FROM agent_config WHERE id = 1 LIMIT 1")
    fun getAgentConfig(): Flow<AgentConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAgentConfig(config: AgentConfigEntity)
}
