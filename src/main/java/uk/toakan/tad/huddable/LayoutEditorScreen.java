package uk.toakan.tad.huddable;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * The layout editor (HUD Designer), opened from {@link HuddableScreen}. In a world the live HUD shows through it:
 * drag an element to move it (it snaps to the screen edges and centre and to other elements; Shift places it
 * freely), drag a corner to scale it, drag an edge grip to resize it (chat, Bar-style bars, and other mods'
 * elements that lay themselves out), right-click it for its options (hide, style, colour, font). The Font button
 * sets the profile's font, which every text element uses unless it has its own. Other mods' elements ({@link Parts}) are placed the
 * same way. On the title screen the elements are drawn as labelled boxes. Changes show at once; Done saves them
 * to the active profile, Cancel or Esc drops them (chat's size, a vanilla option, is put back too).
 * The controls are a small window (left edge by default) that drags by its title, so nothing on the HUD is out
 * of reach; Elements opens a list to show or hide every element.
 * Profile actions (switch, new, rename, delete, import) save the current edits first.
 */
public class LayoutEditorScreen extends Screen {
    private static final int GOLD = 0xFFFFAA00;
    private static final int HEADER_BG = 0xE0101010;
    private static final int HEADER_BORDER = 0xFF555555;
    private static final int PANEL_BG = 0xE0181818;
    private static final int SNAP_GUIDE = 0xFF55FFFF;
    /** The controls are a small window, so they never cover HUD elements the player wants to grab. */
    private static final int TOOLBAR_WIDTH = 72;
    /** Title strip at the top of the controls; drag it to move them. */
    private static final int GRIP = 12;
    private static final int TOOLBAR_BUTTONS = 12;
    private static final int TOOLBAR_HEIGHT = GRIP + TOOLBAR_BUTTONS * 22 + 2;
    private static final int HANDLE = 6;
    private static final int PANEL_WIDTH = 130;
    private static final int FONT_PANEL_WIDTH = 170;
    private static final int ROW = 22;
    private static final int SCALE_STEP = 10;
    private static final int SIZE_STEP = 5;
    /** Colour choices; 0 is the element's own default. */
    private static final int[] PALETTE = {0, 0xFFD83A3A, 0xFFE8C33A, 0xFF4CC15A, 0xFF5AA0E8, 0xFFB46CE8, 0xFFFFFFFF, 0xFF9AA0A6};
    private static final long STATUS_MILLIS = 3000;

    private final Screen parent;
    private final int mouseLeft = HuddableClient.inputCode("MOUSE_BUTTON_LEFT");
    private final int mouseRight = HuddableClient.inputCode("MOUSE_BUTTON_RIGHT");
    private Layout working;
    /** The element whose options panel is open, or null. */
    private HudPart selected;
    /** The element being dragged, scaled or resized, or null. */
    private HudPart active;
    private boolean scaling;
    /** The edge grip being dragged, or null when moving or scaling. */
    private Edge resizing;
    private int startMouseX, startMouseY, startDx, startDy, startScale;
    private HudWrapper.Box startBox;
    private int guideX = -1;
    private int guideY = -1;
    private boolean renaming;
    private EditBox nameBox;
    private boolean confirmDelete;
    private String status;
    private long statusUntil;
    private int panelX, panelY, panelWidth, panelHeight;
    /** The Elements panel: every element with a show/hide toggle. */
    private boolean showList;
    /** The font list: for {@link #fontTarget}, or the profile font when that is null. */
    private boolean pickingFont;
    private HudPart fontTarget;
    private int fontPage;
    /** Where the controls were left, kept while the game runs; -1 = default (left edge, halfway down). */
    private static int toolbarX = -1;
    private static int toolbarY = -1;
    private boolean movingToolbar;
    private int grabX, grabY;
    /** Chat's size options when the editor opened, put back on Cancel; chat resizes through them. */
    private final double chatWidth;
    private final double chatHeight;
    /** Set by Done, so closing keeps chat's new size. */
    private boolean saved;

    /** Mid-edge resize grips. */
    private enum Edge { LEFT, RIGHT, TOP, BOTTOM }

    public LayoutEditorScreen(Screen parent) {
        super(Component.literal("Huddable"));
        this.parent = parent;
        Options options = Minecraft.getInstance().options;
        chatWidth = options.chatWidth().get();
        chatHeight = options.chatHeightUnfocused().get();
        loadWorking();
    }

    private void loadWorking() {
        working = Profiles.active().copy();
        Profiles.setPreview(working);
        selected = null;
        pickingFont = false;
    }

    // ---- Widgets ----

    @Override
    protected void init() {
        // A rebuild (resize, toolbar moved) keeps a half-typed name.
        String draft = renaming && nameBox != null ? nameBox.getValue() : Profiles.activeName();
        if (toolbarX < 0) {
            toolbarX = 4;
            toolbarY = (height - TOOLBAR_HEIGHT) / 2;
        }
        toolbarX = Math.clamp(toolbarX, 0, Math.max(0, width - TOOLBAR_WIDTH));
        toolbarY = Math.clamp(toolbarY, 0, Math.max(0, height - TOOLBAR_HEIGHT));
        int x = toolbarX + 2;
        int y = toolbarY + GRIP;
        if (renaming) {
            nameBox = new EditBox(font, x + 1, y + 1, TOOLBAR_WIDTH - 6, 18, Component.translatable("huddable.editor.profile_name"));
            nameBox.setMaxLength(32);
            nameBox.setValue(draft);
            addRenderableWidget(nameBox);
            setInitialFocus(nameBox);
        } else {
            addRenderableWidget(Button.builder(Component.literal(Profiles.activeName()), b -> nextProfile())
                .tooltip(Tooltip.create(Component.translatable("huddable.editor.profile.tooltip", Profiles.activeName())))
                .bounds(x, y, TOOLBAR_WIDTH - 4, 20).build());
        }
        y += ROW;
        y = toolbarButton(x, y, I18n.get("huddable.editor.elements"), I18n.get("huddable.editor.elements.tooltip"), () -> {
            showList = !showList;
            pickingFont = false;
            selected = null;
            rebuildWidgets();
        });
        y = toolbarButton(x, y, I18n.get("huddable.editor.font"),
            I18n.get("huddable.editor.font.tooltip", fontName(working.font, false)), () -> {
            boolean open = pickingFont && fontTarget == null;
            showList = false;
            selected = null;
            openFontList(null);
            pickingFont = !open;
            rebuildWidgets();
        });
        y = toolbarButton(x, y, I18n.get("huddable.editor.size", working.sizing.label()),
            I18n.get("huddable.editor.size.tooltip"), () -> {
            working.sizing = working.sizing.next();
            rebuildWidgets();
        });
        y = toolbarButton(x, y, I18n.get("huddable.editor.new"), I18n.get("huddable.editor.new.tooltip"), this::newProfile);
        y = toolbarButton(x, y, I18n.get(renaming ? "huddable.editor.rename.ok" : "huddable.editor.rename"),
            I18n.get("huddable.editor.rename.tooltip"), this::rename);
        y = toolbarButton(x, y, I18n.get(confirmDelete ? "huddable.editor.delete.confirm" : "huddable.editor.delete"),
            I18n.get("huddable.editor.delete.tooltip"), this::delete);
        y = toolbarButton(x, y, I18n.get("huddable.editor.export"), I18n.get("huddable.editor.export.tooltip"), this::exportLayout);
        y = toolbarButton(x, y, I18n.get("huddable.editor.import"), I18n.get("huddable.editor.import.tooltip"), this::importLayout);
        y = toolbarButton(x, y, I18n.get("huddable.editor.reset"), I18n.get("huddable.editor.reset.tooltip"), this::resetAll);
        y = toolbarButton(x, y, I18n.get("huddable.editor.cancel"), I18n.get("huddable.editor.cancel.tooltip"), this::onClose);
        toolbarButton(x, y, I18n.get("huddable.editor.done"), I18n.get("huddable.editor.done.tooltip"), this::done);
        if (showList) buildList();
        else if (pickingFont) buildFontList();
        else if (selected != null) buildPanel();
    }

    private int toolbarButton(int x, int y, String label, String tip, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> action.run())
            .tooltip(Tooltip.create(Component.literal(tip))).bounds(x, y, TOOLBAR_WIDTH - 4, 20).build());
        return y + ROW;
    }

    /** Puts a panel of {@code w} beside the controls, on whichever side has room. */
    private void placePanel(int w) {
        panelWidth = w;
        panelX = toolbarX + TOOLBAR_WIDTH + w + 2 <= width ? toolbarX + TOOLBAR_WIDTH + 2 : toolbarX - w - 2;
        panelY = toolbarY;
    }

    /** Every element in two columns; a click shows or hides it. Hidden ones are struck through. */
    private void buildList() {
        int columnWidth = 100;
        List<HudPart> parts = Parts.all();
        int rows = (parts.size() + 1) / 2;
        placePanel(2 * columnWidth + 10);
        for (int i = 0; i < parts.size(); i++) {
            HudPart element = parts.get(i);
            boolean hidden = working.get(element).hidden;
            String name = font.plainSubstrByWidth(element.label(), columnWidth - 8);
            Component label = hidden
                ? Component.literal(name).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH)
                : Component.literal(name);
            int x = panelX + 4 + (i / rows) * (columnWidth + 2);
            int y = panelY + 16 + (i % rows) * ROW;
            addRenderableWidget(Button.builder(label, b -> {
                working.edit(element).hidden = !hidden;
                rebuildWidgets();
            }).tooltip(Tooltip.create(Component.translatable(hidden ? "huddable.editor.list.hidden" : "huddable.editor.list.shown",
                element.label())))
                .bounds(x, y, columnWidth, 20).build());
        }
        int y = panelY + 16 + rows * ROW;
        addRenderableWidget(Button.builder(Component.translatable("huddable.editor.list.show_all"), b -> {
            for (HudPart element : Parts.all()) {
                if (working.get(element).hidden) working.edit(element).hidden = false;
            }
            rebuildWidgets();
        }).bounds(panelX + 4, y, columnWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("huddable.editor.close"), b -> {
            showList = false;
            rebuildWidgets();
        }).bounds(panelX + 6 + columnWidth, y, columnWidth, 20).build());
        panelHeight = y + ROW - panelY;
    }

    /** The options panel for {@link #selected}, beside the controls. */
    private void buildPanel() {
        ElementLayout l = working.get(selected);
        placePanel(PANEL_WIDTH);
        int x = panelX + 4;
        int w = PANEL_WIDTH - 8;
        int y = panelY + 16;
        y = panelButton(x, y, w, I18n.get(l.hidden ? "huddable.editor.panel.show" : "huddable.editor.panel.hide"),
            () -> working.edit(selected).hidden = !l.hidden);
        if (selected.movable()) {
            addRenderableWidget(Button.builder(Component.literal("-"), b -> changeScale(-SCALE_STEP)).bounds(x, y, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal("+"), b -> changeScale(SCALE_STEP)).bounds(x + w - 20, y, 20, 20).build());
            y += ROW;
        }
        if (selected.restyleable()) {
            y = panelButton(x, y, w, I18n.get("huddable.editor.panel.style", l.style.label()),
                () -> working.edit(selected).style = l.style.next());
            if (l.style != Style.VANILLA) y = panelButton(x, y, w, I18n.get("huddable.editor.panel.colour"), this::nextColour);
            if (l.style == Style.BAR) {
                addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBar(-SIZE_STEP, 0)).bounds(x, y, 20, 20).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBar(SIZE_STEP, 0)).bounds(x + w - 20, y, 20, 20).build());
                y += ROW;
                addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBar(0, -1)).bounds(x, y, 20, 20).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBar(0, 1)).bounds(x + w - 20, y, 20, 20).build());
                y += ROW;
                y = panelButton(x, y, w, I18n.get(l.showText ? "huddable.editor.panel.text.on" : "huddable.editor.panel.text.off"),
                    () -> working.edit(selected).showText = !l.showText);
            }
        }
        if (HudFonts.fontable(selected, l)) {
            y = panelButton(x, y, w, I18n.get("huddable.editor.panel.font", fontName(l.font, true)), () -> openFontList(selected));
        }
        y = panelButton(x, y, w, I18n.get("huddable.editor.panel.reset"), () -> working.reset(selected));
        y = panelButton(x, y, w, I18n.get("huddable.editor.close"), () -> selected = null);
        panelHeight = y - panelY + 2;
    }

    private void openFontList(HudPart target) {
        pickingFont = true;
        fontTarget = target;
        fontPage = 0;
    }

    /** A font choice's name: {@code null} is the profile font for an element, vanilla's for the profile. */
    private static String fontName(String id, boolean element) {
        if (id == null) return I18n.get(element ? "huddable.editor.font.profile" : "huddable.editor.font.vanilla");
        return id.equals(HudFonts.DEFAULT_FONT) ? I18n.get("huddable.editor.font.vanilla") : id;
    }

    /**
     * Every font the resource packs provide, each shown in itself, a page at a time; the current choice is gold.
     * For an element the first choice is the profile font. Back returns to the element's options.
     */
    private void buildFontList() {
        placePanel(FONT_PANEL_WIDTH);
        int x = panelX + 4;
        int w = FONT_PANEL_WIDTH - 8;
        List<String> available = HudFonts.availableFonts();
        List<String> choices = new ArrayList<>();
        if (fontTarget != null) choices.add(null);
        choices.addAll(available);
        String current = fontTarget != null ? working.get(fontTarget).font
            : working.font != null ? working.font : HudFonts.DEFAULT_FONT;
        if (current != null && !choices.contains(current)) choices.add(current); // set, but its pack is off now
        int perPage = Math.max(1, (height - panelY - 16 - 2 * ROW - 2) / ROW);
        int pages = (choices.size() + perPage - 1) / perPage;
        fontPage = Math.clamp(fontPage, 0, pages - 1);
        int y = panelY + 16;
        for (int i = fontPage * perPage; i < Math.min(choices.size(), (fontPage + 1) * perPage); i++) {
            String id = choices.get(i);
            addRenderableWidget(Button.builder(fontLabel(id, id == null ? current == null : id.equals(current)), b -> {
                setFont(id);
                rebuildWidgets();
            }).tooltip(Tooltip.create(Component.literal(fontTip(id, available)))).bounds(x, y, w, 20).build());
            y += ROW;
        }
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                fontPage--;
                rebuildWidgets();
            }).bounds(x, y, 20, 20).build()).active = fontPage > 0;
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                fontPage++;
                rebuildWidgets();
            }).bounds(x + w - 20, y, 20, 20).build()).active = fontPage < pages - 1;
            y += ROW;
        }
        y = panelButton(x, y, w, I18n.get(fontTarget != null ? "huddable.editor.back" : "huddable.editor.close"),
            () -> pickingFont = false);
        panelHeight = y - panelY + 2;
    }

    private Component fontLabel(String id, boolean chosen) {
        String name = fontName(id, fontTarget != null);
        MutableComponent label = Component.literal(chosen ? "> " + name : name);
        // A pack's font is shown in itself, so the list previews it.
        if (id != null && !id.equals(HudFonts.DEFAULT_FONT)) {
            FontDescription look = new FontDescription.Resource(Identifier.parse(id));
            label.withStyle(style -> style.withFont(look));
        }
        return chosen ? label.withStyle(ChatFormatting.GOLD) : label;
    }

    private String fontTip(String id, List<String> available) {
        if (id == null) return I18n.get("huddable.editor.font.profile.tooltip");
        if (id.equals(HudFonts.DEFAULT_FONT)) {
            return I18n.get(fontTarget != null ? "huddable.editor.font.vanilla.element_tooltip" : "huddable.editor.font.vanilla.tooltip");
        }
        return available.contains(id) ? id : I18n.get("huddable.editor.font.pack_off", id);
    }

    private void setFont(String id) {
        if (fontTarget == null) {
            working.font = HudFonts.DEFAULT_FONT.equals(id) ? null : id;
            return;
        }
        working.edit(fontTarget).font = id;
        // Text in another font has another width.
        if (fontTarget.movable()) keepOnScreen(fontTarget);
    }

    /** A full-width panel button; the panel is rebuilt after it runs, so labels show the new value. */
    private int panelButton(int x, int y, int w, String label, Runnable action) {
        addRenderableWidget(Button.builder(Component.literal(label), b -> {
            action.run();
            rebuildWidgets();
        }).bounds(x, y, w, 20).build());
        return y + ROW;
    }

    private void changeScale(int step) {
        ElementLayout l = working.edit(selected);
        l.scale = Math.clamp(l.scale + step, ElementLayout.MIN_SCALE, ElementLayout.MAX_SCALE);
        keepOnScreen(selected);
    }

    private void changeBar(int dw, int dh) {
        ElementLayout l = working.edit(selected);
        l.barWidth = Math.clamp(l.barWidth + dw, ElementLayout.MIN_BAR_WIDTH, ElementLayout.MAX_BAR_WIDTH);
        l.barHeight = Math.clamp(l.barHeight + dh, ElementLayout.MIN_BAR_HEIGHT, ElementLayout.MAX_BAR_HEIGHT);
        keepOnScreen(selected);
    }

    private void nextColour() {
        ElementLayout l = working.edit(selected);
        int i = 0;
        while (i < PALETTE.length && PALETTE[i] != l.color) i++;
        l.color = PALETTE[(i + 1) % PALETTE.length];
    }

    // ---- Profiles ----

    private void saveWorking() {
        Profiles.saveActive(working);
    }

    private void nextProfile() {
        saveWorking();
        List<String> names = Profiles.names();
        int i = names.indexOf(Profiles.activeName());
        Profiles.switchTo(names.get((i + 1) % names.size()));
        loadWorking();
        rebuildWidgets();
    }

    private void newProfile() {
        saveWorking();
        Profiles.create("Profile", new Layout());
        loadWorking();
        rebuildWidgets();
    }

    private void rename() {
        if (renaming && !Profiles.renameActive(nameBox.getValue())) setStatus(I18n.get("huddable.editor.status.name_taken"));
        renaming = !renaming;
        rebuildWidgets();
    }

    private void delete() {
        if (confirmDelete) {
            Profiles.deleteActive();
            loadWorking();
            setStatus(I18n.get("huddable.editor.status.deleted"));
        }
        confirmDelete = !confirmDelete;
        rebuildWidgets();
    }

    private void exportLayout() {
        minecraft.keyboardHandler.setClipboard(working.toCode());
        setStatus(I18n.get("huddable.editor.status.exported"));
    }

    private void importLayout() {
        Layout imported = Layout.fromCode(minecraft.keyboardHandler.getClipboard());
        if (imported == null) {
            setStatus(I18n.get("huddable.editor.status.no_code"));
            return;
        }
        saveWorking();
        String name = Profiles.create("Imported", imported);
        loadWorking();
        rebuildWidgets();
        setStatus(I18n.get("huddable.editor.status.imported", name));
    }

    private void resetAll() {
        working.elements.clear();
        working.sizing = Sizing.GUI_SCALE;
        working.font = null;
        selected = null;
        pickingFont = false;
        rebuildWidgets();
    }

    private void done() {
        saved = true;
        saveWorking();
        if (chatResized()) minecraft.options.save();
        close();
    }

    private boolean chatResized() {
        return minecraft.options.chatWidth().get() != chatWidth || minecraft.options.chatHeightUnfocused().get() != chatHeight;
    }

    private void setStatus(String message) {
        status = message;
        statusUntil = Util.getMillis() + STATUS_MILLIS;
    }

    // ---- Drawing ----

    private HudWrapper.Box boxOf(HudPart element) {
        return HudWrapper.box(element, working.get(element), minecraft, width, height);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        boolean live = minecraft.level != null;
        context.fill(0, 0, width, height, live ? 0x30000000 : 0xC0101010);
        HudPart hovered = active != null ? active : elementAt(mouseX, mouseY);
        for (HudPart element : Parts.all()) {
            ElementLayout l = working.get(element);
            HudWrapper.Box b = boxOf(element);
            if (!live) context.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x60406080);
            if (l.hidden) context.fill(b.x(), b.y(), b.x() + b.width(), b.y() + b.height(), 0x40FF0000);
            boolean focus = element == selected || element == active;
            int outline = focus ? 0xFFFFDD00 : l.hidden ? 0xFFFF5555 : element == hovered ? 0xFFFFFFFF : 0x66FFFFFF;
            context.outline(b.x(), b.y(), b.width(), b.height(), outline);
            if (element.movable() && (focus || element == hovered)) {
                for (int[] c : corners(b)) context.fill(c[0] - HANDLE / 2, c[1] - HANDLE / 2, c[0] + HANDLE / 2, c[1] + HANDLE / 2, GOLD);
                // White grips mid-edge (resize), gold squares on the corners (scale).
                if (element.resizable(l)) {
                    for (Edge edge : Edge.values()) {
                        int[] g = grip(b, edge);
                        context.fill(g[0] - HANDLE / 2, g[1] - HANDLE / 2, g[0] + HANDLE / 2, g[1] + HANDLE / 2, 0xFFFFFFFF);
                    }
                }
            }
            if (!live || focus || element == hovered || l.hidden) {
                String label = l.hidden ? I18n.get("huddable.editor.element.hidden", element.label()) : element.label();
                int labelY = b.y() >= 10 ? b.y() - 10 : b.y() + b.height() + 2;
                context.text(font, label, b.x(), labelY, 0xFFFFFFFF, true);
            }
        }
        if (active != null && !scaling && resizing == null) {
            if (guideX >= 0) context.fill(guideX, 0, guideX + 1, height, SNAP_GUIDE);
            if (guideY >= 0) context.fill(0, guideY, width, guideY + 1, SNAP_GUIDE);
        }

        context.fill(toolbarX, toolbarY, toolbarX + TOOLBAR_WIDTH, toolbarY + TOOLBAR_HEIGHT, HEADER_BG);
        context.outline(toolbarX, toolbarY, TOOLBAR_WIDTH, TOOLBAR_HEIGHT, movingToolbar ? 0xFFFFDD00 : HEADER_BORDER);
        context.text(font, "Huddable", toolbarX + 4, toolbarY + 2, GOLD, false);
        // Grip dots: the title strip drags the controls.
        context.fill(toolbarX + TOOLBAR_WIDTH - 12, toolbarY + 4, toolbarX + TOOLBAR_WIDTH - 4, toolbarY + 5, 0xFF888888);
        context.fill(toolbarX + TOOLBAR_WIDTH - 12, toolbarY + 7, toolbarX + TOOLBAR_WIDTH - 4, toolbarY + 8, 0xFF888888);
        String hint = I18n.get(live ? "huddable.editor.hint.live" : "huddable.editor.hint.preview");
        if (status != null && Util.getMillis() < statusUntil) hint = status;
        context.text(font, font.plainSubstrByWidth(hint, width - 8), 4, height - 12, GOLD, true);

        if (showList) {
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
            context.outline(panelX, panelY, panelWidth, panelHeight, HEADER_BORDER);
            context.text(font, Component.translatable("huddable.editor.list.title"), panelX + 4, panelY + 4, GOLD, false);
        } else if (pickingFont) {
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
            context.outline(panelX, panelY, panelWidth, panelHeight, HEADER_BORDER);
            String title = fontTarget != null ? I18n.get("huddable.editor.panel.font", fontTarget.label())
                : I18n.get("huddable.editor.font.profile_title");
            context.text(font, font.plainSubstrByWidth(title, panelWidth - 8), panelX + 4, panelY + 4, GOLD, false);
        } else if (selected != null) {
            ElementLayout l = working.get(selected);
            context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_BG);
            context.outline(panelX, panelY, panelWidth, panelHeight, HEADER_BORDER);
            context.text(font, font.plainSubstrByWidth(selected.label(), panelWidth - 8), panelX + 4, panelY + 4, GOLD, false);
            // Values between the -/+ buttons, on the rows buildPanel laid out.
            int y = panelY + 16 + ROW;
            if (selected.movable()) {
                centred(context, I18n.get("huddable.editor.panel.scale", l.scale), y);
                y += ROW;
            }
            if (selected.restyleable()) {
                y += ROW; // the Style row
                if (l.style != Style.VANILLA) {
                    // Swatch at the right end of the Colour button.
                    int colour = l.color != 0 ? l.color : BarRenderers.defaultColor((Element) selected);
                    int sx = panelX + panelWidth - 4 - 14;
                    context.outline(sx - 1, y + 5, 10, 10, 0xFF000000);
                    context.fill(sx, y + 6, sx + 8, y + 14, colour);
                    y += ROW;
                }
                if (l.style == Style.BAR) {
                    centred(context, I18n.get("huddable.editor.panel.width", l.barWidth), y);
                    centred(context, I18n.get("huddable.editor.panel.height", l.barHeight), y + ROW);
                }
            }
        }
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void centred(GuiGraphicsExtractor context, String text, int rowY) {
        context.text(font, text, panelX + (panelWidth - font.width(text)) / 2, rowY + 6, 0xFFFFFFFF, false);
    }

    private static int[][] corners(HudWrapper.Box b) {
        int r = b.x() + b.width();
        int bottom = b.y() + b.height();
        return new int[][] {{b.x(), b.y()}, {r, b.y()}, {b.x(), bottom}, {r, bottom}};
    }

    private static int[] grip(HudWrapper.Box b, Edge edge) {
        int midX = b.x() + b.width() / 2;
        int midY = b.y() + b.height() / 2;
        return switch (edge) {
            case LEFT -> new int[] {b.x(), midY};
            case RIGHT -> new int[] {b.x() + b.width(), midY};
            case TOP -> new int[] {midX, b.y()};
            case BOTTOM -> new int[] {midX, b.y() + b.height()};
        };
    }

    private static boolean near(int mouseX, int mouseY, int[] point) {
        return Math.abs(mouseX - point[0]) <= HANDLE / 2 && Math.abs(mouseY - point[1]) <= HANDLE / 2;
    }

    // ---- Mouse ----

    /** The smallest element box under the mouse, so a small element on top of a big one can still be picked. */
    private HudPart elementAt(int mouseX, int mouseY) {
        if (inToolbar(mouseX, mouseY) || inPanel(mouseX, mouseY)) return null;
        HudPart best = null;
        long bestArea = Long.MAX_VALUE;
        for (HudPart element : Parts.all()) {
            HudWrapper.Box b = boxOf(element);
            boolean inside = mouseX >= b.x() - HANDLE / 2 && mouseX <= b.x() + b.width() + HANDLE / 2
                && mouseY >= b.y() - HANDLE / 2 && mouseY <= b.y() + b.height() + HANDLE / 2;
            long area = (long) b.width() * b.height();
            if (inside && area < bestArea) {
                best = element;
                bestArea = area;
            }
        }
        return best;
    }

    private boolean inToolbar(int mouseX, int mouseY) {
        return mouseX >= toolbarX && mouseX < toolbarX + TOOLBAR_WIDTH && mouseY >= toolbarY && mouseY < toolbarY + TOOLBAR_HEIGHT;
    }

    private boolean inPanel(int mouseX, int mouseY) {
        return (showList || pickingFont || selected != null) && mouseX >= panelX && mouseX < panelX + panelWidth
            && mouseY >= panelY && mouseY < panelY + panelHeight;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent ctx, boolean doubleClick) {
        if (super.mouseClicked(ctx, doubleClick)) return true;
        int mouseX = (int) ctx.x();
        int mouseY = (int) ctx.y();
        if (inToolbar(mouseX, mouseY)) {
            if (ctx.button() == mouseLeft && mouseY < toolbarY + GRIP) {
                movingToolbar = true;
                grabX = mouseX - toolbarX;
                grabY = mouseY - toolbarY;
            }
            return true;
        }
        if (inPanel(mouseX, mouseY)) return true;
        HudPart element = elementAt(mouseX, mouseY);
        if (ctx.button() == mouseRight) {
            if (element != null) showList = false;
            pickingFont = false;
            selected = element;
            rebuildWidgets();
            return true;
        }
        if (ctx.button() != mouseLeft || element == null) return false;
        if (selected != null && selected != element) {
            selected = element;
            pickingFont = false;
            rebuildWidgets();
        }
        if (!element.movable()) return true;
        HudWrapper.Box b = boxOf(element);
        ElementLayout l = working.get(element);
        active = element;
        scaling = false;
        resizing = null;
        for (int[] c : corners(b)) {
            if (near(mouseX, mouseY, c)) scaling = true;
        }
        if (!scaling && element.resizable(l)) {
            for (Edge edge : Edge.values()) {
                if (near(mouseX, mouseY, grip(b, edge))) resizing = edge;
            }
        }
        startMouseX = mouseX;
        startMouseY = mouseY;
        startDx = l.dx;
        startDy = l.dy;
        startScale = l.scale;
        startBox = b;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent ctx, double deltaX, double deltaY) {
        if (movingToolbar) {
            toolbarX = (int) ctx.x() - grabX;
            toolbarY = (int) ctx.y() - grabY;
            rebuildWidgets(); // the buttons and panel follow
            return true;
        }
        if (active == null) return super.mouseDragged(ctx, deltaX, deltaY);
        int mouseX = (int) ctx.x();
        int mouseY = (int) ctx.y();
        ElementLayout l = working.edit(active);
        if (resizing != null) {
            resize(l, mouseX, mouseY);
        } else if (scaling) {
            // Grow by how far the mouse moved away from the box centre on both axes, averaged.
            int centreX = startBox.x() + startBox.width() / 2;
            int centreY = startBox.y() + startBox.height() / 2;
            float startDist = Math.max(4, Math.abs(startMouseX - centreX) + Math.abs(startMouseY - centreY));
            float dist = Math.abs(mouseX - centreX) + Math.abs(mouseY - centreY);
            l.scale = Math.clamp(Math.round(startScale * dist / startDist), ElementLayout.MIN_SCALE, ElementLayout.MAX_SCALE);
        } else {
            int x = startBox.x() + mouseX - startMouseX;
            int y = startBox.y() + mouseY - startMouseY;
            guideX = -1;
            guideY = -1;
            if (!ctx.hasShiftDown()) {
                PanelSnap.Result snapX = PanelSnap.snap(x, startBox.width(), lines(active, true));
                PanelSnap.Result snapY = PanelSnap.snap(y, startBox.height(), lines(active, false));
                x = snapX.pos();
                y = snapY.pos();
                guideX = snapX.line();
                guideY = snapY.line();
            }
            l.dx = startDx + x - startBox.x();
            l.dy = startDy + y - startBox.y();
        }
        keepOnScreen(active);
        return true;
    }

    /**
     * Edge drag: the element's unscaled size grows by how far the grip moved (in its own units), and the opposite
     * edge stays where it was.
     */
    private void resize(ElementLayout l, int mouseX, int mouseY) {
        float scale = HudWrapper.scale(active, l);
        boolean horizontal = resizing == Edge.LEFT || resizing == Edge.RIGHT;
        int outward = horizontal
            ? (mouseX - startMouseX) * (resizing == Edge.LEFT ? -1 : 1)
            : (mouseY - startMouseY) * (resizing == Edge.TOP ? -1 : 1);
        int startWidth = Math.round(startBox.width() / scale);
        int startHeight = Math.round(startBox.height() / scale);
        int grow = Math.round(outward / scale);
        active.resize(minecraft, l, horizontal ? Math.max(1, startWidth + grow) : startWidth,
            horizontal ? startHeight : Math.max(1, startHeight + grow));
        HudWrapper.Box b = boxOf(active);
        switch (resizing) {
            case LEFT -> l.dx += startBox.x() + startBox.width() - (b.x() + b.width());
            case RIGHT -> l.dx += startBox.x() - b.x();
            case TOP -> l.dy += startBox.y() + startBox.height() - (b.y() + b.height());
            case BOTTOM -> l.dy += startBox.y() - b.y();
        }
    }

    /** Snap lines on one axis: screen edges and centre, and every other element's edges and centre. */
    private int[] lines(HudPart moving, boolean horizontal) {
        int size = horizontal ? width : height;
        List<Integer> lines = new ArrayList<>(List.of(0, size / 2, size));
        for (HudPart element : Parts.all()) {
            if (element == moving || working.get(element).hidden) continue;
            HudWrapper.Box b = boxOf(element);
            int start = horizontal ? b.x() : b.y();
            int length = horizontal ? b.width() : b.height();
            lines.add(start);
            lines.add(start + length / 2);
            lines.add(start + length);
        }
        return lines.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Pulls the element back inside the screen if a move or scale pushed it past an edge. */
    private void keepOnScreen(HudPart element) {
        ElementLayout l = working.edit(element);
        HudWrapper.Box b = boxOf(element);
        if (b.x() + b.width() > width) l.dx -= b.x() + b.width() - width;
        if (b.y() + b.height() > height) l.dy -= b.y() + b.height() - height;
        b = boxOf(element);
        if (b.x() < 0) l.dx -= b.x();
        if (b.y() < 0) l.dy -= b.y();
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent ctx) {
        if (movingToolbar) {
            movingToolbar = false;
            return true;
        }
        if (active != null) {
            active = null;
            scaling = false;
            resizing = null;
            guideX = -1;
            guideY = -1;
            return true;
        }
        return super.mouseReleased(ctx);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Cancel and Esc: the edits are dropped and chat's size put back, both in {@link #removed}. */
    @Override
    public void onClose() {
        close();
    }

    private void close() {
        minecraft.gui.setScreen(parent);
    }

    /**
     * However the editor closes (Cancel, Esc, a disconnect or another screen taking over), the preview ends and,
     * unless Done saved them, chat's size options go back to what they were.
     */
    @Override
    public void removed() {
        Profiles.setPreview(null);
        if (!saved && chatResized()) {
            minecraft.options.chatWidth().set(chatWidth);
            minecraft.options.chatHeightUnfocused().set(chatHeight);
        }
        super.removed();
    }
}
