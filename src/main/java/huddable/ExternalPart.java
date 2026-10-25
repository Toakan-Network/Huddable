package huddable;

import huddable.api.HuddableApi;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * Another mod's HUD element. Two kinds:
 * <ul>
 * <li>Fabric: a layer found in Fabric's HUD registry ({@link HudDiscovery}). Its box is unknown, so it is
 * measured by drawing the layer into a scratch render state and taking the bounds of what it drew. Its anchor is
 * the box's left, centre or right (top, middle or bottom) by which third of the screen it sits in, so a layout
 * follows the screen like the vanilla anchors do.</li>
 * <li>API: registered through {@link HuddableApi}, which gives the box (and may let it resize).</li>
 * </ul>
 */
public final class ExternalPart implements HudPart {
    /** How often an element that drew nothing is measured again, while it matters. */
    private static final long RETRY_MILLIS = 1000;

    public final Identifier id;
    private final Component label;
    /** API elements only. */
    private final HuddableApi.Spec spec;
    /** Fabric layers only: the wrapped layer, set when Huddable wraps it. */
    HudElement original;
    /** Fabric layers only: the last measured box (null until something was drawn) and the screen it was on. */
    private HuddableApi.Box measured;
    private int measuredWidth = -1;
    private int measuredHeight = -1;
    private long lastMeasure;

    private ExternalPart(Identifier id, Component label, HuddableApi.Spec spec) {
        this.id = id;
        this.label = label;
        this.spec = spec;
    }

    static ExternalPart fabric(Identifier id, Component label) {
        return new ExternalPart(id, label, null);
    }

    public static ExternalPart api(Identifier id, Component label, HuddableApi.Spec spec) {
        return new ExternalPart(id, label, spec);
    }

    boolean isFabric() {
        return spec == null;
    }

    @Override
    public String key() {
        return id.toString();
    }

    @Override
    public String label() {
        String name = label.getString();
        return isFabric() && measured == null ? name + " (not showing)" : name;
    }

    @Override
    public boolean movable() {
        return true;
    }

    @Override
    public boolean restyleable() {
        return false;
    }

    @Override
    public float baseScale() {
        return isFabric() ? 1f : (float) spec.baseScale().getAsDouble();
    }

    @Override
    public boolean resizable(ElementLayout layout) {
        return !isFabric() && spec.resizable();
    }

    @Override
    public int[] size(Minecraft mc, Geometry g, ElementLayout layout) {
        if (!resizable(layout)) return new int[] {g.width(), g.height()};
        int width = layout.width > 0 ? Math.max(spec.minWidth(), layout.width) : g.width();
        int height = layout.height > 0 ? Math.max(spec.minHeight(), layout.height) : g.height();
        return new int[] {width, height};
    }

    @Override
    public void resize(Minecraft mc, ElementLayout layout, int width, int height) {
        layout.width = Math.max(spec.minWidth(), width);
        layout.height = Math.max(spec.minHeight(), height);
    }

    @Override
    public Geometry geometry(Minecraft mc, int w, int h) {
        if (!isFabric()) {
            HuddableApi.Box box = defaultBox(w, h);
            return new Geometry(box.x(), box.y(), box.x(), box.y(), box.width(), box.height());
        }
        HuddableApi.Box box = measured;
        if (box == null) return new Geometry(w / 2, 20, w / 2 - 30, 20, 60, 20); // a spot to find it by
        return new Geometry(third(box.x(), box.width(), w), third(box.y(), box.height(), h),
            box.x(), box.y(), box.width(), box.height());
    }

    /** The box's start, middle or end, by which third of the screen its centre is in. */
    private static int third(int start, int size, int screen) {
        int centre = start + size / 2;
        if (centre < screen / 3) return start;
        if (centre > screen * 2 / 3) return start + size;
        return centre;
    }

    /** API elements: the unmoved box the mod gives. */
    public HuddableApi.Box defaultBox(int w, int h) {
        return spec.defaultBox().box(w, h);
    }

    /** API elements: where to draw this frame. */
    public HuddableApi.Placement placement(int w, int h) {
        ElementLayout layout = Profiles.current().get(this);
        Minecraft mc = Minecraft.getInstance();
        Geometry g = geometry(mc, w, h);
        int[] size = size(mc, g, layout);
        return new HuddableApi.Placement(g.x() + layout.dx, g.y() + layout.dy, size[0], size[1],
            HudWrapper.scale(this, layout), layout.hidden);
    }

    /**
     * Fabric layers: measures the box when the screen size changed, when nothing was found yet and it matters
     * ({@code wanted}), and once a second while the editor is open (the element may have grown or moved).
     */
    void measureIfNeeded(GuiGraphicsExtractor graphics, DeltaTracker delta, boolean wanted) {
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        long now = Util.getMillis();
        boolean resized = measured != null && (w != measuredWidth || h != measuredHeight);
        boolean retry = (measured == null && wanted || Profiles.editing()) && now - lastMeasure >= RETRY_MILLIS;
        if (!resized && !retry) return;
        lastMeasure = now;
        measuredWidth = w;
        measuredHeight = h;
        HuddableApi.Box box = measure(delta);
        if (box != null || resized) measured = box;
    }

    /** Draws the layer, unmoved, into a scratch render state and returns the bounds of what it drew. */
    private HuddableApi.Box measure(DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        GuiRenderState state = new GuiRenderState();
        try {
            original.extractRenderState(new GuiGraphicsExtractor(mc, state, -1, -1), delta);
        } catch (RuntimeException e) {
            HuddableClient.LOGGER.warn("Couldn't measure HUD element {}", id, e);
            return null;
        }
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        state.forEachElement(e -> union(b, e.bounds()), GuiRenderState.TraverseRange.ALL);
        state.forEachText(t -> union(b, t.bounds()));
        state.forEachItem(i -> union(b, i.bounds()));
        if (b[0] > b[2] || b[1] > b[3]) return null;
        return new HuddableApi.Box(b[0], b[1], b[2] - b[0], b[3] - b[1]);
    }

    private static void union(int[] b, ScreenRectangle r) {
        if (r == null || r.width() <= 0 || r.height() <= 0) return;
        b[0] = Math.min(b[0], r.left());
        b[1] = Math.min(b[1], r.top());
        b[2] = Math.max(b[2], r.right());
        b[3] = Math.max(b[3], r.bottom());
    }
}
