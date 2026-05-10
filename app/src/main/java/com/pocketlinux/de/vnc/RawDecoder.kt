package com.pocketlinux.de.vnc

import android.graphics.Bitmap
import java.io.DataInputStream

class RawDecoder {

    fun decode(
        input: DataInputStream,
        bitmap: Bitmap,
        x: Int, y: Int, w: Int, h: Int
    ) {
        val pixels = IntArray(w * h)
        for (i in 0 until w * h) {
            // Server sends: red(1), green(1), blue(1), padding(1) in our requested format
            val r = input.readUnsignedByte()
            val g = input.readUnsignedByte()
            val b = input.readUnsignedByte()
            input.readUnsignedByte() // padding byte (alpha channel, unused)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(pixels, 0, w, x, y, w, h)
    }
}
