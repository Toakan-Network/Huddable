package uk.toakan.tad.huddable;

import net.minecraft.client.resources.language.I18n;

/** How a restyleable bar (health, food, armor, air) is drawn. */
public enum Style {
    /** Vanilla's own icons, only moved and scaled. */
    VANILLA("huddable.style.vanilla"),
    /** A filled bar. */
    BAR("huddable.style.bar"),
    /** Text such as "18/20". */
    NUMBER("huddable.style.number"),
    /** One vanilla icon followed by the number. */
    ICON_NUMBER("huddable.style.icon_number");

    /** Lang key of the name shown in the editor. */
    private final String labelKey;

    Style(String labelKey) {
        this.labelKey = labelKey;
    }

    public String label() {
        return I18n.get(labelKey);
    }

    public Style next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
