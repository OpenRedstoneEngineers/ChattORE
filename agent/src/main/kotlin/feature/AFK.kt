package org.openredstone.chattore.agent.feature

import io.papermc.paper.event.player.AsyncChatEvent
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun getTime() = System.currentTimeMillis() / 1000

class ChattoreExpansion(private val afkUsers: MutableSet<UUID>) : PlaceholderExpansion() {

    private val badge = "[AFK]"

    override fun getIdentifier(): String = "chattore"

    override fun getAuthor(): String = "Open Redstone Engineers"

    override fun getVersion(): String = "1.0.0"

    override fun onRequest(player: OfflinePlayer, params: String): String? {
        return when (params) {
            "afk_badge" -> {
                if (player.uniqueId in afkUsers) {
                    return badge
                }
                return ""
            }

            else -> null
        }
    }

}

class AfkListener(private val afkMap: ConcurrentHashMap<UUID, Long>) : Listener {
    @EventHandler
    fun onMoveEvent(event: PlayerMoveEvent) {
        afkMap[event.player.uniqueId] = getTime()
    }

    @EventHandler
    fun onQuitEvent(event: PlayerQuitEvent) {
        afkMap.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onJoinEvent(event: PlayerJoinEvent) {
        afkMap[event.player.uniqueId] = getTime()
    }

    @EventHandler
    fun onChatEvent(event: AsyncChatEvent) {
        afkMap[event.player.uniqueId] = getTime()
    }
}

fun startAfkFeature(plugin: JavaPlugin) {
    val afkMap: ConcurrentHashMap<UUID, Long> = ConcurrentHashMap()
    val afkPlayers: MutableSet<UUID> = mutableSetOf()
    startAfkTimer(plugin, afkMap, afkPlayers)
    plugin.server.pluginManager.registerEvents(AfkListener(afkMap), plugin)
    plugin.server.pluginManager.getPlugin("PlaceholderAPI")?.let { ChattoreExpansion(afkPlayers).register() }
        ?: throw RuntimeException("No PlaceholderAPI found; not loading placeholders")
}

fun startAfkTimer(
    plugin: JavaPlugin,
    afkMap: ConcurrentHashMap<UUID, Long>,
    afkPlayers: MutableSet<UUID>,
) {
    plugin.server.scheduler.scheduleSyncRepeatingTask(plugin, {
        afkMap.forEach { (uuid, lastActive) ->
            if (lastActive + 300 < getTime()) {
                afkPlayers.add(uuid)
            } else {
                afkPlayers.remove(uuid)
            }
        }
    }, 600L, 600L)
}
