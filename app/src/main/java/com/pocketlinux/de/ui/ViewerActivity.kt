package com.pocketlinux.de.ui

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pocketlinux.de.R
import com.pocketlinux.de.bootstrap.DistroProfile
import com.pocketlinux.de.bootstrap.SessionLauncher
import com.pocketlinux.de.databinding.ActivityViewerBinding
import com.pocketlinux.de.vnc.RfbClient
import com.pocketlinux.de.vnc.RfbProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewerBinding
    private var rfbClient: RfbClient? = null
    private var profile: DistroProfile? = null
    private lateinit var launcher: SessionLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launcher = SessionLauncher(this)

        val profileId = intent.getStringExtra(EXTRA_PROFILE_ID)
        profile = profileId?.let { DistroProfile.fromId(it) }
        if (profile == null) {
            Toast.makeText(this, R.string.error_unknown_profile, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        binding.btnDisconnect.setOnClickListener { disconnect() }
        binding.btnKeyboard.setOnClickListener {
            binding.vncCanvas.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.vncCanvas, InputMethodManager.SHOW_FORCED)
        }
        binding.btnCtrlAltDel.setOnClickListener { sendCtrlAltDel() }

        connectToVnc()
    }

    private fun connectToVnc() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = getString(R.string.connecting)

        lifecycleScope.launch(Dispatchers.IO) {
            var lastError: Throwable? = null
            repeat(10) { attempt ->
                try {
                    val client = RfbClient(VNC_HOST, VNC_PORT)
                    client.onFramebufferUpdate = {
                        runOnUiThread {
                            binding.vncCanvas.framebuffer = client.framebuffer
                            if (binding.progressBar.visibility == View.VISIBLE) {
                                binding.progressBar.visibility = View.GONE
                                binding.tvStatus.visibility = View.GONE
                            }
                        }
                    }
                    client.onBell = { vibrateOnce() }
                    client.onDisconnect = { err ->
                        runOnUiThread {
                            if (err != null) {
                                Toast.makeText(
                                    this@ViewerActivity,
                                    getString(R.string.disconnected, err.message),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            finish()
                        }
                    }
                    client.connect()
                    rfbClient = client
                    binding.vncCanvas.inputEncoder = client.inputEncoder

                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.tvStatus.visibility = View.GONE
                    }

                    client.runReceiveLoop() // blocks until disconnected
                    return@launch
                } catch (e: Exception) {
                    lastError = e
                    delay(1500L * (attempt + 1))
                }
            }
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = getString(R.string.connection_failed, lastError?.message)
                Toast.makeText(
                    this@ViewerActivity,
                    getString(R.string.connection_failed, lastError?.message),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun disconnect() {
        rfbClient?.disconnect()
        profile?.let { launcher.stopVncServer(it) }
        finish()
    }

    private fun sendCtrlAltDel() {
        val enc = rfbClient?.inputEncoder ?: return
        enc.sendKeyEvent(true, RfbProtocol.KEY_CTRL_L)
        enc.sendKeyEvent(true, RfbProtocol.KEY_ALT_L)
        enc.sendKeyEvent(true, RfbProtocol.KEY_DELETE)
        enc.sendKeyEvent(false, RfbProtocol.KEY_DELETE)
        enc.sendKeyEvent(false, RfbProtocol.KEY_ALT_L)
        enc.sendKeyEvent(false, RfbProtocol.KEY_CTRL_L)
    }

    private fun vibrateOnce() {
        @Suppress("DEPRECATION")
        (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(50)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val keySym = androidKeyToX11(keyCode) ?: return super.onKeyDown(keyCode, event)
        binding.vncCanvas.sendKeyEvent(true, keySym)
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val keySym = androidKeyToX11(keyCode) ?: return super.onKeyUp(keyCode, event)
        binding.vncCanvas.sendKeyEvent(false, keySym)
        return true
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN && event != null) {
            for (ch in event.characters ?: "") {
                binding.vncCanvas.sendCharacter(ch)
            }
            return true
        }
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    private fun androidKeyToX11(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_BACK -> null // let the system handle Back
        KeyEvent.KEYCODE_DEL -> RfbProtocol.KEY_BACKSPACE
        KeyEvent.KEYCODE_FORWARD_DEL -> RfbProtocol.KEY_DELETE
        KeyEvent.KEYCODE_TAB -> RfbProtocol.KEY_TAB
        KeyEvent.KEYCODE_ENTER -> RfbProtocol.KEY_RETURN
        KeyEvent.KEYCODE_ESCAPE -> RfbProtocol.KEY_ESCAPE
        KeyEvent.KEYCODE_DPAD_LEFT -> RfbProtocol.KEY_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> RfbProtocol.KEY_RIGHT
        KeyEvent.KEYCODE_DPAD_UP -> RfbProtocol.KEY_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> RfbProtocol.KEY_DOWN
        KeyEvent.KEYCODE_CTRL_LEFT -> RfbProtocol.KEY_CTRL_L
        KeyEvent.KEYCODE_CTRL_RIGHT -> RfbProtocol.KEY_CTRL_R
        KeyEvent.KEYCODE_ALT_LEFT -> RfbProtocol.KEY_ALT_L
        KeyEvent.KEYCODE_ALT_RIGHT -> RfbProtocol.KEY_ALT_R
        KeyEvent.KEYCODE_META_LEFT, KeyEvent.KEYCODE_META_RIGHT -> RfbProtocol.KEY_SUPER_L
        KeyEvent.KEYCODE_F1 -> RfbProtocol.KEY_F1
        KeyEvent.KEYCODE_F2 -> RfbProtocol.KEY_F1 + 1
        KeyEvent.KEYCODE_F3 -> RfbProtocol.KEY_F1 + 2
        KeyEvent.KEYCODE_F4 -> RfbProtocol.KEY_F1 + 3
        KeyEvent.KEYCODE_F5 -> RfbProtocol.KEY_F1 + 4
        KeyEvent.KEYCODE_F6 -> RfbProtocol.KEY_F1 + 5
        KeyEvent.KEYCODE_F7 -> RfbProtocol.KEY_F1 + 6
        KeyEvent.KEYCODE_F8 -> RfbProtocol.KEY_F1 + 7
        KeyEvent.KEYCODE_F9 -> RfbProtocol.KEY_F1 + 8
        KeyEvent.KEYCODE_F10 -> RfbProtocol.KEY_F1 + 9
        KeyEvent.KEYCODE_F11 -> RfbProtocol.KEY_F1 + 10
        KeyEvent.KEYCODE_F12 -> RfbProtocol.KEY_F1 + 11
        else -> null
    }

    override fun onDestroy() {
        super.onDestroy()
        rfbClient?.disconnect()
    }

    companion object {
        const val EXTRA_PROFILE_ID = "profile_id"
        private const val VNC_HOST = "127.0.0.1"
        private const val VNC_PORT = 5901
    }
}
