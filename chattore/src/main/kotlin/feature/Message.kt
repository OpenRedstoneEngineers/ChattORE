package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import co.aikar.commands.velocity.contexts.OnlinePlayer
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.RED
import org.openredstone.chattore.*
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun PluginScope.createMessageFeature(
    messenger: Messenger,
) {
    val replyMap = ConcurrentHashMap<UUID, UUID>()
    registerCommands(
        Message(logger, messenger, replyMap),
        Reply(proxy, logger, messenger, replyMap),
    )
}

@CommandAlias("m|pm|msg|message|vmsg|vmessage|whisper|tell")
@CommandPermission("chattore.message")
private class Message(
    private val logger: Logger,
    private val messenger: Messenger,
    private val replyMap: ConcurrentHashMap<UUID, UUID>,
) : BaseCommand() {
    @Default
    @Syntax("[target] <message>")
    // unsure if this is needed
    @CommandCompletion("@players")
    fun default(sender: Player, recipient: OnlinePlayer, message: String) {
        sendMessage(logger, messenger, replyMap, sender, recipient.player, message)
    }
}

@CommandAlias("r|reply")
@CommandPermission("chattore.message")
private class Reply(
    private val proxy: ProxyServer,
    private val logger: Logger,
    private val messenger: Messenger,
    private val replyMap: ConcurrentHashMap<UUID, UUID>,
) : BaseCommand() {
    @Default
    fun default(sender: Player, message: String) {
        val recipientUuid = replyMap[sender.uniqueId] ?: throw ChattoreException("You have no one to reply to!")
        val recipient = proxy.playerOrNull(recipientUuid)
            ?: throw ChattoreException("The person you are trying to reply to is no longer online!")
        sendMessage(logger, messenger, replyMap, sender, recipient, message)
    }
}

private fun sendMessage(
    logger: Logger,
    messenger: Messenger,
    replyMap: MutableMap<UUID, UUID>,
    sender: Player,
    recipient: Player,
    message: String,
) {
    logger.info(
        "${sender.username} (${sender.uniqueId}) -> " +
            "${recipient.username} (${recipient.uniqueId}): $message"
    )
    fun prefix(from: String, to: String) = "[".c(GOLD) + from.c(RED) + " -> ".c(GOLD) + to.c(RED) + "] ".c(GOLD)
    val prepared = messenger.prepareChatMessage(message, sender)
    sender.sendMessage(prefix("me", recipient.username) + prepared)
    recipient.sendMessage(prefix(sender.username, "me") + prepared)
    replyMap[recipient.uniqueId] = sender.uniqueId
    replyMap[sender.uniqueId] = recipient.uniqueId
}
