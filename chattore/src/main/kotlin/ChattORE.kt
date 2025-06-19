package org.openredstone.chattore

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandIssuer
import co.aikar.commands.RegisteredCommand
import co.aikar.commands.VelocityCommandManager
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import net.kyori.adventure.text.format.NamedTextColor.RED
import net.kyori.adventure.text.format.TextDecoration.BOLD
import net.luckperms.api.LuckPermsProvider
import org.openredstone.chattore.feature.*
import org.slf4j.Logger
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.exists

@Plugin(
    id = "chattore",
    name = "ChattORE",
    version = BuildConfig.VERSION,
    url = "https://openredstone.org",
    description = "Because we want to have a chat system that actually wOREks for us.",
    authors = ["Nickster258", "PaukkuPalikka", "StackDoubleFlow", "sodiboo", "Waffle [Wueffi]"],
    dependencies = [Dependency(id = "luckperms")]
)
class ChattORE @Inject constructor(
    private val proxy: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataFolder: Path,
) {
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        val config = loadConfig()
        val luckPerms = LuckPermsProvider.get()
        val database = Storage(dataFolder.resolve(config.storage))
        val commandManager = VelocityCommandManager(proxy, this)
        val pluginScope = PluginScope(this, ChattORE::class.java, proxy, dataFolder, logger, commandManager)
        commandManager.apply {
            setDefaultExceptionHandler(::handleCommandException, false)
            commandCompletions.registerCompletion("username") { listOf(it.player.username) }
        }
        pluginScope.apply {
            val emojis = createEmojiFeature()
            val messenger = createMessenger(emojis, database, luckPerms, config.format.global)
            val userCache = createUserCache(database.database)
            createAliasFeature()
            createChatFeature(
                messenger,
                ChatConfirmationConfig(config.regexes),
            )
            createChattoreFeature()
            createDiscordFeature(messenger, emojis, config.discord)
            createFunCommandsFeature()
            createHelpOpFeature()
            createJoinLeaveFeature(config.format)
            createMailFeature(database, userCache)
            createMessageFeature(messenger)
            createNicknameFeature(
                database, userCache, NicknameConfig(
                    config.clearNicknameOnChange,
                    // IDK, this when config
                    config.nicknamePresets.mapValues { (_, v) -> NickPreset(v) }.toSortedMap(),
                )
            )
            createProfileFeature(database, luckPerms, userCache)
            createSpyingFeature(database)
        }
    }

    private fun loadConfig(): ChattOREConfig {
        if (!dataFolder.exists()) {
            logger.info("No resource directory found, creating")
            dataFolder.createDirectory()
        }
        val config = readConfig<ChattOREConfig>(logger, dataFolder.resolve("config.yml"))
        logger.info("Loaded config.yml")
        return config
    }

    private fun handleCommandException(
        command: BaseCommand,
        registeredCommand: RegisteredCommand<*>,
        sender: CommandIssuer,
        args: List<String>,
        throwable: Throwable,
    ): Boolean {
        val exception = throwable as? ChattoreException ?: return false
        val message = exception.message ?: "Something went wrong!"
        if (sender is Player) {
            sender.sendMessage("Oh NO ! "[BOLD + RED] + ": "[GRAY] + message[RED])
        } else {
            sender.sendMessage("Error: $message")
        }
        return true
    }
    // TODO reloading functionality
}

class ChattoreException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
