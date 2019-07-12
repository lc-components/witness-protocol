package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*

class Implementation(
    val witness: Witness,
    val storage: Storage,
    val messenger: Messenger
) {

    val TRANSMISSION_SECRET_SIZE = 64

    fun startProtocol(
        document: Encrypted<Document>,
        protocolAccessKey: PublicKey,
        underwriters: List<Underwriter>,
        context: RequestContext
    ): String {
        val protocolCreation = ProtocolCreation(
            context.ipAddress,
            calculateHashCode(document)
        )

        val testifiedProtocol: Encrypted<Testified<*>> = encrypt(
            protocolAccessKey,
            testify(protocolCreation)
        )

        val protocol = Protocol(
            protocolAccessKey, document, witness, underwriters,
            mutableListOf(testifiedProtocol)
        )

        return storage.createProtocol(protocol)
    }

    fun sendToUnderwriter(protocolKey: String, underwriterPubKey: PublicKey) {
        val protocol = storage.getProtocol(protocolKey)
        val underwriter = protocol.underwriters.first { it.pubKey == underwriterPubKey }

        val encryptedSecret = encrypt(underwriter.pubKey, randomString(TRANSMISSION_SECRET_SIZE))

        protocol.testifications.add(
            encrypt(
                protocol.protocolAccessPubKey,
                testify(
                    UnderwritingRequestTransmission(
                        underwriter.email,
                        underwriter.pubKey,
                        encryptedSecret
                    )
                )
            )
        )
        storage.saveProtocol(protocol)

        messenger.send(
            underwriter.email, MessageTemplate.SIGNING_REQUEST, mapOf(
                MessageVariable.PROTOCOL_KEY to protocolKey
            )
        )
    }

    private fun <T> encrypt(pubKey: PublicKey, subject: T): Encrypted<T> {
        TODO("not implemented")
    }

    private fun randomString(size: Int): String {
        TODO("not implemented")
    }

    private fun calculateHashCode(subject: Any): HashCode {
        TODO("not implemented")
    }

    private fun testify(subject: Any): Testified<*> {
        TODO("not implemented")
    }

}

class RequestContext(val ipAddress: String)