// Kit.java
package com.goldorgrave.essentials.perms.model;

import java.util.List;

public record Kit(
        String name,
        String permission,
        long cooldownSeconds,
        List<KitItem> items
) {}
