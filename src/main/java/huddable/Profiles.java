package huddable;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Named layouts. Each profile is {@code config/huddable/profiles/<name>.json}; {@code settings.json} names the
 * active one. Layouts are shared as codes through {@link Layout#toCode} and {@link Layout#fromCode}.
 */
public final class Profiles {
    public static final String DEFAULT_NAME = "Default";
    private static final int MAX_NAME_LENGTH = 32;

    private static final File DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), HuddableClient.MOD_ID);
    private static final File PROFILES_DIR = new File(DIR, "profiles");
    private static final File SETTINGS = new File(DIR, "settings.json");

    private static final class Settings {
        String active = DEFAULT_NAME;
    }

    private static String activeName = DEFAULT_NAME;
    private static Layout active = new Layout();
    /** The editor's working copy while it is open, drawn instead of the active layout. */
    private static volatile Layout preview;

    private Profiles() {}

    /** What the HUD draws now. */
    public static Layout current() {
        Layout p = preview;
        return p != null ? p : active;
    }

    public static String activeName() {
        return activeName;
    }

    public static Layout active() {
        return active;
    }

    public static void setPreview(Layout layout) {
        preview = layout;
    }

    public static void load() {
        Settings settings = JsonFiles.read(SETTINGS, Settings.class);
        activeName = settings != null && settings.active != null ? cleanName(settings.active) : DEFAULT_NAME;
        active = read(activeName);
    }

    private static Layout read(String name) {
        Layout layout = JsonFiles.read(file(name), Layout.class);
        return layout != null ? layout.clamp() : new Layout();
    }

    /** Every saved profile name, sorted, always including the active one. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        File[] files = PROFILES_DIR.listFiles((dir, n) -> n.endsWith(".json"));
        if (files != null) {
            for (File f : files) names.add(f.getName().substring(0, f.getName().length() - 5));
        }
        if (!names.contains(activeName)) names.add(activeName);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    /** Saves {@code layout} as the active profile. */
    public static void saveActive(Layout layout) {
        active = layout.copy().clamp();
        Layout snapshot = active.copy();
        JsonFiles.writeAsync(file(activeName), () -> snapshot);
    }

    public static void switchTo(String name) {
        activeName = cleanName(name);
        active = read(activeName);
        saveSettings();
    }

    /** Saves {@code layout} as a new profile with a free name based on {@code base}, and makes it active. */
    public static String create(String base, Layout layout) {
        activeName = uniqueName(base);
        saveSettings();
        saveActive(layout);
        return activeName;
    }

    /** Renames the active profile; returns false when the name is taken or the file can't be moved. */
    public static boolean renameActive(String newName) {
        String name = cleanName(newName);
        if (name.equalsIgnoreCase(activeName)) return true;
        if (file(name).exists()) return false;
        JsonFiles.flush();
        File old = file(activeName);
        if (old.exists() && !old.renameTo(file(name))) return false;
        activeName = name;
        saveSettings();
        if (!old.exists()) saveActive(active);
        return true;
    }

    /** Deletes the active profile and switches to another (a fresh Default when it was the last). */
    public static void deleteActive() {
        JsonFiles.flush();
        if (!file(activeName).delete() && file(activeName).exists()) {
            HuddableClient.LOGGER.warn("Could not delete profile {}", activeName);
        }
        List<String> rest = names();
        rest.remove(activeName);
        switchTo(rest.isEmpty() ? DEFAULT_NAME : rest.getFirst());
    }

    /** Letters, digits, spaces, '-' and '_' only, so any name is a safe file name. */
    static String cleanName(String name) {
        String clean = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _-]", "").trim();
        if (clean.length() > MAX_NAME_LENGTH) clean = clean.substring(0, MAX_NAME_LENGTH).trim();
        // Windows refuses these as file names.
        if (clean.isEmpty() || clean.toUpperCase(Locale.ROOT).matches("CON|PRN|AUX|NUL|COM\\d|LPT\\d")) return DEFAULT_NAME;
        return clean;
    }

    private static String uniqueName(String base) {
        String name = cleanName(base);
        if (!file(name).exists() && !name.equalsIgnoreCase(activeName)) return name;
        for (int i = 2; ; i++) {
            String suffix = " " + i;
            String candidate = cleanName(name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH - suffix.length())) + suffix);
            if (!file(candidate).exists()) return candidate;
        }
    }

    private static File file(String name) {
        return new File(PROFILES_DIR, name + ".json");
    }

    private static void saveSettings() {
        Settings settings = new Settings();
        settings.active = activeName;
        JsonFiles.writeAsync(SETTINGS, () -> settings);
    }
}
