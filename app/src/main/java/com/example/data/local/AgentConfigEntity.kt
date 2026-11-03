package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AgentConfig

@Entity(tableName = "agent_config")
data class AgentConfigEntity(
    @PrimaryKey
    val id: Long = 1,
    val wExpiration: Double = 0.55,
    val wPerishability: Double = 0.30,
    val wQuantity: Double = 0.15,
    val highPriorityThreshold: Double = 0.70,
    val mediumPriorityThreshold: Double = 0.45
) {
    fun toDomain(): AgentConfig = AgentConfig(
        id = id,
        wExpiration = wExpiration,
        wPerishability = wPerishability,
        wQuantity = wQuantity,
        highPriorityThreshold = highPriorityThreshold,
        mediumPriorityThreshold = mediumPriorityThreshold
    )

    companion object {
        fun fromDomain(config: AgentConfig): AgentConfigEntity = AgentConfigEntity(
            id = config.id,
            wExpiration = config.wExpiration,
            wPerishability = config.wPerishability,
            wQuantity = config.wQuantity,
            highPriorityThreshold = config.highPriorityThreshold,
            mediumPriorityThreshold = config.mediumPriorityThreshold
        )
    }
}
