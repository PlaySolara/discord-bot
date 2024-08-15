package gg.solara.discord.core.minecraft.command

import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.messages.Embed
import gg.scala.aware.Aware
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.type.DataStoreStorageType
import gg.solara.discord.core.minecraft.DiscordProfile
import gg.solara.discord.core.utilities.Colors
import net.dv8tion.jda.api.JDA
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.util.*

/**
 * @author GrowlyX
 * @since 7/1/2024
 */
@Component
class LinkCommand(
    private val client: JDA,
    private val redisAware: Aware<AwareMessage>,
    private val redisTemplate: RedisTemplate<String, String>
) : InitializingBean
{
    @Value("\${solara.discord.link.role}") lateinit var roleID: String

    override fun afterPropertiesSet()
    {
        client.onCommand("link") { event ->
            val code = event.getOption("code")?.asString
                ?: return@onCommand

            event.deferReply(true).queue()

            val matching = redisTemplate.opsForValue()
                .get(
                    "discord-sync-codes:codes:$code"
                )

            val uniqueId = UUID.fromString(matching)

            val discordProfile = DataStoreObjectControllerCache
                .findNotNull<DiscordProfile>()
                .load(uniqueId, DataStoreStorageType.MONGO)
                .join()

            if (discordProfile == null)
            {
                event.hook.sendMessageEmbeds(Embed {
                    color = Colors.Failure
                    title = "Internal System Error"
                    description = "We were unable to link your account. You have no in-game profile for your Discord account."
                }).queue()
                return@onCommand
            }

            val privateMember = event.user

            discordProfile.discordId = privateMember.idLong
            discordProfile.lastCachedDiscordUsername = privateMember.name
            discordProfile.save().join()

            redisTemplate.opsForValue().apply {
                getAndDelete("discord-sync-codes:codes:$code")
                getAndDelete("discord-sync-codes:accounts:$uniqueId")
            }

            AwareMessage.of(
                "linked",
                redisAware,
                "player-uuid" to uniqueId
            ).publish(
                AwareThreadContext.SYNC
            )

            val username = redisTemplate.opsForHash<String, String>()
                .get(
                    "DataStore:UuidCache:UUID",
                    discordProfile.identifier.toString()
                )
                ?: "N/A"

            runCatching {
                event.guild!!
                    .addRoleToMember(
                        event.member!!,
                        event.guild!!.getRoleById(roleID)!!
                    )
                    .queue()

                if (username != "N/A")
                {
                    event.member!!
                        .modifyNickname(username)
                        .queue()
                }
            }

            event.hook.sendMessageEmbeds(Embed {
                color = Colors.Success
                title = "Discord Linked"
                description = "We've successfully linked your Discord account to the Minecraft username: `${username}`!"
            }).queue()
        }
    }
}
