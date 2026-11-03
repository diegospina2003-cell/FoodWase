package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.InventoryMovement
import com.example.data.model.MovementType

@Entity(tableName = "inventory_movements")
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val foodItemId: Long,
    val foodItemName: String,
    val movementType: String,
    val quantity: Double,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = ""
) {
    fun toDomain(): InventoryMovement = InventoryMovement(
        id = id,
        foodItemId = foodItemId,
        foodItemName = foodItemName,
        movementType = try {
            MovementType.valueOf(movementType)
        } catch (_: Exception) {
            MovementType.CREACION
        },
        quantity = quantity,
        unit = unit,
        timestamp = timestamp,
        reason = reason
    )

    companion object {
        fun fromDomain(movement: InventoryMovement): InventoryMovementEntity = InventoryMovementEntity(
            id = movement.id,
            foodItemId = movement.foodItemId,
            foodItemName = movement.foodItemName,
            movementType = movement.movementType.name,
            quantity = movement.quantity,
            unit = movement.unit,
            timestamp = movement.timestamp,
            reason = movement.reason
        )
    }
}
