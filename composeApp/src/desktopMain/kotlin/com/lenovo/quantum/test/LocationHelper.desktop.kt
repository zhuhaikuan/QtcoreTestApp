/*
 * Copyright (C) 2026 Lenovo
 * All Rights Reserved.
 * Lenovo Confidential Restricted.
 */
package com.lenovo.quantum.test

import com.lenovo.quantum.sdk.logging.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets


@Serializable
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

@Serializable
private data class LocationResult(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val error: String? = null,
    val status: String? = null
)

object WindowsLocationHelper {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getCurrentLatLng(timeoutMs: Long = 10_000): LatLng? =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(timeoutMs) {
                try {
                    val psScript = buildPowerShellScript()

                    val process = ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-ExecutionPolicy", "Bypass",
                        "-Command", psScript
                    )
                        .redirectErrorStream(true)
                        .start()

                    val output = process.inputStream
                        .bufferedReader(StandardCharsets.UTF_8)
                        .use { it.readText().trim() }

                    val exitCode = process.waitFor()
                    if (exitCode != 0 || output.isBlank()) {
                        logE { "Error getting location: $output, exit code: $exitCode" }
                        return@withTimeoutOrNull null
                    }

                    val result = json.decodeFromString<LocationResult>(output)

                    if (result.error != null) return@withTimeoutOrNull null
                    if (result.latitude == null || result.longitude == null) return@withTimeoutOrNull null

                    LatLng(result.latitude, result.longitude)
                } catch (_: Throwable) {
                    null
                }
            }
        }

    private fun buildPowerShellScript(): String = """
        Add-Type -AssemblyName System.Runtime.WindowsRuntime

        ${'$'}asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() |
            Where-Object {
                ${'$'}_.Name -eq 'AsTask' -and
                ${'$'}_.GetParameters().Count -eq 1 -and
                ${'$'}_.IsGenericMethod
            })[0]

        function Await-WinRT([Object] ${'$'}asyncOp, [Type] ${'$'}resultType) {
            ${'$'}asTask = ${'$'}asTaskGeneric.MakeGenericMethod(${ '$' }resultType)
            ${'$'}task = ${'$'}asTask.Invoke(${ '$' }null, @(${ '$' }asyncOp))
            ${'$'}task.Wait()
            return ${'$'}task.Result
        }

        ${'$'}accessOp = [Windows.Devices.Geolocation.Geolocator, Windows, ContentType=WindowsRuntime]::RequestAccessAsync()
        ${'$'}status = Await-WinRT ${'$'}accessOp ([Windows.Devices.Geolocation.GeolocationAccessStatus])

        if (${ '$' }status -ne [Windows.Devices.Geolocation.GeolocationAccessStatus]::Allowed) {
            Write-Output (\"{\"\"error\"\":\"\"not_allowed\"\",\"\"status\"\":\"\"${'$'}status\"\"}\")
            exit 1
        }

        ${'$'}geo = New-Object Windows.Devices.Geolocation.Geolocator
        ${'$'}geo.DesiredAccuracy = [Windows.Devices.Geolocation.PositionAccuracy]::High

        ${'$'}posOp = ${'$'}geo.GetGeopositionAsync()
        ${'$'}pos = Await-WinRT ${'$'}posOp ([Windows.Devices.Geolocation.Geoposition])

        ${'$'}lat = ${'$'}pos.Coordinate.Point.Position.Latitude
        ${'$'}lon = ${'$'}pos.Coordinate.Point.Position.Longitude

        Write-Output (\"{\"\"latitude\"\":${'$'}lat,\"\"longitude\"\":${'$'}lon}\")
    """.trimIndent()

}