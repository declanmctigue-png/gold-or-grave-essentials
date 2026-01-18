// File: com/goldorgrave/essentials/GoGPermissions.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.lang.reflect.Method;
import java.util.UUID;

public final class GoGPermissions {
    private GoGPermissions() {}

    // OP exception + perms
    public static boolean has(CommandContext ctx, Stores stores, UUID uuid, String node) {
        if (node == null || node.isBlank()) return true;
        if (uuid == null) return false;

        if (isOp(ctx)) return true;

        return stores.permManager().hasPermission(uuid, node);
    }

    // Best-effort OP detection across builds
    public static boolean isOp(CommandContext ctx) {
        Object sender = (ctx == null) ? null : ctx.sender();
        if (sender == null) return false;

        // 1) If sender implements PermissionHolder, use it
        try {
            if (sender instanceof com.hypixel.hytale.server.core.permissions.PermissionHolder ph) {
                return ph.hasPermission("*")
                        || ph.hasPermission("hytale.command")
                        || ph.hasPermission("hytale.command.gamemode")
                        || ph.hasPermission("hytale.command.perm")
                        || ph.hasPermission("server.admin");
            }
        } catch (Throwable ignored) {}

        // 2) Reflection probes
        Object unwrapped = unwrapSender(sender);

        Boolean b;

        b = asBoolean(GoGReflection.invoke0(unwrapped, "isOp"));
        if (b != null) return b;

        b = asBoolean(GoGReflection.invoke0(unwrapped, "isOperator"));
        if (b != null) return b;

        b = asBoolean(GoGReflection.invoke0(unwrapped, "isAdmin"));
        if (b != null) return b;

        b = asBoolean(GoGReflection.invoke0(unwrapped, "hasOperatorPermissions"));
        if (b != null) return b;

        // 3) Permission level style APIs
        Object lvl = GoGReflection.invoke0(unwrapped, "getPermissionLevel");
        if (lvl instanceof Number n) return n.intValue() > 0;

        lvl = GoGReflection.invoke0(unwrapped, "getOperatorLevel");
        if (lvl instanceof Number n) return n.intValue() > 0;

        // 4) Last resort: hasPermission via reflection
        if (senderHasPermission(sender, "*")) return true;
        if (senderHasPermission(sender, "hytale.command.perm")) return true;
        if (senderHasPermission(sender, "hytale.command.gamemode")) return true;

        return false;
    }

    public static Object unwrapSender(Object sender) {
        if (sender == null) return null;

        Object v;
        v = GoGReflection.invoke0(sender, "getPlayer");
        if (v != null) return v;
        v = GoGReflection.invoke0(sender, "player");
        if (v != null) return v;

        v = GoGReflection.invoke0(sender, "getEntity");
        if (v != null) return v;
        v = GoGReflection.invoke0(sender, "entity");
        if (v != null) return v;

        return sender;
    }

    private static Boolean asBoolean(Object v) {
        return (v instanceof Boolean b) ? b : null;
    }

    private static boolean senderHasPermission(Object sender, String perm) {
        try {
            Method m = sender.getClass().getMethod("hasPermission", String.class);
            Object r = m.invoke(sender, perm);
            return (r instanceof Boolean b) && b;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
