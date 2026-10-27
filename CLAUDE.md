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

### Checkpoint 4 (tiles drifting off the grid)
- Reported on the emulator: after moves, tiles overlapped and sat off-grid. Cause: `TilePrintableAndroid` mutated each tile's
  `Path` with accumulated offsets (rounding + repeated drags drifted). Rewritten to draw every frame from the tile's absolute
  position (world centre, or touch point while dragged). Touch handling rewritten: drag follows finger (grab offset kept);
  drop on another tile = exact swap of grid centres; tap/hold without travel = rotate CW/ACW; anything else snaps back.
  Verified on API 37 with 6 mixed swipes (incl. drop outside board): all tiles on grid.
- Removed `translateTilePath`/`moveComplete`/in-transit offset code (demo mode uses absolute `setInTransitTile`).

### Checkpoint 5 (demo mode = planned solve)
- Old random-move demo removed. `DemoSolver` (pure Java, unit tested on all 6 level sizes) searches an assignment for the
  current board (perimeter first, then inner cells) and returns a plan ordered CORNER -> EDGE -> INNER. Corner tiles only go
  to corners and edge tiles only to edges, each rotated (one CW quarter turn at a time) until outside edges face the border.
  Before the first inner tile the demo calls `BoardRules.isBorderValid` and toasts "Border verified" (stops if invalid).
- `uTurn.UTurnView.demoTick()` is the executor (phases NEXT/DRAG/ROTATE, driven from onDraw, no sleeps); touching the screen
  or the menu item stops it. Verified on API 37: ring placed first, then inner tiles, "Level 1 complete!" shown (first
  real win on a device).
- Tests: 13 in uTurn (BoardRulesTest 7, DemoSolverTest 6).

### Checkpoint 6 (all levels + sound on device)
- Scripted run on the API 37 emulator (sound on, Demo Mode started each level via the menu): every level 1-6 solved and
  "level N complete" logged (3x3 18s, 3x4 25s, 4x4 28s, 4x5 34s, 5x5 40s, 5x6 53s; plans of 9/12/16/20/25/30 placements).
- Sound: both samples load with status 0; 240 `SoundPool.play` calls during the run all returned a non-zero stream id.
  The emulator runs with `-no-audio`, so audible output is NOT confirmed - listen on a real device.
- uTurn.java now logs (tag `uTurn`) sound load/play, demo plan size and level completion.

### Checkpoint 7 (unlimited levels, saved progress, rotation, wishlist)
- `LevelRules`: unlimited levels. 3x3 .. 6x8 over the first 9 levels (first six unchanged), then fewer interior colours
  (5 -> 4 at level 3, 4 -> 3 at level 15). `MAX_TILES` = 48. `DemoSolver` now skips look-alike tiles; unit test solves levels 1-50.
- Progress: `GameState` (text encode/decode, validated) + SharedPreferences `uturn` (`level`, `sound`, `board`). Saved in
  `onPause` and on level advance; restored in `onCreate`/`init()` only when level and board size still match. Verified on API 37:
  level 2 board identical after Home+relaunch and after force-stop.
- Rotation: portrait lock removed, `configChanges` handles it; `rescaleBoard()` refits tiles to their cells instead of restarting.
  Verified in landscape: board kept, action bar + overflow menu follow the device orientation.
- `FEATURES.md` = feature wishlist (themes, patterns/pictures, music, hints, daily puzzle, sharing...). Music sources to check
  licences of: FreePD, OpenGameArt CC0, Pixabay, incompetech (CC BY). Keep a CREDITS file when added.
- Known: landscape board is just smaller, not re-laid out. (Demo solve used to run on the UI thread; fixed in checkpoint 14.)

### Checkpoint 8 (easier early levels, soft colours, edge-colour options)
- Colour schedule (`LevelRules.COLOURS`): levels 1-9 use 7,7,6,6,5,5,4,4,4 interior colours, 4 until level 14, then 3
  (more colours = fewer look-alike edges = easier). Palette gained three softer colours: 6 lavender, 7 peach, 8 pink.
- Edge colouring options for the solved board: `BoardRules.createSolvedBoard` (repeating pattern, current default) vs
  `createRandomSolvedBoard(..., seed, avoidDuplicates)`. Finding: the repeating pattern gives 20 duplicate tiles on a 6x8/4-colour
  board; random gives ~4, best-effort duplicate-free ~2 (zero is impossible with 4 colours). With 7 colours a 3x3 is duplicate-free.
  Comparison image: `doc-files/edge-colour-options.png`. **DECIDED: option C** - `LevelRules.RANDOM_EDGES=true`,
  `AVOID_DUPLICATE_TILES=true`. Verified on API 37: demo solved levels 1-9 (19/22/31/37/46/59/62/81/88 s) with random boards;
  DemoSolver unit test now runs on random boards up to level 50.
