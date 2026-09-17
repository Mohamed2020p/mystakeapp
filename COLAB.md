# Build the APK on Google Colab (free, no PC needed)

You have **2 options**. Option A is the easiest.

---

## Option A — Ready-made notebook (recommended)

1. Go to [Google Colab](https://colab.research.google.com/) → **File → Upload notebook**
2. Upload **`colab_build.ipynb`** from this repo
3. Click **Runtime → Run all**
4. Wait ~5–10 minutes (first run installs Java + Android SDK + Gradle)
5. `app-debug.apk` auto-downloads at the end → send it to your phone and install
   (allow **"Install unknown apps"** when Android asks)

---

## Option B — Paste commands into an empty notebook

Create an empty notebook and run these cells **top to bottom**:

**Cell 1 — Java 17**

```python
import os
!sudo apt-get update -qq
!sudo apt-get install -y -qq openjdk-17-jdk unzip wget > /dev/null
os.environ['JAVA_HOME'] = '/usr/lib/jvm/java-17-openjdk-amd64'
os.environ['PATH'] = os.environ['JAVA_HOME'] + '/bin:' + os.environ['PATH']
!java -version
```

**Cell 2 — Android SDK**

```python
import os
os.environ['ANDROID_HOME'] = '/opt/android-sdk'
os.environ['ANDROID_SDK_ROOT'] = '/opt/android-sdk'
os.environ['PATH'] = '/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools:' + os.environ['PATH']

!mkdir -p /opt/android-sdk/cmdline-tools
!wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip
!rm -rf /opt/android-sdk/cmdline-tools/latest /opt/android-sdk/cmdline-tools/cmdline-tools
!unzip -q -o /tmp/cmdtools.zip -d /opt/android-sdk/cmdline-tools
!mv /opt/android-sdk/cmdline-tools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
!yes | sdkmanager --licenses > /dev/null
!sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

**Cell 3 — Gradle 8.7**

```python
import os
os.environ['PATH'] = '/opt/gradle-8.7/bin:' + os.environ['PATH']
!wget -q https://services.gradle.org/distributions/gradle-8.7-bin.zip -O /tmp/gradle.zip
!rm -rf /opt/gradle-8.7
!unzip -q -o /tmp/gradle.zip -d /opt
!gradle --version
```

**Cell 4 — Download the source code**

```python
!rm -rf /content/mystakeapp
!git clone --branch arena/01a0aca7-mystakeapp --depth 1 https://github.com/Mohamed2020p/mystakeapp.git /content/mystakeapp
!ls /content/mystakeapp
```

**Cell 5 — Build the APK**

```python
!cd /content/mystakeapp && gradle assembleDebug --no-daemon
```

**Cell 6 — Download the APK**

```python
!ls -lh /content/mystakeapp/app/build/outputs/apk/debug/
from google.colab import files
files.download('/content/mystakeapp/app/build/outputs/apk/debug/app-debug.apk')
```

---

## Optional — Signed Release APK

Run this **after** the cells above. Change `mystake123` to your own passwords,
and **back up `release.keystore`** — lose it and you can never update the app.

```python
import os
os.environ['PATH'] = '/opt/gradle-8.7/bin:/opt/android-sdk/build-tools/34.0.0:' + os.environ['PATH']

# 1) Create signing key (only if it doesn't exist yet)
!test -f /content/mystakeapp/release.keystore || keytool -genkeypair -v -keystore /content/mystakeapp/release.keystore -alias mystake -keyalg RSA -keysize 2048 -validity 10000 -storepass mystake123 -keypass mystake123 -dname "CN=MyStake App" 2>&1 | tail -3

# 2) Build unsigned release
!cd /content/mystakeapp && gradle assembleRelease --no-daemon

# 3) Align + sign + verify
!zipalign -v -p 4 /content/mystakeapp/app/build/outputs/apk/release/app-release-unsigned.apk /content/mystakeapp/app-release-aligned.apk 2>&1 | tail -2
!apksigner sign --ks /content/mystakeapp/release.keystore --ks-pass:pass:mystake123 --key-pass:pass:mystake123 --out /content/mystakeapp/app-release.apk /content/mystakeapp/app-release-aligned.apk
!apksigner verify --print-certs /content/mystakeapp/app-release.apk 2>&1 | head -5
!ls -lh /content/mystakeapp/app-release.apk

from google.colab import files
files.download('/content/mystakeapp/app-release.apk')
files.download('/content/mystakeapp/release.keystore')  # BACK THIS UP
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Build is slow / looks stuck | First build downloads ~500MB of dependencies — wait up to 10 min |
| `sdkmanager: command not found` | Re-run Cells 1–2 (env vars reset if the runtime restarted) |
| `JAVA_HOME is not set` | Re-run Cell 1 |
| Colab disconnected | Runtime → Reconnect, then re-run from Cell 1 (Colab is ephemeral, reinstalls each session) |
| Test page shows error | Your `test.html` must exist on the hosting. Right now `https://chciken2website.rf.gd/test.html` returns **404** — upload the file first, then the app will show it (https first, http fallback) |
| App shows mystake.com but game opens test page | That's the intended test override — tap **ORIGINAL** on the chip to see the real game, or change URLs in `AppConfig.kt` |
