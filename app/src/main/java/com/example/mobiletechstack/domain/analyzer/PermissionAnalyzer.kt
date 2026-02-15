package com.example.mobiletechstack.domain.analyzer

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.example.mobiletechstack.domain.model.PermissionInfo
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

            permissions.add(
                PermissionInfo(
                    name = permissionName,
                    granted = granted
                )
            )
        }

        return permissions
    }
}