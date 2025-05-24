package org.openredstone.chattore

import co.aikar.commands.InvalidCommandArgument
import com.velocitypowered.api.event.player.ServerPreConnectEvent
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.*

class UserCache(private val database: Database) {
    // TODO fix thread safety?
    private var uuidToName = mapOf<UUID, String>()
    private var nameToUuid = mapOf<String, UUID>()

    fun ensureCachedUsername(user: UUID, username: String) = transaction(database) {
        UsernameCache.upsert {
            it[this.uuid] = user.toString()
            it[this.username] = username
        }
        refresh()
    }

    fun refresh() = transaction(database) {
        uuidToName = UsernameCache.selectAll().associate {
            UUID.fromString(it[UsernameCache.uuid]) to it[UsernameCache.username]
        }
        nameToUuid = uuidToName.entries.associate { (k, v) -> v to k }

    }

    fun fetchUuid(input: String): UUID? = parseUuid(input) ?: nameToUuid[input]

    fun usernameOrNull(uuid: UUID): String? = uuidToName[uuid]
    fun uuidOrNull(username: String): UUID? = nameToUuid[username]

    fun usernameOrUuid(uuid: UUID) = usernameOrNull(uuid) ?: uuid.toString()

    // TODO: inline?
    fun usernameOrUuid(u: User) = usernameOrUuid(u.uuid)

    val usernames get() = uuidToName.values
    val uuids get() = nameToUuid.values

    companion object {
        const val COMPLETION_USERNAMES = "usernameCache"
        const val COMPLETION_USERNAMES_AND_UUIDS = "uuidAndUsernameCache"
    }
}

fun PluginScope.createUserCache(database: Database): UserCache = UserCache(database).also { cache ->
    cache.refresh()
    onEvent<ServerPreConnectEvent> { event ->
        cache.ensureCachedUsername(event.player.uniqueId, event.player.username)
    }
    commandManager.apply {
        commandContexts.registerContext(User::class.java) {
            cache.fetchUuid(it.popFirstArg())?.let(::User)
                ?: throw InvalidCommandArgument("Don't know that user")
        }
        commandContexts.registerContext(KnownUser::class.java) {
            val uuid = cache.fetchUuid(it.popFirstArg()) ?: throw InvalidCommandArgument("Don't know that user")
            val ign = cache.usernameOrNull(uuid) ?: throw InvalidCommandArgument("Don't know that user")
            KnownUser(uuid, ign)
        }
        commandCompletions.registerCompletion(UserCache.COMPLETION_USERNAMES) { cache.usernames }
        commandCompletions.registerCompletion(UserCache.COMPLETION_USERNAMES_AND_UUIDS) {
            cache.usernames + cache.uuids.map(UUID::toString)
        }
        // TODO check this
        commandCompletions.setDefaultCompletion(UserCache.COMPLETION_USERNAMES, User::class.java, KnownUser::class.java)
    }
}

// idk what to call it
class KnownUser(val uuid: UUID, val name: String)
class User(val uuid: UUID)
