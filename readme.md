# Huddable

Rearrange the vanilla Minecraft HUD in game. Drag health, hunger, the hotbar, the scoreboard and the rest where
you want them, make them bigger or smaller, hide what you don't need, and give the status bars a cleaner look.
Other mods' HUD elements can be placed the same way. No resource packs or datapacks needed. Client-side, works
on any server.

Requires Fabric API. Huddable's screens open from [Mod Menu](https://modrinth.com/mod/modmenu)'s Configure
button; without Mod Menu a saved layout still applies, but you can't edit it.

Minecraft 26.2 and 26.3 (one jar per version).

## Huddable's screen
Open Mod Menu, find Huddable and press Configure. It has two buttons:
- **HUD Designer:** the layout editor below, for moving, styling and choosing fonts, saved per profile.
- **General Settings:** settings for the whole game rather than one profile (see below).

## The editor (HUD Designer)
In a world the live HUD shows through the editor; on the title screen each element is drawn as a labelled box.

The editor's buttons are in a small window on the left edge. Drag its title strip to move it out of the way of
whatever you're placing; it stays where you left it until the game closes.

- **Elements:** a list of every HUD element, other mods' included. Click one to hide or show it (hidden ones are
  struck through), or press Show all.
- **Size:** how the whole HUD's size follows your screen, per profile (the same three modes as Skyblock Shopping
  List):
  - **GUI** (default): follows Minecraft's GUI Scale, like vanilla.
  - **Window:** grows and shrinks with the window height.
  - **Fixed:** the same size on screen at any GUI Scale.

  With Window and Fixed, positions move with the size too, so the layout keeps its shape when the window or GUI
  Scale changes. Switching mode in the editor keeps elements where they are and only changes their size.
- **Font:** the profile's font, picked from every font your resource packs provide (each shown in itself).
  Every element that draws text uses it unless you give that element its own (see Fonts below).
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
  - Font, for elements that draw text: Profile (the profile's font), Vanilla, or any pack font.
  - Reset element.
- **Done** saves, **Cancel** or Esc drops your changes.

Elements: hotbar, health, armor, hunger, air, mount health, experience bar, experience level, held item name,
effects, boss bar, scoreboard, action bar, title, tab list, crosshair, subtitles, toasts, and chat. Moved or scaled
chat stays where you put it when you open it (T), and clicking links and hovering messages work there; the input
box stays at the bottom of the screen.

### Other mods' HUD elements
Elements other mods add through Fabric API's HUD registry are found when the game starts and listed under their
mod's name (for example `Some Mod: info_panel`). Move, scale or hide them like vanilla ones. Huddable measures
each one from what it draws; one that isn't drawing anything right now shows as a small "(not showing)" box
until it appears. Mods can also register their elements with Huddable directly (see below), which lets them
resize too. Positions are kept in the profile even if you remove the mod.

**Toasts** (the pop-ups in the top-right corner) move, scale and hide as one element. With
[Fancy Notify](https://modrinth.com/mod/fancy-notify) installed, its notifications move with them: the box starts
at the corner set in Fancy Notify's options, and Huddable moves it from there. Hiding toasts also pauses Fancy
Notify's notifications until you show them again.

**meowdding overlays** (SkyCubed, SkyOcean and other mods built on meowdding-lib) are listed one by one under
their mod's name, for example `SkyCubed: Health`, and move, scale and hide like the rest. Their box is the one
meowdding's own overlay editor highlights. meowdding's own editor (dragging overlays with chat open) still works, but its
highlight and clicks stay where meowdding places the overlay, so pick one editor per overlay. SkyCubed's Moveable
Hotbar and Moveable Item Text aren't listed: SkyCubed draws those on the vanilla hotbar, which Huddable already
places. Overlays switched off in their own mod aren't listed; switch them on there and they come back with
their saved position. Turn this off under General Settings, Advanced.

Other mods that draw their HUD some other way can't be moved until they support Huddable.

### Fonts
Huddable doesn't ship fonts: it uses the ones your resource packs add (`assets/<namespace>/font/<name>.json`), the
same list other mods' font options show. Pick one for the whole profile with the toolbar's **Font** button, or
per element in its options; an element set to Vanilla keeps Minecraft's font even when the profile has another.

Elements with fonts: hotbar (item counts), experience level, held item name, boss bar, scoreboard, action bar,
title, tab list, subtitles, chat, toasts, and health, hunger, armor and air in the Bar and Number styles. Text
that a server or mod draws in a font of its own (icons, for example) keeps it, and characters the chosen font
doesn't have fall back to vanilla's. Backgrounds such as the scoreboard's and the tab list's fit the new font.
If a profile's font comes from a pack you've turned off, those elements draw in vanilla's font until it's back,
and share codes keep the font for players who have the pack.

Chat in a new font re-wraps its lines to fit. Toasts drawn by another mod (Fancy Notify, for example) keep that
mod's font; vanilla's toasts change.

### Styles
- **Bar:** a filled bar with the value written on it. Health shows absorption and hunger shows saturation as a
  thin strip along the top.
- **Number:** text such as `18/20`.
- **Icon + number:** one vanilla icon and the number.

Hunger and air bars drain from the left like vanilla's icons. The styles appear only when vanilla would draw the
bar (armor only with armor on, air only underwater or while it refills).

## General Settings
- **Font quality** (default **Pack's own**): how sharp TrueType fonts from resource packs are drawn, their
  "oversample". **Pack's own** uses what each pack sets, as vanilla does; a higher value is sharper at large GUI
  scales but uses more video memory. Players on the old 4x default were moved to Pack's own in 0.3.4.
  This applies to every TrueType font in the game, other mods' included, as Minecraft loads each font once for
  everyone. Changing it reloads the resource packs once when you leave the screen.
- **Advanced, meowdding overlays** (default on, only shown with meowdding-lib installed): list meowdding-lib's
  overlays as their own elements (see Other mods' HUD elements). Turned off, they go back to where meowdding puts
  them and leave the editor; their saved positions are kept for when you turn it on again.

Saved in `config/huddable/settings.json`, which also remembers the active profile.

## Profiles
The top button in the editor's window shows the active profile; click it to switch to the next one.
- **New:** an empty (vanilla) profile.
- **Rename:** type a name, then OK.
- **Delete:** press twice.
- **Export:** copies the layout to the clipboard as a code starting `HUD1:`.
- **Import:** reads a code from the clipboard into a new profile.
- **Reset:** puts every element, the Size mode and the profile font back to vanilla.

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
`uk.toakan.tad.huddable.api.HuddableApi` from a `uk.toakan.tad.huddable.api.HuddablePlugin` entrypoint. Huddable
only loads that class when it is installed, so it can stay an optional dependency.

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
                .resizable(60, 20)                    // optional: edge grips, with a minimum size
                .listedWhen(() -> MyHud.enabled()));  // optional: left out of the editor while switched off
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

or, for a resizable element, read `HuddableApi.placement(PANEL)` (top-left in GUI pixels, width and height in your
units, the scale to draw at, and whether it's hidden) and lay it out to that. `HuddableApi.openEditor(parent)` opens
the editor, for your own "Move HUD" button. Only the `uk.toakan.tad.huddable.api` package is meant for other mods.
