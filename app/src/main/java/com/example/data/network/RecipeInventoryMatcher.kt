package com.example.data.network

import com.example.data.model.FoodItem

data class RecipeMatchResult(
    val mealId: String,
    val mealName: String,
    val mealThumb: String?,
    val category: String?,
    val area: String?,
    val instructions: String?,
    val sourceUrl: String?,
    val youtubeUrl: String?,
    val priorityIngredientUsed: String,
    val requiredIngredients: List<Pair<String, String>>,
    val availableIngredients: List<Pair<String, String>>,
    val missingIngredients: List<Pair<String, String>>,
    val compatibilityPercentage: Int,
    val canCookCompletely: Boolean,
    val otherExpiringItemsUtilized: List<String>
)

object RecipeInventoryMatcher {

    /**
     * Compara los ingredientes requeridos de una receta de TheMealDB con el inventario actual.
     * Clasifica cada ingrediente en disponible o faltante y calcula el porcentaje de compatibilidad.
     */
    fun matchRecipe(
        meal: MealDetailDto,
        inventory: List<FoodItem>,
        priorityIngredient: String
    ): RecipeMatchResult {
        val extracted = meal.extractIngredients()
        val available = mutableListOf<Pair<String, String>>()
        val missing = mutableListOf<Pair<String, String>>()
        val otherExpiring = mutableListOf<String>()

        val activeInventory = inventory.filter { !it.isArchived && it.quantity > 0 }

        for (item in extracted) {
            val (ingName, measure) = item
            val matchedFood = activeInventory.firstOrNull { food ->
                IngredientTranslator.matchesInventory(ingName, food.name, food.nameEn)
            }

            if (matchedFood != null) {
                available.add(item)
                // Si aprovecha otro alimento que no es el prioritario y vence en <= 5 días
                val days = matchedFood.daysUntilExpiration()
                if (matchedFood.name != priorityIngredient && days != null && days in 0..5) {
                    otherExpiring.add("${matchedFood.name} (vence en $days d)")
                }
            } else {
                missing.add(item)
            }
        }

        val total = extracted.size
        val compatibility = if (total > 0) {
            ((available.size.toDouble() / total.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        return RecipeMatchResult(
            mealId = meal.idMeal,
            mealName = meal.strMeal,
            mealThumb = meal.strMealThumb,
            category = meal.strCategory,
            area = meal.strArea,
            instructions = meal.strInstructions,
            sourceUrl = meal.strSource,
            youtubeUrl = meal.strYoutube,
            priorityIngredientUsed = priorityIngredient,
            requiredIngredients = extracted,
            availableIngredients = available,
            missingIngredients = missing,
            compatibilityPercentage = compatibility,
            canCookCompletely = missing.isEmpty(),
            otherExpiringItemsUtilized = otherExpiring.distinct()
        )
    }
}
