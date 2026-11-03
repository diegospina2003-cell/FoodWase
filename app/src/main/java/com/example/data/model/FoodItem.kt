package com.example.data.model

/**
 * Modelo de dominio para un alimento en el inventario de FoodWIse.
 * Incluye campos para seguimiento de fechas, cantidad, perecibilidad y ubicación.
 */
data class FoodItem(
    val id: Long = 0,
    val name: String,
    val nameEn: String? = null,
    val quantity: Double,
    val unit: String, // kg, g, l, ml, unidades, porciones
    val expirationDate: Long?, // Epoch millis, nulo si fecha es desconocida
    val acquisitionDate: Long = System.currentTimeMillis(),
    val category: String = "General",
    val location: FoodLocation = FoodLocation.NEVERA,
    val perishability: PerishabilityLevel = PerishabilityLevel.MEDIA,
    val notes: String = "",
    val isArchived: Boolean = false
) {
    /**
     * Calcula los días restantes hasta el vencimiento en base a un timestamp de referencia.
     * Devuelve null si la fecha de vencimiento es desconocida.
     */
    fun daysUntilExpiration(referenceTimeMillis: Long = System.currentTimeMillis()): Long? {
        val exp = expirationDate ?: return null
        val diffMillis = exp - referenceTimeMillis
        val millisPerDay = 1000L * 60L * 60L * 24L
        return if (diffMillis >= 0) {
            diffMillis / millisPerDay
        } else {
            // Número negativo de días transcurridos desde que venció
            -((-diffMillis + millisPerDay - 1) / millisPerDay)
        }
    }
}
