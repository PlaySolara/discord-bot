package gg.solara.discord.core.minecraft

import gg.scala.store.controller.DataStoreObjectControllerCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author GrowlyX
 * @since 8/21/2022
 */
@Configuration
class DiscordProfileConfiguration
{
    @Bean
    fun discordProfileController() = DataStoreObjectControllerCache.create<DiscordProfile>()
}
