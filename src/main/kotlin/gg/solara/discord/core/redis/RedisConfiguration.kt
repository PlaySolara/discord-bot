package gg.solara.discord.core.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

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
