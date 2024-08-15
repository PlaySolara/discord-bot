package gg.solara.discord.core.redis

import gg.scala.aware.Aware
import gg.scala.aware.AwareBuilder
import gg.scala.aware.AwareHub
import gg.scala.aware.codec.codecs.interpretation.AwareMessageCodec
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.uri.WrappedAwareUri
import net.evilblock.cubed.serializers.Serializers
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 7/28/2024
 */
@Configuration
class RedisConfiguration
{
    @Value("\${solara.redis.host}") lateinit var redisHost: String
    @Value("\${solara.redis.port}") var redisPort: Int? = null

    @Bean
    fun redisAware(): Aware<AwareMessage>
    {
        AwareHub.configure(
            WrappedAwareUri(address = redisHost, port = redisPort ?: 6379, password = null)
        ) {
            Serializers.gson
        }

        return AwareBuilder
            .of<AwareMessage>("discord")
            .codec(AwareMessageCodec)
            .logger(Logger.getGlobal())
            .build()
            .apply {
                connect().toCompletableFuture().join()
            }
    }

    @Bean
    fun lettuceConnectionFactory(): LettuceConnectionFactory
    {
        return LettuceConnectionFactory().apply {
            standaloneConfiguration.hostName = redisHost
            standaloneConfiguration.port = redisPort ?: 6379
        }
    }

    @Bean
    fun redisTemplate(): RedisTemplate<String, Any>
    {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = lettuceConnectionFactory()
        return template
    }
}
