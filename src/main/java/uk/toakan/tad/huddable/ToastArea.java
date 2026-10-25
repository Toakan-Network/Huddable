package uk.toakan.tad.huddable;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

/**
 * Where toasts sit, for {@link Element#TOASTS}. Vanilla stacks them in the top-right corner. With Fancy Notify
 * installed its notifications replace most toasts: it translates to a padded screen corner (its {@code anchor} and
 * {@code padding} settings) and stacks up to {@code maxAmount} notifications {@code notificationsWidth} wide from
 * there, so its corner is the anchor. Its settings are read from its own config file, rechecked at most once a
 * second as the anchor is needed every frame; the height is a typical stack, as each notification sizes itself.
 */
final class ToastArea {
    private static final String FANCY_NOTIFY = "fancynotify";
    /** Vanilla toasts are 160 wide by default and 32 tall; room for three. */
    private static final int VANILLA_WIDTH = 160;
    private static final int VANILLA_HEIGHT = 96;
    /** A typical Fancy Notify notification's height plus its gap. */
    private static final int NOTIFICATION_HEIGHT = 36;
    private static final long RECHECK_MILLIS = 1000;

    private static boolean left;
    private static boolean top = true;
    private static int padding;
    private static int width = VANILLA_WIDTH;
    private static int height = VANILLA_HEIGHT;
    private static long lastCheck = -RECHECK_MILLIS;
    private static long lastModified = -1;

    private ToastArea() {}

    static HudPart.Geometry geometry(int w, int h) {
        refresh();
        int anchorX = left ? padding : w - padding;
        int anchorY = top ? padding : h - padding;
        int x = left ? anchorX : anchorX - width;
        int y = top ? anchorY : anchorY - height;
        return new HudPart.Geometry(anchorX, anchorY, x, y, width, height);
    }

    private static void refresh() {
        long now = Util.getMillis();
        if (now - lastCheck < RECHECK_MILLIS) return;
        lastCheck = now;
        if (!FabricLoader.getInstance().isModLoaded(FANCY_NOTIFY)) return;
        Path file = FabricLoader.getInstance().getConfigDir().resolve(FANCY_NOTIFY).resolve("general.json");
        try {
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified == lastModified) return;
            lastModified = modified;
            JsonObject config;
            try (Reader reader = Files.newBufferedReader(file)) {
                config = JsonParser.parseReader(reader).getAsJsonObject();
            }
            String anchor = config.has("anchor") ? config.get("anchor").getAsString() : "TOP_RIGHT";
            left = anchor.endsWith("LEFT");
            top = anchor.startsWith("TOP");
            padding = config.has("padding") ? config.get("padding").getAsInt() : 0;
            width = config.has("notificationsWidth") ? config.get("notificationsWidth").getAsInt() : VANILLA_WIDTH;
            int count = config.has("maxAmount") ? config.get("maxAmount").getAsInt() : 3;
            height = Math.max(1, Math.min(count, 6)) * NOTIFICATION_HEIGHT;
        } catch (IOException | RuntimeException e) {
            // Not written yet or unreadable: keep the last (or vanilla) area.
        }
    }
}
