// File: com/goldorgrave/essentials/GoGWhoAmIFeature.java
package com.goldorgrave.essentials;

import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGWhoAmIFeature {
    private GoGWhoAmIFeature() {}

    public static CompletableFuture<Void> handleWhoAmI(CommandContext ctx) {
        UUID uuid = GoGPlayers.getUuid(ctx.sender());
        if (uuid == null) GoGInput.send(ctx, "&c[GoG] Player only command");
        else GoGInput.send(ctx, "&a[GoG] Your UUID: &f" + uuid);
        return GoGInput.done();
    }
}
