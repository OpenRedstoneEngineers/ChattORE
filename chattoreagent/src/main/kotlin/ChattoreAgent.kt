package org.openredstone.chattore.agent

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.openredstone.chattore.common.ALIAS_CHANNEL
import org.openredstone.chattore.common.AliasMessage

class ChatListener : Listener {
    @EventHandler
    fun onChatEvent(event: AsyncPlayerChatEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onJoinEvent(event: PlayerJoinEvent) {
        event.joinMessage = null
    }

    @EventHandler
    fun onLeaveEvent(event: PlayerQuitEvent) {
        event.quitMessage = null
    }
}

class ChattoreAgent : JavaPlugin() {
    override fun onEnable() {
        logger.info("Registering events")
        server.pluginManager.registerEvents(ChatListener(), this)
        logger.info("Registering message listener")
        server.messenger.registerIncomingPluginChannel(this, ALIAS_CHANNEL, this::onPluginMessageReceived)
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun onPluginMessageReceived(channel: String, target: Player, data: ByteArray) {
        if (channel != ALIAS_CHANNEL) return
        val alias = Cbor.decodeFromByteArray<AliasMessage>(data)
        server.getPlayer(alias.targetPlayer)?.let { player ->
            server.dispatchCommand(player, alias.command)
        }
    }
}
