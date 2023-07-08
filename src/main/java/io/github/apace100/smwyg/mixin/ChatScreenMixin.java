package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.ShowMeWhatYouGotClient;
import io.github.apace100.smwyg.duck.ItemSharingTextFieldWidget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true)
    private boolean smwyg$sendItemSharingMessage(boolean addToHistory) {
        if(this.chatField instanceof ItemSharingTextFieldWidget istfw) {
            if(!istfw.hasStack()) {
                return addToHistory;
            }
            String before = istfw.getTextBefore();
            ItemStack stack = istfw.getStack();
            String after = istfw.getTextAfter();
            NbtCompound nbt = new NbtCompound();
            stack.writeNbt(nbt);
            String stackString = "[[smwyg:" + nbt + "]]";
            ShowMeWhatYouGotClient.sendItemSharingMessage(istfw.getInsertionStart(), istfw.getInsertionEnd(), stack);
            this.client.inGameHud.getChatHud().addToMessageHistory(before + stackString + after);
            return false;
        }
        return addToHistory;
    }
}
