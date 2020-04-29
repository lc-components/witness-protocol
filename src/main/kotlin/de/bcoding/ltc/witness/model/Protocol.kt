package de.bcoding.ltc.witness.model

import de.bcoding.ltc.witness.serialization.Data
import kotlinx.serialization.*
import kotlin.reflect.KClass

@Serializable
class Protocol(
    val key: String,
    val protocolAccessPubKey: PublicKey,
    val document: Encrypted<Document>,
    val witness: Witness,
    val signers: List<Signer>,
    val creation: Encrypted<Testified<ProtocolCreation>>
)

class ExtendedSerializer : KSerializer<Protocol> {
    private val delegate = Protocol.serializer()

    override val descriptor: SerialDescriptor
        get() = delegate.descriptor

    override fun deserialize(decoder: Decoder): Protocol {
        val protocol = delegate.deserialize(decoder)
        protocol.document.type = Document::class
        protocol.signers.forEach {
            it.documentSeen?.type = Testified::class as KClass<Testified<DocumentSeen>>
        }
        protocol.creation.type = Testified::class as KClass<Testified<ProtocolCreation>>
        return protocol
    }

    override fun serialize(encoder: Encoder, obj: Protocol) {
        delegate.serialize(encoder, obj)
    }

}

@Serializable
class Witness(val pubKey: PublicKey)

@Serializable
class Document(val data: Data)

@Serializable
class Signer(
    val email: String,
    val pubKey: PublicKey,
    val protocolAccessPrivKey: Encrypted<PrivateKey>,
    val personData: Encrypted<PersonData>
) {
    fun getStatus(): SignerStatus {
        return if (signingRequestTransmission != null) {
            if (documentSeen != null) {
                if (signature != null) {
                    SignerStatus.DONE
                } else {
                    SignerStatus.DOCUMENT_SEEN
                }
            } else {
                SignerStatus.SIGNATURE_REQUESTED
            }
        } else {
            SignerStatus.INITIAL
        }
    }

    var signingRequestTransmission: Testified<SigningRequestTransmission>? = null
    var documentSeen: Encrypted<Testified<DocumentSeen>>? = null
    var signature: Encrypted<Testified<DocumentSignature>>? = null
}

enum class SignerStatus {
    INITIAL, SIGNATURE_REQUESTED, DOCUMENT_SEEN, DONE
}

@Serializable
class PersonData(val firstName: String, val lastName: String, val company: String)
