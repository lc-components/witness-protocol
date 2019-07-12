package de.bcoding.ltc.witness.model

class HashCode

class Signature

/**
 * data contains a serialized version of the object
 */
class Encrypted<OBJ>(
    var data: ByteArray,
    var accessKey: PublicKey
)

class PublicKey
class PrivateKey