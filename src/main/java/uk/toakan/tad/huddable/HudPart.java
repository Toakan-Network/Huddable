package uk.toakan.tad.huddable;

import net.minecraft.client.Minecraft;

/**
 * Something on the HUD the editor can place: a vanilla {@link Element}, or another mod's element
 * ({@link ExternalPart}). Its {@link ElementLayout} is stored in the profile under {@link #key()}.
 */
public interface HudPart {
    /** Anchor point plus the unmoved, unscaled box, in GUI pixels. */
    record Geometry(int anchorX, int anchorY, int x, int y, int width, int height) {}

    /** The profile key: the enum name for vanilla elements, {@code mod:path} for other mods'. */
    String key();

    String label();

    boolean movable();

    /** True for the status bars, which have {@link Style}s besides vanilla. */
    boolean restyleable();

    /** True when a frame can be drawn around this element ({@link HudFrames}). */
    default boolean frameable() {
        return true;
    }

    /** Where the element draws now, for a screen of {@code w} x {@code h} GUI pixels. */
    Geometry geometry(Minecraft mc, int w, int h);

    /** Just the anchor, for drawing every frame. */
    default int[] anchor(Minecraft mc, int w, int h) {
        Geometry g = geometry(mc, w, h);
        return new int[] {g.anchorX(), g.anchorY()};
    }

    /** The element's own scale under the layout's: 1 for vanilla, the owning mod's HUD scale for API elements. */
    default float baseScale() {
        return 1f;
    }

    /** True when edge grips can set this element's width and height with {@code layout}. */
    default boolean resizable(ElementLayout layout) {
        return false;
    }

    /** The unmoved, unscaled size the element draws at with {@code layout}. */
    default int[] size(Minecraft mc, Geometry g, ElementLayout layout) {
        return new int[] {g.width(), g.height()};
    }

    /** Sets the unscaled size from an edge drag; only called when {@link #resizable} is true. */
    default void resize(Minecraft mc, ElementLayout layout, int width, int height) {
    }
}
