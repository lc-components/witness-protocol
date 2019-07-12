package de.bcoding.ltc.witness.model

import java.time.LocalDateTime

class Testified<T>(
    val content: T,
    val time: LocalDateTime,
    val signingParty: PublicKey,
    val signature: Signature
)

class BrowserData(val browser: String, val os: String)

class ProtocolCreation(
    val ipAddress: String,
    val encryptedDocumentHash: HashCode
)

class DocumentDownload(
    val ipAddress: String,
    val encryptedDocumentHash: HashCode,
    val editorAppHash: HashCode
)


class Underwriting(
    val documentHash: HashCode,
    val editorAppHash: HashCode,
    val editorUri: String,
    val browser: Encrypted<BrowserData>
)


class UnderwritingRequestTransmission(
    val recipientEmail: String,
    val recipientPublicKey: PublicKey,
    /**
     * contains a secret encrypted with the public key of recipient
     */
    val secret: Encrypted<String>
)

class UnderwritingUpload(
    val ipAddress: String,
    val uploader: PublicKey,
    /**
     * contains a the secret from the UnderwritingRequestTransmissionProtocol, encrypted with the public key of the email sender
     */
    val secret: Encrypted<String>
)