package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.proxy.Player
import org.openredstone.chattore.ChattoreException
import org.openredstone.chattore.Messenger
import org.openredstone.chattore.PluginScope
import org.openredstone.chattore.sendSimpleMM
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class ChatConfirmationConfig(
    val regexes: List<String> = listOf(),
)

fun PluginScope.createChatFeature(
    messenger: Messenger,
    config: ChatConfirmationConfig,
) {
    val flaggedMessages = ConcurrentHashMap<UUID, String>()
    registerCommands(ConfirmMessage(flaggedMessages, logger, messenger))
    registerListeners(ChatListener(config, flaggedMessages, logger, messenger))
}

private class ChatListener(
    config: ChatConfirmationConfig,
    private val flaggedMessages: ConcurrentHashMap<UUID, String>,
    private val logger: Logger,
    private val messenger: Messenger,
) {
    private val regexes = config.regexes.map(::Regex)

    @Subscribe
    fun onChatEvent(event: PlayerChatEvent) {
        val player = event.player
        val message = event.message
        if (isFlagged(player, message)) return
        logger.info("${player.username} (${player.uniqueId}): $message")
        player.currentServer.ifPresent { server ->
            messenger.broadcastChatMessage(server.serverInfo.name, player, message)
        }
    }

    private fun isFlagged(player: Player, message: String): Boolean {
        val matches = regexes.filter { it.containsMatchIn(message) }
        if (matches.isEmpty()) {
            flaggedMessages.remove(player.uniqueId)
            return false
        }
        fun String.highlight(r: Regex) = r.replace(this) { match -> "<red>${match.value}</red>" }
        val highlighted = matches.fold(message, String::highlight)
        logger.info("${player.username} (${player.uniqueId}) Attempting to send flagged message: $message")
        player.sendSimpleMM(
            "<red><bold>The following message was not sent because it contained " +
                "potentially inappropriate language:<newline><reset><message><newline><red>To send this message anyway, run " +
                "<gray>/confirmmessage<red>.",
            highlighted,
        )
        flaggedMessages[player.uniqueId] = message
        return true
    }
}

@CommandAlias("confirmmessage")
@CommandPermission("chattore.confirmmessage")
private class ConfirmMessage(
    private val flaggedMessages: ConcurrentHashMap<UUID, String>,
    private val logger: Logger,
    private val messenger: Messenger,
) : BaseCommand() {
    @Default
    fun default(player: Player) {
        val message = flaggedMessages[player.uniqueId] ?: throw ChattoreException("You have no message to confirm!")
        player.sendRichMessage("<red>Override recognized")
        flaggedMessages.remove(player.uniqueId)
        logger.info("${player.username} (${player.uniqueId}) FLAGGED MESSAGE OVERRIDE: $message")
        player.currentServer.ifPresent { server ->
            messenger.broadcastChatMessage(server.serverInfo.name, player, message)
        }
    }
}
