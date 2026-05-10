package com.pocketlinux.de.bootstrap

import android.content.Context
import android.util.Base64

object BootstrapScript {

    // Install proot-distro, install the chosen distro, then run the bootstrap inside it.
    // The script is base64-encoded to survive safe transit through the shell command string.
    fun buildInstallCommand(context: Context, profile: DistroProfile): String {
        val scriptBytes = context.assets.open(profile.bootstrapAsset).readBytes()
        val b64 = Base64.encodeToString(scriptBytes, Base64.NO_WRAP)
        return buildString {
            append("set -e; ")
            append("echo '--- [PocketLinux] Installing proot-distro ---'; ")
            append("pkg install -y proot-distro 2>&1 || true; ")
            append("echo '--- [PocketLinux] Installing ${profile.displayName} ---'; ")
            append("proot-distro install ${profile.prootName} 2>&1 || ")
            append("echo '${profile.prootName} already installed, continuing...'; ")
            append("echo '--- [PocketLinux] Running bootstrap ---'; ")
            append("echo '$b64' | base64 -d | ")
            append("proot-distro login ${profile.prootName} -- bash; ")
            append("echo '--- [PocketLinux] Bootstrap complete! ---'")
        }
    }

    // Start TigerVNC on display :1 with no authentication (safe for loopback).
    fun buildVncStartCommand(profile: DistroProfile): String =
        "proot-distro login ${profile.prootName} -- " +
                "bash -c 'vncserver :1 -geometry 1280x720 -depth 24 -SecurityTypes None 2>&1 | tail -8'"

    // Stop the VNC server.
    fun buildVncStopCommand(profile: DistroProfile): String =
        "proot-distro login ${profile.prootName} -- " +
                "bash -c 'vncserver -kill :1 2>/dev/null; true'"

    // Check whether vncserver :1 is currently running inside the container.
    // Writes the string "RUNNING" to stdout if it is.
    fun buildVncCheckCommand(profile: DistroProfile): String =
        "proot-distro login ${profile.prootName} -- " +
                "bash -c '[ -e /tmp/.X11-unix/X1 ] && echo RUNNING || echo STOPPED'"
}
