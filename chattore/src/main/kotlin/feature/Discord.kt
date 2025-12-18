package org.openredstone.chattore.feature

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.proxy.ProxyServer
import dev.kord.common.annotation.KordPreview
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.live.channel.live
import dev.kord.core.live.channel.onMessageCreate
import dev.kord.gateway.Intent
import dev.kord.gateway.Intents
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.*
import org.openredstone.chattore.*
import org.slf4j.Logger

fun String.discordEscape() = this.replace("""_""", "\\_")

data class DiscordConfig(
    val enable: Boolean = false,
    val networkToken: String = "nouNetwork",
    val channelId: Long = 1234L,
    val chadId: Long = 1234L,
    val playingMessage: String = "on the ORE Network",
    val discordFormat: String = "`%prefix%` **%sender%**: %message%",
    val serverTokens: Map<String, String> = mapOf(
        "serverOne" to "token1",
        "serverTwo" to "token2",
        "serverThree" to "token3"
    ),
    val ingameFormat: String = "<dark_aqua>Discord</dark_aqua> <gray>|</gray> <dark_purple><sender></dark_purple><gray>:</gray> <message>",
)

// TO Discord
data class DiscordBroadcastEvent(
    val prefix: String,
    val sender: String,
    val server: String,
    val message: String,
)

// Comes under the "ORE Network" bot
data class DiscordBroadcastEventMain(
    val format: String,
    val player: String,
)

fun PluginScope.createDiscordFeature(
    messenger: Messenger,
    emojis: Emojis,
    config: DiscordConfig,
) {
    if (!config.enable) return

    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch(Dispatchers.Default) {
        coroutineScope {
            val discordNetwork = Kord(config.networkToken)
            // login blocks until the bot shuts down, so we launch it in its own coroutine
            launch {
                discordNetwork.login {
                    @OptIn(PrivilegedIntent::class)
                    intents += Intent.MessageContent
                    presence {
                        playing(config.playingMessage)
                    }
                }
            }
            val discordMap = spawnServerBots(proxy, logger, config)
            val serverChannels = discordMap.mapValues { (_, api) -> getGameChat(api, config.channelId) }
            val mainBotChannel = getGameChat(discordNetwork, config.channelId)
            val listener = DiscordListener(logger, messenger, proxy, emojis, config)
            @OptIn(KordPreview::class)
            mainBotChannel.live().onMessageCreate(block = listener::onMessageCreate)
            registerListeners(DiscordBroadcastListener(config, serverChannels, mainBotChannel, this))
            onEvent<ProxyShutdownEvent> {
                // block so that velocity waits before shutting down
                // future considerations:
                // - can this use async velocity events?
                // - should we do the shutdowns concurrently?
                runBlocking {
                    discordNetwork.shutdown()
                    discordMap.forEach { (_, kord) -> kord.shutdown() }
                }
            }
        }
    }
}

private suspend fun getGameChat(api: Kord, id: Long): TextChannel = api.getChannelOf(Snowflake(id))
    ?: throw IllegalArgumentException("Cannot find game-chat channel")

private class DiscordBroadcastListener(
    private val config: DiscordConfig,
    private val serverChannelMapping: Map<String, TextChannel>,
    private val mainBotChannel: TextChannel,
    private val scope: CoroutineScope,
) {
    @Subscribe
    fun onBroadcastEvent(event: DiscordBroadcastEvent) {
        scope.launch {
            val channel = serverChannelMapping[event.server] ?: return@launch
            val content = config.discordFormat
                .replace("%prefix%", event.prefix)
                .replace("%sender%", event.sender.discordEscape())
                .replace("%message%", event.message)
            channel.createMessage(content)
        }
    }

    @Subscribe
    fun onBroadcastEventRaw(event: DiscordBroadcastEventMain) {
        scope.launch {
            val message = event.format
                .replace("%player%", event.player.discordEscape())
            mainBotChannel.createMessage(message)
        }
    }
}

private class DiscordListener(
    private val logger: Logger,
    private val messenger: Messenger,
    private val proxy: ProxyServer,
    private val emojis: Emojis,
    private val config: DiscordConfig,
) {
    private val emojiPattern = emojis.emojiToName.keys.joinToString("|", "(", ")") { Regex.escape(it) }
    private val emojiRegex = Regex(emojiPattern)
    private val urlMarkdownRegex = """\[([^]]*)]\(\s?(\S+)\s?\)""".toRegex()

    private fun replaceEmojis(input: String) = emojiRegex.replace(input) { matchResult ->
        val emoji = matchResult.value
        val emojiName = emojis.emojiToName[emoji]
        if (emojiName != null) ":$emojiName:" else emoji
    }

    fun onMessageCreate(event: MessageCreateEvent) {
        // guaranteed to not happen because events are filtered beforehand
        val sender = event.member ?: throw IllegalStateException("onMessageCreate: event.member is null")
        if (sender.isBot && sender.id != Snowflake(config.chadId)) return
        val attachments = event.message.attachments.joinToString(" ", " ") { it.url }
        val toSend = replaceEmojis(event.message.content) + attachments
        val displayName = sender.effectiveName
        logger.info("[Discord] $displayName (${sender.id}): $toSend")
        val transformedMessage = toSend.replace(urlMarkdownRegex) { matchResult ->
            val text = matchResult.groupValues[1].trim()
            val url = matchResult.groupValues[2].trim()
            "$text: $url"
        }.replace("""\s+""".toRegex(), " ")
        proxy.all.sendRichMessage(
            config.ingameFormat,
            "sender" toS displayName,
            "message" toC messenger.prepareChatMessage(transformedMessage, null),
        )
    }
}

private suspend fun CoroutineScope.spawnServerBots(
    proxy: ProxyServer,
    logger: Logger,
    config: DiscordConfig,
): Map<String, Kord> {
    val serverTokens = config.serverTokens
    val availableServers = proxy.allServers.map { it.serverInfo.name.lowercase() }.sorted()
    val configServers = serverTokens.map { it.key.lowercase() }.sorted()
    if (availableServers != configServers) {
        logger.warn(
            """
                    Supplied server keys in Discord configuration section does not match available servers:
                    Available servers: ${availableServers.joinToString()}
                    Configured servers: ${configServers.joinToString()}
                """.trimIndent()
        )
    }
    return serverTokens.mapValues { (_, token) ->
        val kord = Kord(token)
        launch {
            kord.login {
                // server bots don't need any intents
                intents = Intents()
                presence {
                    playing(config.playingMessage)
                }
            }
        }
        kord
    }
}
