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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FoodItem
import com.example.data.model.FoodLocation
import com.example.data.model.PerishabilityLevel
import com.example.ui.FoodWiseUiState
import com.example.ui.theme.PriorityCriticalRed
import com.example.ui.theme.PriorityMediumOrange
import com.example.ui.theme.PrioritySafeGreen
import com.example.ui.theme.PriorityWarningAmber

@Composable
fun InventoryScreen(
    state: FoodWiseUiState,
    onSearchChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onLocationSelected: (FoodLocation?) -> Unit,
    onSaveFood: (id: Long, name: String, nameEn: String?, qty: Double, unit: String, expDate: Long?, category: String, loc: FoodLocation, perish: PerishabilityLevel, notes: String) -> Unit,
    onDeleteFood: (FoodItem, String) -> Unit,
    onConsumePartial: (FoodItem, Double, String) -> Unit,
    onConsumeTotal: (FoodItem, String) -> Unit,
    onDiscardFood: (FoodItem, String) -> Unit,
    onNavigateToRecipes: (FoodItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<FoodItem?>(null) }
    var itemToConsume by remember { mutableStateOf<FoodItem?>(null) }
    var itemToDiscard by remember { mutableStateOf<FoodItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FoodItem?>(null) }

    val categories = listOf("Todos", "Carnes", "Lácteos", "Vegetales", "Frutas", "Despensa", "Legumbres", "Condimentos")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    itemToEdit = null
                    showAddDialog = true
                },
                modifier = Modifier.testTag("inventory_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Alimento")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("inventory_screen")
        ) {
            // Buscador
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("inventory_search_field"),
                placeholder = { Text("Buscar por nombre o categoría...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Filtro por Ubicación
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedLocation == null,
                        onClick = { onLocationSelected(null) },
                        label = { Text("Todas Ubicaciones") }
                    )
                }
                items(FoodLocation.entries) { loc ->
                    FilterChip(
                        selected = state.selectedLocation == loc,
                        onClick = { onLocationSelected(loc) },
                        label = { Text(loc.displayName) }
                    )
                }
            }

            // Filtro por Categoría
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = state.selectedCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            // Lista de Alimentos
            if (state.filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (state.searchQuery.isNotBlank() || state.selectedCategory != "Todos") {
                                "No hay alimentos que coincidan con los filtros"
                            } else {
                                "Inventario vacío. Pulsa el botón '+' para registrar alimentos."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredItems, key = { it.id }) { food ->
                        FoodItemCard(
                            item = food,
                            onConsume = { itemToConsume = food },
                            onDiscard = { itemToDiscard = food },
                            onRecipes = { onNavigateToRecipes(food) },
                            onEdit = {
                                itemToEdit = food
                                showAddDialog = true
                            },
                            onDelete = { itemToDelete = food }
                        )
                    }
                }
            }
        }
    }

    // Diálogos de Gestión
    if (showAddDialog) {
        AddEditFoodDialog(
            initialItem = itemToEdit,
            onDismiss = { showAddDialog = false },
            onConfirm = { id, name, nameEn, qty, unit, expDate, cat, loc, perish, notes ->
                onSaveFood(id, name, nameEn, qty, unit, expDate, cat, loc, perish, notes)
                showAddDialog = false
            }
        )
    }

    itemToConsume?.let { food ->
        ConsumeFoodDialog(
            item = food,
            onDismiss = { itemToConsume = null },
            onConsumePartial = { qty, note ->
                onConsumePartial(food, qty, note)
                itemToConsume = null
            },
            onConsumeTotal = { note ->
                onConsumeTotal(food, note)
                itemToConsume = null
            }
        )
    }

    itemToDiscard?.let { food ->
        DiscardFoodDialog(
            item = food,
            onDismiss = { itemToDiscard = null },
            onConfirm = { reason ->
                onDiscardFood(food, reason)
                itemToDiscard = null
            }
        )
    }

    itemToDelete?.let { food ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Eliminar Registro") },
            text = { Text("¿Deseas eliminar '${food.name}' del inventario? Esta acción quedará registrada en el historial de movimientos.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFood(food, "Eliminado manualmente por el usuario")
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityCriticalRed)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FoodItemCard(
    item: FoodItem,
    onConsume: () -> Unit,
    onDiscard: () -> Unit,
    onRecipes: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val daysRemaining = item.daysUntilExpiration()
    val isExpired = daysRemaining != null && daysRemaining < 0

    val badgeColor = when {
        daysRemaining == null -> Color.Gray
        isExpired -> PriorityCriticalRed
        daysRemaining == 0L -> PriorityCriticalRed
        daysRemaining in 1..2 -> PriorityWarningAmber
        daysRemaining in 3..5 -> PriorityMediumOrange
        else -> PrioritySafeGreen
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!item.nameEn.isNullOrBlank()) {
                        Text(
                            text = "Mapeo TheMealDB: ${item.nameEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "${item.quantity} ${item.unit} • ${item.category} • ${item.location.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar datos") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Etiquetas de estado y perecibilidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    val statusText = when {
                        daysRemaining == null -> "Vencimiento desconocido"
                        isExpired -> "Venció hace ${-daysRemaining}d"
                        daysRemaining == 0L -> "Vence hoy"
                        daysRemaining == 1L -> "Vence mañana"
                        else -> "Vence en ${daysRemaining}d"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Perecibilidad: ${item.perishability.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Acciones directas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConsume,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Consumir", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onDiscard,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PriorityCriticalRed),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Descartar", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onRecipes,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Recetas", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddEditFoodDialog(
    initialItem: FoodItem?,
    onDismiss: () -> Unit,
    onConfirm: (
        id: Long,
        name: String,
        nameEn: String?,
        quantity: Double,
        unit: String,
        expirationDate: Long?,
        category: String,
        location: FoodLocation,
        perishability: PerishabilityLevel,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name.orEmpty()) }
    var nameEn by remember { mutableStateOf(initialItem?.nameEn.orEmpty()) }
    var quantityStr by remember { mutableStateOf(initialItem?.quantity?.toString() ?: "1.0") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "unidades") }
    var hasUnknownExpiration by remember { mutableStateOf(initialItem?.expirationDate == null && initialItem != null) }
    var daysUntilExpStr by remember {
        mutableStateOf(
            initialItem?.daysUntilExpiration()?.toString() ?: "5"
        )
    }
    var category by remember { mutableStateOf(initialItem?.category ?: "Vegetales") }
    var selectedLocation by remember { mutableStateOf(initialItem?.location ?: FoodLocation.NEVERA) }
    var selectedPerishability by remember { mutableStateOf(initialItem?.perishability ?: PerishabilityLevel.MEDIA) }
    var notes by remember { mutableStateOf(initialItem?.notes.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialItem == null) "Registrar Nuevo Alimento" else "Editar Alimento")
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del alimento *") },
                        placeholder = { Text("Ej. Tomates, Pechuga de pollo...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("Ingrediente en inglés (opcional para TheMealDB)") },
                        placeholder = { Text("Ej. Chicken, Tomato, Rice...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Cantidad *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unidad *") },
                            placeholder = { Text("kg, g, l, un") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasUnknownExpiration = !hasUnknownExpiration }
                    ) {
                        Checkbox(
                            checked = hasUnknownExpiration,
                            onCheckedChange = { hasUnknownExpiration = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Fecha de vencimiento desconocida (incertidumbre)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (!hasUnknownExpiration) {
                    item {
                        OutlinedTextField(
                            value = daysUntilExpStr,
                            onValueChange = { daysUntilExpStr = it },
                            label = { Text("Días restantes hasta el vencimiento *") },
                            placeholder = { Text("0 = hoy, negativo = vencido") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Text("Ubicación en el hogar:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FoodLocation.entries.forEach { loc ->
                            FilterChip(
                                selected = selectedLocation == loc,
                                onClick = { selectedLocation = loc },
                                label = { Text(loc.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                item {
                    Text("Perecibilidad intrínseca:", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PerishabilityLevel.entries.forEach { lvl ->
                            FilterChip(
                                selected = selectedPerishability == lvl,
                                onClick = { selectedPerishability = lvl },
                                label = { Text("${lvl.displayName} (P = ${lvl.score})") }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas u observaciones") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }

                errorMessage?.let { err ->
                    item {
                        Text(
                            text = err,
                            color = PriorityCriticalRed,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "El nombre del alimento es obligatorio"
                        return@Button
                    }
                    val qty = quantityStr.toDoubleOrNull()
                    if (qty == null || qty <= 0.0) {
                        errorMessage = "Ingresa una cantidad numérica válida mayor a 0"
                        return@Button
                    }
                    val expDateMillis: Long? = if (hasUnknownExpiration) {
                        null
                    } else {
                        val days = daysUntilExpStr.toLongOrNull()
                        if (days == null) {
                            errorMessage = "Ingresa un número válido de días hasta el vencimiento"
                            return@Button
                        }
                        System.currentTimeMillis() + (days * 24L * 60L * 60L * 1000L)
                    }

                    onConfirm(
                        initialItem?.id ?: 0L,
                        name,
                        nameEn,
                        qty,
                        unit,
                        expDateMillis,
                        category,
                        selectedLocation,
                        selectedPerishability,
                        notes
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ConsumeFoodDialog(
    item: FoodItem,
    onDismiss: () -> Unit,
    onConsumePartial: (Double, String) -> Unit,
    onConsumeTotal: (String) -> Unit
) {
    var consumeQtyStr by remember { mutableStateOf(item.quantity.toString()) }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Consumo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "¿Cuánto de '${item.name}' vas a consumir? (Disponible: ${item.quantity} ${item.unit})",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = consumeQtyStr,
                    onValueChange = { consumeQtyStr = it },
                    label = { Text("Cantidad a consumir (${item.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota o receta preparada (opcional)") },
                    placeholder = { Text("Ej. Almuerzo, ensalada, cena...") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMsg?.let {
                    Text(text = it, color = PriorityCriticalRed, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val qty = consumeQtyStr.toDoubleOrNull()
                        if (qty == null || qty <= 0.0) {
                            errorMsg = "Cantidad inválida"
                            return@Button
                        }
                        if (qty >= item.quantity) {
                            onConsumeTotal(note)
                        } else {
                            onConsumePartial(qty, note)
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DiscardFoodDialog(
    item: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Vencimiento sobrepasado") }
    val reasons = listOf(
        "Vencimiento sobrepasado",
        "Signos de descomposición o mal olor",
        "Moho o contaminación visible",
        "Deterioro por frío / quemadura de congelador",
        "Sobrante olvidado en el refrigerador"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Descarte de Alimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Se registrará el descarte de ${item.quantity} ${item.unit} de '${item.name}' para el control de mermas e historial.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text("Motivo del descarte:", style = MaterialTheme.typography.labelMedium)
                reasons.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = r }
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (selectedReason == r) PriorityCriticalRed else Color.LightGray)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(r, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = PriorityCriticalRed)
            ) {
                Text("Descartar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
