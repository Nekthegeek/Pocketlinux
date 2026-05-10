package com.pocketlinux.de.bootstrap

import android.content.Context
import com.pocketlinux.de.termux.TermuxBridge

class SessionLauncher(private val context: Context) {

    private val bridge = TermuxBridge(context)

    /**
     * Install proot-distro, the chosen Linux distro, and run its bootstrap script.
     * Opens a visible Termux terminal so the user can watch progress.
     */
    fun installAndBootstrap(profile: DistroProfile) {
        val command = BootstrapScript.buildInstallCommand(context, profile)
        bridge.runInTerminal(command)
    }

    /**
     * Start the VNC server inside the proot container (silent background task).
     * The server listens on localhost:5901 with no password (loopback-safe).
     */
    fun startVncServer(profile: DistroProfile) {
        bridge.runInBackground(BootstrapScript.buildVncStartCommand(profile))
    }

    /**
     * Kill the VNC server inside the proot container.
     */
    fun stopVncServer(profile: DistroProfile) {
        bridge.runInBackground(BootstrapScript.buildVncStopCommand(profile))
    }
}
