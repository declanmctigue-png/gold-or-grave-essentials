// Kit.java
package com.goldorgrave.essentials.model;

import java.util.List;

public record Kit(
        String name,
        String permission,
        long cooldownSeconds,
        List<KitItem> items
) {}
