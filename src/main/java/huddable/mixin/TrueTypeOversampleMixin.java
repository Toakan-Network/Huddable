package huddable.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import huddable.GeneralSettings;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Font quality (General Settings): the oversample every TrueType font is rasterised at, in place of the value
 * its resource pack sets. Higher is sharper at large GUI scales and uses more video memory; it applies to every
 * mod's TTF fonts, as vanilla loads each font once for everyone. Runs on the font loading threads.
 */
@Mixin(TrueTypeGlyphProviderDefinition.class)
abstract class TrueTypeOversampleMixin {
    @ModifyExpressionValue(method = "load",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/font/providers/TrueTypeGlyphProviderDefinition;oversample:F"))
    private float huddable$fontQuality(float packOversample) {
        int quality = GeneralSettings.get().fontOversample;
        return quality > 0 ? quality : packOversample;
    }
}
