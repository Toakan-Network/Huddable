package uk.toakan.tad.huddable;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Container screens (inventory, chests, furnaces...) drawn with a chosen resource pack's background picture instead
 * of the top pack's. Each screen's background is one fixed PNG with the slots painted in, so another pack's copy
 * lines up the same. The profile has one pack for every screen ({@link Layout#screenArt}); a screen may have its own
 * ({@link Layout#screenArtFor}, {@link #TOP} = the top pack's). A pack that isn't enabled, or hasn't got that
 * screen, leaves the top pack's picture. Swapped in {@code ContainerArtMixin}; the art comes from {@link PackFrames}.
 */
public final class HudScreenArt {
    private HudScreenArt() {}

    /** A screen's choice of the top pack's picture, even when the profile picks a pack. */
    public static final String TOP = "top";
    private static final String PREFIX = "textures/gui/container/";
    private static final String SUFFIX = ".png";
    private static final int MAX_ID_LENGTH = 256;

    /** Vanilla's container backgrounds (26.2 and 26.3), by the name saved in profiles. */
    public static final List<String> SCREENS = List.of("inventory", "generic_54", "shulker_box", "crafting_table",
        "furnace", "blast_furnace", "smoker", "dispenser", "hopper", "crafter", "brewing_stand", "enchanting_table",
        "anvil", "grindstone", "smithing", "stonecutter", "loom", "cartography_table", "beacon", "villager", "horse",
        "nautilus");

    /** Per screen, the enabled packs that have its picture (top first); listed once per resource reload. */
    private static Map<String, List<String>> packs;

    public static Identifier file(String screen) {
        return Identifier.withDefaultNamespace(PREFIX + screen + SUFFIX);
    }

    /** The texture to draw in place of {@code location} in the current layout; {@code location} when unchanged. */
    public static Identifier swap(Identifier location) {
        Layout layout = Profiles.current();
        if (layout.screenArt == null && layout.screenArtFor.isEmpty()) return location;
        if (!location.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) return location;
        String path = location.getPath();
        if (!path.startsWith(PREFIX) || !path.endsWith(SUFFIX)) return location;
        String pack = pack(layout, path.substring(PREFIX.length(), path.length() - SUFFIX.length()));
        return pack == null ? location : PackFrames.packTexture(location, pack);
    }

    /** The pack {@code screen} is drawn from in {@code layout}; null = the top pack's. */
    public static String pack(Layout layout, String screen) {
        String own = layout.screenArtFor.get(screen);
        String pack = own != null ? own : layout.screenArt;
        return TOP.equals(pack) ? null : pack;
    }

    /** The enabled packs that have {@code screen}'s picture, top first. */
    public static List<String> packsWith(String screen) {
        return packs().getOrDefault(screen, List.of());
    }

    /** The enabled packs that have any screen's picture, top first. */
    public static List<String> packsWithAny() {
        List<Identifier> files = new ArrayList<>();
        for (String screen : SCREENS) files.add(file(screen));
        return PackFrames.packsWithFiles(files);
    }

    private static Map<String, List<String>> packs() {
        if (packs == null) {
            Map<String, List<String>> found = new LinkedHashMap<>();
            for (String screen : SCREENS) found.put(screen, PackFrames.packsWithFiles(List.of(file(screen))));
            packs = found;
        }
        return packs;
    }

    /** A pack choice as the editor shows it: null and {@link #TOP} are the top pack. */
    public static String packName(String pack) {
        return pack == null || TOP.equals(pack) ? I18n.get("huddable.screens.top_pack") : PackFrames.title(pack);
    }

    /** {@code pack} as a clean pack choice ({@link #TOP} included), or null. */
    public static String cleanPack(String pack) {
        if (pack == null || pack.isBlank() || pack.length() > MAX_ID_LENGTH) return null;
        return pack.trim();
    }

    /** A screen's name for the editor. */
    public static String name(String screen) {
        return I18n.get(switch (screen) {
            case "inventory" -> "huddable.screen.inventory";
            case "generic_54" -> "huddable.screen.generic_54";
            case "shulker_box" -> "huddable.screen.shulker_box";
            case "crafting_table" -> "huddable.screen.crafting_table";
            case "furnace" -> "huddable.screen.furnace";
            case "blast_furnace" -> "huddable.screen.blast_furnace";
            case "smoker" -> "huddable.screen.smoker";
            case "dispenser" -> "huddable.screen.dispenser";
            case "hopper" -> "huddable.screen.hopper";
            case "crafter" -> "huddable.screen.crafter";
            case "brewing_stand" -> "huddable.screen.brewing_stand";
            case "enchanting_table" -> "huddable.screen.enchanting_table";
            case "anvil" -> "huddable.screen.anvil";
            case "grindstone" -> "huddable.screen.grindstone";
            case "smithing" -> "huddable.screen.smithing";
            case "stonecutter" -> "huddable.screen.stonecutter";
            case "loom" -> "huddable.screen.loom";
            case "cartography_table" -> "huddable.screen.cartography_table";
            case "beacon" -> "huddable.screen.beacon";
            case "villager" -> "huddable.screen.villager";
            case "horse" -> "huddable.screen.horse";
            case "nautilus" -> "huddable.screen.nautilus";
            default -> screen;
        });
    }

    /** Resource packs changed: which packs have which screens. The art itself is dropped by {@link HudFrames}. */
    public static void reloaded() {
        packs = null;
    }
}
