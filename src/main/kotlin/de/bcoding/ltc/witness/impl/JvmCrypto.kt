package de.bcoding.ltc.witness.impl

import de.bcoding.ltc.witness.model.*
import de.bcoding.ltc.witness.serialization.Data
import java.security.KeyPairGenerator

class JvmCrypto : Crypto {
    override fun <T : Any> decrypt(encrypted: Encrypted<T>, pubKey: PublicKey): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> decrypt(encrypted: Encrypted<T>, pubKey: PrivateKey): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> encrypt(pubKey: PublicKey, subject: T): Encrypted<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any> encrypt(keys: KeyPair, subject: T): Encrypted<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun randomString(size: Int): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateHashCode(subject: Encrypted<*>): HashCode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateHashCode(document: Document): HashCode {
        TODO("Not yet implemented")
    }

    override fun <T : Any> testify(subject: T, keys: KeyPair): Testified<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("DSA", "SUN")
        val keyPair = keyGen.generateKeyPair()
        return KeyPair(PublicKey("DSA", Data(keyPair.public.encoded)), PrivateKey("DSA", Data(keyPair.private.encoded)))
    }

}