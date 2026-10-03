# Glyph Matrix Android Development Guide

> Practical integration notes for building companion-app previews and Glyph Toys
> with Nothing's Glyph Matrix Developer Kit.
>
> Last reviewed: 2026-08-29

## Purpose

The official Developer Kit documents the public API. This guide records the
engineering details that are easy to miss when turning those APIs into a
reliable Android application.

Always check the current official documentation, sample project, SDK binary,
supported devices, firmware requirements, and license before starting a new
project. These can change independently.

## Know which experience you are building

There are two distinct ways to draw on the Glyph Matrix:

| Experience | Owner | Frame API | Lifetime |
|---|---|---|---|
| Glyph Toy | Nothing OS Toy carousel | `setMatrixFrame(...)` | While the system binds the Toy service |
| Companion-app preview | Foreground application | `setAppMatrixFrame(...)` | While the preview UI is visible |

Do not use `setMatrixFrame(...)` from an Activity. A foreground application
must use `setAppMatrixFrame(...)` so Nothing OS can preserve Toy priority.

The Toy carousel has higher priority than direct app rendering. An application
preview may therefore be interrupted when the user operates the Glyph Button.
Treat this as normal platform behavior rather than an error.

## Device capability matrix

| Device | SDK identifier | Matrix | Glyph Touch | Supported Toy behavior |
|---|---|---:|---|---|
| Phone (3) | `Glyph.DEVICE_23112` | 25 x 25 | Yes | Active animation, touch, and AOD |
| Phone (4a) Pro | `Glyph.DEVICE_25111p` | 13 x 13 | No | AOD only |

Use `Common.getDeviceMatrixLength()` instead of assuming a 25 x 25 surface when
supporting more than one model. Keep active-touch interactions optional because
not every Matrix device supports them.

Direct application rendering requires Nothing OS system version `20250801` or
newer. The documented intent for opening Manage Glyph Toys requires system
version `20250829` or newer.

## SDK acquisition and project setup

1. Obtain the current AAR from the official Developer Kit repository.
2. Place it in the application module's `libs/` directory.
3. Add it as a local file dependency.
4. Match the official example project's supported Android configuration unless
   a newer SDK release says otherwise.
5. Recheck the SDK license before publishing or sharing the project.

```kotlin
dependencies {
    implementation(files("libs/glyph-matrix-sdk.aar"))
}
```

The current official example uses Android API 34 as its minimum SDK. Treat that
as an integration baseline, not a permanent platform guarantee.

The official sample manifest includes a `NothingKey` application metadata
entry with a development value. Confirm key requirements with Nothing before a
production release; do not assume the sample value is a production credential.

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE" />

<application>
    <meta-data
        android:name="NothingKey"
        android:value="test" />
</application>
```

No Internet permission is required for local Matrix rendering.

## Glyph Toy manifest contract

Nothing OS discovers a Toy through an exported Android service and the exact
`com.nothing.glyph.TOY` action:

```xml
<service
    android:name=".glyph.ExampleToyService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.nothing.glyph.TOY" />
    </intent-filter>

    <meta-data
        android:name="com.nothing.glyph.toy.name"
        android:resource="@string/toy_name" />
    <meta-data
        android:name="com.nothing.glyph.toy.image"
        android:resource="@drawable/toy_preview" />
    <meta-data
        android:name="com.nothing.glyph.toy.summary"
        android:resource="@string/toy_summary" />
    <meta-data
        android:name="com.nothing.glyph.toy.longpress"
        android:value="1" />
    <meta-data
        android:name="com.nothing.glyph.toy.aod_support"
        android:value="1" />
