package de.bcoding.ltc.witness.model

import java.time.LocalDateTime

class Testified<T>(
    val content: T,
    val time: LocalDateTime,
    val signature: Signature
)

class BrowserData(val browser: String, val os: String)

class ProtocolCreation(
    val ipAddress: String,
    val encryptedDocumentHash: HashCode,
    val protocolKey: String
)

class DocumentSeen(
    val ipAddress: String,
    val encryptedDocumentHash: HashCode, // this is set by the witness to testify which data was given out
    val editorAppHash: HashCode,
    val secret: Encrypted<String>,
    signerPubKey: PublicKey
)

class SigningRequestTransmission(
    val recipientEmail: String,
    val recipientPublicKey: PublicKey,
    val secret: String,
    val protocolKey: String
)

class DocumentSignature(
    val ipAddress: String,
    val uploader: PublicKey,
    val secret: Encrypted<String>,
    val documentHash: HashCode,
    val editorAppHash: HashCode,
    val editorUri: String,
    val browser: Encrypted<BrowserData>
)
