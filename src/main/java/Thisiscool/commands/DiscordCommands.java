package Thisiscool.commands;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.DiscordConfig.*;
import static Thisiscool.utils.Checks.*;
import static mindustry.Vars.*;

import java.time.Duration;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.net.LegenderyCum;
import Thisiscool.config.Config;
import Thisiscool.config.Config.Gamemode;
import Thisiscool.database.Database;
import Thisiscool.database.models.Ban;
import Thisiscool.database.models.PlayerData;
import Thisiscool.discord.DiscordBot;
import Thisiscool.discord.MessageContext;
import Thisiscool.listeners.LegenderyCumEvents.ArtvRequest;
import Thisiscool.listeners.LegenderyCumEvents.BanRequest;
import Thisiscool.listeners.LegenderyCumEvents.ExitRequest;
import Thisiscool.listeners.LegenderyCumEvents.KickRequest;
import Thisiscool.listeners.LegenderyCumEvents.MapRequest;
import Thisiscool.listeners.LegenderyCumEvents.RemoveMapRequest;
import Thisiscool.listeners.LegenderyCumEvents.SetRankSyncEvent;
import Thisiscool.listeners.LegenderyCumEvents.StatusRequest;
import Thisiscool.listeners.LegenderyCumEvents.UnbanRequest;
import Thisiscool.listeners.LegenderyCumEvents.UploadMapRequest;
import Thisiscool.listeners.LegenderyCumEvents.unkickRequest;
import Thisiscool.utils.Find;
import Thisiscool.utils.PageIterator;
import Thisiscool.utils.Utils;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.util.CommandHandler;
import arc.util.Http;
import arc.util.Strings;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.server.ServerControl;

public class DiscordCommands {
    public static final IntMap<User> playerLinkCodes = new IntMap<>();

