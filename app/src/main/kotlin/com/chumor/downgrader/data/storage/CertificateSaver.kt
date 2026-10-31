package com.chumor.downgrader.data.storage

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CertificateSaver {
    @Suppress("DEPRECATION")
    fun save(certificate: String): String {
        val bytes = certificate.toByteArray(StandardCharsets.UTF_8)
        val directory = Environment.getExternalStorageDirectory()
        val destination = File(directory, FILE_NAME)
        val temporary = File.createTempFile("dota_sig", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { it.write(bytes) }
            Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            return destination.path
        } finally {
            temporary.delete()
        }
    }

    private companion object {
        const val FILE_NAME = "dota_sig.txt"
    }
}
