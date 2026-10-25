package uk.toakan.tad.huddable;

import java.io.File;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Settings for the whole game rather than one layout profile, in {@code config/huddable/settings.json}: which
 * profile is active, and the General Settings screen's options. Gson POJO; fields may be missing in old files,
 * so {@link #clamp} fills them in.
 */
public final class GeneralSettings {
    public static final int DEFAULT_FONT_OVERSAMPLE = 4;
    /** Font quality's highest setting; 0 means each resource pack's own oversample. */
    public static final int MAX_FONT_OVERSAMPLE = 16;

    private static final File FILE = new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), HuddableClient.MOD_ID), "settings.json");
    private static volatile GeneralSettings current;

    public String active = Profiles.DEFAULT_NAME;
    /** Oversample for every TrueType font, or 0 for the pack's own. Read on font-loading threads. */
    public volatile int fontOversample = DEFAULT_FONT_OVERSAMPLE;

    private GeneralSettings() {}

    /**
     * The settings, read from disk on first use. Fonts can load before client init runs, so whoever asks first
     * (Profiles at init, or the font quality mixin) loads them.
     */
    public static GeneralSettings get() {
        GeneralSettings s = current;
        if (s != null) return s;
        synchronized (GeneralSettings.class) {
            if (current == null) {
                GeneralSettings read = JsonFiles.read(FILE, GeneralSettings.class);
                current = (read != null ? read : new GeneralSettings()).clamp();
            }
            return current;
        }
    }

    /** Saves a copy, so later changes don't race the background write. */
    public static void save() {
        GeneralSettings s = get();
        GeneralSettings copy = new GeneralSettings();
        copy.active = s.active;
        copy.fontOversample = s.fontOversample;
        JsonFiles.writeAsync(FILE, () -> copy);
    }

    private GeneralSettings clamp() {
        if (active == null) active = Profiles.DEFAULT_NAME;
        fontOversample = Math.clamp(fontOversample, 0, MAX_FONT_OVERSAMPLE);
        return this;
    }
}
