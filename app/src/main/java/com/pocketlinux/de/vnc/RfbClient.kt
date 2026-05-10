package com.pocketlinux.de.vnc

import android.graphics.Bitmap
import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class RfbClient(
    private val host: String,
    private val port: Int,
    private val password: String? = null
) {
    private val tag = "RfbClient"

    private lateinit var socket: Socket
    private lateinit var input: DataInputStream
    private lateinit var output: DataOutputStream

    private val running = AtomicBoolean(false)

    var framebuffer: Bitmap? = null
        private set

    var desktopWidth = 0
        private set
    var desktopHeight = 0
        private set

    lateinit var inputEncoder: InputEncoder
        private set

    var onFramebufferUpdate: (() -> Unit)? = null
    var onBell: (() -> Unit)? = null
    var onDisconnect: ((Throwable?) -> Unit)? = null

    fun connect() {
        socket = Socket(host, port)
        socket.tcpNoDelay = true
        input = DataInputStream(socket.getInputStream())
        output = DataOutputStream(socket.getOutputStream())
        inputEncoder = InputEncoder(output)

        performHandshake()
        performAuth()
        performInit()
        sendSetPixelFormat()
        sendSetEncodings()
        requestFullUpdate()
        running.set(true)
    }

    private fun performHandshake() {
        val serverProto = ByteArray(12)
        input.readFully(serverProto)
        Log.d(tag, "Server protocol: ${String(serverProto).trim()}")
        output.write(RfbProtocol.VERSION.toByteArray(Charsets.US_ASCII))
        output.flush()
    }

    private fun performAuth() {
        val numTypes = input.readUnsignedByte()
        if (numTypes == 0) {
            val reasonLen = input.readInt()
            val reason = ByteArray(reasonLen)
            input.readFully(reason)
            throw IOException("Server refused connection: ${String(reason)}")
        }

        val types = ByteArray(numTypes)
        input.readFully(types)
        Log.d(tag, "Security types offered: ${types.toList()}")

        when {
            RfbProtocol.SEC_NONE in types -> {
                output.writeByte(RfbProtocol.SEC_NONE.toInt())
                output.flush()
                val result = input.readInt()
                if (result != 0) throw IOException("Security handshake failed (None)")
            }
            RfbProtocol.SEC_VNC_AUTH in types -> {
                output.writeByte(RfbProtocol.SEC_VNC_AUTH.toInt())
                output.flush()
                val challenge = ByteArray(16)
                input.readFully(challenge)
                val response = encryptVncAuth(password ?: "", challenge)
                output.write(response)
                output.flush()
                val result = input.readInt()
                if (result != 0) {
                    val reasonLen = input.readInt()
                    val reason = ByteArray(reasonLen)
                    input.readFully(reason)
                    throw IOException("VNC auth failed: ${String(reason)}")
                }
            }
            else -> throw IOException("No supported security type in ${types.toList()}")
        }
    }

    private fun performInit() {
        output.writeByte(1) // shared desktop
        output.flush()

        desktopWidth = input.readUnsignedShort()
        desktopHeight = input.readUnsignedShort()
        Log.d(tag, "Desktop size: ${desktopWidth}x${desktopHeight}")

        // Skip pixel format (16 bytes) and desktop name
        input.skipBytes(16)
        val nameLen = input.readInt()
        input.skipBytes(nameLen)

        framebuffer = Bitmap.createBitmap(desktopWidth, desktopHeight, Bitmap.Config.ARGB_8888)
    }

    // Ask for RGBX 32bpp little-endian (red-shift=16, green-shift=8, blue-shift=0)
    private fun sendSetPixelFormat() {
        output.writeByte(RfbProtocol.MSG_SET_PIXEL_FORMAT.toInt())
        output.writeByte(0); output.writeByte(0); output.writeByte(0) // padding
        // PixelFormat struct (16 bytes)
        output.writeByte(32)   // bits-per-pixel
        output.writeByte(24)   // depth
        output.writeByte(0)    // big-endian = false
        output.writeByte(1)    // true-colour = true
        output.writeShort(255) // red-max
        output.writeShort(255) // green-max
        output.writeShort(255) // blue-max
        output.writeByte(16)   // red-shift
        output.writeByte(8)    // green-shift
        output.writeByte(0)    // blue-shift
        output.writeByte(0); output.writeByte(0); output.writeByte(0) // padding
        output.flush()
    }

    private fun sendSetEncodings() {
        output.writeByte(RfbProtocol.MSG_SET_ENCODINGS.toInt())
        output.writeByte(0) // padding
        output.writeShort(2) // number of encodings
        output.writeInt(RfbProtocol.ENC_RAW)
        output.writeInt(RfbProtocol.ENC_COPY_RECT)
        output.flush()
    }

    fun requestFullUpdate() {
        val fb = framebuffer ?: return
        sendUpdateRequest(incremental = false, 0, 0, fb.width, fb.height)
    }

    fun requestIncrementalUpdate() {
        val fb = framebuffer ?: return
        sendUpdateRequest(incremental = true, 0, 0, fb.width, fb.height)
    }

    private fun sendUpdateRequest(incremental: Boolean, x: Int, y: Int, w: Int, h: Int) {
        output.writeByte(RfbProtocol.MSG_FRAMEBUFFER_UPDATE_REQUEST.toInt())
        output.writeByte(if (incremental) 1 else 0)
        output.writeShort(x)
        output.writeShort(y)
        output.writeShort(w)
        output.writeShort(h)
        output.flush()
    }

    fun runReceiveLoop() {
        val rawDecoder = RawDecoder()
        val copyRectDecoder = CopyRectDecoder()
        try {
            while (running.get()) {
                when (val msgType = input.readUnsignedByte()) {
                    RfbProtocol.MSG_FRAMEBUFFER_UPDATE.toInt() -> {
                        input.readUnsignedByte() // padding
                        val numRects = input.readUnsignedShort()
                        val fb = framebuffer ?: break
                        repeat(numRects) {
                            val rx = input.readUnsignedShort()
                            val ry = input.readUnsignedShort()
                            val rw = input.readUnsignedShort()
                            val rh = input.readUnsignedShort()
                            val encoding = input.readInt()
                            when (encoding) {
                                RfbProtocol.ENC_RAW -> rawDecoder.decode(input, fb, rx, ry, rw, rh)
                                RfbProtocol.ENC_COPY_RECT -> copyRectDecoder.decode(input, fb, rx, ry, rw, rh)
                                else -> Log.w(tag, "Unsupported encoding: $encoding, rect ${rw}x${rh}")
                            }
                        }
                        onFramebufferUpdate?.invoke()
                        requestIncrementalUpdate()
                    }
                    RfbProtocol.MSG_SET_COLOUR_MAP_ENTRIES.toInt() -> {
                        input.readUnsignedByte() // padding
                        input.readUnsignedShort() // first-colour
                        val numColours = input.readUnsignedShort()
                        input.skipBytes(numColours * 6)
                    }
                    RfbProtocol.MSG_BELL.toInt() -> onBell?.invoke()
                    RfbProtocol.MSG_SERVER_CUT_TEXT.toInt() -> {
                        input.skipBytes(3) // padding
                        val len = input.readInt()
                        input.skipBytes(len)
                    }
                    else -> {
                        Log.e(tag, "Unknown server message type: $msgType — disconnecting")
                        break
                    }
                }
            }
        } catch (e: IOException) {
            if (running.get()) onDisconnect?.invoke(e)
        } finally {
            running.set(false)
        }
    }

    fun disconnect() {
        running.set(false)
        runCatching { socket.close() }
        onDisconnect?.invoke(null)
    }

    // VNC uses DES with bits reversed within each byte of the key
    private fun encryptVncAuth(password: String, challenge: ByteArray): ByteArray {
        val keyBytes = ByteArray(8)
        val pwBytes = password.toByteArray(Charsets.ISO_8859_1)
        for (i in 0 until minOf(8, pwBytes.size)) {
            keyBytes[i] = reverseBits(pwBytes[i])
        }
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "DES"))
        return cipher.doFinal(challenge)
    }

    private fun reverseBits(b: Byte): Byte {
        var v = b.toInt() and 0xFF
        var r = 0
        repeat(8) {
            r = (r shl 1) or (v and 1)
            v = v shr 1
        }
        return r.toByte()
    }
}
