package huddable.api;

import huddable.ExternalPart;
import huddable.LayoutEditorScreen;
import huddable.Parts;
import huddable.Profiles;
import java.util.function.DoubleSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Lets other mods' HUD elements be moved, scaled, resized and hidden in Huddable's editor. Elements drawn through
 * Fabric's {@code HudElementRegistry} are picked up without this; use it for elements drawn some other way, or
 * to let Huddable size an element that lays itself out (resizable).
 *
 * <p>Register from a {@link HuddablePlugin}. When drawing, either wrap the drawing in {@link #push}/{@link #pop},
 * or read {@link #placement} and lay the element out to it. Everything here is client-thread only.
 */
public final class HuddableApi {
    private HuddableApi() {}

    /** A box in GUI pixels. */
    public record Box(int x, int y, int width, int height) {}

    /** Where the element draws when nothing has moved it, for a screen of the given GUI-scaled size. */
    @FunctionalInterface
    public interface BoxProvider {
        Box box(int screenWidth, int screenHeight);
    }

    /**
     * How an element is placed. {@code defaultBox} is its unmoved box: position in GUI pixels, size in the
     * element's own units before {@code baseScale}. {@code baseScale} is the element's own scale (1 when it has
     * none). Resizable elements get edge grips and must lay themselves out to {@link Placement#width}/height.
     */
    public record Spec(BoxProvider defaultBox, DoubleSupplier baseScale, boolean resizable, int minWidth, int minHeight) {
        public static Spec of(BoxProvider defaultBox) {
            return new Spec(defaultBox, () -> 1.0, false, 1, 1);
        }

        public Spec withBaseScale(DoubleSupplier scale) {
            return new Spec(defaultBox, scale, resizable, minWidth, minHeight);
        }

        public Spec resizable(int minWidth, int minHeight) {
            return new Spec(defaultBox, baseScale, true, Math.max(1, minWidth), Math.max(1, minHeight));
        }
    }

    /**
     * Where to draw this frame: top-left corner in GUI pixels, size in the element's own units, and the scale to
     * draw at (base scale, layout scale and the profile's screen scale together).
     */
    public record Placement(int x, int y, int width, int height, float scale, boolean hidden) {}

    /** Adds an element to the editor. Registering an ID twice replaces the first. */
    public static void register(Identifier id, Component label, Spec spec) {
        Parts.add(ExternalPart.api(id, label, spec));
    }

    /**
     * Keeps Huddable from wrapping one of your Fabric HUD layers itself, for a layer whose elements you register
     * here one by one instead.
     */
    public static void ignoreLayer(Identifier fabricLayerId) {
        Parts.ignore(fabricLayerId);
    }

    /** This frame's placement of a registered element, or null when {@code id} isn't registered. */
    public static Placement placement(Identifier id) {
        ExternalPart part = Parts.external(id);
        if (part == null) return null;
        Minecraft mc = Minecraft.getInstance();
        return part.placement(mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
    }

    /**
     * Moves and scales the pose so drawing the element at its default box lands where the player put it. Returns
     * false, and changes nothing, when the element is hidden or not registered; call {@link #pop} only after true.
     */
    public static boolean push(GuiGraphicsExtractor graphics, Identifier id) {
        ExternalPart part = Parts.external(id);
        if (part == null) return false;
        Placement p = part.placement(graphics.guiWidth(), graphics.guiHeight());
        if (p.hidden()) return false;
        Box box = part.defaultBox(graphics.guiWidth(), graphics.guiHeight());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(p.x(), p.y());
        pose.scale(p.scale(), p.scale());
        pose.translate(-box.x(), -box.y());
        return true;
    }

    public static void pop(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    /** True while Huddable's editor is open (placements then show unsaved edits). */
    public static boolean isEditing() {
        return Profiles.editing();
    }

    /** Opens Huddable's editor, for a mod's own "Move HUD" button. */
    public static void openEditor(Screen parent) {
        Minecraft.getInstance().gui.setScreen(new LayoutEditorScreen(parent));
    }
}
