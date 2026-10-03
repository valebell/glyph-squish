# Glyph Squish

**Tilt, shake, squish!** A little creature living on the Glyph Matrix of your
**Nothing Phone (3)**.

<img src="app/src/main/res/drawable-nodpi/toy_preview.png" alt="Squish in its Blob form on the Glyph Matrix" width="160" height="160">

Move Squish around the back of your phone, discover new moves, and unlock new forms.
The companion app keeps a live preview, your collection, and your settings together.
It supports English and ten translations, including Arabic and Urdu with RTL layouts.
Built by Valentin with AI assistance through Codex, with the engineering decisions
and automated checks shared alongside the code.

## Play

Glyph Squish requires **Nothing Phone (3)**. Other devices show a compatibility
screen and cannot use the toy.

1. Install the APK on your Phone (3). Once a version is published, use the signed
   APK attached to a [GitHub Release](https://github.com/valebell/glyph-squish/releases).
   For development builds, download the debug APK artifact from a successful
   [Build APKs run](https://github.com/valebell/glyph-squish/actions/workflows/build.yml).
2. Open Glyph Squish and go to **Settings → Glyph Toys** to add it to your Glyph.
   You can also use your phone's **Settings → Glyph Interface → Glyph Toys**.
3. Activate Glyph Squish on the rear Glyph Matrix and start moving.

If Android asks, allow your browser or file manager to install the downloaded APK.

| Action | What happens |
| --- | --- |
| Tilt your phone | Move Squish around the matrix. |
| Shake | Cycle through your unlocked forms. |
| Hold the rear Glyph Button | Squish the creature and play your selected move. |
| Open the companion app | Browse forms and moves, replay animations, and adjust feedback. |

Blob and Burst are yours from the start. Discover four more moves by playing on
the **physical Glyph Matrix**; each one reveals a new form. Your selections and
unlocks are saved between sessions, and the launcher icon follows your chosen form.

<details>
<summary>Show the unlock recipes</summary>

| Move | Recipe | Reveals |
| --- | --- | --- |
| Helix | Complete two circles around the rim. | Butterfly |
| Echo | Bounce left → right → left → right, or top → bottom → top → bottom. | Cat |
| Ricochet | Cross the same diagonal twice. | Shark |
| Thunderbolt | Bounce between opposite edges, hold the Glyph Button, then circle the rim after your move finishes. | Star |

The Moves gallery also shows these recipes. Each recipe triggers its unlock
animation once; repeating a learned recipe does nothing. Replay unlocked moves
with the Glyph Button or in the companion app. The companion preview does not
unlock moves.

</details>

## Built with AI, checked with automation

AI collaboration is part of this project's development story. The
[repository](https://github.com/valebell/glyph-squish) includes the Kotlin
implementation, regression tests, review notes, and build
workflows so you can inspect the result and the decisions behind it.

The [development guide](docs/development.md#ai-collaboration-and-review) connects
concrete examples: simplifying the companion app, fixing gesture recognition,
and revising an optimization after review exposed a silent SDK failure mode.
Historical reviews record their own results and limitations.

GitHub Actions checks pull requests into `dev` and `release/**` with formatting,
static analysis, unit tests, and Android lint, then produces a debug APK and an
unsigned release APK. Builds on `release/<major>.<minor>` also produce a signed
APK and checksum once the signing secrets are configured. Publishing that APK to
GitHub Releases is a manual step. Device tests and hands-on Glyph testing remain
separate from those automated build checks.

## Build

Use **JDK 25**, **Android SDK 37**, and the checked-in Gradle wrapper. The project
uses Nothing's Glyph Matrix SDK 2.0, obtained separately from its official source.

```shell
python3 tools/fetch_glyph_sdk.py
./gradlew assembleDebug
```

The development APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
See the [development guide](docs/development.md) for SDK setup, quality checks,
architecture, and asset generation, and the [release guide](docs/releasing.md)
for APK signing and GitHub workflow setup.

Development happens on `dev`. Open feature and fix pull requests against that
branch. Creating `release/1.0` keeps version `1.0.0` on that branch and advances
`dev` to `1.1.0` automatically. The [release guide](docs/releasing.md) explains the
one-time setup and how to download and publish the signed APK.

## Feedback

Found a bug or have an idea? [Open an issue](https://github.com/valebell/glyph-squish/issues)
with your Phone model, Nothing OS version, app version, what you did, and what you
expected. A short recording is especially useful for motion or animation problems.

Made for the [Nothing Playground community](https://playground.nothing.tech/toys),
using [Nothing's Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit).
