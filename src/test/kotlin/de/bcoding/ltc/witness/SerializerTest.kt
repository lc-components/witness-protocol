package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.*
import de.bcoding.ltc.witness.serialization.Data
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SerializerTest {
    @Test
    fun testJson() {
        val crypto = TestCrypto(Json(JsonConfiguration.Stable.copy(prettyPrint = true)))

        val keyPair : KeyPair = crypto.createKeyPair()
        val document = Document(Data("asdf".toByteArray()))
        val protocolCreation = ProtocolCreation(
            "192.168.1.1",
            HashCode("md5", Data("2394293429347234asdfa".toByteArray())),
            "the-protocol")
        val protocol = Protocol("asdf", keyPair.publicKey, crypto.encrypt(keyPair, document),
            Witness(keyPair.publicKey),
            listOf(),
            crypto.encrypt(keyPair, crypto.testify(protocolCreation, keyPair))
        )
        val deserializer = Protocol.serializer()
        val serialized = Json(JsonConfiguration(prettyPrint = true)).stringify(deserializer, protocol)

        println(serialized)

        val deserialized = Json.parse(ExtendedSerializer(), serialized)
        println(deserialized.creation.decrypt(keyPair.privateKey, crypto).time)
    }

    @Test
    fun testCbor() {
        val kBson = Cbor()
        val crypto = TestCrypto(kBson)

        val keyPair : KeyPair = crypto.createKeyPair()
        val document = Document(Data("asdf".toByteArray()))
        val protocolCreation = ProtocolCreation(
            "192.168.1.1",
            HashCode("md5", Data("2394293429347234asdfa".toByteArray())),
            "the-protocol")
        val protocol = Protocol("asdf", keyPair.publicKey, crypto.encrypt(keyPair, document),
            Witness(keyPair.publicKey),
            listOf(),
            crypto.encrypt(keyPair, crypto.testify(protocolCreation, keyPair))
        )
        val deserializer = Protocol.serializer()
        val serialized = kBson.dump(deserializer, protocol)

        println(serialized)

        val deserialized = kBson.load(ExtendedSerializer(), serialized)
        println(deserialized.creation.decrypt(keyPair.privateKey, crypto).time)
    }

}