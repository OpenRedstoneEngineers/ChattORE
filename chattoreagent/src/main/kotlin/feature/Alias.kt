package org.openredstone.chattore.agent.feature

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.bukkit.plugin.java.JavaPlugin
import org.openredstone.chattore.common.ALIAS_CHANNEL
import org.openredstone.chattore.common.AliasMessage

@OptIn(ExperimentalSerializationApi::class)
fun startAliasFeature(plugin: JavaPlugin) {
    plugin.server.messenger.registerIncomingPluginChannel(plugin, ALIAS_CHANNEL) { channel, _, data ->
        if (channel != ALIAS_CHANNEL) return@registerIncomingPluginChannel
        val alias = Cbor.decodeFromByteArray<AliasMessage>(data)
        plugin.server.getPlayer(alias.targetPlayer)?.let { target ->
            plugin.server.dispatchCommand(target, alias.command)
        }
    }
}
