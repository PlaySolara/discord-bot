package gg.solara.discord.core.minecraft

import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.scala.store.storage.storable.IDataStoreObject
import gg.scala.store.storage.type.DataStoreStorageType
import java.util.*

/**
 * @author GrowlyX
 * @since 8/21/2022
 */
data class DiscordProfile(
    override val identifier: UUID,
    var discordId: Long? = null,
    var lastCachedDiscordUsername: String? = null,
    var notifiedOfSuccessfulLink: Boolean = false,
    var claimedInitialLinkAward: Boolean = false
) : IDataStoreObject
{
    fun save() = DataStoreObjectControllerCache
        .findNotNull<DiscordProfile>()
        .save(this, DataStoreStorageType.MONGO)
}
