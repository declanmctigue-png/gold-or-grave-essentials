// File: com/goldorgrave/essentials/GoGCommand.java
package com.goldorgrave.essentials;

import com.goldorgrave.essentials.storage.Stores;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class GoGCommand extends AbstractCommand {

    private final Stores stores;

    // Feature modules
    private final GoGOwnerFeature owner;
    private final GoGJoinMsgFeature joinMsg;
    private final GoGRankFeature ranks;
    private final GoGHomesFeature homes;
    private final GoGKitsFeature kits;
    private final GoGPermDebugFeature permDebug;
    private final GoGJoinBroadcastCmdFeature joinBroadcastCmd;


    public GoGCommand(Stores stores) {
        super("gog", "Gold or Grave Essentials");
        this.stores = stores;

        this.owner = new GoGOwnerFeature(stores);
        this.joinMsg = new GoGJoinMsgFeature(stores);
        this.ranks = new GoGRankFeature(stores);
        this.homes = new GoGHomesFeature(stores);
        this.kits = new GoGKitsFeature(stores);
        this.permDebug = new GoGPermDebugFeature(stores);
        this.joinBroadcastCmd = new GoGJoinBroadcastCmdFeature(stores);


        setAllowsExtraArguments(true);
    }

    @Override
    protected @Nullable CompletableFuture<Void> execute(@NotNull CommandContext ctx) {
        String[] args = GoGInput.getArgs(ctx);

        if (args.length == 0 || GoGInput.eq(args[0], "help")) {
            GoGHelp.sendHelp(ctx);
            return GoGInput.done();
        }

        if (GoGInput.eq(args[0], "whoami")) {
            return GoGWhoAmIFeature.handleWhoAmI(ctx);
        }

        if (GoGInput.eq(args[0], "joinbroadcast")) {
            return joinBroadcastCmd.handleJoinBroadcast(ctx, args);
        }


        // Debug / bootstrap
        if (GoGInput.eq(args[0], "permdebug")) return permDebug.handlePermDebug(ctx);
        if (GoGInput.eq(args[0], "ownerme")) return owner.handleOwnerMe(ctx);

        // Join message
        if (GoGInput.eq(args[0], "joinmsg")) return joinMsg.handleJoinMsg(ctx, args);

        // Ranks
        if (GoGInput.eq(args[0], "ranks")) return ranks.handleRanksList(ctx);
        if (GoGInput.eq(args[0], "rank")) return ranks.handleRank(ctx, args);

        // Homes
        if (GoGInput.eq(args[0], "homes")) return homes.handleHomesList(ctx);
        if (GoGInput.eq(args[0], "sethome")) return homes.handleSetHome(ctx, args);
        if (GoGInput.eq(args[0], "home")) return homes.handleHome(ctx, args);
        if (GoGInput.eq(args[0], "delhome")) return homes.handleDelHome(ctx, args);

        // Kits
        if (GoGInput.eq(args[0], "kits")) return kits.handleKitsList(ctx);
        if (GoGInput.eq(args[0], "kit")) return kits.handleKit(ctx, args);

        GoGInput.send(ctx, "&c[GoG] Unknown subcommand. Try /gog help");
        return GoGInput.done();
    }
}
