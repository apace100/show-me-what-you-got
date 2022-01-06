package io.github.apace100.smwyg.mixin;

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
        return this.text.substring(0, insertedIndex);
    }

    @Override
    public String getTextAfter() {
        return this.text.substring(insertedIndex + insertedLength);
    }

    @Override
    public boolean hasStack() {
        return this.itemStack != null;
    }

    @Override
    public void onSuggestionInserted(int start, int offset) {
        if(this.itemStack != null && start <= insertedIndex) {
            this.insertedIndex += offset;
        }
    }

    @Inject(method = "getCursorPosWithOffset", at = @At("RETURN"), cancellable = true)
    private void modifyCursorOffset(int offset, CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if(original > insertedIndex && original < insertedIndex + insertedLength) {
            if(offset < 0) {
                cir.setReturnValue(insertedIndex);
            } else {
                cir.setReturnValue(insertedIndex + insertedLength);
            }
        }
    }

    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setSelectionStart(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void moveInsertedText(String text, CallbackInfo ci, int i, int j, int k, String string, int l, String string2) {
        if(i <= insertedIndex && j <= insertedIndex) {
            this.insertedIndex += l;
        }
    }

    @ModifyVariable(method = "setCursor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifyCursorSetting(int original) {
        if(original > insertedIndex && original < insertedIndex + insertedLength) {
            if(original <= insertedIndex + (insertedLength / 2)) {
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
        if(original > insertedIndex && original < insertedIndex + insertedLength) {
            return insertedIndex;
        } else {
            return original;
        }
    }

    @Inject(method = "eraseCharacters", at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;<init>(Ljava/lang/String;)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void eraseInsertion(int characterOffset, CallbackInfo ci, int i, int j, int k) {
        boolean isStartInside = j > insertedIndex && j < insertedIndex + insertedLength;
        boolean isEndInside = k > insertedIndex && k < insertedIndex + insertedLength;
        if(isStartInside || isEndInside || (j < insertedIndex && k >= insertedIndex + insertedLength)) {
            this.itemStack = null;
            this.insertedLength = 0;
            this.insertedString = "";
            this.insertedIndex = 0;
        } else {
            if(j <= insertedIndex || k <= insertedIndex) {
                this.insertedIndex -= (k - j);
            }
        }
    }

    @ModifyVariable(method = "setSelectionEnd", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int modifySelectionEnd(int original) {
        if(original > insertedIndex && original < insertedIndex + insertedLength) {
            return insertedIndex;
        } else {
            return original;
        }
    }
}
