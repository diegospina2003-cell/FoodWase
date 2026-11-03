package com.example.agent

import com.example.data.model.AgentConfig
import com.example.data.model.AgentPriorityResult
import com.example.data.model.FoodItem
import com.example.data.model.PriorityLevel
import com.example.data.model.RecommendedAction
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * CAPA 1: Decisión del agente inteligente de FoodWIse basada exclusivamente en función de utilidad.
 *
 * Definición central:
 * "El agente decide, en cada momento, qué alimento del hogar requiere atención primero
 * y qué acción recomendar sobre él —alertar o proponer una receta— para evitar que se desperdicie".
 */
class UtilityDecisionAgent(
    private val defaultReferenceTimeProvider: () -> Long = { System.currentTimeMillis() }
) {

    fun evaluateInventory(
        items: List<FoodItem>,
        config: AgentConfig = AgentConfig(),
        referenceTimeMillis: Long = defaultReferenceTimeProvider()
    ): List<AgentPriorityResult> {
        val normalizedConfig = config.normalized()

        return items
            .filter { !it.isArchived }
            .map { item -> calculateItemPriority(item, normalizedConfig, referenceTimeMillis) }
            .sortedWith(
                compareByDescending<AgentPriorityResult> { it.urgencyScore }
                    .thenBy { it.daysUntilExpiration ?: Long.MAX_VALUE }
                    .thenByDescending { it.pPerishability }
                    .thenBy { it.foodItem.id }
            )
    }

    fun calculateItemPriority(
        item: FoodItem,
        config: AgentConfig = AgentConfig().normalized(),
        referenceTimeMillis: Long = defaultReferenceTimeProvider()
    ): AgentPriorityResult {
        val days = item.daysUntilExpiration(referenceTimeMillis)
        val isExpired = days != null && days < 0
        val hasUnknownDate = item.expirationDate == null

        // 1. Proximidad del Vencimiento: P_vencimiento in [0.0, 1.0]
        val pExpiration: Double = when {
            hasUnknownDate -> 0.40
            days == null -> 0.40
            days < 0 -> 1.00
            days == 0L -> 0.95
            days in 1..14 -> 1.00 - (days.toDouble() / 15.0)
            else -> max(0.05, 1.0 / (days.toDouble() + 1.0))
        }

        // 2. Perecibilidad: P_perecibilidad in [0.0, 1.0]
        val pPerishability: Double = item.perishability.score

        // 3. Cantidad en riesgo: P_cantidad in [0.0, 1.0]
        val normalizedQuantity = max(0.0, item.quantity)
        val pQuantity: Double = when {
            normalizedQuantity <= 0.0 -> 0.05
            normalizedQuantity < 1.0 -> 0.30
            normalizedQuantity in 1.0..5.0 -> 0.30 + (normalizedQuantity - 1.0) * (0.40 / 4.0)
            else -> min(1.00, 0.70 + (normalizedQuantity - 5.0) * 0.05)
        }

        // U = w1*P_exp + w2*P_per + w3*P_qty
        val rawScore = (config.wExpiration * pExpiration) +
                (config.wPerishability * pPerishability) +
                (config.wQuantity * pQuantity)

        val urgencyScore = (rawScore * 1000.0).roundToInt() / 1000.0

        val priorityLevel = when {
            urgencyScore >= config.highPriorityThreshold || isExpired -> PriorityLevel.ALTA
            urgencyScore >= config.mediumPriorityThreshold -> PriorityLevel.MEDIA
            else -> PriorityLevel.BAJA
        }

        val recommendedAction = when {
            isExpired -> RecommendedAction.ALERTA_DESCARTE
            priorityLevel == PriorityLevel.ALTA && (days != null && days <= 4) -> RecommendedAction.PROPONER_RECETA
            priorityLevel == PriorityLevel.ALTA || priorityLevel == PriorityLevel.MEDIA -> RecommendedAction.SUGERIR_USO_PROXIMO
            else -> RecommendedAction.MONITOREO_ESTABLE
        }

        val explanation = buildExplanation(
            item = item,
            urgencyScore = urgencyScore,
            isExpired = isExpired,
            hasUnknownDate = hasUnknownDate,
            days = days,
            pExpiration = pExpiration,
            pPerishability = pPerishability,
            pQuantity = pQuantity,
            config = config,
            action = recommendedAction
        )

        return AgentPriorityResult(
            foodItem = item,
            urgencyScore = urgencyScore,
            daysUntilExpiration = days,
            isExpired = isExpired,
            hasUnknownExpiration = hasUnknownDate,
            pExpiration = pExpiration,
            pPerishability = pPerishability,
            pQuantity = pQuantity,
            priorityLevel = priorityLevel,
            recommendedAction = recommendedAction,
            explanation = explanation
        )
    }

    private fun buildExplanation(
        item: FoodItem,
        urgencyScore: Double,
        isExpired: Boolean,
        hasUnknownDate: Boolean,
        days: Long?,
        pExpiration: Double,
        pPerishability: Double,
        pQuantity: Double,
        config: AgentConfig,
        action: RecommendedAction
    ): String {
        return buildString {
            append(String.format(Locale.ROOT, "Puntaje de Urgencia U = %.3f. ", urgencyScore))
            when {
                isExpired -> {
                    val daysAgo = -(days ?: 0)
                    append("ALERTA CRÍTICA: El alimento venció hace $daysAgo día(s). ")
                }
                hasUnknownDate -> {
                    append("NOTA DE INCERTIDUMBRE: Sin fecha de vencimiento registrada (valor base asumido de 0.40). ")
                }
                days == 0L -> {
                    append("ATENCIÓN INMEDIATA: Vence hoy mismo. ")
                }
                days != null -> {
                    append("Tiempo restante: $days día(s). ")
                }
            }
            append("Perecibilidad: ${item.perishability.displayName} (P = ${String.format(Locale.ROOT, "%.2f", pPerishability)}). ")
            append("Cantidad en riesgo: ${item.quantity} ${item.unit}. ")
            append(String.format(
                Locale.ROOT,
                "Desglose: (%.2f×%.2f) + (%.2f×%.2f) + (%.2f×%.2f). ",
                config.wExpiration, pExpiration,
                config.wPerishability, pPerishability,
                config.wQuantity, pQuantity
            ))
            append("Acción: ${action.title}.")
        }
    }
}
