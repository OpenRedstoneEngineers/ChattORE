package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.annotation.*
import co.aikar.commands.velocity.contexts.OnlinePlayer
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.*
import net.kyori.adventure.text.event.ClickEvent.runCommand
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.event.HoverEvent.showText
import org.openredstone.chattore.*
import java.util.*
import kotlin.jvm.optionals.getOrNull

private val ShowGlobalChatInBubble = Setting("showGlobalChatInBubble", default = false)
private const val BUBBLE_OWNED = "bubbleOwned"

fun PluginScope.createBubbleFeature(
    messenger: Messenger,
    database: Storage,
    chatConfirmations: ChatConfirmations,
    formatConfig: FormatConfig,
    userCache: UserCache,
): BubbleManager {
    val bubbleManager = BubbleManager()
    commandManager.apply {
        commandContexts.registerIssuerOnlyContext(Bubble::class.java) { ctx ->
            val sender = ctx.sender as? Player
                ?: throw InvalidCommandArgument("This command can only be used by players!", false)
            val bubble = bubbleManager.getBubbleByPlayer(sender)
                ?: throw InvalidCommandArgument("You are not in a bubble!", false)
            if (ctx.hasFlag(BUBBLE_OWNED) && bubble.owner != sender.uniqueId) {
                val ownerName = proxy.playerOrNull(bubble.owner)?.username ?: bubble.owner.toString()
                throw InvalidCommandArgument(
                    "You must be the owner of the bubble to use this command. (Current owner: $ownerName)",
                    false,
                )
            }
            bubble
        }
        commandContexts.registerContext(Array<Player>::class.java) { ctx ->
            // distinct() is called here already so that sendError is not called more than once per string.
            // If this is upgraded to do fuzzy matching, then something different is needed.
            ctx.args.map(String::lowercase).distinct()
                .mapNotNull { arg ->
                    proxy.getPlayer(arg).getOrNull() ?: run {
                        ctx.sender.sendError("Player $arg is not online!")
                        null
                    }
                }
                .toTypedArray()
        }
        commandCompletions.setDefaultCompletion("players", Array<Player>::class.java)
    }
    registerCommands(
        BubbleCommand(
            messenger,
            proxy,
            database,
            bubbleManager,
            chatConfirmations,
            formatConfig,
            userCache,
        ),
    )
    return bubbleManager
}

