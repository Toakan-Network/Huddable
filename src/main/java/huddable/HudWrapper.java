package huddable;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2fStack;

/**
 * Wraps every {@link Element} through Fabric's {@link HudElementRegistry#replaceElement}, so vanilla (and any mod
 * that wrapped it first) still does the drawing: Huddable only shifts and scales the pose around it, skips it when
 * hidden, or draws a {@link BarRenderers} style instead. No mixins.
 */
public final class HudWrapper {
    /** A box on screen, in GUI pixels. */
    public record Box(int x, int y, int width, int height) {}

    private HudWrapper() {}

    public static void registerAll() {
        for (Element element : Element.values()) {
            HudElementRegistry.replaceElement(element.id, original -> (graphics, delta) -> render(element, original, graphics, delta));
        }
    }

    private static void render(Element element, HudElement original, GuiGraphicsExtractor graphics, DeltaTracker delta) {
        ElementLayout layout = Profiles.current().get(element);
        if (layout.hidden) return;
        if (layout.isDefault()) {
            original.extractRenderState(graphics, delta);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int[] anchor = element.anchor(mc, graphics.guiWidth(), graphics.guiHeight());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        transform(pose, anchor[0], anchor[1], layout);
        if (layout.style == Style.VANILLA) {
            original.extractRenderState(graphics, delta);
        } else if (mc.getCameraEntity() instanceof Player player) {
            BarRenderers.Reading reading = BarRenderers.read(element, player);
            if (reading != null) BarRenderers.draw(graphics, mc.font, element, layout, anchor[0], anchor[1], reading);
        }
        pose.popMatrix();
    }

    /** Moves the anchor by the layout's offset and scales around it. */
    static void transform(Matrix3x2fStack pose, int anchorX, int anchorY, ElementLayout layout) {
        float scale = layout.scaleFactor();
        pose.translate(anchorX + layout.dx, anchorY + layout.dy);
        pose.scale(scale, scale);
        pose.translate(-anchorX, -anchorY);
    }

    /** Where the element ends up on screen with {@code layout}, for the editor. */
    public static Box box(Element element, ElementLayout layout, Minecraft mc, int w, int h) {
        Element.Geometry g = element.geometry(mc, w, h);
        int x = g.x();
        int y = g.y();
        int width = g.width();
        int height = g.height();
        if (layout.style != Style.VANILLA) {
            BarRenderers.Reading reading = mc.getCameraEntity() instanceof Player player ? BarRenderers.read(element, player) : null;
            int[] size = BarRenderers.size(element, layout, mc.font, reading);
            width = size[0];
            height = size[1];
            x = element.rightAligned ? g.anchorX() - width : g.anchorX();
            y = g.anchorY() - height;
        }
        float scale = layout.scaleFactor();
        int left = Math.round(g.anchorX() + layout.dx + (x - g.anchorX()) * scale);
        int top = Math.round(g.anchorY() + layout.dy + (y - g.anchorY()) * scale);
        return new Box(left, top, Math.max(1, Math.round(width * scale)), Math.max(1, Math.round(height * scale)));
    }
}
