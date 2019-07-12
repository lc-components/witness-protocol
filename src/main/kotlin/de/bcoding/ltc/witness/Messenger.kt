package de.bcoding.ltc.witness

import kotlin.reflect.KClass

interface Messenger {
    fun send(email: String, template: MessageTemplate, mapOf: Map<MessageVariable, *>)
}

enum class MessageVariable(val type: KClass<*>) {
    PROTOCOL_KEY(String::class)
}

enum class MessageTemplate(vararg variables: MessageVariable) {
    SIGNING_REQUEST(
        MessageVariable.PROTOCOL_KEY
        )
}

