package chattore.feature

import chattore.Feature
import chattore.Messenger
import chattore.render
import chattore.toComponent
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerPostConnectEvent

data class JoinLeaveConfig(
    val join: String = "<yellow><player> has joined the network",
    val leave: String = "<yellow><player> has left the network",
    val joinDiscord: String = "**%player% has joined the network**",
    val leaveDiscord: String = "**%player% has left the network**",
)

fun createJoinLeaveFeature(
    messenger: Messenger,
    eventManager: EventManager,
    config: JoinLeaveConfig
): Feature {
    return Feature(
        listeners = listOf(JoinLeaveListener(messenger, eventManager, config))
    )
}

class JoinLeaveListener(
    private val messenger: Messenger,
    private val eventManager: EventManager,
    private val config: JoinLeaveConfig
) {

    @Subscribe
    fun joinEvent(event: ServerPostConnectEvent) {
        if (event.previousServer != null) return
        val username = event.player.username
        messenger.broadcast(
            config.join.render(mapOf(
                "player" to username.toComponent()
            ))
        )
        val discordConnectEvent = DiscordBroadcastEventMain(
            config.joinDiscord,
            username,
        )
        eventManager.fireAndForget(discordConnectEvent)
    }

    @Subscribe
    fun leaveMessage(event: DisconnectEvent) {
        if (event.loginStatus != DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN) return
        val username = event.player.username
        messenger.broadcast(
            config.leave.render(mapOf(
                "player" to username.toComponent()
            ))
        )
        val discordDisconnectEvent = DiscordBroadcastEventMain(
            config.leaveDiscord,
            username,
        )
        eventManager.fireAndForget(discordDisconnectEvent)
    }
}
