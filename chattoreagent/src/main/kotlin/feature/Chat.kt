package org.openredstone.chattore.agent.feature

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

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

fun startChatFeature(plugin: JavaPlugin) {
    plugin.server.pluginManager.registerEvents(ChatListener(), plugin)
}
