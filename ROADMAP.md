# Huddable roadmap

The goal is MMO-style UI building in Minecraft, like ElvUI or NaowhUI: players lay out and theme their whole HUD
in game. v1 is the base: wrapping and moving the vanilla elements.

## Done (0.1.0)
- Every vanilla HUD element wrapped through Fabric's `HudElementRegistry` (no mixins): move, scale, hide.
- Health, hunger, armor and air styles: Bar, Number, Icon + number, with colours.
- Editor screen from Mod Menu: drag, snap, corner scale, right-click options panel, title-screen placeholder mode.
- 0.1.2: chat can be moved and scaled, open chat and its clicks included (one mixin on
  `ChatComponent.extractRenderState`); the dev client starts with it applied.
- 0.1.1: the controls are a draggable window (they covered the boss bar), plus an Elements list to show or hide
  every element.
- Profiles with switch, new, rename, delete, and `HUD1:` clipboard export/import.
- Builds for 26.2 and 26.3; the dev client starts and registers every wrapper without errors.
- 0.2.0: other mods' Fabric HUD layers are found at client start and placed like vanilla ones (measured from
  what they draw); an opt-in API (`uk.toakan.tad.huddable.api`) for elements drawn elsewhere or that resize; edge-grip resize
  for chat, Bar-style bars and resizable API elements; per-profile screen scale (GUI / Window / Fixed, Shopping
  List's maths).

- 0.3.0: fonts. A profile font and per-element fonts from the resource packs (every text element, restyled
  bars), through a wrapper `Font` handed to vanilla's HUD code (`HudFontMixin`) and chat's line styles
  (`ChatComponentMixin`); a font list in the editor that previews each font; Mod Menu's Configure opens a hub with
  HUD Designer and General Settings (vanilla options list), whose Font quality sets every TTF font's oversample
  (`TrueTypeOversampleMixin`, default 4x, reload on leave). The dev client starts with all mixins applied.
- 0.3.3: queued saves are flushed when the game closes (Done then quitting at once kept nothing).
- 0.3.4: Font quality defaults to Pack's own (vanilla's 1x oversample); the old 4x default raised every mod's TTF
  fonts to 16x the glyph memory. A saved 4 from before is moved to Pack's own once (`settingsVersion`).
- 0.3.5: the "(not showing)" label of an undrawn mod layer comes from the lang file.
- 0.3.6: chat's size options are put back however the editor closes without Done (a disconnect too), not only
  on Cancel/Esc.
- 0.3.7: switching profiles waits for queued saves, so switching away and straight back reads the latest file.
- 0.3.8: a profile can be renamed to a change of case only ("pvp" to "PvP").
- 0.3.9: the font list is read from the resource packs once per reload, not on every editor widget rebuild
  (each toolbar drag step).
- 0.3.10: `inputCode` looks each key code up once (same helper as ChatTabs and Shopping List).
- 0.3.11: share codes import with line breaks or spaces in them (as Discord wraps long codes).
- 0.4.0: meowdding-lib overlays (SkyCubed, SkyOcean) placed one by one: each is an API part, and an optional
  mixin (`mixin.meowdding`, applied only with meowdding-lib loaded) moves its draw call inside meowdding's
  overlay loop. General Settings has an Advanced toggle to turn it off.

## Untested in game
- The whole editor: dragging, snapping, scaling, the options panel, and the profile buttons.
- Element boxes against what vanilla draws, especially health with extra heart rows, air while riding, effects
  and the scoreboard.
- The bar styles' visibility (armor at 0, air surfacing, hunger while riding).
- Moved chat: open chat at the new spot, link clicks, hover tooltips, the message-tag hover, and the chat box in
  the editor against vanilla's (width, line spacing, chat scale). Mods that draw at vanilla's chat position don't
  follow it unless they read chat's pose, as ChatTabs 0.2.0 does.
- 26.3 in a real client.
- 0.2.0: which installed mods' layers discovery finds (the log lists them), measured boxes against what they
  draw, moving and scaling them; the "(not showing)" placeholder; chat edge-resize and Cancel restoring chat's
  size; Bar edge-resize; the Size modes when the window or GUI Scale changes. The API has no user yet.
- 0.2.1: toasts moving, with vanilla toasts and with Fancy Notify (each anchor corner). The dev client starts with
  `GuiToastsMixin` applied.

- 0.3.0: every font element in game with a TTF pack (scoreboard and tab list backgrounds fitting the text,
  Hypixel icons keeping their font, chat re-wrapping and its clicks/hovers in a new font, restyled bar numbers,
  toasts), the font list paging, a profile font from a disabled pack falling back, share codes keeping fonts,
  Font quality reloading once and surviving a restart.

- 0.4.0: meowdding overlays on SkyBlock with SkyCubed: listed with stable IDs, boxes matching what they draw
  (RPG overlay, text overlays, alignment END/CENTER), moving, scaling and hiding them, the profile screen scale,
  the Advanced toggle off and on, and meowdding's own editor still working alongside.

## Next (ElvUI direction)
- Shopping List on Huddable: register its five panels through the API (resizable), hand its Move HUD to
  Huddable's editor and ignore its `sandbox_widget` layer. Waiting for Huddable on Modrinth, so Shopping List's CI
  can compile against `maven.modrinth:huddable`.
- Fancy Notify's box is its corner plus a typical stack height (read from its `general.json`); measuring what it
  draws would fit it exactly.
- Custom widgets: text, bars and icons tied to live data (coordinates, FPS, durability, clock, ping).
- Unit frames: a player frame (health, hunger, armor and XP in one panel) and a target frame for the entity
  under the crosshair.
- A split hotbar: rows or columns, set per slot.
- A global theme: border, background and colour scheme for every element and widget (the profile font, 0.3.0,
  is its first part).
- Fonts for other mods' layers: they read the font themselves, so it needs their support (an API hook).
- Full UI presets (NaowhUI style) shipped with the mod and shareable as codes.
- Keyboard nudging in the editor (arrow keys, read through `HuddableClient.inputCode`).
- Moving whole container windows: `AbstractContainerScreen` has `protected int leftPos, topPos` in both 26.2 and
  26.3 (`..\.mc-sources\<mc>\net\minecraft\client\gui\screens\inventory\AbstractContainerScreen.java:49-50`);
  slots are placed relative to them, so shifting both after `init` moves the window and keeps clicks lined up.
