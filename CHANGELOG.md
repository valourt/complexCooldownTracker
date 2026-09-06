# Changelog

All notable changes to Cooldown Tracker. Check your running version with
`/cooldowns version` in-game.

**Versioning scheme (from 1.1.0 onward):** MAJOR.MINOR.PATCH, where PATCH
counts 1 through 9; the update after 1.1.9 is 1.2.0 (not 1.1.10), and so on.
Everything before this point was numbered 1.0.0 through 2.5.0 under the old
scheme - this file keeps those entries as-is for history, but 1.1.0 below
picks up exactly where 2.5.0 left off (same code, renumbered).

## 1.2.3

- **Fixed the actual bug**: `ArmorEffectTracker` only checked the equipped
  helmet/boots' own display name for "Snake Eyes"/"Mood Swings" - but like
  the rune abilities tracked elsewhere in this mod, those names live in the
  item's lore, not its display name. Diagnostic logs confirmed both
  trackers were parsing chat/action-bar messages perfectly the whole time
  (buff and mood updated correctly with every roll/switch) - the equip
  check was just never returning true, so the boxes stayed hidden during
  actual gameplay (they only appeared correctly in the HUD editor, which
  shows every box regardless of equip status). Now checks lore lines too,
  same as inventory ownership matching does.
- Also made the tracked buff/mood fields `volatile`, since they're written
  from the network thread and read from the render thread - a correctness
  fix for thread visibility, independent of the bug above.
- Removed the diagnostic logging added to track this down (1.2.1/1.2.2
  were diagnostic-only builds) - all of it would have fired on every chat
  line/action-bar update/render frame indefinitely, which is exactly the
  kind of leftover debug logging that's caused real performance problems
  earlier in this project.

## 1.2.0

- Added **Snake Eyes** box: shows the currently-active buff (e.g. "Weakness
  III (7s)"), parsed directly from the chat message the item announces its
  roll with - reuses the existing proven chat pipeline, no new detection
  mechanism needed. Auto-hides unless you're wearing the Snake Eyes helmet.
- Added **Mood Swings** box: shows your current mood (Aggressive/Playful/
  Lazy), color-coded per mood. This one required a genuinely new capability
  - Mood Swings announces via the action bar (the text above your hotbar),
  which is a different network message than regular chat
  (`OverlayMessageS2CPacket` vs `GameMessageS2CPacket`). Added a third
  injection into the same proven-working mixin class to catch it. Auto-
  hides unless you're wearing the Mood Swings boots.
- Both fully integrated: draggable/resizable in `/cooldowns hud`,
  toggleable via `/cooldowns togglesnakeeyes` / `/cooldowns togglemoodswings`
  or Settings, scale via `/cooldowns setscale snake|mood <50-300>`.

## 1.1.9

- Totem watch label: raised further above the player's head (was
  overlapping cosmetic wings/backpacks on tall accessories), switched to
  solid white text with a darker backdrop for contrast against busy
  backgrounds - the previous pale yellow-green was hard to read and,
  worth clarifying, was never tied to your own totem's custom color
  setting (that only affects your own boxes, not this label).

## 1.1.8

- Added an **in-world floating label** above any player currently on totem
  cooldown, showing their remaining time - billboarded to always face you,
  same technique many waypoint/marker mods use for in-world text. This is
  a genuinely new category of code for this mod (full 3D world-space
  rendering with camera-relative positioning), rather than the flat 2D HUD
  overlays everything else uses, so it carries real first-build risk -
  more so than the texture/font work did.

## 1.1.7

- **Custom colors now only apply to the item name** - the countdown suffix
  (e.g. the "1:23" part) always follows the automatic urgency coloring
  (white -> amber -> red) regardless of whether that item has a custom
  color set, instead of the whole line adopting the custom color.
