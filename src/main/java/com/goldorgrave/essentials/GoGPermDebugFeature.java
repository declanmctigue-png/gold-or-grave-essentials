// File: com/goldorgrave/essentials/GoGPermDebugFeature.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.goldorgrave.essentials.util.MessageUtil;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGPermDebugFeature {

    private final Stores stores;

    public GoGPermDebugFeature(Stores stores) {
        this.stores = stores;
    }

    public CompletableFuture<Void> handlePermDebug(CommandContext ctx) {
        Object sender = ctx.sender();

        GoGInput.send(ctx, "&6[GoG] senderClass=&f" + (sender == null ? "null" : sender.getClass().getName()));

        Object unwrapped = GoGPermissions.unwrapSender(sender);
        GoGInput.send(ctx, "&6[GoG] unwrappedClass=&f" + (unwrapped == null ? "null" : unwrapped.getClass().getName()));

        UUID uuid = GoGPlayers.getUuid(ctx.sender());
        if (uuid != null) {
            String group = stores.permManager().getPrimaryGroup(uuid);
            GoGInput.send(ctx, "&6[GoG] primaryGroup=&f" + (group == null ? "default" : group));

            String prefix = "-";
            try {
                prefix = GoGInput.safe(stores.permManager().getChatMeta(uuid).getPrefix());
            } catch (Throwable ignored) {}

            GoGInput.send(ctx, "&6[GoG] chatPrefix(raw)=&f" + prefix);
            if (!prefix.isBlank()) {
                GoGInput.send(ctx, "&6[GoG] chatPrefix(rendered):");
                ctx.sender().sendMessage(MessageUtil.legacy(prefix + "&r &7(player)"));
            }
        }

        if (unwrapped != null) {
            String[] probes = new String[] {
                    "isOp", "isOperator", "isAdmin", "hasOperatorPermissions",
                    "getPermissionLevel", "getRole", "getRank", "getOperatorLevel"
            };

            for (String p : probes) {
                Object v = GoGReflection.invoke0(unwrapped, p);
                if (v != null) GoGInput.send(ctx, "&6[GoG] &f" + p + "() -> " + v + " (" + v.getClass().getSimpleName() + ")");
            }

            int shown = 0;
            for (Method m : unwrapped.getClass().getMethods()) {
                if (shown >= 25) break;
                String n = m.getName().toLowerCase(Locale.ROOT);
                if (n.contains("op") || n.contains("admin") || n.contains("perm") || n.contains("role") || n.contains("rank")) {
                    GoGInput.send(ctx, "&7[GoG] method: &f" + m.getName() + "(" + m.getParameterCount() + ")");
                    shown++;
                }
            }
        }

        return GoGInput.done();
    }
}
