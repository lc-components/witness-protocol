package de.bcoding.ltc.witness

import com.tinder.StateMachine
import de.bcoding.ltc.witness.model.*


sealed class SignerState {
    object Start : SignerState()
    object RequestSent : SignerState()
    object Downloaded : SignerState()
    object Signed : SignerState()
}


sealed class SignerEvent {
    class OnSendRequest(
        document: Encrypted<Document>,
        protocolAccessKey: PublicKey,
        signers: List<Signer>
    ) : SignerEvent()

    class OnDownload(
        encryptedSecret: Encrypted<String>,
        signerPubKey: PublicKey,
        context: RequestContext,
        editorAppHash: HashCode
    ) : SignerEvent()

    class OnSign(
        signerPubKey: PublicKey,
        context: RequestContext,
        editorAppHash: HashCode,
        documentHash: HashCode,
        editorUri: String,
        browser: Encrypted<BrowserData>
    ) : SignerEvent()
}

sealed class SignerSideEffect {
    object CreateProtocol : SignerSideEffect()
    object SendToSigner : SignerSideEffect()
    object SaveDownloaded : SignerSideEffect()
    object LogCondensed : SignerSideEffect()
}

class SignerWrapper(val signer: Signer) {
    val stateMachine = StateMachine.create<SignerState, SignerEvent, SignerSideEffect> {
        initialState(signer.getMachineState())
        state<SignerState.Start> {
            on<SignerEvent.OnSendRequest> {
                onEnter {
                    // TODO: send to signer
                }
                transitionTo(SignerState.RequestSent, SignerSideEffect.SendToSigner)
            }
        }
        state<SignerState.RequestSent> {
            on<SignerEvent.OnDownload> {
                onEnter {
                    // TODO: create protocol entry
                }
                transitionTo(SignerState.Downloaded, SignerSideEffect.SaveDownloaded)
            }
        }
        state<SignerState.Downloaded> {
            on<SignerEvent.OnDownload> {
                onEnter {
                    // TODO: create protocol entry
                }
                transitionTo(SignerState.Downloaded, SignerSideEffect.SaveDownloaded)
            }
            on<SignerEvent.OnSign> {
                onEnter {
                    // TODO: create protocol entry
                }
                transitionTo(SignerState.Signed, SignerSideEffect.SaveDownloaded)
            }
        }
//    onTransition {
//        val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
//        when (validTransition.sideEffect) {
//            SideEffect.LogMelted -> logger.log(ON_MELTED_MESSAGE)
//            SideEffect.LogFrozen -> logger.log(ON_FROZEN_MESSAGE)
//            SideEffect.LogVaporized -> logger.log(ON_VAPORIZED_MESSAGE)
//            SideEffect.LogCondensed -> logger.log(ON_CONDENSED_MESSAGE)
//        }
//    }
    }
}

private fun Signer.getMachineState(): SignerState {
    return if (signingRequestTransmission != null) {
        if (documentSeen != null) {
            if (signature != null) {
                SignerState.Signed
            } else {
                SignerState.Downloaded
            }
        } else {
            SignerState.RequestSent
        }
    } else {
        SignerState.Start
    }
}