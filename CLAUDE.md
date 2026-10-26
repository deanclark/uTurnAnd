# CLAUDE.md — uTurnAnd working notes

This file is the resume point for Claude Code sessions. **Update it at every checkpoint** with the
current thinking, decisions and next steps so work can restart exactly where it stopped.

## Standing rules

- **Commit and push to GitHub regularly** (after every meaningful, building checkpoint) with clean,
  descriptive commit messages, so status and work are never lost. Do this in *both* repos:
  - `uTurnAnd` (this repo) → https://github.com/deanclark/uTurnAnd.git (branch `master`)
  - `Crystals` (sibling folder `../Crystals`) → https://github.com/deanclark/Crystals.git (branch `master`)
- Keep `readme.md` up to date with behaviour, build instructions and dependency versions.
- **Keep uTurn and Crystals in step.** uTurn consumes `libs/CrystalsCommon.jar`, built from the
  `com/crystalwink/crystalscommon/**` classes of Crystals. After *any* change under
  `Crystals/src/com/crystalwink/crystalscommon/`, rebuild the jar (`./gradlew crystalsCommonJar` in
  Crystals), copy `build/android-libs/CrystalsCommon.jar` to `uTurnAnd/libs/`, rebuild uTurn, and
  commit both repos together. Record the Crystals commit hash the jar came from in the table below.
- A conversation summary lives in `CONVERSATION_SUMMARY.md` (local only, excluded via
  `.git/info/exclude` — never commit it).
- Before closing a session, publish/refresh the "Active Memory" artifact.

## Build / run

- uTurn: `./gradlew assembleDebug testDebugUnitTest lintDebug` (Windows: `gradlew.bat`).
  AGP 9.4.0, Gradle 9.6.0, compile/target SDK 37, minSdk 31 (Android 12), Java 11 bytecode, JDK 17+ (25 works).
- Crystals: `./gradlew build` / `./gradlew crystalsCommonJar` (Gradle 9.8.2, Java 11 release).
  Entry point (desktop applet/frame): `com.crystalwink.crystals.GraphPanel.GraphPanel`/`Crystal`.
- Android SDK: `C:\Users\deanc\AppData\Local\Android\Sdk`; emulators `uTurnApi31`, `Medium_Phone_API_37.0`.

## Jar sync table

| CrystalsCommon.jar in uTurnAnd/libs | built from Crystals commit |
|---|---|
| (original, from `f8f1f12`) | f8f1f12 — class sizes identical to a fresh build at session start |

## Game rules (uTurn)

- Tiles have 4 coloured edge segments (palette index; **0 = outside/border colour**, 1..N = interior colours).
- Player drags a tile onto another to swap them, taps to rotate clockwise, long-press (>=500 ms) to rotate anticlockwise.
- Level is won when every shared edge matches its neighbour and border edges sit on the outside edge.
- Victory is **position-based** (`BoardRules.isSolved`, driven by `uTurn.isBoardSolved`), *not* a stored
  arrangement. Any arrangement meeting the edge rules wins, including the 180°/90° global turns of the
  generated solution. `createSolvedBoard` only guarantees the tile set is solvable before scrambling;
  scrambling may legitimately be solved a different way.
- Levels 1..6 grow the board (3x3 → 5x6); after the win celebration (3 s toast) the next level starts.

## Known issues / decisions log

### Checkpoint 1 (initial review)
- uTurn builds, 4 → 7 unit tests pass. Crystals compiles; 45/73 of its tests fail but nearly all are Eclipse
  `fail("Not yet implemented")` stubs (not regressions).
- **Bug fixed:** `uTurn.isBoardSolved` passed `Color.DKGRAY` as the outside-edge marker while tiles use palette
  index 0, so a level could *never* be won. Now uses `BoardRules.OUTSIDE_EDGE` (=0).
- Removed UI-thread `Thread.sleep` calls (touch 16 ms, idle 500 ms, demo 3 s) → `postInvalidateDelayed` /
  timestamp. Replaced deprecated `SoundPool(…)` ctor, `getDefaultDisplay().getMetrics`, FLAG_FULLSCREEN and
  `requestWindowFeature(NO_TITLE)`. Action bar kept so the options menu is reachable (no menu key on modern devices).
  Board is laid out from view size + window insets (edge-to-edge safe) and centred.
- Emulator `emulator-5554` was unresponsive to adb at the time of the review (adb shell hung); on-device verification
  still outstanding — retry (cold boot the AVD if needed).

## Obsolete / to-modernise list (Android 12+)

- No third-party runtime libraries; the only dependency is `CrystalsCommon.jar` (pure Java, Java 11 bytecode — fine).
- Eclipse leftovers (`.classpath`, `.project`, `.settings`, `project.properties`) — obsolete, harmless; can be deleted.
- `GridView` layout `res/layout/main.xml` only used by dead code path (`showAllTiles=false`).
- Icon only has ldpi/mdpi/hdpi PNGs → add xhdpi/xxhdpi + adaptive icon (required look on Android 8+).
- `android:screenOrientation="portrait"` is ignored on large screens from Android 16; board already lays out from view size.
- Hard-coded colours `Color.DKGRAY/RED/…` palette → needs a replacement (patterns/pictures) for viral appeal.
- AGP 9.4.1 / Gradle 9.8.2 are available (lint notes); optional bump.

## Backlog (in rough priority)

1. Crystals: HighWater/backtrack oscillation in non-floating mode (`FrameStructure.drawFrame`).
2. Crystals: floating mode tile overlap (`GraphPanel.relax` dy bug, overlap resolution, "Large" tile size).
3. Crystals: non-floating Wireframe checkbox should draw tile segment wireframes; unchecked should fill tiles.
4. Rebuild + copy `CrystalsCommon.jar`; record hash above.
5. uTurn: replace colour palette with patterns/pictures per segment; celebration animation; icon set; settings.
6. Verify on emulator (API 31 and 37) once adb is healthy.
