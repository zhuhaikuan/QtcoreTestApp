/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import android.util.Log
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object AndroidUtil {

    const val TAG = "Util"
    private const val ANDROID_OS_SYSTEM_PROPERTIES = "android.os.SystemProperties"
    private const val GET = "get"

    enum class DEV_PROP_STR(private val property: String) {
        TARGET_DEVICE("ro.product.device"),
        BUILD_ID("ro.build.id"),
        HARDWARE_RADIO("ro.vendor.hw.radio"),
        CAMERA_VARIANT("ro.vendor.hw.cam_variant"),
        SECURE_HW("ro.boot.secure_hardware"),
        PRODUCT_IS_PRC("ro.product.is_prc"),
        PRODUCT_IS_MID_RAM("ro.config.moto_mid_ram"),
        PRODUCT_IS_DF("persist.mot.dogfooding"),
        CID_SW("ro.boot.cid"),
        DEVICE_ENV("aicore.device.env"),
        BOARD_PLATFORM("ro.board.platform"),
        SOC_MANUFACTURER("ro.hardware.soc.manufacturer"),
        SOC_MODEL("ro.soc.model"),
        BUILD_TYPE("ro.system.build.type");

        private var mValue: String? = null
        var isSet = false

        var value: String?
            get() = mValue
            set(value) {
                mValue = value
                isSet = true
            }
        fun getProperty() : String = property
    }

    fun isDevicePRC() : Boolean {
        return getString(DEV_PROP_STR.PRODUCT_IS_PRC).toBoolean()
    }

    fun getString(property: DEV_PROP_STR): String {
        if (!property.isSet) {
            property.value = getSystemString(property.getProperty(), property.value)
        }
        return property.value ?: ""
    }

    private fun getSystemMethod(
        methodName: String,
        argList: Array<Class<*>>
    ): Method? {
        var method: Method? = null
        try {
            method = Class.forName(ANDROID_OS_SYSTEM_PROPERTIES).getMethod(methodName, *argList)
        } catch (e: ClassNotFoundException) {
            Log.e(TAG, "getMethod ClassNotFoundException: $e")
        } catch (e: NoSuchMethodException) {
            Log.e(TAG, "getMethod NoSuchMethodException: $e")
        }
        return method
    }

    @JvmStatic
    fun getSystemString(key: String, defaultValue: String?): String {
        var result = defaultValue ?: ""
        try {
            val method = getSystemMethod(
                GET,
                arrayOf(
                    String::class.java, String::class.java
                )
            )
            if (method != null) {
                result = method.invoke(null, key, defaultValue) as String
            }
        } catch (e: IllegalAccessException) {
            Log.e(TAG, "getString IllegalAccessException $e")
        } catch (e: InvocationTargetException) {
            Log.e(TAG, "getString InvocationTargetException $e")
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "getString IllegalArgumentException $e")
        }

        return result
    }
}