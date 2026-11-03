package com.example.data.model

/**
 * Ponderaciones y umbrales configurables para la función de utilidad del agente inteligente.
 * Los pesos wExpiration + wPerishability + wQuantity deben estar normalizados a 1.0.
 */
data class AgentConfig(
    val id: Long = 1,
    val wExpiration: Double = 0.55,
    val wPerishability: Double = 0.30,
    val wQuantity: Double = 0.15,
    val highPriorityThreshold: Double = 0.70,
    val mediumPriorityThreshold: Double = 0.45
) {
    init {
        val sum = wExpiration + wPerishability + wQuantity
        require(sum > 0.0) { "La suma de los pesos de utilidad debe ser mayor a 0" }
    }

    /**
     * Devuelve una versión donde la suma de pesos suma exactamente 1.0.
     */
    fun normalized(): AgentConfig {
        val total = wExpiration + wPerishability + wQuantity
        if (total == 0.0) return this
        return copy(
            wExpiration = wExpiration / total,
            wPerishability = wPerishability / total,
            wQuantity = wQuantity / total
        )
    }
}
