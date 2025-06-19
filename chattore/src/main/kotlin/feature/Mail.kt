package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor.*
import net.kyori.adventure.text.format.TextDecoration.BOLD
import net.kyori.adventure.text.format.TextDecoration.ITALIC
import org.openredstone.chattore.*
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min

fun PluginScope.createMailFeature(
    database: Storage,
    userCache: UserCache,
) {
    registerCommands(Mail(database, userCache))
    registerListeners(MailListener(plugin, database, proxy))
}

private fun getRelativeTimestamp(unixTimestamp: Long): String {
    val currentTime = LocalDateTime.now(ZoneOffset.UTC)
    val eventTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneOffset.UTC)

    val difference = ChronoUnit.MINUTES.between(eventTime, currentTime)

    return when {
        difference < 1 -> "just now"
        difference < 60 -> "$difference minutes ago"
        difference < 120 -> "an hour ago"
        difference < 1440 -> "${difference / 60} hours ago"
        else -> "${difference / 1440} days ago"
    }
}

data class MailboxItem(
    val id: Int,
    val timestamp: Int,
    val sender: UUID,
    val read: Boolean,
)

private class MailContainer(private val userCache: UserCache, private val messages: List<MailboxItem>) {
    private val pageSize = 6
    fun getPage(page: Int = 0): Component {
        val maxPage = messages.size / pageSize
        if (page > maxPage || page < 0) {
            return "Invalid page requested".c
        }
        val pageStart = page * pageSize
        val requestedMessages = messages.subList(pageStart, min(messages.size, pageStart + pageSize))
        var body = "Mailbox, page $page"[RED] + Component.newline() + "ID: Sender Timestamp"[GOLD]
        requestedMessages.forEach {
            val readComponent = if (it.read) {
                "Read"[ITALIC + YELLOW]
            } else {
                "Unread"[BOLD + RED]
            }
            val sender = userCache.usernameOrUuid(it.sender)
            val timestamp = getRelativeTimestamp(it.timestamp.toLong())
            val btnRead = Buttons.run("Click to read"[RED], "/mail read ${it.id}")
            val item = ("From: "[YELLOW] + sender[GOLD] + ", $timestamp"[YELLOW])[btnRead] +
                " ("[YELLOW] + readComponent + ")"[YELLOW]
            body = body.append(Component.newline() + item)
        }
        if (maxPage > 0) {
            var pageCompo = Component.newline() as Component
            pageCompo += if (page == 0) {
                "\uD83D\uDEAB"[RED + HoverEvent.showText("No previous page"[RED])]
            } else {
                "←"[RED + Buttons.run("Previous page"[RED], "/mailbox ${page - 1}")]
            }
            pageCompo += " | "[YELLOW]
            pageCompo += if (page == maxPage) {
                "\uD83D\uDEAB"[RED + HoverEvent.showText("No next page"[RED])]
            } else {
                "→"[RED + Buttons.run("Next page"[RED], "/mailbox ${page + 1}")]
            }
            body = body.append(pageCompo)
        }
        return body
    }
}

@CommandAlias("mail")
@Description("Send a message to an offline player")
@CommandPermission("chattore.mail")
private class Mail(
    private val database: Storage,
    private val userCache: UserCache,
) : BaseCommand() {
    private val mailTimeouts = mutableMapOf<UUID, Long>()

    @Default
    @CatchUnknown
    @CommandAlias("mailbox")
    @Subcommand("mailbox")
    fun mailbox(player: Player, @Default("0") page: Int) {
        val container = MailContainer(
            userCache,
            database.getMessages(player.uniqueId)
        )
        player.sendMessage(container.getPage(page))
    }

    @Subcommand("send")
    @CommandCompletion("@${UserCache.COMPLETION_USERNAMES}")
    fun send(player: Player, @Single target: String, message: String) {
        val now = System.currentTimeMillis().floorDiv(1000)
        mailTimeouts[player.uniqueId]?.let {
            // 60 second timeout to prevent flooding
            if (now < it + 60) throw ChattoreException("You are mailing too quickly!")
        }
        val targetUuid = userCache.uuidOrNull(target)
            ?: throw ChattoreException("We do not recognize that user!")
        mailTimeouts[player.uniqueId] = now
        database.insertMessage(player.uniqueId, targetUuid, message)
        player.sendMessage("["[GOLD] + "To $target"[RED] + "] "[GOLD] + message.c)
    }

    @Subcommand("read")
    fun read(player: Player, id: Int) {
        val (senderUUID, message) = database.readMessage(player.uniqueId, id)
            ?: throw ChattoreException("Invalid message ID!")
        val sender = userCache.usernameOrUuid(senderUUID)
        player.sendMessage("["[GOLD] + "From $sender"[RED] + "] "[GOLD] + message.c)
    }
}

private class MailListener(
    private val plugin: Any,
    private val database: Storage,
    private val proxy: ProxyServer,
) {
    @Subscribe
    fun joinEvent(event: LoginEvent) {
        val unreadCount = database.getMessages(event.player.uniqueId).filter { !it.read }.size
        if (unreadCount > 0)
            proxy.scheduler.buildTask(plugin, Runnable {
                event.player.sendMessage(
                    "You have "[YELLOW] + "$unreadCount"[RED] + " unread message(s)! "[YELLOW]
                        + "Click here to view"[GOLD + BOLD + Buttons.run("View your mailbox".c, "/mail mailbox")]
                )
            }).delay(2L, TimeUnit.SECONDS).schedule()
    }
}
