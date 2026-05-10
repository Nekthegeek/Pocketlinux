package com.pocketlinux.de.vnc

object RfbProtocol {
    const val VERSION = "RFB 003.008\n"

    // Security types
    const val SEC_NONE: Byte = 1
    const val SEC_VNC_AUTH: Byte = 2

    // Client-to-server message types
    const val MSG_SET_PIXEL_FORMAT: Byte = 0
    const val MSG_SET_ENCODINGS: Byte = 2
    const val MSG_FRAMEBUFFER_UPDATE_REQUEST: Byte = 3
    const val MSG_KEY_EVENT: Byte = 4
    const val MSG_POINTER_EVENT: Byte = 5

    // Server-to-client message types
    const val MSG_FRAMEBUFFER_UPDATE: Byte = 0
    const val MSG_SET_COLOUR_MAP_ENTRIES: Byte = 1
    const val MSG_BELL: Byte = 2
    const val MSG_SERVER_CUT_TEXT: Byte = 3

    // Encoding types
    const val ENC_RAW = 0
    const val ENC_COPY_RECT = 1
    const val ENC_ZRLE = 16

    // PointerEvent button masks
    const val BTN_LEFT = 0x01
    const val BTN_MIDDLE = 0x02
    const val BTN_RIGHT = 0x04
    const val BTN_SCROLL_UP = 0x08
    const val BTN_SCROLL_DOWN = 0x10

    // X11 keysyms (common subset)
    const val KEY_BACKSPACE = 0xFF08
    const val KEY_TAB = 0xFF09
    const val KEY_RETURN = 0xFF0D
    const val KEY_ESCAPE = 0xFF1B
    const val KEY_DELETE = 0xFFFF
    const val KEY_LEFT = 0xFF51
    const val KEY_UP = 0xFF52
    const val KEY_RIGHT = 0xFF53
    const val KEY_DOWN = 0xFF54
    const val KEY_F1 = 0xFFBE
    const val KEY_CTRL_L = 0xFFE3
    const val KEY_CTRL_R = 0xFFE4
    const val KEY_ALT_L = 0xFFE9
    const val KEY_ALT_R = 0xFFEA
    const val KEY_SUPER_L = 0xFFEB
}