- Tests: 20 in uTurn.

### Checkpoint 9 (themes + Settings dialog)
- `Palettes` (pure, tested): SOFT (default; calm pastel edges, warm off-white background, slate outside edge) and VIVID
  (original saturated colours, white background; high-contrast option). Unit test enforces every pair of edge colours is
  >60 apart in RGB (it caught slate-vs-teal and peach-vs-pink clashes) and that every theme has >= MAX_COLOURS+1 entries.
- Settings menu item now opens a single-choice "Colour theme" dialog; choice stored in prefs `theme`. Verified on API 37:
  switch to Vivid, force-stop + relaunch keeps Vivid, switch back keeps the same board.
- `TilePrintableAndroid.outlineColor` is set per frame from the theme. Tests: 24 in uTurn.

### Checkpoint 10 (background music)
- Music is GENERATED IN CODE (`MusicSynth`: 8-bar loops, pad + arpeggio + bass + drums per mood) so there are no audio files
  and no licences to track. Moods: Soothing (58 bpm, no drums), Focus (84 bpm), Intense (132 bpm, kick + driving bass).
  Tests: audible, unclipped, 12-40 s, silent loop seam, busier = more onsets/s (1.1 / 2.4 / 6.7), repeatable.
- `MusicPlayer` (AudioTrack MODE_STATIC looped, rendered on a background thread, cached per mood; audio focus: pauses on loss,
  ducks on transient-duck). Activity: onResume/onPause/onDestroy. Settings dialog -> Colour theme / Background music
  (Off default, pref `music`). Verified on API 37 by log: plays when visible, stops on Home, resumes, setting survives restart.
- NOT verified by ear (emulator is `-no-audio`). Local-only previews to listen to on a PC: `music-preview/*.wav` (excluded from git).
- Tests: 29 in uTurn.

### Checkpoint 11 (undo / redo / hints)
- `MoveHistory` (pure): undo/redo stacks of SWAP(tileA,tileB) and ROTATE(tile,turns); a new move forgets the redo stack;
  cleared on new board/level and when demo starts (demo moves are not undoable, and undo is only valid in strict order).
- `HintFinder` (pure) + `DemoSolver.solve(..., cellOfTile)`: the search tries each tile's current cell and orientation first,
  so correct placements are kept; the hint is the first tile (corners -> edges -> inner) that is not right, as
  fromCell -> toCell plus clockwise turns. Look-alike tiles are handled by comparing colours, not tile ids.
  Simulation test: following hints solves scrambled boards from 3x3 to 8x6 within 2n moves.
- UI: menu items Undo / Redo / Hint (enabled only when the game is idle - not during demo, drag or level-complete pause).
  Hint = orange outline on the tile + dashed outline on the target cell for 5 s, toast text, cleared by touch.
  `hintsUsed` counter (per level) is logged and ready for star ratings.
- Verified on API 37: undo x2 restores the board pixel-exactly, redo replays; solving level 1 by ONLY pressing Hint and doing the
  gestures it describes took 9 hints and completed the level.
- Tests: 35 in uTurn.

### Checkpoint 12 (level select / stars)
- `LevelProgress` (pure): best stars per level, stored as "3,0,2" in pref `stars`; `starsFor(hints, demoUsed)`:
  0 hints = 3, 1-2 = 2, 3+ or demo = 1; only improvements kept; `highestUnlocked(current)` = last completed + 1 (or the current
  level, so players saved before stars existed keep access).
- UI: menu Levels opens a single-choice list "Level n ★★☆ cols x rows" up to the highest unlocked; picking one calls
  `startLevel` (cancels pending advance/demo, fresh board, saves). Level-complete toast shows the stars. `demoUsed` per level.
- Verified on API 37: demo-solved level 1 -> "1 stars", dialog shows ★☆☆ and Level 2 unlocked.
- Tests: 39 in uTurn.

### Checkpoint 13 (patterns theme)
- `Palettes.PATTERNS` (theme 2, `COUNT` = 3): the Soft colours plus a symbol per edge colour index, drawn at each segment's centroid by
  `TilePrintableAndroid.drawSymbol` (flag `showSymbols`, set per frame from `Palettes.hasSymbols(theme)`). Symbols 1..8 = dot, square,
  diamond, plus, ring, cross, hollow square, two dots; outside edge (0) has none; symbols are upright whatever the tile's turn.
- Theme dialog has a third entry "Patterns (colour-blind friendly)". Verified on API 37 (3x3 board shows all symbols).
- Tests: 40 in uTurn.

### Checkpoint 14 (solvers off the UI thread)
- Hint and demo solves run on a single-thread `ExecutorService` (`uTurn.solver`); the result is posted back to the main thread and
  used only if `solveToken` is unchanged. `cancelSolve()` (token++) is called by board init, touch down, undo/redo and the Demo menu
  item, so a stale plan never reaches a changed board. `solving` disables the menu items (`gameIdle`) while a solve is in flight.
  The inputs (colours, cells) are snapshotted on the UI thread; `onDestroy` shuts the executor down.
