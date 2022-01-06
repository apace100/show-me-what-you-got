package io.github.apace100.smwyg;

import io.github.apace100.smwyg.mixin.HandledScreenFocusedSlotAccessor;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.impl.util.version.SemanticVersionImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

public class ShowMeWhatYouGotClient implements ClientModInitializer {

    boolean sharedStack = false;

    @Override
    public void onInitializeClient() {
        Optional<ModContainer> calioMod = FabricLoader.getInstance().getModContainer("calio");
        if(calioMod.isPresent()) {
            try {
                Version highestCalioVersionWithItemSharing = Version.parse("1.4.2");
                if(calioMod.get().getMetadata().getVersion().compareTo(highestCalioVersionWithItemSharing) <= 0) {
                    ShowMeWhatYouGot.LOGGER.info("Calio <= 1.4.2 detected, turning off Show Me What You Got item sharing keybind.");
                    return;
                }
            } catch (VersionParsingException e) {
                ShowMeWhatYouGot.LOGGER.warn("Could not properly detect whether Calio was present. Possible side-effect: items are shared in chat twice.");
            }
        }
        ClientTickEvents.START_CLIENT_TICK.register(tick -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player != null && client.currentScreen instanceof HandledScreen) {
                HandledScreenFocusedSlotAccessor focusedSlotAccessor = (HandledScreenFocusedSlotAccessor)client.currentScreen;
                Slot focusedSlot = focusedSlotAccessor.getFocusedSlot();
                boolean isCtrlPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL);
                InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(client.options.keyChat);
                boolean isChatPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), key.getCode());
                if(isCtrlPressed && isChatPressed && !sharedStack) {
                    sharedStack = true;
                    if (client.player.currentScreenHandler.getCursorStack().isEmpty() && focusedSlot != null && focusedSlot.hasStack()) {
                        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
                        NbtCompound nbt = new NbtCompound();
                        focusedSlot.getStack().writeNbt(nbt);
                        buf.writeNbt(nbt);
                        ClientPlayNetworking.send(ShowMeWhatYouGot.PACKET_ID, buf);
                    }
                }
                if(sharedStack && (!isCtrlPressed || !isChatPressed)) {
                    sharedStack = false;
                }
            }
        });
    }
}
