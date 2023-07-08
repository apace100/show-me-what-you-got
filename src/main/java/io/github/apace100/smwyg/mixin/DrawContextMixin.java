package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.ShowMeWhatYouGot;
import io.github.apace100.smwyg.tooltip.HorizontalLayoutTooltipComponent;
import io.github.apace100.smwyg.tooltip.ItemStackTooltipComponent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import net.minecraft.item.ItemStack;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(DrawContext.class)
public class DrawContextMixin {

    @Unique
    private ItemStack smwyg$hoveredStack;

    @Inject(method = "drawHoverEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawItemTooltip(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;II)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void smwyg$cacheHoveredStack(TextRenderer textRenderer, Style style, int x, int y, CallbackInfo ci, HoverEvent hoverEvent, HoverEvent.ItemStackContent itemStackContent) {
        ItemStack stack = itemStackContent.asStack();
        if(stack.hasNbt()) {
            if(stack.getNbt().getBoolean(ShowMeWhatYouGot.HIDE_STACK_NBT)) {
                return;
            }
        }
        smwyg$hoveredStack = stack;
    }

    @Inject(method = "drawTooltip(Lnet/minecraft/client/font/TextRenderer;Ljava/util/List;IILnet/minecraft/client/gui/tooltip/TooltipPositioner;)V", at = @At("HEAD"))
    private void smwyg$modifyFirstTooltipComponent(TextRenderer textRenderer, List<TooltipComponent> components, int x, int y, TooltipPositioner positioner, CallbackInfo ci) {
        if(smwyg$hoveredStack == null || smwyg$hoveredStack.isEmpty() || components.size() == 0) {
            return;
        }
        TooltipComponent originalComponent = components.get(0);
        TooltipComponent stackComponent = new ItemStackTooltipComponent(smwyg$hoveredStack);
        TooltipComponent combinedComponent;
        if(textRenderer.isRightToLeft()) {
            combinedComponent = new HorizontalLayoutTooltipComponent(List.of(originalComponent, stackComponent), 3);
        } else {
            combinedComponent = new HorizontalLayoutTooltipComponent(List.of(stackComponent, originalComponent), 3);
        }
        components.set(0, combinedComponent);
        smwyg$hoveredStack = null;
    }
}
