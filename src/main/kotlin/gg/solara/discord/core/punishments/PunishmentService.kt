package gg.solara.discord.core.punishments

import org.springframework.stereotype.Service
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author GrowlyX
 * @since 6/21/2024
 */
@Service
class PunishmentService(
    private val punishmentRepository: PunishmentRepository
)
{
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy, hh:mm:ss a")

    fun getAllActivePunishments(player: UUID) = punishmentRepository
        .findAllByTargetEqualsAndRemovedAtEquals(target = player.toString())
        .filter {
            it.category != PunishmentCategory.KICK &&
                dateFormat.parse(it.expireDate).after(Date())
        }
}
