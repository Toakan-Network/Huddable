# Huddable roadmap

The goal is MMO-style UI building in Minecraft, like ElvUI or NaowhUI: players lay out and theme their whole HUD
in game. v1 is the base: wrapping and moving the vanilla elements.

## Done (0.1.0)
- Every vanilla HUD element wrapped through Fabric's `HudElementRegistry` (no mixins): move, scale, hide.
- Health, hunger, armor and air styles: Bar, Number, Icon + number, with colours.
- Editor screen from Mod Menu: drag, snap, corner scale, right-click options panel, title-screen placeholder mode.
- 0.1.1: the controls are a draggable window (they covered the boss bar), plus an Elements list to show or hide
  every element.
- Profiles with switch, new, rename, delete, and `HUD1:` clipboard export/import.
- Builds for 26.2 and 26.3; the dev client starts and registers every wrapper without errors.

## Untested in game
- The whole editor: dragging, snapping, scaling, the options panel, and the profile buttons.
- Element boxes against what vanilla draws, especially health with extra heart rows, air while riding, effects
  and the scoreboard.
- The bar styles' visibility (armor at 0, air surfacing, hunger while riding).
- 26.3 in a real client.

## Next (ElvUI direction)
- Custom widgets: text, bars and icons tied to live data (coordinates, FPS, durability, clock, ping).
- Unit frames: a player frame (health, hunger, armor and XP in one panel) and a target frame for the entity
  under the crosshair.
- A split hotbar: rows or columns, set per slot.
- A global theme: one font, border, background and colour scheme for every element and widget.
- Full UI presets (NaowhUI style) shipped with the mod and shareable as codes.
- Keyboard nudging in the editor (arrow keys, read through `HuddableClient.inputCode`).
- Moving whole container windows: `AbstractContainerScreen` has `protected int leftPos, topPos` in both 26.2 and
  26.3 (`..\.mc-sources\<mc>\net\minecraft\client\gui\screens\inventory\AbstractContainerScreen.java:49-50`);
  slots are placed relative to them, so shifting both after `init` moves the window and keeps clicks lined up.
