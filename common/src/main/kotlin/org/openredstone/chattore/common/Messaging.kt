@file:UseSerializers(UUIDSerializer::class)

package org.openredstone.chattore.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

const val ALIAS_CHANNEL: String = "chattore:alias"

@Serializable
data class AliasMessage(
    val targetPlayer: UUID,
    val command: String,
)

@Serializable
private data class UUIDSurrogate(val high: Long, val low: Long) {
    fun toUUID(): UUID = UUID(high, low)

    companion object {
        fun fromUUID(uuid: UUID) = UUIDSurrogate(uuid.mostSignificantBits, uuid.leastSignificantBits)
    }
}

private object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        UUIDSurrogate.serializer().descriptor

    override fun deserialize(decoder: Decoder): UUID =
        decoder.decodeSerializableValue(UUIDSurrogate.serializer()).toUUID()

    override fun serialize(encoder: Encoder, value: UUID) =
        encoder.encodeSerializableValue(UUIDSurrogate.serializer(), UUIDSurrogate.fromUUID(value))
}
