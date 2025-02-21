package chattore

import chattore.entity.ChattORESpec
import chattore.feature.*
import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandIssuer
import co.aikar.commands.InvalidCommandArgument
import co.aikar.commands.RegisteredCommand
import co.aikar.commands.VelocityCommandManager
import com.google.inject.Inject
import com.uchuhimo.konf.Config
import com.uchuhimo.konf.source.yaml
import com.uchuhimo.konf.source.yaml.toYaml
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.TextReplacementConfig
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path
import java.util.*

data class Feature(
    val commands: List<BaseCommand> = emptyList(),
    val listeners: List<Any> = emptyList(),
    val completions: List<Any> = emptyList(), // Could be useful, but currently not used
    val unload: () -> Unit = {},
)

const val VERSION = "1.2"

@Plugin(
    id = "chattore",
    name = "ChattORE",
    version = VERSION,
    url = "https://openredstone.org",
    description = "Because we want to have a chat system that actually wOREks for us.",
    authors = ["Nickster258", "PaukkuPalikka", "StackDoubleFlow", "sodiboo", "Waffle [Wueffi]"],
    dependencies = [Dependency(id = "luckperms")]
)
class ChattORE @Inject constructor(val proxy: ProxyServer, val logger: Logger, @DataDirectory dataFolder: Path) {
    lateinit var luckPerms: LuckPerms
    lateinit var config: Config
    lateinit var database: Storage
    lateinit var messenger: Messenger
    private lateinit var emojis: Map<String, String>
    lateinit var emojisToNames: Map<String, String>
    private val dataFolder = dataFolder.toFile()
    val chatReplacements: MutableList<TextReplacementConfig> = mutableListOf(
        formatReplacement("**", "b"),
        formatReplacement("*", "i"),
        formatReplacement("__", "u"),
        formatReplacement("~~", "st")
    )

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        config = loadConfig()
        luckPerms = LuckPermsProvider.get()
        database = Storage(this.dataFolder.resolve(config[ChattORESpec.storage]).toString())
        val pluginEvents = PluginEvents(this, proxy.eventManager)
        val userCache = UserCache.create(database.database, pluginEvents)
        emojis = loadResource("/emojis.csv").lineSequence().associate { item ->
            val parts = item.split(",")
            parts[0] to parts[1]
        }
        emojisToNames = emojis.entries.associateBy({ it.value }) { it.key }
        chatReplacements.add(buildEmojiReplacement(emojis))
        logger.info("Loaded ${emojis.size} emojis")

        messenger = Messenger(this, config[ChattORESpec.format.global])

