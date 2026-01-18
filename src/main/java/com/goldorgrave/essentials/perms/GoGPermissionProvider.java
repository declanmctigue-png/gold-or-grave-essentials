package com.goldorgrave.essentials.perms;

import com.hypixel.hytale.server.core.permissions.provider.PermissionProvider;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter between Hytale's PermissionProvider interface and our internal PermissionManager.
 *
 * Hytale calls into this provider for:
 * - adding/removing raw permission strings
 * - adding/removing group membership
 * - fetching raw sets (mostly for commands / debugging)
 *
 * Our plugin still does the real "has permission?" checks in PermissionManager.hasPermission(uuid, nodeKey).
 */
public final class GoGPermissionProvider implements PermissionProvider {

    private final PermissionManager perms;

    public GoGPermissionProvider(PermissionManager perms) {
        this.perms = perms;
    }

    @Override
    @Nonnull
    public String getName() {
        return "GoldOrGraveEssentials";
    }

    // -----------------------------
    // User permissions
    // -----------------------------

    @Override
    public void addUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        if (permissions.isEmpty()) return;
        perms.userAddPermissions(uuid, permissions);
    }

    @Override
    public void removeUserPermissions(@Nonnull UUID uuid, @Nonnull Set<String> permissions) {
        if (permissions.isEmpty()) return;
        perms.userRemovePermissions(uuid, permissions);
    }

    @Override
    @Nonnull
    public Set<String> getUserPermissions(@Nonnull UUID uuid) {
        Set<String> out = perms.userListPermissions(uuid);
        return out == null ? Collections.emptySet() : Collections.unmodifiableSet(out);
    }

    // -----------------------------
    // Group permissions
    // -----------------------------

    @Override
    public void addGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions) {
        if (permissions.isEmpty()) return;
        perms.groupAddPermissions(group, permissions);
    }

    @Override
    public void removeGroupPermissions(@Nonnull String group, @Nonnull Set<String> permissions) {
        if (permissions.isEmpty()) return;
        perms.groupRemovePermissions(group, permissions);
    }

    @Override
    @Nonnull
    public Set<String> getGroupPermissions(@Nonnull String group) {
        Set<String> out = perms.groupListPermissions(group);
        return out == null ? Collections.emptySet() : Collections.unmodifiableSet(out);
    }

    // -----------------------------
    // Group membership
    // -----------------------------

    @Override
    public void addUserToGroup(@Nonnull UUID uuid, @Nonnull String group) {
        perms.userAddToGroup(uuid, group);
    }

    @Override
    public void removeUserFromGroup(@Nonnull UUID uuid, @Nonnull String group) {
        perms.userRemoveFromGroup(uuid, group);
    }

    @Override
    @Nonnull
    public Set<String> getGroupsForUser(@Nonnull UUID uuid) {
        Set<String> groups = perms.userListGroups(uuid);
        if (groups == null || groups.isEmpty()) {
            return Collections.singleton("default");
        }
        return Collections.unmodifiableSet(new HashSet<>(groups));
    }

}
