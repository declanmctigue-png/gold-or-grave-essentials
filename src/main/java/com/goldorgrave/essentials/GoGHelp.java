// File: com/goldorgrave/essentials/GoGHelp.java
package com.goldorgrave.essentials;

import com.hypixel.hytale.server.core.command.system.CommandContext;

public final class GoGHelp {
    private GoGHelp() {}

    public static void sendHelp(CommandContext ctx) {
        GoGInput.send(ctx, "&6-----Base Commands-----");
        GoGInput.send(ctx, "&7[GoG] &f/gog whoami");
        GoGInput.send(ctx, "&7[GoG] &f/gog permdebug");
        GoGInput.send(ctx, "&7[GoG] &f/gog ownerme");
        GoGInput.send(ctx, "&6-----HOMES-----");
        GoGInput.send(ctx, "&7[GoG] &f/gog homes");
        GoGInput.send(ctx, "&7[GoG] &f/gog sethome <name>");
        GoGInput.send(ctx, "&7[GoG] &f/gog home <name>");
        GoGInput.send(ctx, "&7[GoG] &f/gog delhome <name>");
        GoGInput.send(ctx, "&6-----KITS-----");
        GoGInput.send(ctx, "&7[GoG] &f/gog kits");
        GoGInput.send(ctx, "&7[GoG] &f/gog kit <name>");
        GoGInput.send(ctx, "&7[GoG] &f/gog kit create <name> [cooldownSeconds] [permissionNode]");
        GoGInput.send(ctx, "&7[GoG] &f/gog kit delete <name>");
        GoGInput.send(ctx, "&7[GoG] &f/gog kit give <name> [playerName]");
        GoGInput.send(ctx, "&6-----RANKS-----");
        GoGInput.send(ctx, "&7[GoG] &f/gog ranks");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank create <name> [weight]");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank delete <name>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank perm add <rank> <node>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank perm remove <rank> <node>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank set <playerName> <rank>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank get [playerName]");
        GoGInput.send(ctx, "&6-----PREFIX-----");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank prefix set <rank> <prefix...>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank prefix get <rank>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank prefix clear <rank>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank prefixcolor set <rank> <code>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank prefixcolor get <rank>");
        GoGInput.send(ctx, "&7[GoG] &f/gog rank prefixcolor clear <rank>");
        GoGInput.send(ctx, "&6-----JOIN MSG-----");
        GoGInput.send(ctx, "&7[GoG] Tokens: &f%player% %user% %uuid%");
        GoGInput.send(ctx, "&7[GoG] &f/gog joinmsg set");
        GoGInput.send(ctx, "&7[GoG] &f/gog joinmsg get");
        GoGInput.send(ctx, "&7[GoG] &f/gog joinmsg clear");
        GoGInput.send(ctx, "&6-----JOIN BROADCAST-----");
        GoGInput.send(ctx, "&7[GoG] &f/gog joinbroadcast set <message...>");
        GoGInput.send(ctx, "&7[GoG] &f/gog joinbroadcast get");
        GoGInput.send(ctx, "&7[GoG] &f/gog joinbroadcast clear");
        GoGInput.send(ctx, "&7[GoG] &fTokens: &f%player% %user% %uuid%");
        GoGInput.send(ctx, "&7[GoG] &fMultiline: &fuse || or |n|");

        GoGInput.send(ctx, "&6Ask online staff if you have any questions.");
    }
}
