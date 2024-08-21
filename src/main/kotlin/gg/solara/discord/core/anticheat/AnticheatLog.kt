package gg.solara.discord.core.anticheat

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * @author GrowlyX
 * @since 8/21/2024
 */
@Document(collection = "PlayerLogs")
data class AnticheatLog(
    @Id val id: ObjectId,
    val uuid: String,
    val name: String,
    val checkdata: String
)
