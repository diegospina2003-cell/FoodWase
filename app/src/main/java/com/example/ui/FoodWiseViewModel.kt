package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.UtilityDecisionAgent
import com.example.agent.evaluation.AgentEvaluationEngine
import com.example.agent.evaluation.EvaluationReport
import com.example.data.local.FoodWiseDatabase
import com.example.data.model.AgentConfig
import com.example.data.model.AgentPriorityResult
import com.example.data.model.FoodItem
import com.example.data.model.FoodLocation
import com.example.data.model.InventoryMovement
import com.example.data.model.MovementType
import com.example.data.model.PerishabilityLevel
import com.example.data.model.PriorityLevel
import com.example.data.network.RecipeMatchResult
import com.example.data.network.TheMealDbClient
import com.example.data.repository.FoodWiseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FoodWiseUiState(
    val items: List<FoodItem> = emptyList(),
    val filteredItems: List<FoodItem> = emptyList(),
    val priorityResults: List<AgentPriorityResult> = emptyList(),
    val topPriorityItem: AgentPriorityResult? = null,
    val movements: List<InventoryMovement> = emptyList(),
    val agentConfig: AgentConfig = AgentConfig(),
    val suggestedRecipes: List<RecipeMatchResult> = emptyList(),
    val selectedRecipeDetail: RecipeMatchResult? = null,
    val isLoadingRecipes: Boolean = false,
    val recipeErrorMessage: String? = null,
    val activeRecipeIngredient: FoodItem? = null,
    val searchQuery: String = "",
    val selectedCategory: String = "Todos",
    val selectedLocation: FoodLocation? = null,
    val evaluationReport: EvaluationReport? = null,
    val isEvaluatingAgent: Boolean = false,
    val totalActiveCount: Int = 0,
    val expiredCount: Int = 0,
    val expiringSoonCount: Int = 0,
    val highPriorityCount: Int = 0,
    val totalConsumedCount: Int = 0,
    val totalDiscardedCount: Int = 0
)

private data class RepoState(
    val items: List<FoodItem> = emptyList(),
    val movements: List<InventoryMovement> = emptyList(),
    val config: AgentConfig = AgentConfig()
)

private data class FilterState(
    val search: String = "",
    val category: String = "Todos",
    val location: FoodLocation? = null
)

private data class RecipeState(
    val recipes: List<RecipeMatchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeIngredient: FoodItem? = null,
    val selectedRecipe: RecipeMatchResult? = null
)

class FoodWiseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FoodWiseRepository
    private val agent = UtilityDecisionAgent()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("Todos")
    private val _selectedLocation = MutableStateFlow<FoodLocation?>(null)

    private val _suggestedRecipes = MutableStateFlow<List<RecipeMatchResult>>(emptyList())
    private val _selectedRecipeDetail = MutableStateFlow<RecipeMatchResult?>(null)
    private val _isLoadingRecipes = MutableStateFlow(false)
    private val _recipeErrorMessage = MutableStateFlow<String?>(null)
    private val _activeRecipeIngredient = MutableStateFlow<FoodItem?>(null)

    private val _evaluationReport = MutableStateFlow<EvaluationReport?>(null)
    private val _isEvaluatingAgent = MutableStateFlow(false)

    init {
        val database = FoodWiseDatabase.getDatabase(application, viewModelScope)
        repository = FoodWiseRepository(database.foodWiseDao(), TheMealDbClient.api)
    }

    private val repoFlow = combine(
        repository.allActiveItems,
        repository.allMovements,
        repository.agentConfig
    ) { items, movements, config ->
        RepoState(items, movements, config)
    }

    private val filterFlow = combine(
        _searchQuery,
        _selectedCategory,
        _selectedLocation
    ) { search, cat, loc ->
        FilterState(search, cat, loc)
    }

    private val recipeFlow = combine(
        _suggestedRecipes,
        _isLoadingRecipes,
        _recipeErrorMessage,
        _activeRecipeIngredient,
        _selectedRecipeDetail
    ) { recipes, loading, error, ingredient, selected ->
        RecipeState(recipes, loading, error, ingredient, selected)
    }

    val uiState: StateFlow<FoodWiseUiState> = combine(
        repoFlow,
        filterFlow,
        recipeFlow,
        _evaluationReport,
        _isEvaluatingAgent
    ) { repo, filter, recipe, evalReport, isEvaluating ->

        val items = repo.items
        val movements = repo.movements
        val config = repo.config

        // 1. Decisión inteligente del agente
        val priorities = agent.evaluateInventory(items, config)
        val topPriority = priorities.firstOrNull()

        // 2. Filtros de inventario
        val filtered = items.filter { item ->
            val matchesSearch = filter.search.isBlank() ||
                    item.name.contains(filter.search, ignoreCase = true) ||
                    (item.nameEn?.contains(filter.search, ignoreCase = true) == true) ||
                    item.category.contains(filter.search, ignoreCase = true)

            val matchesCategory = filter.category == "Todos" || item.category.equals(filter.category, ignoreCase = true)
            val matchesLocation = filter.location == null || item.location == filter.location

            matchesSearch && matchesCategory && matchesLocation
        }

        // 3. Métricas
        val expired = items.count {
            val d = it.daysUntilExpiration()
            d != null && d < 0
        }
        val expiringSoon = items.count {
            val d = it.daysUntilExpiration()
            d != null && d in 0..3
        }
        val highPriority = priorities.count { it.priorityLevel == PriorityLevel.ALTA }

        val consumedCount = movements.count {
            it.movementType == MovementType.CONSUMO_PARCIAL || it.movementType == MovementType.CONSUMO_TOTAL
        }
        val discardedCount = movements.count {
            it.movementType == MovementType.DESCARTE
        }

        FoodWiseUiState(
            items = items,
            filteredItems = filtered,
            priorityResults = priorities,
            topPriorityItem = topPriority,
            movements = movements,
            agentConfig = config,
            suggestedRecipes = recipe.recipes,
            selectedRecipeDetail = recipe.selectedRecipe,
            isLoadingRecipes = recipe.isLoading,
            recipeErrorMessage = recipe.error,
            activeRecipeIngredient = recipe.activeIngredient,
            searchQuery = filter.search,
            selectedCategory = filter.category,
            selectedLocation = filter.location,
            evaluationReport = evalReport,
            isEvaluatingAgent = isEvaluating,
            totalActiveCount = items.size,
            expiredCount = expired,
            expiringSoonCount = expiringSoon,
            highPriorityCount = highPriority,
            totalConsumedCount = consumedCount,
            totalDiscardedCount = discardedCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FoodWiseUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedLocation(location: FoodLocation?) {
        _selectedLocation.value = location
    }

    fun setSelectedRecipeDetail(recipe: RecipeMatchResult?) {
        _selectedRecipeDetail.value = recipe
    }

    fun saveFoodItem(
        id: Long = 0,
        name: String,
        nameEn: String? = null,
        quantity: Double,
        unit: String,
        expirationDate: Long?,
        category: String,
        location: FoodLocation,
        perishability: PerishabilityLevel,
        notes: String
    ) {
        viewModelScope.launch {
            val item = FoodItem(
                id = id,
                name = name.trim(),
                nameEn = nameEn?.trim()?.ifBlank { null },
                quantity = quantity,
                unit = unit.trim(),
                expirationDate = expirationDate,
                category = category.trim().ifBlank { "General" },
                location = location,
                perishability = perishability,
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.registerFoodItem(item)
            } else {
                repository.updateFoodItem(item)
            }
        }
    }

    fun deleteFoodItem(item: FoodItem, reason: String) {
        viewModelScope.launch {
            repository.deleteFoodItem(item, reason)
        }
    }

    fun consumePartial(item: FoodItem, qty: Double, note: String) {
        viewModelScope.launch {
            repository.consumePartial(item, qty, note)
        }
    }

    fun consumeTotal(item: FoodItem, note: String) {
        viewModelScope.launch {
            repository.consumeTotal(item, note)
        }
    }

    fun discardFood(item: FoodItem, reason: String) {
        viewModelScope.launch {
            repository.discardFood(item, reason)
        }
    }

    fun updateConfig(config: AgentConfig) {
        viewModelScope.launch {
            repository.saveAgentConfig(config.normalized())
        }
    }

    fun resetDemoPantry() {
        viewModelScope.launch {
            repository.resetDemoPantry()
        }
    }

    fun fetchRecipesForFood(item: FoodItem) {
        _activeRecipeIngredient.value = item
        _isLoadingRecipes.value = true
        _recipeErrorMessage.value = null

        viewModelScope.launch {
            val currentItems = uiState.value.items
            val result = repository.fetchRecipesForIngredient(item, currentItems)
            result.onSuccess { recipes ->
                _isLoadingRecipes.value = false
                _suggestedRecipes.value = recipes
                if (recipes.isEmpty()) {
                    _recipeErrorMessage.value = "No se encontraron recetas en TheMealDB para '${item.name}'."
                }
            }.onFailure { err ->
                _isLoadingRecipes.value = false
                _recipeErrorMessage.value = "Error de conexión con TheMealDB: ${err.localizedMessage ?: "Verifica tu conexión"}"
            }
        }
    }

    fun runAgentEvaluation() {
        _isEvaluatingAgent.value = true
        viewModelScope.launch {
            val report = AgentEvaluationEngine.runAllTests()
            _evaluationReport.value = report
            _isEvaluatingAgent.value = false
        }
    }
}
