package de.bcoding.ltc.witness

//import de.bcoding.ltc.witness.model.PublicKey
//import de.bcoding.ltc.witness.model.Testified
//import org.assertj.core.api.AbstractAssert
//import org.assertj.core.api.Assertions
//
//class CryptoAssert<T>(private val testified: Testified<T>) :
//    AbstractAssert<CryptoAssert<T>, Testified<T>>(testified, CryptoAssert::class.java) {
//    companion object {
//        fun <T> assertThat(actual: Testified<T>): CryptoAssert<T> {
//            return CryptoAssert(actual)
//        }
//    }
//
//    fun isSignedBy(publicKey: PublicKey): CryptoAssert<T> {
//        Assertions.assertThat(testified.signature.publicKey).isEqualTo(publicKey)
//        // TODO: check signature
//        return this
//    }
//}
