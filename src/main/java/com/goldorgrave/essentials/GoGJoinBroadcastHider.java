package com.goldorgrave.essentials;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;

import java.lang.reflect.Method;

public final class GoGJoinBroadcastHider {

    // Hide default join message
    public void onAddPlayerToWorld(AddPlayerToWorldEvent event) {
        if (event == null) return;

        // false = do NOT show the default "X has joined" message
        event.setBroadcastJoinMessage(false);
    }

    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event == null) return;

        System.out.println("[GoG] PlayerDisconnectEvent fired for " +
                (event.getPlayerRef() != null ? event.getPlayerRef().getUsername() : "unknown"));

        try {
            Method m = event.getClass().getMethod("setBroadcastLeaveMessage", boolean.class);
            m.invoke(event, false);
            System.out.println("[GoG] setBroadcastLeaveMessage(false) succeeded");
        } catch (NoSuchMethodException e) {
            System.out.println("[GoG] setBroadcastLeaveMessage method NOT found on this build");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}