- **Added Totem Watch**: a new box tracking OTHER players' totem-of-undying
  activations and remaining cooldown, using the same vanilla entity-status
  packet that triggers the totem-pop effect for anyone nearby (not just
  you) - this is the same proven-working injection point as chat detection,
  just watching a different packet type on it. The cooldown length applied
  is whatever you've configured for your own "totem" entry, on the
  assumption the server enforces one shared duration for everyone.
  Draggable/resizable/toggleable exactly like the other boxes
  (`/cooldowns toggletotemwatch`, `/cooldowns setscale totem <50-300>`, or
  via Settings).
  In-world highlighting (e.g. a glow on the actual player model) would be a
  much bigger, riskier feature - this list-based version uses the same
  low-risk approach as everything else in the mod. Worth trying this first
  and seeing if a highlight is still wanted afterward.

## 1.1.6

- Backpack box now auto-hides entirely in-game when nothing's equipped in
  your offhand, instead of showing a "—" placeholder. It still shows (with
  the placeholder) in the HUD editor, so you can position it ahead of time
  without needing a backpack equipped.

## 1.1.5

- Added a **Backpack** box showing your currently-equipped backpack's
  capacity (e.g. "28 / 500"), read directly from the item's own name while
  it's in your offhand - matches this server's "hold in offhand to equip"
  convention, and is far more reliable than trying to catch the fleeting
  pickup notification, since the name is always readable. Turns amber then
  red as it fills up, same urgency coloring as cooldowns.
- Fully integrated with the existing HUD tools: draggable/resizable in
  `/cooldowns hud`, toggleable via `/cooldowns togglebackpackbox` or the
  Settings screen, uses the same rounded panel and Inter font as everything
  else, and its scale is settable via `/cooldowns setscale backpack <50-300>`.

## 1.1.4

- **Fixed**: denial messages other than "on cooldown" - "You do not have a
  target.", "This may only be used in combat.", and presumably others we
  haven't seen yet - still contain the item name and were wrongly starting
  the cooldown, since only the specific "on cooldown" phrase was filtered.
  Flipped the logic: every genuine success message on this server includes
  a checkmark symbol (✔/✅/✓) right after the item name, so a cooldown now
  only starts when one of those is present, rather than trying to blacklist
  every possible denial phrasing individually.
- **Fixed**: setting background opacity to 0% still showed a faint shadow
  rectangle, so "clean text only" was never actually achievable. 0% now
  skips the panel and its shadow entirely. Added a one-click "Text Only"
  button in the Settings screen alongside the opacity field.

## 1.1.3

- **Fixed**: other players' public ability-use broadcasts (e.g. "Lightning
  Crash I | BaconEggAnCheese summoned 5 lightning bolts!") were wrongly
  starting your own cooldown for that item, since matching was purely
  text-based and didn't check who actually triggered it. Now cross-checks
  each chat line against the current online player list (from the tab
  list) - a line mentioning another online player by name is treated as
  their activation, not yours, and skipped before item matching.

## 1.1.2

- **Fixed totem detection reliability**: the previous check required health
  hitting exactly 1.0 AND absorption hitting exactly 8.0 in the *same*
  client tick. Vanilla applies these through separate update paths that
  don't always land in the same tick under normal network jitter, so a
  real totem pop could be missed entirely if the two signals arrived even
  one tick apart. Now fires on whichever signal arrives first, with a
  short debounce so the second one (when it does land nearby) doesn't
  double-count the same activation.

## 1.1.1

- **Fixed**: `/cooldowns additem` or `/cooldowns addrune` naming/IDing
  something "golden apple" or "totem" would silently overwrite the
  built-in game-detected entry with a chat-trigger one - since those two
  (and enchanted golden apple) never send a chat message, this made them
  stop working with no visible error. All three add paths (additem,
  addrune, importcsv) now refuse to touch these three reserved ids.
- Added `/cooldowns fixbuiltins` to repair any of the three built-in
  entries that were already broken this way - restores the correct
  trigger type while keeping whatever cooldownSeconds/color you'd set.

## 1.1.0 (was 2.5.0) - Full performance audit


Went through every hot path in the mod, not just chat - the inventory
scanner had never received the same optimization chat matching got a few
versions back, and was very likely the real remaining cause of lag on
chat-heavy commands (or just general play):

