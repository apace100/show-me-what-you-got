package io.github.apace100.smwyg;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ShowMeWhatYouGot implements ModInitializer {

	public static final String MODID = "smwyg";
	public static final Logger LOGGER = LogManager.getLogger(ShowMeWhatYouGot.class);

	public static final Identifier PACKET_ID = new Identifier(MODID, "share_item");

	public static final String HIDE_STACK_NBT = MODID + ":hide_stack";

	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, ((minecraftServer, serverPlayerEntity, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
			String before = packetByteBuf.readString();
			NbtCompound nbt = packetByteBuf.readNbt();
			ItemStack stack = ItemStack.fromNbt(nbt);
			String after = packetByteBuf.readString();
			minecraftServer.execute(() -> {
				MutableText beforeText = Text.literal(before);
				MutableText afterText = Text.literal(after);
				Text completeText = beforeText.append(stack.toHoverableText()).append(afterText);
				Text chatText = Text.translatable("chat.type.text", serverPlayerEntity.getDisplayName(), completeText);
				minecraftServer.getPlayerManager().broadcast(chatText, false);
			});
		}));
	}
}
