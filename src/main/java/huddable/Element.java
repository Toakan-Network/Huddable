package huddable;

import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.Scoreboard;

/**
 * The vanilla HUD elements Huddable wraps, with where vanilla draws each one. The IDs are Fabric API's
 * {@link VanillaHudElements} (fabric-rendering-v1 25.3.3 for 26.2 and 27.0.14 for 26.3 have the same set). The
 * numbers follow {@code net.minecraft.client.gui.Hud} (and ContextualBar, BossHealthOverlay, SubtitleOverlay).
 *
 * <p>Each element has an anchor: the point vanilla positions it from. Moving adds an offset to the anchor and
 * scaling grows the element around it, so a bottom-centre element keeps hugging the bottom-centre when the screen
 * changes size. The box is only for the editor (hit testing, outlines, snapping); for elements whose size depends
 * on live state it is measured the way vanilla measures it, or a typical size when that state isn't readable.
 */
public enum Element {
    HOTBAR(VanillaHudElements.HOTBAR, "Hotbar", true, false, false),
    HEALTH_BAR(VanillaHudElements.HEALTH_BAR, "Health", true, true, false),
    ARMOR_BAR(VanillaHudElements.ARMOR_BAR, "Armor", true, true, false),
    FOOD_BAR(VanillaHudElements.FOOD_BAR, "Hunger", true, true, true),
    AIR_BAR(VanillaHudElements.AIR_BAR, "Air", true, true, true),
    MOUNT_HEALTH(VanillaHudElements.MOUNT_HEALTH, "Mount health", true, false, true),
    INFO_BAR(VanillaHudElements.INFO_BAR, "Experience bar", true, false, false),
    EXPERIENCE_LEVEL(VanillaHudElements.EXPERIENCE_LEVEL, "Experience level", true, false, false),
    HELD_ITEM_TOOLTIP(VanillaHudElements.HELD_ITEM_TOOLTIP, "Held item name", true, false, false),
    MOB_EFFECTS(VanillaHudElements.MOB_EFFECTS, "Effects", true, false, false),
    BOSS_BAR(VanillaHudElements.BOSS_BAR, "Boss bar", true, false, false),
    SCOREBOARD(VanillaHudElements.SCOREBOARD, "Scoreboard", true, false, false),
    OVERLAY_MESSAGE(VanillaHudElements.OVERLAY_MESSAGE, "Action bar", true, false, false),
    TITLE_AND_SUBTITLE(VanillaHudElements.TITLE_AND_SUBTITLE, "Title", true, false, false),
    PLAYER_LIST(VanillaHudElements.PLAYER_LIST, "Tab list", true, false, false),
    CROSSHAIR(VanillaHudElements.CROSSHAIR, "Crosshair", true, false, false),
    SUBTITLES(VanillaHudElements.SUBTITLES, "Subtitles", true, false, false),
    // Moved by ChatComponentMixin rather than HudWrapper, so the open chat and its clicks move too.
    CHAT(VanillaHudElements.CHAT, "Chat", true, false, false);

    /** Vanilla's distance from the bottom of the screen to the top of the first status bar row. */
    private static final int STATUS_LINE = 39;
    /** Chat's anchor: its bottom-left corner, {@code ChatComponent}'s {@code screenHeight - 40}. */
    public static final int CHAT_ANCHOR_X = 0;
    public static final int CHAT_BOTTOM_OFFSET = 40;

    public final Identifier id;
    public final String label;
    public final boolean movable;
    public final boolean restyleable;
    /** Status bars on the right of the hotbar fill from the right, so a restyled one is right-aligned. */
    public final boolean rightAligned;

    Element(Identifier id, String label, boolean movable, boolean restyleable, boolean rightAligned) {
        this.id = id;
        this.label = label;
        this.movable = movable;
        this.restyleable = restyleable;
        this.rightAligned = rightAligned;
    }

    /** Anchor point plus the unmoved, unscaled box, in GUI pixels. */
    public record Geometry(int anchorX, int anchorY, int x, int y, int width, int height) {}

    /** Where vanilla draws this element now, for a screen of {@code w} x {@code h} GUI pixels. */
    public Geometry geometry(Minecraft mc, int w, int h) {
        Font font = mc.font;
        Player player = mc.getCameraEntity() instanceof Player p ? p : null;
        int centre = w / 2;
        int base = h - STATUS_LINE;
        return switch (this) {
            case HOTBAR -> new Geometry(centre, h, centre - 91, h - 22, 182, 22);
            case HEALTH_BAR -> {
                // Extra heart rows (high max health, absorption) stack upwards from the first row.
                int rows = healthRows(player);
                int rowHeight = healthRowHeight(rows);
                int top = base - (rows - 1) * rowHeight;
                yield new Geometry(centre - 91, base + 9, centre - 91, top, 81, base + 9 - top);
            }
            case ARMOR_BAR -> {
                int rows = healthRows(player);
                int y = base - (rows - 1) * healthRowHeight(rows) - 10;
                yield new Geometry(centre - 91, y + 9, centre - 91, y, 81, 9);
            }
            case FOOD_BAR, MOUNT_HEALTH -> new Geometry(centre + 91, base + 9, centre + 10, base, 81, 9);
            case AIR_BAR -> {
                // Above the food row, or above every mount heart row when riding.
                int mountRows = mountRows(player);
                int y = base - 10 - Math.max(0, mountRows - 1) * 10;
                yield new Geometry(centre + 91, y + 9, centre + 10, y, 81, 9);
            }
            case INFO_BAR -> new Geometry(centre, h - 24, centre - 91, h - 29, 182, 5);
            case EXPERIENCE_LEVEL -> {
                int level = player != null && player.experienceLevel > 0 ? player.experienceLevel : 30;
                int textWidth = font.width(Component.translatable("gui.experience.level", level));
                int y = h - 24 - 9 - 2;
                yield new Geometry(centre, y, centre - textWidth / 2 - 1, y - 1, textWidth + 2, 11);
            }
            case HELD_ITEM_TOOLTIP -> {
                int y = h - 59;
                yield new Geometry(centre, y, centre - 50, y - 2, 100, 13);
            }
            case MOB_EFFECTS -> effectsGeometry(mc, player, w);
            case BOSS_BAR -> new Geometry(centre, 0, centre - 91, 3, 182, 14);
            case SCOREBOARD -> scoreboardGeometry(mc, w, h);
            case OVERLAY_MESSAGE -> new Geometry(centre, h - 68, centre - 75, h - 73, 150, 11);
            case TITLE_AND_SUBTITLE -> new Geometry(centre, h / 2, centre - 100, h / 2 - 42, 200, 72);
            case PLAYER_LIST -> new Geometry(centre, 0, centre - 100, 10, 200, 100);
            case CROSSHAIR -> new Geometry(centre, h / 2, (w - 15) / 2, (h - 15) / 2, 15, 15);
            case SUBTITLES -> new Geometry(w, h - 35, w - 152, h - 80, 150, 50);
            case CHAT -> chatGeometry(mc, h);
        };
    }

