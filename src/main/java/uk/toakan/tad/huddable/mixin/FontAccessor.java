package uk.toakan.tad.huddable.mixin;

import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The game font's glyph provider (private), which {@code HudFonts} wraps to send the default font elsewhere. */
@Mixin(Font.class)
public interface FontAccessor {
    @Accessor("provider")
    Font.Provider huddable$provider();
}
