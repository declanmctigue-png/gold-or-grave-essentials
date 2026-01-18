// File: com/goldorgrave/essentials/GoGOwnerFeature.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGOwnerFeature {

    private final Stores stores;

    public GoGOwnerFeature(Stores stores) {
        this.stores = stores;
    }

    public CompletableFuture<Void> handleOwnerMe(CommandContext ctx) {
        UUID uuid = GoGPlayers.requirePlayer(ctx);
        if (uuid == null) return GoGInput.done();

        // Only OP can bootstrap owner now
        if (!GoGPermissions.isOp(ctx)) {
            if (!stores.permManager().hasPermission(uuid, "gog.commands.ownerme")) {
                GoGInput.send(ctx, "&c[GoG] OP only (or missing permission: &f gog.commands.ownerme&c)");
                return GoGInput.done();
            }
        }

        stores.assignOwner(uuid);
        GoGInput.send(ctx, "&a[GoG] You are now owner.");
        return GoGInput.done();
    }
}
