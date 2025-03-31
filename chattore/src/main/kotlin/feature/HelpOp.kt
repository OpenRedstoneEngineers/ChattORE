package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Syntax
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import org.openredstone.chattore.*
import org.slf4j.Logger

fun PluginScope.createHelpOpFeature() {
    registerCommands(HelpOp(logger, proxy))
}

@CommandAlias("helpop|ac")
@CommandPermission("chattore.helpop")
private class HelpOp(
    private val logger: Logger,
    private val proxy: ProxyServer,
) : BaseCommand() {
    @Default
    @Syntax("[message]")
    fun default(player: Player, statement: String) {
        if (statement.isEmpty()) throw ChattoreException("You have to have a problem first!") // : )
        logger.info("[HelpOp] ${player.username}: $statement")
        proxy.all { it.hasChattorePrivilege || it.uniqueId == player.uniqueId }
            .sendRichMessage(
                "<gold>[</gold><red>Help</red><gold>]</gold> <red><sender></red><gold>:</gold> <message>",
                "message" toS statement,
                "sender" toS player.username,
            )
    }
}
