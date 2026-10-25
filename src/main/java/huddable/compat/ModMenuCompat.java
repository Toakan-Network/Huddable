package huddable.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import huddable.HuddableScreen;

/**
 * Mod Menu's "Configure" button for this mod, opening Huddable's screen (HUD Designer and General Settings).
 * Loaded only by Mod Menu through the "modmenu" entrypoint, so this is the only class that may touch its API.
 */
public final class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HuddableScreen::new;
    }
}
