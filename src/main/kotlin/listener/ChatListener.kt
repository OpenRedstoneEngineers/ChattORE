package chattore.listener

import chattore.ChattORE
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.TabCompleteEvent
import com.velocitypowered.api.proxy.Player
import chattore.entity.ChattORESpec
import chattore.render
import chattore.toComponent
import com.velocitypowered.api.event.player.ServerPreConnectEvent

class ChatListener(
    private val chattORE: ChattORE
) {
    @Subscribe
    fun onTabComplete(event: TabCompleteEvent) {
        // TODO: Autocomplete player names and stuff idk
        event.suggestions.clear()
    }

    @Subscribe
    fun onJoin(event: ServerPreConnectEvent) {
        chattORE.database.ensureCachedUsername(
            event.player.uniqueId,
            event.player.username
        )
    }

    @Subscribe
    fun onChatEvent(event: PlayerChatEvent) {
        event.result = PlayerChatEvent.ChatResult.denied()

        val pp = event.player
        pp.currentServer.ifPresent { server ->
            chattORE.logger.info("${pp.username}: ${event.message}")
            chattORE.broadcastChatMessage(server.serverInfo.name, pp.uniqueId, event.message)
        }
    }

    @Subscribe
    fun onCommandEvent(event: CommandExecuteEvent) {
        chattORE.sendPrivileged(
            chattORE.config[ChattORESpec.format.commandSpy].render(
                mapOf(
                    "message" to event.command.toComponent(),
                    "sender" to ((event.commandSource as? Player)?.username ?: "Console").toComponent()
                )
            )
        )
    }
}