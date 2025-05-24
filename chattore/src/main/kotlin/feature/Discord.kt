package org.openredstone.chattore.feature

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.proxy.ProxyServer
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.intent.Intent
import org.javacord.api.entity.message.MessageBuilder
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.listener.message.MessageCreateListener
import org.openredstone.chattore.*
import org.slf4j.Logger
import kotlin.jvm.optionals.getOrNull

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
    val discordNetwork = DiscordApiBuilder()
        .setToken(config.networkToken)
        .addIntents(Intent.MESSAGE_CONTENT)
        .login()
        .join()
    val discordMap = loadDiscordTokens(proxy, logger, config.serverTokens)
    discordMap.forEach { (_, discordApi) -> discordApi.updateActivity(config.playingMessage) }
    val textChannel = discordNetwork.getTextChannelById(config.channelId).getOrNull()
        ?: throw ChattoreException("Cannot find Discord channel")
    textChannel.addMessageCreateListener(
        DiscordListener(logger, messenger, proxy, emojis, config)
    )
    registerListeners(DiscordBroadcastListener(config, discordMap, discordNetwork))
}

private class DiscordBroadcastListener(
    private val config: DiscordConfig,
    discordMap: Map<String, DiscordApi>,
    discordApi: DiscordApi,
) {
    private val serverChannelMapping: Map<String, TextChannel> = discordMap.entries.associate { (server, api) ->
        server to (api.getTextChannelById(config.channelId).getOrNull()
            ?: throw IllegalArgumentException("Could not get specified channel"))
    }

    private val mainBotChannel: TextChannel = discordApi.getTextChannelById(config.channelId).getOrNull()
        ?: throw IllegalArgumentException("Could not get specified channel")

    @Subscribe
    fun onBroadcastEvent(event: DiscordBroadcastEvent) {
        val channel = serverChannelMapping[event.server] ?: return
        val content = config.discordFormat
            .replace("%prefix%", event.prefix)
            .replace("%sender%", event.sender.discordEscape())
            .replace("%message%", event.message)
        MessageBuilder().setContent(content).send(channel)
    }

    @Subscribe
    fun onBroadcastEventRaw(event: DiscordBroadcastEventMain) {
        val message = event.format
            .replace("%player%", event.player.discordEscape())
        MessageBuilder().setContent(message).send(mainBotChannel)
    }
}

private class DiscordListener(
    private val logger: Logger,
    private val messenger: Messenger,
    private val proxy: ProxyServer,
    private val emojis: Emojis,
    private val config: DiscordConfig,
) : MessageCreateListener {

    private val emojiPattern = emojis.emojiToName.keys.joinToString("|", "(", ")") { Regex.escape(it) }
    private val emojiRegex = Regex(emojiPattern)
    private val urlMarkdownRegex = """\[([^]]*)]\(\s?(\S+)\s?\)""".toRegex()

    private fun replaceEmojis(input: String) = emojiRegex.replace(input) { matchResult ->
        val emoji = matchResult.value
        val emojiName = emojis.emojiToName[emoji]
        if (emojiName != null) ":$emojiName:" else emoji
    }

    override fun onMessageCreate(event: MessageCreateEvent) {
        if (event.messageAuthor.isBotUser && event.messageAuthor.id != config.chadId) return
        val attachments = event.messageAttachments.joinToString(" ", " ") { it.url.toString() }
        val toSend = replaceEmojis(event.message.readableContent) + attachments
        logger.info("[Discord] ${event.messageAuthor.displayName} (${event.messageAuthor.id}): $toSend")
        val transformedMessage = toSend.replace(urlMarkdownRegex) { matchResult ->
            val text = matchResult.groupValues[1].trim()
            val url = matchResult.groupValues[2].trim()
            "$text: $url"
        }.replace("""\s+""".toRegex(), " ")
        proxy.all.sendRichMessage(
            config.ingameFormat,
            "sender" toS event.messageAuthor.displayName,
            "message" toC messenger.prepareChatMessage(transformedMessage, null),
        )
    }
}

private fun loadDiscordTokens(
    proxy: ProxyServer,
    logger: Logger,
    serverTokens: Map<String, String>,
): Map<String, DiscordApi> {
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
        DiscordApiBuilder()
            .setToken(token)
            .login()
            .join()
    }
}
