package chattore.commands

import chattore.ChattORE
import chattore.render
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player
import chattore.entity.ChattORESpec
import chattore.legacyDeserialize

@CommandAlias("chattore")
@CommandPermission("chattore.manage")
class Chattore(private val chattORE: ChattORE) : BaseCommand() {
    @Default
    @CatchUnknown
    @Subcommand("version")
    fun version(player: Player) {
        player.sendMessage(
            chattORE.config[ChattORESpec.format.chattore].render(
                "Version &7${chattORE.getVersion()}".legacyDeserialize()
            )
        )
    }

    @Subcommand("reload")
    fun reload(player: Player) {
        chattORE.reload()
        player.sendMessage(
            chattORE.config[ChattORESpec.format.chattore].render(
                "Reloaded ChattORE"
            )
        )
    }
}