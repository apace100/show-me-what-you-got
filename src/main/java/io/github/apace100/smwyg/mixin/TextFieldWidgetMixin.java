package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.duck.ItemSharingTextFieldWidget;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
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
    // FIXME: normally, chat messages have whitespace trimmed and collapsed, but sending a tag message bypasses that
    // FIXME: add to message history

    @Shadow private String text;
    @Shadow private int selectionStart;
    @Shadow private int selectionEnd;

    private ItemStack itemStack;
    private String insertedString;
    private int insertedLength;
    private int insertedIndex = 0;

    @Override
    public void setStack(ItemStack stack) {
        this.itemStack = stack;
        Text text = stack.toHoverableText();
        this.insertedString = text.getString();
        this.insertedLength = insertedString.length();
    }

    @Override
    public ItemStack getStack() {
        return this.itemStack;
    }

    @Override
    public String getTextBefore() {
        if (insertedIndex < 0 || insertedIndex > this.text.length()) {
            Log.warn(LogCategory.GENERAL, "Prevented getTextBefore crash with text=%s length=%d insertedIndex=%d", this.text, this.text.length(), insertedIndex);
            return "";
        }

        return this.text.substring(0, insertedIndex);
    }

    @Override
    public String getTextAfter() {
        if (insertedIndex + insertedLength < 0 || insertedIndex + insertedLength > this.text.length()) {
            Log.warn(LogCategory.GENERAL, "Prevented getTextAfter crash with text=%s length=%d insertedIndex=%d insertedLength=%d", this.text, this.text.length(), insertedIndex, insertedLength);
            return "";
        }

        return this.text.substring(insertedIndex + insertedLength);
    }

    @Override
    public boolean hasStack() {
        return this.itemStack != null;
    }

    @Override
    public void onSuggestionInserted(int start, int offset) {
        if (this.hasStack() && start <= insertedIndex) {
            this.insertedIndex += offset;
        }
    }

    @Inject(method = "keyPressed", at = @At("TAIL"))
    private void logKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        Log.info(LogCategory.GENERAL, "insertedString=\"%s\" insertedIndex=%d insertedLength=%d", insertedString, insertedIndex, insertedLength);
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
                while (skipPos < this.text.length() && this.text.charAt(skipPos) == ' ') {
                    ++skipPos;
                }
                cir.setReturnValue(skipPos);
            }
        }
    }

    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionStart(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void moveInsertedText(String text, CallbackInfo ci, int i, int j, int k, String string, int l, String string2) {
        if (!this.hasStack()) {
            return;
        }

        if (i <= insertedIndex && j <= insertedIndex) {
            this.insertedIndex += l;
        }
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
            return insertedIndex + insertedLength + 1;
        } else {
            return original;
        }
    }

    @Inject(method = "eraseCharacters", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;write(Ljava/lang/String;)V"))
    private void eraseHighlighted(CallbackInfo ci) {
        if (!this.hasStack()) {
            return;
        }

        // handles highlighting backwards and forwards
        // the range to be deleted is [left,right)
        int left = Math.min(this.selectionStart, this.selectionEnd);
        int right = Math.max(this.selectionStart, this.selectionEnd);

        boolean isErasingBefore = left < insertedIndex && right <= insertedIndex;

        // selecting a range inside the tag is not typically possible, but handle it anyway
        boolean isLeftInside = left > insertedIndex && left < insertedIndex + insertedLength;
        boolean isRightInside = right > insertedIndex && right < insertedIndex + insertedLength;

        boolean isErasingEntireTag = left <= insertedIndex && right >= insertedIndex + insertedLength;

        Log.info(LogCategory.GENERAL, "ShowMeWhatYouGot: erasing selection. left=%d right=%d", left, right);
        Log.info(LogCategory.GENERAL, "isErasingBefore=%b isLeftInside=%b isRightInside=%b isErasingEntireTag=%b",
                isErasingBefore,
                isLeftInside,
                isRightInside,
                isErasingEntireTag
        );

        if (isLeftInside || isRightInside || isErasingEntireTag) {
            Log.info(LogCategory.GENERAL,
                    "ShowMeWhatYouGot: removing tag"
            );
            this.itemStack = null;
            this.insertedLength = 0;
            this.insertedString = null;
            this.insertedIndex = 0;
        } else if (isErasingBefore) {
            int offset = right - left;
            Log.info(LogCategory.GENERAL,
                    "ShowMeWhatYouGot: erasing %d before tag, insertedIndex=%d -> %d",
                    offset,
                    this.insertedIndex,
                    this.insertedIndex - offset
            );
            this.insertedIndex -= offset;
        }
    }

    @Inject(method = "eraseCharacters", at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;<init>(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void eraseInsertion(int characterOffset, CallbackInfo ci, int index, int indexStart, int indexEnd) {
        if (!this.hasStack()) {
            return;
        }

        Log.info(LogCategory.GENERAL, "==eraseInsertion==");
        Log.info(LogCategory.GENERAL, "insertedIndex=%d insertedLength=%d", insertedIndex, insertedLength);
        Log.info(LogCategory.GENERAL, "index=%d indexStart=%d indexEnd=%d", index, indexStart, indexEnd);

        // erasing inside the tag is not typically possible, but handle it anyway
        boolean isStartInside = indexStart > insertedIndex && indexStart < insertedIndex + insertedLength;
        boolean isEndInside = indexEnd > insertedIndex && indexEnd < insertedIndex + insertedLength;

        // It's this check that handles most cases of the tag being erased
        boolean isErasingTag = indexStart <= insertedIndex && indexEnd >= insertedIndex + insertedLength;

        Log.info(LogCategory.GENERAL, "isStartInside=%b isEndInside=%b isErasingTag=%b", isStartInside, isEndInside, isErasingTag);

        if (isStartInside || isEndInside || isErasingTag) {
            this.itemStack = null;
            this.insertedLength = 0;
            this.insertedString = null;
            this.insertedIndex = 0;
            Log.info(LogCategory.GENERAL, "ShowMeWhatYouGot: erasing tag");
        } else {
            boolean isErasingBefore = indexStart <= insertedIndex || indexEnd <= insertedIndex;
            if (isErasingBefore) {
                int oldIdx = this.insertedIndex;
                this.insertedIndex -= (indexEnd - indexStart);
                Log.info(LogCategory.GENERAL, "ShowMeWhatYouGot: shifting tag backwards, %d - %d = %d", oldIdx, indexEnd - indexStart, this.insertedIndex);
            }
        }
    }

    @Inject(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;write(Ljava/lang/String;)V"))
    private void handleHighlightOverwrite(CallbackInfoReturnable<Boolean> cir) {
        if (!this.hasStack()) {
            return;
        }

        // handles highlighting backwards and forwards
        // the range to be overwritten is [left,right)
        int left = Math.min(this.selectionStart, this.selectionEnd);
        int right = Math.max(this.selectionStart, this.selectionEnd);

        if (left == right) {
            return;
        }

        boolean isErasingBefore = left < insertedIndex && right <= insertedIndex;

        // selecting a range inside the tag is not typically possible, but handle it anyway
        boolean isLeftInside = left > insertedIndex && left < insertedIndex + insertedLength;
        boolean isRightInside = right > insertedIndex && right < insertedIndex + insertedLength;

        boolean isErasingEntireTag = left <= insertedIndex && right >= insertedIndex + insertedLength;

        Log.info(LogCategory.GENERAL, "ShowMeWhatYouGot: overwriting selection. left=%d right=%d", left, right);
        Log.info(LogCategory.GENERAL, "isErasingBefore=%b isLeftInside=%b isRightInside=%b isErasingEntireTag=%b",
                isErasingBefore,
                isLeftInside,
                isRightInside,
                isErasingEntireTag
        );

        if (isLeftInside || isRightInside || isErasingEntireTag) {
            Log.info(LogCategory.GENERAL,
                    "ShowMeWhatYouGot: removing tag"
            );
            this.itemStack = null;
            this.insertedLength = 0;
            this.insertedString = null;
            this.insertedIndex = 0;
        } else if (isErasingBefore) {
            int offset = right - left;
            Log.info(LogCategory.GENERAL,
                    "ShowMeWhatYouGot: offset %d before tag, insertedIndex=%d -> %d",
                    offset,
                    this.insertedIndex,
                    this.insertedIndex - offset
            );
            this.insertedIndex -= offset;
            // further index offset of +1 is handled in the write() mixin.
        }
    }
}
