// File: com/goldorgrave/essentials/GoGPlayers.java
package com.goldorgrave.essentials;

import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GoGPlayers {
    private GoGPlayers() {}

    public static UUID requirePlayer(CommandContext ctx) {
        UUID uuid = getUuid(ctx.sender());
        if (uuid == null) {
            GoGInput.send(ctx, "&c[GoG] Player only command");
            return null;
        }
        return uuid;
    }

    public static UUID getUuid(Object sender) {
        if (sender == null) return null;
        try {
            Method m = sender.getClass().getMethod("getUuid");
            return (UUID) m.invoke(sender);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static PlayerRef findPlayerRefByName(String username) {
        if (username == null || username.isBlank()) return null;

        try {
            Universe u = Universe.get();
            if (u == null) return null;

            PlayerRef pr = u.getPlayerByUsername(username, NameMatching.STARTS_WITH_IGNORE_CASE);
            if (pr == null) pr = u.getPlayerByUsername(username, NameMatching.EXACT_IGNORE_CASE);
            return pr;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object findPlayerEntityByName(CommandContext ctx, String name) {
        try {
            Object server = GoGReflection.invoke(ctx, "getServer");
            if (server == null) server = GoGReflection.invoke(ctx, "server");
            if (server == null) return null;

            Object pm = GoGReflection.invoke(server, "getPlayerManager");
            if (pm == null) pm = GoGReflection.invoke(server, "playerManager");
            if (pm == null) return null;

            Object p = GoGReflection.invoke(pm, "getPlayerByName", name);
            if (p == null) p = GoGReflection.invoke(pm, "findByName", name);
            if (p == null) p = GoGReflection.invoke(pm, "getPlayer", name);
            return p;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static LivingEntity asLivingEntity(Object sender) {
        if (sender == null) return null;
        if (sender instanceof LivingEntity le) return le;

        Object v = GoGReflection.invoke(sender, "getPlayer");
        if (v instanceof LivingEntity le) return le;
        v = GoGReflection.invoke(sender, "player");
        if (v instanceof LivingEntity le) return le;

        v = GoGReflection.invoke(sender, "getEntity");
        if (v instanceof LivingEntity le) return le;
        v = GoGReflection.invoke(sender, "entity");
        if (v instanceof LivingEntity le) return le;

        return null;
    }
}
