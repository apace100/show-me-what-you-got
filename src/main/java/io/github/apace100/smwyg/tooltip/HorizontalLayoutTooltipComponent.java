package io.github.apace100.smwyg.tooltip;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import java.util.List;

public class HorizontalLayoutTooltipComponent implements TooltipComponent {

    private final List<TooltipComponent> components;
    private final int gap;

    private final int height;

    public HorizontalLayoutTooltipComponent(List<TooltipComponent> components, int gap) {
        this.components = components;
        this.gap = gap;
        int h = 0;
        for(TooltipComponent tc : components) {
            if(tc.getHeight() > h) {
                h = tc.getHeight();
            }
        }
        height = h;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        int sumOfWidths = 0;
        for(TooltipComponent tc : components) {
            sumOfWidths += tc.getWidth(textRenderer);
        }
        sumOfWidths += gap * (components.size() - 1);
        return sumOfWidths;
    }

    private int getComponentY(TooltipComponent component) {
        int height = component.getHeight();
        return (this.height - height) / 2;
    }

    @Override
    public void drawText(TextRenderer textRenderer, int x, int y, Matrix4f matrix, VertexConsumerProvider.Immediate vertexConsumers) {
        int currentX = x;
        for(TooltipComponent tc : components) {
            tc.drawText(textRenderer, currentX, y + getComponentY(tc), matrix, vertexConsumers);
            currentX += tc.getWidth(textRenderer) + gap;
        }
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z, TextureManager textureManager) {
        int currentX = x;
        for(TooltipComponent tc : components) {
            tc.drawItems(textRenderer, currentX, y + getComponentY(tc), matrices, itemRenderer, z, textureManager);
            currentX += tc.getWidth(textRenderer) + gap;
        }
    }
}
