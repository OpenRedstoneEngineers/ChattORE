package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent.suggestCommand
import net.kyori.adventure.text.event.HoverEvent.showText
import org.openredstone.chattore.*
import org.slf4j.Logger

fun PluginScope.createFunCommandsFeature(chatConfirmations: ChatConfirmations) {
    val commands = Json.decodeFromString<List<FunCommand>>(loadDataResourceAsString("commands.json"))
    createFunCommands(logger, proxy, chatConfirmations, proxy.commandManager, commands)
    registerCommands(FunCommandsCommand(commands))
}

@CommandAlias("funcommands|fc")
@CommandPermission("chattore.funcommands")
private class FunCommandsCommand(
    private val commands: List<FunCommand>,
) : BaseCommand() {

    @Default
    @Description("Displays information about the /funcommands command")
    fun onDefault(player: Player) {
        player.sendRichMessage("<green>FunCommands v1.1 by <gold>Waffle [Wueffi]</gold></green>")
    }

    @Subcommand("list")
    @Description("Lists all available fun commands in alphabetical order")
    fun onList(player: Player) {
        if (commands.isEmpty()) {
            player.sendRichMessage("<red>No Fun Commands found.</red>")
            return
        }

        player.sendRichMessage("<yellow>Available Fun Commands:</yellow>")

        commands
            .sortedBy { it.command }
            .map {
                text("/${it.command}")
                    .clickEvent(suggestCommand("/${it.command}"))
                    .hoverEvent(showText(text(it.description)))
            }
            .let { Component.join(JoinConfiguration.spaces(), it) }
            .let(player::sendMessage)
    }

    @Subcommand("info")
    @Description("Displays information about a specific fun command")
    @Syntax("<command>")
    fun onInfo(player: Player, commandName: String) {
        if (commandName.isEmpty()) {
            throw ChattoreException("You must specify a command.")
        }

        val cmd = commands.find { it.command.equals(commandName, ignoreCase = true) }
            ?: throw ChattoreException("Command '$commandName' not found.")

        player.sendRichMessage(
            "<gold>Description for <yellow>/<command></yellow>: <description></gold>",
            "command" toS cmd.command,
            "description" toS cmd.description,
        )
    }
}


@Serializable
private data class FunCommand(
    val command: String,
    val description: String,
    // message to sender only
    val localChat: String? = null,
    // broadcast to all
    val globalChat: String? = null,
    // send to everyone except the sender
    val othersChat: String? = null,
    // execute action
    val run: String? = null,
)

private fun createFunCommands(
    logger: Logger,
    proxy: ProxyServer,
    chatConfirmations: ChatConfirmations,
    commandManager: CommandManager,
    commands: List<FunCommand>,
) {
    fun executeAction(action: String, player: Player) {
        when {
            action.startsWith("kick") -> {
                val reason = action.removePrefix("kick").trim()
                player.disconnect(text(reason))
            }

            action.startsWith("kill") -> {
                commandManager.executeAsync(player, "suicide")
            }
        }
    }

    fun createDynamicCommand(cmd: FunCommand) = SimpleCommand { invocation ->
        val source = invocation.source()
        val args = invocation.arguments()

        if (source !is Player) {
            source.sendMessage(text("This command can only be used by players!"))
            return@SimpleCommand
        }

        val allArgs = args.joinToString(" ")
        val replacements = arrayOf(
            "name" toS source.username,
            "arg-all" toS allArgs,
            "arg-1" toS (args.getOrNull(1) ?: "<missing>"),
            "arg-2" toS (args.getOrNull(2) ?: "<missing>"),
        )

        chatConfirmations.submit(source, "/${invocation.alias()} $allArgs") { source ->
            cmd.globalChat?.let { proxy.all.sendRichMessage(it, *replacements) }
            cmd.localChat?.let { source.sendRichMessage(it, *replacements) }
            cmd.othersChat?.let { proxy.allBut(source).sendRichMessage(it, *replacements) }
            cmd.run?.let { executeAction(it, source) }
        }
    }

    commands.forEach { commandConfig ->
        val meta = commandManager.metaBuilder(commandConfig.command).build()
        commandManager.register(meta, createDynamicCommand(commandConfig))
    }
    logger.info("Loaded ${commands.size} fun commands")
}
