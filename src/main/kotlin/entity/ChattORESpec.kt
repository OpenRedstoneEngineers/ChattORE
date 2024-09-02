package chattore.entity

import com.uchuhimo.konf.ConfigSpec
import chattore.pridePresets
import chattore.birdflopPresets
import chattore.playerPresets

object ChattORESpec : ConfigSpec("") {

    val storage by optional("storage.db")
    val clearNicknameOnChange by optional(true)

    object discord : ConfigSpec() {
        val enable by optional(false)
        val networkToken by optional("nouNetwork")
        val playingMessage by optional("on the ORE Network")
        val channelId by optional(1234L)
        val serverTokens by optional(
            mapOf(
                "serverOne" to "token1",
                "serverTwo" to "token2",
                "serverThree" to "token3"
            )
        )
        val format by optional("`%prefix%` **%sender%**: %message%")
        val chadId by optional(1234L)
    }

    object format : ConfigSpec() {
        val global by optional("<prefix> <gray>|</gray> <hover:show_text:'<username> | <i>Click for more</i>'><yellow><sender></yellow></hover><gray>:</gray> <message>")
        val discord by optional("<dark_aqua>Discord</dark_aqua> <gray>|</gray> <dark_purple><sender></dark_purple><gray>:</gray> <message>")
        val mailReceived by optional("<gold>[</gold><red>From <sender></red><gold>]</gold> <message>")
        val mailSent by optional("<gold>[</gold><red>To <recipient></red><gold>]</gold> <message>")
        val mailUnread by optional("<yellow>You have <red><count></red> unread message(s)! <gold><b><hover:show_text:'View your mailbox'><click:run_command:'/mail mailbox'>Click here to view</click></hover></b></gold>.")
        val messageReceived by optional("<gold>[</gold><red><sender></red> <gold>-></gold> <red>me</red><gold>]</gold> <message>")
        val messageSent by optional("<gold>[</gold><red>me</red> <gold>-></gold> <red><recipient></red><gold>]</gold> <message>")
        val me by optional("<i>* <blue><sender> <message></blue> *</i>")
        // Other Roles: <alt_ranks><newline>
        val playerProfile by optional("<gold><st>  </st> Player Profile <st>  </st></gold><newline>IGN: <ign><newline>Nickname: <nickname><newline>Rank: <rank><newline><gold><st>                        </st></gold><newline>About me: <yellow><about><reset><newline><gold><st>                        </st></gold>")
        val socialSpy by optional("<gold>[</gold><sender> <gold>-></gold> <red><recipient></red><gold>]</gold> <message>")
        val commandSpy by optional("<gold><sender>: <message>")
        val error by optional("<b><red>Oh NO ! </red></b><gray>:</gray> <red><message></red>")
        val chattore by optional("<gold>[</gold><red>ChattORE</red><gold>]</gold> <red><message></red>")
        val help by optional("<gold>[</gold><red>Help</red><gold>]</gold> <red><sender></red><gold>:</gold> <message>")
        val join by optional("<yellow><player> has joined the network")
        val leave by optional("<yellow><player> has left the network")
        val joinDiscord by optional("**<player> has joined the network**")
        val leaveDiscord by optional("**<player> has left the network**")
    }
    val nicknamePresets by optional(
        mapOf(
            "pride" to pridePresets.toGradientMinimessage(),
            "birdflop" to birdflopPresets.toGradientMinimessage(),
            "players" to playerPresets.toGradientMinimessage()
        )
    )

}
