package huddable.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import huddable.LayoutEditorScreen;

/**
 * Mod Menu's "Configure" button for this mod, opening the layout editor. Loaded only by Mod Menu through the
 * "modmenu" entrypoint, so this is the only class that may touch its API.
 */
public final class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return LayoutEditorScreen::new;
    }
}
