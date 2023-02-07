package io.github.apace100.smwyg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Optional;

public class MessageItemList {

    private final HashMap<ServerPlayerEntity, SmwygItemMatch> sharedItems = new HashMap<>();

    public MessageItemList() {
        ServerLifecycleEvents.SERVER_STARTED.register((server -> sharedItems.clear()));
    }

    public void add(ServerPlayerEntity sender, SmwygItemMatch itemMatch) {
        sharedItems.put(sender, itemMatch);
    }

    public Optional<SmwygItemMatch> get(ServerPlayerEntity sender) {
        if(sharedItems.containsKey(sender)) {
            return Optional.of(sharedItems.remove(sender));
        }
        return Optional.empty();
    }
}
