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
import net.kyori.adventure.text.JoinConfiguration
import org.openredstone.chattore.*
import org.slf4j.Logger

fun PluginScope.createFunCommandsFeature() {
    val commands = Json.decodeFromString<List<FunCommand>>(loadDataResourceAsString("commands.json"))
    createFunCommands(logger, proxy, proxy.commandManager, commands)
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
            player.sendRichMessage("<red>No fun commands found.</red>")
            return
        }

        player.sendRichMessage("<yellow>Available Fun Commands:</yellow>")

        // NOTE: interpolating like this is generally unsafe. We trust the commands.json file contents so this is fine.
        // same applies to the info subcommand
        commands
            .sortedBy { it.command }
            .map {
                "<hover:show_text:'${it.description}'><click:suggest_command:'/${it.command}'>/${it.command}".render()
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

        player.sendRichMessage("<gold>Description for <yellow>/${cmd.command}</yellow>: ${cmd.description}></gold>")
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
    commandManager: CommandManager,
    commands: List<FunCommand>,
) {
    fun executeAction(action: String, player: Player) {
        when {
            action.startsWith("kick") -> {
                val reason = action.removePrefix("kick").trim()
                player.disconnect(Component.text(reason))
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
            source.sendMessage(Component.text("This command can only be used by players!"))
            return@SimpleCommand
        }

        val replacements = arrayOf(
            "name" toS source.username,
            "arg-all" toS args.joinToString(" "),
            "arg-1" toS (args.getOrNull(1) ?: "<missing>"),
            "arg-2" toS (args.getOrNull(2) ?: "<missing>")
        )

        cmd.globalChat?.let { proxy.all.sendRichMessage(it, *replacements) }
        cmd.localChat?.let { source.sendRichMessage(it, *replacements) }
        cmd.othersChat?.let { proxy.allBut(source).sendRichMessage(it, *replacements) }
        cmd.run?.let { executeAction(it, source) }
    }

    commands.forEach { commandConfig ->
        val meta = commandManager.metaBuilder(commandConfig.command).build()
        commandManager.register(meta, createDynamicCommand(commandConfig))
    }
    logger.info("Loaded ${commands.size} fun commands")
}
