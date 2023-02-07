package io.github.apace100.smwyg;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.message.*;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowMeWhatYouGot implements ModInitializer {

	public static final String MODID = "smwyg";
	public static final Logger LOGGER = LogManager.getLogger(ShowMeWhatYouGot.class);

	public static final Identifier PACKET_ID = new Identifier(MODID, "share_item");

	public static final String HIDE_STACK_NBT = MODID + ":hide_stack";

	@Override
	public void onInitialize() {

		ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, ((sender, message) -> {
			return CompletableFuture.completedFuture(parseMessage(message));
		}));

		ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, ((minecraftServer, serverPlayerEntity, serverPlayNetworkHandler, packetByteBuf, packetSender) -> {
			String before = packetByteBuf.readString();
			NbtCompound nbt = packetByteBuf.readNbt();
			ItemStack stack = ItemStack.fromNbt(nbt);
			String after = packetByteBuf.readString();
			minecraftServer.execute(() -> {
				MutableText beforeText = Text.literal(before);
				MutableText afterText = Text.literal(after);
				Text completeText = beforeText.append(stack.toHoverableText()).append(afterText);
				Registry<MessageType> registry = minecraftServer.getRegistryManager().get(RegistryKeys.MESSAGE_TYPE);
				int messageTypeId = registry.getRawId(registry.get(MessageType.CHAT));
				var uuid = UUID.randomUUID();
				/*UUID uUID = serverPlayerEntity.getUuid();new PlayerListEntry(entry.profile(), this.isSecureChatEnforced())
				PlayerListEntry playerListEntry = this.getPlayerListEntry(uUID);
				if (playerListEntry == null) {
					this.connection.disconnect(CHAT_VALIDATION_FAILED_TEXT);
					return;
				}
				PublicPlayerSession publicPlayerSession = playerListEntry.getSession();
				MessageLink messageLink = publicPlayerSession != null ? new MessageLink(packet.index(), uUID, publicPlayerSession.sessionId()) : MessageLink.of(uUID);
				SignedMessage signedMessage = new SignedMessage(messageLink, packet.signature(), optional.get(), packet.unsignedContent(), packet.filterMask());
				var signer = new MessageLink(uuid, Instant.now(), (new Random()).nextLong());
				var signature = MessageSignatureData.EMPTY;*/

				/*
				var packet = new ChatMessageS2CPacket(uuid, 0, new MessageSignatureData(new byte[0]),
						new MessageBody.Serialized(messageTypeId, serverPlayerEntity.getDisplayName(), afterText));

				minecraftServer.getPlayerManager().sendToAll(new ChatMessageS2CPacket(
						new SignedMessage(
								new MessageHeader(signature, uuid),
								signature,
								new MessageBody(
										new DecoratedContents("", completeText),
										signer.timestamp(),
										signer.salt(),
										LastSeenMessageList.EMPTY
								),
								Optional.of(completeText),
								FilterMask.PASS_THROUGH
						),
						new MessageType.Serialized(messageTypeId, serverPlayerEntity.getDisplayName(), afterText)
				));*/
			});
		}));
	}

	private static final Pattern ITEM_REGEX = Pattern.compile("\\[\\[smwyg:(?<item>.*)\\]\\]");

	public static boolean hasSmwygItem(String text) {
		Matcher matcher = ITEM_REGEX.matcher(text);
		return matcher.find();
	}

	public static class SmwygItemMatch {
		public ItemStack stack;
		public int start;
		public int end;
	}

	public static SmwygItemMatch extractItem(String text) {
		Matcher matcher = ITEM_REGEX.matcher(text);
		if(!matcher.find()) {
			return null;
		}
		String itemNbt = matcher.group("item");
		int start = matcher.start();
		int end = matcher.end();
		SmwygItemMatch itemMatch = new SmwygItemMatch();
		itemMatch.start = start;
		itemMatch.end = end;
		try {
			NbtCompound nbt = new StringNbtReader(new StringReader(itemNbt)).parseCompound();
			itemMatch.stack = ItemStack.fromNbt(nbt);
		} catch (CommandSyntaxException ignored) {
		}
		return itemMatch;
	}

	public static Text parseMessage(Text message) {
		String msgContent = message.getString();
		Matcher matcher = ITEM_REGEX.matcher(msgContent);
		if(!matcher.find()) {
			return message;
		}
		String itemNbt = matcher.group("item");
		int start = matcher.start();
		int end = matcher.end();
		MutableText resultText = Text.literal(msgContent.substring(0, start));
		try {
			NbtCompound nbt = new StringNbtReader(new StringReader(itemNbt)).parseCompound();
			Text nbtText = ItemStack.fromNbt(nbt).toHoverableText();
			resultText.append(nbtText);
		} catch (CommandSyntaxException e) {
			resultText.append(Text.translatable("smwyg.chat.stale_link"));
		}
		resultText.append(Text.literal(msgContent.substring(end)));

		return resultText;
	}
}
