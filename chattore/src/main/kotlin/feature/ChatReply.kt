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

class ChatReply(
    private val confirmations: ChatConfirmations,
) {
    private val maxMessages = 100
    private val messageIdCounter = AtomicInteger(0)

    private val messages: MutableMap<Int, StoredMessage> = Collections.synchronizedMap(
        object : LinkedHashMap<Int, StoredMessage>(maxMessages) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, StoredMessage>?): Boolean =
                size > maxMessages
        }
    )

    private val discordMessageIds: MutableMap<Long, Int> = ConcurrentHashMap()

    fun saveMessage(author: String, content: String): Int {
        val messageId = messageIdCounter.incrementAndGet()
        messages[messageId] = StoredMessage(author, content)
        return messageId
    }

    fun getMessage(id: Int): StoredMessage? = messages[id]

    fun linkDiscordMessage(discordSnowflake: Long, messageId: Int) {
        discordMessageIds[discordSnowflake] = messageId
    }

    fun getMessageByDiscordSnowflake(discordSnowflake: Long): StoredMessage? =
        discordMessageIds[discordSnowflake]?.let { getMessage(it) }

    fun reply(
        sender: Player,
        messenger: Messenger,
        id: Int,
        message: String,
    ) {
        val original = getMessage(id) ?: throw ChattoreException("That message is too old!")

        confirmations.submit(sender, message) {
            val newMessageId = saveMessage(sender.username, message)
            messenger.broadcastChatMessage(
                sender,
                message,
                newMessageId,
                reply = original,
            )
        }
    }
}

fun PluginScope.createChatReplyFeature(
    messenger: Messenger,
    confirmations: ChatConfirmations,
): ChatReply {
    val chatReply = ChatReply(confirmations)

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
            chatReply.reply(
                sender = sender,
                messenger = messenger,
                id = id,
                message = message,
            )
        }
    }

    registerCommands(ChatReplyCommand())
    return chatReply
}