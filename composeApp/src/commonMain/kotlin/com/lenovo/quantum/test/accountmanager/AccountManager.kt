/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.accountmanager

import kotlinx.coroutines.flow.StateFlow

// User data retrieved by MotoAccountSDK
data class UserData (
    val tokenId: String? = null,
    val email: String? = null,
    val id: String? = null,
    val name: String? = null,
    val avatarUrl: String? = null
)

expect object AccountManager {

    // Initializes the SDK and does platform specific configuration
    fun initialize(context: Any? = null)

    // Performs the required action when the account is clicked (e.g. launch login screen)
    fun onAccountClick()

    // Indicates if the AccountManager was initialized and can be used
    var isInitialized : Boolean
        private set

    // Indicates if a user is logged in
    val isLoggedIn : Boolean

    // Manages the state of the user account information. Useful to automatically update UI.
    val state : StateFlow<UserData>

    // Used to fetch current user data at a specific time (e.g. AccountManager.data.tokenId)
    val data : UserData
}
