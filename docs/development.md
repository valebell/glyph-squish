# Developing Glyph Squish

[Back to the project README](../README.md)

This guide covers the build, architecture, and behavior contracts behind the app.
Historical review notes record what was tested at each stage; their test counts,
commit references, and local report paths describe those snapshots. Generated
reports under `app/build/` are not included in the repository.

## Build prerequisites

- JDK 25. The checked-in Gradle daemon configuration also requests this version.
- Android SDK Platform 37 and the Android SDK command-line tools.
- An Android SDK location supplied through `ANDROID_HOME` or an untracked
  `local.properties` file containing `sdk.dir=/absolute/path/to/Android/sdk`.
- Python 3.9 or newer to fetch the pinned Nothing Glyph Matrix SDK 2.0.

Use the checked-in `./gradlew` wrapper. The project pins Gradle 9.8.0,
Android Gradle Plugin 9.4.1, and Kotlin 2.4.20; the version catalog is the source of
truth for library versions. The Android minimum SDK is 34, and both the compile
and target SDK are 37. Phone (3) is the only supported device for the app itself.

Fetch the SDK from Nothing's official repository. The helper verifies the pinned
artifact checksum and places it at `app/libs/glyph-matrix-sdk-2.0.aar`, which is
excluded from Git. Then create a development APK:

