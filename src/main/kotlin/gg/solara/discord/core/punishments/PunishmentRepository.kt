package gg.solara.discord.core.punishments

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * @author GrowlyX
 * @since 6/21/2024
 */
interface PunishmentRepository : MongoRepository<Punishment, String>
{
    fun findAllByTargetEqualsAndRemovedAtEquals(
        target: String,
        removedAt: String = "-1"
    ): List<Punishment>

    fun findByIdStartingWith(id: String): Punishment?
}
