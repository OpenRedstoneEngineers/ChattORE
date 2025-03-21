package chattore.feature

import chattore.*
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player

data class ChattoreConfig(
    val format: String = "<gold>[</gold><red>ChattORE</red><gold>]</gold> <red><message></red>",
)

fun createChattoreFeature(
    plugin: ChattORE,
    config: ChattoreConfig
): Feature {
    return Feature(
        commands = listOf(Chattore(plugin, config))
    )
}

@CommandAlias("chattore")
class Chattore(
    private val plugin: ChattORE,
    private val config: ChattoreConfig
) : BaseCommand() {

    @Default
    @CatchUnknown
    @Subcommand("version")
    fun version(player: Player) {
        player.sendMessage(
            config.format.render(
                "Version &7${plugin.getVersion()}".legacyDeserialize()
            )
        )
    }

    @Subcommand("reload")
    @CommandPermission("chattore.manage")
    fun reload(player: Player) {
        plugin.reload()
        player.sendMessage(
            config.format.render(
                "Reloaded ChattORE"
            )
        )
    }
}
