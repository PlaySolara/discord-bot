package gg.solara.discord.core.discord

import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.jdabuilder.light
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author GrowlyX
 * @since 6/28/2024
 */
@Configuration
class DiscordConfiguration
{
    @Value("\${solara.discord.token}") lateinit var token: String

    @Bean
    fun discordClient() = light(token, enableCoroutines = true) {
        intents += setOf(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
    }
}
