package io.github.apace100.smwyg.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import io.github.apace100.smwyg.ShowMeWhatYouGot;
import io.github.apace100.smwyg.duck.ItemSharingTextFieldWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.CommandSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(CommandSuggestor.SuggestionWindow.class)
public class SuggestionWindowMixin {

    @Shadow @Final private List<Suggestion> suggestions;

    @Shadow private int selection;

    @Inject(method = "complete", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionStart(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void moveInsertedText(CallbackInfo ci, Suggestion suggestion, int i) {
        if(MinecraftClient.getInstance().currentScreen instanceof ChatScreen chatScreen) {
            TextFieldWidget textFieldWidget = ((ChatScreenAccessor)chatScreen).getChatField();
            if(textFieldWidget instanceof ItemSharingTextFieldWidget istfw) {
                int start = suggestion.getRange().getStart();
                int end = suggestion.getRange().getEnd();
                int offset = suggestion.getText().length() - (end - start);
                istfw.onSuggestionInserted(start, offset);
            }
        }
    }
}
