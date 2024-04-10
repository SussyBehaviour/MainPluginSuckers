package Thisiscool.commands;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.DiscordConfig.*;
import static Thisiscool.utils.Checks.*;
import static mindustry.Vars.*;

import java.time.Duration;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.database.Database;
import Thisiscool.discord.MessageContext;
import Thisiscool.features.net.LegenderyCum;
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
import arc.util.CommandHandler;
import arc.util.Http;
import arc.util.Strings;


public class DiscordCommands {

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

        discordHandler.<MessageContext>register("maps", "<server>", "List of all maps of the server.",
                PageIterator::maps);
        discordHandler.<MessageContext>register("players", "<server>", "List of all players of the server.",
                PageIterator::players);

        discordHandler.<MessageContext>register("status", "<server>", "Display server status.", (args, context) -> {
            var server = args[0];
            if (notFound(context, server))
                return;

            LegenderyCum.request(new StatusRequest(server), context::reply, context::timeout);
        });

        discordHandler.<MessageContext>register("exit", "<server>", "Exit the server application.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;

            var server = args[0];
            if (notFound(context, server))
                return;

            LegenderyCum.request(new ExitRequest(server), context::reply, context::timeout);
        });

        discordHandler.<MessageContext>register("artv", "<server> [map...]", "Force map change.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;

            var server = args[0];
            if (notFound(context, server))
                return;

            LegenderyCum.request(new ArtvRequest(server, args.length > 1 ? args[1] : null, context.member().getDisplayName()),
                    context::reply, context::timeout);
        });

        discordHandler.<MessageContext>register("map", "<server> <map...>", "Map", (args, context) -> {
            var server = args[0];
            if (notFound(context, server))
                return;

            LegenderyCum.request(new MapRequest(server, args[1]), context::reply, context::timeout);
        });

        discordHandler.<MessageContext>register("uploadmap", "<server>", "Upload a map to the server.",
                (args, context) -> {
                    if (noRole(context, discordConfig.mapReviewerRoleIDs) || notMap(context))
                        return;

                    var server = args[0];
                    if (notFound(context, server))
                        return;

                    context.message()
                            .getAttachments()
                            .stream()
                            .filter(attachment -> attachment.getFilename().endsWith(mapExtension))
                            .forEach(attachment -> Http.get(attachment.getUrl(), response -> {
                                var file = tmpDirectory.child(attachment.getFilename());
                                file.writeBytes(response.getResult());

                                LegenderyCum.request(new UploadMapRequest(server, file.absolutePath()), context::reply,
                                        context::timeout);
                            }));
                });

        discordHandler.<MessageContext>register("removemap", "<server> <map...>", "Remove a map from the server.",
                (args, context) -> {
                    if (noRole(context, discordConfig.mapReviewerRoleIDs))
                        return;

                    var server = args[0];
                    if (notFound(context, server))
                        return;

                    LegenderyCum.request(new RemoveMapRequest(server, args[1]), context::reply, context::timeout);
                });

        discordHandler.<MessageContext>register("kick", "<server> <player> <duration> [reason...]", "Kick a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;

                    var server = args[0];
                    if (notFound(context, server))
                        return;

                    LegenderyCum.request(new KickRequest(server, args[1], args[2],
                            args.length > 3 ? args[3] : "Not Specified", context.member().getDisplayName()),
                            context::reply, context::timeout);
                });

        discordHandler.<MessageContext>register("unkick", "<server> <player...>", "unkick a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;

                    var server = args[0];
                    if (notFound(context, server))
                        return;

                    LegenderyCum.request(new unkickRequest(server, args[1]), context::reply, context::timeout);
                });

        discordHandler.<MessageContext>register("ban", "<server> <player> <duration> [reason...]", "Ban a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;

                    var server = args[0];
                    if (notFound(context, server))
                        return;

                    LegenderyCum.request(new BanRequest(server, args[1], args[2], args.length > 3 ? args[3] : "Not Specified",
                            context.member().getDisplayName()), context::reply, context::timeout);
                });

        discordHandler.<MessageContext>register("unban", "<server> <player...>", "Unban a player.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;

            var server = args[0];
            if (notFound(context, server))
                return;

            LegenderyCum.request(new UnbanRequest(server, args[1]), context::reply, context::timeout);
        });

        discordHandler.<MessageContext>register("stats", "<player...>", "Look up a player stats.", (args, context) -> {
            var data = Find.playerData(args[0]);
            if (notFound(context, data))
                return;

            context.info(embed -> embed
                    .title("Player Stats")
                    .addField("Player:", data.plainName(), false)
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
                    .addField("Total playtime:", Bundle.formatDuration(Duration.ofMinutes(data.playTime)), false))
                    .subscribe();
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
