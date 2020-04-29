package de.bcoding.ltc.witness

import kotlin.reflect.KClass

interface EventBus {
    fun publish(event: Any)
    fun <T : Any> listenTo(kClass: KClass<T>, function: (T) -> Unit)
}


