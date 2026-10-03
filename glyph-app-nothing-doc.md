# Nothing Phone Glyph Toys: Complete Development Guide & Android Limitations

> **The Ultimate Reference for Building Glyph Toys on Nothing Phone (3) and (4a) Pro**
> 
> *Includes: SDK Setup, Development Guide, Existing Toys Catalog, and Critical Android Background Limitations*
> 
> *Last Updated: August 28, 2026*
> *Author: Vibe (for Valentin Bell)*

---

## 📖 Table of Contents

### Part 1: Glyph Toys Development
1. [Introduction to Glyph Toys](#-introduction-to-glyph-toys)
2. [Official SDKs & Development Resources](#-official-sdks--development-resources)
3. [Step-by-Step Development Guide](#-step-by-step-development-guide)
4. [Nothing Playground: Existing Glyph Toys](#-nothing-playground-existing-glyph-toys)
5. [Detailed Toy Analysis](#-detailed-toy-analysis)
6. [Community Resources & Examples](#-community-resources--examples)
7. [Technical Specifications](#-technical-specifications)
8. [Inspiration & Ideas for New Toys](#-inspiration--ideas-for-new-toys)

### Part 2: Android Background Limitations
9. [Critical Android Background Limitations](#-critical-android-background-limitations)
10. [Audio & Microphone Restrictions](#-audio--microphone-restrictions)
11. [Camera Restrictions](#-camera-restrictions)
12. [Location Restrictions](#-location-restrictions)
13. [Sensor Restrictions](#-sensor-restrictions)
14. [Foreground Service Restrictions](#-foreground-service-restrictions)
15. [General Background Execution Limits](#-general-background-execution-limits)
16. [Summary: What You Can & Cannot Do](#-summary-what-you-can--cannot-do)

### Part 3: Practical Development
17. [Best Practices for Glyph Toy Development](#-best-practices-for-glyph-toy-development)
18. [Common Pitfalls & Solutions](#-common-pitfalls--solutions)
19. [Next Steps](#-next-steps)
20. [Additional Resources](#-additional-resources)

---

## 🎯 Part 1: Glyph Toys Development

---

## 🎯 Introduction to Glyph Toys

### What Are Glyph Toys?

**Glyph Toys** are custom interactive experiences for the **Glyph Matrix** — the LED matrix display on the back of Nothing Phone devices. They transform the rear panel into a programmable canvas for animations, games, utilities, and artistic expressions.

### Key Capabilities

- ✅ **Custom Animations**: Create static images, scrolling text, or complex animations
- ✅ **Button Interaction**: Respond to Glyph Button events (short press, long press, touch)
- ✅ **System Integration**: React to notifications, music, charging status, and more
- ✅ **Always-On Display (AOD)**: Run toys when the screen is off
- ✅ **Multi-Frame Animations**: Smooth transitions and dynamic content

### Supported Devices

| Device | Matrix Size | Total LEDs | SDK | OS Version |
|--------|-------------|------------|-----|-------------|
| **Phone (3)** | 25×25 | 625 | GlyphMatrix SDK v2.0 | Nothing OS 3.5+ → 5.0 |
| **Phone (4a) Pro** | 13×13 | 169 | GlyphMatrix SDK v2.0 | Nothing OS 4.0+ |
| Phone (1) | Glyph Light Stripes | 11 segments | Glyph SDK (Legacy) | Older |
| Phone (2)/(2a) | Glyph Light Stripes | 11 segments | Glyph SDK (Legacy) | Older |

> **Note**: This guide focuses on **Phone (3)** and **Phone (4a) Pro** with Glyph Matrix displays.

---

## 📦 Official SDKs & Development Resources

### 1. Primary SDK: GlyphMatrix Developer Kit

**Repository**: [Nothing-Developer-Programme/GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)

**Purpose**: Core SDK for building Glyph Toys for Phone (3) and (4a) Pro

**Key Components**:
- `GlyphMatrixSDK.aar` - Android library file
- Full API documentation
- Java and Kotlin examples
- GlyphMatrixFrame builder
- GlyphButton event handlers

**Latest Release**: V1.1 (August 2025)
- Optimized third-party app usage
- Object brightness multiplier capped at 255; raw matrix intensity uses 0–4095
- New proprietary methods for app integration
- Requires system version **20250801** or later

---

### 2. Example Project

**Repository**: [Nothing-Developer-Programme/GlyphMatrix-Example-Project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project)

**Purpose**: Complete demo with working Glyph Toys

**Features**:
- Pre-configured with GlyphMatrix SDK
- Multiple toy examples
- Kotlin wrapper: `GlyphMatrixService.kt`
- Ready to run in Android Studio

---

### 3. Legacy SDK (Glyph Light Stripes)

**Repository**: [Nothing-Developer-Programme/Glyph-Developer-Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit)

**Purpose**: For Phone (1), (2), (2a) with Glyph Light Stripes (not Matrix)

**Note**: Glyph SDK and GlyphMatrix SDK are the same. Use the single AAR for integration.

---

## 🛠️ Step-by-Step Development Guide

### Prerequisites

#### Hardware Requirements
- ✅ **Nothing Phone (3)** or **Phone (4a) Pro**
- ✅ **Nothing OS 3.5+** (for Phone 3) or compatible firmware
- ✅ **USB Cable** for debugging

#### Software Requirements
- ✅ **Android Studio** (Ladybug/2024.x or later recommended)
- ✅ **Java/Kotlin** knowledge
- ✅ **Gradle** (AGP 9.2.0+, Gradle 9.4.1+)
- ✅ **Minimum SDK**: Android 14 (API 34)

#### Device Setup
1. Enable **Developer Options**: Settings → About phone → Software info → Tap "Build number" 7 times
2. Enable **USB Debugging**: Settings → Developer options → USB debugging
3. Connect phone via USB
4. Select **File Transfer** mode

---

### Step 1: Set Up the Project

#### Create New Android Project
1. Open **Android Studio**
2. Create new project: **File → New → New Project**
3. Select **Empty Activity** template
4. Set **Minimum SDK** to **Android 14 (API 34)**
5. Language: **Kotlin** (recommended) or Java

#### Add SDK Library
1. Create `libs` folder under `app/src/main/`
2. Download `GlyphMatrixSDK.aar` from [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
3. Copy to `app/src/main/libs/GlyphMatrixSDK.aar`

#### Configure Gradle
Edit `app/build.gradle`:
```gradle
android {
    compileSdk 35
    
    defaultConfig {
        minSdk 34
        targetSdk 35
    }
    
    repositories {
        flatDir {
            dirs 'src/main/libs'
        }
    }
}

dependencies {
    implementation fileTree(dir: 'libs', include: ['*.aar'])
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
}
```

4. **Sync Gradle**: File → Sync Project with Gradle Files

---

### Step 2: Configure AndroidManifest.xml

Add these configurations **inside the `<application>` tag**:

```xml
<!-- Register your Glyph Toy as a service -->
<service
    android:name=".YourGlyphToyService"
    android:exported="true"
    android:permission="com.nothing.ketchum.permission.ENABLE">
    <intent-filter>
        <action android:name="com.nothing.glyph.toy" />
    </intent-filter>
    <meta-data
        android:name="com.nothing.glyph.toy"
        android:resource="@xml/glyph_toy_config" />
</service>

<!-- Required permissions -->
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />
```

#### For Debugging (Optional)
```xml
<meta-data
    android:name="NothingKey"
    android:value="test" />
```
> **Warning**: Debug mode auto-disables after **48 hours**

#### For Always-On Display (Optional)
```xml
<meta-data
    android:name="com.nothing.glyph.toy.aod_support"
    android:value="1" />
```

---

### Step 3: Create a Glyph Toy Service (Kotlin)

```kotlin
package com.yourpackage

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.nothing.glyph.GlyphToyService
import com.nothing.glyph.GlyphButtonEvent
import com.nothing.glyph.GlyphMatrixFrame

class MyGlyphToyService : GlyphToyService() {
    
    override fun onBind(intent: Intent): IBinder {
        return super.onBind(intent)
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize your toy here
    }
    
    // Handle Glyph Button events
    override fun onGlyphButtonEvent(event: GlyphButtonEvent) {
        when (event) {
            GlyphButtonEvent.ACTION_DOWN -> {
                // Short press (down)
            }
            GlyphButtonEvent.ACTION_UP -> {
                // Short press (release)
            }
            GlyphButtonEvent.LONG_PRESS -> {
                // Long press
            }
            GlyphButtonEvent.CHANGE -> {
                // Long press (subsequent)
            }
        }
    }
    
    // Generate frames for display
    override fun onFrameRequest(): GlyphMatrixFrame {
        val matrixSize = if (isPhone3()) 625 else 169
        val frameData = IntArray(matrixSize) { index ->
            // Raw SDK 2.0 frame: 0 = off, 4095 = full white
            if (index % 2 == 0) 4095 else 0 // Checkerboard pattern
        }
        
        return GlyphMatrixFrame.Builder()
            .setMatrixData(frameData)
            .build()
    }
}
```

---

### Step 4: Build & Deploy

1. Click **Run (▶)** in Android Studio
2. Select your connected Nothing Phone
3. Go to **Settings → Glyph Interface → Glyph Toys**
4. Enable your toy
5. Press **Glyph Button** to test!

---

### Step 5: Debugging

#### Enable Debug Mode
```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```
> Debug mode auto-disables after **48 hours**

#### Check Logs
```bash
adb logcat | grep Glyph
```

---

## 🎪 Nothing Playground: Existing Glyph Toys

**Platform**: [https://playground.nothing.tech/toys](https://playground.nothing.tech/toys)

The **Nothing Playground** is the official community hub for discovering, sharing, and installing Glyph Toys.

---

### Most Popular Toys (by Downloads)

| Rank | Toy Name | Creator | Device | Downloads | Category |
|------|----------|---------|--------|-----------|----------|
| 1 | Music Visualizer - Dot Hub | PaulGsk | Phone (4a) Pro | **478** | Music |
| 2 | Glyph Call Status | Abhishek Lakhani | Phone (3) | **238** | Utility |
| 3 | Pomodoro Timer | Abhishek Lakhani | Phone (4a) Pro | **206** | Productivity |
| 4 | Wattage Indicator | Abhishek Lakhani | Phone (3) | **197** | Utility |
| 5 | Leveller | Rahul | Phone (3) | **190** | Utility |
| 6 | Glyph Invaders | RR226 | Phone (3) | **94** | Game |
| 7 | GlyphEyes | yuki | Phone (3) | - | Entertainment |
| 8 | Glyph Museum | pauwma | Phone (3) | - | Creative |
| 9 | Glyph Decibel | Otto | Phone (3) | - | Music |
| 10 | 180 BPM Metronome | Xeelee | Phone (3) | - | Music |

---

## 🔍 Detailed Toy Analysis

### 1. Music Visualizer - Dot Hub (PaulGsk)
- **Description**: Dynamic music-reactive animations synced to audio beats
- **Downloads**: 478 (most popular)
- **Technical**: Uses AudioRecord API, FFT for frequency analysis, optimized for 13×13 matrix
- **Compatibility**: Phone (4a) Pro, OS 4.0+

### 2. Glyph Call Status (Abhishek Lakhani)
- **Description**: Visual call notifications (incoming, outgoing, missed)
- **Downloads**: 238
- **Features**: Pulsing animations, caller ID display, color-coded states

### 3. Pomodoro Timer (Abhishek Lakhani)
- **Description**: Productivity timer with visual countdown
- **Downloads**: 206
- **Features**: 25-min work / 5-min break cycles, AOD support, vibration alerts

### 4. Wattage Indicator (Abhishek Lakhani)
- **Description**: Real-time charging/discharging wattage
- **Downloads**: 197
- **Features**: Voltage × current calculation, color-coded for speed

### 5. Glyph Museum (pauwma)
- **Description**: Community gallery for sharing glyph art
- **Features**: Browse posts, live preview, create art, customizable frames, achievements
- **Compatibility**: Phone (3), OS 4.0+

---

## 👥 Community Resources

### GitHub Repositories
- [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) - Official SDK
- [GlyphMatrix-Example-Project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project) - Demo project
- [GlyphMarquee](https://github.com/bluehomewu/GlyphMarquee) - Scrolling text
- [pikachu-glyph](https://github.com/nadigchinnu/pikachu-glyph) - Pikachu animation
- [toyph](https://github.com/antonvidishchev/toyph) - Customizable framework
- [GlyphBeat](https://github.com/pauwma/GlyphBeat) - Media player toy

---

## 📊 Technical Specifications

### Matrix Specifications
| Device | Matrix Size | Total LEDs | Min Frame Interval |
|--------|-------------|------------|---------------------|
| Phone (3) | 25×25 | 625 | ≥30ms |
| Phone (4a) Pro | 13×13 | 169 | ≥30ms |

### Glyph Button Events
| Event | Trigger | Use Case |
|-------|---------|----------|
| `ACTION_DOWN` | Short press (down) | Start action |
| `ACTION_UP` | Short press (release) | End action |
| `LONG_PRESS` | Long press (~1s) | Toggle/activate |
| `CHANGE` | Long press (subsequent) | Cycle states |

### Performance Constraints
- Frame Rate: Minimum 30ms between frames (≈33 FPS max)
- Object brightness multiplier: 0–255. Raw matrix frames: 0–4095; see [brightness contract](docs/glyph-brightness.md).
- Memory: Limited resources for toys
- Background: Only foreground apps can use Glyph SDK
- Emulator: **Not supported** - Requires physical device

---

## 💡 Inspiration & Ideas for New Toys

### High-Potential Categories
- **Fitness**: Step counter, workout timer, heart rate
- **Weather**: Temperature, precipitation, UV index
- **Smart Home**: IoT device status, security alerts
- **Finance**: Stock ticker, crypto prices
- **Games**: Snake, Pong, Tetris, Memory
- **Utilities**: Battery monitor, storage monitor
- **Creative**: Pixel art editor, animation creator
- **Accessibility**: Flash alerts, vibration patterns

### Innovative Concepts
- Glyph Piano (musical notes)
- World Clock (multiple time zones)
- Countdown Timer (events, launches)
- Morse Code Trainer
- AI-generated art
- Multi-device sync

---

---

## 🎯 Part 2: Android Background Limitations

---

## 🚨 Critical Android Background Limitations (2024-2026)

> **These restrictions will completely block your app from performing certain actions in the background**

### The Golden Rule:
> **"If it involves user privacy (camera, microphone, location, sensors) or system resources (audio, continuous processing), you CANNOT do it from a pure background context. You MUST have a visible activity or foreground service that was started while the app was visible to the user."**

---

## 🎵 Audio & Microphone Restrictions

### ❌ YOU CANNOT:
- **Play audio** from a background service started from background (Android 11+)
- **Access microphone** from a background service started from background (Android 11+)
- **Create a `microphone` or `mediaPlayback` foreground service** from background (Android 14+)

### ✅ YOU CAN:
- Play audio from a foreground service **started while app is in foreground**
- Access microphone from a foreground service **started while app is visible**

### 📌 Key Rule from Android Docs:
> *"If your app starts a foreground service while running in the background, the foreground service **cannot access the microphone or camera**"*

### 🔧 Workaround:
```xml
<service
    android:name=".AudioService"
    android:foregroundServiceType="microphone|mediaPlayback" />
```

```kotlin
// Must start from visible Activity
startForegroundService(Intent(this, AudioService::class.java))
```

**Required Permissions:**
- `android.permission.RECORD_AUDIO`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MICROPHONE` (Android 14+)

---

## 📷 Camera Restrictions

### ❌ YOU CANNOT:
- **Access camera** from a background service started from background (Android 11+)
- **Create a `camera` foreground service** from background (Android 14+)
- **Use camera without user-visible UI** (Android 9+)

### ✅ YOU CAN:
- Access camera from foreground service **started while app is visible**

### 📌 Key Rule:
> *"If your app starts a foreground service while running in the background, the foreground service **cannot access the camera**"*

### 🔧 Workaround:
```xml
<service
    android:name=".CameraService"
    android:foregroundServiceType="camera" />
```

**Required Permissions:**
- `android.permission.CAMERA`
- `android.permission.FOREGROUND_SERVICE_CAMERA` (Android 14+)

---

## 📍 Location Restrictions

### ❌ YOU CANNOT:
- **Access location** from background service **without `ACCESS_BACKGROUND_LOCATION`** (Android 10+)
- **Create a `location` foreground service** from background (Android 14+)

### ✅ YOU CAN:
- Access location while app is in foreground (with `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`)
- Access location from background **with `ACCESS_BACKGROUND_LOCATION`** permission

### 📌 Key Rule:
> *"The service **cannot access location unless your app has background location access**"*

### 🔧 Workaround:
```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

```xml
<service
    android:name=".LocationService"
    android:foregroundServiceType="location" />
```

**Note:** On Android 10+, users must **explicitly grant** background location in settings.

---

## 🔄 Sensor Restrictions (Accelerometer, Gyroscope, etc.)

### ❌ YOU CANNOT:
- **Access continuous reporting mode sensors** (accelerometer, gyroscope) from background (Android 9+)
- **Get sensor events** when app is in background
- **Use high sampling rate sensors** without `HIGH_SAMPLING_RATE_SENSORS` permission (Android 12+)

### ✅ YOU CAN:
- Access sensors from **foreground service** started while app is visible
- Use sensors when app is in foreground

### 📌 Key Rule from Android Docs:
> *"On devices running Android 9 (API level 28) or higher, apps running in the background have the following restrictions: **Sensors that use the continuous reporting mode, such as accelerometers and gyroscopes, don't receive events**"*

### 🔧 Workaround:
```xml
<!-- For Android 12+ high sampling rate -->
<uses-permission android:name="android.permission.HIGH_SAMPLING_RATE_SENSORS" />
```

```kotlin
class SensorService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, notification)
        // Now you can register sensor listeners
    }
}
```

**Required for Motion Sensors (Android 10+):**
- `ACTIVITY_RECOGNITION` permission

---

## 💓 Body Sensors / Health Data Restrictions

### ❌ YOU CANNOT:
- **Access body sensors** (heart rate, step counter) from background without special permission (Android 11+)
- **Create health service** from background (Android 14+)

### ✅ YOU CAN:
- Access body sensors from **foreground service**
- Use with `BODY_SENSORS_BACKGROUND` permission (API 33-35) or `READ_HEALTH_DATA_IN_BACKGROUND` (API 36+)

### 📌 Key Rule:
> *"you cannot create a health foreground service that uses body sensors while your app is in the background, **unless you've been granted the BODY_SENSORS_BACKGROUND or READ_HEALTH_DATA_IN_BACKGROUND permissions**"*

### 🔧 Workaround:
```xml
<uses-permission android:name="android.permission.BODY_SENSORS_BACKGROUND" />
<!-- OR for API 36+ -->
<uses-permission android:name="android.permission.READ_HEALTH_DATA_IN_BACKGROUND" />
```

```xml
<service
    android:name=".HealthService"
    android:foregroundServiceType="health" />
```

---

## ⚙️ Foreground Service Restrictions (Android 14+)

### ❌ YOU CANNOT:
- **Start ANY foreground service** from background (Android 14+)
- **Start foreground service** from `BOOT_COMPLETED` receiver
- **Start foreground service** from WorkManager/AlarmManager in background

### ✅ YOU CAN:
- Start foreground service from **visible activity**
- Start foreground service from **user interaction**

### 📌 Key Rule:
> *"If your app targets Android 14 (API level 34) or higher, **it must specify at least one foreground service type** for each foreground service"*

### 🔧 Exceptions (Android 15+):
- User disabled battery optimization for your app
- App has `SYSTEM_ALERT_WINDOW` permission **AND** visible overlay
- App is device owner/profile owner

---

## ⚡ General Background Execution Limits

### ❌ YOU CANNOT:
| Restriction | Version | Details |
|-------------|---------|---------|
| Run code continuously in background | Android 8+ | Apps are throttled |
| Use infinite loops in background | Android 8+ | Will be killed |
| Access network freely in background | Android 6+ | Restricted by Doze/App Standby |
| Use AlarmManager for exact timing | Android 6+ | Throttled in Doze |
| Start activities from background | Android 8+ | Blocked |

### ✅ YOU CAN:
- Use **WorkManager** for deferrable background tasks
- Use **Foreground Service** for user-visible long-running tasks
- Use **JobScheduler** for scheduled tasks

---

## 📋 Summary: What You Can & Cannot Do

| **Feature** | **Android Version** | **Restriction** | **Required Workaround** |
|------------|-------------------|-----------------|------------------------|
| **Microphone** | 9+ (strict 11+) | No access from background-started service | Foreground service from visible activity |
| **Camera** | 9+ (strict 11+) | No access from background-started service | Foreground service from visible activity |
| **Location** | 10+ | Need `ACCESS_BACKGROUND_LOCATION` | Request background location permission |
| **Sensors (accel/gyro)** | 9+ | No continuous mode events | Foreground service |
| **Body Sensors** | 11+ | Need special background permission | `BODY_SENSORS_BACKGROUND` or `READ_HEALTH_DATA_IN_BACKGROUND` |
| **Foreground Service Start** | 14+ | Cannot start from background | Start from visible activity |
| **Audio Playback** | 17+ | Audio APIs blocked from background | Foreground service with `mediaPlayback` type |
| **High Sampling Rate Sensors** | 12+ | Need special permission | Declare `HIGH_SAMPLING_RATE_SENSORS` |
| **BOOT_COMPLETED** | 14+ | Cannot start foreground services | User must manually start |

---

---

## 🎯 Part 3: Practical Development

---

## 🏆 Best Practices for Glyph Toy Development

### 1. Always Use Foreground Service
```kotlin
class MyGlyphToyService : GlyphToyService() {
    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            createNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }
}
```

### 2. Declare Proper Service Type
```xml
<service
    android:name=".MyGlyphToyService"
    android:foregroundServiceType="dataSync"
    android:permission="com.nothing.ketchum.permission.ENABLE"
    android:exported="true">
    <intent-filter>
        <action android:name="com.nothing.glyph.toy" />
    </intent-filter>
    <meta-data
        android:name="com.nothing.glyph.toy.aod_support"
        android:value="1" />
</service>
```

### 3. Start from Visible Activity
```kotlin
// In your Activity
fun startGlyphService() {
    val intent = Intent(this, MyGlyphToyService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}
```

### 4. Request Battery Optimization Exemption
```kotlin
fun requestBatteryOptimizationExemption(context: Context) {
    val intent = Intent()
    val packageName = context.packageName
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(packageName)) {
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:$packageName")
        context.startActivity(intent)
    }
}
```

### 5. Handle Service Lifecycle
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY // Service will be restarted if killed
}
```

---

## ⚠️ Common Pitfalls & Solutions

| **Problem** | **Cause** | **Solution** |
|-------------|-----------|--------------|
| Service killed after 3 minutes | Using `shortService` type | Use proper service type (dataSync, mediaPlayback, etc.) |
| Glyph not updating in AOD | Not declaring AOD support | Add `aod_support` metadata |
| Service killed when app swiped | Default behavior | Use `START_STICKY` and persistent notification |
| Glyph disabled in pocket | Pocket mode | Cannot override - design around it |
| Cannot start on boot | Android 14+ restriction | User must manually start |
| Background audio not working | SDK restriction | Start foreground service from visible activity |
| Sensor access denied | Background restriction | Use foreground service |

---

## 🚀 Next Steps

### Option 1: Analyze a Specific Toy
Recommended: GlyphMarquee, pikachu-glyph, toyph, GlyphBeat

### Option 2: Brainstorm Your Idea
Tell me your concept and I'll validate feasibility, suggest architecture, and provide code structure

### Option 3: Start Development
I can help set up Android Studio project, configure SDK, write boilerplate code

### Option 4: Clone & Modify
Pick an existing toy and I'll guide you through customization

---

## 🔗 Additional Resources

### Official Android Documentation
- [Foreground Service Restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Foreground Service Types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Sensor Restrictions](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview)
- [Background Audio Hardening (Android 17)](https://developer.android.com/about/versions/17/changes/bg-audio)
- [Behavior Changes Android 14](https://developer.android.com/about/versions/14/behavior-changes-14)

### Nothing-Specific Documentation
- [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
- [Nothing OS 4.0 Release Notes](https://nothing.community/en/d/38769-the-great-glyph-matrix-toy-index)
- [NothingTec: Glyph Matrix Guide](https://nothingtec.com/en/2025/07/02/glyph-matrix-developer-kit/)

### Community
- [Nothing Community](https://nothing.community)
- [Nothing Playground](https://playground.nothing.tech)
- [r/NothingTech on Reddit](https://www.reddit.com/r/NothingTech/)

---

## 🎉 Conclusion

You now have **complete knowledge** to build Glyph Toys for Nothing Phone devices:

✅ **Understand** what Glyph Toys are and how they work
✅ **Access** official SDKs and documentation
✅ **Follow** step-by-step development guide
✅ **Explore** existing toys on Nothing Playground
✅ **Know** all critical Android background limitations
✅ **Apply** best practices for foreground services
✅ **Avoid** common pitfalls

**The Glyph Matrix is your canvas. What will you create?** 🎨

---

*This comprehensive guide was compiled by Vibe for Valentin Bell on August 28, 2026.*
*For the latest information, always check the official Nothing Developer Program repositories and Android Developer Documentation.*