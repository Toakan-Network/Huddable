package uk.toakan.tad.huddable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import uk.toakan.tad.huddable.Element;
import uk.toakan.tad.huddable.ElementLayout;
import uk.toakan.tad.huddable.HudFonts;
import uk.toakan.tad.huddable.HudWrapper;
import uk.toakan.tad.huddable.Profiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Moves, scales or hides toasts. Toasts aren't a Fabric HUD element: Gui draws them itself, after the HUD, with
 * one call to {@code ToastManager.extractRenderState}. Wrapping that call moves vanilla's toasts and anything
 * drawn inside it; Fancy Notify draws its notifications from its own HEAD inject there, so they move too, and
 * its own pose push and translate stay relative to ours. A toasts font is handed to vanilla's toast code
 * through {@code HudFontMixin} while the call runs.
 */
@Mixin(Gui.class)
abstract class GuiToastsMixin {
    @WrapOperation(method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void huddable$moveToasts(ToastManager toasts, GuiGraphicsExtractor graphics, Operation<Void> original) {
        ElementLayout layout = Profiles.current().get(Element.TOASTS);
        if (layout.hidden) return;
        float scale = HudWrapper.scale(Element.TOASTS, layout);
        Font font = HudFonts.wrapped(Element.TOASTS, Profiles.current());
        if (layout.dx == 0 && layout.dy == 0 && scale == 1f) {
            HudWrapper.drawInFont(font, () -> original.call(toasts, graphics));
            return;
        }
        int[] anchor = Element.TOASTS.anchor(Minecraft.getInstance(), graphics.guiWidth(), graphics.guiHeight());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        HudWrapper.transform(pose, anchor[0], anchor[1], layout, scale);
        HudWrapper.drawInFont(font, () -> original.call(toasts, graphics));
        pose.popMatrix();
    }
}
