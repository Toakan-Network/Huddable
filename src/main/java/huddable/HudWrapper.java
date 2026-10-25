package huddable;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

/**
 * Wraps every {@link Element} through Fabric's {@link HudElementRegistry#replaceElement}, so vanilla (and any mod
 * that wrapped it first) still does the drawing: Huddable only shifts and scales the pose around it, skips it when
 * hidden, swaps its font ({@link HudFonts}), or draws a {@link BarRenderers} style instead. Other mods' Fabric layers are wrapped the same way by
 * {@link HudDiscovery}. Chat is the exception: it is moved by
 * {@code mixin.ChatComponentMixin}, as the open chat isn't drawn through the HUD.
 */
public final class HudWrapper {
    /** A box on screen, in GUI pixels. */
    public record Box(int x, int y, int width, int height) {}

    private HudWrapper() {}

    public static void registerAll() {
        for (Element element : Element.values()) {
            if (element.id == null) continue; // toasts: GuiToastsMixin
            HudElementRegistry.replaceElement(element.id, original -> (graphics, delta) -> render(element, original, graphics, delta));
        }
    }

    private static void render(Element element, HudElement original, GuiGraphicsExtractor graphics, DeltaTracker delta) {
        ElementLayout layout = Profiles.current().get(element);
        if (layout.hidden) return;
        // Chat is moved by ChatComponentMixin, which also covers the open chat and its clicks.
        float scale = scale(element, layout);
        // Chat's font goes into its line styles instead (ChatComponentMixin).
        Font font = element == Element.CHAT ? null : HudFonts.wrapped(element, Profiles.current());
        if (element == Element.CHAT || layout.isDefault() && scale == 1f && font == null) {
            original.extractRenderState(graphics, delta);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int[] anchor = element.anchor(mc, graphics.guiWidth(), graphics.guiHeight());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        transform(pose, anchor[0], anchor[1], layout, scale);
        if (layout.style == Style.VANILLA) {
            drawInFont(font, () -> original.extractRenderState(graphics, delta));
        } else if (mc.getCameraEntity() instanceof Player player) {
            BarRenderers.Reading reading = BarRenderers.read(element, player);
            if (reading != null) {
                BarRenderers.draw(graphics, font != null ? font : mc.font, element, layout, anchor[0], anchor[1], reading);
            }
        }
        pose.popMatrix();
    }

    /** Runs vanilla's drawing with {@code font} (null = vanilla's) in place of the game font ({@code HudFontMixin}). */
    public static void drawInFont(Font font, Runnable draw) {
        if (font == null) {
            draw.run();
            return;
        }
        Font previous = HudFonts.setActive(font);
        try {
            draw.run();
        } finally {
            HudFonts.setActive(previous);
        }
    }

    /** Another mod's Fabric HUD layer ({@link HudDiscovery}): moved and scaled around its measured anchor. */
    static void renderExternal(ExternalPart part, HudElement original, GuiGraphicsExtractor graphics, DeltaTracker delta) {
        ElementLayout layout = Profiles.current().get(part);
        part.measureIfNeeded(graphics, delta, !layout.isDefault());
        if (layout.hidden) return;
        float scale = scale(part, layout);
        if (layout.dx == 0 && layout.dy == 0 && scale == 1f) {
            original.extractRenderState(graphics, delta);
            return;
        }
        int[] anchor = part.anchor(Minecraft.getInstance(), graphics.guiWidth(), graphics.guiHeight());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        transform(pose, anchor[0], anchor[1], layout, scale);
        original.extractRenderState(graphics, delta);
        pose.popMatrix();
    }

    /** The scale an element draws at: its own, the layout's and the profile's screen scale together. */
    public static float scale(HudPart part, ElementLayout layout) {
        return part.baseScale() * layout.scaleFactor() * Profiles.current().sizing.factor();
    }

    /** Moves the anchor by the layout's offset and scales around it. */
    public static void transform(Matrix3x2f pose, int anchorX, int anchorY, ElementLayout layout, float scale) {
        pose.translate(anchorX + layout.dx, anchorY + layout.dy);
        pose.scale(scale, scale);
        pose.translate(-anchorX, -anchorY);
    }

    /** Where the element ends up on screen with {@code layout}, for the editor. */
    public static Box box(HudPart part, ElementLayout layout, Minecraft mc, int w, int h) {
        HudPart.Geometry g = part.geometry(mc, w, h);
        int x = g.x();
        int y = g.y();
        int width;
        int height;
        if (part instanceof Element element && layout.style != Style.VANILLA) {
            BarRenderers.Reading reading = mc.getCameraEntity() instanceof Player player ? BarRenderers.read(element, player) : null;
            int[] size = BarRenderers.size(element, layout, HudFonts.measuring(element, Profiles.current()), reading);
            width = size[0];
            height = size[1];
            x = element.rightAligned ? g.anchorX() - width : g.anchorX();
            y = g.anchorY() - height;
        } else {
            int[] size = part.size(mc, g, layout);
            width = size[0];
            height = size[1];
        }
        float scale = scale(part, layout);
        int left = Math.round(g.anchorX() + layout.dx + (x - g.anchorX()) * scale);
        int top = Math.round(g.anchorY() + layout.dy + (y - g.anchorY()) * scale);
        return new Box(left, top, Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
    }
}
