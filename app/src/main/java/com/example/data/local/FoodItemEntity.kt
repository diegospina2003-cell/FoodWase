package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.FoodItem
import com.example.data.model.FoodLocation
import com.example.data.model.PerishabilityLevel

@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val nameEn: String? = null,
    val quantity: Double,
    val unit: String,
    val expirationDate: Long?,
    val acquisitionDate: Long = System.currentTimeMillis(),
    val category: String = "General",
    val location: String = FoodLocation.NEVERA.name,
    val perishability: String = PerishabilityLevel.MEDIA.name,
    val notes: String = "",
    val isArchived: Boolean = false
) {
    fun toDomain(): FoodItem = FoodItem(
        id = id,
        name = name,
        nameEn = nameEn,
        quantity = quantity,
        unit = unit,
        expirationDate = expirationDate,
        acquisitionDate = acquisitionDate,
        category = category,
        location = FoodLocation.fromString(location),
        perishability = PerishabilityLevel.fromString(perishability),
        notes = notes,
        isArchived = isArchived
    )

    companion object {
        fun fromDomain(item: FoodItem): FoodItemEntity = FoodItemEntity(
            id = item.id,
            name = item.name,
            nameEn = item.nameEn,
            quantity = item.quantity,
            unit = item.unit,
            expirationDate = item.expirationDate,
            acquisitionDate = item.acquisitionDate,
            category = item.category,
            location = item.location.name,
            perishability = item.perishability.name,
            notes = item.notes,
            isArchived = item.isArchived
        )
    }
}
