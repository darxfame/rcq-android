package com.rcq.messenger.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RegistrationBundle(
    val identityKeyPair: String,
    val registrationId: Int,
    val preKeys: List<String>,
    val signedPreKey: String,
    val signedPreKeySignature: String
)