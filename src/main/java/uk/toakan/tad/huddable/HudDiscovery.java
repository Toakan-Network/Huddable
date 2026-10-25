package uk.toakan.tad.huddable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.impl.client.rendering.hud.HudElementRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.hud.HudLayer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Finds the HUD elements other mods added through Fabric's {@code HudElementRegistry} and wraps each one, so it
 * can be moved, scaled and hidden like a vanilla element without its mod doing anything.
 *
 * <p>Fabric's API has no way to list elements, so this reads {@code HudElementRegistryImpl.ROOT_ELEMENTS}
 * (Fabric's own status bar height code reads it the same way). It only reads it; the wrapping goes through the
 * public {@link HudElementRegistry#replaceElement}. If a Fabric update changes those internals, discovery logs a
 * warning and turns itself off; nothing else depends on it. Runs once, when the client has started, after every
 * mod's init. Elements a mod adds later than that are not found.
 */
final class HudDiscovery {
    private HudDiscovery() {}

    static void discover() {
        List<Identifier> found = new ArrayList<>();
        try {
            // Each root is a vanilla element; anything else in its layer list was added by a mod.
            Set<Identifier> vanilla = HudElementRegistryImpl.ROOT_ELEMENTS.keySet();
            for (HudElementRegistryImpl.RootLayer root : HudElementRegistryImpl.ROOT_ELEMENTS.values()) {
                for (HudLayer layer : root.layers()) {
                    Identifier id = layer.id();
                    if (layer.isRemoved() || vanilla.contains(id) || Parts.claimed(id)) continue;
                    if (id.getNamespace().equals(HuddableClient.MOD_ID)) continue;
                    found.add(id);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            HuddableClient.LOGGER.warn("Couldn't list other mods' HUD elements; only vanilla and API elements can be placed", e);
            return;
        }
        for (Identifier id : found) {
            ExternalPart part = ExternalPart.fabric(id, label(id));
            HudElementRegistry.replaceElement(id, original -> {
                part.original = original;
                return (graphics, delta) -> HudWrapper.renderExternal(part, original, graphics, delta);
            });
            Parts.add(part);
        }
        if (!found.isEmpty()) HuddableClient.LOGGER.info("Placing {} HUD elements from other mods: {}", found.size(), found);
    }

    /** "Mod Name: path", from the mod that owns the namespace when there is one. */
    private static Component label(Identifier id) {
        String mod = FabricLoader.getInstance().getModContainer(id.getNamespace())
            .map(c -> c.getMetadata().getName()).orElse(id.getNamespace());
        return Component.literal(mod + ": " + id.getPath());
    }
}
