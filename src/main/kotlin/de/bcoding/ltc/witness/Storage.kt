package de.bcoding.ltc.witness

import de.bcoding.ltc.witness.model.Protocol

interface Storage {
    fun createProtocol(protocol: Protocol): String
    fun getProtocol(protocolKey: String): Protocol
    fun saveProtocol(protocol: Protocol)
}
