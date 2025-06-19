package org.openredstone.chattore

import co.aikar.commands.BaseCommand
import co.aikar.commands.VelocityCommandManager
import com.velocitypowered.api.event.EventHandler
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.ForwardingAudience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.slf4j.Logger
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeBytes
import kotlin.jvm.optionals.getOrNull

fun Component.miniMessageSerialize() = MiniMessage.miniMessage().serialize(this)

private val legacyNoObfuscate = LegacyComponentSerializer.builder()
    .character('&')
    .formats(CharacterAndFormat.defaults().filter { it != CharacterAndFormat.OBFUSCATED })
    .build()

fun String.legacyDeserialize(canObfuscate: Boolean = false): TextComponent {
    // remove section signs so the client doesn't render them. ew
    val noSections = replace('§', '&')
    return if (canObfuscate) {
        LegacyComponentSerializer.legacyAmpersand().deserialize(noSections)
    } else {
        legacyNoObfuscate.deserialize(noSections)
    }
}

fun String.render(vararg resolvers: TagResolver): Component =
    MiniMessage.miniMessage().deserialize(this, *resolvers)

// TODO remove toS?
// Convenience functions for constructing TagResolvers that act as placeholders
infix fun String.toS(message: String) = this toC message.c
infix fun String.toC(message: Component) = Placeholder.component(this, message)

private val infoPrefix = "["[GOLD] + "ChattORE"[RED] + "]"[GOLD]
fun Audience.sendInfo(message: Component) = sendMessage(infoPrefix + " ".c + message)
fun Audience.sendInfo(message: String) = sendInfo(message.c)

/** Mirrors Player.sendRichMessage **/
fun Audience.sendRichMessage(message: String, vararg resolvers: TagResolver) = sendMessage(message.render(*resolvers))

fun ProxyServer.playerOrNull(uuid: UUID): Player? = getPlayer(uuid).getOrNull()

class PluginScope(
    val plugin: Any,
    private val pluginClass: Class<*>,
    val proxy: ProxyServer,
    private val dataFolder: Path,
    val logger: Logger,
    val commandManager: VelocityCommandManager,
) {
    fun registerListeners(vararg listeners: Any) =
        listeners.forEach { proxy.eventManager.register(plugin, it) }

    fun registerCommands(vararg commands: BaseCommand) = commands.forEach(commandManager::registerCommand)

    /***
     * Convert a relative [filename] of a resource to a URL.
     * Throws FileNotFoundException if not found.
     */
    fun getResource(filename: String): URL = pluginClass.getResource("/$filename")
        ?: throw FileNotFoundException("Cannot load resource file /$filename. This is probably a bug.")

    fun loadResourceAsString(name: String): String = getResource(name).readText()
    fun loadDataResourceAsString(name: String): String = getResourceOrCopyDefault(name).readText()

    fun getResourceOrCopyDefault(name: String): Path {
        // NOTE/TODO: we may want to merge with existing files in data folder in the future
        val resource = dataFolder.resolve(name)
        if (!resource.exists()) {
            logger.info("Resource $name not saved to data folder, saving")
            resource.writeBytes(getResource(name).readBytes())
        }
        return resource
    }
}

inline fun <reified T> PluginScope.onEvent(postOrder: Short = 0, noinline handler: (T) -> Unit): EventHandler<T> =
    EventHandler(handler).also { proxy.eventManager.register(plugin, T::class.java, postOrder, it) }


private val uuidRegex = """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}""".toRegex()
fun parseUuid(input: String): UUID? = if (uuidRegex.matches(input)) UUID.fromString(input) else null

/** Dynamic audience consisting of only those players on proxy who pass the test */
fun ProxyServer.all(test: (Player) -> Boolean): Audience =
    ForwardingAudience { allPlayers.filter(test) }

fun ProxyServer.allBut(player: Player) = all { it != player }
val ProxyServer.all get() = ForwardingAudience { allPlayers }

val Player.hasChattorePrivilege get() = hasPermission("chattore.privileged")
