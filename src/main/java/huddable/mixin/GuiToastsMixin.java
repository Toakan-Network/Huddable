package huddable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import huddable.Element;
import huddable.ElementLayout;
import huddable.HudWrapper;
import huddable.Profiles;
import net.minecraft.client.Minecraft;
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
 * its own pose push and translate stay relative to ours.
 */
@Mixin(Gui.class)
abstract class GuiToastsMixin {
    @WrapOperation(method = "extractRenderState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/toasts/ToastManager;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void huddable$moveToasts(ToastManager toasts, GuiGraphicsExtractor graphics, Operation<Void> original) {
        ElementLayout layout = Profiles.current().get(Element.TOASTS);
        if (layout.hidden) return;
        float scale = HudWrapper.scale(Element.TOASTS, layout);
        if (layout.dx == 0 && layout.dy == 0 && scale == 1f) {
            original.call(toasts, graphics);
            return;
        }
        int[] anchor = Element.TOASTS.anchor(Minecraft.getInstance(), graphics.guiWidth(), graphics.guiHeight());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        HudWrapper.transform(pose, anchor[0], anchor[1], layout, scale);
        original.call(toasts, graphics);
        pose.popMatrix();
    }
}
