package com.example

import com.example.agent.UtilityDecisionAgent
import com.example.agent.evaluation.AgentEvaluationEngine
import com.example.data.model.AgentConfig
import com.example.data.model.FoodItem
import com.example.data.model.PerishabilityLevel
import com.example.data.model.PriorityLevel
import com.example.data.model.RecommendedAction
import com.example.data.network.IngredientTranslator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityDecisionAgentTest {

    private val agent = UtilityDecisionAgent()
    private val now = 1700000000000L
    private val oneDay = 86400000L

    @Test
    fun testUtilityMonotonicity_soonerExpirationYieldsGreaterOrEqualUrgency() {
        val config = AgentConfig()
        val itemSooner = FoodItem(
            name = "Leche 1",
            quantity = 1.0,
            unit = "l",
            expirationDate = now + (2 * oneDay),
            perishability = PerishabilityLevel.ALTA
        )
        val itemLater = FoodItem(
            name = "Leche 2",
            quantity = 1.0,
            unit = "l",
            expirationDate = now + (5 * oneDay),
            perishability = PerishabilityLevel.ALTA
        )

        val resSooner = agent.calculateItemPriority(itemSooner, config, now)
        val resLater = agent.calculateItemPriority(itemLater, config, now)

        assertTrue(
            "Un alimento que vence antes debe tener mayor o igual urgencia que uno que vence después",
            resSooner.urgencyScore > resLater.urgencyScore
        )
    }

    @Test
    fun testExpiredFoodGetsAlertAction() {
        val config = AgentConfig()
        val item = FoodItem(
            name = "Pescado",
            quantity = 500.0,
            unit = "g",
            expirationDate = now - oneDay, // Vencido ayer
            perishability = PerishabilityLevel.MUY_ALTA
        )

        val res = agent.calculateItemPriority(item, config, now)

        assertTrue(res.isExpired)
        assertEquals(RecommendedAction.ALERTA_DESCARTE, res.recommendedAction)
        assertEquals(PriorityLevel.ALTA, res.priorityLevel)
    }

    @Test
    fun testUnknownExpirationDate_explicitUncertaintyHandled() {
        val config = AgentConfig()
        val item = FoodItem(
            name = "Especias",
            quantity = 100.0,
            unit = "g",
            expirationDate = null,
            perishability = PerishabilityLevel.BAJA
        )

        val res = agent.calculateItemPriority(item, config, now)

        assertTrue(res.hasUnknownExpiration)
        assertEquals(0.40, res.pExpiration, 0.001)
        assertTrue(res.explanation.contains("INCERTIDUMBRE"))
    }

    @Test
    fun testTieBreaker_sameScoreResolvedDeterministically() {
        val config = AgentConfig()
        val itemA = FoodItem(id = 10, name = "A", quantity = 1.0, unit = "kg", expirationDate = now + (3 * oneDay), perishability = PerishabilityLevel.MEDIA)
        val itemB = FoodItem(id = 20, name = "B", quantity = 1.0, unit = "kg", expirationDate = now + (3 * oneDay), perishability = PerishabilityLevel.MEDIA)

        val ranking = agent.evaluateInventory(listOf(itemB, itemA), config, now)

        assertEquals(2, ranking.size)
        assertEquals(10L, ranking[0].foodItem.id)
        assertEquals(20L, ranking[1].foodItem.id)
    }

    @Test
    fun testBilingualIngredientTranslator() {
        assertEquals("chicken", IngredientTranslator.translateToEnglish("Pechuga de Pollo Fresca"))
        assertEquals("tomato", IngredientTranslator.translateToEnglish("Tomates Maduros"))
        assertEquals("rice", IngredientTranslator.translateToEnglish("Arroz Blanco"))
        assertEquals("milk", IngredientTranslator.translateToEnglish("Leche entera"))

        assertTrue(IngredientTranslator.matchesInventory("Chicken Breast", "Pechuga de Pollo", "Chicken"))
        assertFalse(IngredientTranslator.matchesInventory("Garlic", "Arroz", "Rice"))
    }

    @Test
    fun testAgentEvaluationEngine_15CasesPassWith100Percent() {
        val report = AgentEvaluationEngine.runAllTests()

        assertEquals(15, report.totalTests)
        assertEquals(15, report.passedTests)
        assertEquals(0, report.failedTests)
        assertEquals(100.0, report.accuracyPercentage, 0.001)
    }
}