- **Inventory scanning now skips entirely when nothing changed.** Every
  0.5 seconds, it was re-parsing lore JSON for every held/worn item AND
  checking all 500+ tracked items against the result, unconditionally -
  even when you hadn't touched your inventory. A cheap fingerprint (item
  identity + count + NBT hash per slot) now short-circuits the whole scan
  when nothing's different from last time, which is the vast majority of
  moments during normal play.
- **Inventory matching switched from regex to the same fast substring scan
  chat matching already uses** - this alone was up to ~500 items × ~150
  inventory name/lore strings = ~75,000 regex evaluations every single
  scan. Extracted the matching logic (`TextMatch`) into one shared utility
  so both chat and inventory checks use the identical, cheaper path
  instead of two separate implementations drifting apart.

## 2.4.0

- **Reverted to two separate boxes** (Ready, On Cooldown) - the unified
  single-box grid from 2.2.0 didn't work out. Both boxes keep everything
  from the recent redesign: rounded panels, Inter font, multi-column
  wrapping, per-item colors.
- **Fixed a real bug causing the ready pop-up to show twice**: the code
  updated its internal "what's still active" tracking using a snapshot
  taken *before* that tick's cleanup ran, which still included the item
  that had just expired. The next tick, that same item looked like it had
  "just expired" again, firing a second toast for the same event.
- **Added an in-game Settings screen** (opened via a button in
  `/cooldowns hud`, or directly via `/cooldowns settings`): toggle each
  box, toggle the ready pop-up, and set background color/opacity, all
  without typing commands. Per-item settings (colors, bulk import) still
  use chat commands - a proper picker for 500+ possible items is a bigger
  feature on its own.
- The ready pop-up's position is now itself draggable in the HUD editor
  (shown as a sample "Example is ready!" preview) instead of being fixed
  to auto-center at the top of the screen.
- Commands reshuffled to match: `togglereadybox`, `togglecooldownbox`,
  `togglepopup` replace `togglehud`; `setscale` now takes a target
  (`ready` or `cooldown`) since each box scales independently again.
- **Note**: another `hud_layout.json` structure change - reposition once
  via `/cooldowns hud` after updating.

## 2.3.0

- **Swapped the font**: Roboto out, Inter in (SIL Open Font License) - Inter
  is specifically designed for UI/screen legibility at small sizes, rather
  than Roboto's general body-text design. Also bumped oversample from 2 to
  4 for sharper rendering at HUD scale.
- **Reduced the corner rounding** on the panel texture (corner radius
  20px -> 8px) - the previous version was too aggressively "pill"-shaped.
- **Polished the ready pop-up**: now uses the same rounded panel, a green
  marker square matching the HUD's style, bold text, and better-centered
  padding instead of a plain flat rectangle.

## 2.2.0 - Unified grid + real rounded panel

- **Merged into one unified list**: no more separate Ready/On Cooldown
  sections - every owned rune shows in one alphabetically-sorted grid, up
  to 5 rows per column, wrapping into a new column (not a new row) once a
  column fills up. An item's alphabetical slot stays roughly stable as it
  flips between ready and on-cooldown, rather than the whole layout
  reshuffling by remaining time.
- **Genuinely rounded background**: previous versions faked "soft corners"
  by not drawing a couple of pixels at each corner of a flat rectangle,
  which still reads as blocky up close. This bundles an actual anti-aliased
  rounded-rect PNG (generated at 4x supersampling) and draws it as a
  9-slice, so corners are truly curved regardless of box size.
- **Configurable background**: `/cooldowns setbgcolor <hex>` and
  `/cooldowns setopacity <0-100>` control the panel's color/transparency
  independently, persisted the same way as position/scale.
- Ready items now default to pure white (`FFFFFF`) instead of a slightly
  tinted off-white, unless you've set a custom color for that item via
  `/cooldowns setcolor` - which now also applies while the item is ready,
  not just while it's on cooldown.
- **Note**: another `hud_layout.json` structure change - reposition once
  via `/cooldowns hud` after updating.

