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
import java.util.concurrent.atomic.AtomicInteger
import java.util.LinkedHashMap

data class StoredMessage(
    val author: String,
    val content: String,
)

object ChatReply {
    private const val maxMessages = 100

    private val messageIdCounter = AtomicInteger(0)

    private val messages: MutableMap<Int, StoredMessage> = Collections.synchronizedMap(
        object : LinkedHashMap<Int, StoredMessage>(maxMessages, 0.75f, false) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, StoredMessage>?): Boolean =
                size > maxMessages
        }
    )

    fun nextMessageId(): Int = messageIdCounter.incrementAndGet()

    fun saveMessage(id: Int, author: String, content: String) {
        messages[id] = StoredMessage(author, content)
    }

    fun getMessage(id: Int): StoredMessage? = messages[id]
}

fun PluginScope.createChatReplyFeature(messenger: Messenger) {
    @CommandAlias("chatreply")
    @CommandPermission("chattore.chat")
    class ChatReplyCommand : BaseCommand() {
        @Default
        @Syntax("<id> <message>")
        fun default(sender: Player, id: Int, message: String) {
            val original = ChatReply.getMessage(id)
                ?: throw ChattoreException("That message is too old!")
            messenger.broadcastChatMessage(
                sender,
                message,
                replyAuthor = original.author,
                replyContent = original.content,
            )
        }
    }

    registerCommands(ChatReplyCommand())
}