    /**
     * The closed chat's full area as ChatComponent sizes it: chat width plus its 4 px margins on each side, and
     * as many lines as the unfocused height holds.
     */
    private static Geometry chatGeometry(Minecraft mc, int h) {
        double scale = mc.options.chatScale().get();
        int lineHeight = (int) (9.0 * (mc.options.chatLineSpacing().get() + 1.0));
        int lines = ChatComponent.getHeight(mc.options.chatHeightUnfocused().get()) / lineHeight;
        int width = (int) Math.round(ChatComponent.getWidth(mc.options.chatWidth().get()) + 12 * scale);
        int height = Math.max(1, (int) Math.round(lines * lineHeight * scale));
        int bottom = h - CHAT_BOTTOM_OFFSET;
        return new Geometry(CHAT_ANCHOR_X, bottom, CHAT_ANCHOR_X, bottom - height, width, height);
    }

    /**
     * Just the anchor, for drawing every frame: skips measuring the elements whose box takes work (effects,
     * scoreboard), as their anchor doesn't depend on it.
     */
    public int[] anchor(Minecraft mc, int w, int h) {
        return switch (this) {
            case MOB_EFFECTS -> new int[] {w, 0};
            case SCOREBOARD -> new int[] {w, h / 2};
            default -> {
                Geometry g = geometry(mc, w, h);
                yield new int[] {g.anchorX(), g.anchorY()};
            }
        };
    }

    /** Heart rows vanilla draws, as in Hud.extractPlayerHealth. */
    private static int healthRows(Player player) {
        if (player == null) return 1;
        float maxHealth = Math.max(player.getMaxHealth(), player.getHealth());
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        return Math.max(1, Mth.ceil((maxHealth + absorption) / 2.0F / 10.0F));
    }

    private static int healthRowHeight(int rows) {
        return Math.max(10 - (rows - 2), 3);
    }

    /** Mount heart rows, as in Hud.getVehicleMaxHearts and getVisibleVehicleHeartRows; 0 when not riding one. */
    private static int mountRows(Player player) {
        if (player == null || !(player.getVehicle() instanceof LivingEntity mount) || !mount.showVehicleHealth()) {
            return 0;
        }
        int hearts = Math.min(30, (int) (mount.getMaxHealth() + 0.5F) / 2);
        return (int) Math.ceil(hearts / 10.0);
    }

    /** Effect icons sit in the top right, 25 px apart: beneficial on the first row, harmful on the second. */
    private static Geometry effectsGeometry(Minecraft mc, Player player, int w) {
        int good = 0;
        int bad = 0;
        if (player != null) {
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (!effect.showIcon()) continue;
                if (effect.getEffect().value().isBeneficial()) good++;
                else bad++;
            }
        }
        if (good == 0 && bad == 0) good = 3; // a typical size to place by
        int top = mc.isDemo() ? 16 : 1;
        int width = 25 * Math.max(good, bad);
        int height = bad > 0 ? 26 + 24 : 24;
        return new Geometry(w, 0, w - width, top, width, height);
    }

    /** The sidebar is right-aligned and centred a little below the middle, as in Hud.displayScoreboardSidebar. */
    private static Geometry scoreboardGeometry(Minecraft mc, int w, int h) {
        Font font = mc.font;
        int width = 90;
        int entries = 10;
        Objective objective = mc.level != null ? mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) : null;
        if (objective != null) {
            Scoreboard scoreboard = objective.getScoreboard();
            width = font.width(objective.getDisplayName());
            int spacer = font.width(": ");
            entries = 0;
            for (PlayerScoreEntry score : scoreboard.listPlayerScores(objective)) {
                if (score.isHidden()) continue;
                if (++entries > 15) {
                    entries = 15;
                    break;
                }
                int scoreWidth = font.width(score.formatValue(objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT)));
                width = Math.max(width, font.width(score.ownerName()) + (scoreWidth > 0 ? spacer + scoreWidth : 0));
            }
        }
        int height = entries * 9;
        int bottom = h / 2 + height / 3;
        int left = w - width - 3;
        int top = bottom - height - 10;
        return new Geometry(w, h / 2, left - 2, top, w - 1 - (left - 2), bottom - top);
    }
}
