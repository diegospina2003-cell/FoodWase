package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.FoodItem
import com.example.data.network.RecipeMatchResult
import com.example.ui.FoodWiseUiState
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.PrioritySafeGreen
import com.example.ui.theme.PriorityWarningAmber

@Composable
fun RecipesScreen(
    state: FoodWiseUiState,
    onSelectFoodForRecipes: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRecipeDetail by remember { mutableStateOf<RecipeMatchResult?>(null) }
    val activeFood = state.activeRecipeIngredient ?: state.topPriorityItem?.foodItem

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recipes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Selector de Ingrediente para Consulta en TheMealDB
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "CAPA 2: Ejecución de Recetas (TheMealDB)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "El agente selecciona el alimento prioritario y consulta la API real de TheMealDB, cruzando ingredientes con tu despensa para maximizar el aprovechamiento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Selecciona un alimento a consultar:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.items.filter { !it.isArchived }) { item ->
                            val isSelected = activeFood?.id == item.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectFoodForRecipes(item) },
                                label = { Text(item.name) }
                            )
                        }
                    }
                }
            }
        }

        // Estado de Carga
        if (state.isLoadingRecipes) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Consultando TheMealDB para '${activeFood?.name ?: "alimento"}'...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Error o Sin Recetas
        if (!state.isLoadingRecipes && state.recipeErrorMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PriorityWarningAmber.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = PriorityWarningAmber)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Información de Consulta",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = PriorityWarningAmber
                            )
                            Text(
                                text = state.recipeErrorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        activeFood?.let { food ->
                            IconButton(onClick = { onSelectFoodForRecipes(food) }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reintentar")
                            }
                        }
                    }
                }
            }
        }

        // Lista de Recetas Obtenidas de TheMealDB
        if (!state.isLoadingRecipes && state.suggestedRecipes.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recetas Sugeridas (${state.suggestedRecipes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Para: ${activeFood?.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(state.suggestedRecipes) { recipe ->
                RecipeResultCard(
                    recipe = recipe,
                    onViewDetails = { selectedRecipeDetail = recipe }
                )
            }
        }
    }

    // Modal de Detalle Completo de la Receta
    selectedRecipeDetail?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            onDismiss = { selectedRecipeDetail = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeResultCard(
    recipe: RecipeMatchResult,
    onViewDetails: () -> Unit
) {
    val compatColor = when {
        recipe.compatibilityPercentage >= 70 -> PrioritySafeGreen
        recipe.compatibilityPercentage >= 40 -> PriorityWarningAmber
        else -> PriorityCriticalRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Imagen de la receta con banner de compatibilidad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (!recipe.mealThumb.isNullOrBlank()) {
                    AsyncImage(
                        model = recipe.mealThumb,
                        contentDescription = recipe.mealName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Surface(
                    color = compatColor,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = "${recipe.compatibilityPercentage}% disponible",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = recipe.mealName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${recipe.category.orEmpty()} • ${recipe.area.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Alerta si aprovecha otros alimentos en riesgo
                if (recipe.otherExpiringItemsUtilized.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "💡 Aprovecha también: ${recipe.otherExpiringItemsUtilized.joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Resumen de Ingredientes: Disponibles vs Faltantes
                Text(
                    text = "Ingredientes Disponibles (${recipe.availableIngredients.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrioritySafeGreen
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    recipe.availableIngredients.take(5).forEach { ing ->
                        Surface(
                            color = PrioritySafeGreen.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "✓ ${ing.first}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrioritySafeGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (recipe.missingIngredients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ingredientes Faltantes (${recipe.missingIngredients.size}):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PriorityWarningAmber
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.missingIngredients.take(5).forEach { ing ->
                            Surface(
                                color = PriorityWarningAmber.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "✗ ${ing.first}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PriorityWarningAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Ver Instrucciones de Preparación")
                }
            }
        }
    }
}

@Composable
fun RecipeDetailDialog(
    recipe: RecipeMatchResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = recipe.mealName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!recipe.mealThumb.isNullOrBlank()) {
                    item {
                        AsyncImage(
                            model = recipe.mealThumb,
                            contentDescription = recipe.mealName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                item {
                    Text(
                        text = "Compatibilidad con tu Despensa: ${recipe.compatibilityPercentage}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Text(
                        text = "Todos los Ingredientes y Medidas:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        recipe.requiredIngredients.forEach { (ing, measure) ->
                            val isAvailable = recipe.availableIngredients.any { it.first.equals(ing, ignoreCase = true) }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isAvailable) Icons.Default.Check else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (isAvailable) PrioritySafeGreen else PriorityCriticalRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$ing: $measure",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAvailable) PrioritySafeGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Instrucciones de Preparación:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = recipe.instructions ?: "Sin instrucciones disponibles.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!recipe.sourceUrl.isNullOrBlank()) {
                    item {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(recipe.sourceUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver Receta Original")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