</service>
```

Important details:

- Name and image metadata are required for discovery and presentation.
- Summary, introduction Activity, long press, and AOD support are optional.
- Declare long-press support only when the service handles the event.
- Declare AOD support only when the service can render a useful low-frequency
  state.
- Follow the official per-device preview-image specification. A normal app icon
  is not automatically a good Toy preview.
- Reinstall the application after changing service metadata. Then verify the
  entry in Manage Glyph Toys.

## Connection lifecycle

`GlyphMatrixManager` is obtained asynchronously:

1. Get the manager instance with the application context.
2. Call `init(callback)`.
3. Wait for `onServiceConnected`.
4. Call `register(...)` for the intended device.
5. Send frames only after registration succeeds.
6. Stop frame production before disconnection.
7. Call the matching cleanup method exactly once.

For a Toy service, initialize in `onBind()` and clean up in `onUnbind()`. For a
companion preview, connect only while the preview is requested and the Activity
is in the foreground.

### One owner per process

Treat `GlyphMatrixManager` as a process-wide hardware connection. Do not let an
Activity and a Toy service independently call `init()` and `unInit()` on the
same manager.

Use one centralized coordinator that:

- Tracks the current connection owner.
- Gives Toy rendering priority over app preview.
- Revokes or pauses app preview before a Toy acquires the manager.
- Balances every `init()` with one `unInit()`.
- Resumes preview only if its screen is still active after the Toy releases the
  connection.
- Serializes manager access.

This avoids callback replacement, duplicate binding, unbalanced unbinding, and
one component disconnecting another component's live session.

### Cleanup rules

For application previews:

1. Stop the frame loop.
2. Call `closeAppMatrix()`.
3. Call `unInit()`.

For Toy services:

1. Stop the frame loop and sensor collection.
2. Turn off the Toy frame if appropriate.
3. Call `unInit()`.
4. Release callbacks, scopes, and large frame caches.

Never leave app-matrix content active after the preview Activity enters the
background.

## Error handling

Manager calls can fail when the backing Nothing service disconnects or is not
ready. Kotlin does not force handling of Java checked exceptions, so explicitly
handle `GlyphException` around:

- `register(...)`
- `setMatrixFrame(...)`
- `setAppMatrixFrame(...)`
- `closeAppMatrix()`
- Other SDK calls that declare it

An SDK failure should stop or downgrade the current hardware session. Do not
keep a tight frame loop repeatedly calling a disconnected service.

Keep the software preview functional when hardware is unavailable. Hardware
support should enhance the editor, not prevent the companion app from opening.

## Threading and frame delivery

Use a single serialized execution context for mutable playback state and SDK
calls.

Recommended model:

- Render or precompute frames on a background dispatcher.
- Send frames through one coroutine or channel.
- Perform manager calls from the main dispatcher, matching the official sample.
- Route button events into the same playback state machine.
- Cancel the frame job before releasing the manager.

Avoid reading playback state on one thread while Glyph events mutate it on
another. A `Channel`, actor-style loop, or strict dispatcher confinement is
preferable to several independent mutable fields.

Do not perform image analysis, network requests, database migrations, or heavy
allocation in a Toy frame callback.

## Glyph Button event handling

A Toy receives interaction events through a `Messenger`. Return its binder from
`onBind()` and handle only messages where:

```kotlin
message.what == GlyphToy.MSG_GLYPH_TOY
```

Read the event with the SDK constant:

```kotlin
val event = message.data?.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
```

Handle the public event constants:

| Event | Meaning |
|---|---|
| `GlyphToy.EVENT_CHANGE` | Long press |
| `GlyphToy.EVENT_ACTION_DOWN` | Touch/hold begins |
| `GlyphToy.EVENT_ACTION_UP` | Touch/hold ends |
| `GlyphToy.EVENT_AOD` | AOD refresh |

Short press is system-owned and cycles between enabled Toys. Do not design a
Toy that requires receiving short press as an in-experience action.

Every Toy should remain understandable without optional input. Touch-down may
not be followed immediately by touch-up if the service loses focus, so reset
held state during unbind.

## AOD behavior

AOD is not a normal animation mode. Nothing OS documents one AOD event per
minute for the selected AOD Toy.

Design AOD output as:

- A static final composition.
- A clock-like state updated once per minute.
- A deliberately slow generative state.

Do not depend on 8-12 FPS animation, continuous touch, or sensors in AOD mode.
Phone (4a) Pro supports AOD Toys but not Glyph Touch.

## Frame representation

Choose one canonical, device-independent representation inside the application,
then adapt it at the SDK boundary.

For a raw square buffer:

```kotlin
val index = y * matrixLength + x
```

Required invariants:

- Array length is `matrixLength * matrixLength`.
- Coordinates are bounds-checked.
- Frames are immutable once queued for display.
- Brightness or color values follow the SDK version's documented encoding.
- Rotation and mirroring are deliberate and verified on the physical rear
  display.

For monochrome artwork, keep engine and preview values in `0..255`, but convert
raw frames to `0..4095` before calling `setMatrixFrame(int[])` with the bundled
SDK 2.0. The conversion at full object brightness is `gray8 * 4095 / 255` using
integer division. `GlyphMatrixObject.setBrightness(255)` is an object multiplier;
it does not mean raw intensity 255 is full white. SDK-rendered bitmap frames are
already expanded and must not be converted twice.

See [Physical Glyph Matrix brightness](docs/glyph-brightness.md) for the verified
SDK behavior and regression tests. Confirm perceived intensity on hardware because
the phone screen does not reproduce rear Matrix brightness accurately.

`GlyphMatrixFrame.Builder` supports at most three `GlyphMatrixObject` layers:
low, middle, and top. Flatten complex scenes into a raw frame or bitmap before
delivery rather than trying to exceed the layer limit.

## Animation performance

The display loop should be boring and deterministic:

- Pre-render authored animation where possible.
- Reuse `IntArray` buffers carefully or queue immutable copies.
- Keep one frame clock.
- Use meaningful key poses rather than excessive frame rate.
- Avoid rapid full-matrix flashing.
- Pause on important states so content remains readable.
- Cache only bounded data and clear it on service shutdown.

Measure timing on hardware. Emulator and Compose preview performance do not
represent the Nothing system service or physical LEDs.

## Companion-app preview

A useful companion app should provide a software Matrix preview even on
unsupported devices.

For physical preview:

1. Detect that compatible hardware and firmware are available.
2. Connect after the user explicitly enables preview.
3. Wait for registration before displaying a "ready" state.
4. Use `setAppMatrixFrame(...)`, never the Toy method.
5. Expect the system Toy carousel to interrupt the preview.
6. Call `closeAppMatrix()` when preview is disabled or the app leaves the
   foreground.
7. Surface unavailable, connecting, ready, and interrupted states separately.

Do not label a preview as live merely because the device manufacturer matches;
only mark it ready after the SDK callback and registration complete.

## Opening Manage Glyph Toys

On supported Nothing OS versions, launch:

```kotlin
val intent = Intent().setComponent(
    ComponentName(
        "com.nothing.thirdparty",
        "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
    ),
)
```

Call `resolveActivity(...)` first and provide an explanatory fallback when the
component is unavailable. Opening the manager does not activate the Toy; the
user still adds it to the active queue.

After installation, verify all of the following:

- The Toy appears in the manager.
- Its name, summary, and image are correct.
- It can be added and removed.
- Selecting it binds the service.
- Moving to another Toy unbinds it.

## Testing strategy

### Host-side tests

Keep rendering logic independent from Android and test:

- Every frame has the correct dimensions.
- Every value is within the supported range.
- No authored frame is unexpectedly blank.
- Drawing primitives clip safely at every edge.
- Mirroring and rotation preserve dimensions.
- Input events produce deterministic state transitions.
- AOD frames are valid without an active animation loop.

### Device tests

At minimum, test on every supported hardware family:

1. Fresh install and Toy discovery.
2. Add/remove in Manage Glyph Toys.
3. Repeated select/unselect lifecycle.
4. App preview start, background, foreground, and stop.
5. App preview interrupted by the Toy carousel.
6. Long press, hold, release, and lost-focus reset.
7. AOD selection and minute update.
8. Screen-off, screen-on, rotation, process death, and app update.
9. Brightness, orientation, readability, and animation timing.
10. Rapid switching without crashes or stale Matrix content.

Use `adb logcat` while testing, filtering for the application tag,
`AndroidRuntime`, and connection errors.

## Common failure modes

| Symptom | Likely cause |
|---|---|
| Toy is absent from the manager | Wrong service action, missing name/image metadata, stale install, or unsupported device |
| Service binds but no frame appears | Registration not completed, wrong device identifier, invalid frame length, or another owner has priority |
| Preview works once, then disconnects | Unbalanced `init()`/`unInit()` or multiple components managing the same connection |
| Toy stops when the app backgrounds | Activity cleanup disconnected a manager shared with the Toy |
| App preview remains after leaving | Missing `closeAppMatrix()` during foreground teardown |
| Long press does nothing | Manifest flag absent, wrong Messenger binder, or event constant not handled |
| Hold remains active | Release/lost-focus state was not reset on unbind |
| AOD animation is frozen | AOD updates are minute-based, not a continuous frame callback |
| Hardware output is mirrored or rotated | Rear-display orientation was assumed from the front-screen preview |
| Frame loop crashes after switching Toys | SDK calls continued after disconnection or `GlyphException` was not handled |

## Privacy and security

- Request only permissions required by the experience.
- Do not log frame data derived from private user content.
- Keep generated content in app-private storage.
- Exclude sensitive generated data from backup when the product promises local
  retention.
- Stop sensors, jobs, and hardware output when the Toy is not selected.
- Treat exported Toy services as externally managed entry points: validate any
  custom intents and do not trust arbitrary extras.

## Licensing and distribution

The Glyph SDK is closed-source software licensed by Nothing.

As of this review:

- Commercial use requires Nothing's prior written permission.
- The SDK may not be reverse engineered or modified.
- Redistribution and transfer are restricted.
- The SDK license may change between releases.

Do not copy the AAR into a public template, artifact repository, or distributable
starter project unless the current license and Nothing explicitly permit it.
Prefer documented acquisition from the official repository during project
setup.

Commercial licensing contact listed by Nothing:
`GDKsupport@nothing.tech`.

## New-project checklist

- [ ] Confirm supported devices and required Nothing OS versions.
- [ ] Read the current SDK license.
- [ ] Acquire the AAR from the official source.
- [ ] Add the required permission and application metadata.
- [ ] Register the exported Toy service with exact metadata names.
- [ ] Use dynamic matrix dimensions where applicable.
- [ ] Centralize process-wide manager ownership.
- [ ] Register only after the service-connected callback.
- [ ] Serialize frame delivery and interaction state.
- [ ] Handle `GlyphException`.
- [ ] Separate Toy and app-preview frame APIs.
- [ ] Close app preview on background.
- [ ] Implement complete bind/unbind cleanup.
- [ ] Provide a useful minute-scale AOD state.
- [ ] Add a Manage Glyph Toys entry point with fallback.
- [ ] Test discovery, priority changes, input, AOD, and lifecycle on hardware.
- [ ] Recheck commercial and redistribution approval before release.

## Official references

- [Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
- [Developer Kit README](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/README.md)
- [Glyph SDK license](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/LICENSE.md)
- [Official example project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project)
- [Nothing support: manage Glyph Toys](https://support.nothing.tech/hc/en-us/articles/36457460917905)
