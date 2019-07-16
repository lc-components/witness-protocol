package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*
import de.bcoding.ltc.witness.model.Crypto.Companion.calculateHashCode
import de.bcoding.ltc.witness.model.Crypto.Companion.decrypt
import de.bcoding.ltc.witness.model.Crypto.Companion.encrypt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.security.KeyPairGenerator
import kotlin.reflect.KClass

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImplementationTest {
    private val witness: Witness = Witness(CryptoId().pair)

    private val storage: Storage = object : Storage {
        override fun createProtocol(key: String, protocol: Protocol) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getProtocol(protocolKey: String): Protocol {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun saveProtocol(protocol: Protocol) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }


    val events = mutableListOf<Any>()

    private val eventBus: EventBus = object: EventBus {

        val listeners = mutableMapOf<KClass<*>, (Any) -> Unit>()

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> listenTo(kClass: KClass<T>, function: (T) -> Unit) {
            listeners[kClass] = function as (Any) -> Unit
        }

        override fun publish(event: Any) {
            events.add(event)
            if(listeners.containsKey(event::class)) {
                listeners[event::class]!!.invoke(event)
            }
        }

    }

    private val implementation = Implementation(
        witness,
        storage,
        eventBus
    )

    class CryptoId {
        private val keyGen = KeyPairGenerator.getInstance("DSA", "SUN")
        private val tmp = keyGen.generateKeyPair()
        private val priv = tmp.private
        private val pub = tmp.public
        val pair = KeyPair(PublicKey(pub), PrivateKey(priv))
    }

    @Test
    fun test() {
        val appHash = randomHash()

        // start workflow component
        Implementation.Workflow(eventBus, implementation, storage)

        val documentCrypto = CryptoId()

        // signer1
        val signerCrypto1 = CryptoId()
        val signer1 = Signer(
            "hank@boki.com",
            signerCrypto1.pair.publicKey,
            encrypt(signerCrypto1.pair.publicKey, documentCrypto.pair.privateKey),
            encrypt(documentCrypto.pair.publicKey, PersonData("Hank", "Boki", "ACME Inc."))
        )

        // signer2
        val signerCrypto2 = CryptoId()
        val signer2 = Signer(
            "peter@fox.de",
            signerCrypto2.pair.publicKey,
            encrypt(signerCrypto2.pair.publicKey, documentCrypto.pair.privateKey),
            encrypt(documentCrypto.pair.publicKey, PersonData("Peter", "Fox", "Test GmbH"))
        )

        val documentData = "test data".toByteArray()
        val document = Document(documentData)
        val encryptedDocument = encrypt(documentCrypto.pair.publicKey, document)

        // document is uploaded
        val protocolKey =
            implementation.startProtocol(
                encryptedDocument,
                documentCrypto.pair.publicKey,
                listOf(signer1, signer2),
                RequestContext("134.76.1.45")
            )

        // check if signer1 got a message
        assertThat(events.size).isEqualTo(2)
        assertThat(events[0]).isInstanceOf(ProtocolCreation::class.java)

        val protocolCreation = events[0] as ProtocolCreation

        var protocol = storage.getProtocol(protocolCreation.protocolKey)
        checkCreation(protocol, documentCrypto)

        assertThat(events[1]).isInstanceOf(SigningRequestTransmission::class.java)

        val transmission = events[1] as SigningRequestTransmission
        val transmissionSecret = checkTransmission(protocol, transmission, signer1)

        events.clear()

        val encryptedSecret = encrypt(signerCrypto1.pair.privateKey, transmissionSecret)
        implementation.protocolDownload(protocolKey,
            encryptedSecret,
            signer1.pubKey,
            RequestContext("8.3.4.3"),
            appHash
            )

        checkDocumentSeen(
            protocol,
            protocolKey,
            signer1,
            documentCrypto,
            appHash,
            encryptedDocument,
            transmissionSecret
        )

        events.clear()
        implementation.signDocument(protocolKey, encryptedSecret, signer1.pubKey,
            RequestContext("8.3.4.3"), appHash, calculateHashCode(document),
            "https://some.uri/....",
            encrypt(documentCrypto.pair.publicKey, BrowserData("chrome", "linux"))
            )

        // TODO: complete test
    }

    private fun checkDocumentSeen(
        protocol: Protocol,
        protocolKey: String,
        signer1: Signer,
        documentCrypto: CryptoId,
        appHash: HashCode,
        encryptedDocument: Encrypted<Document>,
        transmissionSecret: String
    ) {
        // check DocumentSeen event and changes in protocol
        var protocol = protocol
        assertThat(events.size).isEqualTo(1)
        assertThat(events[0]).isInstanceOf(DocumentSeen::class.java)

        protocol = storage.getProtocol(protocolKey)
        // check if document seen event if recorded in protocol
        val signer = protocol.signers.findSigner(signer1.pubKey)!!
        assertThat(signer.documentSeen).isNotNull()
        val documentSeen = decrypt(signer.documentSeen!!, documentCrypto.pair.privateKey)
        assertThat(documentSeen.content.editorAppHash).isEqualTo(appHash)
        assertThat(documentSeen.content.encryptedDocumentHash).isEqualTo(Crypto.calculateHashCode(encryptedDocument))
        assertThat(documentSeen.content.ipAddress).isEqualTo("8.3.4.3")
        // signer1 encrypted the secret, check value
        assertThat(transmissionSecret).isEqualTo(decrypt(documentSeen.content.secret, signer1.pubKey))
        isSignedBy(documentSeen, witness.keyPair.publicKey)
    }

    private fun checkTransmission(
        protocol: Protocol,
        transmission: SigningRequestTransmission,
        signer1: Signer
    ): String {
        // check if transmission is recorded in protocol
        val transmissionRecipient = protocol.signers.findSigner(transmission.recipientPublicKey)
        assertThat(transmissionRecipient).isNotNull
        val signingRequestTransmission = transmissionRecipient!!.signingRequestTransmission
        assertThat(signingRequestTransmission).isNotNull
        // should be signed by witness
        isSignedBy(signingRequestTransmission!!, witness.keyPair.publicKey)

        // check transmission to signer1
        assertThat(transmission.recipientEmail).isEqualTo(signer1.email)
        assertThat(transmission.recipientPublicKey).isEqualTo(signer1.pubKey)
        val transmissionSecret = transmission.secret
        assertThat(transmissionSecret).isNotBlank()
        assertThat(transmission.protocolKey).isEqualTo(protocol.key)
        return transmissionSecret
    }

    private fun checkCreation(
        protocol: Protocol,
        documentCrypto: CryptoId
    ) {
        // creation protocol is encrypted using document public key
        assertThat(protocol.creation.publicKey).isEqualTo(documentCrypto.pair.publicKey)
        val creation = decrypt(protocol.creation, documentCrypto.pair.privateKey)
        // witness should have signed the creation protocol
        isSignedBy(creation, witness.keyPair.publicKey)
    }

    private fun isSignedBy(testified: Testified<*>, publicKey: PublicKey) {
        assertThat(testified.signature.publicKey).isEqualTo(publicKey)
        // TODO: check signature
    }

    private fun randomHash(): HashCode {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

private fun Iterable<Signer>.findSigner(publicKey: PublicKey): Signer? {
    return find { signer -> signer.pubKey == publicKey }
}
