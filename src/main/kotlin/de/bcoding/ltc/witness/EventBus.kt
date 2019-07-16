package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.PublicKey
import kotlin.reflect.KClass

interface EventBus {
    fun publish(event: Any)
    fun <T : Any> listenTo(kClass: KClass<T>, function: (T) -> Unit)
}


