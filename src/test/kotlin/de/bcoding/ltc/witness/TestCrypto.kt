package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*
import de.bcoding.ltc.witness.serialization.Data
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.builtins.serializer
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.random.Random
import kotlin.random.nextUBytes
import kotlin.reflect.KClass


fun <T : Any> KClass<T>.myserializer(): KSerializer<T> = when (this) {
    Protocol::class -> Protocol.serializer() as KSerializer<T>
    KeyPair::class -> KeyPair.serializer() as KSerializer<T>
    Document::class -> Document.serializer() as KSerializer<T>
    DocumentSeen::class -> DocumentSeen.serializer() as KSerializer<T>
    ProtocolCreation::class -> ProtocolCreation.serializer() as KSerializer<T>
    Testified::class -> Testified.serializer(ProtocolCreation.serializer()) as KSerializer<T>
    PrivateKey::class -> PrivateKey.serializer() as KSerializer<T>
    PublicKey::class -> PublicKey.serializer() as KSerializer<T>
    PersonData::class -> PersonData.serializer() as KSerializer<T>
    String::class -> String.serializer() as KSerializer<T>
    else -> throw IllegalArgumentException("No serializer for class ${this}")
}

class TestCrypto(val format: SerialFormat) : Crypto {
    override fun createKeyPair(): KeyPair {
        return KeyPair(
            PublicKey("dummy", Data("pub".toByteArray())),
            PrivateKey("dummy", Data("private".toByteArray()))
        )
    }

    @ExperimentalStdlibApi
    override fun <T : Any> decrypt(encrypted: Encrypted<T>, pubKey: PublicKey): T {
        val serializer = encrypted.type!!.myserializer()
        return when (format) {
            is BinaryFormat -> format.load(serializer, encrypted.data.bytes)
            is StringFormat -> format.parse(serializer, encrypted.data.bytes.decodeToString())
            else -> throw IllegalStateException("Bad format")
        }
    }

    @ExperimentalStdlibApi
    override fun <T : Any> decrypt(encrypted: Encrypted<T>, privateKey: PrivateKey): T {
        val serializer = encrypted.type!!.myserializer()
        return when (format) {
            is BinaryFormat -> format.load(serializer, encrypted.data.bytes)
            is StringFormat -> format.parse(serializer, encrypted.data.bytes.decodeToString())
            else -> throw IllegalStateException("Bad format")
        }
    }

    override fun <T : Any> encrypt(pubKey: PublicKey, subject: T): Encrypted<T> {
        val serial = subject::class.myserializer() as KSerializer<T>
        val result = Encrypted(
            true,
            serializeData(format, serial, subject),
            pubKey,
            subject::class
        )
        return result as Encrypted<T>
    }

    private fun <T : Any> serializeData(
        format: SerialFormat,
        serial: KSerializer<T>,
        subject: T
    ): Data {
        return when (format) {
            is BinaryFormat -> Data(format.dump(serial, subject))
            is StringFormat -> Data(format.stringify(serial, subject).toByteArray())
            else -> throw IllegalStateException("Bad format")
        }
    }

    override fun <T : Any> encrypt(keyPair: KeyPair, subject: T): Encrypted<T> {
        val serial = subject::class.myserializer() as KSerializer<T>
        val result = Encrypted(
            false,
            serializeData(format, serial, subject),
            keyPair.publicKey,
            subject::class
        )
        return result as Encrypted<T>
    }

    override fun randomString(size: Int): String = Random.nextUBytes(20).toString()

    override fun calculateHashCode(subject: Encrypted<*>): HashCode {
        val digest = MessageDigest.getInstance("SHA-256")
        return HashCode("SHA-256", Data(digest.digest(subject.data.bytes)))
    }

    override fun calculateHashCode(subject: Document): HashCode {
        val digest = MessageDigest.getInstance("SHA-256")
        return HashCode("SHA-256", Data(digest.digest(subject.data.bytes)))
    }

    override fun <T : Any> testify(subject: T, keys: KeyPair): Testified<T> {
        return Testified(
            subject,
            LocalDateTime.now(),
            Signature(HashCode("md5", Data("dummy".toByteArray())), keys.publicKey)
        )
    }
}