## 2.1.0

- **Real font**: the HUD now renders with Roboto (a proper TrueType font,
  Apache 2.0 licensed, bundled directly in the mod jar) instead of
  Minecraft's built-in blocky bitmap font. Headers use the bold weight,
  body/countdown text uses regular - this is genuine anti-aliased font
  rendering via Minecraft's own resource-pack font system, not a visual
  trick.
- Fixed the lopsided layout when one column is empty and the other isn't
  (e.g. nothing on cooldown but several things ready) - an empty column now
  shows a muted "None" placeholder instead of a blank gap under its header.

## 2.0.0 - Merged single-box redesign

- **Structural change**: replaced the two separate boxes with one merged
  panel - Ready items on the left, On Cooldown items on the right (sorted
  soonest-ready first). Using an item visibly moves it from the left column
  to the right as it starts cooling down, and back again once it expires.
- **Tier hidden from display**: item names now show without their trailing
  Roman numeral (e.g. "Illusion" instead of "Illusion V") - the underlying
  id/pattern/tier data is untouched, this only affects what's drawn on
  screen. Cooldown entries read like "Illusion 1:05" now (no colon).
- Replaced the two separate toggle commands/keybinds with one:
  `/cooldowns togglehud` (was `togglecooldown`/`toggleready`).
- Replaced the two separate resize scales with one shared scale, still
  adjustable by scrolling in `/cooldowns hud`, or now also via
  `/cooldowns setscale <50-300>` for a precise value without opening the
  editor.
- **Note**: this changes `hud_layout.json`'s structure - your saved box
  position/scale from before this version will reset to the default corner
  on first launch. You'll need to reposition once via `/cooldowns hud`.

## 1.9.0

- Actually implemented a proper smooth gradient background (the 1.8.0
  "polish" was still flat color under the hood) - computed row-by-row so
  the soft corners don't introduce a seam the way overlapping gradient
  rects would have.
- Empty boxes ("None on cooldown" / "None ready") now collapse to a
  compact single line instead of a full block of dead space.
- Added a small color-coded marker before each line in "On Cooldown",
  reading more like a designed list than plain colored text.
- Refined the color palette (deeper charcoal background, warmer accent
  hues).

## 1.8.0

- Added visibility toggles: both boxes can now be shown/hidden independently
  via `/cooldowns togglecooldown` / `/cooldowns toggleready`, or by binding
  the matching keybinds (Options > Controls > Key Binds > Cooldown
  Tracker - both unbound by default).
- Added per-item custom colors: `/cooldowns setcolor <id> <hex>` (e.g.
  `FF8800`), or a `color` column in your CSV. A custom color always
  overrides the automatic urgency coloring for that item, so you can tell
  abilities apart by identity rather than by how soon they're ready.
- Visual refresh: soft-cornered panels (previously hard rectangles), a
  cleaner header/body divider line, and general spacing polish.

## 1.7.0

