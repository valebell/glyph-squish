# Physical Glyph Matrix brightness

## Two different numeric ranges

Squish's engine and on-screen preview use **8-bit grayscale: 0–255**. Nothing's
`GlyphMatrixObject.Builder.setBrightness(...)` also accepts **0–255**, but that
argument is an object brightness multiplier, not the encoding of a raw frame.

The bundled **Glyph Matrix SDK 2.0** converts bitmap pixels to **12-bit raw matrix
intensity: 0–4095**. For grayscale artwork at object brightness 255, its conversion is:

```text
rawIntensity = gray8 * 4095 / 255    // integer division
```

`GlyphMatrixManager.setMatrixFrame(int[])` forwards the array directly to the
Nothing service's `setMatrixColors(int[])`; it does not perform this expansion.
Passing 255 directly therefore requests about **6.2% of the raw intensity range**,
not full white. This numeric ratio is not a measurement of perceived brightness.

## Squish's rendering boundary

```text
SlimeEngine.renderFrame()          0–255 grayscale; off pixels remain 0
    ↓
GlyphFrameEncoder.encode(...)      expands to 0–4095 once, then applies idle scale
    ↓
GlyphMatrixManager.setMatrixFrame(int[])
```

- Keep engine frames, preview colors, and animation calculations in 0–255.
- Convert only at the raw hardware send boundary in `SlimeToyService`.
- Do not encode `GlyphMatrixFrame.render()` output again: SDK-rendered frames
  are already in the raw range.
- Do not pass packed Android ARGB colors to the raw-array overload.
- The physical service uses `shadeBody = false`: its solid body is 255 in the
  engine and 3481 on the wire during active play (85% of raw intensity). Trails, particles, and morph fades keep their
  relative intensities through the same conversion.
- Nothing OS's global/automatic brightness still controls actual LED output.
  Reaching 4095 verifies the app's requested intensity, not a photometric reading.

## Idle dimming

`GlyphIdleBrightness` is a session-local envelope driven by the monotonic uptime
clock and the physical sensor/button inputs:

- Five seconds without input at 85% active brightness.
- A 1.5-second smooth fade to 25% of raw intensity (full-white pixels become 1024).
- A 150ms smooth return to 85% active brightness on motion, shake, or button input.
- A held button, active special move, or form transition postpones dimming.
  Changing the selected form or move also wakes the Glyph.
- A fresh/reconnected Glyph session resets to 85% active brightness.

The motion sensor supplies acceleration normalized to Earth's gravity. Activity
requires a gravity-vector change of at least 0.18 (roughly ten degrees) within a
rolling one-second window, or linear acceleration of at least 0.10g sustained for
120ms. Gaps over 100ms restart acceleration evidence. The same motion gate controls
both waking and postponing idle, so small posture changes, isolated spikes, and
slow drift neither brighten the LEDs nor keep them awake. Entering dimming clears
the earlier tilt history and acceleration evidence. These thresholds affect only
brightness; the creature's movement and gesture recognition keep their sensor input.
Buttons and recognized shakes still wake immediately. Simulated creature velocity
does not count as physical input. Thresholds are initial tuning values, not a
physical-device calibration.

The encoder applies the envelope **after** 8-to-12-bit conversion, preserving
low-intensity animation detail and black/off pixels. Its default scale is 1,
which must still match the SDK exactly. Do not dim the engine or preview frames,
persist the idle state, or change the user's system brightness settings.

`GlyphIdleBrightnessTest` checks the idle delay, fade and wake timing, minor posture
changes, slow drift, transient spikes, pre-idle history, deliberate motion across
sampling rates, held interactions, frame gaps, and session reset.
`GlyphFrameEncoderTest` checks the scaled raw values.

## Evidence and regression checks

Verified on 2026-09-20 by inspecting the bundled SDK bytecode:

- `GlyphMatrixUtils.convertToGlyphMatrix(Bitmap, ...)` averages RGB, multiplies
  by 4095, divides by 255, applies the object's 0–255 brightness multiplier, and
  clamps the result to 0–4095.
- `GlyphMatrixManager.setMatrixFrame(int[])` passes the input straight to the
  service without scaling.

The public [Nothing GDK documentation](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
describes the object's 0–255 brightness parameter. It must not be interpreted as
the range of the raw-array overload.

`GlyphFrameEncodingTest` runs on Android against the actual bundled SDK. It
compares our encoded array with an equivalent grayscale bitmap rendered through
`GlyphMatrixObject` and `GlyphMatrixFrame`, for all 256 gray levels and all five
creature forms. Keep this test when replacing the SDK: a changed SDK encoding
must be investigated before changing the constants.

`GlyphFrameEncoderTest` also checks black, full white, intermediate values,
monotonic fades, bounds, and that the engine's array is not mutated.

The original regression came from confusing the object multiplier with the raw
frame encoding. Making the body uniformly 255 removed shading but did not fix
its hardware range. The explicit conversion at the send boundary fixes that.
