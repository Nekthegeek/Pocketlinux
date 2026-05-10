package com.pocketlinux.de.termux

import android.content.Context
import android.content.Intent
import android.util.Log

class TermuxBridge(private val context: Context) {

    private val tag = "TermuxBridge"

    /**
     * Run [command] in a new visible Termux terminal session.
     * The user will see the output, which is useful for long-running installs.
     */
    fun runInTerminal(command: String) {
        dispatch(command, background = false)
    }

    /**
     * Run [command] silently in the background.
     * Use for fire-and-forget operations like starting or stopping the VNC server.
     */
    fun runInBackground(command: String) {
        dispatch(command, background = true)
    }

    private fun dispatch(command: String, background: Boolean) {
        val intent = Intent().apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            action = ACTION_RUN_COMMAND
            putExtra(EXTRA_PATH, "${TermuxStatus.TERMUX_PREFIX}/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
            putExtra(EXTRA_WORKDIR, TermuxStatus.TERMUX_HOME)
            putExtra(EXTRA_BACKGROUND, background)
            if (!background) {
                // 0 = switch to the new session; 1 = keep current session
                putExtra(EXTRA_SESSION_ACTION, "0")
            }
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to dispatch command to Termux", e)
        }
    }

    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
        private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        private const val EXTRA_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    }
}
