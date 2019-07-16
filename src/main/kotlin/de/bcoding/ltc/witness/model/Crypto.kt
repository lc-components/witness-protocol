package de.bcoding.ltc.witness.model

class HashCode(val bytes: ByteArray) {
    fun toByteArray(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class Signature(
    data: HashCode,
    accessKey: PublicKey
) : Encrypted<HashCode>(true, data.toByteArray(), accessKey)

/**
 * data contains a serialized version of the object
 */
open class Encrypted<OBJ>(
    val isPublicKeyEncrypted: Boolean,
    val data: ByteArray,
    val publicKey: PublicKey
) : Data {
    override fun toByteArray(): ByteArray {
        return data
    }
}

class PublicKey(val key: java.security.PublicKey)
class PrivateKey(val key: java.security.PrivateKey)

class KeyPair(
    val publicKey: PublicKey,
    val privateKey: PrivateKey
)

class Crypto {
    companion object {


        @JvmStatic
        fun <T> decrypt(encrypted: Encrypted<T>, pubKey: PublicKey): T {
            TODO("not implemented")
        }

        @JvmStatic
        fun <T> decrypt(encrypted: Encrypted<T>, pubKey: PrivateKey): T {
            TODO("not implemented")
        }

        @JvmStatic
        fun <T> encrypt(pubKey: PublicKey, subject: T): Encrypted<T> {
            TODO("not implemented")
        }

        @JvmStatic
        fun <T> encrypt(pubKey: PrivateKey, subject: T): Encrypted<T> {
            TODO("not implemented")
        }

        @JvmStatic
        fun randomString(size: Int): String {
            TODO("not implemented")
        }

        @JvmStatic
        fun calculateHashCode(subject: Data): HashCode {
            TODO("not implemented")
        }

        @JvmStatic
        fun <T> testify(subject: T): Testified<T> {
            TODO("not implemented")
        }
    }
}