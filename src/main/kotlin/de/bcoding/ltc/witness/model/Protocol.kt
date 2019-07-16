package de.bcoding.ltc.witness.model

class Protocol(
    val key: String,
    val protocolAccessPubKey: PublicKey,
    val document: Encrypted<Document>,
    val witness: Witness,
    val signers: List<Signer>,
    val creation: Encrypted<Testified<ProtocolCreation>>
)

class Witness(val keyPair: KeyPair)
class Document(val data: ByteArray) : Data {
    override fun toByteArray(): ByteArray {
        return data
    }
}

interface Data {
    fun toByteArray() : ByteArray
}

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

class PersonData(val firstName: String, val lastName: String, val company: String)
