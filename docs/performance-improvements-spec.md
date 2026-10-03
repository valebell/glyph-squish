# Squish performance and size specification

This specification records the challenged review and the implementation contract for
reducing unnecessary startup work, device communication, sensor activity, database
writes, and packaged audio size. Existing gesture changes in the working tree are
outside this change and must be preserved.

## Baseline and evidence

The October 3 release APK is 3,715,862 bytes. Its 23 FLAC effects occupy 1,182,092
bytes. All 199 JVM tests pass. Release optimization and resource shrinking are
already enabled.

Desktop JVM probes found eager radial geometry initialization on the first render
and 1,704 identical consecutive encoded frames in a stationary 1,800-frame sequence.
These observations identify avoidable work; they do not establish Android startup
latency, typical gameplay savings, or battery improvement.

## Decisions after skeptical review

| Proposal | Decision and reason |
| --- | --- |
| Initialize radial geometry on demand | Accept; retain exact sampling and cached lookups. |
| Suppress identical encoded Glyph frames | Accept with a one-second retry bound; the SDK swallows some IPC failures. |
| Stop preview sensors outside active playback | Accept for loading, failure, pause and disposal. Preserve shakes while browsing a scrolled gallery. |
| Avoid unchanged unlock table replacement | Accept inside the existing transaction, using persisted membership rather than a process cache. |
| Reduce audio sample rate or remove variations | Reject for this change; these alter established audio fidelity or variety. |
| Improve lossless FLAC compression | Defer; settings constrained to the streamable subset saved less than 2 KB across all assets. |
| Replace Room with DataStore | Defer; no measured size benefit justifies a storage redesign here. |
| Pool frame arrays or reduce physics sampling | Defer; modest allocation evidence does not justify ownership or recognition risks. |

## Geometry initialization

Store a thread-safe lazy radial table for each form. Accessing masks and eye layout
must not generate radial samples. The first radial lookup generates only that form's
table, and subsequent lookups reuse it. Keep all angles, radius steps, rounding,
silhouettes, shading and particle positions unchanged. This defers computation;
it does not remove the first lookup cost for shaded forms.

Acceptance: existing geometry, rendering and transition tests pass; compare cold
host probes and deterministic rendered sequences before and after without using
timing thresholds as correctness tests.

## Glyph frame delivery

Keep the simulation, input, rendering and brightness cadence. Compare each final
encoded frame with the last submission and retry duplicates on the first frame
tick at least one second later. Changed pixels and brightness must be submitted
immediately. Clear delivery state when rendering stops and before registration.
Propagated exceptions must not advance the comparison frame or timestamp. Retained
comparison data must not alias mutable caller arrays.

Nothing's [official Toy service example](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/README.md)
submits a static image once after connecting and clears it when unbinding. This
supports persistent frames, although it does not explicitly specify a refresh
timeout. The independent implementation review found that SDK 2.0 catches and logs
`RemoteException` without notifying the caller. A normal return therefore cannot
acknowledge delivery. Retry unchanged output approximately once per second to recover from silent
drops or a late asynchronous clear; this is a reliability policy, not a documented
display refresh requirement. Do not claim physical display validation from an SDK
encoding test.

Acceptance: tests cover duplicate output, changed pixels, changed encoded brightness,
reset, propagated failure, silent drops, retry deadlines and caller mutation. The
engine output sequence and gesture cadence remain unchanged. Record whether actual Phone (3) display validation was
possible separately from ordinary Android instrumentation.

## Preview motion lifecycle

Register preview sensors only when creature state is ready and the activity is
resumed. Stop sensors and cancel transient interaction on pause, loss of readiness
or disposal. Composition while already resumed must start correctly, and resume
must not register twice. Retain gallery-wide shake selection while the home screen
is ready and resumed, including when the preview is scrolled away.

Acceptance: exercise readiness and lifecycle transitions, including repeated resume,
pause, disposal and recovery from loading. Existing preview, gallery, playback,
accessibility and settings instrumentation must remain valid.

## Persistence writes

Compare desired unlocked move names against stored membership inside the existing
save transaction. Skip the unlock-table delete and inserts when sets are equal,
independent of iteration order. When membership differs, preserve authoritative
replacement, including removals, and atomicity with the character row. Keep revision
checks and compatibility with multiple repository instances; do not cache membership
outside the transaction. The extra small read trades for fewer writes; no wall-clock
speedup is assumed.

Acceptance: unchanged membership saves succeed with triggers rejecting unlock-table
mutations; changed membership persists; failed inserts roll back both tables; stale
revisions fail; distinct repository instances use current stored membership.

## Audio compression experiment

Preserve the current 23 effects, 44.1 kHz mono 16-bit samples, durations, variations,
and SoundPool resource loading. Read-only encoder experiments decoded every
candidate and verified identical PCM. Unconstrained compression level 12 saved
13,337 bytes, but that configuration can use prediction orders outside FLAC's
streamable subset. Constraining prediction order to 12 saved only 235 bytes;
adding exact Rice parameter calculation saved 1,967 bytes in total. This does not
justify changing all audio assets or expanding decoder compatibility requirements.

