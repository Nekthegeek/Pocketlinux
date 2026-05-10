package com.pocketlinux.de.bootstrap

enum class DistroProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val prootName: String,
    val bootstrapAsset: String,
    val sizeEstimate: String
) {
    ALPINE_I3(
        id = "alpine_i3",
        displayName = "Alpine + i3",
        description = "Minimal (~150 MB) · keyboard-driven tiling WM",
        prootName = "alpine",
        bootstrapAsset = "alpine-i3.sh",
        sizeEstimate = "~150 MB"
    ),
    DEBIAN_I3(
        id = "debian_i3",
        displayName = "Debian + i3",
        description = "Stable base (~400 MB) with i3 tiling WM",
        prootName = "debian",
        bootstrapAsset = "debian-i3.sh",
        sizeEstimate = "~400 MB"
    ),
    DEBIAN_XFCE(
        id = "debian_xfce",
        displayName = "Debian + XFCE",
        description = "Full classic desktop (~900 MB) on Debian",
        prootName = "debian",
        bootstrapAsset = "debian-xfce.sh",
        sizeEstimate = "~900 MB"
    ),
    UBUNTU_OPENBOX(
        id = "ubuntu_openbox",
        displayName = "Ubuntu + Openbox",
        description = "Ubuntu base (~600 MB) · lightweight Openbox WM",
        prootName = "ubuntu",
        bootstrapAsset = "ubuntu-openbox.sh",
        sizeEstimate = "~600 MB"
    );

    companion object {
        fun fromId(id: String): DistroProfile? = entries.firstOrNull { it.id == id }
    }
}
