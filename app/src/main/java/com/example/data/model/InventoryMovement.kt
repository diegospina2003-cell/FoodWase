package com.example.data.model

enum class MovementType(val displayName: String) {
    CREACION("Registro inicial"),
    CONSUMO_PARCIAL("Consumo parcial"),
    CONSUMO_TOTAL("Consumo total"),
    DESCARTE("Descarte / Merma")
}

/**
 * Trazabilidad inmutable de todas las acciones sobre el inventario.
 */
data class InventoryMovement(
    val id: Long = 0,
    val foodItemId: Long,
    val foodItemName: String,
    val movementType: MovementType,
    val quantity: Double,
    val unit: String,
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = ""
)
