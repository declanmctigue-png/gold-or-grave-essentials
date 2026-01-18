package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GoGPermsListener {

    private final Stores stores;

    public GoGPermsListener(Stores stores) {
        this.stores = stores;
    }

    // Try common join event method signatures by name
    public void onPlayerJoin(Object event) {
        Object player = extractPlayer(event);
        UUID uuid = extractUuid(player);
        String username = extractUsername(player);

        if (uuid != null) {
            stores.permManager().ensureUserLoaded(uuid, username);
        }
    }

    // Try common leave event method signatures by name
    public void onPlayerLeave(Object event) {
        Object player = extractPlayer(event);
        UUID uuid = extractUuid(player);

        if (uuid != null) {
            stores.permManager().unloadUser(uuid);
        }
    }

    private static Object extractPlayer(Object event) {
        if (event == null) return null;

        Object p = invoke0(event, "getPlayer");
        if (p != null) return p;

        p = invoke0(event, "player");
        if (p != null) return p;

        p = invoke0(event, "getEntity");
        if (p != null) return p;

        p = invoke0(event, "entity");
        return p;
    }

    private static UUID extractUuid(Object player) {
        if (player == null) return null;

        Object v = invoke0(player, "getUuid");
        if (v instanceof UUID id) return id;

        return null;
    }

    private static String extractUsername(Object player) {
        if (player == null) return null;

        Object v = invoke0(player, "getUsername");
        if (v instanceof String s) return s;

        return null;
    }

    private static Object invoke0(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            m.setAccessible(true);
            return m.invoke(obj);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
