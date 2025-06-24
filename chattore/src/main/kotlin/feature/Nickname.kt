package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.annotation.*
import co.aikar.commands.annotation.Optional
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.openredstone.chattore.*
import java.util.*

data class NicknameConfig(
    val clearNicknameOnChange: Boolean = true,
    val presets: SortedMap<String, NickPreset>,
)

fun PluginScope.createNicknameFeature(
    database: Storage,
    userCache: UserCache,
    config: NicknameConfig,
) {
    commandManager.apply {
        @Suppress("LocalVariableName")
        val COMPLETION_NICK_PRESETS = "nickPresets"
        commandCompletions.registerCompletion(COMPLETION_NICK_PRESETS) { config.presets.keys }
        commandContexts.registerContext(NickPreset::class.java) {
            config.presets[it.popFirstArg()]
                ?: throw InvalidCommandArgument("Unknown nickname preset! Use /nick presets to see available presets.")
        }
        commandCompletions.setDefaultCompletion(COMPLETION_NICK_PRESETS, NickPreset::class.java)
        commandCompletions.registerCompletion(COMPLETION_COLORS) { ctx ->
            (hexColorMap.values.map { it.second }
                + if (ctx.input.isEmpty()) {
                listOf("#", "&")
            } else if (ctx.input.startsWith("#")) {
                (hexColorMap.values.map { it.first }
                    + ctx.input.padEnd(7, '0').let {
                    // do not duplicate if it's already in the list
                    // do not suggest if it's not a valid hex color
                    if (it.matches(Regex("^#[0-9A-Fa-f]{6}$"))
                        && ctx.input.uppercase() !in hexColorMap.values.map
                        { (hex, _) -> hex.substring(0, ctx.input.length) }
                    ) {
                        listOf(it.uppercase())
                    } else {
                        listOf()
                    }
                })
            } else if (ctx.input.startsWith("&")) {
                hexColorMap.keys.map { "&$it" }
            } else {
                listOf()
            })
        }
    }
    registerCommands(Nickname(database, proxy, userCache, config))
    registerListeners(NicknameListener(database, userCache, config))
}

private const val COMPLETION_COLORS = "colors"

val hexColorMap = mapOf(
    "0" to Pair("#000000", "black"),
    "1" to Pair("#00AA00", "dark_blue"),
    "2" to Pair("#00AA00", "dark_green"),
    "3" to Pair("#00AAAA", "dark_aqua"),
    "4" to Pair("#AA0000", "dark_red"),
    "5" to Pair("#AA00AA", "dark_purple"),
    "6" to Pair("#FFAA00", "gold"),
    "7" to Pair("#AAAAAA", "gray"),
    "8" to Pair("#555555", "dark_gray"),
    "9" to Pair("#5555FF", "blue"),
    "a" to Pair("#55FF55", "green"),
    "b" to Pair("#55FFFF", "aqua"),
    "c" to Pair("#FF5555", "red"),
    "d" to Pair("#FF55FF", "light_purple"),
    "e" to Pair("#FFFF55", "yellow"),
    "f" to Pair("#FFFFFF", "white")
)

private val hexPattern = """#[0-9a-fA-F]{6}""".toRegex()

// TODO: parse instead?
private fun String.validateColor() = if (this.startsWith("&")) {
    if (this.length != 2) {
        throw ChattoreException("When providing legacy color codes, use a single character after &.")
    }
    val (_, colorName) = hexColorMap[this.substring(1)]
        ?: throw ChattoreException("Invalid legacy color code provided")
    colorName
} else if (hexPattern.matches(this)) {
    this
} else if (this in hexColorMap.values.map { it.second }) {
    this
} else {
    throw ChattoreException("Invalid color code provided")
}

data class NickPreset(val miniMessageFormat: String) {
    fun render(username: String): Component = miniMessageFormat.render("username" toS username)
    val isGeneric get() = "<username>" in miniMessageFormat

    companion object {
        fun colorOrGradient(colors: Array<out String>): NickPreset {
            val codes = colors.joinToString(":") { it.validateColor() }
            val nickname = if (colors.size == 1) {
                "<color:$codes><username></color:$codes>"
            } else {
                "<gradient:$codes><username></gradient>"
            }
            return NickPreset(nickname)
        }
    }
}