- Verified on API 37: hint-only solve of level 2 (9 hints) and demo solve of level 3 both completed, no crash in logcat.
- Tests: 40 in uTurn (the threading is Activity code, covered by the device run only).

### Checkpoint 15 (Crystals floating mode, from the user's screenshot)
- The user ran floating mode (Large + Floating, ~2600 iterations, 230 locks): tiles were stacked on each other in a big clump, so
  the checkpoint 2 overlap fix was not enough. Cause: `resolveTileOverlaps` made only 4 push passes, which cannot settle a dense clump.
  Reproduced headlessly (240-300 tiles, most locked, packed into one clump: hundreds of overlapping pairs left).
- Fix in Crystals `GraphPanel` (commit after 96a81ed; NOT under crystalscommon, so the jar is unchanged): push up to 60 passes and stop when
  a pass finds nothing; after 20 passes everyone yields (a weak tile can be trapped between stronger ones); clamp every tile
  to the panel; finally `relocateOverlappingTiles` moves the weaker tile of any pair still overlapping to the nearest free spot.
  Test `denseClumpOfMostlyLockedTilesEndsUpWithoutOverlaps` (240 tiles) passes in ~0.2 s. At 300 tiles in the same clump (57% of the
  panel area) a few overlaps remain: the free-spot search is lattice based.
- User re-ran it (Large + Floating, 2623 iterations, 240 locks): NO overlapping tiles any more, so the overlap bug is closed.
  Observed instead: the tiles are spread over the whole panel as many small groups with gaps, not one growing crystal, and a few
  tiles are drawn on top of the Iterations / Locks text at the bottom right. Not yet investigated; possible causes are the
  relocate step flinging tiles away from their wire-matched spots and the wire pull being too weak to bring tiles back together.

### Checkpoint 16 (Crystals floating mode: why tiles scattered, and corner/edge bias)
- Headless harness (`FloatingLayoutDiagnosticsTest`, Crystals): Crystal is an Applet so it is allocated with `sun.misc.Unsafe` without its
  constructor, then tiles are loaded/wired like `mainInit` and `relax()` is called in a loop (works headless, ~1 ms per iteration).
- Causes of the scatter: (1) the old `relax()` placement put each new tile ~12 px off the grid and used a hand-written orientation table that
  rarely made the matching edges touch; (2) the first lock used `Tile.LOCKED` = -1, which the lock decay (`decreaseLock` removes a lock at <= 0)
  released at once, so the crystal never had an anchor; (3) my checkpoint 15 overlap resolver then pushed/relocated misplaced tiles to arbitrary
  spots; (4) it also made `relax()` 30-100 ms per iteration (a linear `isLocked` scan per tile pair per pass) - now cached, 60x faster.
- New growth (`GraphPanel.seedCrystal/growCrystal`): playing area = ceil(sqrt(n)) columns, centred on the panel, cell = tile scale. A corner tile
  is locked into cell (0,0) (lock level n*n, not Tile.LOCKED) with greys facing top/left. Each step: random locked tile, random segment, random
  matching wire; the tile at the far end goes into the adjacent cell, oriented so the two matching segments touch (segment s of a tile with
  orientation o faces world side (s+o)%4), only when the cell is inside the square, holds no locked tile, and the tile fits (greys face outside
  only: corner tiles fit corners, edge tiles the border). Tiles are not placed over locked tiles, so locked tiles are always on the grid.
- Bias (`biasTowardsBorder`): unlocked corner tiles drift to a corner and edge tiles to a side (chosen by `id % 4`, so the 4 corner tiles and
  the 56 edge tiles are shared out evenly), grey edges turned outwards; the pull stops within 2 tiles of the target so it never fights the overlap
  resolver. Interior tiles are left alone. The "everyone yields after 20 passes" rule from checkpoint 15 was removed.
- Result over 6 headless runs (3000 iterations, 256 tiles): one connected crystal of 140-160 locked tiles, 0 overlaps, 3-4 of 4 corner tiles and
  55-56 of 56 edge tiles at the border; ~60% of touching locked pairs match on both sides. By 12000 iterations ~200 tiles are locked.
  Rendered PNGs checked by eye: a tidy grid growing from the top-left corner with border tiles hovering around it.
- Not under crystalscommon, so `CrystalsCommon.jar` is unchanged. Crystals tests: 84 (45 failing are the same old stubs).
- Still to do: the user should re-run `./gradlew run` in Crystals and compare. Ideas: interior tiles could also drift into the square; the
  crystal grows slowly late on (locks decay); tiles drawn over the Iterations/Locks text.

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
