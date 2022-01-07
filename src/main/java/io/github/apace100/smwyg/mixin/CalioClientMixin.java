package io.github.apace100.smwyg.mixin;

import io.github.apace100.calio.CalioClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CalioClient.class, remap = false)
public class CalioClientMixin {

    @Inject(method = "lambda$onInitializeClient$0", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelCalioSharing(MinecraftClient focusedSlotAccessor, CallbackInfo ci) {
        ci.cancel();
    }
}
