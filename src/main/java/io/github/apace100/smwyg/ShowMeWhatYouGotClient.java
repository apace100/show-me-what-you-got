package io.github.apace100.smwyg;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.github.apace100.smwyg.mixin.HandledScreenFocusedSlotAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.screen.slot.Slot;
import org.lwjgl.glfw.GLFW;

public class ShowMeWhatYouGotClient implements ClientModInitializer {

    boolean sharedStack = false;
    public static ItemStack sharingItem;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.START_CLIENT_TICK.register(tick -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if(client.player != null && client.currentScreen instanceof HandledScreen) {
                HandledScreenFocusedSlotAccessor focusedSlotAccessor = (HandledScreenFocusedSlotAccessor)client.currentScreen;
                Slot focusedSlot = focusedSlotAccessor.getFocusedSlot();
                boolean isCtrlPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL);
                InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(client.options.chatKey);
                boolean isChatPressed = InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), key.getCode());
                if(isCtrlPressed && isChatPressed && !sharedStack) {
                    sharedStack = true;
                    if (client.player.currentScreenHandler.getCursorStack().isEmpty() && focusedSlot != null && focusedSlot.hasStack()) {
                        // Open chat with sharing item
                        sharingItem = focusedSlot.getStack();
                        client.setScreen(new ChatScreen(focusedSlot.getStack().toHoverableText().getString()));
                    }
                }
                if(sharedStack && (!isCtrlPressed || !isChatPressed)) {
                    sharedStack = false;
                }
            }
        });
    }

    public static void sendItemSharingMessage(int start, int end, ItemStack stack) {
        ClientPlayNetworking.send(new ItemSharingMessage(start, end, stack));
    }

    public static String stackToString(ItemStack stack) {
        DataResult<NbtElement> encoding = ItemStack.CODEC.encodeStart(getOps(), stack);
        return encoding.getOrThrow().toString();
    }

    public static ItemStack stackFromString(String itemStackString) {
        try {
            NbtCompound nbt = StringNbtReader.readCompound(itemStackString);
            DataResult<Pair<ItemStack, NbtElement>> decoding = ItemStack.CODEC.decode(getOps(), nbt);
            return decoding.getOrThrow().getFirst();
        } catch (CommandSyntaxException ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static DynamicOps<NbtElement> getOps() {
        return MinecraftClient.getInstance().world.getRegistryManager().getOps(NbtOps.INSTANCE);
    }
}
