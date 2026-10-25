package huddable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

/**
 * Huddable's settings for the whole game ({@link GeneralSettings}), in vanilla's options list. Font quality only
 * takes effect when the fonts are rebuilt, so closing the screen after changing it reloads the resource packs once.
 */
public class GeneralSettingsScreen extends OptionsSubScreen {
    private final int startOversample;
    private int oversample;

    public GeneralSettingsScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options,Component.literal("Huddable: General Settings"));
        startOversample = GeneralSettings.get().fontOversample;
        oversample = startOversample;
    }

    @Override
    protected void addOptions() {
        list.addBig(new OptionInstance<>("Font quality",
            OptionInstance.cachedConstantTooltip(Component.literal(
                "How sharp TrueType fonts from resource packs are drawn (their oversample). Higher is sharper at "
                    + "large GUI scales but uses more video memory; Pack's own uses each pack's setting. Applies "
                    + "to every mod's fonts, not just Huddable's, and reloads the resource packs when you leave.")),
            (caption, value) -> Options.genericValueLabel(caption, Component.literal(value == 0 ? "Pack's own" : value + "x")),
            new OptionInstance.IntRange(0, GeneralSettings.MAX_FONT_OVERSAMPLE),
            oversample,
            value -> oversample = value));
    }

    @Override
    public void onClose() {
        super.onClose();
        if (oversample != startOversample) {
            GeneralSettings.get().fontOversample = oversample;
            GeneralSettings.save();
            minecraft.reloadResourcePacks();
        }
    }
}
