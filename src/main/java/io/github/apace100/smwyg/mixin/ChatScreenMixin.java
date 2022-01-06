package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.ShowMeWhatYouGotClient;
import io.github.apace100.smwyg.duck.ItemSharingTextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow protected TextFieldWidget chatField;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setText(Ljava/lang/String;)V"))
    private void setItemSharingText(CallbackInfo ci) {
        if(ShowMeWhatYouGotClient.sharingItem != null) {
            if(this.chatField instanceof ItemSharingTextFieldWidget istfw) {
                istfw.setStack(ShowMeWhatYouGotClient.sharingItem);
                ShowMeWhatYouGotClient.sharingItem = null;
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/ChatScreen;chatField:Lnet/minecraft/client/gui/widget/TextFieldWidget;", opcode = Opcodes.GETFIELD))
    private void sendItemSharingMessage(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if(this.chatField instanceof ItemSharingTextFieldWidget istfw) {
            if(!istfw.hasStack()) {
                return;
            }
            String before = istfw.getTextBefore();
            ItemStack stack = istfw.getStack();
            String after = istfw.getTextAfter();
            ShowMeWhatYouGotClient.sendItemSharingMessage(before, stack, after);
            this.chatField.setText("");
        }
    }
}
