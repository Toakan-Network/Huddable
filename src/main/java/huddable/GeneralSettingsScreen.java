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
        super(parent, Minecraft.getInstance().options, Component.translatable("huddable.general.title"));
        startOversample = GeneralSettings.get().fontOversample;
        oversample = startOversample;
    }

    @Override
    protected void addOptions() {
        list.addBig(new OptionInstance<>("huddable.general.font_quality",
            OptionInstance.cachedConstantTooltip(Component.translatable("huddable.general.font_quality.desc")),
            (caption, value) -> Options.genericValueLabel(caption, value == 0
                ? Component.translatable("huddable.general.font_quality.pack")
                : Component.translatable("huddable.general.font_quality.value", value)),
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
