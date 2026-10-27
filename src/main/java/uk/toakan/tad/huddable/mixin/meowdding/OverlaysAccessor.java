package uk.toakan.tad.huddable.mixin.meowdding;

import java.util.List;
import me.owdding.lib.overlays.Overlay;
import me.owdding.lib.overlays.Overlays;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** meowdding-lib's list of registered overlays (a Kotlin object's private field, so static), read-only. */
@Mixin(Overlays.class)
public interface OverlaysAccessor {
    @Accessor("overlays")
    static List<Overlay> huddable$overlays() {
        throw new AssertionError();
    }
}
