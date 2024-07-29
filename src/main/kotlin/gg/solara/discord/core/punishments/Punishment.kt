package gg.solara.discord.core.punishments

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * @author GrowlyX
 * @since 6/21/2024
 */
@Document(collection = "Punishment")
data class Punishment(
    @Id val id: String,
    val addedAt: String,
    val addedBy: String?,
    val addedOn: String,
    val addedReason: String,
    val category: PunishmentCategory,
    val duration: String,
    val expireDate: String,
    val removedAt: String,
    val removedBy: String?,
    val removedOn: String?,
    val removedReason: String?,
    val target: String
)
