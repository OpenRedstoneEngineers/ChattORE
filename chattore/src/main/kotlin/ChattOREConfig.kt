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
    val chatMessage: String = "<prefix> <gray>|</gray> <sender><gray>:</gray> <message>",
    val join: String = "<yellow><player> has joined the network",
    val leave: String = "<yellow><player> has left the network",
    val bubblePrefix: String = "<bubble_info>\uD83D\uDCAC</bubble_info>",
    val spyPrefix: String = "<hover:show_text:'Spy'>\uD83D\uDD75</hover>",
    val shoutPrefix: String = "<hover:show_text:'You've disabled global chat in bubbles. You won't see if anyone responds.'>\uD83D\uDD15</hover>",
    val joinDiscord: String = "**%player% has joined the network**",
    val leaveDiscord: String = "**%player% has left the network**",
)
