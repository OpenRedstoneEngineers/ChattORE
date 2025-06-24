package org.openredstone.chattore.feature

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Subcommand
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor.GOLD
import net.kyori.adventure.text.format.NamedTextColor.YELLOW
import net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH
import net.luckperms.api.LuckPerms
import org.openredstone.chattore.*
import net.luckperms.api.model.user.User as LPUser

fun PluginScope.createProfileFeature(
    database: Storage,
    luckPerm: LuckPerms,
    userCache: UserCache,
) {
    registerCommands(Profile(proxy, database, luckPerm, userCache))
}

@CommandAlias("profile|playerprofile")
@CommandPermission("chattore.profile")
private class Profile(
    private val proxy: ProxyServer,
    private val database: Storage,
    private val luckPerms: LuckPerms,
    private val userCache: UserCache,
) : BaseCommand() {

    @Subcommand("info")
    @CommandCompletion("@${UserCache.COMPLETION_USERNAMES_AND_UUIDS}")
    fun profile(player: Player, target: KnownUser) {
        // TODO this might fail
        luckPerms.userManager.loadUser(target.uuid).whenComplete { user, _ ->
            player.sendMessage(parsePlayerProfile(user, target.name))
        }
    }

    @Subcommand("about")
    @CommandPermission("chattore.profile.about")
    fun about(player: Player, about: String) {
        database.setAbout(player.uniqueId, about)
        player.sendInfo("Set your about to '$about'.")
    }

    @Subcommand("setabout")
    @CommandPermission("chattore.profile.about.others")
    // TODO do we want to complete uuids too here?
    @CommandCompletion("@${UserCache.COMPLETION_USERNAMES_AND_UUIDS}")
    fun setAbout(player: Player, target: User, about: String) {
        database.setAbout(target.uuid, about)
        player.sendInfo("Set about for '${userCache.usernameOrUuid(target)}' to '$about'.")
        proxy.playerOrNull(target.uuid)?.sendInfo("Your about has been set to '$about'")
    }

    private fun parsePlayerProfile(user: LPUser, ign: String): Component {
        var group = user.primaryGroup
        luckPerms.groupManager.getGroup(user.primaryGroup)?.let {
            it.cachedData.metaData.prefix?.let { prefix -> group = prefix }
        }
        val about = database.getAbout(user.uniqueId) ?: "no about yet :("
        val nickname = database.getNickname(user.uniqueId)?.render(ign) ?: "No nickname set".c
        val rank = group.legacyDeserialize()
        return listOf(
            "  ".c(GOLD + STRIKETHROUGH) + " Player Profile ".c(GOLD) + "  ".c(GOLD + STRIKETHROUGH),
            "IGN: $ign".c,
            "Nickname: ".c + nickname,
            "Rank: ".c + rank,
            "                        ".c(GOLD + STRIKETHROUGH),
            "About me: ".c + about.c(YELLOW),
            "                        ".c(GOLD + STRIKETHROUGH),
        ).join(Component.newline())
    }
}
