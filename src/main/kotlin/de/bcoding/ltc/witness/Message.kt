package de.bcoding.ltc.witness

import kotlin.reflect.KClass

enum class MessageVariable(val type: KClass<*>) {
    PROTOCOL_KEY(String::class),
    ENCRYPTED_SECRET(ByteArray::class)
}

enum class MessageTemplate(vararg variables: MessageVariable) {
    SIGNING_REQUEST(
        MessageVariable.PROTOCOL_KEY,
        MessageVariable.ENCRYPTED_SECRET
    )
}

class Message(
    val receipient: String,
    val template: MessageTemplate,
    val mapOf: Map<MessageVariable, *>
)