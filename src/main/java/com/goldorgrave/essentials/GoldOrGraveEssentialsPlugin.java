package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.nio.file.Path;

public final class GoldOrGraveEssentialsPlugin extends JavaPlugin {

    private Stores stores;

    public GoldOrGraveEssentialsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        stores = new Stores(Path.of("mods", "GoldOrGraveEssentials"));
        stores.ensureDefaults();
        System.out.println("[GoG] setup complete");
    }

    @Override
    public void start() {
        CommandManager.get().register(new GoGCommand(stores));
        System.out.println("[GoG] started");
    }

    @Override
    protected void shutdown() {
        stores.saveAll();
        System.out.println("[GoG] shutdown");
    }
}
