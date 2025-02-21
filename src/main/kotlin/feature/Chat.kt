package chattore.feature

import chattore.*
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.ResultedEvent.GenericResult
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.proxy.Player
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class ChatConfirmationConfig(
    val regexes: List<String> = listOf(),
    val confirmationPrompt: String = "\"<red><bold>The following message was not sent because it contained " +
        "potentially inappropriate language:<newline><reset><message><newline><red>To send this message anyway, run " +
        "<gray>/confirmmessage<red>.\"",
    val chatConfirm: String = "<red>Override recognized"
)

fun createChatFeature(
    logger: Logger,
    messenger: Messenger,
    eventManager: EventManager,
    config: ChatConfirmationConfig,
): Feature {
    val flaggedMessages = ConcurrentHashMap<UUID, String>()
    return Feature(
        commands = listOf(ConfirmMessage(config, flaggedMessages, logger, messenger)),
        listeners = listOf(ChatListener(config, flaggedMessages, logger, messenger, eventManager)),
    )
}

data class ChatEvent(val player: Player, val message: String, private var result: GenericResult) : ResultedEvent<GenericResult> {
    override fun getResult() = result
    override fun setResult(result: GenericResult) {
        this.result = result
    }
}

class ChatListener(
    private val config: ChatConfirmationConfig,
    private val flaggedMessages: ConcurrentHashMap<UUID, String>,
    private val logger: Logger,
    private val messenger: Messenger,
    private val eventManager: EventManager,
) {
    private val regexes = config.regexes.map(::Regex)

    @Subscribe
    fun onChatEvent(event: PlayerChatEvent) {
        eventManager.fire(ChatEvent(event.player, event.message, GenericResult.allowed()))
            .thenAccept { e ->
                if (!e.result.isAllowed) return@thenAccept
                val (player, message) = e
                logger.info("${player.username} (${player.uniqueId}): $message")
                player.currentServer.ifPresent { server ->
                    messenger.broadcastChatMessage(server.serverInfo.name, player, message)
                }
            }
    }

    @Subscribe
    fun onChatEvent(event: ChatEvent) {
        val (player, message) = event
        val matches = regexes.filter { it.containsMatchIn(message) }
        if (matches.isEmpty()) {
            flaggedMessages.remove(player.uniqueId)
            return
        }
        fun String.highlight(r: Regex) = r.replace(this) { match -> "<red>${match.value}</red>" }
        val highlighted = matches.fold(message, String::highlight)
        logger.info("${player.username} (${player.uniqueId}) Attempting to send flagged message: $message")
        player.sendSimpleMM(config.confirmationPrompt, highlighted)
        flaggedMessages[player.uniqueId] = message
        event.result = GenericResult.denied()
    }
}

@CommandAlias("confirmmessage")
@CommandPermission("chattore.confirmmessage")
class ConfirmMessage(
    private val config: ChatConfirmationConfig,
    private val flaggedMessages: ConcurrentHashMap<UUID, String>,
    private val logger: Logger,
    private val messenger: Messenger,
) : BaseCommand() {
    @Default
    fun default(player: Player) {
        val message = flaggedMessages[player.uniqueId] ?:
            throw ChattoreException("You have no message to confirm!")
        player.sendRichMessage(config.chatConfirm)
        flaggedMessages.remove(player.uniqueId)
        logger.info("${player.username} (${player.uniqueId}) FLAGGED MESSAGE OVERRIDE: $message")
        player.currentServer.ifPresent { server ->
            messenger.broadcastChatMessage(server.serverInfo.name, player, message)
        }
    }
}