    public static void load() {
        discordHandler = new CommandHandler(discordConfig.prefix);
        discordHandler.<MessageContext>register("help", "List of all commands.", (args, context) -> {
            var builder = new StringBuilder();
            discordHandler.getCommandList()
                    .each(command -> builder.append(discordHandler.prefix).append("**").append(command.text)
                            .append("**").append(command.paramText.isEmpty() ? "" : " " + command.paramText)
                            .append(" - ").append(command.description).append("\n"));

            context.info("All available commands:", builder.toString()).subscribe();
        });
        discordHandler.<MessageContext>register("maps", "List of all maps of the server.",
                PageIterator::maps);
        discordHandler.<MessageContext>register("players", "List of all players of the server.",
                PageIterator::players);

        discordHandler.<MessageContext>register("status", "Display server status.", (args, context) -> {
            Gamemode server = Config.getMode();
            LegenderyCum.request(new StatusRequest(server.displayName), context::reply, context::timeout);
        });
        discordHandler.<MessageContext>register("exit", "Exit the server application.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            Gamemode server = Config.getMode();
            LegenderyCum.request(new ExitRequest(server.displayName), context::reply, context::timeout);
        });
        discordHandler.<MessageContext>register("artv", "[map...]", "Force map change.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            Gamemode server = Config.getMode();
            LegenderyCum.request(
                    new ArtvRequest(server.displayName, args.length > 0 ? args[0] : null,
                            context.member().getDisplayName()),
                    context::reply, context::timeout);
        });
        discordHandler.<MessageContext>register("js", "<code...>", "Run arbitrary JavaScript", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            String message = Utils.runConsole(args[0]);

            context.info(embed -> embed
                    .title("JavaScript run")
                    .addField("Code run", args[0], false)
                    .addField("Output: ", message, false)).subscribe();
        });
        discordHandler.<MessageContext>register("console", "<args...>", "Run console command", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            var response = ServerControl.instance.handler.handleMessage(args[0]);
            switch (response.type) {
                case unknownCommand -> context.message().addReaction(ReactionEmoji.unicode("❓")).subscribe();
                case valid -> context.message().addReaction(ReactionEmoji.unicode("✅")).subscribe();
                case manyArguments -> context.message().getChannel()
                        .flatMap(channel -> channel.createMessage("Too many arguments")).subscribe();
                case fewArguments -> context.message().getChannel()
                        .flatMap(channel -> channel.createMessage("Too less arguments")).subscribe();
                case noCommand -> context.message().addReaction(ReactionEmoji.unicode("⚠")).subscribe();
            }
        });
        discordHandler.<MessageContext>register("map", "<map...>", "Map", (args, context) -> {
            Gamemode server = Config.getMode();
            LegenderyCum.request(new MapRequest(server.displayName, args[0]), context::reply, context::timeout);
        });
        discordHandler.<MessageContext>register("uploadmap", "Upload a map to the server.",
                (args, context) -> {
                    if (noRole(context, discordConfig.mapReviewerRoleIDs) || notMap(context))
                        return;
                    Gamemode server = Config.getMode();
                    context.message()
                            .getAttachments()
                            .stream()
                            .filter(attachment -> attachment.getFilename().endsWith(mapExtension))
                            .forEach(attachment -> Http.get(attachment.getUrl(), response -> {
                                var file = tmpDirectory.child(attachment.getFilename());
                                file.writeBytes(response.getResult());

                                LegenderyCum.request(new UploadMapRequest(server.displayName, file.absolutePath()),
                                        context::reply,
                                        context::timeout);
                            }));
                });

        discordHandler.<MessageContext>register("removemap", "<map...>", "Remove a map from the server.",
                (args, context) -> {
                    if (noRole(context, discordConfig.mapReviewerRoleIDs))
                        return;
                    Gamemode server = Config.getMode();
                    LegenderyCum.request(new RemoveMapRequest(server.displayName, args[0]), context::reply,
                            context::timeout);
                });

        discordHandler.<MessageContext>register("kick", "<player> <duration> [reason...]", "Kick a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;
                    Gamemode server = Config.getMode();
                    LegenderyCum.request(new KickRequest(server.displayName, args[0], args[1],
                            args.length > 2 ? args[2] : "Not Specified", context.member().getDisplayName()),
                            context::reply, context::timeout);
                });

        discordHandler.<MessageContext>register("unkick", "<player...>", "unkick a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;
                    Gamemode server = Config.getMode();
                    LegenderyCum.request(new unkickRequest(server.displayName, args[0]), context::reply,
                            context::timeout);
                });

        discordHandler.<MessageContext>register("ban", "<player> <duration> [reason...]", "Ban a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;
                    Gamemode server = Config.getMode();
                    LegenderyCum.request(
                            new BanRequest(server.displayName, args[0], args[1],
                                    args.length > 1 ? args[1] : "Not Specified",
                                    context.member().getDisplayName()),
                            context::reply, context::timeout);
                });

        discordHandler.<MessageContext>register("unban", "<player...>", "Unban a player.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            Gamemode server = Config.getMode();
            LegenderyCum.request(new UnbanRequest(server.displayName, args[0]), context::reply, context::timeout);
        });
        discordHandler.<MessageContext>register("info", "<player...>", "Look up a player stats.", (args, context) -> {
            var data = Find.playerData(args[0]);
            if (notFound(context, data))
                return;

            // Start building the embed message
            context.info(embed -> {
                embed.title("Player Stats")
                        .addField("Player:", data.plainName(), false)
                        .addField("DiscordName",DiscordBot.getUserNameById(data.DiscordId).block(), false)
                        .addField("ID:", String.valueOf(data.id), false)
                        .addField("Rank:", data.rank.name(), false)
                        .addField("Blocks placed:", String.valueOf(data.blocksPlaced), false)
                        .addField("Blocks broken:", String.valueOf(data.blocksBroken), false)
                        .addField("Games played:", String.valueOf(data.gamesPlayed), false)
                        .addField("Waves survived:", String.valueOf(data.wavesSurvived), false)
                        .addField("Wins:",
                                Strings.format("""
                                        - Attack: @
                                        - Towerdefense: @
                                        - Football: @
                                        - HungerGames: @
                                        - PvP: @
                                        """, data.attackWins, data.TowerdefenseWins, data.FootballWins,
                                        data.HungerGamesWins, data.pvpWins),
                                false)
                        .addField("Total playtime:", Bundle.formatDuration(Duration.ofMinutes(data.playTime)), false);

                // Check if the user has the admin role and add additional fields
                if (!noRole(context, discordConfig.adminRoleIDs)) {
                    embed.addField("Admin-only field:", "This is visible only to admins.", false);
                    embed.addField("UUIDs", data.uuid, false);
                    Ban ban = Database.getBanByUUID(data.uuid);
                    if (ban != null) {
                        embed.addField("Unban Date:", ban.getUnbanDate().toString(), false);
                        embed.addField("IP:", ban.getIp(), false);
                    } else {
                        embed.addField("Unban Date:", "N/A", false);
                        embed.addField("IP:", "N/A", false);
                    }
                }
            }).subscribe();
        });
        discordHandler.<MessageContext>register("link", "<playerID...>", "Link to a player.",
                (args, context) -> {
                    PlayerData data = Find.playerData(args[0]);
                    int code = Mathf.random(100000, 999999);
                    playerLinkCodes.put(code, context.member());
                    Groups.player.forEach(p -> {
                        if (p.uuid().equals(data.uuid)) {
                            Call.sendMessage("[accent]" + context.member().getDisplayName(), " wants to link with you. If you wish to link your account, type [accent] /link " + code
                            + " to link.", p);
                        }
                    });
                });
        discordHandler.<MessageContext>register("setrank", "<player> <rank>", "Set a player's rank.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;

                    var data = Find.playerData(args[0]);
                    if (notFound(context, data))
                        return;

                    var rank = Find.rank(args[1]);
                    if (notFound(context, rank))
                        return;

                    data.rank = rank;
                    Database.savePlayerData(data);

                    LegenderyCum.send(new SetRankSyncEvent(data.uuid, rank));
                    context.success(embed -> embed
                            .title("Rank Changed")
                            .addField("Player:", data.plainName(), false)
                            .addField("Rank:", rank.name(), false)).subscribe();
                });
    }
}
