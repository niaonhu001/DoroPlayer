package com.example.funplayer

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat

internal class BluetoothPermissionManager(private val context: Context) {
    
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    fun hasAllPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun shouldShowPermissionRationale(): Boolean {
        if (Build.VERSION.SDK_INT < 31) {
            return !hasLocationPermission()
        }
        return !hasBluetoothScanPermission() || !hasBluetoothConnectPermission() || !hasLocationPermission()
    }
    
    private fun hasBluetoothScanPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasBluetoothConnectPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == 
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}