@CommandAlias("bubble|bb")
@CommandPermission("chattore.bubble")
private class BubbleCommand(
    private val messenger: Messenger,
    private val proxy: ProxyServer,
    private val database: Storage,
    private val bubbleManager: BubbleManager,
    private val chatConfirmations: ChatConfirmations,
    private val formatConfig: FormatConfig,
    private val userCache: UserCache,
) : BaseCommand() {

    @CatchUnknown
    @HelpCommand
    @Subcommand("help")
    fun help(sender: CommandSource, help: CommandHelp) {
        sender.sendInfoMM("<b><gold>Bubble help")
        help.showHelp()
    }

    // NOTE: for some reason Array<Player> still requires the explicit CommandCompletion annotation
    @Subcommand("create|blow")
    @Description("Create (\"blow\") a bubble and invite players")
    @CommandCompletion("@players")
    fun create(sender: Player, @ConsumesRest players: Array<Player>) {
        if (bubbleManager.getBubbleByPlayer(sender) != null)
            throw ChattoreException("You are already in a bubble!")
        val bubble = bubbleManager.createBubble(sender.uniqueId)
        addExcluded(sender.uniqueId)
        sender.sendInfo("Bubble created.")
        sendInvites(sender, bubble, players)
    }

    @Subcommand("invite")
    @Description("Invite players to your bubble (if it is private)")
    @CommandCompletion("@players")
    fun invite(sender: Player, bubble: Bubble, @ConsumesRest targets: Array<Player>) {
        if (!bubble.isPrivate)
            throw ChattoreException("Your bubble is public, anyone can join without invitation.")
        if (targets.isEmpty())
            throw ChattoreException("Please specify one or more players to invite.")
        sendInvites(sender, bubble, targets)
    }

    private fun sendInvites(sender: Player, bubble: Bubble, targets: Array<Player>) {
        for (player in targets) {
            try {
                doInvite(sender, bubble, player)
            } catch (e: ChattoreException) {
                sender.sendError(e.message ?: throw e)
            }
        }
    }

    private fun doInvite(sender: Player, bubble: Bubble, player: Player) {
        if (sender == player)
            throw ChattoreException("You cannot invite yourself!")
        if (player.uniqueId in bubble.invitedPlayers)
            throw ChattoreException("${player.username} is already invited!")
        if (player.uniqueId in bubble.players)
            throw ChattoreException("${player.username} is already in the bubble!")

        bubble.invitedPlayers.add(player.uniqueId)
        sender.sendInfo("Invited ${player.username}.")
        player.sendInfoC(
            textOfChildren(
                text("${sender.username} invited you to their bubble. "),
                bubble.joinButton(userCache),
                newline(),
                text("Currently in the bubble: ${bubble.playersString(userCache)}"),
            ),
        )
    }

    @Subcommand("join")
    @Description("Join a player's bubble")
    fun join(sender: Player, target: OnlinePlayer) {
        if (bubbleManager.getBubbleByPlayer(sender) != null)
            throw ChattoreException("You are already in a bubble!")

        val player = target.player
        val bubble = bubbleManager.getBubbleByPlayer(player)
            ?: throw ChattoreException("${player.username} is not in a bubble!")

        if (bubble.isPrivate && sender.uniqueId !in bubble.invitedPlayers && !sender.hasBubblePrivilege)
            throw ChattoreException("You are not invited to the bubble!")

        bubble.invitedPlayers.remove(sender.uniqueId)
        bubble.players.add(sender.uniqueId)
        addExcluded(sender.uniqueId)
        bubble.sendInfos(
            sender,
            "You joined ${player.username}'s bubble.",
            "${sender.username} joined the bubble.",
        )
    }

    @Subcommand("leave")
    @Description("Leave your current bubble")
    fun leave(sender: Player, bubble: Bubble) {
        bubble.players.remove(sender.uniqueId)
        messenger.excludedFromGlobalChat.remove(sender.uniqueId)
        sender.sendInfo("You left the bubble.")
        if (bubble.players.isEmpty()) {
            bubbleManager.removeBubble(bubble)
            return
        }
        bubble.broadcastInfo("${sender.username} left the bubble.")
        if (bubble.owner == sender.uniqueId) {
            val newOwner = bubble.players.first()
            bubble.owner = newOwner
            proxy.playerOrNull(newOwner)?.sendInfo("You are now the owner of the bubble.")
        }
    }

    @Subcommand("delete|pop")
    @Description("Delete (\"pop\") your own bubble")
    fun delete(sender: Player, @Flags(BUBBLE_OWNED) bubble: Bubble) {
        bubble.sendInfos(
            sender,
            "You popped the bubble.",
            "${sender.username} popped the bubble.",
        )
        messenger.excludedFromGlobalChat.removeAll(bubble.players)
        bubbleManager.removeBubble(bubble)
    }

    @Subcommand("kick")
    @Description("Kick a player from your own bubble")
    fun kick(sender: Player, @Flags(BUBBLE_OWNED) bubble: Bubble, target: OnlinePlayer) {
        val player = target.player
        if (player.uniqueId == sender.uniqueId)
            throw ChattoreException("You cannot kick yourself!")

        bubble.players.remove(player.uniqueId)
        messenger.excludedFromGlobalChat.remove(player.uniqueId)
        player.sendInfo("You were kicked out of the bubble.")
        bubble.sendInfos(
            sender,
            "You kicked ${player.username} from the bubble.",
            "${sender.username} kicked ${player.username} from the bubble.",
        )
    }

    @Subcommand("setprivate")
    @Description("Set the visibility of your own bubble")
    fun setPrivate(sender: Player, @Flags(BUBBLE_OWNED) bubble: Bubble, isPrivate: Boolean) {
        bubble.isPrivate = isPrivate
        val visibility = if (isPrivate) "private" else "public"
        bubble.sendInfos(
            sender,
            "The bubble is now $visibility.",
            "${sender.username} set the bubble to $visibility.",
        )
    }

    private fun Player.canSee(bubble: Bubble) = hasBubblePrivilege
        || !bubble.isPrivate || uniqueId in bubble.players || uniqueId in bubble.invitedPlayers

    private val Player.hasBubblePrivilege: Boolean get() = hasPermission("chattore.bubble.manage")

    @Subcommand("list")
    @Description("List all bubbles")
    fun list(sender: Player) {
        val bubbles = bubbleManager.bubbles.filter { sender.canSee(it) }
        if (bubbles.isEmpty()) {
            sender.sendInfo("There are currently no bubbles.")
            return
        }
        sender.sendRichMessage("<yellow>Bubbles:</yellow>")
        for (bubble in bubbles) {
            val info = if (sender.uniqueId in bubble.players) {
                "<gold>Your current bubble".render()
            } else {
                bubble.joinButton(userCache)
            }
            sender.sendMessage(
                textOfChildren(
                    text(bubble.playersString(userCache)),
                    " <gray>|</gray> ".render(),
                    info,
                ),
            )
        }
    }

    @Subcommand("burst")
    @CommandPermission("chattore.bubble.manage")
    @Description("Burst (delete) someone's bubble")
    fun burst(sender: Player, target: OnlinePlayer) {
        val player = target.player
        val bubble = bubbleManager.getBubbleByPlayer(player)
            ?: throw ChattoreException("${player.username} is not in a bubble!")
        bubble.broadcastInfo("The bubble was burst by ${sender.username}.")
        messenger.excludedFromGlobalChat.removeAll(bubble.players)
        bubbleManager.removeBubble(bubble)
        sender.sendInfo("You burst ${player.username}'s bubble.")
    }

    @Subcommand("showglobalchat|sgc")
    @Description("Control the visibility of global chat when in a bubble")
    fun showGlobalChat(sender: Player, showGlobalChat: Boolean) {
        database.setSetting(ShowGlobalChatInBubble, sender.uniqueId, showGlobalChat)
        if (showGlobalChat) {
            messenger.excludedFromGlobalChat.remove(sender.uniqueId)
        } else if (bubbleManager.getBubbleByPlayer(sender) != null) {
            messenger.excludedFromGlobalChat.add(sender.uniqueId)
        }
        sender.sendInfo(
            if (showGlobalChat) "You will now see global chat in bubbles."
            else "You will no longer see global chat in bubbles.",
        )
    }

    @CommandAlias("shout")
    @Description("Send a message to global chat when in a bubble")
    fun shout(sender: Player, message: String) {
        chatConfirmations.submit(sender, message) { sender ->
            messenger.broadcastChatMessage(sender, message)
            if (sender.uniqueId in messenger.excludedFromGlobalChat) {
                sender.sendMessage(
                    textOfChildren(
                        formatConfig.shoutPrefix.render(),
                        space(),
                        messenger.formatChatMessage(message, sender),
                    ),
                )
            }
        }
    }

    private fun Bubble.sendInfos(you: Player, yourMessage: String, theirMessage: String) {
        players.forEach { uuid ->
            val player = proxy.playerOrNull(uuid) ?: return@forEach
            player.sendInfo(if (player == you) yourMessage else theirMessage)
        }
    }

    private fun Bubble.broadcastInfo(message: String) {
        players.forEach { uuid ->
            proxy.playerOrNull(uuid)?.sendInfo(message)
        }
    }

    private fun addExcluded(uuid: UUID) {
        if (!database.getSetting(ShowGlobalChatInBubble, uuid)) {
            messenger.excludedFromGlobalChat.add(uuid)
        }
    }
}

class Bubble(
    var owner: UUID,
    val players: MutableSet<UUID>,
    val invitedPlayers: MutableSet<UUID>,
    var isPrivate: Boolean,
) {
    fun playersString(userCache: UserCache) =
        players.joinToString(", ", transform = userCache::usernameOrUuid)

    fun formatInfo(userCache: UserCache): HoverEvent<Component> =
        showText(text("Bubble: ${playersString(userCache)}"))

    fun joinButton(userCache: UserCache): Component {
        val ownerName = userCache.usernameOrUuid(owner)
        return "<gray>[</gray><green>Join</green><gray>]</gray>".render()
            .clickEvent(runCommand("/bubble join $ownerName"))
            .hoverEvent(showText(text("Click to join bubble")))
    }
}

class BubbleManager {
    val bubbles: List<Bubble>
        field = mutableListOf()

    fun createBubble(player: UUID): Bubble =
        Bubble(player, mutableSetOf(player), mutableSetOf(), isPrivate = true).also(bubbles::add)

    fun removeBubble(bubble: Bubble) {
        bubbles.remove(bubble)
    }

    fun getBubbleByPlayer(player: Player): Bubble? = bubbles.firstOrNull { player.uniqueId in it.players }
}