        // command manager lol
        val commandManager = VelocityCommandManager(proxy, this).apply {
            setDefaultExceptionHandler(::handleCommandException, false)
            // TODO move to the place
            commandContexts.registerContext(User::class.java) {
                userCache.fetchUuid(it.popFirstArg())?.let(::User)
                    ?: throw InvalidCommandArgument("Don't know that user")
            }
            commandContexts.registerContext(KnownUser::class.java) {
                val uuid = userCache.fetchUuid(it.popFirstArg()) ?: throw InvalidCommandArgument("Don't know that user")
                val ign = userCache.usernameOrNull(uuid) ?: throw InvalidCommandArgument("Don't know that user")
                KnownUser(uuid, ign)
            }
            commandCompletions.registerCompletion("usernameCache") { userCache.usernames }
            commandCompletions.registerCompletion("uuidAndUsernameCache") {
                userCache.usernames + userCache.uuids.map(UUID::toString)
            }
            // TODO check this
            commandCompletions.setDefaultCompletion("usernameCache", User::class.java, KnownUser::class.java)

            commandCompletions.registerCompletion("bool") { listOf("true", "false") }
            commandCompletions.registerCompletion("colors") { ctx ->
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
            commandCompletions.registerCompletion("emojis") { emojis.keys }
            commandCompletions.registerCompletion("username") { listOf(it.player.username) }
            commandCompletions.registerCompletion("nickPresets") { config[ChattORESpec.nicknamePresets].keys }
        }
        val features = listOf(
            createChatFeature(
                logger, messenger, proxy.eventManager, ChatConfirmationConfig(
                    config[ChattORESpec.regexes],
                    config[ChattORESpec.format.chatConfirmPrompt],
                    config[ChattORESpec.format.chatConfirm],
                )
            ),
            createChattoreFeature(this),
            createDiscordFeature(
                this, DiscordConfig(
                    config[ChattORESpec.discord.enable],
                    config[ChattORESpec.discord.networkToken],
                    config[ChattORESpec.discord.channelId],
                    config[ChattORESpec.discord.chadId],
                    config[ChattORESpec.discord.playingMessage],
                    config[ChattORESpec.discord.format],
                    config[ChattORESpec.discord.serverTokens],
                    config[ChattORESpec.format.discord],
                )
            ),
            createEmojiFeature(emojis),
            createFunCommandsFeature(logger, messenger, proxy.commandManager),
            createHelpOpFeature(
                logger, messenger, HelpOpConfig(
                    config[ChattORESpec.format.help],
                )
            ),
            createJoinLeaveFeature(
                messenger, proxy.eventManager, JoinLeaveConfig(
                    config[ChattORESpec.format.join],
                    config[ChattORESpec.format.leave],
                    config[ChattORESpec.format.joinDiscord],
                    config[ChattORESpec.format.leaveDiscord],
                )
            ),
            createMailFeature(
                this, userCache, MailConfig(
                    config[ChattORESpec.format.mailReceived],
                    config[ChattORESpec.format.mailSent],
                    config[ChattORESpec.format.mailUnread],
                )
            ),
            createMessageFeature(
                proxy, logger, messenger, MessageConfig(
                    config[ChattORESpec.format.messageReceived],
                    config[ChattORESpec.format.messageSent],
                )
            ),
            createNicknameFeature(
                proxy, database, userCache, NicknameConfig(
                    config[ChattORESpec.clearNicknameOnChange],
                    // IDK, this when config
                    config[ChattORESpec.nicknamePresets].mapValues { (_, v) -> NickPreset(v) }.toSortedMap(),
                )
            ),
            createProfileFeature(
                proxy, database, luckPerms, userCache, ProfileConfig(
                    config[ChattORESpec.format.playerProfile],
                )
            ),
            createSpyingFeature(
                database, messenger, SpyingConfig(
                    config[ChattORESpec.format.commandSpy],
                )
            )
        )
        features.forEach { (commands, listeners, completions) ->
            commands.forEach(commandManager::registerCommand)
            //completions.forEach { (it, handler) -> commandManager.commandCompletions.registerCompletion(it) { handler } }
            listeners.forEach { proxy.eventManager.register(this, it) }
        }
        logger.info("Loaded ${features.size} features")
    }

    private fun loadConfig(reloaded: Boolean = false): Config {
        if (!dataFolder.exists()) {
            logger.info("No resource directory found, creating directory")
            dataFolder.mkdir()
        }
        val configFile = File(dataFolder, "config.yml")
        val loadedConfig = if (!configFile.exists()) {
            logger.info("No config file found, generating from default config.yml")
            configFile.createNewFile()
            Config { addSpec(ChattORESpec) }
        } else {
            Config { addSpec(ChattORESpec) }.from.yaml.watchFile(configFile)
        }
        loadedConfig.toYaml.toFile(configFile)
        logger.info("${if (reloaded) "Rel" else "L"}oaded config.yml")
        return loadedConfig
    }

    private fun handleCommandException(
        command: BaseCommand,
        registeredCommand: RegisteredCommand<*>,
        sender: CommandIssuer,
        args: List<String>,
        throwable: Throwable
    ): Boolean {
        val exception = throwable as? ChattoreException ?: return false
        val message = exception.message ?: "Something went wrong!"
        if (sender is Player) {
            sender.sendSimpleS(config[ChattORESpec.format.error], message)
        } else {
            sender.sendMessage("Error: $message")
        }
        return true
    }

    fun getVersion(): String {
        return VERSION
    }

    fun reload() {
        // TODO
    }
}

class ChattoreException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
