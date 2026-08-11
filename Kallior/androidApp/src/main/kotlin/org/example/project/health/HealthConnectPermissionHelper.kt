package org.example.project.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord

/**
 * Helper for checking Health Connect availability and permission state.
 *
 * Use [isHealthConnectAvailable] to check whether Health Connect is
 * installed/enabled on the device before trying to read data.
 *
 * Use [hasAllPermissions] to check whether READ_STEPS and READ_SLEEP
 * have been granted by the user.
 */
class HealthConnectPermissionHelper(private val context: Context) {

    /**
     * True when Health Connect is installed and the SDK is available.
     * If this returns false, [getOrCreate] will throw, so always check
     * this first.
     */
    fun isHealthConnectAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    /**
     * True when the user has granted both READ_STEPS and READ_SLEEP.
     * Also returns false if Health Connect itself is unavailable.
     */
    suspend fun hasAllPermissions(): Boolean {
        if (!isHealthConnectAvailable()) return false
        val client = HealthConnectClient.getOrCreate(context)
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(REQUIRED_PERMISSIONS)
    }

    /**
     * Returns the set of permissions that should be requested.
     * Pass this to [PermissionController.createRequestPermissionResultContract].
     */
    fun requiredPermissionSet(): Set<String> = REQUIRED_PERMISSIONS

    companion object {
        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
        )
    }
}
