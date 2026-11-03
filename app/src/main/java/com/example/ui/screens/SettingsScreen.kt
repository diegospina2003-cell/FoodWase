package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AgentConfig
import com.example.ui.FoodWiseUiState
import com.example.ui.theme.PriorityCriticalRed
import java.util.Locale

@Composable
fun SettingsScreen(
    state: FoodWiseUiState,
    onSaveConfig: (AgentConfig) -> Unit,
    onResetDemoPantry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var wExp by remember(state.agentConfig) { mutableFloatStateOf(state.agentConfig.wExpiration.toFloat()) }
    var wPer by remember(state.agentConfig) { mutableFloatStateOf(state.agentConfig.wPerishability.toFloat()) }
    var wQty by remember(state.agentConfig) { mutableFloatStateOf(state.agentConfig.wQuantity.toFloat()) }
    var highThresh by remember(state.agentConfig) { mutableFloatStateOf(state.agentConfig.highPriorityThreshold.toFloat()) }
    var medThresh by remember(state.agentConfig) { mutableFloatStateOf(state.agentConfig.mediumPriorityThreshold.toFloat()) }

    var showResetDialog by remember { mutableStateOf(false) }

    val totalWeight = wExp + wPer + wQty
    val normExp = if (totalWeight > 0f) wExp / totalWeight else 0.33f
    val normPer = if (totalWeight > 0f) wPer / totalWeight else 0.33f
    val normQty = if (totalWeight > 0f) wQty / totalWeight else 0.33f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Calibrador de la Función de Utilidad
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Calibrador de Ponderaciones (Agente de Utilidad)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ajusta la importancia relativa de cada factor en la función U = w₁·P_venc + w₂·P_perec + w₃·P_cant. Los pesos se normalizan automáticamente a 1.0.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Peso 1: Vencimiento
                    Text(
                        text = String.format(Locale.ROOT, "w₁ Proximidad del Vencimiento: %.2f (Normalizado: %.2f)", wExp, normExp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = wExp,
                        onValueChange = { wExp = it },
                        valueRange = 0.1f..1.0f
                    )

                    // Peso 2: Perecibilidad
                    Text(
                        text = String.format(Locale.ROOT, "w₂ Perecibilidad Intrínseca: %.2f (Normalizado: %.2f)", wPer, normPer),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = wPer,
                        onValueChange = { wPer = it },
                        valueRange = 0.1f..1.0f
                    )

                    // Peso 3: Cantidad
                    Text(
                        text = String.format(Locale.ROOT, "w₃ Cantidad en Riesgo: %.2f (Normalizado: %.2f)", wQty, normQty),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = wQty,
                        onValueChange = { wQty = it },
                        valueRange = 0.05f..1.0f
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Umbrales
                    Text(
                        text = String.format(Locale.ROOT, "Umbral Alta Prioridad: ≥ %.2f", highThresh),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = highThresh,
                        onValueChange = { highThresh = it },
                        valueRange = 0.50f..0.90f
                    )

                    Text(
                        text = String.format(Locale.ROOT, "Umbral Media Prioridad: ≥ %.2f", medThresh),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        value = medThresh,
                        onValueChange = { medThresh = it },
                        valueRange = 0.20f..0.60f
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val newConfig = AgentConfig(
                                    wExpiration = normExp.toDouble(),
                                    wPerishability = normPer.toDouble(),
                                    wQuantity = normQty.toDouble(),
                                    highPriorityThreshold = highThresh.toDouble(),
                                    mediumPriorityThreshold = medThresh.toDouble()
                                )
                                onSaveConfig(newConfig)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar y Recalcular")
                        }

                        OutlinedButton(
                            onClick = {
                                wExp = 0.55f
                                wPer = 0.30f
                                wQty = 0.15f
                                highThresh = 0.70f
                                medThresh = 0.45f
                                onSaveConfig(AgentConfig())
                            }
                        ) {
                            Text("Por Defecto")
                        }
                    }
                }
            }
        }

        // Gestión de Datos y Demostración
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Despensa de Demostración",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Restaura la despensa de prueba representativa con 10 productos (lácteos, carnes, verduras, despensa seca) y fechas de vencimiento variadas para evaluar el comportamiento del agente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restaurar Despensa de Demostración")
                    }
                }
            }
        }

        // Marco Conceptual y Académico
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fundamentación Académica",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "1. Decisión Central:\n" +
                                "«El agente decide, en cada momento, qué alimento del hogar requiere atención primero y qué acción recomendar sobre él —alertar o proponer una receta— para evitar que se desperdicie».\n\n" +
                                "2. Tipo de Agente: Basado en Utilidad Exclusivamente:\n" +
                                "Compara alternativas cuantitativamente en un espacio de preferencias donde los factores compiten entre sí. No implementa algoritmos de búsqueda de caminos (A*, BFS) porque no busca alcanzar un estado meta mediante secuencias de transiciones.\n\n" +
                                "3. Tres Capas Separadas:\n" +
                                "• Capa 1: Núcleo de utilidad determinista.\n" +
                                "• Capa 2: Ejecución de la acción mediante consulta a TheMealDB.\n" +
                                "• Capa 3: Trazabilidad inmutable de consumos y descartes.\n\n" +
                                "4. Transparencia:\n" +
                                "No se emplea un modelo de Machine Learning simulado ni se afirman reducciones de desperdicio sin estudios de campo rigurosos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restaurar Despensa") },
            text = { Text("¿Deseas restablecer el inventario a la despensa de prueba predeterminada? Se reiniciarán los alimentos y movimientos.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDemoPantry()
                        showResetDialog = false
                    }
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
