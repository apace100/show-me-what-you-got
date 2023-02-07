package io.github.apace100.smwyg.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class ItemStackTooltipComponent implements TooltipComponent {

    private final ItemStack stack;

    public ItemStackTooltipComponent(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return 18;
    }

    @Override
    public int getHeight() {
        return 18;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z) {
        itemRenderer.renderInGui(stack, x, y);
        int count = stack.getCount();
        String countLabel = "";
        if(count > 1) {
            countLabel = "" + stack.getCount();
        }
        itemRenderer.renderGuiItemOverlay(textRenderer, stack, x, y, countLabel);
    }
}
