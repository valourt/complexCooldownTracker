# Cooldown Tracker (Fabric, 1.18.2)

Current version: **1.2.3** — check `/cooldowns version` in-game to confirm
which build you're actually running, and see `CHANGELOG.md` for what
changed in each version.

A client-side mod that tracks item cooldowns on **Complex Gaming Factions**
and shows a countdown HUD in the top-left corner. Built for:

- **Runes** (e.g. your Tier 2 Halloween Axe) — detected from the chat message
  the server sends when you use one, matched against a regex you configure.
- **Totem of Undying** — detected directly from the game's totem-pop
  animation trigger (a core vanilla mechanic, not dependent on the server).
- **Golden Apple / Enchanted Golden Apple** — detected the moment you finish
  eating one (also a direct game-event hook, not a chat message).

Because it's a *client* mod, it only needs to be installed on your own
computer — nothing needs to change on the server side.

## 1. Building it

This project is already merged with the official Fabric 1.18.2 template (the
one from https://github.com/FabricMC/fabric-example-mod, `1.18.2` branch),
so the Gradle wrapper is included — you don't need to combine anything
yourself.

You'll need Java 17 and about 10 minutes for the first build (it downloads
Minecraft/mappings/Fabric API).

1. Unzip this project.
2. In a terminal, inside the unzipped folder, run:
   - Windows: `gradlew.bat build`
   - Mac/Linux: `./gradlew build`
3. The finished mod jar appears at `build/libs/cooldown-tracker-1.0.0.jar`.

If `yarn_mappings` in `gradle.properties` ever fails to resolve (Fabric
occasionally retires old mapping builds), check
https://fabricmc.net/develop for the current 1.18.2 Yarn build number and
update that one line.

## 2. Installing it

1. Install **Fabric Loader** for Minecraft 1.18.2 (via the Fabric installer,
   or through your launcher of choice).
2. Download **Fabric API** for 1.18.2 and drop the jar into
   `.minecraft/mods/`.
3. Drop `cooldown-tracker-1.0.0.jar` into `.minecraft/mods/` too.
4. Launch the Fabric 1.18.2 profile and join Complex Gaming Factions.

## 3. Moving the HUD boxes

