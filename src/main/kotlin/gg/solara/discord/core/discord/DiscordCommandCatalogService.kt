package gg.solara.discord.core.discord

import dev.minn.jda.ktx.interactions.commands.slash
import dev.minn.jda.ktx.interactions.commands.updateCommands
import gg.solara.discord.core.utilities.INFO_COLOUR
import gg.solara.discord.core.utilities.logger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service

@Service
class DiscordCommandCatalogService(
    private val discord: JDA
) : InitializingBean
{
    fun updateCommands()
    {
        discord.updateCommands {
            slash(
                name = "close",
                description = "Close an open support ticket!"
            ) {
                defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.MESSAGE_MANAGE)
            }

            slash(
                name = "support-embed",
                description = "Send a support embed panel!"
            ) {
                defaultPermissions = DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
            }
        }.queue {
            logger.info { "${INFO_COLOUR}Updated all commands!" }
        }
    }

    override fun afterPropertiesSet()
    {
        updateCommands()
        discord.presence.activity = Activity.watching("over solara.gg")
    }
}
