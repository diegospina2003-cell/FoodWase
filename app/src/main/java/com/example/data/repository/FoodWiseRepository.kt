package com.example.data.repository

import com.example.data.local.AgentConfigEntity
import com.example.data.local.FoodItemEntity
import com.example.data.local.FoodWiseDao
import com.example.data.local.InitialDataSeeder
import com.example.data.local.InventoryMovementEntity
import com.example.data.model.AgentConfig
import com.example.data.model.FoodItem
import com.example.data.model.InventoryMovement
import com.example.data.model.MovementType
import com.example.data.network.IngredientTranslator
import com.example.data.network.MealDetailDto
import com.example.data.network.RecipeInventoryMatcher
import com.example.data.network.RecipeMatchResult
import com.example.data.network.TheMealDbApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoodWiseRepository(
    private val dao: FoodWiseDao,
    private val api: TheMealDbApi
) {

    // --- Inventario Reactivo ---
    val allActiveItems: Flow<List<FoodItem>> = dao.getAllActiveItems().map { list ->
        list.map { it.toDomain() }
    }

    val allMovements: Flow<List<InventoryMovement>> = dao.getAllMovements().map { list ->
        list.map { it.toDomain() }
    }

    val agentConfig: Flow<AgentConfig> = dao.getAgentConfig().map { entity ->
        entity?.toDomain() ?: AgentConfig()
    }

    // --- Operaciones de Inventario ---
    suspend fun registerFoodItem(item: FoodItem): Long {
        val entity = FoodItemEntity.fromDomain(item)
        val id = dao.insertItem(entity)
        dao.insertMovement(
            InventoryMovementEntity(
                foodItemId = id,
                foodItemName = item.name,
                movementType = MovementType.CREACION.name,
                quantity = item.quantity,
                unit = item.unit,
                timestamp = System.currentTimeMillis(),
                reason = "Registro manual en el inventario doméstico"
            )
        )
        return id
    }

    suspend fun updateFoodItem(item: FoodItem) {
        dao.updateItem(FoodItemEntity.fromDomain(item))
    }

    suspend fun deleteFoodItem(item: FoodItem, reason: String = "Eliminado por el usuario") {
        dao.deleteItem(FoodItemEntity.fromDomain(item))
        dao.insertMovement(
            InventoryMovementEntity(
                foodItemId = item.id,
                foodItemName = item.name,
                movementType = MovementType.DESCARTE.name,
                quantity = item.quantity,
                unit = item.unit,
                timestamp = System.currentTimeMillis(),
                reason = reason
            )
        )
    }

    suspend fun consumePartial(item: FoodItem, consumedQty: Double, note: String = "") {
        val remaining = item.quantity - consumedQty
        if (remaining <= 0.0) {
            consumeTotal(item, note)
        } else {
            val updated = item.copy(quantity = remaining)
            dao.updateItem(FoodItemEntity.fromDomain(updated))
            dao.insertMovement(
                InventoryMovementEntity(
                    foodItemId = item.id,
                    foodItemName = item.name,
                    movementType = MovementType.CONSUMO_PARCIAL.name,
                    quantity = consumedQty,
                    unit = item.unit,
                    timestamp = System.currentTimeMillis(),
                    reason = if (note.isNotBlank()) note else "Consumo parcial doméstico"
                )
            )
        }
    }

    suspend fun consumeTotal(item: FoodItem, note: String = "") {
        dao.archiveItem(item.id)
        dao.insertMovement(
            InventoryMovementEntity(
                foodItemId = item.id,
                foodItemName = item.name,
                movementType = MovementType.CONSUMO_TOTAL.name,
                quantity = item.quantity,
                unit = item.unit,
                timestamp = System.currentTimeMillis(),
                reason = if (note.isNotBlank()) note else "Consumo total del alimento"
            )
        )
    }

    suspend fun discardFood(item: FoodItem, reason: String) {
        dao.archiveItem(item.id)
        dao.insertMovement(
            InventoryMovementEntity(
                foodItemId = item.id,
                foodItemName = item.name,
                movementType = MovementType.DESCARTE.name,
                quantity = item.quantity,
                unit = item.unit,
                timestamp = System.currentTimeMillis(),
                reason = reason.ifBlank { "Descarte preventivo por vencimiento o deterioro" }
            )
        )
    }

    suspend fun saveAgentConfig(config: AgentConfig) {
        dao.saveAgentConfig(AgentConfigEntity.fromDomain(config))
    }

    suspend fun resetDemoPantry() {
        dao.clearAllItems()
        dao.clearAllMovements()
        val sampleItems = InitialDataSeeder.getSampleItems()
        dao.insertItems(sampleItems)
        val sampleMovements = InitialDataSeeder.getInitialMovements(sampleItems)
        sampleMovements.forEach { dao.insertMovement(it) }
        dao.saveAgentConfig(AgentConfigEntity())
    }

    // --- CAPA 2: Consulta TheMealDB y Comparación de Recetas ---
    suspend fun fetchRecipesForIngredient(
        foodItem: FoodItem,
        currentInventory: List<FoodItem>,
        limit: Int = 8
    ): Result<List<RecipeMatchResult>> {
        return runCatching {
            val queryIngredient = foodItem.nameEn?.ifBlank { null }
                ?: IngredientTranslator.translateToEnglish(foodItem.name)

            val filterResponse = api.searchMealsByIngredient(queryIngredient)
            val mealSummaries = filterResponse.meals.orEmpty().take(limit)

            if (mealSummaries.isEmpty()) {
                return@runCatching emptyList()
            }

            // Consultar detalle completo de cada receta para contrastar ingredientes
            val results = mutableListOf<RecipeMatchResult>()
            for (summary in mealSummaries) {
                try {
                    val detailResponse = api.getMealDetails(summary.idMeal)
                    val detail = detailResponse.meals?.firstOrNull() ?: continue
                    val matchResult = RecipeInventoryMatcher.matchRecipe(
                        meal = detail,
                        inventory = currentInventory,
                        priorityIngredient = foodItem.name
                    )
                    results.add(matchResult)
                } catch (_: Exception) {
                    // Si falla una receta individual por timeout o formato, continuar con las demás
                }
            }

            // Ordenar recetas: primero por porcentaje de compatibilidad, luego por cantidad de otros alimentos que aprovecha
            results.sortedWith(
                compareByDescending<RecipeMatchResult> { it.compatibilityPercentage }
                    .thenByDescending { it.otherExpiringItemsUtilized.size }
            )
        }
    }
}
