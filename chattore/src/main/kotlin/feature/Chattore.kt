package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player
import org.openredstone.chattore.PluginScope
import org.openredstone.chattore.VERSION
import org.openredstone.chattore.sendInfo
import org.openredstone.chattore.sendInfoMM

fun PluginScope.createChattoreFeature() {
    registerCommands(Chattore())
}

@CommandAlias("chattore")
private class Chattore : BaseCommand() {
    @Default
    @CatchUnknown
    @Subcommand("version")
    fun version(player: Player) {
        player.sendInfoMM("Version <gray>$VERSION")
    }

    @Subcommand("reload")
    @CommandPermission("chattore.manage")
    fun reload(player: Player) {
        player.sendInfo("Not implemented yet :(")
    }
}
