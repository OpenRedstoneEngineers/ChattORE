package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.velocitypowered.api.proxy.Player
import org.openredstone.chattore.ChattoreException
import org.openredstone.chattore.PluginScope
import org.openredstone.chattore.sendInfoMM

fun PluginScope.createEmojiFeature(): Emojis {
    val emojis = loadResourceAsString("emojis.csv").lineSequence().associate { item ->
        val parts = item.split(",")
        parts[0] to parts[1]
    }
    logger.info("Loaded ${emojis.size} emojis")
    commandManager.commandCompletions.registerCompletion("emojis") { emojis.keys }
    registerCommands(EmojiCommand(emojis))
    return Emojis(
        nameToEmoji = emojis,
        emojiToName = emojis.entries.associateBy({ it.value }) { it.key },
    )
}

class Emojis(val nameToEmoji: Map<String, String>, val emojiToName: Map<String, String>) {
    companion object {
        const val COMPLETION_EMOJI = "emojis"
    }
}

@CommandAlias("emoji")
@Description("Preview an emoji")
@CommandPermission("chattore.emoji")
private class EmojiCommand(
    private val emojis: Map<String, String>,
) : BaseCommand() {
    @Default
    @CommandCompletion("@${Emojis.COMPLETION_EMOJI}")
    fun default(player: Player, vararg emojiNames: String) {
        if (!emojis.keys.containsAll(emojiNames.toSet())) {
            val notEmoji = emojiNames.toSet().minus(emojis.keys)
            throw ChattoreException("The following are not valid emojis: ${notEmoji.joinToString(", ")}")
        }
        val emojiMiniMessage = emojiNames.toSet().intersect(emojis.keys).joinToString(", ") {
            "<hover:show_text:$it>${emojis[it]}</hover>"
        }
        player.sendInfoMM("Emojis: $emojiMiniMessage")
    }
}
