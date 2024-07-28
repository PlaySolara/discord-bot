package gg.solara.discord.core.utilities

import com.twitter.snowflake.Snowflake

/**
 * @author GrowlyX
 * @since 6/16/2024
 */
data class Snowflake(val id: Long)

fun snowflake() = Snowflake.ID.nextId()
