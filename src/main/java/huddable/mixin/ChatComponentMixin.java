package huddable.mixin;

import huddable.Element;
import huddable.ElementLayout;
import huddable.HudWrapper;
import huddable.Profiles;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves and scales the chat. The closed chat (HUD), the open chat (ChatScreen) and the chat screen's click test
 * all run through this method, and each of its graphics accesses applies {@code updatePose} to what it draws or
 * hit-tests (hovers invert the pose for the mouse), so one transform here keeps drawing and clicking together.
 * The HUD wrapper can't do this: it only sees the closed chat.
 */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At("HEAD"))
    private void huddable$moveChat(ChatComponent.ChatGraphicsAccess graphics, int screenHeight, int ticks,
                                   ChatComponent.DisplayMode displayMode, CallbackInfo ci) {
        ElementLayout layout = Profiles.current().get(Element.CHAT);
        float scale = HudWrapper.scale(Element.CHAT, layout);
        if (layout.dx == 0 && layout.dy == 0 && scale == 1f) return;
        graphics.updatePose(pose -> HudWrapper.transform(pose, Element.CHAT_ANCHOR_X, screenHeight - Element.CHAT_BOTTOM_OFFSET, layout, scale));
    }
}
