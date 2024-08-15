package gg.solara.discord.core.minecraft.datastore

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import gg.scala.aware.Aware
import gg.scala.aware.message.AwareMessage
import gg.scala.store.ScalaDataStoreShared
import gg.scala.store.connection.AbstractDataStoreConnection
import gg.scala.store.connection.mongo.AbstractDataStoreMongoConnection
import gg.scala.store.connection.mongo.impl.UriDataStoreMongoConnection
import gg.scala.store.connection.mongo.impl.details.DataStoreMongoConnectionDetails
import gg.scala.store.connection.redis.AbstractDataStoreRedisConnection
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * @author GrowlyX
 * @since 8/13/2022
 */
@Component
@Order(1000)
class ScalaDataStoreStandaloneImpl(
    private val aware: Aware<AwareMessage>
) : ScalaDataStoreShared(), InitializingBean
{
    @Value("\${spring.data.mongodb.database}") lateinit var mongoDatabase: String
    @Value("\${spring.data.mongodb.uri}") lateinit var mongoURI: String

    override fun getNewRedisConnection(): AbstractDataStoreRedisConnection
    {
        return object : AbstractDataStoreRedisConnection()
        {
            override fun createNewConnection() = aware
        }
    }

    override fun getNewMongoConnection(): AbstractDataStoreMongoConnection
    {
        return UriDataStoreMongoConnection(
            DataStoreMongoConnectionDetails(
                database = mongoDatabase
            ),
            MongoClient(MongoClientURI(mongoURI))
        )
    }

    override fun debug(from: String, message: String)
    {
        AbstractDataStoreConnection.LOGGER.info("$from: $message")
    }

    override fun forceDisableRedisThreshold() = true
    override fun afterPropertiesSet()
    {
        INSTANCE = this
    }
}
