package huddable;

import com.mojang.blaze3d.platform.InputConstants;
import huddable.api.HuddablePlugin;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HuddableClient implements ClientModInitializer {
    public static final String MOD_ID = "huddable";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Reads an InputConstants code at runtime. The codes differ between Minecraft versions (26.3 moved
     * from GLFW to SDL), and javac would inline a direct reference, so one source tree works on both this way.
     */
    public static int inputCode(String name) {
        try {
            return InputConstants.class.getField(name).getInt(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Missing InputConstants." + name, e);
        }
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
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> HudDiscovery.discover());
    }
}
