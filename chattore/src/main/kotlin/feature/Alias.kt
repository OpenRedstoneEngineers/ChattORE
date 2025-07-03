package org.openredstone.chattore.feature

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.json.Json
import net.kyori.adventure.text.format.NamedTextColor.RED
import org.openredstone.chattore.PluginScope
import org.openredstone.chattore.c
import org.openredstone.chattore.common.ALIAS_CHANNEL
import org.openredstone.chattore.common.AliasMessage
import org.openredstone.chattore.get
import org.slf4j.Logger

private val IDENTIFIER: MinecraftChannelIdentifier = MinecraftChannelIdentifier.from(ALIAS_CHANNEL)

private val argumentsRegex = Regex("\\\$(args|[0-9]+)")

@Serializable
data class AliasConfig(
    val alias: String = "alias",
    val commands: List<String> = listOf("some", "commands"),
)

fun PluginScope.createAliasFeature() {
    proxy.channelRegistrar.register(IDENTIFIER)
    fun String.toCommandAliasMeta() = proxy.commandManager.metaBuilder(this)
        .plugin(plugin)
        .build()

    val aliases = Json.decodeFromString<List<AliasConfig>>(loadDataResourceAsString("aliases.json"))
    aliases.forEach { (alias, commands) ->
        val meta = alias.toCommandAliasMeta()
        val aliasCommand = AliasCommand(logger, proxy, alias, commands)
        proxy.commandManager.register(meta, aliasCommand)
    }
    logger.info("Loaded ${aliases.size} aliases")
    registerListeners(PluginMessageListener(logger))
}

private class AliasCommand(
    private val logger: Logger,
    private val proxy: ProxyServer,
    private val base: String,
    private val commands: List<String>,
) : SimpleCommand {

    private val requiredArgs = commands.flatMap {
        argumentsRegex.findAll(it).map { match ->
            match.groupValues[1].toIntOrNull()?.plus(1) ?: 1
        }
    }.maxOrNull() ?: 0

    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()
        val args = invocation.arguments()

        if (requiredArgs > args.size) {
            source.sendMessage("Not enough arguments! I expected $requiredArgs argument(s)!".c)
            return
        }

        if (source !is Player) {
            source.sendMessage("This command can only be used by players!".c)
            return
        }

        if (args.isEmpty()) {
            logger.info("Executing alias $base for user ${source.username} (${source.uniqueId})")
        } else {
            logger.info(
                "Executing alias $base for user ${source.username}" +
                    " (${source.uniqueId}) with args: ${args.joinToString(" ")}"
            )
        }
        executeAlias(source, args.toList())
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun executeAlias(player: Player, args: List<String> = emptyList()) {
        val server = player.currentServer.orElse(null) ?: return
        commands.forEach { command ->
            val commandLine = command.replace(argumentsRegex) { matchResult ->
                val group = matchResult.groupValues[1]
                if (group == "args") {
                    args.joinToString(" ")
                } else {
                    args[group.toInt()]
                }
            }
            proxy.commandManager.executeImmediatelyAsync(player, commandLine).whenComplete { result, throwable ->
                if (throwable != null) {
                    logger.error(throwable.message, throwable)
                }
                if (result) {
                    logger.info("Ran \"$commandLine\" on the proxy")
                    return@whenComplete
                }
                logger.info("Forwarding \"$commandLine\" to game server")
                server.sendPluginMessage(
                    IDENTIFIER,
                    Cbor.encodeToByteArray(AliasMessage(player.uniqueId, commandLine))
                )
            }.join()  // Hack, but guarantees execution order, assuming plugin messages are processed sequentially
        }
    }
}

private class PluginMessageListener(private val logger: Logger) {
    @Subscribe
    fun onPluginMessage(message: PluginMessageEvent) {
        if (IDENTIFIER != message.identifier) {
            return
        }
        // cancel forwarding immediately
        message.result = PluginMessageEvent.ForwardResult.handled()
        when (val src = message.source) {
            is Player -> {
                logger.warn("Player ${src.username} (${src.uniqueId}) tried to send a plugin message to $IDENTIFIER!")
                src.disconnect("We don't really like it when you try to break things"[RED])
            }
            // is ServerConnection for when we want to handle messages from chattoreagent
        }
    }
}
