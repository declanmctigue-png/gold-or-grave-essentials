package com.goldorgrave.essentials.perms.model;

public final class HomeLocation {

    public final String world;
    public final double x;
    public final double y;
    public final double z;

    public HomeLocation(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }
}