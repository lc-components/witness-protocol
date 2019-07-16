package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*
import de.bcoding.ltc.witness.model.Crypto.Companion.calculateHashCode
import de.bcoding.ltc.witness.model.Crypto.Companion.decrypt
import de.bcoding.ltc.witness.model.Crypto.Companion.encrypt
import de.bcoding.ltc.witness.model.Crypto.Companion.randomString
import de.bcoding.ltc.witness.model.Crypto.Companion.testify

class Implementation(
    val witness: Witness,
    private val storage: Storage,
    private val eventBus: EventBus
) {

    companion object {
        private const val PROTOCOL_KEY_SIZE = 64
        private const val TRANSMISSION_SECRET_SIZE = 64
    }

    class Workflow(
        eventBus: EventBus,
        implementation: Implementation,
        storage: Storage
    ) {
        init {

            // TODO: notify other party about completed process or send message to next signer
            eventBus.listenTo(DocumentSignedEvent::class) {

            }

            // TODO: notify other party that the document has been watched
            eventBus.listenTo(DocumentSeen::class) {

            }

            // connect protocol creation event to sending of message to next signer
            eventBus.listenTo(ProtocolCreation::class) {

                val protocolKey = it.protocolKey
                val protocol = storage.getProtocol(protocolKey)
                implementation.sendToSigner(
                    protocolKey,
                    protocol.signers.first { it.getStatus() == SignerStatus.INITIAL }.pubKey
                )
            }
        }
    }


    fun startProtocol(
        document: Encrypted<Document>,
        protocolAccessKey: PublicKey,
        signers: List<Signer>,
        context: RequestContext
    ): String {


        val key = randomString(PROTOCOL_KEY_SIZE)
        val protocolCreation = ProtocolCreation(
            context.ipAddress,
            calculateHashCode(document),
            key
        )

        val testifiedProtocol: Encrypted<Testified<ProtocolCreation>> = encrypt(
            protocolAccessKey,
            testify(protocolCreation)
        )

        val protocol = Protocol(key,
            protocolAccessKey, document, witness, signers,
            testifiedProtocol
        )

        storage.createProtocol(key, protocol)

        eventBus.publish(protocolCreation)

        return key
    }

    fun sendToSigner(protocolKey: String, signerPubKey: PublicKey) {
        val protocol = storage.getProtocol(protocolKey)
        val signer = protocol.signers.first { it.pubKey == signerPubKey }

        val transmissionSecret = randomString(TRANSMISSION_SECRET_SIZE)

        signer.signingRequestTransmission = testify(
            SigningRequestTransmission(
                signer.email,
                signer.pubKey,
                transmissionSecret,
                protocolKey
            )
        )
        storage.saveProtocol(protocol)

        eventBus.publish(
            Message(
                signer.email, MessageTemplate.SIGNING_REQUEST, mapOf(
                    MessageVariable.PROTOCOL_KEY to protocolKey,
                    MessageVariable.ENCRYPTED_SECRET to encrypt(signer.pubKey, transmissionSecret)
                )
            )
        )
    }

    fun protocolDownload(
        protocolKey: String,
        encryptedSecret: Encrypted<String>,
        signerPubKey: PublicKey,
        context: RequestContext,
        editorAppHash: HashCode
    ): Protocol {

        val protocol = storage.getProtocol(protocolKey)
        val signer = protocol.signers.first { it.pubKey == signerPubKey }

        checkStatusAndSecret(signer, encryptedSecret)

        val documentSeen = DocumentSeen(
            context.ipAddress,
            calculateHashCode(protocol.document),
            editorAppHash,
            encryptedSecret,
            signerPubKey)
        signer.documentSeen = encrypt(
            protocol.protocolAccessPubKey, testify(documentSeen)
        )

        storage.saveProtocol(protocol)

        eventBus.publish(documentSeen)
        return protocol
    }

    fun signDocument(
        protocolKey: String,
        encryptedSecret: Encrypted<String>,
        signerPubKey: PublicKey,
        context: RequestContext,
        editorAppHash: HashCode,
        documentHash: HashCode,
        editorUri: String,
        browser: Encrypted<BrowserData>
    ): Protocol {

        val protocol = storage.getProtocol(protocolKey)
        val signer = protocol.signers.first { it.pubKey == signerPubKey }

        checkStatusAndSecret(signer, encryptedSecret)

        signer.signature = encrypt(
            protocol.protocolAccessPubKey, testify(
                DocumentSignature(
                    context.ipAddress,
                    signerPubKey,
                    encryptedSecret,
                    documentHash,
                    editorAppHash,
                    editorUri,
                    browser
                )
            )
        )

        storage.saveProtocol(protocol)

        eventBus.publish(DocumentSignedEvent(signer.pubKey))
        return protocol
    }

    private fun checkStatusAndSecret(signer: Signer, encryptedSecret: Encrypted<String>) {
        if (signer.getStatus() != SignerStatus.SIGNATURE_REQUESTED && signer.getStatus() != SignerStatus.DOCUMENT_SEEN) {
            throw BadStateException("Signer has wrong state for download operation (${signer.getStatus()})")
        }
        if (signer.signingRequestTransmission!!.content.secret != decrypt(encryptedSecret, signer.pubKey)) {
            throw AuthorizationException("Bad secret")
        }
    }
}

/*
* EVENTS: -------------------------------
 */


class DocumentSignedEvent(pubKey: PublicKey) {

}
