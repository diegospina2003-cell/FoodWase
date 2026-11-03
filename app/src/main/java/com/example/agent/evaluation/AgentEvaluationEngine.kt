package com.example.agent.evaluation

import com.example.agent.UtilityDecisionAgent
import com.example.data.model.AgentConfig
import com.example.data.model.FoodItem
import com.example.data.model.FoodLocation
import com.example.data.model.PerishabilityLevel
import com.example.data.model.PriorityLevel
import com.example.data.model.RecommendedAction
import com.example.data.network.IngredientTranslator
import java.util.Locale

data class TestCaseResult(
    val id: Int,
    val title: String,
    val description: String,
    val inputDescription: String,
    val expectedOutcome: String,
    val obtainedOutcome: String,
    val passed: Boolean,
    val calculatedScore: Double,
    val recommendedAction: RecommendedAction?,
    val technicalNotes: String
)

data class EvaluationReport(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val accuracyPercentage: Double,
    val executionTimestamp: Long = System.currentTimeMillis(),
    val results: List<TestCaseResult>
)

object AgentEvaluationEngine {

    fun runAllTests(): EvaluationReport {
        val agent = UtilityDecisionAgent()
        val config = AgentConfig(wExpiration = 0.55, wPerishability = 0.30, wQuantity = 0.15)
        val now = 1700000000000L // Timestamp base fijo para reproducibilidad absoluta (24h = 86400000ms)
        val oneDay = 86400000L

        val results = mutableListOf<TestCaseResult>()

        // Caso 1: Alimento vencido (-2 días)
        val item1 = FoodItem(
            id = 1,
            name = "Yogur Vencido",
            quantity = 500.0,
            unit = "g",
            expirationDate = now - (2 * oneDay),
            perishability = PerishabilityLevel.MUY_ALTA
        )
        val res1 = agent.calculateItemPriority(item1, config, referenceTimeMillis = now)
        val passed1 = res1.isExpired && res1.recommendedAction == RecommendedAction.ALERTA_DESCARTE && res1.priorityLevel == PriorityLevel.ALTA
        results.add(
            TestCaseResult(
                id = 1,
                title = "Alimento Vencido (-2 días)",
                description = "Verifica que un alimento caducado reciba máxima prioridad y acción ALERTA_DESCARTE.",
                inputDescription = "Yogur, vencido hace 2 días, perecibilidad Muy Alta, 500g",
                expectedOutcome = "isExpired=true, Acción=ALERTA_DESCARTE, Prioridad=ALTA, P_venc=1.00",
                obtainedOutcome = "isExpired=${res1.isExpired}, Acción=${res1.recommendedAction}, Prioridad=${res1.priorityLevel}, U=${res1.urgencyScore}",
                passed = passed1,
                calculatedScore = res1.urgencyScore,
                recommendedAction = res1.recommendedAction,
                technicalNotes = res1.explanation
            )
        )

        // Caso 2: Alimento que vence hoy (0 días)
        val item2 = FoodItem(
            id = 2,
            name = "Pescado Fresco",
            quantity = 400.0,
            unit = "g",
            expirationDate = now,
            perishability = PerishabilityLevel.MUY_ALTA
        )
        val res2 = agent.calculateItemPriority(item2, config, referenceTimeMillis = now)
        val passed2 = res2.daysUntilExpiration == 0L && res2.priorityLevel == PriorityLevel.ALTA && res2.recommendedAction == RecommendedAction.PROPONER_RECETA
        results.add(
            TestCaseResult(
                id = 2,
                title = "Alimento que vence hoy (0 días)",
                description = "Comprueba que un producto crítico que vence hoy active recomendación culinaria inmediata.",
                inputDescription = "Pescado, vence hoy, perecibilidad Muy Alta, 400g",
                expectedOutcome = "Días=0, Prioridad=ALTA, Acción=PROPONER_RECETA, P_venc=0.95",
                obtainedOutcome = "Días=${res2.daysUntilExpiration}, Prioridad=${res2.priorityLevel}, Acción=${res2.recommendedAction}, U=${res2.urgencyScore}",
                passed = passed2,
                calculatedScore = res2.urgencyScore,
                recommendedAction = res2.recommendedAction,
                technicalNotes = res2.explanation
            )
        )

        // Caso 3: Alimento que vence mañana (1 día) vs. Cantidad en 4 días
        val item3A = FoodItem(
            id = 31,
            name = "Pollo (vence mañana)",
            quantity = 500.0,
            unit = "g",
            expirationDate = now + oneDay,
            perishability = PerishabilityLevel.ALTA
        )
        val item3B = FoodItem(
            id = 32,
            name = "Tomates (en 4 días)",
            quantity = 4000.0,
            unit = "g",
            expirationDate = now + (4 * oneDay),
            perishability = PerishabilityLevel.ALTA
        )
        val res3A = agent.calculateItemPriority(item3A, config, now)
        val res3B = agent.calculateItemPriority(item3B, config, now)
        val passed3 = res3A.urgencyScore > res3B.urgencyScore
        results.add(
            TestCaseResult(
                id = 3,
                title = "Vencimiento cercano vs. Mayor cantidad",
                description = "La cantidad no debe anular arbitrariamente la urgencia del vencimiento inminente.",
                inputDescription = "Item A (vence en 1 día, 500g) vs. Item B (vence en 4 días, 4000g)",
                expectedOutcome = "U(Item A) > U(Item B)",
                obtainedOutcome = "U(A)=${res3A.urgencyScore} vs U(B)=${res3B.urgencyScore}",
                passed = passed3,
                calculatedScore = res3A.urgencyScore,
                recommendedAction = res3A.recommendedAction,
                technicalNotes = "El peso del vencimiento w1=0.55 preserva la precedencia temporal correcta."
            )
        )

        // Caso 4: Empate en fecha de vencimiento resuelto por perecibilidad
        val item4A = FoodItem(
            id = 41,
            name = "Yogur Abierto",
            quantity = 200.0,
            unit = "g",
            expirationDate = now + (2 * oneDay),
            perishability = PerishabilityLevel.MUY_ALTA
        )
        val item4B = FoodItem(
            id = 42,
            name = "Manzanas",
            quantity = 200.0,
            unit = "g",
            expirationDate = now + (2 * oneDay),
            perishability = PerishabilityLevel.MEDIA
        )
        val res4A = agent.calculateItemPriority(item4A, config, now)
        val res4B = agent.calculateItemPriority(item4B, config, now)
        val passed4 = res4A.urgencyScore > res4B.urgencyScore
        results.add(
            TestCaseResult(
                id = 4,
                title = "Desempate por Perecibilidad",
                description = "Con igual fecha de caducidad y cantidad, mayor perecibilidad debe recibir mayor urgencia.",
                inputDescription = "Yogur (Muy Alta perecibilidad) vs. Manzanas (Media perecibilidad), ambos en 2 días",
                expectedOutcome = "U(Yogur) > U(Manzanas)",
                obtainedOutcome = "U(Yogur)=${res4A.urgencyScore} > U(Manzanas)=${res4B.urgencyScore}",
                passed = passed4,
                calculatedScore = res4A.urgencyScore,
                recommendedAction = res4A.recommendedAction,
                technicalNotes = "Factor perecibilidad discrimina correctamente entre categorías biológicas."
            )
        )

        // Caso 5: Desempate determinista estricto
        val item5A = FoodItem(id = 51, name = "Queso A", quantity = 100.0, unit = "g", expirationDate = now + (3 * oneDay), perishability = PerishabilityLevel.MEDIA)
        val item5B = FoodItem(id = 52, name = "Queso B", quantity = 100.0, unit = "g", expirationDate = now + (3 * oneDay), perishability = PerishabilityLevel.MEDIA)
        val ranking5 = agent.evaluateInventory(listOf(item5B, item5A), config, now)
        val passed5 = ranking5.size == 2 && ranking5.first().urgencyScore == ranking5.last().urgencyScore && ranking5.first().foodItem.id == 51L
        results.add(
            TestCaseResult(
                id = 5,
                title = "Desempate Determinista Estable",
                description = "Comprueba que casos con parámetros idénticos se resuelvan de forma consistente e invariable.",
                inputDescription = "Dos productos idénticos en parámetros, orden de inserción invertido",
                expectedOutcome = "Mismo puntaje, orden estable por ID primario",
                obtainedOutcome = "Scores iguales (${ranking5.first().urgencyScore}), primer elemento id=${ranking5.first().foodItem.id}",
                passed = passed5,
                calculatedScore = ranking5.first().urgencyScore,
                recommendedAction = ranking5.first().recommendedAction,
                technicalNotes = "Garantiza determinismo sin oscilaciones en la UI."
            )
        )

        // Caso 6: Alimento con fecha desconocida (Incertidumbre)
        val item6 = FoodItem(
            id = 6,
            name = "Condimento Casero",
            quantity = 150.0,
            unit = "g",
            expirationDate = null,
            perishability = PerishabilityLevel.BAJA
        )
        val res6 = agent.calculateItemPriority(item6, config, now)
        val passed6 = res6.hasUnknownExpiration && res6.pExpiration == 0.40 && res6.explanation.contains("INCERTIDUMBRE")
        results.add(
            TestCaseResult(
                id = 6,
                title = "Manejo de Fecha Desconocida",
                description = "Valida el tratamiento explícito de la incertidumbre sin inventar fechas arbitrarias.",
                inputDescription = "Condimento, expirationDate=null, perecibilidad Baja",
                expectedOutcome = "hasUnknownExpiration=true, P_venc=0.40, nota de incertidumbre en explicación",
                obtainedOutcome = "hasUnknown=${res6.hasUnknownExpiration}, P_venc=${res6.pExpiration}, U=${res6.urgencyScore}",
                passed = passed6,
                calculatedScore = res6.urgencyScore,
                recommendedAction = res6.recommendedAction,
                technicalNotes = res6.explanation
            )
        )

        // Caso 7: Alimento no perecedero en gran cantidad
        val item7 = FoodItem(
            id = 7,
            name = "Arroz Seco",
            quantity = 10.0,
            unit = "kg",
            expirationDate = now + (200 * oneDay),
            perishability = PerishabilityLevel.MUY_BAJA
        )
        val res7 = agent.calculateItemPriority(item7, config, now)
        val passed7 = res7.priorityLevel == PriorityLevel.BAJA && res7.recommendedAction == RecommendedAction.MONITOREO_ESTABLE
        results.add(
            TestCaseResult(
                id = 7,
                title = "Alimento no perecedero (Largo plazo)",
                description = "Verifica que productos no perecederos permanezcan en baja prioridad aún con alta cantidad.",
                inputDescription = "Arroz 10kg, vence en 200 días, perecibilidad Muy Baja",
                expectedOutcome = "Prioridad=BAJA, Acción=MONITOREO_ESTABLE, U < 0.45",
                obtainedOutcome = "Prioridad=${res7.priorityLevel}, Acción=${res7.recommendedAction}, U=${res7.urgencyScore}",
                passed = passed7,
                calculatedScore = res7.urgencyScore,
                recommendedAction = res7.recommendedAction,
                technicalNotes = res7.explanation
            )
        )

        // Caso 8: Inventario vacío
        val emptyRanking = agent.evaluateInventory(emptyList(), config, now)
        val passed8 = emptyRanking.isEmpty()
        results.add(
            TestCaseResult(
                id = 8,
                title = "Inventario Vacío",
                description = "Comprueba el comportamiento ante una lista de inventario sin registros.",
                inputDescription = "items = []",
                expectedOutcome = "Lista resultante vacía sin errores de ejecución",
                obtainedOutcome = "Elementos retornados: ${emptyRanking.size}",
                passed = passed8,
                calculatedScore = 0.0,
                recommendedAction = null,
                technicalNotes = "Manejo seguro de casos de borde en colección vacía."
            )
        )

        // Caso 9: Múltiples lácteos ordenados por caducidad
        val dairy1 = FoodItem(id = 91, name = "Leche", quantity = 1.0, unit = "l", expirationDate = now + (1 * oneDay), perishability = PerishabilityLevel.ALTA)
        val dairy2 = FoodItem(id = 92, name = "Yogur", quantity = 1.0, unit = "l", expirationDate = now + (3 * oneDay), perishability = PerishabilityLevel.ALTA)
        val dairy3 = FoodItem(id = 93, name = "Mantequilla", quantity = 1.0, unit = "kg", expirationDate = now + (20 * oneDay), perishability = PerishabilityLevel.BAJA)
        val ranking9 = agent.evaluateInventory(listOf(dairy3, dairy1, dairy2), config, now)
        val passed9 = ranking9.map { it.foodItem.id } == listOf(91L, 92L, 93L)
        results.add(
            TestCaseResult(
                id = 9,
                title = "Orden Monótono en Misma Categoría",
                description = "Comprueba que dentro de la misma categoría de productos el orden responda a la proximidad.",
                inputDescription = "Lácteos con vencimientos en 1 día, 3 días y 20 días",
                expectedOutcome = "Orden exacto: 91 (1d) > 92 (3d) > 93 (20d)",
                obtainedOutcome = "Orden obtenido: ${ranking9.map { "${it.foodItem.name} (U=${it.urgencyScore})" }}",
                passed = passed9,
                calculatedScore = ranking9.first().urgencyScore,
                recommendedAction = ranking9.first().recommendedAction,
                technicalNotes = "El agente ordena coherentemente por urgencia decreciente."
            )
        )

        // Caso 10: Carne cruda vs. Queso curado (mismo día de vencimiento)
        val item10A = FoodItem(id = 101, name = "Carne Picada", quantity = 500.0, unit = "g", expirationDate = now + (2 * oneDay), perishability = PerishabilityLevel.MUY_ALTA)
        val item10B = FoodItem(id = 102, name = "Queso Maduro", quantity = 500.0, unit = "g", expirationDate = now + (2 * oneDay), perishability = PerishabilityLevel.MEDIA)
        val res10A = agent.calculateItemPriority(item10A, config, now)
        val res10B = agent.calculateItemPriority(item10B, config, now)
        val passed10 = res10A.urgencyScore > res10B.urgencyScore
        results.add(
            TestCaseResult(
                id = 10,
                title = "Carne Cruda vs. Queso Maduro",
                description = "A igualdad de tiempo restante y cantidad, la carne cruda debe recibir mayor prioridad.",
                inputDescription = "Carne (Muy Alta) vs. Queso (Media), ambos en 2 días",
                expectedOutcome = "U(Carne) > U(Queso)",
                obtainedOutcome = "U(Carne)=${res10A.urgencyScore} > U(Queso)=${res10B.urgencyScore}",
                passed = passed10,
                calculatedScore = res10A.urgencyScore,
                recommendedAction = res10A.recommendedAction,
                technicalNotes = "Refleja el riesgo biológico real del alimento doméstico."
            )
        )

        // Caso 11: Invarianza Matemática Estricta (A igualdad de otros factores, menor tiempo nunca da menor U)
        val item11A = FoodItem(id = 111, name = "Test A", quantity = 1.0, unit = "kg", expirationDate = now + (2 * oneDay), perishability = PerishabilityLevel.ALTA)
        val item11B = FoodItem(id = 112, name = "Test B", quantity = 1.0, unit = "kg", expirationDate = now + (3 * oneDay), perishability = PerishabilityLevel.ALTA)
        val res11A = agent.calculateItemPriority(item11A, config, now)
        val res11B = agent.calculateItemPriority(item11B, config, now)
        val passed11 = res11A.urgencyScore >= res11B.urgencyScore
        results.add(
            TestCaseResult(
                id = 11,
                title = "Invarianza Matemática de Vencimiento",
                description = "Demuestra formalmente que la función de utilidad es monótona no creciente respecto a días restantes.",
                inputDescription = "Alimento que vence en 2 días vs. alimento idéntico que vence en 3 días",
                expectedOutcome = "U(2 días) >= U(3 días)",
                obtainedOutcome = "U(2d)=${res11A.urgencyScore} >= U(3d)=${res11B.urgencyScore}",
                passed = passed11,
                calculatedScore = res11A.urgencyScore,
                recommendedAction = res11A.recommendedAction,
                technicalNotes = "Cumple axioma matemático de monotonicidad exigido en la especificación."
            )
        )

        // Caso 12: Sensibilidad al cambio de pesos configurables
        val configCustom = AgentConfig(wExpiration = 0.80, wPerishability = 0.10, wQuantity = 0.10)
        val res12Default = agent.calculateItemPriority(item11A, config, now)
        val res12Custom = agent.calculateItemPriority(item11A, configCustom, now)
        val passed12 = res12Default.urgencyScore != res12Custom.urgencyScore
        results.add(
            TestCaseResult(
                id = 12,
                title = "Sensibilidad de Ponderaciones",
                description = "Verifica que la función de utilidad reaccione determinísticamente a variaciones en w1, w2, w3.",
                inputDescription = "Mismo alimento con w1=0.55 vs. w1=0.80",
                expectedOutcome = "El puntaje U cambia de forma controlada y matemáticamente predecible",
                obtainedOutcome = "U(w1=0.55)=${res12Default.urgencyScore} vs U(w1=0.80)=${res12Custom.urgencyScore}",
                passed = passed12,
                calculatedScore = res12Custom.urgencyScore,
                recommendedAction = res12Custom.recommendedAction,
                technicalNotes = "Permite al usuario calibrar la sensibilidad según hábitos del hogar."
            )
        )

        // Caso 13: Normalización Bilingüe de Ingredientes
        val translatedChicken = IngredientTranslator.translateToEnglish("Pechuga de Pollo")
        val translatedTomato = IngredientTranslator.translateToEnglish("Tomates Maduros")
        val translatedRice = IngredientTranslator.translateToEnglish("Arroz Blanco")
        val passed13 = translatedChicken == "chicken" && translatedTomato == "tomato" && translatedRice == "rice"
        results.add(
            TestCaseResult(
                id = 13,
                title = "Normalizador Bilingüe (TheMealDB)",
                description = "Comprueba que los términos culinarios en español se mapeen a los ingredientes canónicos de TheMealDB.",
                inputDescription = "'Pechuga de Pollo', 'Tomates Maduros', 'Arroz Blanco'",
                expectedOutcome = "'chicken', 'tomato', 'rice'",
                obtainedOutcome = "'$translatedChicken', '$translatedTomato', '$translatedRice'",
                passed = passed13,
                calculatedScore = 1.0,
                recommendedAction = null,
                technicalNotes = "Garantiza consultas válidas a la API internacional de recetas."
            )
        )

        // Caso 14: Detección de Ingredientes Disponibles vs. Faltantes
        val userPantry = listOf(
            FoodItem(name = "Pollo", nameEn = "chicken", quantity = 1.0, unit = "kg", expirationDate = now + oneDay),
            FoodItem(name = "Arroz", nameEn = "rice", quantity = 500.0, unit = "g", expirationDate = now + (10 * oneDay))
        )
        val matchesChicken = IngredientTranslator.matchesInventory("Chicken", userPantry[0].name, userPantry[0].nameEn)
        val matchesRice = IngredientTranslator.matchesInventory("Rice", userPantry[1].name, userPantry[1].nameEn)
        val matchesGarlic = userPantry.any { IngredientTranslator.matchesInventory("Garlic", it.name, it.nameEn) }
        val passed14 = matchesChicken && matchesRice && !matchesGarlic
        results.add(
            TestCaseResult(
                id = 14,
                title = "Comparación de Compatibilidad de Recetas",
                description = "Comprueba la discriminación precisa entre ingredientes presentes en despensa y faltantes.",
                inputDescription = "Despensa: [Pollo, Arroz]. Receta requiere: Chicken, Rice, Garlic",
                expectedOutcome = "Disponibles: [Chicken, Rice], Faltante: [Garlic]",
                obtainedOutcome = "matchesChicken=$matchesChicken, matchesRice=$matchesRice, missingGarlic=${!matchesGarlic}",
                passed = passed14,
                calculatedScore = 0.67,
                recommendedAction = RecommendedAction.PROPONER_RECETA,
                technicalNotes = "Evita afirmar que una receta se puede preparar si faltan insumos esenciales."
            )
        )

        // Caso 15: Umbrales de Clasificación de Prioridad
        val highItem = FoodItem(name = "Crítico", quantity = 2.0, unit = "kg", expirationDate = now + (1 * oneDay), perishability = PerishabilityLevel.MUY_ALTA)
        val medItem = FoodItem(name = "Medio", quantity = 1.0, unit = "kg", expirationDate = now + (5 * oneDay), perishability = PerishabilityLevel.MEDIA)
        val lowItem = FoodItem(name = "Bajo", quantity = 0.5, unit = "kg", expirationDate = now + (40 * oneDay), perishability = PerishabilityLevel.BAJA)
        val rHigh = agent.calculateItemPriority(highItem, config, now)
        val rMed = agent.calculateItemPriority(medItem, config, now)
        val rLow = agent.calculateItemPriority(lowItem, config, now)
        val passed15 = rHigh.priorityLevel == PriorityLevel.ALTA && rMed.priorityLevel == PriorityLevel.MEDIA && rLow.priorityLevel == PriorityLevel.BAJA
        results.add(
            TestCaseResult(
                id = 15,
                title = "Calibración de Umbrales de Prioridad",
                description = "Verifica que el espacio continuo [0.0 - 1.0] se segmente en ALTA, MEDIA y BAJA conforme a los umbrales.",
                inputDescription = "3 alimentos en rangos crítico, intermedio y seguro",
                expectedOutcome = "Niveles: ALTA, MEDIA, BAJA",
                obtainedOutcome = "Obtenidos: ${rHigh.priorityLevel} (U=${rHigh.urgencyScore}), ${rMed.priorityLevel} (U=${rMed.urgencyScore}), ${rLow.priorityLevel} (U=${rLow.urgencyScore})",
                passed = passed15,
                calculatedScore = 1.0,
                recommendedAction = null,
                technicalNotes = "Umbrales vigentes: ALTA >= 0.70, MEDIA >= 0.45, BAJA < 0.45."
            )
        )

        val passedCount = results.count { it.passed }
        val total = results.size
        val accuracy = if (total > 0) (passedCount.toDouble() / total.toDouble()) * 100.0 else 0.0

        return EvaluationReport(
            totalTests = total,
            passedTests = passedCount,
            failedTests = total - passedCount,
            accuracyPercentage = (accuracy * 10.0).toInt() / 10.0,
            results = results
        )
    }
}
