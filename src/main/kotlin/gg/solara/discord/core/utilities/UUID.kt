package gg.solara.discord.core.utilities

import java.util.*
import java.util.regex.Pattern

/**
 * @author GrowlyX
 * @since 6/19/2024
 */
private val pattern = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})")

fun String.toUuidDashed() = UUID.fromString(
    pattern.matcher(this).replaceAll("$1-$2-$3-$4-$5")
)

fun String.toUuid() = UUID.fromString(this)
