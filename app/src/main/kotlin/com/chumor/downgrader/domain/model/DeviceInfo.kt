package com.chumor.downgrader.domain.model

import androidx.compose.runtime.Immutable
import java.io.IOException

@Immutable
data class DeviceInfo(
    val model: String?,
    val softwareVersion: String?,
    val productName: String?,
    val serial: String?,
    val androidVersion: String?,
)

data class DeviceReadResult(
    val info: DeviceInfo,
    val credential: String,
)

@Immutable
sealed interface DeviceReadError {
    data object Unavailable : DeviceReadError
    data class Failed(val exitCode: Int) : DeviceReadError
    data object Interrupted : DeviceReadError
}

class DeviceReadException(
    val error: DeviceReadError,
    cause: Throwable? = null,
) : IOException(cause)
