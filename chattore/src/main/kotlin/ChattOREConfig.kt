package org.openredstone.chattore

import org.openredstone.chattore.feature.DiscordConfig
import java.util.*

data class ChattOREConfig(
    val storage: String = "storage.db",
    val clearNicknameOnChange: Boolean = true,
    val regexes: List<String> = emptyList(),

    val discord: DiscordConfig = DiscordConfig(),
    val format: FormatConfig = FormatConfig(),

    val nicknamePresets: SortedMap<String, String> = pridePresets,
)

data class FormatConfig(
    val global: String = "<prefix> <gray>|</gray> <sender><gray>:</gray> <message>",
    val join: String = "<yellow><player> has joined the network",
    val leave: String = "<yellow><player> has left the network",
    val joinDiscord: String = "**<player> has joined the network**",
    val leaveDiscord: String = "**<player> has left the network**",
)
