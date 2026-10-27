package uk.toakan.tad.huddable.mixin.meowdding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.owdding.lib.overlays.Overlay;
import me.owdding.lib.overlays.Overlays;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import uk.toakan.tad.huddable.compat.MeowddingCompat;

/**
 * Moves, scales or hides each meowdding-lib overlay ({@link MeowddingCompat}). {@code onHudRender} loops over
 * every overlay and calls its {@code extract} after setting meowdding's own position; wrapping that one call
 * places each overlay on its own, while meowdding's hover outline and click-to-edit stay at its own position.
 */
@Mixin(Overlays.class)
abstract class OverlaysMixin {
    @WrapOperation(method = "onHudRender",
        at = @At(value = "INVOKE", target = "Lme/owdding/lib/overlays/Overlay;extract(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void huddable$placeOverlay(Overlay overlay, GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                       float partialTicks, Operation<Void> original) {
        MeowddingCompat.draw(overlay, graphics, () -> original.call(overlay, graphics, mouseX, mouseY, partialTicks));
    }
}
