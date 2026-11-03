package com.example.data.local

import com.example.data.model.FoodLocation
import com.example.data.model.MovementType
import com.example.data.model.PerishabilityLevel

object InitialDataSeeder {

    fun getSampleItems(currentTime: Long = System.currentTimeMillis()): List<FoodItemEntity> {
        val oneDay = 24L * 60L * 60L * 1000L

        return listOf(
            FoodItemEntity(
                name = "Yogur Griego Natural",
                nameEn = "Yogurt",
                quantity = 500.0,
                unit = "g",
                expirationDate = currentTime - oneDay, // Vencido ayer (-1 día)
                acquisitionDate = currentTime - (6 * oneDay),
                category = "Lácteos",
                location = FoodLocation.NEVERA.name,
                perishability = PerishabilityLevel.MUY_ALTA.name,
                notes = "Envase abierto hace 3 días en refrigeración."
            ),
            FoodItemEntity(
                name = "Pechuga de Pollo Fresca",
                nameEn = "Chicken",
                quantity = 1.2,
                unit = "kg",
                expirationDate = currentTime + oneDay, // Vence mañana (+1 día)
                acquisitionDate = currentTime - oneDay,
                category = "Carnes",
                location = FoodLocation.NEVERA.name,
                perishability = PerishabilityLevel.MUY_ALTA.name,
                notes = "Filetes frescos listos para cocinar o marinar."
            ),
            FoodItemEntity(
                name = "Espinacas Frescas",
                nameEn = "Spinach",
                quantity = 300.0,
                unit = "g",
                expirationDate = currentTime + (2 * oneDay), // Vence en 2 días
                acquisitionDate = currentTime - oneDay,
                category = "Vegetales",
                location = FoodLocation.NEVERA.name,
                perishability = PerishabilityLevel.ALTA.name,
                notes = "Bolsa en cajón de verduras."
            ),
            FoodItemEntity(
                name = "Tomates Maduros",
                nameEn = "Tomato",
                quantity = 1.5,
                unit = "kg",
                expirationDate = currentTime + (3 * oneDay), // Vence en 3 días
                acquisitionDate = currentTime - (2 * oneDay),
                category = "Vegetales",
                location = FoodLocation.NEVERA.name,
                perishability = PerishabilityLevel.ALTA.name,
                notes = "Ideal para salsas, guisos o ensaladas."
            ),
            FoodItemEntity(
                name = "Leche Entera",
                nameEn = "Milk",
                quantity = 2.0,
                unit = "l",
                expirationDate = currentTime + (4 * oneDay), // Vence en 4 días
                acquisitionDate = currentTime - oneDay,
                category = "Lácteos",
                location = FoodLocation.NEVERA.name,
                perishability = PerishabilityLevel.MEDIA.name,
                notes = "Cartón en la puerta del refrigerador."
            ),
            FoodItemEntity(
                name = "Manzanas Rojas",
                nameEn = "Apple",
                quantity = 1.0,
                unit = "kg",
                expirationDate = currentTime + (7 * oneDay), // Vence en 7 días
                acquisitionDate = currentTime - (2 * oneDay),
                category = "Frutas",
                location = FoodLocation.DESPENSA.name,
                perishability = PerishabilityLevel.MEDIA.name,
                notes = "Frutero en temperatura ambiente."
            ),
            FoodItemEntity(
                name = "Queso Curado",
                nameEn = "Cheese",
                quantity = 250.0,
                unit = "g",
                expirationDate = currentTime + (12 * oneDay), // Vence en 12 días
                acquisitionDate = currentTime - (4 * oneDay),
                category = "Lácteos",
                location = FoodLocation.NEVERA.name,
                perishability = PerishabilityLevel.MEDIA.name,
                notes = "Cuña de queso curado al vacío."
            ),
            FoodItemEntity(
                name = "Arroz Blanco Grano Largo",
                nameEn = "Rice",
                quantity = 2.0,
                unit = "kg",
                expirationDate = currentTime + (180 * oneDay), // 180 días
                acquisitionDate = currentTime - (10 * oneDay),
                category = "Despensa",
                location = FoodLocation.DESPENSA.name,
                perishability = PerishabilityLevel.MUY_BAJA.name,
                notes = "Paquete cerrado en alacena seca."
            ),
            FoodItemEntity(
                name = "Lentejas Pardinas",
                nameEn = "Lentils",
                quantity = 1.0,
                unit = "kg",
                expirationDate = currentTime + (240 * oneDay), // 240 días
                acquisitionDate = currentTime - (15 * oneDay),
                category = "Legumbres",
                location = FoodLocation.DESPENSA.name,
                perishability = PerishabilityLevel.MUY_BAJA.name,
                notes = "Almacenado herméticamente."
            ),
            FoodItemEntity(
                name = "Mix de Especias Casero",
                nameEn = null,
                quantity = 80.0,
                unit = "g",
                expirationDate = null, // Fecha desconocida (ejemplo de dato incompleto)
                acquisitionDate = currentTime - (30 * oneDay),
                category = "Condimentos",
                location = FoodLocation.DESPENSA.name,
                perishability = PerishabilityLevel.BAJA.name,
                notes = "Frasco reutilizable. Fecha de vencimiento original no disponible."
            )
        )
    }

    fun getInitialMovements(items: List<FoodItemEntity>, currentTime: Long = System.currentTimeMillis()): List<InventoryMovementEntity> {
        return items.map { item ->
            InventoryMovementEntity(
                foodItemId = 0,
                foodItemName = item.name,
                movementType = MovementType.CREACION.name,
                quantity = item.quantity,
                unit = item.unit,
                timestamp = item.acquisitionDate,
                reason = "Carga inicial de despensa de demostración para evaluación del agente"
            )
        }
    }
}
