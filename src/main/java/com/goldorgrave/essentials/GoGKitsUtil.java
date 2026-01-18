// File: com/goldorgrave/essentials/GoGKitsUtil.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.perms.model.Kit;
import com.goldorgrave.essentials.perms.model.KitItem;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;

import java.util.*;

public final class GoGKitsUtil {
    private GoGKitsUtil() {}

    public static List<KitItem> snapshotInventoryAsKitItems(Object sender) {
        try {
            LivingEntity entity = GoGPlayers.asLivingEntity(sender);
            if (entity == null) return null;

            Inventory inv = entity.getInventory();
            if (inv == null) return null;

            ItemContainer hotbar = inv.getHotbar();
            ItemContainer storage = inv.getStorage();

            Map<String, Integer> counts = new LinkedHashMap<>();
            if (hotbar != null) hotbar.forEach((slot, stack) -> accumulateKitStack(counts, stack));
            if (storage != null) storage.forEach((slot, stack) -> accumulateKitStack(counts, stack));

            List<KitItem> out = new ArrayList<>();
            for (Map.Entry<String, Integer> e : counts.entrySet()) out.add(new KitItem(e.getKey(), e.getValue()));
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void accumulateKitStack(Map<String, Integer> counts, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        String id = stack.getItemId();
        int qty = stack.getQuantity();
        if (id == null || id.isBlank() || qty <= 0) return;
        counts.put(id, counts.getOrDefault(id, 0) + qty);
    }

    public static boolean giveKitToPlayer(Object targetSender, Kit kit) {
        try {
            LivingEntity entity = GoGPlayers.asLivingEntity(targetSender);
            if (entity == null) return false;

            Inventory inv = entity.getInventory();
            if (inv == null) return false;

            CombinedItemContainer combined = inv.getCombinedHotbarFirst();

            for (KitItem item : kit.items()) {
                if (item == null) continue;
                String id = item.itemId();
                int amount = item.amount();
                if (id == null || id.isBlank() || amount <= 0) continue;

                ItemStack stack = new ItemStack(id, amount);

                ItemStackTransaction tx = (combined != null)
                        ? combined.addItemStack(stack)
                        : inv.getStorage().addItemStack(stack);

                ItemStack remainder = tx.getRemainder();
                if (remainder != null && !remainder.isEmpty() && remainder.getQuantity() > 0) {
                    System.out.println("[GoG] giveKitToPlayer: not enough space for " + id + " x" + amount
                            + " remainder=" + remainder.getQuantity());
                }
            }

            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
