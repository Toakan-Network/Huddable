package huddable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A whole HUD layout: one {@link ElementLayout} per changed element, keyed by the element's enum name. Elements
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

    /** The element's layout, or a shared vanilla one when it has none; don't change what this returns. */
    public ElementLayout get(Element element) {
        ElementLayout l = elements.get(element.name());
        return l != null ? l : VANILLA;
    }

    /** The element's own layout, created if missing, for editing. */
    public ElementLayout edit(Element element) {
        return elements.computeIfAbsent(element.name(), k -> new ElementLayout());
    }

    public void reset(Element element) {
        elements.remove(element.name());
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
        return c;
    }

    /**
     * Makes a loaded or imported layout safe to draw: unknown element names and vanilla-looking entries are
     * dropped, and every value is put in range.
     */
    public Layout clamp() {
        if (elements == null) elements = new LinkedHashMap<>();
        Map<String, ElementLayout> kept = new LinkedHashMap<>();
        for (Element element : Element.values()) {
            ElementLayout l = elements.get(element.name());
            if (l == null) continue;
            l.clamp(element);
            if (!l.isDefault()) kept.put(element.name(), l);
        }
        elements = kept;
        return this;
    }
}
