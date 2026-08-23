package org.openredstone.chattore

import co.aikar.commands.*
import co.aikar.commands.velocity.contexts.OnlinePlayer
import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
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
    dependencies = [Dependency(id = "luckperms")],
)
class ChattORE @Inject constructor(
    private val proxy: ProxyServer,
    private val logger: Logger,
    @param:DataDirectory private val dataFolder: Path,
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
            commandCompletions.registerStaticCompletion("boolean", arrayOf("true", "false"))
            commandCompletions.setDefaultCompletion("boolean", Boolean::class.java)
            commandCompletions.setDefaultCompletion("players", OnlinePlayer::class.java)
            @Suppress("DEPRECATION")
            enableUnstableAPI("help")
        }
        pluginScope.apply {
            val emojis = createEmojiFeature()
            val userCache = createUserCache(database.database)
            val wiretap = createSpyingFeature(database, config.format)
            val messenger = createMessenger(emojis, database, luckPerms, config.format, wiretap, userCache)
            val chatConfirmations = createChatConfirmations(ChatConfirmationConfig(config.regexes))
            val bubbleManager = createBubbleFeature(messenger, database, chatConfirmations, config.format, userCache)
            createAliasFeature()
            createChatFeature(messenger, chatConfirmations, bubbleManager)
            createChattoreFeature()
            createDiscordFeature(messenger, emojis, config.discord)
            createFunCommandsFeature(chatConfirmations)
            createHelpOpFeature(chatConfirmations)
            createJoinLeaveFeature(config.format)
            createMailFeature(database, userCache, chatConfirmations, wiretap)
            createMessageFeature(messenger, chatConfirmations, wiretap)
            createNicknameFeature(
                database, userCache,
                NicknameConfig(
                    config.clearNicknameOnChange,
                    // IDK, this when config
                    config.nicknamePresets.mapValues { (_, v) -> NickPreset(v) }.toSortedMap(),
                ),
            )
            createProfileFeature(database, luckPerms, userCache, chatConfirmations)
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
        // cast ok because we're running on Velocity
        (sender as VelocityCommandIssuer).issuer.sendError(message)
        return true
    }
    // TODO reloading functionality
}

class ChattoreException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
