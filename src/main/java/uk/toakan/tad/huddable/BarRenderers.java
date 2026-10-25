package uk.toakan.tad.huddable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * The non-vanilla styles for health, food, armor and air ({@link Style#BAR}, {@link Style#NUMBER},
 * {@link Style#ICON_NUMBER}). They read the player only. Each draws in an unmoved, unscaled box next to the
 * element's anchor (bottom-left, or bottom-right for {@link Element#rightAligned} bars); {@link HudWrapper}
 * moves and scales it like it does vanilla's drawing.
 */
public final class BarRenderers {
    private static final int BACKGROUND = 0xAA000000;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int ICON = 9;
    private static final int ICON_GAP = 2;
    // Icons are vanilla's own sprites (Hud's HEART/FOOD/ARMOR/AIR full sprites), so resource packs restyle them too.
    private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier FOOD_SPRITE = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier ARMOR_SPRITE = Identifier.withDefaultNamespace("hud/armor_full");
    private static final Identifier AIR_SPRITE = Identifier.withDefaultNamespace("hud/air");

    /** One reading: {@code value} of {@code max}, plus an extra amount drawn over the bar (absorption, saturation). */
    record Reading(float value, float max, float extra, String text) {}

    private BarRenderers() {}

    public static int defaultColor(Element element) {
        return switch (element) {
            case HEALTH_BAR -> 0xFFD83A3A;
            case FOOD_BAR -> 0xFFC8843A;
            case ARMOR_BAR -> 0xFFB4BCCB;
            case AIR_BAR -> 0xFF5AA0E8;
            default -> TEXT;
        };
    }

    private static int extraColor(Element element) {
        return element == Element.HEALTH_BAR ? 0xFFE8C33A : 0xFFF0D870;
    }

    /** The reading to show, or null when vanilla would draw nothing (no armor, full air above water). */
    static Reading read(Element element, Player player) {
        return switch (element) {
            case HEALTH_BAR -> {
                float health = player.getHealth();
                float max = Math.max(player.getMaxHealth(), health);
                yield new Reading(health, max, player.getAbsorptionAmount(), Mth.ceil(health) + "/" + Mth.ceil(max));
            }
            case FOOD_BAR -> {
                int food = player.getFoodData().getFoodLevel();
                yield new Reading(food, 20, player.getFoodData().getSaturationLevel(), food + "/20");
            }
            case ARMOR_BAR -> {
                int armor = player.getArmorValue();
                yield armor > 0 ? new Reading(armor, 20, 0, String.valueOf(armor)) : null;
            }
            case AIR_BAR -> {
                int max = player.getMaxAirSupply();
                int air = Math.clamp(player.getAirSupply(), 0, max);
                if (!player.isEyeInFluid(FluidTags.WATER) && air >= max) yield null;
                // Bubbles, as vanilla counts them.
                int bubbles = Mth.ceil(air * 10f / max);
                yield new Reading(air, max, 0, bubbles + "/10");
            }
            default -> null;
        };
    }

    /** Width and height of the drawn box for this style. */
    static int[] size(Element element, ElementLayout layout, Font font, Reading reading) {
        String text = reading != null ? reading.text() : sampleText(element);
        return switch (layout.style) {
            case BAR -> new int[] {layout.barWidth, layout.barHeight};
            case NUMBER -> new int[] {font.width(text), 9};
            case ICON_NUMBER -> new int[] {ICON + ICON_GAP + font.width(text), ICON};
            case VANILLA -> new int[] {0, 0};
        };
    }

    /** Typical text, to size a bar the editor shows with no player to read. */
    static String sampleText(Element element) {
        return switch (element) {
            case HEALTH_BAR -> "20/20";
            case FOOD_BAR -> "20/20";
            case AIR_BAR -> "10/10";
            default -> "20";
        };
    }

    /** Draws {@code reading} with its box's bottom corner at the anchor. */
    static void draw(GuiGraphicsExtractor graphics, Font font, Element element, ElementLayout layout,
                     int anchorX, int anchorY, Reading reading) {
        int[] size = size(element, layout, font, reading);
        int x = element.rightAligned ? anchorX - size[0] : anchorX;
        int y = anchorY - size[1];
        int color = layout.color != 0 ? layout.color : defaultColor(element);
        switch (layout.style) {
            case BAR -> drawBar(graphics, font, element, layout, x, y, size[0], size[1], color, reading);
            case NUMBER -> graphics.text(font, reading.text(), x, y + 1, color, true);
            case ICON_NUMBER -> {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(element), x, y, ICON, ICON);
                graphics.text(font, reading.text(), x + ICON + ICON_GAP, y + 1, layout.color != 0 ? color : TEXT, true);
            }
            case VANILLA -> {}
        }
    }

    private static void drawBar(GuiGraphicsExtractor graphics, Font font, Element element, ElementLayout layout,
                                int x, int y, int w, int h, int color, Reading reading) {
        graphics.fill(x, y, x + w, y + h, BACKGROUND);
        int inner = w - 2;
        int filled = Math.round(inner * Math.clamp(reading.value() / reading.max(), 0f, 1f));
        // Food and air drain from the left like vanilla's icons do, so right-aligned bars fill from the right.
        int fillLeft = element.rightAligned ? x + 1 + inner - filled : x + 1;
        graphics.fill(fillLeft, y + 1, fillLeft + filled, y + h - 1, color);
        if (reading.extra() > 0) {
            // Absorption and saturation as a thin strip along the top, on the same scale as the bar.
            int extra = Math.round(inner * Math.clamp(reading.extra() / reading.max(), 0f, 1f));
            int extraLeft = element.rightAligned ? x + 1 + inner - extra : x + 1;
            graphics.fill(extraLeft, y + 1, extraLeft + extra, y + 1 + Math.max(1, (h - 2) / 3), extraColor(element));
        }
        if (layout.showText && h >= 8) {
            int textWidth = font.width(reading.text());
            graphics.text(font, reading.text(), x + (w - textWidth) / 2, y + (h - 8) / 2, TEXT, true);
        }
    }

    private static Identifier sprite(Element element) {
        return switch (element) {
            case FOOD_BAR -> FOOD_SPRITE;
            case ARMOR_BAR -> ARMOR_SPRITE;
            case AIR_BAR -> AIR_SPRITE;
            default -> HEART_SPRITE;
        };
    }
}
