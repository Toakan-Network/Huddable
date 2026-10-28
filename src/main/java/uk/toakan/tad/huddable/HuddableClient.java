package uk.toakan.tad.huddable;

import com.mojang.blaze3d.platform.InputConstants;
import uk.toakan.tad.huddable.api.HuddablePlugin;
import uk.toakan.tad.huddable.compat.MeowddingCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HuddableClient implements ClientModInitializer {
    public static final String MOD_ID = "huddable";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final java.util.Map<String, Integer> INPUT_CODES = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Reads an InputConstants code at runtime. The codes differ between Minecraft versions (26.3 moved
     * from GLFW to SDL), and javac would inline a direct reference, so one source tree works on both this way.
     */
    public static int inputCode(String name) {
        // Looked up once per name: callers ask on every click.
        return INPUT_CODES.computeIfAbsent(name, n -> {
            try {
                return InputConstants.class.getField(n).getInt(null);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Missing InputConstants." + n, e);
            }
        });
    }

    @Override
    public void onInitializeClient() {
        Profiles.load();
        HudWrapper.registerAll();
        // Other mods' API elements first, so the layers they claim aren't also wrapped by discovery.
        for (EntrypointContainer<HuddablePlugin> plugin : FabricLoader.getInstance().getEntrypointContainers(MOD_ID, HuddablePlugin.class)) {
            try {
                plugin.getEntrypoint().register();
            } catch (RuntimeException | LinkageError e) {
                LOGGER.error("Huddable plugin from {} failed", plugin.getProvider().getMetadata().getId(), e);
            }
        }
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            HudDiscovery.discover();
            MeowddingCompat.start();
        });
        // Saves run on a background thread; make sure the last ones reach the disk before the game closes.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> JsonFiles.flush());
        // After the fonts reload (resource packs, F3+T, font quality), which fonts exist and their widths may change.
        Identifier fontsReloaded = Identifier.fromNamespaceAndPath(MOD_ID, "hud_fonts");
        ResourceLoader resources = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        resources.registerReloadListener(fontsReloaded, (ResourceManagerReloadListener) manager -> HudFonts.reloaded());
        resources.addListenerOrdering(ResourceReloaderKeys.Client.FONTS, fontsReloaded);
        // Which packs have frame art, and the art read from them, may change too.
        resources.registerReloadListener(Identifier.fromNamespaceAndPath(MOD_ID, "hud_frames"),
            (ResourceManagerReloadListener) manager -> HudFrames.reloaded());
    }
}
