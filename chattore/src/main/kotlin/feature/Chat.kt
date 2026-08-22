package org.openredstone.chattore.feature

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.PlayerChatEvent
import org.openredstone.chattore.Messenger
import org.openredstone.chattore.PluginScope
import org.openredstone.chattore.sendError

fun PluginScope.createChatFeature(
    messenger: Messenger,
    confirmations: ChatConfirmations,
    bubbleManager: BubbleManager,
    chatReply: ChatReply,
) {
    registerListeners(ChatListener(confirmations, messenger, bubbleManager, chatReply))
}

private class ChatListener(
    private val confirmations: ChatConfirmations,
    private val messenger: Messenger,
    private val bubbleManager: BubbleManager,
    private val chatReply: ChatReply,
) {
    @Subscribe
    fun onChatEvent(event: PlayerChatEvent) {
        val player = event.player
        val message = event.message
        val bubble = bubbleManager.getBubbleByPlayer(player)
        if (bubble == null) {
            confirmations.submit(player, message) { player ->
                val messageId = chatReply.saveMessage(player.username, message)
                messenger.broadcastChatMessage(player, message, messageId)
            }
            return
        }
        confirmations.submit(player, message) { player ->
            if (bubbleManager.getBubbleByPlayer(player) != bubble) {
                player.sendError("You are no longer in the bubble you're trying to send a message to")
                return@submit
            }
            messenger.broadcastBubbleMessage(player, message, bubble)
        }
    }
}
