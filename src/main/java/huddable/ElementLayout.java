package huddable;

/**
 * Where and how one HUD element is drawn. Offsets are GUI pixels from the element's own anchor, so a layout
 * follows the screen when its size or the GUI scale changes. Gson POJO: fields may be missing in old or imported
 * files, so {@link #clamp} fills them in.
 */
public final class ElementLayout {
    public static final int MIN_SCALE = 25;
    public static final int MAX_SCALE = 400;
    public static final int MIN_BAR_WIDTH = 10;
    public static final int MAX_BAR_WIDTH = 400;
    public static final int MIN_BAR_HEIGHT = 2;
    public static final int MAX_BAR_HEIGHT = 40;

    public boolean hidden;
    public int dx;
    public int dy;
    /** Percent. */
    public int scale = 100;
    public Style style = Style.VANILLA;
    /** BAR only: size in GUI pixels before scaling. */
    public int barWidth = 81;
    public int barHeight = 9;
    /** ARGB; 0 means the element's default colour. */
    public int color;
    /** BAR only: draw the value as text on the bar. */
    public boolean showText = true;
    /** Resizable API elements only: size in the element's own units; 0 = its default. */
    public int width;
    public int height;

    public ElementLayout copy() {
        ElementLayout c = new ElementLayout();
        c.hidden = hidden;
        c.dx = dx;
        c.dy = dy;
        c.scale = scale;
        c.style = style;
        c.barWidth = barWidth;
        c.barHeight = barHeight;
        c.color = color;
        c.showText = showText;
        c.width = width;
        c.height = height;
        return c;
    }

    /** True when this draws exactly like vanilla, so it needn't be saved. */
    public boolean isDefault() {
        return !hidden && dx == 0 && dy == 0 && scale == 100 && style == Style.VANILLA && width == 0 && height == 0;
    }

    /** Puts every field in range; a style the element can't use falls back to vanilla. */
    public void clamp(HudPart part) {
        clamp(part.restyleable(), part.movable());
    }

    /** As {@link #clamp(HudPart)}, for another mod's element that isn't loaded now (kept, not restyleable). */
    public void clamp(boolean restyleable, boolean movable) {
        scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
        barWidth = Math.clamp(barWidth, MIN_BAR_WIDTH, MAX_BAR_WIDTH);
        barHeight = Math.clamp(barHeight, MIN_BAR_HEIGHT, MAX_BAR_HEIGHT);
        // Far enough to leave any screen, small enough that the maths can't overflow.
        dx = Math.clamp(dx, -10000, 10000);
        dy = Math.clamp(dy, -10000, 10000);
        width = Math.clamp(width, 0, 10000);
        height = Math.clamp(height, 0, 10000);
        if (style == null || !restyleable) style = Style.VANILLA;
        if (!movable) {
            dx = 0;
            dy = 0;
            scale = 100;
        }
    }

    public float scaleFactor() {
        return scale / 100f;
    }
}
