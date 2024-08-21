package gg.solara.discord.core.anticheat

import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

/**
 * @author GrowlyX
 * @since 8/21/2024
 */
interface AnticheatLogRepository : MongoRepository<AnticheatLog, ObjectId>
{
    fun findAllByUuid(uuid: String): List<AnticheatLog>
}
