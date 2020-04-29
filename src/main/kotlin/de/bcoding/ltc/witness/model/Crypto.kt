package de.bcoding.ltc.witness.model

import de.bcoding.ltc.witness.serialization.Data
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlin.reflect.KClass

@Serializable
data class HashCode(val algorithm: String, val data: Data)

val json = Json(JsonConfiguration.Stable)

@Serializable
data class Signature(
    val hashCode: HashCode,
    val accessKey: PublicKey
) : Encrypted<HashCode>(
    false,
    Data(json.stringify(HashCode.serializer(), hashCode).toByteArray()),
    accessKey,
    HashCode::class
)


/**
 * data contains a serialized version of the object
 */
@Serializable
open class Encrypted<OBJ : Any>(
    val isPublicKeyEncrypted: Boolean,
    val data: Data,
    val publicKey: PublicKey,
    @Transient
    var type: KClass<OBJ>? = null
) {
    fun decrypt(key: PrivateKey, crypto: Crypto): OBJ {
        return crypto.decrypt(this, key)
    }
}

@Serializable
data class PublicKey(val algorithm: String, val key: Data)

@Serializable
data class PrivateKey(val algorithm: String, val key: Data)

@Serializable
class KeyPair(
    val publicKey: PublicKey,
    val privateKey: PrivateKey
)

interface Crypto {
    fun <T : Any> decrypt(encrypted: Encrypted<T>, pubKey: PublicKey): T
    fun <T : Any> decrypt(encrypted: Encrypted<T>, pubKey: PrivateKey): T
    fun <T : Any> encrypt(pubKey: PublicKey, subject: T): Encrypted<T>
    fun <T : Any> encrypt(keys: KeyPair, subject: T): Encrypted<T>
    fun randomString(size: Int): String
    fun calculateHashCode(document: Encrypted<*>): HashCode
    fun calculateHashCode(document: Document): HashCode
    fun <T : Any> testify(subject: T, keys: KeyPair): Testified<T>
    fun createKeyPair(): KeyPair
}