package uk.toakan.tad.huddable.compat;

import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import me.owdding.lib.overlays.Overlay;
import me.owdding.lib.overlays.Rect;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import uk.toakan.tad.huddable.ExternalPart;
import uk.toakan.tad.huddable.GeneralSettings;
import uk.toakan.tad.huddable.HudFrames;
import uk.toakan.tad.huddable.HuddableClient;
import uk.toakan.tad.huddable.Parts;
import uk.toakan.tad.huddable.Profiles;
import uk.toakan.tad.huddable.api.HuddableApi;
import uk.toakan.tad.huddable.mixin.meowdding.OverlaysAccessor;

/**
 * Places meowdding-lib's overlays (SkyCubed, SkyOcean and the other meowdding mods) one by one. meowdding-lib
 * draws them all in one loop inside SkyblockAPI's {@code RenderHudEvent}, which fires from vanilla's sleep
 * overlay, so none is a Fabric HUD layer {@code HudDiscovery} could find. Each overlay is registered here as an
 * API element, and {@code mixin.meowdding.OverlaysMixin} moves its draw call inside that loop. General Settings'
 * Advanced toggle turns this off. Only this class and that mixin package touch meowdding-lib. Client thread only.
 */
public final class MeowddingCompat {
    public static final String MOD_ID = "meowdding-lib";
    /** SkyCubed overlays drawn by SkyCubed's own mixins on the vanilla hotbar: their draw call draws nothing to place. */
    private static final Set<String> SKIPPED = Set.of(
        "tech.thatgravyboat.skycubed.features.overlays.vanilla.MovableHotbar",
        "tech.thatgravyboat.skycubed.features.overlays.vanilla.MovableItemText");

    private static final Map<Overlay, Identifier> PARTS = new IdentityHashMap<>();
    private static boolean failed;

    private MeowddingCompat() {}

    /** True when meowdding-lib is installed and listing its overlays hasn't failed. */
    public static boolean available() {
        return !failed && FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    /** Lists every overlay registered by now as a part, when the General Settings toggle is on. */
    public static void start() {
        if (!available() || !GeneralSettings.get().meowddingOverlays || !PARTS.isEmpty()) return;
        try {
            for (Overlay overlay : OverlaysAccessor.huddable$overlays()) {
                if (SKIPPED.contains(overlay.getClass().getName())) continue;
                Identifier id = uniqueId(overlay);
                PARTS.put(overlay, id);
                // An overlay its mod has switched off isn't listed until it's switched back on there.
                Parts.add(ExternalPart.api(id, label(overlay), HuddableApi.Spec.of((w, h) -> box(overlay)))
                    .listedWhen(overlay::getEnabled));
            }
        } catch (RuntimeException | LinkageError e) {
            HuddableClient.LOGGER.warn("Couldn't list meowdding-lib's overlays; they can't be placed", e);
            stop();
            failed = true;
            return;
        }
        if (!PARTS.isEmpty()) HuddableClient.LOGGER.info("Placing {} meowdding-lib overlays: {}", PARTS.size(), PARTS.values());
    }

    /** Takes the overlays out of the editor; their saved positions stay in the profiles. */
    public static void stop() {
        for (Identifier id : PARTS.values()) Parts.remove(id);
        PARTS.clear();
    }

    /**
     * Runs one overlay's draw call where the player put it. The pose already holds meowdding's own position and
     * scale, so Huddable's move is applied on top of it, in screen space.
     */
    public static void draw(Overlay overlay, GuiGraphicsExtractor graphics, Runnable draw) {
        Identifier id = PARTS.get(overlay);
        ExternalPart part = id == null ? null : Parts.external(id);
        if (part == null) {
            draw.run();
            return;
        }
        int w = graphics.guiWidth();
        int h = graphics.guiHeight();
        HuddableApi.Placement p = part.placement(w, h);
        if (p.hidden()) return;
        HuddableApi.Box box = part.defaultBox(w, h);
        HudFrames.Choice frame = HudFrames.resolve(part, Profiles.current());
        if (frame != null) {
            // In screen space: the pose holds meowdding's own position, which the box already includes.
            Matrix3x2fStack framePose = graphics.pose();
            framePose.pushMatrix();
            framePose.identity();
            HudFrames.drawAround(graphics, frame, p.x(), p.y(), p.width() * p.scale(), p.height() * p.scale());
            framePose.popMatrix();
        }
        if (p.x() == box.x() && p.y() == box.y() && p.scale() == 1f) {
            draw.run();
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        Matrix3x2f moved = new Matrix3x2f().translate(p.x(), p.y()).scale(p.scale(), p.scale())
            .translate(-box.x(), -box.y()).mul(pose);
        pose.pushMatrix();
        pose.set(moved);
        draw.run();
        pose.popMatrix();
    }

    /** Where meowdding draws the overlay now: the box its own editor highlights, in GUI pixels. */
    private static HuddableApi.Box box(Overlay overlay) {
        Rect r = overlay.getEditBounds().times(overlay.getPosition().getScale());
        return new HuddableApi.Box(r.getX(), r.getY(), Math.max(1, r.getWidth()), Math.max(1, r.getHeight()));
    }

    /**
     * {@code modid:name}, from the overlay's translation key when it has one (stays the same in every language),
     * else its text. Profiles store positions under it, so it must not change between starts.
     */
    private static Identifier uniqueId(Overlay overlay) {
        Component name = overlay.getName();
        String raw = name.getContents() instanceof TranslatableContents t ? t.getKey() : name.getString();
        String namespace = clean(overlay.getModId(), "[^a-z0-9_.-]");
        String path = clean(raw, "[^a-z0-9/._-]");
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        for (int n = 2; PARTS.containsValue(id); n++) {
            id = Identifier.fromNamespaceAndPath(namespace, path + "_" + n);
        }
        return id;
    }

    private static String clean(String s, String illegal) {
        String cleaned = s.toLowerCase(Locale.ROOT).replaceAll(illegal, "_");
        return cleaned.isEmpty() ? "overlay" : cleaned;
    }

    /** "Mod Name: Overlay name", from the mod that registered it when there is one. */
    private static Component label(Overlay overlay) {
        String mod = FabricLoader.getInstance().getModContainer(overlay.getModId())
            .map(c -> c.getMetadata().getName()).orElse(overlay.getModId());
        return Component.literal(mod + ": ").append(overlay.getName());
    }
}
