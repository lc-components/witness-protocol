package de.bcoding.ltc.witness.serialization

import com.google.protobuf.ByteString
import kotlinx.serialization.*

@Serializable
class Data(val bytes: ByteArray) {

    val size: Int
        get() = bytes.size

    override fun toString(): String {
        return bytes.toHEXString()
    }

    fun toBytes() = this.bytes

    fun toByteString() = ByteString.copyFrom(this.bytes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Data) return false

        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }

    @Serializer(forClass = Data::class)
    companion object : KSerializer<Data> {

        private fun wrap(bytes: ByteArray): Data {
            return Data(bytes)
        }

        private fun wrap(hexString: String): Data {
            return Data(hexString.hexToByteArray())
        }

        override fun deserialize(decoder: Decoder): Data {
            return when (decoder) {
                is BinaryDecoder -> wrap(decoder.decodeByteArray())
                else -> wrap(decoder.decodeString())
            }
        }

        override fun serialize(encoder: Encoder, obj: Data) {
            when (encoder) {
                is BinaryEncoder -> encoder.encodeByteArray(obj.bytes)
                else -> encoder.encodeString(obj.toString())
            }
        }
    }
}
