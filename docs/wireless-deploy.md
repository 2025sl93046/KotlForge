# Wireless Device Deployment

KotlForge can build and deploy APKs directly to a physical Android device over WiFi — no USB cable required. This works on Android 11 and above.

---

## Prerequisites

- Android device running Android 11 or higher
- Device and Mac on the same WiFi network
- Android SDK installed with `adb` available (`ANDROID_HOME` set)

---

## Step 1 — Enable Wireless Debugging on your phone

1. Go to **Settings → About Phone**
2. Tap **Build Number** 7 times to enable Developer Options
3. Go to **Settings → Developer Options**
4. Enable **Wireless Debugging**

---

## Step 2 — Pair your phone

1. In **Developer Options → Wireless Debugging**, tap **"Pair device with pairing code"**
2. Note the IP address, port, and 6-digit pairing code shown on screen
3. Run:

```bash
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./cli/build/install/cli/bin/cli deploy --pair 192.168.x.x:XXXXX --code XXXXXX
```

Expected output:
```
Pairing with 192.168.x.x:XXXXX...
Paired with 192.168.x.x:XXXXX
```

---

## Step 3 — Connect to your phone

1. In **Developer Options → Wireless Debugging**, note the IP address and port shown at the top of the screen (different from the pairing port)
2. Run:

```bash
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./cli/build/install/cli/bin/cli deploy --connect 192.168.x.x:XXXXX
```

Expected output:
```
Connecting to 192.168.x.x:XXXXX...
Connected to 192.168.x.x:XXXXX (saved for reuse)
```

The device is saved to `~/.kotlforge/devices.json` so you don't need to reconnect next time (as long as Wireless Debugging stays enabled).

---

## Step 4 — Build and deploy

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./cli/build/install/cli/bin/cli deploy --project /path/to/android-project
```

Expected output:
```
Building debug variant at: /path/to/android-project
...
Build successful
Installing /path/to/android-project/app/build/outputs/apk/debug/app-debug.apk...
Installed on 192.168.x.x:XXXXX
```

---

## Options

| Flag | Description |
|---|---|
| `--project` / `-p` | Path to the Android project root |
| `--variant` / `-v` | Build variant (`debug` or `release`), default `debug` |
| `--device` / `-d` | Target a specific device by serial when multiple are connected |
| `--pair` | Pair with a new device: `--pair <ip:port> --code <code>` |
| `--connect` | Connect to a paired device: `--connect <ip:port>` |

---

## Known device quirks

| Device | Issue | Fix |
|---|---|---|
| Samsung | Developer Options hidden under "Software Information" | Tap Build Number in Software Information |
| Xiaomi / MIUI | Additional "Mi Unlock Status" step required | Enable "USB Debugging (Security Settings)" in Developer Options |
| OnePlus | Wireless Debugging may turn off after reboot | Re-enable in Developer Options after each reboot |
| All devices | Pairing port and connection port are different | Use pairing port with `--pair`, connection port with `--connect` |

---

## Troubleshooting

| Error | Cause | Fix |
|---|---|---|
| `ADB not found` | `ANDROID_HOME` not set or adb not in PATH | Set `ANDROID_HOME` before running |
| `No devices connected` | Device not connected or Wireless Debugging off | Enable Wireless Debugging on device |
| `Pairing failed` | Wrong pairing code or code expired | Generate a new code in Wireless Debugging settings |
| `Connection failed` | Wrong IP/port or device not on same network | Check IP in Developer Options → Wireless Debugging |
| `Multiple devices connected` | More than one device connected | Use `--device <serial>` to target a specific one |
