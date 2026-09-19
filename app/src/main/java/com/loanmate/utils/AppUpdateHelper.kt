package com.loanmate.utils

import android.app.Activity
import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppUpdateHelper(private val context: Context) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    sealed class UpdateStatus {
        object Idle : UpdateStatus()
        object Available : UpdateStatus()
        object Downloading : UpdateStatus()
        object Downloaded : UpdateStatus()
        object Failed : UpdateStatus()
    }

    private val listener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                _updateStatus.value = UpdateStatus.Downloading
            }
            InstallStatus.DOWNLOADED -> {
                _updateStatus.value = UpdateStatus.Downloaded
            }
            InstallStatus.FAILED -> {
                _updateStatus.value = UpdateStatus.Failed
            }
            else -> {}
        }
    }

    init {
        appUpdateManager.registerListener(listener)
    }

    fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                if (appUpdateInfo.isFlexibleUpdateAllowed) {
                    _updateStatus.value = UpdateStatus.Available
                } else if (appUpdateInfo.isImmediateUpdateAllowed) {
                    // We can handle immediate updates differently if needed
                    // For now, focusing on background/flexible
                }
            }
        }
    }

    fun startFlexibleUpdate(activity: Activity) {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isFlexibleUpdateAllowed
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun unregisterListener() {
        appUpdateManager.unregisterListener(listener)
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 1001
    }
}
