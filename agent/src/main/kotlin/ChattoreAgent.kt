package org.openredstone.chattore.agent

import org.bukkit.plugin.java.JavaPlugin
import org.openredstone.chattore.agent.feature.startAfkFeature
import org.openredstone.chattore.agent.feature.startAliasFeature
import org.openredstone.chattore.agent.feature.startChatFeature

class ChattoreAgent : JavaPlugin() {
    override fun onEnable() {
        logger.info("Starting AFK feature")
        startAfkFeature(this)
        logger.info("Starting Chat feature")
        startChatFeature(this)
        logger.info("Starting Alias feature")
        startAliasFeature(this)
    }
}
