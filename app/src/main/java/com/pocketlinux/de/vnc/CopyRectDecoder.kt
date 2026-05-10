package com.pocketlinux.de.vnc

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import java.io.DataInputStream

class CopyRectDecoder {

    fun decode(
        input: DataInputStream,
        bitmap: Bitmap,
        dstX: Int, dstY: Int, w: Int, h: Int
    ) {
        val srcX = input.readUnsignedShort()
        val srcY = input.readUnsignedShort()

        val src = Bitmap.createBitmap(bitmap, srcX, srcY, w, h)
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(src, null, Rect(dstX, dstY, dstX + w, dstY + h), null)
        src.recycle()
    }
}
