// File: com/goldorgrave/essentials/GoGTeleport.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.perms.model.HomeLocation;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class GoGTeleport {
    private GoGTeleport() {}

    public static CompletableFuture<HomeLocation> captureHomeLocationFromEntityStore(CommandContext ctx) {
        try {
            UUID uuid = GoGPlayers.getUuid(ctx.sender());
            if (uuid == null) return CompletableFuture.completedFuture(null);

            Universe u = Universe.get();
            if (u == null) return CompletableFuture.completedFuture(null);

            PlayerRef pr = u.getPlayer(uuid);
            if (pr == null) return CompletableFuture.completedFuture(null);

            UUID worldUuid = pr.getWorldUuid();
            if (worldUuid == null) return CompletableFuture.completedFuture(null);

            World world = u.getWorld(worldUuid);
            if (world == null) return CompletableFuture.completedFuture(null);

            Ref<EntityStore> ref = pr.getReference();
            if (ref == null) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            if (store == null) return CompletableFuture.completedFuture(null);

            CompletableFuture<HomeLocation> out = new CompletableFuture<>();

            world.execute(() -> {
                try {
                    TransformComponent tc = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
                    if (tc == null) {
                        out.complete(null);
                        return;
                    }

                    Vector3d pos = tc.getPosition();
                    if (pos == null) {
                        out.complete(null);
                        return;
                    }

                    String wname = world.getName() == null ? "default" : world.getName();
                    out.complete(new HomeLocation(wname, pos.getX(), pos.getY(), pos.getZ()));
                } catch (Throwable t) {
                    System.out.println("[GoG] captureHomeLocationFromEntityStore failed: " + t);
                    t.printStackTrace();
                    out.complete(null);
                }
            });

            return out;
        } catch (Throwable t) {
            System.out.println("[GoG] captureHomeLocationFromEntityStore outer failed: " + t);
            t.printStackTrace();
            return CompletableFuture.completedFuture(null);
        }
    }

    public static CompletableFuture<Boolean> teleportToHomeViaTeleportComponent(CommandContext ctx, HomeLocation loc) {
        try {
            UUID uuid = GoGPlayers.getUuid(ctx.sender());
            if (uuid == null) return CompletableFuture.completedFuture(false);

            Universe u = Universe.get();
            if (u == null) return CompletableFuture.completedFuture(false);

            PlayerRef pr = u.getPlayer(uuid);
            if (pr == null) return CompletableFuture.completedFuture(false);

            Ref<EntityStore> ref = pr.getReference();
            if (ref == null) return CompletableFuture.completedFuture(false);

            Store<EntityStore> store = ref.getStore();
            if (store == null) return CompletableFuture.completedFuture(false);

            UUID currentWorldUuid = pr.getWorldUuid();
            if (currentWorldUuid == null) return CompletableFuture.completedFuture(false);

            World sourceWorld = u.getWorld(currentWorldUuid);
            if (sourceWorld == null) return CompletableFuture.completedFuture(false);

            World targetWorld = (loc.world == null) ? null : u.getWorld(loc.world);
            if (targetWorld == null) targetWorld = sourceWorld;

            Vector3d destPos = new Vector3d(loc.x, loc.y, loc.z);

            CompletableFuture<Boolean> out = new CompletableFuture<>();
            World finalTargetWorld = targetWorld;

            sourceWorld.execute(() -> {
                try {
                    Vector3f rot = new Vector3f(0.0f, 0.0f, 0.0f);

                    HeadRotation hr = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
                    if (hr != null && hr.getRotation() != null) {
                        rot = hr.getRotation();
                    }

                    Teleport tp = new Teleport(finalTargetWorld, destPos, rot).withHeadRotation(rot);
                    store.addComponent(ref, Teleport.getComponentType(), tp);

                    out.complete(true);
                } catch (Throwable t) {
                    System.out.println("[GoG] teleportToHomeViaTeleportComponent failed: " + t);
                    t.printStackTrace();
                    out.complete(false);
                }
            });

            return out;
        } catch (Throwable t) {
            System.out.println("[GoG] teleportToHomeViaTeleportComponent outer failed: " + t);
            t.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }
}
