package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.format.NamedTextColor.GRAY
import org.openredstone.chattore.*

fun PluginScope.createChattoreFeature() {
    registerCommands(Chattore())
}

@CommandAlias("chattore")
private class Chattore : BaseCommand() {
    @Default
    @CatchUnknown
    @Subcommand("version")
    fun version(player: Player) {
        player.sendInfo(c("Version ".c, BuildConfig.VERSION[GRAY]))
    }

    @Subcommand("reload")
    @CommandPermission("chattore.manage")
    fun reload(player: Player) {
        player.sendInfo("Not implemented yet :(")
    }
}
