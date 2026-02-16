package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.mobiletechstack.domain.model.PermissionInfo
import com.example.mobiletechstack.domain.model.PermissionCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PermissionAnalyzer(private val context: Context) {

    private val packageManager = context.packageManager

    suspend fun extractPermissions(packageName: String): List<PermissionInfo> = withContext(Dispatchers.IO) {
        try {
            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            )

            parsePermissions(packageInfo)

        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parsePermissions(packageInfo: PackageInfo): List<PermissionInfo> {
        val permissions = mutableListOf<PermissionInfo>()

        val requestedPermissions = packageInfo.requestedPermissions
        val permissionsFlags = packageInfo.requestedPermissionsFlags

        if (requestedPermissions == null) {
            return emptyList()
        }

        requestedPermissions.forEachIndexed { index, permissionName ->
            val granted = if (permissionsFlags != null && index < permissionsFlags.size) {
                (permissionsFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            } else {
                false
            }

            val category = categorizePermission(permissionName)

            permissions.add(
                PermissionInfo(
                    name = permissionName,
                    granted = granted,
                    category = category
                )
            )
        }

        return permissions
    }

    private fun categorizePermission(permissionName: String): PermissionCategory {
        val name = permissionName.uppercase()

        return when {
            name.contains("CAMERA") -> PermissionCategory.CAMERA

            name.contains("LOCATION") ||
                    name.contains("GPS") ||
                    name.contains("FINE_LOCATION") ||
                    name.contains("COARSE_LOCATION") ||
                    name.contains("BACKGROUND_LOCATION") -> PermissionCategory.LOCATION

            name.contains("STORAGE") ||
                    name.contains("EXTERNAL") ||
                    name.contains("READ_MEDIA") ||
                    name.contains("WRITE_MEDIA") ||
                    name.contains("MANAGE_MEDIA") ||
                    name.contains("MANAGE_EXTERNAL_STORAGE") -> PermissionCategory.STORAGE

            name.contains("MICROPHONE") ||
                    name.contains("RECORD_AUDIO") -> PermissionCategory.MICROPHONE

            name.contains("CONTACTS") ||
                    name.contains("READ_CONTACTS") ||
                    name.contains("WRITE_CONTACTS") ||
                    name.contains("GET_ACCOUNTS") -> PermissionCategory.CONTACTS

            name.contains("PHONE") ||
                    name.contains("CALL") ||
                    name.contains("READ_PHONE_STATE") ||
                    name.contains("CALL_PHONE") ||
                    name.contains("ANSWER_PHONE_CALLS") ||
                    name.contains("READ_CALL_LOG") ||
                    name.contains("WRITE_CALL_LOG") -> PermissionCategory.PHONE

            name.contains("SMS") ||
                    name.contains("MMS") ||
                    name.contains("READ_SMS") ||
                    name.contains("SEND_SMS") ||
                    name.contains("RECEIVE_SMS") -> PermissionCategory.SMS

            name.contains("CALENDAR") ||
                    name.contains("READ_CALENDAR") ||
                    name.contains("WRITE_CALENDAR") -> PermissionCategory.CALENDAR

            name.contains("SENSOR") ||
                    name.contains("BODY_SENSORS") ||
                    name.contains("ACTIVITY_RECOGNITION") ||
                    name.contains("STEP") -> PermissionCategory.SENSORS

            name.contains("INTERNET") ||
                    name.contains("NETWORK") ||
                    name.contains("ACCESS_NETWORK_STATE") ||
                    name.contains("ACCESS_WIFI_STATE") ||
                    name.contains("CHANGE_NETWORK_STATE") ||
                    name.contains("CHANGE_WIFI_STATE") -> PermissionCategory.NETWORK

            name.contains("BLUETOOTH") ||
                    name.contains("NFC") ||
                    name.contains("BLUETOOTH_SCAN") ||
                    name.contains("BLUETOOTH_CONNECT") ||
                    name.contains("BLUETOOTH_ADVERTISE") -> PermissionCategory.BLUETOOTH

            name.contains("SYSTEM") ||
                    name.contains("WRITE_SETTINGS") ||
                    name.contains("PACKAGE") ||
                    name.contains("INSTALL") ||
                    name.contains("DELETE") ||
                    name.contains("REBOOT") ||
                    name.contains("WAKE_LOCK") ||
                    name.contains("VIBRATE") ||
                    name.contains("FLASHLIGHT") ||
                    name.contains("MODIFY_AUDIO_SETTINGS") -> PermissionCategory.SYSTEM

            else -> PermissionCategory.OTHER
        }
    }
}