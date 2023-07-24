package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.ShowMeWhatYouGot;
import io.github.apace100.smwyg.duck.ItemSharingTextFieldWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin implements ItemSharingTextFieldWidget {
    @Shadow private String text;

    private ItemStack itemStack;
    private String insertedString;
    private int insertedLength;
    private int insertedIndex = 0;

    @Override
    public void setStack(ItemStack stack) {
        itemStack = stack;
        Text text = stack.toHoverableText();
        insertedString = text.getString();
        insertedLength = insertedString.length();
    }

    @Override
    public ItemStack getStack() {
        return itemStack;
    }

    @Override
    public String getTextBefore() {
        if (insertedIndex < 0 || insertedIndex > text.length()) {
            ShowMeWhatYouGot.LOGGER.warn("Invalid tag positioning in getTextBefore");
            ShowMeWhatYouGot.LOGGER.warn("text={} length={} insertedIndex={} insertedLength={}",
                    text, text.length(), insertedIndex, insertedLength);
            return "";
        }

        return text.substring(0, insertedIndex);
    }

    @Override
    public String getTextAfter() {
        if (insertedIndex + insertedLength < 0 || insertedIndex + insertedLength > text.length()) {
            ShowMeWhatYouGot.LOGGER.warn("Invalid tag positioning in getTextAfter");
            ShowMeWhatYouGot.LOGGER.warn("text={} length={} insertedIndex={} insertedLength={}",
                    text, text.length(), insertedIndex, insertedLength);
            return "";
        }

        return text.substring(insertedIndex + insertedLength);
    }

    @Override
    public boolean hasStack() {
        return itemStack != null;
    }

    @Override
    public void onSuggestionInserted(int start, int offset) {
        if (this.hasStack() && start <= insertedIndex) {
            insertedIndex += offset;
        }
    }

    @Inject(method = "getCursorPosWithOffset", at = @At("RETURN"), cancellable = true)
    private void modifyCursorOffset(int offset, CallbackInfoReturnable<Integer> cir) {
        if (!this.hasStack()) {
            return;
        }

        int original = cir.getReturnValue();
        if (original > insertedIndex && original < insertedIndex + insertedLength) {
            if (offset < 0) {
                cir.setReturnValue(insertedIndex);
            } else {
                cir.setReturnValue(insertedIndex + insertedLength);
            }
        }
    }

    @Inject(method = "getWordSkipPosition", at = @At("RETURN"), cancellable = true)
    private void modifyWordSkipPosition(int wordOffset, CallbackInfoReturnable<Integer> cir) {
        if (!this.hasStack()) {
            return;
        }

        int original = cir.getReturnValue();
        if (original > insertedIndex && original < insertedIndex + insertedLength) {
            if (wordOffset < 0) {
                cir.setReturnValue(insertedIndex);
            } else {
                int skipPos = insertedIndex + insertedLength;
                // when skipping forwards over a tag, keep going until we hit the beginning of the next word
                // or the end of the text
                while (skipPos < text.length() && text.charAt(skipPos) == ' ') {
                    ++skipPos;
                }
                cir.setReturnValue(skipPos);
            }
        }
    }

    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionStart(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void moveInsertedText(String rawText, CallbackInfo ci, int start, int end, int spaceRemaining, String sanitizedText, int sanitizedTextLength, String newText) {
        if (!this.hasStack()) {
            return;
        }

        handleOverwrite(start, end, sanitizedTextLength);
    }

    @ModifyVariable(method = "setCursor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyCursorSetting(int original) {
        if (!this.hasStack()) {
            return original;
        }

        if (original > insertedIndex && original < insertedIndex + insertedLength) {
            if (original <= insertedIndex + (insertedLength / 2)) {
                return insertedIndex;
            } else {
                return insertedIndex + insertedLength;
            }
        } else {
            return original;
        }
    }

    @ModifyVariable(method = "setSelectionStart", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifySelectionStart(int original) {
        if (!this.hasStack()) {
            return original;
        }

        if (original > insertedIndex && original < insertedIndex + insertedLength) {
            return insertedIndex;
        } else {
            return original;
        }
    }

    @ModifyVariable(method = "setSelectionEnd", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifySelectionEnd(int original) {
        if (!this.hasStack()) {
            return original;
        }

        if (original > insertedIndex && original < insertedIndex + insertedLength) {
            return insertedIndex + insertedLength;
        } else {
            return original;
        }
    }

    // all input actions are performed through calls to write(), except for backspace/delete when nothing is selected. handle that here.
    @Inject(method = "eraseCharacters", at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;<init>(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void eraseInsertion(int characterOffset, CallbackInfo ci, int index, int start, int end) {
        if (!this.hasStack()) {
            return;
        }

        handleOverwrite(start, end, 0);
    }

    private void handleOverwrite(int start, int end, int replacementLength) {
        // when no text is selected, start == end

        boolean isErasingEntireTag = start <= insertedIndex && end >= insertedIndex + insertedLength;
        // selecting a range inside the tag is not typically possible, but handle it anyway
        boolean isStartInside = start > insertedIndex && start < insertedIndex + insertedLength;
        boolean isEndInside = end > insertedIndex && end < insertedIndex + insertedLength;
        boolean isErasingBefore = start <= insertedIndex && end <= insertedIndex;

        if (isErasingEntireTag || isStartInside || isEndInside) {
            reset();
        } else if (isErasingBefore) {
            int offset = replacementLength - (end - start);
            insertedIndex += offset;
        }
    }

    private void reset() {
        itemStack = null;
        insertedLength = 0;
        insertedString = null;
        insertedIndex = 0;
    }
}
