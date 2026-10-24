package huddable;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
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
    }
}
