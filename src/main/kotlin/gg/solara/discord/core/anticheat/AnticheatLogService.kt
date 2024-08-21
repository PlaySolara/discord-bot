package gg.solara.discord.core.anticheat

import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.MessageCreate
import gg.solara.discord.core.utilities.Colors
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.beans.factory.InitializingBean
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.*

/**
 * @author GrowlyX
 * @since 8/21/2024
 */
@Service
class AnticheatLogService(
    private val anticheatLogRepository: AnticheatLogRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val discord: JDA
) : InitializingBean
{
    override fun afterPropertiesSet()
    {
        discord.onCommand("anticheatlogs") { event ->
            if (event.member?.hasPermission(Permission.ADMINISTRATOR) == false)
            {
                event.reply("You do not have permission to perform this command!").queue()
                return@onCommand
            }

            event.deferReply(true).queue()

            val username = event.getOption("player")?.asString ?: "none"
            val uniqueId = redisTemplate.opsForHash<String, String>()
                .get(
                    "DataStore:UuidCache:Username",
                    username.lowercase()
                )

            if (uniqueId == null)
            {
                event.hook.sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "No Player"
                    description = "This player has never joined the server!"
                }).queue()
                return@onCommand
            }

            val playerLogs = anticheatLogRepository.findAllByUuid(uniqueId)
            if (playerLogs.isEmpty())
            {
                event.hook.sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "No Logs"
                    description = "This player has no logs!"
                }).queue()
                return@onCommand
            }

            var accumulator = """
                Anti Cheat logs for $username
                Generated on ${Date()}
                Total logs: ${playerLogs.size}
                --
            """.trimIndent()

            playerLogs.forEach { log ->
                accumulator += "\n${log.checkdata}"
            }

            event.hook.sendMessage(MessageCreate {
                embed {
                    color = Colors.Success
                    title = "Anti Cheat Logs"
                    description = """
                        Total logs: ${playerLogs.size}
                    """.trimIndent()
                }

                files += FileUpload.fromData(
                    accumulator.encodeToByteArray(),
                    "anticheat-logs-$username.log"
                )
            }).queue()
        }
    }
}
