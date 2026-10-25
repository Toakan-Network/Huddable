package uk.toakan.tad.huddable.api;

/**
 * Entrypoint for mods that put their HUD elements in Huddable's editor. List the class under the {@code "huddable"}
 * entrypoint in {@code fabric.mod.json}; Huddable calls {@link #register} during its client init, so the class
 * (and anything it touches in this package) only loads when Huddable is installed.
 */
@FunctionalInterface
public interface HuddablePlugin {
    /** Register elements with {@link HuddableApi#register} here. */
    void register();
}
