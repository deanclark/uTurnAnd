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
- Before closing a session, publish/refresh the "Active Memory" artifact (https://claude.ai/artifact/CMijS2TFuWfWq4GvSFSnt6).

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
| rebuilt after Tile.rescale/layoutSegments, LockManager.decreaseLock, Segment.setActiveWire fixes | df51356 |

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
- On-device verification still outstanding: the only AVD with an installed image, `Medium_Phone_API_37.0` (started by
  Android Studio, pid 59868/63516), hung and went offline (adb shell timed out, `adb devices` = offline). The
  `uTurnApi31` AVD has no system image installed, and a 2nd instance cannot start while the first is not `-read-only`.
  Next session: restart the AVD (cold boot) then `gradlew installDebug`, play a level to a win, check the menu and insets.

### Checkpoint 2 (Crystals fixes, jar re-synced to Crystals df51356)
- **HighWater oscillation** (non-floating): the fixed backtrack depth (3) made the placed count saw-tooth N, N-3, N...
  `FrameStructure.nextBacktrackDepth` now escalates the depth on repeated dead ends at no greater depth and resets on
  progress; the hint tile is never undone. Reproduced and verified with a headless harness (3000 steps: max placed 61 -> 130).
- **Non-floating wireframe/fill**: `drawFrame` only drew edge lines. `drawPlacedTiles`/`drawCell` now draw each placed
  tile as 4 triangular segments; Wireframe = outline in segment colour, unchecked = filled (verified by rendered PNGs + tests).
- **Floating overlap**: `relax()` used `secondaryTile.yWorldSpace` instead of `relativeSecY` for the y offset;
  `resolveTileOverlaps` ignored locked-vs-locked pairs (weaker lock now yields); right/bottom clamp margin fixed.
- **Large tick box** is now a dedicated checkbox in both modes: `GraphPanel.setTileScale` -> `Tile.rescale`
  (15 / 28 px tiles; frame cell 6 / 40 px). The old behaviour where Large toggled the *random* flag in frame mode is gone;
  `Active` is floating-only (hidden in frame mode).
- Misc: `LockManager.decreaseLock` AIOOBE on unknown lock; `Segment.setActiveWire` compared fromSegment twice.
- NOT verified interactively: floating-mode relax() visuals (Applet cannot be constructed headless) - unit tests only.
  Eyeball it on the desktop (`./gradlew run` in Crystals).
- Crystals test suite: 82 tests, 45 fail - all pre-existing Eclipse `fail("Not yet implemented")` stubs.

### Checkpoint 3 (device check)
- The hung AVD had closed; `Medium_Phone_API_37.0` was started fresh (`emulator -avd Medium_Phone_API_37.0 -no-snapshot
  -no-audio -gpu swiftshader_indirect`). `gradlew installDebug` works; the app launches on Android API 37 with the action
  bar + overflow menu visible, board centred, no crash. A real `adb shell input swipe` between two tiles swapped them.
- Still unchecked on device: reaching a win (no solver; covered by BoardRulesTest), sound, API 31.

## Obsolete / to-modernise list (Android 12+)

- No third-party runtime libraries; the only dependency is `CrystalsCommon.jar` (pure Java, Java 11 bytecode — fine).
- Eclipse leftovers (`.classpath`, `.project`, `.settings`, `project.properties`) — obsolete, harmless; can be deleted.
- `GridView` layout `res/layout/main.xml` only used by dead code path (`showAllTiles=false`).
- Icon only has ldpi/mdpi/hdpi PNGs → add xhdpi/xxhdpi + adaptive icon (required look on Android 8+).
- `android:screenOrientation="portrait"` is ignored on large screens from Android 16; board already lays out from view size.
- Hard-coded colours `Color.DKGRAY/RED/…` palette → needs a replacement (patterns/pictures) for viral appeal.
- AGP 9.4.1 / Gradle 9.8.2 are available (lint notes); optional bump.

## Backlog (in rough priority)

1. Verify on emulator/device (API 31 and 37): win a level, menu reachable, insets, sound.
2. Eyeball Crystals floating mode on the desktop (`./gradlew run` in Crystals): tiles should butt, not overlap, at both Large settings.
3. uTurn: replace the colour palette with patterns/pictures per segment (viral appeal); celebration animation; share action;
   adaptive icon + xhdpi/xxhdpi; real settings screen; delete Eclipse leftovers and the dead GridView path.
4. Crystals: finish or delete the 45 `fail("Not yet implemented")` stub tests.
5. Optional: AGP 9.4.1 / Gradle 9.8.2 bump in uTurn.

Done so far: items 1-4 of the original backlog (backtrack oscillation, floating overlap, non-floating wireframe/fill, jar sync).
