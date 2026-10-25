package uk.toakan.tad.huddable.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import uk.toakan.tad.huddable.Element;
import uk.toakan.tad.huddable.ElementLayout;
import uk.toakan.tad.huddable.HudFonts;
import uk.toakan.tad.huddable.HudWrapper;
import uk.toakan.tad.huddable.Profiles;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves and scales the chat, and gives it its font. The closed chat (HUD), the open chat (ChatScreen) and the
 * chat screen's click test all run through {@code extractRenderState}, and each of its graphics accesses applies
 * {@code updatePose} to what it draws or hit-tests (hovers invert the pose for the mouse), so one transform there
 * keeps drawing and clicking together. The HUD wrapper can't do this: it only sees the closed chat.
 *
 * <p>Chat splits each message into lines once, when it arrives, so its font goes into the lines' styles at that
 * split rather than through {@code HudFonts.active}: every chat view and its hovers then agree. When the font
 * changes (editor, profile switch, resource reload) the lines are split again.
 */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
    @Unique
    private FontDescription huddable$splitFont;
    @Unique
    private int huddable$splitGeneration;

    @Shadow
    public abstract void rescaleChat();

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At("HEAD"))
    private void huddable$moveChat(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks,
                                   ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        FontDescription font = HudFonts.chatFont();
        if (!Objects.equals(font, huddable$splitFont) || HudFonts.generation() != huddable$splitGeneration) {
            huddable$splitFont = font;
            huddable$splitGeneration = HudFonts.generation();
            rescaleChat();
        }
        ElementLayout layout = Profiles.current().get(Element.CHAT);
        float scale = HudWrapper.scale(Element.CHAT, layout);
        if (layout.dx == 0 && layout.dy == 0 && scale == 1f) return;
        graphics.updatePose(pose -> HudWrapper.transform(pose, Element.CHAT_ANCHOR_X, screenHeight - Element.CHAT_BOTTOM_OFFSET, layout, scale));
    }

    /** Splits with the chat font as the message's base style; parts that name their own font keep it. */
    @WrapOperation(method = "addMessageToDisplayQueue",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/chat/GuiMessage;splitLines(Lnet/minecraft/client/gui/Font;I)Ljava/util/List;"))
    private List<FormattedCharSequence> huddable$splitInFont(GuiMessage message, Font font, int maxWidth,
                                                             Operation<List<FormattedCharSequence>> original) {
        FontDescription chatFont = HudFonts.chatFont();
        if (chatFont == null) return original.call(message, font, maxWidth);
        Component content = Component.empty().withStyle(style -> style.withFont(chatFont)).append(message.content());
        GuiMessage styled = new GuiMessage(message.addedTime(), content, message.signature(), message.source(), message.tag());
        return original.call(styled, font, maxWidth);
    }
}
