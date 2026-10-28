package uk.toakan.tad.huddable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutCodeTest {
    @Test
    void codeRoundTrips() {
        Layout layout = new Layout();
        ElementLayout health = layout.edit(Element.HEALTH_BAR);
        health.dx = -40;
        health.dy = 12;
        health.scale = 150;
        health.style = Style.BAR;
        health.barWidth = 120;
        health.color = 0xFF4CC15A;
        layout.edit(Element.SCOREBOARD).hidden = true;

        Layout back = Layout.fromCode(layout.toCode());

        ElementLayout h = back.get(Element.HEALTH_BAR);
        assertEquals(-40, h.dx);
        assertEquals(12, h.dy);
        assertEquals(150, h.scale);
        assertEquals(Style.BAR, h.style);
        assertEquals(120, h.barWidth);
        assertEquals(0xFF4CC15A, h.color);
        assertTrue(back.get(Element.SCOREBOARD).hidden);
    }

    @Test
    void codeWithLineBreaksImports() {
        Layout layout = new Layout();
        layout.edit(Element.HOTBAR).dy = -30;
        String code = layout.toCode();
        // As a long code comes back from Discord: wrapped, with a stray space.
        String wrapped = code.substring(0, 12) + "\n" + code.substring(12, 20) + " " + code.substring(20);

        Layout back = Layout.fromCode(wrapped);

        assertEquals(-30, back.get(Element.HOTBAR).dy);
    }

    @Test
    void importClampsBadValues() {
        Layout layout = new Layout();
        ElementLayout hotbar = layout.edit(Element.HOTBAR);
        hotbar.scale = 5000;
        hotbar.style = Style.NUMBER; // the hotbar can't be restyled
        ElementLayout chat = layout.edit(Element.CHAT);
        chat.dx = 50;
        chat.style = Style.BAR; // chat can't be restyled
        chat.hidden = true;

        Layout back = Layout.fromCode(layout.toCode());

        assertEquals(ElementLayout.MAX_SCALE, back.get(Element.HOTBAR).scale);
        assertEquals(Style.VANILLA, back.get(Element.HOTBAR).style);
        assertEquals(50, back.get(Element.CHAT).dx);
        assertEquals(Style.VANILLA, back.get(Element.CHAT).style);
        assertTrue(back.get(Element.CHAT).hidden);
    }

    @Test
    void vanillaEntriesAreDropped() {
        Layout layout = new Layout();
        layout.edit(Element.CROSSHAIR);
        assertFalse(layout.copy().clamp().elements.containsKey(Element.CROSSHAIR.name()));
    }

    @Test
    void otherModsEntriesAreKept() {
        Layout layout = new Layout();
        ElementLayout panel = new ElementLayout();
        panel.dx = 30;
        panel.width = 150;
        panel.style = Style.BAR; // other mods' elements can't be restyled
        layout.elements.put("skyblock-shopping-list:panel/main", panel);
        layout.elements.put("NOT_AN_ELEMENT", new ElementLayout());
        layout.elements.get("NOT_AN_ELEMENT").dx = 5;
        layout.sizing = Sizing.WINDOW;

        Layout back = Layout.fromCode(layout.toCode());

        ElementLayout p = back.elements.get("skyblock-shopping-list:panel/main");
        assertEquals(30, p.dx);
        assertEquals(150, p.width);
        assertEquals(Style.VANILLA, p.style);
        assertFalse(back.elements.containsKey("NOT_AN_ELEMENT"));
        assertEquals(Sizing.WINDOW, back.sizing);
    }

    @Test
    void sizingMatchesShoppingList() {
        // Shopping List's SandboxWidget.scaleFactor(): GUI_SCALE 1, FIXED 2 / guiScale, WINDOW 2 * (height / 1080) / guiScale.
        assertEquals(1f, Sizing.GUI_SCALE.factor(3, 1440));
        assertEquals(1f, Sizing.FIXED.factor(2, 1080));
        assertEquals(0.5f, Sizing.FIXED.factor(4, 2160));
        assertEquals(1f, Sizing.WINDOW.factor(2, 1080));
        assertEquals(2f * (1440 / 1080f) / 3, Sizing.WINDOW.factor(3, 1440));
        assertEquals(2f, Sizing.WINDOW.factor(0, 1080)); // GUI Scale "auto" reads as at least 1
    }

    @Test
    void offsetsFollowTheSizingFactor() {
        Layout layout = new Layout();
        layout.edit(Element.HOTBAR).dx = -40;
        layout.edit(Element.HOTBAR).dy = 15;
        // Not recorded yet (files from before): offsets draw as saved.
        assertEquals(1f, layout.offsetScale(1.5f));

        layout.offsetFactor = 1f;
        assertEquals(1.5f, layout.offsetScale(1.5f));
        layout.rebaseOffsets(1.5f);
        assertEquals(-60, layout.get(Element.HOTBAR).dx);
        assertEquals(23, layout.get(Element.HOTBAR).dy); // 22.5 rounds up
        assertEquals(1.5f, layout.offsetFactor);
        assertEquals(1f, layout.offsetScale(1.5f));

        layout.rebaseOffsets(1.5f); // again at the same factor: no change
        assertEquals(-60, layout.get(Element.HOTBAR).dx);
    }

    @Test
    void offsetFactorTravelsInCodes() {
        Layout layout = new Layout();
        layout.edit(Element.HOTBAR).dy = -30;
        layout.offsetFactor = 1.25f;
        assertEquals(1.25f, Layout.fromCode(layout.toCode()).offsetFactor);

        layout.offsetFactor = Float.NaN;
        assertEquals(0f, Layout.fromCode(layout.toCode()).offsetFactor);
        layout.offsetFactor = 500f;
        assertEquals(0f, Layout.fromCode(layout.toCode()).offsetFactor);
    }

    @Test
    void rejectsOtherCodes() {
        assertNull(Layout.fromCode("SSL1:abc"));
        assertNull(Layout.fromCode("HUD1:not base64!"));
        assertNull(Layout.fromCode(null));
    }
}
