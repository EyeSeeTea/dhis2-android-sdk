@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package org.hisp.dhis.android.core.user.internal

import kotlinx.serialization.Serializable

// EyeSeeTea customization - Two-Factor Authentication Login
@Serializable
internal class LoginPayload(
    val username: String,
    val password: String,
    val twoFactorCode: String?,
)
