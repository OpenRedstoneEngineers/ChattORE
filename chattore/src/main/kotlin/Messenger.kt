package org.openredstone.chattore

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.luckperms.api.LuckPerms
import org.openredstone.chattore.feature.DiscordBroadcastEvent
import org.openredstone.chattore.feature.NickPreset
import org.slf4j.Logger
import java.net.URI

class Messenger(
    plugin: ChattORE,
    emojis: Map<String, String>,
    private val logger: Logger,
    private val proxy: ProxyServer,
    private val database: Storage,
    private val luckPerms: LuckPerms,
    private val chatBroadcastFormat: String,
) {
    private val urlRegex = """<?((http|https)://([\w_-]+(?:\.[\w_-]+)+)([^\s'<>]+)?)>?""".toRegex()
    private val fileTypeMap: Map<String, List<String>> =
        Json.parseToJsonElement(plugin.loadResourceAsString("filetypes.json"))
            .jsonObject.mapValues { (_, value) -> value.jsonArray.map { it.jsonPrimitive.content } }
            .onEach { (key, values) -> logger.info("Loaded ${values.size} of type $key") }

    private val chatReplacements = listOf(
        formatReplacement("**", "b"),
        formatReplacement("*", "i"),
        formatReplacement("__", "u"),
        formatReplacement("~~", "st"),
        buildEmojiReplacement(emojis),
    )

    private fun formatReplacement(key: String, tag: String): TextReplacementConfig =
        TextReplacementConfig.builder()
            .match("""((\\?)(${Regex.escape(key)}(.*?)${Regex.escape(key)}))""")
            .replacement { result, _ ->
                if (result.group(2).contains("\\") || result.group(4).endsWith("\\")) {
                    result.group(3).toComponent()
                } else {
                    "<$tag>${result.group(4)}</$tag>".render()
                }
            }
            .build()

    private fun buildEmojiReplacement(emojis: Map<String, String>): TextReplacementConfig =
        TextReplacementConfig.builder()
            .match(""":([A-Za-z0-9_\-+]+):""")
            .replacement { result, _ ->
                val match = result.group(1)
                val content = emojis[match] ?: ":$match:"
                "<hover:show_text:'$match'>$content</hover>".render()
            }
            .build()

    fun broadcastChatMessage(originServer: String, player: Player, message: String) {
        val userId = player.uniqueId
        val userManager = luckPerms.userManager
        val luckUser = userManager.getUser(userId) ?: return
        val name = database.getNickname(userId) ?: NickPreset(player.username)
        val sender = name.render(player.username).let {
            "<click:run_command:'/playerprofile info ${player.username}'><message></click>".renderSimpleC(it)
        }

        val prefix = luckUser.cachedData.metaData.prefix
            ?: luckUser.primaryGroup.replaceFirstChar(Char::uppercaseChar)

        val compoPrefix = prefix.legacyDeserialize()
        proxy.all.sendRichMessage(
            chatBroadcastFormat,
            "message" toC prepareChatMessage(message, player),
            "sender" toC sender,
            "username" toS player.username,
            "prefix" toC compoPrefix,
        )

        val plainPrefix = PlainTextComponentSerializer.plainText().serialize(compoPrefix)
        val discordBroadcast = DiscordBroadcastEvent(
            plainPrefix,
            player.username,
            originServer,
            message
        )
        proxy.eventManager.fireAndForget(discordBroadcast)
    }

    fun prepareChatMessage(
        message: String,
        player: Player?,
    ): Component {
        val canObfuscate = player?.hasPermission("chattore.chat.obfuscate") ?: false
        val parts = urlRegex.split(message)
        val matches = urlRegex.findAll(message).iterator()
        val builder = Component.text()
        parts.forEach { part ->
            builder.append(part.legacyDeserialize(canObfuscate))
            if (matches.hasNext()) {
                val nextMatch = matches.next()
                builder.append(formatLink(nextMatch.groupValues[1]))
            }
        }
        return builder.build().performReplacements(chatReplacements)
    }

    private fun formatLink(str: String): Component {
        val link = URI(str).toURL()
        var type = "link"
        var name = link.host
        if (link.file.isNotEmpty()) {
            val last = link.path.split("/").last()
            if (last.contains('.') && !last.endsWith('.') && !last.startsWith('.')) {
                type = last.split('.').last()
                name = if (last.length > 15) {
                    last.substring(0, 15) + "…." + type
                } else {
                    last
                }
            }
        }
        val contentType = fileTypeMap.entries.find { type in it.value }?.key
        val symbol = when (contentType) {
            "IMAGE" -> "\uD83D\uDDBC"
            "AUDIO" -> "\uD83D\uDD0A"
            "VIDEO" -> "\uD83C\uDFA5"
            "TEXT" -> "\uD83D\uDCDD"
            else -> "\uD83D\uDCCE"
        }
        return ("<aqua><click:open_url:'$link'>" +
            "<hover:show_text:'<aqua>$link'>" +
            "[$symbol $name]" +
            "</hover>" +
            "</click><reset>").render()
    }

    private fun Component.performReplacements(replacements: List<TextReplacementConfig>): Component =
        replacements.fold(this, Component::replaceText)
}
