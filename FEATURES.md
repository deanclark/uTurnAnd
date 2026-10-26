# uTurn feature wishlist

Ideas likely to improve the experience and long-term engagement, grouped by theme. Within each theme the items are in
rough priority order. **Effort**: S = a day or less, M = a few days, L = a week or more.
Items already done are marked ✅.

## 1. Look and feel (first priority: the default palette is too intense)

| Feature | Why it helps | Effort |
|---|---|---|
| ✅ **Theme picker**: Soft (default) and Vivid / high contrast, in a Settings dialog. Still to add: an "earth" palette, a dark mode, a colour-blind-safe palette | The five saturated colours were tiring to look at; a pleasant default is the biggest first-impression win | M |
| **High-contrast / colour-blind-safe theme** (keep the current vivid colours here, plus patterns) | Needed for accessibility; what the current palette is good for | S |
| **Shapes or patterns instead of colours**: each edge colour becomes a distinct motif (dots, stripes, waves, triangles, leaves) | Matching by shape works for colour-blind players and looks more designed | M |
| **Picture tiles**: slice a photo or artwork into tiles; edges match by image content instead of colour (curated packs: landscapes, mosaics, patterns, the player's own photo) | Most shareable look; the player's own photo is a strong personal hook | L |
| **Tile polish**: rounded or bevelled tiles, soft shadow while dragging, snap animation on drop, a gentle glow when an edge matches | Makes every move feel good | S |
| **Landscape layout**: use the extra width (side panel for level, timer, hints) instead of a smaller centred board | Rotation already works but wastes space | M |
| **Adaptive icon** and xhdpi/xxhdpi artwork, splash screen | Required look on Android 8+; first impression in the launcher | S |

## 2. Sound and music

| Feature | Why it helps | Effort |
|---|---|---|
| ✅ **Optional background music** with moods *Soothing*, *Focus*, *Intense* and Off, generated in code (no licensed audio); pauses when minimised and respects other apps' audio. Still to add: volume slider, more tracks per mood | Sets the mood; players choose their own pace | M |
| **Licence-free sources** to check before bundling: FreePD (public domain / CC0), OpenGameArt (filter to CC0), Pixabay Music (own licence, no attribution), Kevin MacLeod / incompetech (CC BY, needs attribution on an About screen). Keep a `CREDITS` file with source, licence and link for every track | Avoids licence problems at release | S |
| **Richer effects**: a soft chime when an edge matches, a satisfying click when a tile locks, a short fanfare on level complete; separate sound and music switches | Audio feedback is a big part of "feel" | S |
| **Haptics** for rotate, swap and win (off by default for battery, optional) | Cheap tactile polish | S |

## 3. Progression and content

| Feature | Why it helps | Effort |
|---|---|---|
| ✅ **Unlimited levels** (board grows to 6x8, then fewer colours make it harder) and **progress saved** across minimise, restart and rotation | Done | - |
| **Level map / level select**: jump back to any level already reached | Lets players revisit favourites and skip ahead after a break | M |
| **Stars per level** (moves, time, hints used) and a best-score record | Gives a reason to replay | M |
| ✅ **Hints**: outline the next tile to move and where it goes (never undoes the player's correct placements). Still to add: limited free hints earned by playing, a hint counter in the results | Keeps stuck players from quitting | S |
| ✅ **Undo / redo** of moves (unlimited, per level) | Removes fear of experimenting | S |
| **Themed chapters** (every 10 levels a new picture pack, music mood and background) | Gives levels identity and a sense of a journey | M |
| **Handcrafted and special levels**: fixed-shape boards (heart, star), boards with locked tiles, hidden-picture boards that reveal as you solve | Variety beyond bigger grids | L |
| **Daily puzzle**: same board for everyone, one a day, with a streak counter | The strongest long-term retention mechanic for puzzle games | M |
| **Relax mode** (no timer, no score) and **Challenge mode** (timer, limited moves) | Serves both kinds of player | S |
| **Tutorial** on first launch: highlight a corner, show tap to rotate and drag to swap | Fewer first-session drop-offs | S |

## 4. Social and viral

| Feature | Why it helps | Effort |
|---|---|---|
| **Share a result**: image of the solved board with level and time, via the system share sheet | Organic reach; solved boards look good | S |
| **Challenge a friend**: share a board seed as a link so a friend plays the identical puzzle and compares time | Viral loop with no server needed (seed in the link) | M |
| **Leaderboards and achievements** (Google Play Games services): fastest daily solve, longest streak, levels completed | Competition and goals | M |
| **Custom boards from the player's own photo**, shared as a pack | Personal content people want to show off | L |
| **Watch the demo** as a relaxing "screensaver" and share a time-lapse clip of a solve | Marketing material for free; demo mode already exists | M |

## 5. Quality, accessibility and reach

| Feature | Why it helps | Effort |
|---|---|---|
| **Settings screen** (the menu entry is a placeholder): theme, sound, music, haptics, left-handed layout, reset progress | Home for every option above | M |
| **Accessibility**: TalkBack descriptions for tiles ("top red, right blue..."), larger touch targets, reduce-motion switch | Reaches more players; required for good store ratings | M |
| **Run the demo solver off the UI thread** and add a progress indicator on big boards | The solve can take a second or two on the biggest boards | S |
| **Pause on interruption**: stop the timer when a call arrives or the app is backgrounded | Fair timing | S |
| **Localisation** (strings are already in resources) | Wider audience | S per language |
| **Tablet and foldable layouts** and Chrome OS support | Portrait lock is already removed | M |
| **Privacy-friendly analytics and crash reporting** (opt-in) | Shows which levels players quit on | M |
| **Light monetisation that respects players**: one-off "remove ads", cosmetic theme packs, optional rewarded hints; no ads mid-move | Sustains development without annoying players | M |
| **Release engineering**: signed release build, Play Store listing with screenshots, store-listing videos from demo mode, automated tests on a Gradle managed device in CI | Needed to ship | M |

## Suggested order

1. Calm default theme + theme picker + settings screen (fixes the first impression and unlocks the rest)
2. Shapes/patterns theme (accessibility and a distinctive look)
3. Background music with three moods, plus the sound polish
4. Hints, undo, stars and level select
5. Daily puzzle, share-a-result and challenge links
6. Picture tiles and themed chapters
