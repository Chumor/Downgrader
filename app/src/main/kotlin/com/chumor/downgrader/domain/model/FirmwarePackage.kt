package com.chumor.downgrader.domain.model

import androidx.compose.runtime.Immutable
import java.io.IOException

@Immutable
data class FirmwarePackage(
    val systemVersion: String,
    val androidVersion: String,
    val romVersion: String,
    val sha256: String,
    val fileSizeBytes: Long,
    val releaseTime: String,
    val downloadUrl: String,
    val certificate: String,
)

fun String.isSha256Digest(): Boolean =
    length == 64 && all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }

fun FirmwarePackage.downloadFileName(deviceModel: String): String =
    "${deviceModel}_A_${systemVersion}-update-full.zip"

@Immutable
sealed interface QueryError {
    data object Network : QueryError
    data class HttpStatus(val code: Int) : QueryError
    data class ServerRejected(val code: Int, val serverMessage: String) : QueryError
    data object InvalidResponse : QueryError
    data object Unexpected : QueryError
}

class QueryException(
    val error: QueryError,
    cause: Throwable? = null,
) : IOException(cause)
