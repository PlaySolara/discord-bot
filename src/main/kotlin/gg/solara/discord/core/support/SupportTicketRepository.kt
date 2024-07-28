package gg.solara.discord.core.support

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * @author GrowlyX
 * @since 7/1/2024
 */
interface SupportTicketRepository : MongoRepository<SupportTicket, String>
{
    fun findAllByOwnerID(ownerId: Long): List<SupportTicket>
    fun findByChannelID(channelID: Long): SupportTicket?
}
