package huddable;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

/**
 * How the whole HUD's size follows the screen, set per profile. The same three modes and maths as Skyblock
 * Shopping List's HUD sizing, so both mods size alike.
 */
public enum Sizing {
    /** Vanilla: elements follow Minecraft's GUI Scale. */
    GUI_SCALE("huddable.sizing.gui_scale"),
    /** Size follows the window height; 100% is the size at GUI Scale 2 in a 1080p window. */
    WINDOW("huddable.sizing.window"),
    /** The same size on screen at any GUI Scale. */
    FIXED("huddable.sizing.fixed");

    /** Lang key of the name shown in the editor. */
    private final String labelKey;

    Sizing(String labelKey) {
        this.labelKey = labelKey;
    }

    public String label() {
        return I18n.get(labelKey);
    }

    public Sizing next() {
        return values()[(ordinal() + 1) % values().length];
    }

    /** The multiplier on every element's scale for the current window. */
    public float factor() {
        if (this == GUI_SCALE) return 1f;
        Window window = Minecraft.getInstance().getWindow();
        return factor(window.getGuiScale(), window.getScreenHeight());
    }

    /** The multiplier for a window of {@code screenHeight} real pixels at {@code guiScale}. */
    public float factor(int guiScale, int screenHeight) {
        int scale = Math.max(1, guiScale);
        return switch (this) {
            case GUI_SCALE -> 1f;
            case FIXED -> 2f / scale;
            case WINDOW -> 2f * (screenHeight / 1080f) / scale;
        };
    }
}