There are six boxes:
- **Ready** — every tracked item you currently own that isn't on cooldown,
  in white (or a custom color you've set), up to 5 rows per column,
  wrapping into a new column as needed.
- **On Cooldown** — everything currently counting down, soonest-ready
  first. The countdown itself always follows the automatic urgency
  coloring (white -> amber -> red as it gets close); a custom color you've
  set only tints the item's name, not the countdown. Tier is hidden from
  display (shows "Illusion" rather than "Illusion V") though it's still
  tracked underneath.
- **Backpack** — the capacity of whatever backpack you currently have
  equipped (held in your offhand), read straight from its item name, e.g.
  "28 / 500". Turns amber then red as it fills up. Auto-hides when
  nothing's equipped there.
- **Totem Watch** — other players' totem-of-undying cooldowns, detected
  the moment they pop one (via the same game mechanic that plays the totem
  particle effect for everyone nearby, not just them). Assumes everyone on
  the server shares the same totem cooldown length as the one you've
  configured for your own "totem" entry. Also shows a floating label above
  their head in-world while their cooldown is active.
- **Snake Eyes** — your currently-active rolled buff (e.g. "Weakness III
  (7s)"), parsed from the chat message it announces. Auto-hides unless
  you're wearing the Snake Eyes helmet; shows "Rolling…" between rolls.
- **Mood Swings** — your current mood (Aggressive/Playful/Lazy),
  color-coded per mood. Auto-hides unless you're wearing the Mood Swings
  boots.

Each starts in the top-left corner. To reposition/resize/hide them, or
restyle the background, or move the ready pop-up: run `/cooldowns hud`
(or bind the "Open Cooldown HUD Editor" key under Options > Controls >
Key Binds > Cooldown Tracker — it's unbound by default). In that screen:
1. Click and drag any box, or the green sample pop-up, to move it.
2. Scroll while hovering over a box to resize it (50%–300% each, resized
   independently) — the pop-up only moves, it doesn't resize.
3. Click **Settings** for toggles and background color/opacity, without
   typing commands.
4. Press Esc to save and close. Everything persists in
   `config/cooldowntracker/hud_layout.json` between sessions.

You can also open Settings directly with `/cooldowns settings`, or use the
matching chat commands (see the list below) if you prefer typing.

## 4. Configuring cooldowns

On first launch, the mod writes
`.minecraft/config/cooldowntracker/cooldowns.json`. Example:

```json
[
  {
    "id": "halloween_axe_t2",
    "displayName": "Halloween Axe [T2]",
    "trigger": "chat",
    "pattern": "(?i).*halloween axe.*",
    "itemName": "Halloween Axe II",
    "cooldownSeconds": 220,
    "enabled": true
  },
  {
    "id": "totem",
    "displayName": "Totem of Undying",
    "trigger": "totem",
    "pattern": null,
    "itemName": null,
    "cooldownSeconds": 0,
    "enabled": true
  }
]
```

**`itemName` is important**: it's the item's *exact* in-game display name
(hover over it — copy the name character-for-character). It's how the mod
checks your inventory to decide what belongs in the "Ready" box — a `chat`
trigger item with no `itemName` set will never show as "Ready" (it'll still
show in "On Cooldown" once used, just never as ready-to-use). `totem`,
`golden_apple`, and `enchanted_golden_apple` don't need `itemName` — those
are matched by their real Minecraft item type automatically.

**`itemName` is important**: it's the item's *exact* in-game display name
(hover over it — copy the name character-for-character, no need to include
the season or anything else, just whatever the item is actually called).
Matching strips out icon glyphs and symbols automatically (Complex Gaming
prefixes item names with a small icon), and matches as long as your text
appears somewhere in the cleaned-up name — so "Green Shell V" matches even
though the real name is "🐚 Green Shell V". It's how the mod checks your
inventory to decide what belongs in the "Ready" box — a `chat` trigger item
with no `itemName` set will never show as "Ready" (it'll still show in
"On Cooldown" once used, just never as ready-to-use). `totem`,
`golden_apple`, and `enchanted_golden_apple` don't need `itemName` — those
are matched by their real Minecraft item type automatically.

**Important - things you need to fill in yourself:**

- **`pattern` for the axe/rune entries**: I don't know the exact wording of
  Complex Gaming Factions' chat message. Use it once, copy the exact line
  from your chat log, and tighten the regex to match it (a loose
  `.*halloween axe.*` works fine as a starting point — it just means "the
  line contains this text somewhere", case-insensitively).
- **`itemName` for the axe/rune entries**: must match the real in-game name
  exactly (case doesn't matter, but spelling/spacing does).
- **`cooldownSeconds` for `totem`, `golden_apple`, `enchanted_golden_apple`**:
  I don't know what cooldowns (if any) that server enforces for these — set
  them to the real values. If totems have no cooldown on that server, just
  leave it at `0` (the HUD won't show an entry for a 0-second cooldown).

### Adding a single item quickly

In-game, the simplest option:

```
/cooldowns additem 180 Green Shell II
```

This adds/updates a tracked item named exactly "Green Shell II" with a
180-second cooldown. It auto-generates the id, uses the name itself as the
display name, sets `itemName` to that same name (for inventory matching),
and builds a chat pattern that just looks for that name anywhere in a chat
line. If the real chat message doesn't contain the item's name verbatim,
edit `cooldowns.json` afterwards and fix that one entry's `pattern`.

### Bulk-entering hundreds of items (no Excel needed)

A CSV is just a plain text file — comma-separated values, one item per line.
You can create/edit one in:
- **Any text editor** (Notepad, TextEdit, VS Code) — since for this you only
  need two columns, it's genuinely just typing lines like `Green Shell II,180`.
- **Google Sheets** (free, browser-based, no install) — type your data in a
  table, then File > Download > Comma Separated Values (.csv).
- **LibreOffice Calc** (free, open-source Excel alternative) — same idea,
  File > Save As > CSV.

A starter file, `cooldowns-template.csv`, is included alongside this project:

```csv
itemName,cooldownSeconds
Green Shell II,180
Halloween Axe II,220
```

Only two columns are required — `itemName` (the exact in-game name) and
`cooldownSeconds`. Everything else (id, displayName, chat-match pattern) is
generated automatically from the name. If you want to override any of those
for a specific row, you can add extra columns — `id`, `displayName`,
`trigger`, `pattern`, `enabled` — and only fill them in where you need
something different from the default; leave the cell blank otherwise.

To use it:
1. Open `cooldowns-template.csv` in whatever's convenient, add a row per
   item (`ItemName,CooldownSeconds`), save.
2. Put the file in `.minecraft/config/cooldowntracker/` as `cooldowns.csv`
   (or whatever name you like).
3. In-game: `/cooldowns importcsv` (or `/cooldowns importcsv myfile.csv`).
   It reports how many items imported and lists anything it couldn't parse.

You can re-run `importcsv` any time you update the file — it updates
existing ids rather than duplicating them (matched by the auto-generated id,
which comes from the item name, so keep the name spelled consistently across
edits).

Other commands:
- `/cooldowns version` — confirm which build you're running
- `/cooldowns list` — see how many items are tracked; `/cooldowns list <search>` to find a specific one by name
- `/cooldowns remove <id>` — delete an entry
- `/cooldowns hud` — open the drag-to-reposition HUD editor
- `/cooldowns settings` — open the settings GUI directly
- `/cooldowns togglereadybox` / `/cooldowns togglecooldownbox` / `/cooldowns togglebackpackbox` / `/cooldowns toggletotemwatch` / `/cooldowns togglesnakeeyes` / `/cooldowns togglemoodswings` / `/cooldowns togglepopup` — show/hide a box or the ready pop-up
- `/cooldowns setscale <ready|cooldown|backpack|totem|snake|mood> <50-300>` — set a box's text/size as a percentage
- `/cooldowns setcolor <id> <hex>` — give one item a fixed color (e.g. `FF8800`), overriding the automatic urgency coloring
- `/cooldowns setbgcolor <hex>` / `/cooldowns setopacity <0-100>` — restyle the HUD panels' background (`setopacity 0` gives clean text with no panel at all)
- `/cooldowns importcsv [file]` — bulk-import from a CSV (see above)
- `/cooldowns fixbuiltins` — repair totem/golden apple/enchanted golden apple if `additem`, `addrune`, or a CSV row ever overwrote one of them by using a colliding name (see note below)

**Heads up:** don't `additem`/`addrune`/CSV-import anything named "totem",
"golden apple", or "enchanted golden apple" — those three are detected
directly from the game itself, not chat, so giving them a chat-trigger
entry breaks them silently (no chat message ever arrives to match). The
mod now refuses these on the way in; if you already hit this before that
guard existed, run `/cooldowns fixbuiltins` to repair it.

## How detection works (for future tweaking)

- **Chat-based items**: a mixin taps `ChatHud.addMessage`, so it sees every
  line that appears in chat (regular chat *and* server system messages), and
  regex-matches it against every `"trigger": "chat"` entry.
- **Totem**: a mixin taps `ClientPlayNetworkHandler.onEntityStatus` and
  checks for status byte `35`, which is what the vanilla server sends to
  play the totem-pop animation — this is a core game mechanic, so it works
  the same on any server.
- **Golden/Enchanted Golden Apple**: a mixin taps
  `LivingEntity.eatFood` (as overridden in `PlayerEntity`), which fires the
  instant you finish eating, before the game applies the food's effects.

If Complex Gaming Factions ever adds a chat/action-bar message for totems or
apples, that'd actually be more reliable than the event hooks above — let me
know the exact wording if that happens and I can switch those over to the
same regex system as the runes.
