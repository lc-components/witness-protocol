package de.bcoding.ltc.witness.model

class Protocol(
    val protocolAccessPubKey: PublicKey,
    val document: Encrypted<Document>,
    val witness: Witness,
    val underwriters: List<Underwriter>,
    /**
     * encrypted with document public key
     */
    val testifications: MutableList<Encrypted<Testified<*>>>
)

class Witness(
    val pubKey: PublicKey
)

class Document(val data: ByteArray)

class Underwriter(
    val email: String,
    val pubKey: PublicKey,
    val protocolAccessPrivKey: Encrypted<PrivateKey>,
    val personData: Encrypted<PersonData>
)

class PersonData(val firstName: String, val lastName: String, val company: String)
