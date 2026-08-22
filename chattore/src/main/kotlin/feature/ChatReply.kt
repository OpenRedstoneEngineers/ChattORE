package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Syntax
import com.velocitypowered.api.proxy.Player
import org.openredstone.chattore.ChattoreException
import org.openredstone.chattore.Messenger
import org.openredstone.chattore.PluginScope
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class StoredMessage(
    val author: String,
    val content: String,
)

class ChatReply() {
    private val maxMessages = 100
    private val messageIdCounter = AtomicInteger(0)

    private fun <K, V> createMaps(): MutableMap<K, V> = Collections.synchronizedMap(
        object : LinkedHashMap<K, V>(maxMessages) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean =
                size > maxMessages
        }
    )

    private val messages = createMaps<Int, StoredMessage>()
    private val discordMessageIds = createMaps<Long, Int>()

    fun saveMessage(author: String, content: String, discordSnowflake: Long? = null): Int {
        val messageId = messageIdCounter.incrementAndGet()
        if (discordSnowflake != null) discordMessageIds[discordSnowflake] = messageId
        messages[messageId] = StoredMessage(author, content)
        return messageId
    }

    fun getMessage(id: Int): StoredMessage? = messages[id]

    fun getMessageByDiscordSnowflake(discordSnowflake: Long): StoredMessage? =
        discordMessageIds[discordSnowflake]?.let { getMessage(it) }
}

fun PluginScope.createChatReplyFeature(
    messenger: Messenger,
    confirmations: ChatConfirmations,
): ChatReply {
    val chatReply = ChatReply()

    @CommandAlias("chatreply")
    @CommandPermission("chattore.chat")
    class ChatReplyCommand : BaseCommand() {
        @Default
        @Syntax("<id> <message>")
        fun default(
            sender: Player,
            id: Int,
            message: String,
        ) {
            val original = chatReply.getMessage(id) ?: throw ChattoreException("That message is too old!")

            confirmations.submit(sender, message) {
                val newMessageId = chatReply.saveMessage(sender.username, message)
                messenger.broadcastChatMessage(
                    sender,
                    message,
                    newMessageId,
                    reply = original,
                )
            }
        }
    }

    registerCommands(ChatReplyCommand())
    return chatReply
}
