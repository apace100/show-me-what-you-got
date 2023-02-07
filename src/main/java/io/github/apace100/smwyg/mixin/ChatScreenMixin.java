package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.ShowMeWhatYouGotClient;
import io.github.apace100.smwyg.duck.ItemSharingTextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {

    @Shadow protected TextFieldWidget chatField;

    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Shadow public abstract boolean sendMessage(String chatText, boolean addToHistory);

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setText(Ljava/lang/String;)V"))
    private void setItemSharingText(CallbackInfo ci) {
        if(ShowMeWhatYouGotClient.sharingItem != null) {
            if(this.chatField instanceof ItemSharingTextFieldWidget istfw) {
                istfw.setStack(ShowMeWhatYouGotClient.sharingItem);
                ShowMeWhatYouGotClient.sharingItem = null;
            }
        }
    }

    @Inject(method = "setChatFromHistory", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setText(Ljava/lang/String;)V"))
    private void removeSetItem(int offset, CallbackInfo ci) {
        if(this.chatField instanceof ItemSharingTextFieldWidget istfw) {
            istfw.reset();
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
            NbtCompound nbt = new NbtCompound();
            stack.writeNbt(nbt);
            String stackString = "[[smwyg:" + nbt + "]]";
            ShowMeWhatYouGotClient.sendItemSharingMessage(istfw.getInsertionStart(), istfw.getInsertionEnd(), stack);
            this.sendMessage(this.chatField.getText(), false);
            this.client.inGameHud.getChatHud().addToMessageHistory(before + stackString + after);
            this.chatField.setText("");
        }
    }
}
