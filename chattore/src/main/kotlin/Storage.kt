package org.openredstone.chattore

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.openredstone.chattore.feature.MailboxItem
import org.openredstone.chattore.feature.NickPreset
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object About : Table("about") {
    val uuid = varchar("about_uuid", 36).uniqueIndex()
    val about = varchar("about_about", 512)
    override val primaryKey = PrimaryKey(uuid)
}

object Mail : Table("mail") {
    val id = integer("mail_id").autoIncrement()
    val timestamp = integer("mail_timestamp")
    val sender = varchar("mail_sender", 36).index()
    val recipient = varchar("mail_recipient", 36).index()
    val read = bool("mail_read").default(false)
    val message = varchar("mail_message", 512)
    override val primaryKey = PrimaryKey(id)
}

object Nick : Table("nick") {
    val uuid = varchar("nick_uuid", 36).uniqueIndex()
    val nick = varchar("nick_nick", 2048)
    override val primaryKey = PrimaryKey(uuid)
}

object UsernameCache : Table("username_cache") {
    val uuid = varchar("cache_user", 36).uniqueIndex()
    val username = varchar("cache_username", 16).index()
    override val primaryKey = PrimaryKey(uuid)
}

object JsonSetting : Table("setting") {
    val uuid = varchar("setting_uuid", 36).index()
    val key = varchar("setting_key", 32).index()
    val value = text("setting_value")
    val uuidKeyIndex = index("setting_uuid_key_index", true, uuid, key)
}

class Setting<T : Any>(val key: String, val default: T)

class Storage(
    dbFile: Path,
) {
    private val cacheLength = 86400 // One day
    private val nicknameCache = ConcurrentHashMap<UUID, Pair<NickPreset, Long>>()

    val database = Database.connect("jdbc:sqlite:${dbFile.toAbsolutePath()}", "org.sqlite.JDBC")

    init {
        initTables()
    }

    private fun initTables() = transaction(database) {
        SchemaUtils.create(
            About, Mail, Nick, UsernameCache, JsonSetting,
        )
    }

    fun setAbout(uuid: UUID, about: String) = transaction(database) {
        About.upsert {
            it[this.uuid] = uuid.toString()
            it[this.about] = about
        }
    }

    fun getAbout(uuid: UUID): String? = transaction(database) {
        About.selectAll().where { About.uuid eq uuid.toString() }.firstOrNull()?.let { it[About.about] }
    }

    fun removeNickname(target: UUID) = transaction(database) {
        Nick.deleteWhere { Nick.uuid eq target.toString() }
        nicknameCache.remove(target)
    }

    fun getNickname(target: UUID): NickPreset? = transaction(database) {
        val nickname = nicknameCache[target]?.first ?: run {
            Nick.selectAll().where { Nick.uuid eq target.toString() }.firstOrNull()?.let { NickPreset(it[Nick.nick]) }
        }
        if (nickname != null) cacheNickname(target, nickname)
        nickname
    }

    fun setNickname(target: UUID, nickname: NickPreset) = transaction(database) {
        Nick.upsert {
            it[this.uuid] = target.toString()
            it[this.nick] = nickname.miniMessageFormat
        }
        cacheNickname(target, nickname)
    }

    private fun cacheNickname(target: UUID, nickname: NickPreset) {
        val now = System.currentTimeMillis() / 1000
        nicknameCache.entries.removeIf { it.value.second + cacheLength < now }
        nicknameCache[target] = Pair(nickname, now)
    }

    fun insertMessage(sender: UUID, recipient: UUID, message: String) = transaction(database) {
        Mail.insert {
            it[this.timestamp] = System.currentTimeMillis().floorDiv(1000).toInt()
            it[this.sender] = sender.toString()
            it[this.recipient] = recipient.toString()
            it[this.message] = message
        }
    }

    fun readMessage(recipient: UUID, id: Int): Pair<UUID, String>? = transaction(database) {
        Mail.selectAll().where { (Mail.id eq id) and (Mail.recipient eq recipient.toString()) }
            .firstOrNull()?.let { toReturn ->
                markRead(id, true)
                UUID.fromString(toReturn[Mail.sender]) to toReturn[Mail.message]
            }
    }

    fun getMessages(recipient: UUID): List<MailboxItem> = transaction(database) {
        Mail.selectAll().where { Mail.recipient eq recipient.toString() }
            .orderBy(Mail.timestamp to SortOrder.DESC).map {
                MailboxItem(
                    it[Mail.id],
                    it[Mail.timestamp],
                    UUID.fromString(it[Mail.sender]),
                    it[Mail.read],
                )
            }
    }

    private fun markRead(id: Int, read: Boolean) = transaction(database) {
        Mail.update({ Mail.id eq id }) {
            it[this.read] = read
        }
    }

    private val settingCache = ConcurrentHashMap<Pair<Setting<*>, UUID>, Any>()

    // these ugly wrappers are needed due to reified generics and to keep settingCache private
    inline fun <reified T : Any> setSetting(setting: Setting<T>, uuid: UUID, value: T) =
        unsafeSetSetting(setting, uuid, value, Json.serializersModule.serializer())

    fun <T : Any> unsafeSetSetting(setting: Setting<T>, uuid: UUID, value: T, serializer: SerializationStrategy<T>) {
        transaction(database) {
            JsonSetting.upsert {
                it[JsonSetting.uuid] = uuid.toString()
                it[key] = setting.key
                it[JsonSetting.value] = Json.encodeToString(serializer, value)
            }
        }
        settingCache[setting to uuid] = value
    }

    inline fun <reified T : Any> getSetting(setting: Setting<T>, uuid: UUID): T =
        unsafeGetSetting(setting, uuid, Json.serializersModule.serializer())

    fun <T : Any> unsafeGetSetting(setting: Setting<T>, uuid: UUID, deserializer: DeserializationStrategy<T>): T {
        val cached = settingCache[setting to uuid]
        @Suppress("UNCHECKED_CAST")
        if (cached != null) return cached as T
        val value = transaction {
            val result = JsonSetting.selectAll().where {
                (JsonSetting.uuid eq uuid.toString()) and (JsonSetting.key eq setting.key)
            }.singleOrNull() ?: return@transaction null
            val jsonString = result[JsonSetting.value]
            Json.decodeFromString(deserializer, jsonString)
        } ?: setting.default
        settingCache[setting to uuid] = value
        return value
    }
}
