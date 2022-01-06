package io.github.apace100.smwyg.mixin;

import io.github.apace100.smwyg.tooltip.HorizontalLayoutTooltipComponent;
import io.github.apace100.smwyg.tooltip.ItemStackTooltipComponent;
import io.github.apace100.smwyg.ShowMeWhatYouGot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Screen.class)
@Environment(EnvType.CLIENT)
public class ScreenMixin {

    @Shadow protected TextRenderer textRenderer;
    @Unique
    private ItemStack smwyg$StackToRender;

    @Inject(method = "renderTextHoverEffect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemStack;II)V"))
    private void cacheStackToRender(MatrixStack matrices, Style style, int x, int y, CallbackInfo ci) {
        HoverEvent hoverEvent = style.getHoverEvent();
        HoverEvent.ItemStackContent itemStackContent = hoverEvent.getValue(HoverEvent.Action.SHOW_ITEM);
        if(itemStackContent != null) {
            ItemStack stack = itemStackContent.asStack();
            if(stack.hasNbt()) {
                if(stack.getNbt().getBoolean(ShowMeWhatYouGot.HIDE_STACK_NBT)) {
                    return;
                }
            }
            smwyg$StackToRender = stack;
        }
    }

    @Inject(method = "renderTooltipFromComponents", at = @At("HEAD"))
    private void modifyFirstComponent(MatrixStack matrices, List<TooltipComponent> components, int x, int y, CallbackInfo ci) {
        if(smwyg$StackToRender == null || smwyg$StackToRender.isEmpty() || components.size() == 0) {
            return;
        }
        TooltipComponent originalComponent = components.get(0);
        TooltipComponent stackComponent = new ItemStackTooltipComponent(smwyg$StackToRender);
        TooltipComponent combinedComponent;
        if(this.textRenderer.isRightToLeft()) {
            combinedComponent = new HorizontalLayoutTooltipComponent(List.of(originalComponent, stackComponent), 3);
        } else {
            combinedComponent = new HorizontalLayoutTooltipComponent(List.of(stackComponent, originalComponent), 3);
        }
        components.set(0, combinedComponent);
        smwyg$StackToRender = null;
    }
}

