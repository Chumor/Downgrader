package com.chumor.downgrader.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chumor.downgrader.data.repository.DowngraderRepository
import com.chumor.downgrader.domain.model.DeviceReadError
import com.chumor.downgrader.domain.model.DeviceReadException
import com.chumor.downgrader.domain.model.FirmwareDownloadStatus
import com.chumor.downgrader.domain.model.QueryError
import com.chumor.downgrader.domain.model.QueryException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DowngraderViewModel(
    private val repository: DowngraderRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DowngraderUiState())
    val state = mutableState.asStateFlow()

    private val mutableEffects = Channel<DowngraderUiEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var credential: String? = null

    init {
        // Keep background completions in state and queue their UI effects for the next visible session.
        viewModelScope.launch {
            repository.firmwareDownload.collect { download ->
                val wasDownloadingFirmware = mutableState.value.isDownloadingFirmware
                mutableState.update { it.copy(firmwareDownload = download) }
                if (wasDownloadingFirmware) {
                    when (download.status) {
                        FirmwareDownloadStatus.Completed ->
                            mutableEffects.trySend(DowngraderUiEffect.FirmwareDownloadCompleted)
                        FirmwareDownloadStatus.Failed ->
                            mutableEffects.trySend(DowngraderUiEffect.FirmwareDownloadFailed)
                        else -> Unit
                    }
                }
            }
        }
        loadDevice()
    }

    override fun onCleared() {
        credential = null
        mutableEffects.close()
        super.onCleared()
    }

    private fun loadDevice() {
        mutableState.update { it.copy(isLoadingDevice = true) }
        viewModelScope.launch {
            try {
                val result = repository.readDevice()
                val currentCredential = result.credential.takeIf(String::isNotEmpty)
                credential = currentCredential
                mutableState.update {
                    it.copy(
                        isLoadingDevice = false,
                        device = result.info,
                        credentialAvailable = currentCredential != null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: DeviceReadException) {
                mutableState.update {
                    it.copy(isLoadingDevice = false, deviceError = exception.error)
                }
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(
                        isLoadingDevice = false,
                        deviceError = DeviceReadError.Unavailable,
                    )
                }
            }
        }
    }

    fun queryPackage() {
        val currentCredential = credential ?: return
        if (!mutableState.value.canQueryPackage) return

        mutableState.update {
            it.copy(
                isQuerying = true,
                firmware = null,
                queryError = null,
            )
        }
        viewModelScope.launch {
            try {
                val firmware = repository.queryPackage(currentCredential)
                mutableState.update {
                    it.copy(
                        isQuerying = false,
                        firmware = firmware,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: QueryException) {
                mutableState.update {
                    it.copy(
                        isQuerying = false,
                        queryError = exception.error,
                    )
                }
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(
                        isQuerying = false,
                        queryError = QueryError.Unexpected,
                    )
                }
            }
        }
    }

    fun saveCertificate() {
        val certificate = mutableState.value.firmware?.certificate
            ?.takeIf(String::isNotEmpty)
            ?: return
        if (!mutableState.value.canSaveCertificate) return

        mutableState.update { it.copy(isSavingCertificate = true, savedCertificate = null) }
        viewModelScope.launch {
            val location = try {
                repository.saveCertificate(certificate)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                null
            }

            mutableState.update {
                it.copy(
                    isSavingCertificate = false,
                    savedCertificate = certificate.takeIf { location != null },
                )
            }
            mutableEffects.send(
                if (location == null) {
                    DowngraderUiEffect.CertificateSaveFailed
                } else {
                    DowngraderUiEffect.CertificateSaved(location)
                },
            )
        }
    }

    fun downloadFirmware() {
        val currentState = mutableState.value
        val firmware = currentState.firmware ?: return
        val deviceModel = currentState.device?.model?.takeIf(String::isNotBlank) ?: return
        if (!currentState.canDownloadFirmware) return

        repository.downloadFirmware(firmware, deviceModel)
    }

    fun cancelFirmwareDownload() {
        repository.cancelFirmwareDownload()
    }
}
