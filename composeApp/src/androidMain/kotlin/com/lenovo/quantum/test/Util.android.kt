/*
 * Copyright (C) 2025 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import coil3.PlatformContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object AndroidUtil : Util() {

    const val TAG = "testapp.Util"
    private const val ANDROID_OS_SYSTEM_PROPERTIES = "android.os.SystemProperties"
    private const val GET = "get"

    private val LAST_REQUEST_DURATION_MS = TimeUnit.MINUTES.toMillis(20)
    private const val MAX_WAIT_FOR_LOCATION_MS = 8000L
    private const val MAX_WAIT_FOR_GEOCODE_TIME_MS = 1500L

    enum class DEV_PROP_STR(private val property: String) {
        TARGET_DEVICE("ro.product.device"),
        BUILD_ID("ro.build.id"),
        HARDWARE_RADIO("ro.vendor.hw.radio"),
        CAMERA_VARIANT("ro.vendor.hw.cam_variant"),
        SECURE_HW("ro.boot.secure_hardware"),
        LENOVO_DEVICE_TYPE("ro.odm.lenovo.device"),
        LENOVO_HW_TYPE("ro.boot.hwproj"),
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

    override fun isDevicePrc() : Boolean {
        return getString(DEV_PROP_STR.PRODUCT_IS_PRC).toBoolean()
    }

    private fun getLenovoDeviceType(): String =
        getString(DEV_PROP_STR.LENOVO_DEVICE_TYPE)

    private fun getLenovoHardwareType(): String =
        getString(DEV_PROP_STR.LENOVO_HW_TYPE)

    override fun isDeviceLenovoTablet() : Boolean = getLenovoDeviceType() == "tablet"

    override fun isDevicePrcLenovoTablet() : Boolean = getLenovoHardwareType() == "PRC" && isDeviceLenovoTablet()

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

    override fun readAssetFile(path: String): String {
        val stream = appContext.assets.open(path)
        return stream.bufferedReader().use { it.readText() }
    }


    override fun getLocation(
        context : Any?,
        latLongToLocation : suspend (Double, Double) -> String?
    ): String? {
        val platformContext = context as? Context ?: return null
        val location = getCurrentLocation(platformContext) ?: return null
        val geocoder = Geocoder(platformContext, Locale.getDefault())

        val future = CompletableFuture<Address?>()
        geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
            future.complete(addresses.firstOrNull())
        }

        return try {
            val address = future.get(MAX_WAIT_FOR_GEOCODE_TIME_MS, TimeUnit.MILLISECONDS)
            val city = address?.locality ?: address?.subAdminArea
            val region = address?.adminArea
            if (city != null && region != null) "$city, $region" else null
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val cached = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
        if (cached != null && cached.elapsedRealtimeAgeMillis < LAST_REQUEST_DURATION_MS) {
            return cached
        }

        val cancelSignal = CancellationSignal()
        val future = CompletableFuture<Location?>()

        try {
            locationManager.getCurrentLocation(
                LocationManager.FUSED_PROVIDER,
                cancelSignal,
                context.mainExecutor
            ) { loc -> future.complete(loc) }

            return future.get(MAX_WAIT_FOR_LOCATION_MS, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            cancelSignal.cancel()
            return null
        }
    }

    private fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }
}

actual fun getUtil() : Util = AndroidUtil

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size)?.asImageBitmap() ?:
        throw IllegalArgumentException("Invalid image data")
}