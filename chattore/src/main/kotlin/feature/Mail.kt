package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
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
            return "Invalid page requested".toComponent()
        }
        val pageStart = page * pageSize
        val requestedMessages = messages.subList(pageStart, min(messages.size, pageStart + pageSize))
        var body = "<red>Mailbox, page $page</red><newline><gold>ID: Sender Timestamp".render()
        requestedMessages.forEach {
            val mini =
                "<newline><yellow><hover:show_text:'<red>Click to read'><click:run_command:/mail read ${it.id}>" +
                    "From: <gold><sender></gold>, <timestamp></click></hover> (<read>)</yellow>"
            val readComponent = if (!it.read) {
                "<b><red>Unread</red></b>".render()
            } else {
                "<i><yellow>Read</yellow></i>".render()
            }
            val item = mini.render(
                "sender" toS userCache.usernameOrUuid(it.sender),
                "timestamp" toS getRelativeTimestamp(it.timestamp.toLong()),
                "read" toC readComponent,
            )
            body = body.append(item)
        }
        if (maxPage > 0) {
            var pageMini = "<newline>"
            pageMini += if (page == 0) {
                "<red><hover:show_text:'<red>No previous page'>\uD83D\uDEAB</hover></red>"
            } else {
                "<red><hover:show_text:'<red>Previous page'><click:run_command:/mailbox ${page - 1}>←</click></hover></red>"
            }
            pageMini += " <yellow>|<yellow> "
            pageMini += if (page == maxPage) {
                "<red><hover:show_text:'<red>No next page'>\uD83D\uDEAB</hover></red>"
            } else {
                "<red><hover:show_text:'<red>Next page'><click:run_command:/mailbox ${page + 1}>→</click></hover></red>"
            }
            body = body.append(pageMini.render())
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
        player.sendRichMessage(
            "<gold>[</gold><red>To <recipient></red><gold>]</gold> <message>",
            "message" toS message,
            "recipient" toS target,
        )
    }

    @Subcommand("read")
    fun read(player: Player, id: Int) {
        val (senderUUID, message) = database.readMessage(player.uniqueId, id)
            ?: throw ChattoreException("Invalid message ID!")
        player.sendRichMessage(
            "<gold>[</gold><red>From <sender></red><gold>]</gold> <message>",
            "message" toS message,
            "sender" toS userCache.usernameOrUuid(senderUUID),
        )
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
                event.player.sendRichMessage(
                    "<yellow>You have <red><count></red> unread message(s)! <gold><b><hover:show_text:'View your mailbox'><click:run_command:'/mail mailbox'>Click here to view</click></hover></b></gold>.",
                    "count" toS unreadCount.toString(),
                )
            }).delay(2L, TimeUnit.SECONDS).schedule()
    }
}
