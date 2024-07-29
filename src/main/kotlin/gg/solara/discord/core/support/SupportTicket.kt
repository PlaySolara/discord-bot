package gg.solara.discord.core.support

import gg.solara.discord.core.utilities.snowflake
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * @author GrowlyX
 * @since 7/1/2024
 */
@Document(collection = "SolaraSupportTickets")
data class SupportTicket(
    @Id val id: Long = snowflake(),
    @Indexed val channelID: Long,
    @Indexed val ownerID: Long,
    val roleIDs: String,
    @Indexed var assignedToUser: Long? = null
)
