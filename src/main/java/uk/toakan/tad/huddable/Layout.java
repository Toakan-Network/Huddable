package uk.toakan.tad.huddable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A whole HUD layout: one {@link ElementLayout} per changed element, keyed by {@link HudPart#key()} (the enum name
 * for vanilla elements, {@code mod:path} for other mods'), plus the screen scale and the profile font. Elements
 * left out draw as vanilla. Gson POJO, saved as one profile file and packed into share codes.
 */
public final class Layout {
    private static final ElementLayout VANILLA = new ElementLayout();
    private static final String CODE_PREFIX = "HUD1:";
    private static final int MAX_CODE_CHARS = 20_000;
    private static final int MAX_CODE_JSON_BYTES = 100_000;
    /** Share codes are packed without pretty-printing, so they stay short. */
    private static final Gson COMPACT = new Gson();

    public Map<String, ElementLayout> elements = new LinkedHashMap<>();
    public Sizing sizing = Sizing.GUI_SCALE;
    /** The profile's font for every element that has no font of its own; null = vanilla's ({@link HudFonts}). */
    public String font;

    /** The element's layout, or a shared vanilla one when it has none; don't change what this returns. */
    public ElementLayout get(HudPart part) {
        ElementLayout l = elements.get(part.key());
        return l != null ? l : VANILLA;
    }

    /** The element's own layout, created if missing, for editing. */
    public ElementLayout edit(HudPart part) {
        return elements.computeIfAbsent(part.key(), k -> new ElementLayout());
    }

    public void reset(HudPart part) {
        elements.remove(part.key());
    }

    /** This layout as a {@code HUD1:} share code. */
    public String toCode() {
        return ShareCodes.encode(CODE_PREFIX, COMPACT.toJson(copy().clamp()));
    }

    /** The layout in {@code code}, clamped, or null when it isn't a valid share code. */
    public static Layout fromCode(String code) {
        String json = ShareCodes.decode(CODE_PREFIX, code, MAX_CODE_CHARS, MAX_CODE_JSON_BYTES);
        if (json == null) return null;
        try {
            Layout layout = COMPACT.fromJson(json, Layout.class);
            return layout != null ? layout.clamp() : null;
        } catch (JsonParseException e) {
            return null;
        }
    }

    public Layout copy() {
        Layout c = new Layout();
        elements.forEach((k, v) -> c.elements.put(k, v.copy()));
        c.sizing = sizing;
        c.font = font;
        return c;
    }

    /**
     * Makes a loaded or imported layout safe to draw: unknown vanilla element names and vanilla-looking entries
     * are dropped, and every value is put in range. Other mods' entries ({@code mod:path}) are kept even when
     * that mod isn't loaded, so removing a mod doesn't lose where its elements were.
     */
    public Layout clamp() {
        if (elements == null) elements = new LinkedHashMap<>();
        if (sizing == null) sizing = Sizing.GUI_SCALE;
        font = HudFonts.cleanId(font);
        if (HudFonts.DEFAULT_FONT.equals(font)) font = null;
        Map<String, ElementLayout> kept = new LinkedHashMap<>();
        for (Element element : Element.values()) {
            ElementLayout l = elements.get(element.name());
            if (l == null) continue;
            l.clamp(element);
            if (!l.isDefault()) kept.put(element.name(), l);
        }
        elements.forEach((key, l) -> {
            if (l == null || !key.contains(":")) return;
            l.clamp(false, true);
            if (!l.isDefault()) kept.put(key, l);
        });
        elements = kept;
        return this;
    }
}
