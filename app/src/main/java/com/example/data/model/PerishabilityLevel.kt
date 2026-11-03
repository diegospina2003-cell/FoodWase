package com.example.data.model

/**
 * Categorías de perecibilidad y su valor cuantitativo normalizado [0.0 - 1.0].
 * Representa la susceptibilidad intrínseca del alimento al deterioro físico y biológico.
 */
enum class PerishabilityLevel(val score: Double, val displayName: String, val description: String) {
    MUY_ALTA(1.00, "Muy Alta", "Pescados frescos, mariscos, carnes crudas picadas, lácteos abiertos"),
    ALTA(0.80, "Alta", "Carnes rojas y aves, verduras de hoja, frutas blandas/bayas, yogur"),
    MEDIA(0.50, "Media", "Frutas de pulpa firme, huevos, quesos curados, tubérculos"),
    BAJA(0.20, "Baja", "Panadería envasada, mermeladas, condimentos, mantequilla"),
    MUY_BAJA(0.05, "Muy Baja", "Granos secos, arroz, pastas, legumbres, conservas enlatadas");

    companion object {
        fun fromString(value: String): PerishabilityLevel {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIA
        }
    }
}

enum class FoodLocation(val displayName: String) {
    NEVERA("Nevera / Refrigerador"),
    CONGELADOR("Congelador"),
    DESPENSA("Despensa / Alacena");

    companion object {
        fun fromString(value: String): FoodLocation {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NEVERA
        }
    }
}
