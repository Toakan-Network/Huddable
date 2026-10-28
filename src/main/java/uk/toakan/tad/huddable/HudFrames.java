package uk.toakan.tad.huddable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Frames drawn around HUD elements: vanilla's tooltip, popup or button art, from the top resource pack or a chosen
 * one ({@link PackFrames}). A frame id is {@code kind} (the top pack's art) or {@code kind@packId}. The profile has
 * one frame for every element ({@link Layout#frame}); an element may have its own or {@link #NONE}. An id whose pack
 * isn't enabled draws from the top pack and is kept, so a share code still works for someone who has the pack.
 * Frame art is drawn at screen size, not scaled with the element, so every frame on the HUD matches.
 */
public final class HudFrames {
    private HudFrames() {}

    /** An element's choice of no frame, even when the profile has one. */
    public static final String NONE = "none";
    private static final int MAX_ID_LENGTH = 256;

    /** A frame: its kind and the pack its art comes from ("" = the top pack). */
    public record Choice(PackFrames.Kind kind, String pack) {}

    /** Every frame id the packs offer, listed once per resource reload (the editor rebuilds on every drag). */
    private static List<String> available;

    /** {@code id} as a clean frame id ({@link #NONE} included), or null when it isn't one. */
    public static String cleanId(String id) {
        if (id == null || id.isBlank() || id.length() > MAX_ID_LENGTH) return null;
        String trimmed = id.trim();
        if (NONE.equals(trimmed)) return NONE;
        return parse(trimmed) != null ? trimmed : null;
    }

    /** The frame {@code id} names, or null ({@link #NONE} and unknown kinds). */
    public static Choice parse(String id) {
        if (id == null) return null;
        int at = id.indexOf('@');
        PackFrames.Kind kind = PackFrames.Kind.byId(at < 0 ? id : id.substring(0, at));
        if (kind == null) return null;
        return new Choice(kind, at < 0 ? "" : id.substring(at + 1));
    }

    public static String id(PackFrames.Kind kind, String pack) {
        return pack.isEmpty() ? kind.id : kind.id + "@" + pack;
    }

    /** The frame {@code part} is drawn with in {@code layout}: its own, else the profile's; null for none. */
    public static Choice resolve(HudPart part, Layout layout) {
        if (!part.frameable()) return null;
        String id = layout.get(part).frame;
        if (id == null) id = layout.frame;
        return NONE.equals(id) ? null : parse(id);
    }

    /** Each kind from the top pack, then from every enabled pack that has art for it. */
    public static List<String> available() {
        if (available == null) {
            List<String> ids = new ArrayList<>();
            for (PackFrames.Kind kind : PackFrames.Kind.values()) {
                ids.add(kind.id);
                for (String pack : PackFrames.packsWith(kind)) ids.add(id(kind, pack));
            }
            available = List.copyOf(ids);
        }
        return available;
    }

    /** A frame's name for the editor: "Tooltip (Faithful 64x)". */
    public static String name(String id) {
        Choice choice = parse(id);
        if (choice == null) return id;
        String kind = I18n.get(switch (choice.kind()) {
            case TOOLTIP -> "huddable.frame.tooltip";
            case POPUP -> "huddable.frame.popup";
            case BUTTON -> "huddable.frame.button";
        });
        String pack = choice.pack().isEmpty() ? I18n.get("huddable.frame.top_pack") : PackFrames.title(choice.pack());
        return I18n.get("huddable.frame.name", kind, pack);
    }

    /** True when {@code id}'s art comes from a pack that isn't enabled now (it draws from the top pack). */
    public static boolean packOff(String id) {
        Choice choice = parse(id);
        return choice != null && !choice.pack().isEmpty() && !PackFrames.enabled(choice.pack());
    }

    /** Resource packs changed: which frames exist, and their art. */
    public static void reloaded() {
        available = null;
        PackFrames.reloaded();
    }

    /**
     * Draws {@code choice} around an element's box (given in the current pose), far enough out that the element sits
     * inside the edge art. Full-screen layers get none.
     */
    public static void drawAround(GuiGraphicsExtractor graphics, Choice choice, float x, float y, float w, float h) {
        draw(graphics, choice, x, y, w, h, choice.kind().inset, true);
    }

    /** Draws {@code choice} over the box (given in the current pose): for API elements that keep their content inside. */
    public static void drawOver(GuiGraphicsExtractor graphics, Choice choice, float x, float y, float w, float h) {
        draw(graphics, choice, x, y, w, h, 0, false);
    }

    private static void draw(GuiGraphicsExtractor graphics, Choice choice, float x, float y, float w, float h,
                             int outset, boolean skipFullScreen) {
        Matrix3x2fStack pose = graphics.pose();
        Vector2f a = pose.transformPosition(x, y, new Vector2f());
        Vector2f b = pose.transformPosition(x + w, y + h, new Vector2f());
        int left = Math.round(Math.min(a.x, b.x)) - outset;
        int top = Math.round(Math.min(a.y, b.y)) - outset;
        int right = Math.round(Math.max(a.x, b.x)) + outset;
        int bottom = Math.round(Math.max(a.y, b.y)) + outset;
        if (right <= left || bottom <= top) return;
        if (skipFullScreen && right - left >= graphics.guiWidth() * 3 / 4 && bottom - top >= graphics.guiHeight() * 3 / 4) {
            return;
        }
        pose.pushMatrix();
        pose.identity();
        PackFrames.drawFrame(graphics, choice.kind(), choice.pack(), left, top, right - left, bottom - top);
        pose.popMatrix();
    }
}
