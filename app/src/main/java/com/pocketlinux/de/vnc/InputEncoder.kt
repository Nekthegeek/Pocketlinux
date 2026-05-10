package com.pocketlinux.de.vnc

import java.io.DataOutputStream

class InputEncoder(private val output: DataOutputStream) {

    fun sendPointerEvent(buttonMask: Int, x: Int, y: Int) {
        output.writeByte(RfbProtocol.MSG_POINTER_EVENT.toInt())
        output.writeByte(buttonMask)
        output.writeShort(x)
        output.writeShort(y)
        output.flush()
    }

    fun sendKeyEvent(down: Boolean, keySym: Int) {
        output.writeByte(RfbProtocol.MSG_KEY_EVENT.toInt())
        output.writeByte(if (down) 1 else 0)
        output.writeShort(0) // padding
        output.writeInt(keySym)
        output.flush()
    }

    fun sendKeyPress(keySym: Int) {
        sendKeyEvent(true, keySym)
        sendKeyEvent(false, keySym)
    }

    fun sendCharacter(ch: Char) {
        // Map printable ASCII to X11 keysyms (they are identical for ASCII 0x20-0x7E)
        val keySym = ch.code
        sendKeyPress(keySym)
    }
}