- **The actual fix for `/f who`-style lag**: even with patterns compiled
  once (1.6.0), checking ~500 compiled regexes against every line of a
  large chat dump still has real overhead - the regex engine isn't built
  for "check this same short literal text against 500 patterns, 100 times
  in a row." Auto-generated items (from CSV import or `/cooldowns additem`)
  now use a hand-written substring scan instead, which is dramatically
  cheaper at this scale. Hand-written regex via `/cooldowns addrune` still
  uses the full regex engine (unavoidable - it's genuine regex).
  **Re-run `/cooldowns importcsv` after updating** so your existing 500+
  items get the fast-path field - this doesn't apply retroactively to
  entries already in cooldowns.json from an older import.

## 1.6.0

- **Performance overhaul**: chat/inventory regex patterns are now compiled
  once when the config loads instead of recompiled on every single check -
  this was the root cause of severe stutter on chat-heavy commands (e.g.
  `/f who`) with 500+ tracked items.
- Removed a leftover debug `System.out.println` that fired on every chat
  line - a major contributor to that same stutter, since some launcher
  consoles re-render per printed line.
- Removed dead code from earlier chat-detection experiments (`ChatHudAccessor`,
  `ChatPoller`, `DebugTickMixin`) that were confirmed non-functional in this
  environment but never cleaned up.
- HUD rendering was rebuilding both boxes' full contents on every rendered
  frame (hundreds of times/sec at high FPS) - now throttled to 5x/sec,
  since the countdown display only ever needs whole-second precision.
- Chat matching now iterates a pre-filtered list of only chat-triggered
  items, instead of checking every single tracked item's trigger type on
  every chat line.
- `ReadyToastManager` no longer allocates a snapshot map every tick when
  nothing is on cooldown (the common case).
- `/cooldowns list` no longer dumps every tracked item into chat (would be
  500+ messages) - now shows a count by default, or
  `/cooldowns list <search>` to filter.
- Added `/cooldowns version` and this changelog.

## 1.5.0

- Fixed: chat lines like "Vine Swing III | On cooldown: 1 minute 17 seconds"
  (sent when you try to use something already on cooldown) were matching
  the same pattern as a genuine "used it" message and wrongly resetting the
  timer back to full. Any line containing "on cooldown" is now ignored
  before pattern matching.
- Fixed a `NullPointerException` crash on launch caused by a stray null
  entry in `cooldowns.json` (e.g. from a trailing comma when hand-editing
  the file) - malformed entries are now skipped instead of crashing.

## 1.4.0

- Added HUD box resizing: scroll while hovering over a box in the HUD
  editor (`/cooldowns hud`) to resize it independently, 50%-300%.
- Added color-coded urgency on cooldown entries (white -> amber under 10s
  -> red under 3s).
- Added distinct accent colors per box (orange for "On Cooldown", green for
  "Ready") plus a drop shadow and header divider for visual polish.
- Added a "ready" pop-up toast: a brief on-screen notification when an item
  comes off cooldown, for extra feedback beyond the HUD boxes.

## 1.3.x - Chat detection rewrite

- Root-caused why chat-based detection wasn't working at all: this client
  distribution silently drops mixin callbacks (`@Inject`) into `ChatHud`
  and `PlayerEntity`, and also doesn't populate `ChatHud`'s internal message
  list even when read via a safe Accessor mixin.
- Replaced the mixin-based totem/apple detection with tick-based polling of
  public player state (health/absorption values for totems, item-use
  transitions for apples) - no mixin required, confirmed working.
- Replaced chat detection with a mixin at the network-packet layer
  (`ClientPlayNetworkHandler.onGameMessage`) instead of the display layer
  (`ChatHud`) - this is the one that actually works in this environment.
- Fixed a regex bug where e.g. "Green Shell I" would also match inside
  "Green Shell II"/"III" (Roman numerals share prefixes) - patterns now use
  word boundaries. Applied to both chat matching and inventory-ownership
  matching.
- Fixed inventory-ownership matching to also check item lore lines, not
  just the item's own display name - some servers show the custom ability
  name in lore rather than as the item's actual name.
- Fixed inventory-ownership matching to strip icon/emoji glyphs before
  comparing names.

## 1.2.0

- Added `itemName` field per tracked item and inventory-ownership scanning,
  so the "Ready" box only shows items you actually currently hold.
- Simplified bulk CSV import to only require `itemName` + `cooldownSeconds`
  per row (id/displayName/pattern auto-generated from the name) - no
  spreadsheet software required.
- Added `/cooldowns additem` for adding a single item by name with no
  manual regex writing.

## 1.1.0 (original numbering scheme - unrelated to the 1.1.0 above)

- Added draggable, positioned HUD boxes ("On Cooldown" / "Ready") via
  `/cooldowns hud`, persisted across sessions.
- Added `/cooldowns importcsv` for bulk-importing many items at once.

## 1.0.0

- Initial release: tracks Halloween Axe-style rune cooldowns via chat
  message matching, plus built-in totem/golden apple/enchanted golden apple
  detection, with a basic on-screen countdown display.
