package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*
import de.bcoding.ltc.witness.serialization.Data
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.KClass

@ImplicitReflectionSerializer
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImplementationTest {

    private val storage: Storage = object : Storage {

        val protocols = HashMap<String, Protocol>()

        override fun createProtocol(key: String, protocol: Protocol) {
            protocols[key] = protocol
        }

        override fun getProtocol(protocolKey: String): Protocol {
            return protocols[protocolKey]!!
        }

        override fun saveProtocol(protocol: Protocol) {
            protocols[protocol.key] = protocol
        }
    }

    val messageModule = SerializersModule {
        polymorphic(Any::class, ProtocolCreation::class, DocumentSeen::class) {
            ProtocolCreation::class with ProtocolCreation.serializer()
            DocumentSeen::class with DocumentSeen.serializer()
        }
    }

    private val crypto = TestCrypto(
        Json(JsonConfiguration(prettyPrint = true, useArrayPolymorphism = true), messageModule)
    )

    val wittnessKeyPair = CryptoId(crypto).pair
    private val witness: Witness = Witness(wittnessKeyPair.publicKey)

    class TestSetup(val witness: Witness, val storage: Storage, val crypto: Crypto) {

        lateinit var protocolKey: String
        val eventBus: EventBus = object : EventBus {

            val listeners = mutableMapOf<KClass<*>, (Any) -> Unit>()

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> listenTo(kClass: KClass<T>, function: (T) -> Unit) {
                listeners[kClass] = function as (Any) -> Unit
            }

            override fun publish(event: Any) {
                events.add(event)
                if (listeners.containsKey(event::class)) {
                    listeners[event::class]!!.invoke(event)
                }
            }

        }
        val appHash = randomHash()
        val documentCrypto = CryptoId(crypto)
        val signerCrypto1 = CryptoId(crypto)
        val signerCrypto2 = CryptoId(crypto)
        val documentData = "test data".toByteArray()
        val document = Document(Data(documentData))
        val encryptedDocument = crypto.encrypt(documentCrypto.pair.publicKey, document)
        val events = mutableListOf<Any>()

        private fun randomHash(): HashCode {
            return HashCode("md5", Data("asdf".toByteArray()))
        }

        fun getSigner1Keys(): KeyPair {
            return signerCrypto1.pair
        }

        fun getSigner2Keys(): KeyPair {
            return signerCrypto2.pair
        }

        fun getDocumentKeys(): KeyPair {
            return documentCrypto.pair
        }

        fun checkDocumentSeen(signer1: Signer, transmissionSecret: String) {
            // check DocumentSeen event and changes in protocol
            assertThat(events.size).isEqualTo(1)
            assertThat(events[0]).isInstanceOf(DocumentSeen::class.java)

            val protocol = storage.getProtocol(protocolKey)
            // check if document seen event if recorded in protocol
            val signer = protocol.signers.findSigner(signer1.pubKey)!!
            assertThat(signer.documentSeen).isNotNull
            val documentSeen = crypto.decrypt(signer.documentSeen!!, documentCrypto.pair.privateKey)
            assertThat(documentSeen.content.editorAppHash).isEqualTo(appHash)
            assertThat(documentSeen.content.encryptedDocumentHash).isEqualTo(crypto.calculateHashCode(encryptedDocument))
            assertThat(documentSeen.content.ipAddress).isEqualTo("8.3.4.3")
            // signer1 encrypted the secret, check value
            assertThat(transmissionSecret).isEqualTo(crypto.decrypt(documentSeen.content.secret, signer1.pubKey))
            isSignedBy(documentSeen, witness.pubKey)
        }

        fun checkTransmission(signer1: Signer): String {
            val messages = events.filterIsInstance(Message::class.java)
            assertThat(messages.size).isEqualTo(1)
            val transmission = signer1.signingRequestTransmission?.content
            assertThat(transmission).isNotNull

            val protocol = storage.getProtocol(protocolKey)
            // check if transmission is recorded in protocol
            val transmissionRecipient = protocol.signers.findSigner(transmission!!.recipientPublicKey)
            assertThat(transmissionRecipient).isNotNull
            val signingRequestTransmission = transmissionRecipient!!.signingRequestTransmission
            assertThat(signingRequestTransmission).isNotNull
            // should be signed by witness
            isSignedBy(signingRequestTransmission!!, witness.pubKey)

            // check transmission to signer1
            assertThat(transmission.recipientEmail).isEqualTo(signer1.email)
            assertThat(transmission.recipientPublicKey).isEqualTo(signer1.pubKey)
            val transmissionSecret = transmission.secret
            assertThat(transmissionSecret).isNotBlank()
            assertThat(transmission.protocolKey).isEqualTo(protocol.key)
            return transmissionSecret
        }

        fun checkCreation(protocol: Protocol) {
            // creation protocol is encrypted using document public key
            assertThat(protocol.creation.publicKey).isEqualTo(documentCrypto.pair.publicKey)
            val creation = crypto.decrypt(protocol.creation, documentCrypto.pair.privateKey)
            // witness should have signed the creation protocol
            isSignedBy(creation, witness.pubKey)
        }

        private fun isSignedBy(testified: Testified<*>, publicKey: PublicKey) {
            assertThat(testified.signature.publicKey).isEqualTo(publicKey)
            // TODO: check signature
        }

        fun checkDocumentSignedBy(pubKey: PublicKey, privateKey: PrivateKey) {
            val protocol = storage.getProtocol(protocolKey)
            val signature = protocol.signers.findSigner(pubKey)!!.signature
            assertThat(signature).isNotNull
            val decrypted = crypto.decrypt(signature!!, privateKey)
            isSignedBy(decrypted, pubKey)
            // TODO: check data in signature
        }
    }

    val setup = TestSetup(witness, storage, crypto)

    private val implementation = Implementation(
        wittnessKeyPair,
        storage,
        setup.eventBus,
        crypto
    )

    class CryptoId(crypto: Crypto) {
        val pair = crypto.createKeyPair()
    }

    @Test
    fun test() {

        // start workflow component
        Implementation.Workflow(setup.eventBus, implementation, storage)

        val documentPrivateKey = setup.getDocumentKeys().privateKey

        // signer1
        val signer1PubKey = setup.getSigner1Keys().publicKey
        val documentKeys = setup.getDocumentKeys()
        val signer1 = Signer(
            "hank@boki.com",
            signer1PubKey,
            crypto.encrypt(signer1PubKey, documentPrivateKey),
            crypto.encrypt(documentKeys.publicKey, PersonData("Hank", "Boki", "ACME Inc."))
        )

        // signer2
        val signer2 = Signer(
            "peter@fox.de",
            setup.getSigner2Keys().publicKey,
            crypto.encrypt(setup.getSigner2Keys().publicKey, documentPrivateKey),
            crypto.encrypt(documentKeys.publicKey, PersonData("Peter", "Fox", "Test GmbH"))
        )

        // document is uploaded
        val protocolKey =
            implementation.startProtocol(
                setup.encryptedDocument,
                documentKeys.publicKey,
                listOf(signer1, signer2),
                RequestContext("134.76.1.45")
            )
        setup.protocolKey = protocolKey

        // check if signer1 got a message
        assertThat(setup.events.size).isEqualTo(2)
        assertThat(setup.events[0]).isInstanceOf(ProtocolCreation::class.java)

        val protocolCreation = setup.events[0] as ProtocolCreation

        val protocol = storage.getProtocol(protocolCreation.protocolKey)
        setup.checkCreation(protocol)

        var transmissionSecret = setup.checkTransmission(signer1)

        setup.events.clear()

        val encryptedSecret = crypto.encrypt(setup.getSigner1Keys(), transmissionSecret)
        implementation.protocolDownload(
            protocolKey,
            encryptedSecret,
            signer1.pubKey,
            RequestContext("8.3.4.3"),
            setup.appHash
        )

        setup.checkDocumentSeen(
            signer1,
            transmissionSecret
        )

        setup.events.clear()
        implementation.signDocument(
            protocolKey, signer1.pubKey,
            RequestContext("8.3.4.3"), setup.appHash,
            crypto.calculateHashCode(setup.document),
            "https://some.uri/....",
            crypto.encrypt(documentKeys.publicKey, BrowserData("chrome", "linux"))
        )

        setup.checkDocumentSignedBy(signer1.pubKey, setup.signerCrypto1.pair.privateKey)

        // request should have been sent to signer2
        transmissionSecret = setup.checkTransmission(signer2)
        implementation.protocolDownload(
            protocolKey,
            encryptedSecret,
            signer1.pubKey,
            RequestContext("8.3.4.3"),
            setup.appHash
        )

        // TODO: complete test
    }


}

private fun Iterable<Signer>.findSigner(publicKey: PublicKey): Signer? {
    return find { signer -> signer.pubKey == publicKey }
}
