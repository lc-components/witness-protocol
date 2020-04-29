package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*

class Implementation(
    val witnessKeyPair: KeyPair,
    private val storage: Storage,
    private val eventBus: EventBus,
    private val crypto: Crypto
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

        val witness = Witness(witnessKeyPair.publicKey)

        val key = crypto.randomString(PROTOCOL_KEY_SIZE)
        val protocolCreation = ProtocolCreation(
            context.ipAddress,
            crypto.calculateHashCode(document),
            key
        )

        val testifiedProtocol: Encrypted<Testified<ProtocolCreation>> = crypto.encrypt(
            protocolAccessKey,
            crypto.testify(protocolCreation, witnessKeyPair)
        )

        val protocol = Protocol(
            key,
            protocolAccessKey, document, witness, signers,
            testifiedProtocol
        )

        storage.createProtocol(key, protocol)

        eventBus.publish(protocolCreation)

        return key
    }

    fun sendToSigner(protocolKey: String, signerPubKey: PublicKey) =
        changeProtocol(protocolKey) { protocol ->

            val signer = protocol.signers.first { it.pubKey == signerPubKey }

            val transmissionSecret = crypto.randomString(TRANSMISSION_SECRET_SIZE)

            signer.signingRequestTransmission = crypto.testify(
                SigningRequestTransmission(
                    signer.email,
                    signer.pubKey,
                    transmissionSecret,
                    protocolKey
                ), witnessKeyPair
            )

            eventBus.publish(
                Message(
                    signer.email, MessageTemplate.SIGNING_REQUEST, mapOf(
                        MessageVariable.PROTOCOL_KEY to protocolKey,
                        MessageVariable.ENCRYPTED_SECRET to crypto.encrypt(signer.pubKey, transmissionSecret)
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
        return changeProtocol(protocolKey) { protocol ->
            val signer = protocol.signers.first { it.pubKey == signerPubKey }

            checkStatusAndSecret(signer, encryptedSecret)

            val documentSeen = DocumentSeen(
                context.ipAddress,
                crypto.calculateHashCode(protocol.document),
                editorAppHash,
                encryptedSecret,
                signerPubKey
            )
            signer.documentSeen = crypto.encrypt(
                protocol.protocolAccessPubKey, crypto.testify(documentSeen, witnessKeyPair)
            )
            eventBus.publish(documentSeen)
        }
    }

    fun signDocument(
        protocolKey: String,
        signerPubKey: PublicKey,
        context: RequestContext,
        editorAppHash: HashCode,
        documentHash: HashCode,
        editorUri: String,
        browser: Encrypted<BrowserData>
    ) = changeProtocol(protocolKey) { protocol ->
        val signer = protocol.signers.first { it.pubKey == signerPubKey }

        checkStatusAndSecret(signer)

        signer.signature = crypto.encrypt(
            protocol.protocolAccessPubKey, crypto.testify(
                DocumentSignature(
                    context.ipAddress,
                    signerPubKey,
                    documentHash,
                    editorAppHash,
                    editorUri,
                    browser
                ), witnessKeyPair
            )
        )
        eventBus.publish(DocumentSignedEvent(signer.pubKey))
    }

    private fun changeProtocol(protocolKey: String, function: (Protocol) -> Unit): Protocol {
        val protocol = storage.getProtocol(protocolKey)
        function.invoke(protocol)
        storage.saveProtocol(protocol)
        return protocol
    }

    private fun checkStatusAndSecret(signer: Signer, encryptedSecret: Encrypted<String>? = null) {
        if (signer.getStatus() != SignerStatus.SIGNATURE_REQUESTED && signer.getStatus() != SignerStatus.DOCUMENT_SEEN) {
            throw BadStateException("Signer has wrong state for download operation (${signer.getStatus()})")
        }
        if (encryptedSecret != null && signer.signingRequestTransmission!!.content.secret != crypto.decrypt(
                encryptedSecret,
                signer.pubKey
            )
        ) {
            throw AuthorizationException("Bad secret")
        }
    }
}

/*
* EVENTS: -------------------------------
 */
class DocumentSignedEvent(pubKey: PublicKey)
