package gg.solara.discord.core.support.command

import dev.minn.jda.ktx.events.onCommand
import dev.minn.jda.ktx.interactions.components.button
import dev.minn.jda.ktx.messages.MessageCreate
import gg.solara.discord.core.utilities.Colors
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author GrowlyX
 * @since 7/1/2024
 */
@Component
class SupportEmbedCommand : InitializingBean
{
    @Autowired
    lateinit var client: JDA

    override fun afterPropertiesSet()
    {
        client.onCommand("support-embed") { event ->
            if (event.member?.hasPermission(Permission.ADMINISTRATOR) == false)
            {
                event.reply("You do not have permission to perform this command!").queue()
                return@onCommand
            }

            event.reply("Sending message...").setEphemeral(true).queue()
            event.messageChannel.sendMessage(MessageCreate {
                embed {
                    color = Colors.Primary
                    title = "\uD83C\uDF9F\uFE0F Support"
                    description = """
                        Talk to our support team 1-on-1 in a dedicated, private channel.
                    """.trimIndent()
                }

                actionRow(
                    button(
                        "general",
                        emoji = Emoji.fromUnicode("\uD83C\uDFAB"),
                        label = "General",
                        style = ButtonStyle.SUCCESS
                    ),
                    button(
                        "transactions",
                        emoji = Emoji.fromUnicode("\uD83D\uDCB0"),
                        label = "Transactions",
                        style = ButtonStyle.SUCCESS
                    ),
                    button(
                        "punishments",
                        emoji = Emoji.fromUnicode("\uD83D\uDEA8"),
                        label = "Punishments",
                        style = ButtonStyle.SUCCESS
                    )
                )

                actionRow(
                    button(
                        "staff-application",
                        emoji = Emoji.fromUnicode("\uD83D\uDC6E"),
                        label = "Apply for Staff",
                        style = ButtonStyle.PRIMARY
                    ),
                    button(
                        "media-application",
                        emoji = Emoji.fromUnicode("\uD83D\uDCF9"),
                        label = "Apply for Media",
                        style = ButtonStyle.PRIMARY
                    )
                )
            }).queue()
        }
    }
}