@CommandAlias("nick|nickname")
@Description("Manage nicknames")
@CommandPermission("chattore.nick")
private class Nickname(
    private val database: Storage,
    private val proxy: ProxyServer,
    private val userCache: UserCache,
    private val config: NicknameConfig,
) : BaseCommand() {

    @Subcommand("color")
    @CommandCompletion("@${COMPLETION_COLORS}")
    fun set(player: Player, vararg colors: String) {
        if (colors.isEmpty()) throw ChattoreException("No colors provided! Please provide 1 to 3 colors!")
        if (colors.size > 3) throw ChattoreException("Too many colors! Please provide 1 to 3 colors!")
        val nickname = NickPreset.colorOrGradient(colors)
        database.setNickname(player.uniqueId, nickname)
        player.notifyOfNickChange(nickname)
    }

    @Subcommand("preset")
    @CommandPermission("chattore.nick.preset")
    fun preset(player: Player, preset: NickPreset) {
        database.setNickname(player.uniqueId, preset)
        player.notifyOfNickChange(preset)
    }

    @Subcommand("presets")
    @CommandPermission("chattore.nick.preset")
    @CommandCompletion("@username")
    fun presets(player: Player, @Optional shownText: String?) {
        val renderedPresets = ArrayList<Component>()
        for ((presetName, preset) in config.presets) {
            val renderedPreset = preset.render(presetName)
            val rendered = if (shownText == null) {
                // Primarily show the preset name, else a preview of the nickname.
                renderedPreset.c(HoverEvent.showText("Click to apply ".c + preset.render(player.username)))
            } else {
                // Primarily show the entered text, else the preset name.
                // Also, we're suggesting the username as the autocompleted $shownText.
                preset.render(shownText).c(HoverEvent.showText("Click to apply ".c + renderedPreset + " preset".c))
            }.let { it.c(ClickEvent.runCommand("/nick preset $presetName")) }
            renderedPresets.add(rendered)
        }

        player.sendInfo("Available presets: ".c + renderedPresets.join(", ".c))
    }

    @Subcommand("nick")
    @CommandPermission("chattore.nick.others")
    fun nick(commandSource: CommandSource, target: User, @Single nick: String) {
        val nickname = if (nick.contains("&")) {
            nick.legacyDeserialize().miniMessageSerialize()
        } else {
            nick
        }.let(::NickPreset)
        database.setNickname(target.uuid, nickname)
        proxy.playerOrNull(target.uuid)?.notifyOfNickChange(nickname)
        commandSource.notifyExecutor(target, nickname)
    }

    @Subcommand("remove")
    @CommandPermission("chattore.nick.remove")
    fun remove(commandSource: CommandSource, target: User) {
        database.removeNickname(target.uuid)
        proxy.playerOrNull(target.uuid)?.sendInfo("Your nickname has been removed")
        commandSource.sendInfo("Removed nickname for ${userCache.usernameOrUuid(target)}.")
    }

    @Subcommand("setgradient")
    @CommandPermission("chattore.nick.setgradient")
    fun setgradient(player: Player, target: User, vararg colors: String) {
        if (colors.size < 2) throw ChattoreException("Not enough colors!")
        val nickname = NickPreset.colorOrGradient(colors)
        database.setNickname(target.uuid, nickname)
        proxy.playerOrNull(target.uuid)?.notifyOfNickChange(nickname)
        player.notifyExecutor(target, nickname)
    }

    private fun CommandSource.notifyExecutor(target: User, nickname: NickPreset) {
        val targetName = userCache.usernameOrUuid(target)
        sendInfo("Set nickname for $targetName as ".c + nickname.render(targetName) + ".".c)
    }

    private fun Player.notifyOfNickChange(nickname: NickPreset) =
        sendInfo("Your nickname has been set to ".c + nickname.render(username))
}

private class NicknameListener(
    private val database: Storage,
    private val userCache: UserCache,
    private val config: NicknameConfig,
) {
    @Subscribe
    fun joinEvent(event: LoginEvent) {
        if (!config.clearNicknameOnChange) return
        val existingName = userCache.usernameOrNull(event.player.uniqueId) ?: return
        if (existingName == event.player.username) return
        val nickname = database.getNickname(event.player.uniqueId) ?: return
        if (nickname.isGeneric) return
        database.removeNickname(event.player.uniqueId)
    }
}
