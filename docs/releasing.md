# Build and release

Develop on `dev`, cut a release branch when ready, and download the signed APK
from GitHub Actions. Creating the release branch also advances `dev` to the next
minor version. Publishing a GitHub Release remains a manual step.

This follows the [branch-for-release pattern](https://trunkbaseddevelopment.com/branch-for-release/).
Fix bugs on `dev` first, then cherry-pick the needed fixes through a PR into the
release branch. Keep new features on `dev`; do not merge a release branch back
wholesale.

## One-time setup

Set this repository's default branch to `dev` in GitHub settings after it exists.
The workflow files cannot change that setting. Allow the **Advance development
version** workflow to commit `version.properties` directly to `dev`; repository
permissions and branch rules must permit that bot update.

Add these repository Actions secrets under **Settings → Secrets and variables →
Actions** using one release keystore:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The keystore file encoded as Base64. |
| `ANDROID_KEYSTORE_PASSWORD` | The keystore password. |
| `ANDROID_KEY_ALIAS` | The signing key's alias inside the keystore. |
| `ANDROID_KEY_PASSWORD` | The signing key's password. |

Keep the keystore and passwords backed up outside Git, and use the same key for
all public versions so Android can install updates. See
[Android's signing guide](https://developer.android.com/studio/publish/app-signing#generate-key)
for creating a key.

## Publish a version

1. Create a release branch from a green `dev` commit whose version matches the
   branch name:

   ```shell
   git fetch origin
   git switch -c release/1.0 origin/dev
   git push -u origin release/1.0
   ```

   With `versionName=1.0.0` and `versionCode=1` in
   [version.properties](../version.properties), the branch keeps those values.
   **Advance development version** commits `1.1.0` / code `2` to `dev`. That
   version-only bot commit does not trigger another CI build, and rerunning the
   workflow will not bump an already-advanced version again.
2. Wait for **Build APKs** on `release/1.0` to pass, including its signing job.
   Download the signed release artifact from the
   [successful Actions run](https://github.com/valebell/glyph-squish/actions/workflows/build.yml),
   and extract `glyph-squish-1.0.0.apk` and `glyph-squish-1.0.0.apk.sha256`.
   Save the release R8 mapping from the reports artifact for that same run.
3. Install the signed APK on Phone (3) and check the physical Glyph. Device tests
   can also run with `./gradlew connectedDebugAndroidTest` on a debug test
   installation; they do not replace checking the actual LEDs and controls.
4. Create tag `v1.0.0` at the **exact release commit built by that successful run**.
   In [GitHub Releases](https://github.com/valebell/glyph-squish/releases), draft a
   release using that tag, attach the signed APK and checksum, add a short
   changelog, and publish when ready. The workflows do not create or publish
   GitHub Releases.

For a later public APK, update `version.properties` with the new version name and
a version code greater than every previously published APK. This also applies to
patches on older release branches; the automatic development bump does not
allocate version codes across multiple release lines.

## Build artifacts

[Build APKs](../.github/workflows/build.yml) runs on PRs into `dev` and `release/**`,
pushes to those branches, release branch creation, and manual runs from the
Actions tab. It runs formatting, Detekt, JVM tests, and Android lint, then builds
the app and instrumentation tests. Physical device tests are compiled but not
run in GitHub Actions.

Download artifacts from **Actions → Build APKs → a successful run**:

- `glyph-squish-debug-<run>-<attempt>`: installable development APK.
- `glyph-squish-release-unsigned-<run>-<attempt>`: optimized APK before signing.
- `glyph-squish-signed-<run>-<attempt>`: signed APK and SHA-256 checksum,
  available for trusted release branch builds once signing is configured.
- `glyph-squish-reports-<run>-<attempt>`: test/analysis reports and release R8 mapping.

Only push, branch creation, or manual runs on `release/<major>.<minor>` use the
signing secrets, after the build checks pass. Pull requests and `dev` builds
produce unsigned release artifacts. To rebuild an existing release branch,
select it when manually running **Build APKs**.

Signed artifacts expire after 30 days; other build artifacts expire after 14 days.
Downloads require a GitHub account. Public users should install the signed APK
attached to a GitHub Release. CI debug keys
can change between runs, and debug and release APKs share the application ID.
Moving between differently signed APKs requires uninstalling the old app, which
removes its local progress and settings.

[Dependabot](../.github/dependabot.yml) checks GitHub Actions and Gradle dependencies
weekly and opens grouped PRs into `dev`. Review and merge them normally; there is
no automatic merge. The pinned Glyph SDK download is maintained separately.

A first hosted run is still needed to validate GitHub runner downloads, signing,
and repository settings; local checks alone cannot verify those.
