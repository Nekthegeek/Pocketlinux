# PocketLinux

A Linux desktop environment for Android, built on Termux + proot + VNC. Runs on basically any
Android 7+ device — no root, no AVF, no virtualization, no kernel features needed.

The app lets the user pick a distro/WM combo at install time, bootstraps it through Termux,
launches a VNC server inside, and connects to it through a built-in viewer. One tap from icon
to working desktop.

## Why this approach

The first version of this project targeted Android's Virtualization Framework. AVF is the
"correct" answer technically — real kernel, real isolation, GPU acceleration — but it locks you
out of every device that isn't a Pixel 6+ on Android 15+, which is most phones in the world.

Termux + proot is the pragmatic answer:

- Works on any Android 7+ device with ~1 GB free RAM
- No root required
- Uses Android's existing kernel, so no kernel feature gates
- Termux is the most battle-tested Linux-on-Android project; we don't reinvent it
- VNC over loopback is fast enough that the protocol's compression weakness doesn't matter

Trade-offs we accept:

- proot is slower than a real chroot (no `chroot` syscall on unrooted Android)
- No GPU acceleration for the Linux side (X server runs in software)
- Audio is doable but fiddly; not in scope for v1
- Single-user phone-class hardware means heavy DEs (GNOME, KDE) won't fly

## Architecture

```
┌──────────────────────────────────────────────────┐
│ PocketLinux (this app)                           │
│                                                  │
│ ┌──────────────┐  ┌────────────────────────────┐ │
│ │ Distro       │  │ Built-in VNC Viewer        │ │
│ │ picker       │  │ (pure Kotlin RFB 3.8)      │ │
│ │ Bootstrap UI │  │ Renders to Canvas          │ │
│ └──────┬───────┘  └────────────┬───────────────┘ │
│        │                       │ TCP localhost   │
└────────┼───────────────────────┼─────────────────┘
         │ RUN_COMMAND intent    │ 5901
┌────────▼───────────────────────▼─────────────────┐
│ Termux                                           │
│  ┌────────────────────────────────────────────┐  │
│  │ proot-distro container (Debian/Alpine/etc) │  │
│  │   ├─ TigerVNC server                       │  │
│  │   ├─ Window manager (i3 / XFCE / Openbox)  │  │
│  │   └─ user apps                             │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

## Distros offered (chosen at install time)

| Combo | Disk | Suits |
| --- | --- | --- |
| Alpine + i3 | ~150 MB after install | Old/weak phones, devs who like minimalism |
| Debian + i3 | ~400 MB | Most users, best package availability |
| Debian + XFCE | ~900 MB | Newer phones, "feels like a real desktop" |
| Ubuntu + Openbox | ~600 MB | Familiar to Ubuntu users, lightweight WM |

The picker is shown the first time the app launches. Choice is stored; user can wipe and
re-pick from settings.

## Requirements

- Android 7.0+ (API 24+)
- Termux installed from F-Droid or GitHub (the Play Store version is broken — see below)
- ~1 GB free storage for the smallest combo, ~2 GB for XFCE
- ~1 GB free RAM at runtime

### Why Termux from F-Droid / GitHub specifically

The Play Store version of Termux is abandoned and ships with `allow-external-apps=false` baked
in, which means our `RUN_COMMAND` intents will never execute. The app detects Play Store Termux
and tells the user to reinstall from F-Droid.

## Build

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## What's in the box

```
app/src/main/
├── java/com/pocketlinux/de/
│   ├── PocketLinuxApp.kt           Application class
│   ├── ui/
│   │   ├── MainActivity.kt         Entry, distro picker, status
│   │   ├── ViewerActivity.kt       Hosts the VNC viewer
│   │   ├── DistroPickerFragment.kt One-time install-flow UI
│   │   └── VncCanvasView.kt        Custom view that draws RFB framebuffer
│   ├── termux/
│   │   ├── TermuxBridge.kt         RUN_COMMAND intent dispatcher
│   │   └── TermuxStatus.kt         Detects Termux installed/usable
│   ├── bootstrap/
│   │   ├── DistroProfile.kt        Distro/WM combo definitions
│   │   ├── BootstrapScript.kt      Generates the proot-distro setup commands
│   │   └── SessionLauncher.kt      Starts VNC server in the chosen distro
│   └── vnc/
│       ├── RfbClient.kt            Pure-Kotlin RFB 3.8 client
│       ├── RfbProtocol.kt          Wire format constants and helpers
│       ├── RawDecoder.kt           Raw encoding (mandatory baseline)
│       ├── CopyRectDecoder.kt      CopyRect encoding (huge perf win)
│       └── InputEncoder.kt         Pointer/keyboard event encoding
└── assets/distros/                 Bootstrap shell scripts per distro
    ├── alpine-i3.sh
    ├── debian-i3.sh
    ├── debian-xfce.sh
    └── ubuntu-openbox.sh
```

## Status

Working scaffold. The pure-Kotlin RFB client implements the parts needed for localhost
(security types None + VNC-Auth, Raw + CopyRect encodings, pointer + keyboard events).
Tight/ZRLE encoding is not implemented — over loopback the bandwidth doesn't matter.