Keep the generator, checksum tests, asset bytes and uncompressed resource packaging
unchanged. Report the actual final release APK size, including any implementation
overhead, rather than claiming the rejected resampling savings.

## Verification and independent review

Run formatting, Detekt, JVM tests, debug and release lint, and a release build.
Run connected Android tests when available. Compare final release size with the
saved baseline. Independent skeptical reviewers must inspect the implementation
against this contract; fix actionable findings and rerun affected checks before
delivery. Record measured outcomes and remaining device limitations in this file.

## Implementation review and measured outcomes

Independent reviewers accepted the lifecycle and transaction changes without further
findings. The rendering reviewer found the SDK's swallowed IPC exceptions and a
possible delayed asynchronous clear. The implementation now bounds suppression with
a retry deadline, and regression tests model a silent drop. A second review of that
fix found no remaining actionable defect. A further evidence review checked the
scope, tests and claims.

Unlocked device runs exposed two instrumentation synchronization races. Shortcut
navigation now tolerates a temporarily absent Compose root while polling the
selected gallery, and return-from-settings navigation waits for the destination
button to be displayed. Both waits remain bounded at five seconds and retain their
strict final assertions. Independent skeptical review found no weakened navigation
contract or masked persistent failure. No production change was needed for these
test fixes.

| Check | Result |
| --- | --- |
| Formatting and Detekt | Passed; no findings. |
| JVM tests | 208 passed, zero failures or skips. |
| Focused Android tests | 29 passed on Phone (3), Android 16: 8 repository tests plus 21 runtime, SDK encoding, SoundPool and preview lifecycle tests. |
| Full Android suite | 79 passed, zero failures or skips, on the unlocked Phone (3), Android 16; final run took 60.713 seconds. |
| Debug and release lint | Passed. |
| Release build | Passed. |
| Frame sequence comparison | All 330 scenario hashes match across 63,000 frames, covering forms, scaling, shading, moves and transitions. |
| Stationary simulation with final retry policy | 119 SDK submissions across 1,800 frames, compared with 1,800 previously; 93.39% fewer submissions in this synthetic workload. |
| Desktop cold unshaded render | Three fresh JVM runs: 204–226 ms before and 23–24 ms after. These include class loading and are not Android timings. |
| Release APK | 3,732,246 bytes versus 3,715,862 before: an increase of 16,384 bytes, or 0.44%. |
| DEX payload | 2,157,564 bytes versus 2,155,368 before: an increase of 2,196 bytes. APK alignment amplifies this small code increase. |
| Production Kotlin source | 10,124 lines versus 10,084 before, including the delivery helper and retry policy. This pass improves runtime work, not source or package size. |
| Audio | All 23 packaged payloads are byte-identical to the baseline. |
| Pre-existing gesture edits | Preserved; the tracked gesture diff matches the saved pre-change diff. |

The connected device is the supported A024 model on Android 16. Device validation
is tracked separately below; unit tests, frame hashes and SDK examples do not prove
physical LED persistence, Android cold-start speed or battery savings.

The initial full run was obstructed by Android's secure keyguard. After the user
unlocked the phone, repeated runs exposed the two test synchronization races
described above. One intermediate run also had a missing Compose hierarchy during
a standalone animation screenshot test. Its cause was not established; it did not
recur in the next two full runs, and its image assertions remain unchanged. The
final full run passed all 79 cases, including navigation, animation screenshots,
preview lifecycle and repository transactions. This is a successful device run,
not evidence that every intermittent test issue has been eliminated.

The debug app and instrumentation APK remain installed. The temporary stay-awake
change was restored to its original value. No physical LED observation or Android
battery measurement was made.

Verification commands:

```shell
./gradlew ktlintCheck detekt testDebugUnitTest lintDebug lintRelease assembleRelease --offline
./gradlew connectedDebugAndroidTest --offline
./gradlew :app:ktlintAndroidTestSourceSetCheck :app:detektDebugAndroidTest :app:assembleDebugAndroidTest :app:lintDebug --offline
adb shell am instrument -w -r com.valebell.glyphsquish.test/androidx.test.runner.AndroidJUnitRunner
```

Device UI tests require an unlocked phone. The final successful full suite used
the direct instrumentation command after installing the debug and test APKs;
the earlier Gradle connected run was the keyguard-obstructed attempt. Focused reruns selected
`RoomCreatureRepositoryTest`, `CreatureRuntimePersistenceTest`,
`GlyphUnlockBoundaryTest`, `GlyphFrameEncodingTest`, `GlyphSoundPlayerTest`, and
`PreviewMotionEffectTest`; all passed. The SDK retry correction was included in
the final formatting, analysis, JVM, lint and release checks. The subsequent
test-only synchronization changes passed Android-test formatting, Detekt,
compilation, debug lint and the full device suite.
