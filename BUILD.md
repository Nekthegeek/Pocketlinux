# How to build PocketLinux into an APK

There are two paths. Pick whichever fits your setup.

## Path A: GitHub Actions (no PC required, ~15 min)

If you don't have a computer with Android tools installed — or you're working
from a phone — this is the easiest path. GitHub builds the APK for free on
their servers and gives you a download link.

1. **Create a GitHub account** at https://github.com if you don't have one.

2. **Make a new repository.** Call it whatever — `pocketlinux` is fine.
   Make it public *or* private; both work.

3. **Upload the project.** From a phone you can use the GitHub mobile app or
   the website's "Add file → Upload files" button. Drag in the entire
   `PocketLinux` folder contents (including the hidden `.github/` folder —
   on Android, file managers often hide dot-folders by default; flip the
   "show hidden" toggle).

   From a computer:
   ```bash
   cd PocketLinux
   git init
   git add .
   git commit -m "initial"
   git branch -M main
   git remote add origin https://github.com/YOUR_NAME/pocketlinux.git
   git push -u origin main
   ```

4. **Wait for the build.** Go to the repo's "Actions" tab. You'll see a run
   named "Build APK" appear within a minute. It takes about 5–10 minutes the
   first time (Gradle needs to download dependencies), 2–3 minutes after.

5. **Download the APK.** When the run finishes, scroll to the bottom of the
   run page and click the artifact called `PocketLinux-debug`. You'll get a
   zip file containing `app-debug.apk`.

6. **Install on your phone.** Open the APK on your phone — Android will ask
   you to allow installs from your file manager / browser. Approve, install.

## Path B: Local build with Android Studio (Mac/PC, ~10 min)

1. **Install Android Studio** from https://developer.android.com/studio
   (it's free).

2. **Open the project.** File → Open → pick the `PocketLinux` folder.

3. **Wait for Gradle sync.** Studio will download the Android SDK,
   the build tools, and all dependencies. Takes 5–10 min on first open.

4. **Build the APK.** Build menu → Build Bundle(s) / APK(s) → Build APK(s).
   Or just hit the green Run button with your phone plugged in via USB
   (with USB debugging enabled).

5. **Find the APK** at `app/build/outputs/apk/debug/app-debug.apk`.

## Path C: Command line, if you already have the Android SDK

```bash
cd PocketLinux
gradle wrapper --gradle-version 8.10
./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

---

## After installing

The first launch will tell you to install **Termux** from F-Droid (not the
Play Store — that version is broken). Direct link:
https://f-droid.org/packages/com.termux/

Open Termux once after installing, then run inside it:

```sh
mkdir -p ~/.termux
echo "allow-external-apps=true" >> ~/.termux/termux.properties
```

Then come back to PocketLinux and tap "Re-check". You're good to pick a
distro and start the install.

---

## Troubleshooting

**The Actions build failed on "Generate Gradle wrapper".**
The runner's pre-installed `gradle` may be older than what we need. Edit
`.github/workflows/build.yml` and change `gradle-version 8.10` to `8.7`,
push, retry.

**Studio says "SDK location not found".**
Studio normally creates `local.properties` automatically. If it didn't,
make a `local.properties` next to `settings.gradle.kts` containing one line:
`sdk.dir=/Users/YOU/Library/Android/sdk` (Mac) or
`sdk.dir=C:\\Users\\YOU\\AppData\\Local\\Android\\Sdk` (Windows).

**APK won't install: "App not installed as package conflicts with existing package".**
You probably already have a debug build installed from a previous attempt
with a different signing key. Uninstall the old one first.

**Termux runs my commands but they hang.**
On Xiaomi/MIUI and a few other heavily-customized Androids, the system
kills background services aggressively. Open Settings → Apps → Termux →
disable battery optimization. Same for PocketLinux.
