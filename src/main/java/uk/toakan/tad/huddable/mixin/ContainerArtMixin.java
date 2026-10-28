package uk.toakan.tad.huddable.mixin;

import uk.toakan.tad.huddable.HudScreenArt;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Container screen art: every {@code blit} of a texture by its id ends in the {@code innerBlit} overload that looks
 * the id up here, so a container background ({@code textures/gui/container/*.png}) is swapped for the chosen
 * pack's copy ({@link HudScreenArt}). UVs are already fractions of the texture, so a higher-resolution copy lines
 * up the same. Targets the lookup call rather than the method's descriptor, whose pipeline class moved in 26.3.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class ContainerArtMixin {
    @ModifyArg(method = "innerBlit", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/texture/TextureManager;getTexture(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/texture/AbstractTexture;"))
    private Identifier huddable$screenArt(Identifier location) {
        return HudScreenArt.swap(location);
    }
}
