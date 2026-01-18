package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

public final class GoGJoinBroadcastFeature {

    private final Stores stores;

    public GoGJoinBroadcastFeature(Stores stores) {
        this.stores = stores;
        System.out.println("[GoG] GoGJoinBroadcastFeature constructed!");
    }

    // This suppresses the default join message
    public void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
        System.out.println("[GoG] onAddPlayerToWorld called!");
        if (event == null) {
            System.out.println("[GoG] Event is null!");
            return;
        }
        event.setBroadcastJoinMessage(false);
        System.out.println("[GoG] Default join message suppressed");
    }

    // This sends our custom join message
    public void onPlayerReady(PlayerReadyEvent event) {
        System.out.println("[GoG] onPlayerReady called!");

        if (event == null) {
            System.out.println("[GoG] Event is null!");
            return;
        }

        String template = stores.getJoinBroadcastMessage();
        System.out.println("[GoG] Template from stores: '" + template + "'");

        if (template == null || template.isBlank()) {
            System.out.println("[GoG] Template is null or blank, aborting");
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            System.out.println("[GoG] Player is null!");
            return;
        }

        // Get player name
        String playerName = getPlayerName(player);
        System.out.println("[GoG] Player name: " + playerName);

        // Get UUID
        String uuid = "";
        try {
            Object uuidObj = player.getUuid();
            if (uuidObj != null) {
                uuid = uuidObj.toString();
            }
        } catch (Exception e) {
            System.out.println("[GoG] Error getting UUID: " + e.getMessage());
        }
        System.out.println("[GoG] UUID: " + uuid);

        // Get world name
        String worldName = getWorldName(player);
        System.out.println("[GoG] World name: " + worldName);

        // Render the message
        String rendered = render(template, playerName, uuid, worldName);
        System.out.println("[GoG] Rendered message: '" + rendered + "'");

        // Send the message
        try {
            Message msg = MessageUtil.legacy(rendered);
            System.out.println("[GoG] Message object created, sending to Universe...");
            Universe.get().sendMessage(msg);
            System.out.println("[GoG] Message sent successfully!");
        } catch (Exception e) {
            System.out.println("[GoG] Error sending message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String getPlayerName(Player player) {
        // Try display name first
        try {
            String dn = player.getDisplayName();
            if (dn != null && !dn.isBlank()) {
                return dn;
            }
        } catch (Exception e) {
            System.out.println("[GoG] Error getting display name: " + e.getMessage());
        }

        // Try reflection for username
        try {
            Object username = player.getClass().getMethod("getUsername").invoke(player);
            if (username instanceof String && !((String)username).isBlank()) {
                return (String) username;
            }
        } catch (Exception e) {
            System.out.println("[GoG] Error getting username via reflection: " + e.getMessage());
        }

        return "player";
    }

    private static String getWorldName(Player player) {
        try {
            World world = player.getWorld();
            if (world != null) {
                String name = world.getName();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
        } catch (Exception e) {
            System.out.println("[GoG] Error getting world name: " + e.getMessage());
        }
        return "Unknown";
    }

    private static String render(String template, String playerName, String uuid, String worldName) {
        if (template == null) return "";

        String out = normalizeTemplate(template);

        // Multiline support
        out = out.replace("||", "\n").replace("\\n", "\n");

        String p = (playerName == null || playerName.isBlank()) ? "player" : playerName;
        String u = (uuid == null) ? "" : uuid;
        String w = (worldName == null || worldName.isBlank()) ? "Unknown" : worldName;

        // Direct replacements
        out = out.replace("%player%", p);
        out = out.replace("%user%", p);
        out = out.replace("%uuid%", u);
        out = out.replace("%world%", w);

        return out;
    }

    private static String normalizeTemplate(String s) {
        if (s == null) return "";

        // Convert fullwidth percent to ASCII percent
        s = s.replace('\uFF05', '%');

        // Strip common zero width chars
        s = s.replace("\u200B", "")
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\uFEFF", "");

        return s;
    }
}