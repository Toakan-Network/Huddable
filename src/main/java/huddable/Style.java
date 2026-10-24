package huddable;

/** How a restyleable bar (health, food, armor, air) is drawn. */
public enum Style {
    /** Vanilla's own icons, only moved and scaled. */
    VANILLA("Vanilla"),
    /** A filled bar. */
    BAR("Bar"),
    /** Text such as "18/20". */
    NUMBER("Number"),
    /** One vanilla icon followed by the number. */
    ICON_NUMBER("Icon + number");

    public final String label;

    Style(String label) {
        this.label = label;
    }

    public Style next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
