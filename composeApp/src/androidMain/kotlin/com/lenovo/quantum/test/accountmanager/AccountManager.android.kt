/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test.accountmanager

import QtcoreTestApp.composeApp.BuildConfig
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.lenovo.quantum.sdk.logging.logD
import com.lenovo.quantum.sdk.logging.logE
import com.lenovo.quantum.test.AndroidUtil
import com.motorola.motoaccount.MotoIdApiManager
import com.motorola.motoaccount.utils.LogPrinter
import com.motorola.motoaccount.utils.MotoDeviceType
import com.motorola.motoaccount.utils.MotoIdConfig
import com.motorola.motoaccount.utils.OnStInfoListener
import com.motorola.motoaccount.utils.OnUkiInfoListener
import com.motorola.motoaccount.utils.StInfo
import com.motorola.motoaccount.utils.UkiInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

actual object AccountManager {

    private const val REDIRECT_URL = "com.motorola.aicore://callback"
    private const val LENOVO_USER_STATUS_ACTION = "com.motorola.account.LENOVOUSER_STATUS"
    private const val LOGIN_ACCOUNTS_CHANGED_ACTION = "android.accounts.LOGIN_ACCOUNTS_CHANGED"

    private const val STATUS_EXTRA = "status"
    private const val STATUS_LOGGED_IN = "2"
    private const val STATUS_LOGGED_OUT = "1"

    private const val TAG = "testapp.AccountManager"

    // Holds the reference of the activity that will trigger login flow
    private lateinit var activity: ComponentActivity

    actual var isInitialized = false
        private set

    actual val isLoggedIn : Boolean
        get() = if (isInitialized) MotoIdApiManager.getInstance().isLoggedIn else false

    private val _state = MutableStateFlow(UserData())
    actual val state = _state.asStateFlow()

    actual val data : UserData
        get() = _state.value

    actual fun initialize(context: Any?) {
        if (isInitialized) {
            logE(TAG) { "Already initialized. No need to initialize again." }
            return
        }

        if (context !is ComponentActivity) {
            logE(TAG) { "Could not be initialized." }
            return
        }

        activity = context
        val application = activity.application

        val deviceType = if (AndroidUtil.isDevicePRC()) MotoDeviceType.MOTO_PRC else MotoDeviceType.MOTO_ROW
        logD(TAG) { "device: $deviceType" }
        val motoIdConfig = MotoIdConfig(
            BuildConfig.LENOVOID_CLIENT_ID,
            BuildConfig.LENOVOID_REALM_ID,
            REDIRECT_URL,
            deviceType,
            BuildConfig.LENOVOID_REALM_KEY,
            BuildConfig.LENOVOID_PUBLIC_KEY
        )

        // Set logger
        MotoIdApiManager.getInstance().setLogPrinter(object : LogPrinter {
            override fun onLogD(p0: String, p1: String) {
                logD(TAG) { "[MotoId] $p1" }
            }

            override fun onLogE(p0: String, p1: String) {
                logE(TAG) { "[MotoId] $p1" }
            }
        })

        // Initialize SDK
        MotoIdApiManager.getInstance().init(application, motoIdConfig)
        isInitialized = true

        // Register Broadcast receiver
        registerBroadcastReceiver(application)

        // Load user if logged in
        if (isLoggedIn) {
            updateUserAttributes()
        }

        logD(TAG) { "Initialized" }
    }

    actual fun onAccountClick() {
        if (!isLoggedIn) {
            startLoginFlow()
        } else {
            displayAccountManagement()
        }
    }

    private fun resetUserAttributes() {
        logD(TAG) { "resetUserAttributes::E" }
        _state.update { it.copy(null,null,null, null, null) }
    }

    private fun startLoginFlow() {
        MotoIdApiManager.getInstance().login(activity)
    }

    private fun displayAccountManagement() {
        MotoIdApiManager.getInstance().showAccountPage(activity)
    }

    private val loginListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.action?.also { logD(TAG) { "action received: $it" } }
            val status = intent.getStringExtra(STATUS_EXTRA)?.also { logD(TAG) { "extra received: $it" } }

            when (action) {
                LENOVO_USER_STATUS_ACTION -> {
                    when (status) {
                        STATUS_LOGGED_IN -> updateUserAttributes()
                        STATUS_LOGGED_OUT -> resetUserAttributes()
                    }
                }
                LOGIN_ACCOUNTS_CHANGED_ACTION -> {
                    when (isLoggedIn) {
                        true -> updateUserAttributes()
                        false -> resetUserAttributes()
                    }
                }
            }
        }
    }

    private fun registerBroadcastReceiver(application: Application) {
        ContextCompat.registerReceiver(
            application,
            loginListener,
            IntentFilter().apply {
                addAction(LENOVO_USER_STATUS_ACTION)
                addAction(LOGIN_ACCOUNTS_CHANGED_ACTION)
            },
            ContextCompat.RECEIVER_EXPORTED
        )

        logD(TAG) { "Broadcast Receiver registered" }
    }

    private fun updateUserAttributes() {
        logD(TAG) { "updateUserAttributes::E" }
        _state.update { it.copy(email = MotoIdApiManager.getInstance().userName) }

        // Set user id
        MotoIdApiManager.getInstance().getStData(object : OnStInfoListener {
            override fun onResult(st: StInfo?) {
                if (st == null) return
                st.userid?.let { nonNullId ->
                    if (!TextUtils.isEmpty(nonNullId)) {
                        _state.update { it.copy(id = nonNullId) }
                    }
                }
                _state.update { it.copy(tokenId = st.lpsust) }
                logD(TAG) { "userId and userTokenId set" }
            }

            override fun onFail(msg: String?) {
                logE(TAG) { "Get ST data failed: $msg" }
            }
        })

        // Set user name + avatar
        MotoIdApiManager.getInstance().getUkiInfo(object : OnUkiInfoListener {
            override fun onResult(uki: UkiInfo?) {
                if (uki == null) return

                val fullName = uki.firstname + " " + uki.lastname
                _state.update { it.copy(name = fullName) }
                uki.uid?.let { nonNullId ->
                    if (!TextUtils.isEmpty(nonNullId)) {
                        _state.update { it.copy(id = nonNullId) }
                    }
                }
                _state.update { it.copy(avatarUrl = uki.image) }
                logD(TAG) { "userName and userAvatarURL set" }
            }

            override fun onFail(msg: String) {
                logE(TAG) { "Get UKI info failed: $msg" }
            }
        })
    }
}
