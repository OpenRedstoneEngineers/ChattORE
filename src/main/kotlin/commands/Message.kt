package chattore.commands

import chattore.*
import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import com.uchuhimo.konf.Config
import com.velocitypowered.api.proxy.Player
import chattore.entity.ChattORESpec
import org.slf4j.Logger
import java.util.*

@CommandAlias("msg|message|vmsg|vmessage|whisper|tell")
@CommandPermission("chattore.message")
class Message(
    private val config: Config,
    private val chattORE: ChattORE,
    private val replyMap: MutableMap<UUID, UUID>
) : BaseCommand() {

    @Default
    @Syntax("[target] <message>")
    @CommandCompletion("@players")
    fun default(player: Player, target: String, args: Array<String>) {
        chattORE.proxy.getPlayer(target).ifPresentOrElse({ targetPlayer ->
            sendMessage(chattORE.logger, replyMap, config, player, targetPlayer, args)
        }, {
            throw ChattoreException("That user doesn't exist!")
        })
    }
}

// I don't like putting this here but eggsdee we'll figure out a better place later
fun sendMessage(
    logger: Logger,
    replyMap: MutableMap<UUID, UUID>,
    config: Config,
    player: Player,
    targetPlayer: Player,
    args: Array<String>
) {
    val statement = args.joinToString(" ")
    logger.info("${player.username} -> ${targetPlayer.username}: $statement")
    player.sendMessage(
        config[ChattORESpec.format.messageSent].render(
            mapOf(
                "message" to statement.legacyDeserialize(),
                "recipient" to targetPlayer.username.toComponent()
            )
        )
    )
    targetPlayer.sendMessage(
        config[ChattORESpec.format.messageReceived].render(
            mapOf(
                "message" to statement.legacyDeserialize(),
                "sender" to player.username.toComponent()
            )
        )
    )
    replyMap[targetPlayer.uniqueId] = player.uniqueId
    replyMap[player.uniqueId] = targetPlayer.uniqueId
}
