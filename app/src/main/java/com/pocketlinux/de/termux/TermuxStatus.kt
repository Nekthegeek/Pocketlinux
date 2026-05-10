package com.pocketlinux.de.termux

import android.content.Context
import android.content.pm.PackageManager

class TermuxStatus(private val context: Context) {

    fun isInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun hasRunCommandPermission(): Boolean =
        context.checkSelfPermission(TERMUX_RUN_COMMAND_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED

    companion object {
        const val TERMUX_PACKAGE = "com.termux"
        const val TERMUX_RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
        const val TERMUX_HOME = "/data/data/com.termux/files/home"
        const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    }
}
