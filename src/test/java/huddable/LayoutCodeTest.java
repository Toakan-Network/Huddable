package huddable;

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
    void importClampsBadValues() {
        Layout layout = new Layout();
        ElementLayout hotbar = layout.edit(Element.HOTBAR);
        hotbar.scale = 5000;
        hotbar.style = Style.NUMBER; // the hotbar can't be restyled
        ElementLayout chat = layout.edit(Element.CHAT);
        chat.dx = 50; // chat can't move
        chat.hidden = true;

        Layout back = Layout.fromCode(layout.toCode());

        assertEquals(ElementLayout.MAX_SCALE, back.get(Element.HOTBAR).scale);
        assertEquals(Style.VANILLA, back.get(Element.HOTBAR).style);
        assertEquals(0, back.get(Element.CHAT).dx);
        assertTrue(back.get(Element.CHAT).hidden);
    }

    @Test
    void vanillaEntriesAreDropped() {
        Layout layout = new Layout();
        layout.edit(Element.CROSSHAIR);
        assertFalse(layout.copy().clamp().elements.containsKey(Element.CROSSHAIR.name()));
    }

    @Test
    void rejectsOtherCodes() {
        assertNull(Layout.fromCode("SSL1:abc"));
        assertNull(Layout.fromCode("HUD1:not base64!"));
        assertNull(Layout.fromCode(null));
    }
}
