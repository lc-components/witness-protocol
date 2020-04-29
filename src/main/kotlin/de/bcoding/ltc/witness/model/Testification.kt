package de.bcoding.ltc.witness.model

import kotlinx.serialization.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_DATE_TIME)
    }

    override fun serialize(encoder: Encoder, obj: LocalDateTime) {
        return encoder.encodeString(obj.format(DateTimeFormatter.ISO_DATE_TIME))
    }

}

@Serializable
class Testified<T : Any>(
    @Polymorphic val content: T,
    @Serializable(with = LocalDateTimeSerializer::class)
    val time: LocalDateTime,
    val signature: Signature
)

@Serializable
class BrowserData(val browser: String, val os: String)

@Serializable
class ProtocolCreation(
    val ipAddress: String,
    val encryptedDocumentHash: HashCode,
    val protocolKey: String
)

@Serializable
class DocumentSeen(
    val ipAddress: String,
    val encryptedDocumentHash: HashCode, // this is set by the witness to testify which data was given out
    val editorAppHash: HashCode,
    val secret: Encrypted<String>,
    val signerPubKey: PublicKey
)

@Serializable
class SigningRequestTransmission(
    val recipientEmail: String,
    val recipientPublicKey: PublicKey,
    val secret: String,
    val protocolKey: String
)

@Serializable
class DocumentSignature(
    val ipAddress: String,
    val uploader: PublicKey,
    val documentHash: HashCode,
    val editorAppHash: HashCode,
    val editorUri: String,
    val browser: Encrypted<BrowserData>
)
