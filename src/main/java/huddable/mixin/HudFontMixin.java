package huddable.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import huddable.HudFonts;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Element fonts: while {@code HudWrapper} draws an element with a font of its own, every read of
 * {@code Minecraft.font} in vanilla's HUD code gets that element's wrapper font instead ({@code HudFonts}). These
 * classes are where vanilla's HUD text is measured and drawn (Hud covers the scoreboard, action bar, title, held
 * item name, XP level and item counts through {@code getFont}); the toast instance class is private, so it is
 * named. A Fabric HUD wrapper can't do this: it only sees the graphics, and vanilla reads the font itself.
 */
@Mixin(value = {Hud.class, PlayerTabOverlay.class, BossHealthOverlay.class, SubtitleOverlay.class},
    targets = "net.minecraft.client.gui.components.toasts.ToastManager$ToastInstance")
abstract class HudFontMixin {
    @ModifyExpressionValue(method = "*",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;font:Lnet/minecraft/client/gui/Font;"))
    private Font huddable$elementFont(Font font) {
        Font element = HudFonts.active();
        return element != null ? element : font;
    }
}
