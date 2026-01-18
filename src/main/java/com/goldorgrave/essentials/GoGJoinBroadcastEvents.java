// File: com/goldorgrave/essentials/GoGJoinBroadcastEvents.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;

public final class GoGJoinBroadcastEvents {

    private static final String KEY = "joinbroadcast.template";

    private final Stores stores;

    public GoGJoinBroadcastEvents(Stores stores) {
        this.stores = stores;
    }

    // Run when the player is actually ready
    public void onPlayerReady(PlayerReadyEvent event) {
        if (event == null || stores == null) return;

        Player player = event.getPlayer();
        if (player == null) return;

        String template = stores.getJoinBroadcastMessage();
        if (template == null || template.isBlank()) return;

        String name = safeDisplayName(player);
        String uuid = safeUuid(player);

        String rendered = render(template, name, uuid);

        try {
            var world = player.getWorld();
            if (world == null) return;
            var entityStore = world.getEntityStore();
            if (entityStore == null) return;
            var store = entityStore.getStore();
            if (store == null) return;

            PlayerUtil.broadcastMessageToPlayers(null, MessageUtil.legacy(rendered), store);
        } catch (Throwable t) {
            System.out.println("[GoG] join broadcast failed: " + t);
        }
    }

    // Hide or show the default join message
    public void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
        if (event == null || stores == null) return;

        String template = stores.getJoinBroadcastMessage();
        boolean hasCustom = template != null && !template.isBlank();

        // false hides default join broadcast
        event.setBroadcastJoinMessage(!hasCustom);
    }

    private static String render(String template, String playerName, String uuid) {
        String out = template;

        // multiline support
        out = out.replace("||", "\n").replace("\\n", "\n");

        // tokens
        out = out.replace("%player%", playerName);
        out = out.replace("%user%", playerName);
        out = out.replace("%uuid%", uuid);

        return out;
    }

    private static String safeDisplayName(Player p) {
        try {
            String dn = p.getDisplayName();
            if (dn != null && !dn.isBlank()) return dn;
        } catch (Throwable ignored) {}
        return "player";
    }

    private static String safeUuid(Player p) {
        try {
            Object id = p.getUuid();
            if (id != null) return id.toString();
        } catch (Throwable ignored) {}
        return "";
    }
}
