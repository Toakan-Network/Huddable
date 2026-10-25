package uk.toakan.tad.huddable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * Huddable's config screen (Mod Menu's Configure): the HUD Designer for layouts and profiles, and General Settings
 * for what applies to the whole game rather than one profile.
 */
public class HuddableScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;

    private final Screen parent;

    public HuddableScreen(Screen parent) {
        super(Component.literal("Huddable"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int x = (width - BUTTON_WIDTH) / 2;
        int y = height / 2 - 34;
        addRenderableWidget(Button.builder(Component.translatable("huddable.hub.designer"), b -> minecraft.gui.setScreen(new LayoutEditorScreen(this)))
            .tooltip(Tooltip.create(Component.translatable("huddable.hub.designer.tooltip")))
            .bounds(x, y, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("huddable.hub.general"), b -> minecraft.gui.setScreen(new GeneralSettingsScreen(this)))
            .tooltip(Tooltip.create(Component.translatable("huddable.hub.general.tooltip")))
            .bounds(x, y + 24, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(x, y + 60, BUTTON_WIDTH, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);
        context.centeredText(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
