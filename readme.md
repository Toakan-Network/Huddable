# Huddable

Rearrange the vanilla Minecraft HUD in game. Drag health, hunger, the hotbar, the scoreboard and the rest where
you want them, make them bigger or smaller, hide what you don't need, and give the status bars a cleaner look.
No resource packs or datapacks needed. Client-side, works on any server.

Requires Fabric API. The editor opens from [Mod Menu](https://modrinth.com/mod/modmenu)'s Configure button;
without Mod Menu a saved layout still applies, but you can't edit it.

Minecraft 26.2 and 26.3 (one jar per version).

## The editor
Open Mod Menu, find Huddable and press Configure. In a world the live HUD shows through the editor; on the title
screen each element is drawn as a labelled box.

The editor's buttons are in a small window on the left edge. Drag its title strip to move it out of the way of
whatever you're placing; it stays where you left it until the game closes.

- **Elements:** a list of every HUD element. Click one to hide or show it (hidden ones are struck through), or
  press Show all.
- **Move:** drag an element. It snaps to the screen edges and centre and to other elements' edges and centres
  (cyan guide lines); hold Shift to place it freely.
- **Scale:** drag one of an element's gold corner handles (25% to 400%).
- **Options:** right-click an element for its panel:
  - Hide or show it.
  - Scale with - and +.
  - Style (health, hunger, armor and air only): Vanilla, Bar, Number or Icon + number.
  - Colour (for the non-vanilla styles).
  - Bar width, height and text (Bar style).
  - Reset element.
- **Done** saves, **Cancel** or Esc drops your changes.

Elements: hotbar, health, armor, hunger, air, mount health, experience bar, experience level, held item name,
effects, boss bar, scoreboard, action bar, title, tab list, crosshair, subtitles, and chat (hide only: moving
chat would break clicking on it).

### Styles
- **Bar:** a filled bar with the value written on it. Health shows absorption and hunger shows saturation as a
  thin strip along the top.
- **Number:** text such as `18/20`.
- **Icon + number:** one vanilla icon and the number.

Hunger and air bars drain from the left like vanilla's icons. The styles appear only when vanilla would draw the
bar (armor only with armor on, air only underwater or while it refills).

## Profiles
The top button in the editor's window shows the active profile; click it to switch to the next one.
- **New:** an empty (vanilla) profile.
- **Rename:** type a name, then OK.
- **Delete:** press twice.
- **Export:** copies the layout to the clipboard as a code starting `HUD1:`.
- **Import:** reads a code from the clipboard into a new profile.
- **Reset:** puts every element back to vanilla.

Profile actions save your current edits first. Profiles are in `config/huddable/profiles/`.

## Notes
- The locator bar's waypoint dots aren't part of Fabric's experience bar element, so they stay in place when the
  bar moves.
- Some element boxes in the editor (boss bar, tab list, title, subtitles) are typical sizes, as the real size
  depends on what's showing.
- Other mods that replace the same HUD elements may conflict; whichever wraps last decides.
