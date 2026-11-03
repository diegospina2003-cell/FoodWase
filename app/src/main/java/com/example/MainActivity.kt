package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.FoodItem
import com.example.ui.FoodWiseViewModel
import com.example.ui.screens.AddEditFoodDialog
import com.example.ui.screens.ConsumeFoodDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiscardFoodDialog
import com.example.ui.screens.EvaluationScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.PrioritiesScreen
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FoodWiseTheme

enum class FoodWiseScreen(val title: String, val navLabel: String, val icon: ImageVector) {
    DASHBOARD("FoodWIse", "Inicio", Icons.Default.Home),
    INVENTORY("Mi Inventario", "Despensa", Icons.Default.Inventory2),
    PRIORITIES("Prioridades del Agente", "Prioridad", Icons.AutoMirrored.Filled.Sort),
    RECIPES("Recetas TheMealDB", "Recetas", Icons.Default.Restaurant),
    HISTORY("Historial de Consumo", "Historial", Icons.Default.History),
    EVALUATION("Evaluación del Agente", "Evaluación", Icons.Default.Science),
    SETTINGS("Configuración", "Ajustes", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: FoodWiseViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FoodWiseTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var currentScreen by remember { mutableStateOf(FoodWiseScreen.DASHBOARD) }

                var showAddFoodDialog by remember { mutableStateOf(false) }
                var itemToConsume by remember { mutableStateOf<FoodItem?>(null) }
                var itemToDiscard by remember { mutableStateOf<FoodItem?>(null) }

                // Soporte de botón Atrás nativo
                BackHandler(enabled = currentScreen != FoodWiseScreen.DASHBOARD) {
                    currentScreen = FoodWiseScreen.DASHBOARD
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = currentScreen.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentScreen = FoodWiseScreen.EVALUATION },
                                    modifier = Modifier.testTag("topbar_evaluation_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = "Evaluación del Agente",
                                        tint = if (currentScreen == FoodWiseScreen.EVALUATION) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { currentScreen = FoodWiseScreen.SETTINGS },
                                    modifier = Modifier.testTag("topbar_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Configuración",
                                        tint = if (currentScreen == FoodWiseScreen.SETTINGS) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        // Navegación principal
                        val mainScreens = listOf(
                            FoodWiseScreen.DASHBOARD,
                            FoodWiseScreen.INVENTORY,
                            FoodWiseScreen.PRIORITIES,
                            FoodWiseScreen.RECIPES,
                            FoodWiseScreen.HISTORY
                        )

                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            mainScreens.forEach { screen ->
                                val isSelected = currentScreen == screen
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { currentScreen = screen },
                                    icon = {
                                        Icon(imageVector = screen.icon, contentDescription = screen.navLabel)
                                    },
                                    label = { Text(screen.navLabel) },
                                    modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            FoodWiseScreen.DASHBOARD -> DashboardScreen(
                                state = state,
                                onNavigateToInventory = { currentScreen = FoodWiseScreen.INVENTORY },
                                onNavigateToPriorities = { currentScreen = FoodWiseScreen.PRIORITIES },
                                onNavigateToRecipes = { foodItem ->
                                    viewModel.fetchRecipesForFood(foodItem)
                                    currentScreen = FoodWiseScreen.RECIPES
                                },
                                onOpenAddDialog = { showAddFoodDialog = true },
                                onConsumeFood = { foodItem -> itemToConsume = foodItem },
                                onDiscardFood = { foodItem -> itemToDiscard = foodItem }
                            )

                            FoodWiseScreen.INVENTORY -> InventoryScreen(
                                state = state,
                                onSearchChanged = { viewModel.setSearchQuery(it) },
                                onCategorySelected = { viewModel.setSelectedCategory(it) },
                                onLocationSelected = { viewModel.setSelectedLocation(it) },
                                onSaveFood = { id, name, nameEn, qty, unit, expDate, cat, loc, perish, notes ->
                                    viewModel.saveFoodItem(id, name, nameEn, qty, unit, expDate, cat, loc, perish, notes)
                                },
                                onDeleteFood = { food, reason -> viewModel.deleteFoodItem(food, reason) },
                                onConsumePartial = { food, qty, note -> viewModel.consumePartial(food, qty, note) },
                                onConsumeTotal = { food, note -> viewModel.consumeTotal(food, note) },
                                onDiscardFood = { food, reason -> viewModel.discardFood(food, reason) },
                                onNavigateToRecipes = { foodItem ->
                                    viewModel.fetchRecipesForFood(foodItem)
                                    currentScreen = FoodWiseScreen.RECIPES
                                }
                            )

                            FoodWiseScreen.PRIORITIES -> PrioritiesScreen(
                                state = state,
                                onNavigateToRecipes = { foodItem ->
                                    viewModel.fetchRecipesForFood(foodItem)
                                    currentScreen = FoodWiseScreen.RECIPES
                                }
                            )

                            FoodWiseScreen.RECIPES -> RecipesScreen(
                                state = state,
                                onSelectFoodForRecipes = { foodItem ->
                                    viewModel.fetchRecipesForFood(foodItem)
                                }
                            )

                            FoodWiseScreen.HISTORY -> HistoryScreen(
                                state = state
                            )

                            FoodWiseScreen.EVALUATION -> EvaluationScreen(
                                state = state,
                                onRunEvaluation = { viewModel.runAgentEvaluation() }
                            )

                            FoodWiseScreen.SETTINGS -> SettingsScreen(
                                state = state,
                                onSaveConfig = { viewModel.updateConfig(it) },
                                onResetDemoPantry = { viewModel.resetDemoPantry() }
                            )
                        }
                    }
                }

                // Diálogos Globales
                if (showAddFoodDialog) {
                    AddEditFoodDialog(
                        initialItem = null,
                        onDismiss = { showAddFoodDialog = false },
                        onConfirm = { id, name, nameEn, qty, unit, expDate, cat, loc, perish, notes ->
                            viewModel.saveFoodItem(id, name, nameEn, qty, unit, expDate, cat, loc, perish, notes)
                            showAddFoodDialog = false
                        }
                    )
                }

                itemToConsume?.let { food ->
                    ConsumeFoodDialog(
                        item = food,
                        onDismiss = { itemToConsume = null },
                        onConsumePartial = { qty, note ->
                            viewModel.consumePartial(food, qty, note)
                            itemToConsume = null
                        },
                        onConsumeTotal = { note ->
                            viewModel.consumeTotal(food, note)
                            itemToConsume = null
                        }
                    )
                }

                itemToDiscard?.let { food ->
                    DiscardFoodDialog(
                        item = food,
                        onDismiss = { itemToDiscard = null },
                        onConfirm = { reason ->
                            viewModel.discardFood(food, reason)
                            itemToDiscard = null
                        }
                    )
                }
            }
        }
    }
}
