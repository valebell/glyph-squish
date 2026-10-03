# UI/UX and companion gameplay plan

## Rollback points

The existing work is preserved in `9048516` on `main`. UI improvements are developed
on `codex/ui-ux-polish`, with a separate commit for each step below. Revert individual
implementation commits in reverse order to return to the checkpoint without
discarding later work. This plan is committed before implementation.

## Phase 1: UI/UX (current implementation)

1. **Simplify copy and hierarchy.** Remove the permanent form-selection hint,
   starter availability labels, and unlock history on available forms. Keep locked
   requirements and an explicit Selected badge. Use sentence case, concise progress
   counts, and no trailing periods on short standalone UI copy. Keep punctuation
   between sentences in accessibility descriptions. Explain that unlock recipes
   currently run on the physical Glyph Matrix, and specify the Glyph Button hold.
2. **Improve readability and scaling.** Use body typography for instructions,
   restore readable label sizes, correct the preview identity contrast, and let
   labels, badges, and requirements grow with font size. Adapt gallery columns and
   header arrangements for large text. Keep selected states identifiable without
   relying only on color. Preserve reduced-motion and existing preview behavior.
   Use the compact caption “Tilt, shake, squish!” beneath the preview and move
   detailed gesture guidance into Settings.
   Match help dialogs to the app’s existing surface, border, typography, and button
   styling (user feedback during review).
3. **Make locked-card actions useful and accessible.** Open an accessible Material
   dialog containing the relevant recipe and physical-Glyph context. Name the
   action appropriately for assistive technology, preserve selection semantics,
   and mark section headings for screen-reader navigation. Tapping a locked card
   must not change selection or progression.
4. **Optional Glyph setup guidance.** Open directly into the app and show a small,
   dismissible setup prompt using the existing Glyph Toys intent. Keep the preview
   caption permanently and detailed physical-Glyph controls in Settings.

Update existing UI assertions with copy changes. Add focused regression coverage
for contrast, large-text layout, and locked-card behavior. Run formatting, Detekt,
unit tests, Android lint, a release build, and device tests when a device or emulator
is available. Record results and commit each completed step.

## Phase 2: companion gameplay (deferred)

The intended direction is a complete in-app toy with the physical Glyph Matrix as
an additional way to play. Do not remove the unsupported-device screen before the
companion experience and capability handling are ready.

1. Separate basic app access from hardware capability. Make the companion available
   on supported Android versions, bind Glyph services only where supported, and
   replace the blocking compatibility screen with contextual, dismissible messaging.
   Explain the actual benefit: playing on the rear Glyph Matrix on Nothing Phone (3).
2. Support the same unlock recipes on both surfaces, including an on-screen Glyph
   Button equivalent. Keep progression shared on the phone, recognize input within
   its active surface/session, and prevent mixed inputs or replay animations from
   accidentally completing recipes. Preserve existing saved progress.
3. Add understandable recipe progress and completion feedback. Locked cards explain
   recipes; they do not unlock content just by being tapped. Replace Phase 1's
   physical-only instruction once both surfaces support progression.
4. Provide equivalent accessible interactions for motion-dependent recipes so that
   precise circles, tilting, shaking, or a physical button are not the only paths
   to the same rewards. Account for missing motion sensors and reduced motion.
5. Test companion-only devices, Nothing Phone (3), switching between surfaces,
   persistence/restarts, sensors, TalkBack, Switch Access, large text, and both
   color themes. Commit capability handling, recognition, accessible controls,
   and guidance changes separately.

## Design references

- [Material 3 UX writing](https://m3.material.io/foundations/content-design/style-guide/ux-writing-best-practices)
- [Material 3 punctuation](https://m3.material.io/foundations/content-design/style-guide/grammar-and-punctuation)
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)
- [Android text contrast](https://developer.android.com/guide/topics/ui/accessibility/apps#increase-text-visibility)

## Onboarding removal (2026-10-03)

The first-launch introduction was removed after reviewing it in the app. The
app opens directly into its existing home screen. The practice controller, gesture
validation, completion preferences, resources, tutorial tests, and preview hooks
added solely for the introduction are removed. The experiment is preserved in
commits `7f941c4` through `417ff08` for rollback. The existing Settings help remains.

The preview now permanently retains “Tilt, shake, squish!” and Settings help explains
physical Glyph controls. The setup card uses the app's surface, outline, type, and
rounded Material 3 Button with expressive pressed shapes. Its action is “Open Glyph
Toys”; dismissal uses a labeled Material IconButton. Both retain at least 48 dp touch
targets; text can wrap and content scrolls with the home screen.

The prompt remains until dismissed or the system selects Squish by binding the Glyph
service. This guidance history is persisted separately from gameplay in DataStore.
Opening the manager does not imply completion. A failed launch gives the manual path,
and Settings always retains access to Glyph Toys. Read failures omit this optional
prompt; a failed dismissal write keeps it hidden for the current visit.

Nothing's public SDK does not document an enabled-queue membership query. Observing
Glyph use can suppress guidance but cannot detect subsequent removal from the queue.
Existing users without recorded use may see the prompt once, even if already enabled.
Reference: [Nothing's manager and service lifecycle guidance](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit#best-practice-guiding-users-to-glyph-toys-manager).

Rollback points for this revision:

- `ee2c62f`: remove onboarding and all dedicated practice code
- `faeeeed`: persistent preview caption and physical Glyph controls in Settings
- The following setup-prompt commit: optional guidance and its persistence


Validation for this revision (2026-10-03):

- 156 JVM tests and all 71 instrumented tests passed on A024 / Android 16.
- Setup coverage includes manager launch without false completion, persistent
  dismissal, continued Settings access, observing guidance history written by another
  owner, and manual fallback with wrapping text and accessible dismissal at 200% text.
- Existing gallery, selection, shortcut, compatibility, contrast, and scaling checks pass.
- `ktlintCheck`, Detekt, debug/release lint, and `assembleRelease` pass. Lint retains
  the pre-existing informational autoboxing hint in `AppearanceState.kt`.
- Reviewed device captures of the home prompt, physical-Glyph help, and 200% text.
  Screenshots are in `/private/tmp/squish-optional-setup-review-2026-10-03/`.
- Manual TalkBack/Switch Access and a physical enable/remove cycle were not performed.
  Automatic prompt hiding is tested through the shared persistence signal; there is
  no claimed detection of current enabled-queue membership.
