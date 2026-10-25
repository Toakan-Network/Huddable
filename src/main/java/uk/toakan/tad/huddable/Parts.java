package uk.toakan.tad.huddable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.Identifier;

/** Every element the editor can place: the vanilla ones, then other mods' sorted by label. Client thread only. */
public final class Parts {
    private static final List<ExternalPart> EXTERNAL = new ArrayList<>();
    /** Fabric layers Huddable must not wrap: API-registered IDs and layers their mods asked to skip. */
    private static final Set<Identifier> CLAIMED = new HashSet<>();

    private Parts() {}

    public static List<HudPart> all() {
        List<HudPart> parts = new ArrayList<>(List.of(Element.values()));
        parts.addAll(EXTERNAL);
        return parts;
    }

    /** Adds a part, replacing one with the same ID. */
    public static void add(ExternalPart part) {
        EXTERNAL.removeIf(p -> p.id.equals(part.id));
        EXTERNAL.add(part);
        EXTERNAL.sort(Comparator.comparing(HudPart::label, String.CASE_INSENSITIVE_ORDER));
        if (!part.isFabric()) CLAIMED.add(part.id);
    }

    public static void ignore(Identifier fabricLayerId) {
        CLAIMED.add(fabricLayerId);
    }

    static boolean claimed(Identifier id) {
        return CLAIMED.contains(id);
    }

    /** The other mod's part with this ID, or null. */
    public static ExternalPart external(Identifier id) {
        for (ExternalPart part : EXTERNAL) {
            if (part.id.equals(id)) return part;
        }
        return null;
    }
}