```shell
python3 tools/fetch_glyph_sdk.py
./gradlew assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. A connected Phone (3)
with USB debugging enabled can install it with `./gradlew installDebug`.
The application ID and Kotlin namespace are `com.valebell.glyphsquish`.
Changing from the old application ID installs a separate app; earlier progress and
settings are not transferred automatically.

## Branches and automated builds

Use `dev` as the integration branch and target it with feature and fix pull
requests. [Build APKs](../.github/workflows/build.yml) checks pull requests into
`dev` and `release/**`, pushes to those branches, release branch creation, and
manual runs. Successful builds provide debug and unsigned release APK artifacts.
Release branch builds additionally sign the optimized APK in a separate job once
the repository's signing secrets are configured. Pull requests do not use those
secrets. Unsigned APKs cannot be installed directly.

[version.properties](../version.properties) is the source of the app's
`versionName` and `versionCode`. Creating `release/1.0` preserves `1.0.0` / code `1`
on that branch. [Advance development version](../.github/workflows/bump-dev.yml)
then commits `1.1.0` / code `2` to `dev`. Rerunning that workflow does not increment
an already-advanced development version. Its bot commit changes only version
metadata and does not trigger another build.

The [release guide](releasing.md) covers repository setup, signing secrets,
release branches, and manually publishing the signed APK and checksum. Each
subsequent public APK needs a version code greater than every earlier public APK,
including releases maintained on different branches. GitHub-hosted builds do not
exercise the physical Glyph Matrix; keep device validation explicit in release
notes.

## AI collaboration and review

Glyph Squish is built by Valentin with AI assistance through Codex. Human direction
sets the experience and evaluates how it feels on the phone; AI assistance supports
implementation, documentation, test work, and review. The review records make the
tradeoffs inspectable:

- The [companion design review](companion-review.md) explains why the interactive
  training flow was removed and why unlocks belong on the physical Glyph Matrix.
- The [gesture review](gesture-tutorial-review.md) records recognition failures,
  their fixes, and the difference between simulated input and physical feel.
- The [performance specification](performance-improvements-spec.md) records accepted,
  rejected, and deferred optimizations. An independent review caught that the SDK
  can hide IPC failures, so duplicate-frame suppression gained a bounded retry.
  It also separates desktop probes from measurements on a phone.
- The [September code review](code-review-2026-09.md) documents architecture cleanup
  and the reasoning behind the automated quality rules.

These records describe decisions and evidence, including limitations. Passing
checks helps catch regressions; it does not establish the quality of physical
Glyph interactions or battery behavior.

## Behavior reference

The app opens directly into the gallery, with “Tilt, shake, squish!” beneath the
live preview. Settings > Help > How to play explains the physical Glyph controls:
tilt to move, shake to switch unlocked forms, and hold the rear Glyph Button to
squish and play the selected move.

Settings > Glyph Toys opens Nothing’s Glyph Toys manager to add Glyph Squish to the Glyph.
A failed launch shows manual directions beside the settings entry. Setup guidance
stays in Settings, keeping the gallery focused on the live preview, forms, and moves.
No introduction or tutorial completion is required.

The interface follows Android’s app language setting, with English, Simplified Chinese,
Hindi, Spanish, French, Arabic, Bengali, Portuguese, Russian, Urdu, and Indonesian.
Choose a language in Android’s per-app language settings, or leave it on the system
default. The product name is unchanged in every language. Arabic and Urdu mirror
navigation, galleries, and directional icons; physical Glyph directions and the
Matrix preview retain their real-world orientation. System fonts provide script
fallbacks, and joining scripts use proportional labels without extra letter spacing.

On other phones, launches and launcher shortcuts show a dedicated welcome explaining
that Glyph Squish requires Nothing Phone (3). The welcome screen pairs “Hey! I’m Squish.”
and “Only on Nothing Phone (3)” with a short Glyph Matrix explanation and a filled
Close button matching the app’s action buttons. The real Burst move plays as a
greeting: Squish explodes into pixels, then reassembles. Burst plays once, then the
engine's natural floating and blinking keep Squish alive at about 30 fps. Animation
respects disabled system animations and pauses in the background. Tapping Squish
while idle replays Burst; taps during a move are ignored. The same action is
available to accessibility services and is disabled when animations are off.
Landscape places Squish and the greeting on the left, with the explanation and
Close button on the right; portrait keeps them stacked. Both layouts support
scrolling, large text, TalkBack, and both color themes. Compatibility
uses the bundled SDK's `Common.is23112()` check; unsupported devices do not enter the
companion or bind a Glyph session.

Burst is available from the start. Motion recipes permanently unlock four more special
moves: two circles unlock Helix, a double opposite-edge bounce unlocks Echo, two
traversals of one diagonal axis unlock Ricochet, and bouncing, pressing the Glyph Button,
then drawing a circle unlocks Thunderbolt. Thunderbolt zigzags, explodes the
active form into the Glyph rim, flashes the full matrix, and then restores the character.

Each learned move also reveals a particle-morphed form: Ricochet reveals Shark, Echo
reveals Cat, Helix reveals Butterfly, and Thunderbolt reveals Star. Blob is always
available. The live creature stays visible while a floating Forms/Moves control switches
the gallery below it. Shaking the phone cycles through unlocked forms and remembers the
last selection across launches. Long-press the Squish launcher icon for Forms and
Moves shortcuts that open either gallery directly, including while the app is running.
Shortcuts preserve the selected form, move, and progression.

The launcher icon follows the selected form, including changes made by shaking during
Glyph play. All five adaptive icons use white square pixels on black, with matching
form-only monochrome layers for themed launchers. Android launchers may briefly cache
the previous icon while processing the component change. Icon changes are queued while
the companion is visible or a Glyph session is active, then applied after both end.
This prevents Android from finishing the visible task when its launch alias is disabled;
rotation and overlapping companion activities also retain the current alias.

The Glyph Toys manager uses a fixed Blob preview, since Nothing's documented service
metadata points to a packaged drawable. It has its own full-size artwork matching the
[Phone (3) preview specification](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/image/23112_spec.svg):
512×512 canvas, 440×440 matrix, 36px margins, 13.05px square LEDs, black background,
white lit pixels, and `#1C1C1C` unlit pixels. Launcher layers use separate adaptive-icon
sizing. The manager preview is a density-independent PNG; launcher layers remain vectors.
Regenerate these assets from the runtime form masks with
`python3 tools/generate_icons.py`.

During active play on the physical Glyph Matrix, the solid creature body stays uniform at every depth:
255 inside the engine, converted to 4095 and scaled to **3481 (85%) in the raw hardware frame**.
The SDK object's 0–255 brightness setting is a multiplier, not the raw-array range.
Trails, particles, and transition fades pass through the same conversion; the companion
preview retains its shaded volume rendering. See the
[Glyph brightness contract and regression checks](glyph-brightness.md) before
changing rendering or upgrading the SDK.

After two seconds without physical movement or input, the Glyph fades to 25% brightness
over 1.5 seconds. Movement, shaking, or button input restores 85% active brightness over 150ms.
Holds, special moves, and form transitions keep it awake. Small posture changes, slow drift,
and brief sensor spikes do not count as activity. Motion must turn the phone by about ten
degrees within one second or sustain at least 0.10g acceleration for 120ms. Entering idle
clears earlier motion history, so a tiny extra tilt cannot complete an old wake gesture.
Each new Glyph session starts at 85% active brightness. This dimming affects
only the physical output, not the companion preview or saved progression.

The Moves gallery explains locked recipes directly on its cards. There is no
companion training mode: new moves must be learned while playing Squish on the
physical Glyph Matrix. Companion preview contacts cannot start, complete, or
interrupt a Glyph recipe. Burst is available from the beginning; learned moves
can be selected and replayed with the existing sounds and haptics. After the first unlock,
repeating a recipe on the Glyph does nothing, regardless of whether the companion
has shown or acknowledged the new form. Long-pressing the Glyph Button plays the
selected move, including during a Thunderbolt attempt. While Thunderbolt is still
locked, a valid bounce followed by that long press also advances its recipe: after
the move finishes, complete the circle to unlock it. Queued and active playback
pause the recipe timeout. Another long press before expiry replays the selected
move and restarts the final circle without forgetting the bounce. An expired
prefix or an already unlocked Thunderbolt uses normal selected-move playback.
Queued/active performances and form transitions cannot contribute recipe contacts.
A physical button action during a form transition queues until the transition ends;
an already active or queued move does not accept overlapping playback.

Locked forms and moves keep their requirements inline. Tapping one plays the selection
sound and animates the lock without changing the selection or opening a dialog.
The How to play dialog in Settings uses the same navigation feedback as the rest of
the app when it opens and closes, respecting sound and vibration preferences.
Save failures use an inline Material card with Retry/Dismiss; a failed Glyph Toys
launch is shown beside its settings entry.

Circles require a full turn (in either direction); two turns may use opposite
directions. Circle recognition follows the character's position between impacts,
so an arc that moves inside the rim still counts. A circle starts in the outer
region and can continue inward; crossing the center breaks circular progress.
Bounces and diagonals still need firm contacts. Soft rebounds at the same known
edge preserve the preceding firm contact without extending its timeout.
Repositioning through the center preserves their endpoints and the bounce/button
prefix for Thunderbolt, within the existing timeout. Unreliable weak contacts add
no travel and do not extend it.
Reversing an armed Thunderbolt circle restarts its circle step, both at the rim
and inside it, while preserving the completed bounce/button steps.
Pauses longer than four seconds without progress, depth-dominant contacts, shaking
on the Glyph, and ending a Glyph session reset incomplete attempts. Major reversals
restart circular progress. Recognition progress is transient and is never written
to Room.

Debug and release builds both use the normal progression and unlock recipes.
UI tests supply fake runtime states to exercise unlocked forms and moves.

Newly revealed Forms hold the lock for 400 ms, then dissolve it into the creature
over 1.2 seconds with a synchronized pixel burst when the card first becomes visible
in the foreground Forms gallery. A faint, slowly breathing border glow follows. The glow
persists across gallery visits and launches until that form is selected; an already
selected form is never highlighted. Scrolling or configuration recreation does not
replay a card's burst. With system animations disabled, only the static glow appears.
TalkBack retains the accessible New state without a visible badge. Selecting a
different unlocked form or move in the portrait gallery smoothly returns to the
preview at the top; landscape keeps its scroll position. Locked and current-card
taps do not scroll.
Acknowledged forms persist in Room using the existing seenForms metadata. Database
migrations are not supported during development; clear app data when an installed
database uses an older schema. Legacy `CLOVER` form values still load as Butterfly.

## Architecture


The companion app is a single-activity, single-screen Compose UI. `MainActivity` hosts
one persistent live matrix preview and switches the Forms and Moves galleries in place,
while `SquishViewModel` exposes the app-scoped creature runtime’s `StateFlow` directly.
The preview and gallery thumbnails are rendered with Compose `Canvas`; there are no XML
layouts, fragments, custom Android `View` classes, or secondary activities.

`SlimeEngine` coordinates a per-engine `SlimeScene`, motion and move components, and
renderers. Home layout, preview, gallery, and transitions live in separate files. Audio
playback uses Android SoundPool with up to six simultaneous streams. Packaged lossless
FLAC samples preserve every 44.1 kHz, 16-bit mono sample and all three form variations;
loading is asynchronous and the pool is released when sound is disabled or the UI is
disposed. FLAC resources are excluded from additional ZIP compression so SoundPool can
open them directly by file descriptor.
The sample synthesizer lives in the test/tooling sources; runtime playback uses the
packaged samples and retains only the shared profiles and haptic cues.
With FFmpeg on `PATH`, regenerate samples after changing the sound profiles or animation
timings with `./gradlew :app:generateSoundAssets`. Generation decodes each FLAC and checks
that its PCM exactly matches the synthesizer before replacing the assets. Unit tests
check FLAC metadata and PCM checksums against the synthesizer, enforce SoundPool's
decoded sample limit, and reject duplicate WAV assets. Device tests load every sound
variation through SoundPool. Normal builds and unit tests do not require FFmpeg.

Sound and vibration preferences use one process-wide Preferences DataStore. Compose
observes its Flow and enables feedback only after the saved settings load. Failed reads
or writes expose Retry in Settings. The old SharedPreferences values are intentionally
not migrated; the first launch with DataStore uses the default feedback settings.

The exported `SlimeToyService` remains a separate Android service for the physical
Glyph Matrix. It observes the same runtime state as the Compose app, so changes made
on either surface stay synchronized.

Radial geometry is cached separately for each form and computed only when shading or
particles need it. The Glyph service immediately submits changed encoded pixels,
including brightness fades, and retries unchanged output about once per second because the
SDK can hide IPC failures. Simulation and input retain their existing cadence.
Registration and shutdown clear the delivery cache. Companion sensors run
only when creature state is ready and the activity is resumed, while shaking still
works when browsing a scrolled gallery.

Persistence uses Room's suspending DAO methods and coroutine transactions in the
application scope. Saves are serialized across suspension so rapid selections keep
their revision order, while the UI updates immediately. A failed save does not block
later saves or Retry, and character metadata and unlocked moves commit together.
Saves compare stored unlock membership inside the transaction and replace those rows
only when the set changes.

## Toolchain

Kotlin and the Compose compiler use 2.4.20. The root build explicitly supplies the
Kotlin Gradle plugin version while retaining AGP 9.4.1’s built-in Kotlin support.
Gradle 9.8.0 and the configured JDK 25 daemon compile against Android API 37; the
app supports API 34 and newer. Material 3 remains on its alpha release line
because the UI uses experimental expressive components.

## Code quality

Check Kotlin formatting:

```shell
./gradlew ktlintCheck
```

Automatically fix supported formatting issues:

```shell
./gradlew ktlintFormat
```

Run unit tests and build the application:

```shell
./gradlew detekt testDebugUnitTest lintDebug lintRelease assembleRelease
./gradlew connectedDebugAndroidTest
```

Both cyclomatic and cognitive complexity are limited to 15 across production and tests.
Functions and constructors are limited to seven parameters, including default parameters
and data classes. `detekt` runs type-aware app and test analysis to enforce these rules;
it also runs with `check`. The explicit rules in `config/detekt.yml` additionally check
nullable access and casts, equality contracts, ignored return values, resource handling,
coroutine scopes and cancellation, and exception handling across production and tests.
Configuration warnings fail analysis. Formatting remains owned by ktlint; Detekt does
not inherit its full default style/naming ruleset or use a baseline to hide findings.
Kotlin compiler and Android lint warnings fail
the build. The [September 2026 review](code-review-2026-09.md) records the refactor,
movement validation, dependency versions, and remaining upstream tool warnings.

The [cleanup review](cleanup-review.md) records the original cleanup baseline.
The [gesture review](gesture-tutorial-review.md) records the recognition fixes.
The [companion design review](companion-review.md) describes the current
Glyph-only progression and simplified gallery design.
The [performance specification](performance-improvements-spec.md) records the
challenged optimization decisions, acceptance checks, and measured results.

Room schemas are exported to `app/schemas` and should remain versioned with database
changes.

SonarQube for IDE is configured as a required Android Studio plugin. It analyzes open
Kotlin files automatically without an account, credentials, Docker, or a SonarQube server.

To analyze the whole project, right-click the project directory and select
**SonarQube for IDE** > **Analyze with SonarQube for IDE**. Findings appear in the
**SonarQube** tool window.
