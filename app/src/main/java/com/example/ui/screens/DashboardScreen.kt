package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentPriorityResult
import com.example.data.model.FoodItem
import com.example.data.model.PriorityLevel
import com.example.data.model.RecommendedAction
import com.example.ui.FoodWiseUiState
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.PriorityMediumOrange
import com.example.ui.theme.PrioritySafeGreen
import com.example.ui.theme.PriorityWarningAmber
import java.util.Locale

@Composable
fun DashboardScreen(
    state: FoodWiseUiState,
    onNavigateToInventory: () -> Unit,
    onNavigateToPriorities: () -> Unit,
    onNavigateToRecipes: (FoodItem) -> Unit,
    onOpenAddDialog: () -> Unit,
    onConsumeFood: (FoodItem) -> Unit,
    onDiscardFood: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado de Bienvenida y Propósito
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FoodWIse — Prevención del Desperdicio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Agente inteligente basado en utilidad para priorizar el aprovechamiento de alimentos en el hogar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Resumen de Métricas Clave
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Activos",
                    count = state.totalActiveCount,
                    icon = Icons.Default.Restaurant,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Por Vencer",
                    count = state.expiringSoonCount,
                    icon = Icons.Default.Warning,
                    color = PriorityWarningAmber,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Vencidos",
                    count = state.expiredCount,
                    icon = Icons.Default.Error,
                    color = PriorityCriticalRed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Alta Prioridad",
                    count = state.highPriorityCount,
                    icon = Icons.Default.CheckCircle,
                    color = PriorityMediumOrange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Decisión Central del Agente: Alimento Prioritario
        item {
            state.topPriorityItem?.let { topItem ->
                TopPriorityAgentCard(
                    result = topItem,
                    onNavigateToRecipes = { onNavigateToRecipes(topItem.foodItem) },
                    onConsume = { onConsumeFood(topItem.foodItem) },
                    onDiscard = { onDiscardFood(topItem.foodItem) },
                    onViewAllPriorities = onNavigateToPriorities
                )
            } ?: run {
                EmptyPantryCard(onAddFood = onOpenAddDialog)
            }
        }

        // Sección: Top Alimentos Urgentes
        if (state.priorityResults.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alimentos que Requieren Atención",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ver todos",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onNavigateToPriorities() }
                            .padding(4.dp)
                    )
                }
            }

            items(state.priorityResults.take(4)) { priorityItem ->
                PriorityMiniCard(
                    item = priorityItem,
                    onClickRecipes = { onNavigateToRecipes(priorityItem.foodItem) },
                    onConsume = { onConsumeFood(priorityItem.foodItem) }
                )
            }
        }

        // Botón de Acción Rápida: Agregar Alimento
        item {
            Button(
                onClick = onOpenAddDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_add_food_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar Nuevo Alimento")
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    count: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TopPriorityAgentCard(
    result: AgentPriorityResult,
    onNavigateToRecipes: () -> Unit,
    onConsume: () -> Unit,
    onDiscard: () -> Unit,
    onViewAllPriorities: () -> Unit
) {
    val isExpired = result.isExpired
    val containerColor = if (isExpired) {
        PriorityCriticalRed.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("top_priority_card"),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isExpired) PriorityCriticalRed else PriorityMediumOrange)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DECISIÓN DEL AGENTE: #1 EN PRIORIDAD",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpired) PriorityCriticalRed else MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = if (isExpired) PriorityCriticalRed else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.ROOT, "U = %.3f", result.urgencyScore),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = result.foodItem.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${result.foodItem.quantity} ${result.foodItem.unit} • ${result.foodItem.location.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Recomendación: ${result.recommendedAction.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isExpired) PriorityCriticalRed else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = result.recommendedAction.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isExpired) {
                    Button(
                        onClick = onDiscard,
                        colors = ButtonDefaults.buttonColors(containerColor = PriorityCriticalRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Descarte")
                    }
                } else {
                    Button(
                        onClick = onNavigateToRecipes,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Proponer Receta")
                    }
                }

                FilledTonalButton(
                    onClick = onConsume,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Consumir")
                }
            }
        }
    }
}

@Composable
fun PriorityMiniCard(
    item: AgentPriorityResult,
    onClickRecipes: () -> Unit,
    onConsume: () -> Unit
) {
    val statusColor = when (item.priorityLevel) {
        PriorityLevel.ALTA -> PriorityCriticalRed
        PriorityLevel.MEDIA -> PriorityMediumOrange
        PriorityLevel.BAJA -> PrioritySafeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.foodItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${item.foodItem.quantity} ${item.foodItem.unit} • ${item.daysUntilExpiration?.let { if (it < 0) "Venció hace ${-it}d" else if (it == 0L) "Vence hoy" else "Vence en ${it}d" } ?: "Fecha desconocida"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isExpired) PriorityCriticalRed else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = String.format(Locale.ROOT, "U=%.2f", item.urgencyScore),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onClickRecipes,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Receta", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmptyPantryCard(onAddFood: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tu despensa está vacía",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Registra los alimentos de tu hogar para que el agente calcule sus prioridades y te recomiende recetas de aprovechamiento.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddFood) {
                Text("Registrar Primer Alimento")
            }
        }
    }
}
