package com.example.data.model

enum class PriorityLevel(val displayName: String) {
    ALTA("Alta Prioridad"),
    MEDIA("Media Prioridad"),
    BAJA("Baja Prioridad")
}

enum class RecommendedAction(val title: String, val description: String) {
    ALERTA_DESCARTE(
        "Alerta: Alimento Vencido",
        "El producto ha sobrepasado su fecha de caducidad. Se recomienda inspeccionar su estado de salubridad y descartar o compostar para evitar riesgos alimentarios."
    ),
    PROPONER_RECETA(
        "Proponer Receta de Aprovechamiento",
        "El producto requiere consumo inminente. El agente recomienda preparar una receta que lo aproveche junto a otros ingredientes disponibles."
    ),
    SUGERIR_USO_PROXIMO(
        "Sugerir Consumo Próximo",
        "El producto tiene prioridad moderada o alta pero es un insumo básico o no hay recetas completas. Conviene incluirlo en el menú de los próximos 2 días."
    ),
    MONITOREO_ESTABLE(
        "Monitoreo Preventivo",
        "El alimento se encuentra en condiciones seguras y con margen adecuado. No requiere acción urgente en este momento."
    )
}

/**
 * Resultado detallado del cálculo de utilidad del agente para un alimento específico.
 * Incluye desglose matemático completo para máxima transparencia y reproducibilidad académica.
 */
data class AgentPriorityResult(
    val foodItem: FoodItem,
    val urgencyScore: Double, // [0.0 - 1.0]
    val daysUntilExpiration: Long?,
    val isExpired: Boolean,
    val hasUnknownExpiration: Boolean,
    val pExpiration: Double,
    val pPerishability: Double,
    val pQuantity: Double,
    val priorityLevel: PriorityLevel,
    val recommendedAction: RecommendedAction,
    val explanation: String
)
