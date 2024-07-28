package gg.solara.discord.core.utilities

import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * @author GrowlyX
 * @since 6/16/2024
 */

const val NORMAL = 0
const val BRIGHT = 1
const val FOREGROUND_BLACK = 30
const val FOREGROUND_RED = 31
const val FOREGROUND_GREEN = 32
const val FOREGROUND_YELLOW = 33
const val FOREGROUND_BLUE = 34
const val FOREGROUND_MAGENTA = 35
const val FOREGROUND_CYAN = 36
const val FOREGROUND_WHITE = 37
const val PREFIX = "\u001b["
const val SUFFIX = "m"
const val SEPARATOR = ';'
const val END_COLOUR = PREFIX + SUFFIX

const val FATAL_COLOUR = (PREFIX
    + BRIGHT + SEPARATOR + FOREGROUND_RED + SUFFIX)

const val ERROR_COLOUR = (PREFIX
    + NORMAL + SEPARATOR + FOREGROUND_RED + SUFFIX)

const val WARN_COLOUR = (PREFIX
    + NORMAL + SEPARATOR + FOREGROUND_YELLOW + SUFFIX)

const val INFO_COLOUR = (PREFIX
    + BRIGHT + SEPARATOR + FOREGROUND_GREEN + SUFFIX)

const val DEBUG_COLOUR = (PREFIX
    + NORMAL + SEPARATOR + FOREGROUND_CYAN + SUFFIX)

const val TRACE_COLOUR = (PREFIX
    + NORMAL + SEPARATOR + FOREGROUND_BLUE + SUFFIX)

val logger = KotlinLogging.logger {

}
