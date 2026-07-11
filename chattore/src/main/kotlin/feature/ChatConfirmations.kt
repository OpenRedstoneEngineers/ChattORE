package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.velocitypowered.api.proxy.Player
import org.openredstone.chattore.ChattoreException
import org.openredstone.chattore.PluginScope
import org.openredstone.chattore.sendSimpleMM
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class ChatConfirmationConfig(
    val regexes: List<String> = listOf(),
)

fun PluginScope.createChatConfirmations(config: ChatConfirmationConfig): ChatConfirmations =
    ChatConfirmations(config, logger).also { registerCommands(it.ConfirmMessage()) }

class ChatConfirmations(
    config: ChatConfirmationConfig,
    private val logger: Logger,
) {
    private val regexes = config.regexes.mapNotNull { pattern ->
        runCatching { Regex(pattern, RegexOption.IGNORE_CASE) }
            .onFailure { logger.error("Invalid regex $pattern: ${it.message}") }
            .getOrNull()
    }
    private val flags = ConcurrentHashMap<UUID, Pair<String, (Player) -> Unit>>()

    /**
     * Submit [message] for flagging. [proceed] will be called with an up-to-date instance of [player]
     * upon confirmation or if no flagging happens. This is for the rare case when [player] disconnects
     * after submit is called and joins back to do /confirmmessage. In that case, the Player object is invalidated
     * due to disconnecting, so we can supply back a fresh one from the /confirmmessage event handler in order to
     * simplify calls to [submit]. The recommended way to call this is to shadow the variable for [player] in the
     * [proceed] lambda's argument. See: literally any call to this function.
     */
    fun submit(player: Player, message: String, proceed: (Player) -> Unit) {
        val matches = regexes.filter { it.containsMatchIn(message) }
        if (matches.isEmpty()) {
            flags.remove(player.uniqueId)
            proceed(player)
            return
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
        flags[player.uniqueId] = message to proceed
        return
    }

    @CommandAlias("confirmmessage")
    @CommandPermission("chattore.confirmmessage")
    inner class ConfirmMessage : BaseCommand() {
        @Default
        fun default(player: Player) {
            val (message, proceed) = flags[player.uniqueId]
                ?: throw ChattoreException("You have no message to confirm!")
            player.sendRichMessage("<red>Override recognized")
            flags.remove(player.uniqueId)
            logger.info("${player.username} (${player.uniqueId}) FLAGGED MESSAGE OVERRIDE: $message")
            proceed(player)
        }
    }
}
