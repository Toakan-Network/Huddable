# Huddable

Rearrange the vanilla Minecraft HUD in game. Drag health, hunger, the hotbar, the scoreboard and the rest where
you want them, make them bigger or smaller, hide what you don't need, and give the status bars a cleaner look.
Other mods' HUD elements can be placed the same way. No resource packs or datapacks needed. Client-side, works
on any server.

Requires Fabric API. The editor opens from [Mod Menu](https://modrinth.com/mod/modmenu)'s Configure button;
without Mod Menu a saved layout still applies, but you can't edit it.

Minecraft 26.2 and 26.3 (one jar per version).

## The editor
Open Mod Menu, find Huddable and press Configure. In a world the live HUD shows through the editor; on the title
screen each element is drawn as a labelled box.

The editor's buttons are in a small window on the left edge. Drag its title strip to move it out of the way of
whatever you're placing; it stays where you left it until the game closes.

- **Elements:** a list of every HUD element, other mods' included. Click one to hide or show it (hidden ones are
  struck through), or press Show all.
- **Size:** how the whole HUD's size follows your screen, per profile (the same three modes as Skyblock Shopping
  List):
  - **GUI** (default): follows Minecraft's GUI Scale, like vanilla.
  - **Window:** grows and shrinks with the window height.
  - **Fixed:** the same size on screen at any GUI Scale.
- **Move:** drag an element. It snaps to the screen edges and centre and to other elements' edges and centres
  (cyan guide lines); hold Shift to place it freely.
- **Scale:** drag one of an element's gold corner handles (25% to 400%). The element keeps its shape.
- **Resize:** drag one of the white grips in the middle of an edge to change the width or height; the opposite
  edge stays put. Only elements that can lay themselves out to a new size have grips: chat (this sets vanilla's
  chat width and height options, and Cancel puts them back), Bar-style status bars, and other mods' elements
  that support it.
- **Options:** right-click an element for its panel:
  - Hide or show it.
  - Scale with - and +.
  - Style (health, hunger, armor and air only): Vanilla, Bar, Number or Icon + number.
  - Colour (for the non-vanilla styles).
  - Bar width, height and text (Bar style).
  - Reset element.
- **Done** saves, **Cancel** or Esc drops your changes.

Elements: hotbar, health, armor, hunger, air, mount health, experience bar, experience level, held item name,
effects, boss bar, scoreboard, action bar, title, tab list, crosshair, subtitles, and chat. Moved or scaled
chat stays where you put it when you open it (T), and clicking links and hovering messages work there; the input
box stays at the bottom of the screen.

### Other mods' HUD elements
Elements other mods add through Fabric API's HUD registry are found when the game starts and listed under their
mod's name (for example `Some Mod: info_panel`). Move, scale or hide them like vanilla ones. Huddable measures
each one from what it draws; one that isn't drawing anything right now shows as a small "(not showing)" box
until it appears. Mods can also register their elements with Huddable directly (see below), which lets them
resize too. Positions are kept in the profile even if you remove the mod.

Mods that draw their HUD some other way (for example Fancy Notify's notifications) can't be moved until they
support Huddable.

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
- **Reset:** puts every element and the Size mode back to vanilla.

Profile actions save your current edits first. Profiles are in `config/huddable/profiles/`.

## Notes
- The locator bar's waypoint dots aren't part of Fabric's experience bar element, so they stay in place when the
  bar moves.
- Some element boxes in the editor (boss bar, tab list, title, subtitles) are typical sizes, as the real size
  depends on what's showing.
- Other mods that replace the same HUD elements may conflict; whichever wraps last decides.
- Other mods' elements are found once, when the game has started. One a mod adds later isn't listed.

## For mod authors
Elements added with Fabric's `HudElementRegistry` need nothing: Huddable wraps them. To let Huddable place an
element drawn some other way, or to let players resize one that lays itself out, register it through
`huddable.api.HuddableApi` from a `huddable.api.HuddablePlugin` entrypoint. Huddable only loads that class when it
is installed, so it can stay an optional dependency.

```json
"entrypoints": {
	"huddable": ["com.example.compat.HuddableCompat"]
}
```

```java
public final class HuddableCompat implements HuddablePlugin {
    static final Identifier PANEL = Identifier.fromNamespaceAndPath("example", "panel");

    @Override
    public void register() {
        // The unmoved box: position in GUI pixels, size in your own units before your own scale.
        HuddableApi.register(PANEL, Component.literal("Example panel"),
            HuddableApi.Spec.of((screenWidth, screenHeight) -> new HuddableApi.Box(10, 10, 120, 40))
                .withBaseScale(() -> MyHud.scale())  // optional: your own HUD scale
                .resizable(60, 20));                  // optional: edge grips, with a minimum size
        // If the element is also a Fabric HUD layer, keep Huddable from wrapping that layer as well:
        // HuddableApi.ignoreLayer(MY_FABRIC_LAYER_ID);
    }
}
```

When drawing, either wrap your normal drawing so it lands where the player put it:

```java
if (HuddableApi.push(graphics, PANEL)) {
    drawPanel(graphics);  // at the default box
    HuddableApi.pop(graphics);
}
```

or, for a resizable element, read `HuddableApi.placement(PANEL)` (top-left in GUI pixels, width and height in
your units, the scale to draw at, and whether it's hidden) and lay it out to that. `HuddableApi.openEditor(parent)`
opens the editor, for your own "Move HUD" button. Only the `huddable.api` package is meant for other mods.
