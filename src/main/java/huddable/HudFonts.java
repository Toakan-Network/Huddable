package huddable;

import huddable.mixin.FontAccessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;

/**
 * Fonts for HUD elements: any font the resource packs provide, set per profile ({@link Layout#font}) and per
 * element ({@link ElementLayout#font}).
 *
 * <p>Vanilla looks glyphs up per character style, and prepares text after the element has drawn, using the
 * {@link Font} it was given. So an element's font is a wrapper {@code Font} that sends vanilla's default font to
 * the chosen one: while {@link HudWrapper} draws the element it is {@link #active}, and {@code mixin.HudFontMixin}
 * hands it to vanilla's HUD code in place of {@code Minecraft.font}, for drawing and measuring alike. Text that
 * names its own font (server icons, other mods) keeps it. Chat splits its lines once per message, so it carries
 * the font in the line styles instead ({@code mixin.ChatComponentMixin}).
 */
public final class HudFonts {
    public static final String DEFAULT_FONT = "minecraft:default";
    private static final int MAX_ID_LENGTH = 200;

    /** Wrapper fonts by id. The real font's provider outlives resource reloads, so these stay valid. */
    private static final Map<String, Font> WRAPPED = new HashMap<>();
    private static final Map<String, FontDescription> DESCRIPTIONS = new HashMap<>();
    /** Font ids the loaded packs provide, or null until next asked; cleared on resource reload. */
    private static Set<String> loaded;
    private static Font active;
    private static int generation;

    private HudFonts() {}

    /** Vanilla elements that draw text: the text ones, and the status bars, whose Number and Bar styles do. */
    public static boolean fontable(HudPart part) {
        return part instanceof Element element && element.fontable;
    }

    /** True when {@code layout} would draw the part's text in some font: text elements, or a restyled bar. */
    public static boolean fontable(HudPart part, ElementLayout layout) {
        return fontable(part) && (!part.restyleable() || layout.style != Style.VANILLA);
    }

    /** {@code id} as a clean font id, or null when it isn't one. */
    public static String cleanId(String id) {
        if (id == null || id.isBlank() || id.length() > MAX_ID_LENGTH) return null;
        Identifier parsed = Identifier.tryParse(id.trim());
        return parsed != null ? parsed.toString() : null;
    }

    /** Every font the loaded resource packs provide ({@code assets/<ns>/font/<name>.json}), vanilla's first. */
    public static List<String> availableFonts() {
        List<String> fonts = new ArrayList<>();
        FileToIdConverter converter = FileToIdConverter.json("font");
        for (Identifier file : converter.listMatchingResources(Minecraft.getInstance().getResourceManager()).keySet()) {
            Identifier id = converter.fileToId(file);
            // "include/..." fonts are building blocks other fonts pull in, not fonts to pick.
            if (!id.getPath().startsWith("include/")) fonts.add(id.toString());
        }
        fonts.sort(null);
        fonts.remove(DEFAULT_FONT);
        fonts.addFirst(DEFAULT_FONT);
        return fonts;
    }

    /** The font id the part draws with, or null for vanilla's (none set, or that font isn't loaded now). */
    public static String fontId(HudPart part, Layout layout) {
        if (!fontable(part)) return null;
        String id = layout.get(part).font;
        if (id == null) id = layout.font;
        if (id == null || id.equals(DEFAULT_FONT)) return null;
        if (loaded == null) loaded = new HashSet<>(availableFonts());
        return loaded.contains(id) ? id : null;
    }

    /** The part's wrapper font, or null for vanilla's. */
    public static Font wrapped(HudPart part, Layout layout) {
        String id = fontId(part, layout);
        return id != null ? WRAPPED.computeIfAbsent(id, HudFonts::wrap) : null;
    }

    /** The font to measure the part with: its own, or vanilla's. */
    public static Font measuring(HudPart part, Layout layout) {
        Font font = wrapped(part, layout);
        return font != null ? font : Minecraft.getInstance().font;
    }

    /** The font chat's lines are split with, or null for vanilla's. */
    public static FontDescription chatFont() {
        String id = fontId(Element.CHAT, Profiles.current());
        return id != null ? description(id) : null;
    }

    private static Font wrap(String id) {
        Font.Provider real = ((FontAccessor) Minecraft.getInstance().font).huddable$provider();
        FontDescription target = description(id);
        return new Font(new Font.Provider() {
            @Override
            public GlyphSource glyphs(FontDescription font) {
                return real.glyphs(FontDescription.DEFAULT.equals(font) ? target : font);
            }

            @Override
            public EffectGlyph effect() {
                return real.effect();
            }
        });
    }

    private static FontDescription description(String id) {
        return DESCRIPTIONS.computeIfAbsent(id, k -> new FontDescription.Resource(Identifier.parse(k)));
    }

    /** The font vanilla's HUD code draws with in place of {@code Minecraft.font}, or null. Render thread only. */
    public static Font active() {
        return active;
    }

    /** Sets {@link #active} and returns the one it replaces, to put back after drawing. */
    public static Font setActive(Font font) {
        Font previous = active;
        active = font;
        return previous;
    }

    /** Fonts changed (resource reload, F3+T): which ids exist and every glyph width may be different. */
    public static void reloaded() {
        loaded = null;
        generation++;
    }

    /** Changes on every resource reload, so chat knows to split its lines again. */
    public static int generation() {
        return generation;
    }
}
