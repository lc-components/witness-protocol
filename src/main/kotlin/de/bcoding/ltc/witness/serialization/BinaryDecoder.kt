package de.bcoding.ltc.witness.serialization

import kotlinx.serialization.Decoder
import java.math.BigInteger


interface BinaryDecoder : Decoder {
    fun decodeByteArray(): ByteArray
    fun decodeBigInteger() = BigInteger(decodeByteArray())
}
