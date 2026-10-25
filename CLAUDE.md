# Huddable

Fabric client mod (mod id `huddable`, Java package `huddable`): an in-game editor for the vanilla HUD. Players
move, scale, hide and restyle HUD elements, keep named layout profiles and share them as `HUD1:` codes. Works on
any server, no SkyBlock code. Loom 1.17, Java 25.

Shared rules (commits, versioning, resource light) are in the parent `Games\Minecraft\CLAUDE.md`. This file only
has what differs. Progress, next steps and known issues are in `ROADMAP.md`: read it before continuing, and
update it in the same commit when a step lands or an issue is found or fixed.

## Design
- Every vanilla element is wrapped with Fabric's `HudElementRegistry.replaceElement` (`HudWrapper`), so
  vanilla, or a mod that wrapped it first, still draws; Huddable only shifts and scales the pose, skips it when
  hidden, or draws a `BarRenderers` style instead.
- One mixin, for chat only (`mixin.ChatComponentMixin`): the HUD element only covers the closed chat, while the
  open chat and `ChatScreen`'s click test call `ChatComponent.extractRenderState(ChatGraphicsAccess, ...)`
  directly. Its HEAD applies the chat layout through `updatePose`, which every access honours (hovers invert the
  pose), so drawing and clicks move together. `HudWrapper` must not also shift chat. Add no other mixins
  without a reason like this.
- Positions are offsets from each element's vanilla anchor (`Element.geometry`), so layouts follow screen size
  and GUI scale. Element boxes are for the editor only; keep the per-frame path (`Element.anchor`) cheap.
- Everything placeable is a `HudPart`: the `Element` enum, or `ExternalPart` for other mods (listed by `Parts`,
  stored under `mod:path` keys that `Layout.clamp` keeps even when the mod is gone).
- `HudDiscovery` reads Fabric's internal `HudElementRegistryImpl.ROOT_ELEMENTS`, read-only, because the public
  API can't list layers (Fabric's own status bar height code reads it the same way). Keep it in its try/catch so
  an internals change only turns discovery off; wrap through the public `replaceElement` only. Discovered layers
  are measured by drawing them into a scratch `GuiRenderState`: only when the screen size changes, while the
  editor is open, or (once a second) while an edited layer hasn't drawn yet.
- `huddable.api` is the only package other mods may use; keep its signatures to its own and vanilla types.
- Fabric HUD API sources for checking names are in `..\.mc-sources\fabric-rendering-v1\<mc>\`; vanilla in
  `..\.mc-sources\<mc>\`.
- Dependencies: Fabric API only. Mod Menu is optional (`compileOnly`, `suggests`) and is the only way to open the
  editor; only `compat.ModMenuCompat` may touch its API.

## Builds: one jar per Minecraft version
As in CraftingTracker: `./gradlew build` builds `:mc26.2` and `:mc26.3` from the one `src/` (shared
`common.gradle`, per-version `versions/<mc>/gradle.properties`). Jars land in the root `build/libs`, named
`huddable-<mod_version>+<mc>.jar`. Only use Minecraft and Fabric API members present in every supported version.
- No `org.lwjgl.glfw`. Read `InputConstants` key and mouse codes with `HuddableClient.inputCode("MOUSE_BUTTON_LEFT")`;
  they differ between 26.2 and 26.3.

## Testing in game
Mod Menu isn't in the dev client, so test the editor in the live instance
(`G:\Games\Minecraft\Instances\Hypixel 26.2\`) or any instance with Mod Menu. Config is `config\huddable\`.
