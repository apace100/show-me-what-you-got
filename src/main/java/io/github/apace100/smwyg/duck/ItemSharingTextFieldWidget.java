package io.github.apace100.smwyg.duck;

import net.minecraft.item.ItemStack;

public interface ItemSharingTextFieldWidget {

    void setStack(ItemStack stack);
    ItemStack getStack();
    String getTextBefore();
    String getTextAfter();
    boolean hasStack();
    void onSuggestionInserted(int start, int offset);
    void reset();
    int getInsertionStart();
    int getInsertionEnd();
}
