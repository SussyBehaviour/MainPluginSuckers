package Thisiscool.commands;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.Config.*;
import static Thisiscool.utils.Checks.*;
import static Thisiscool.utils.Utils.*;
import static arc.Core.*;
import static mindustry.Vars.*;

import java.time.Duration;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.net.LegenderyCum;
import Thisiscool.database.Cache;
import Thisiscool.database.Database;
import Thisiscool.database.Ranks;
import Thisiscool.listeners.LegenderyCumEvents.ServerMessageEvent;
import Thisiscool.utils.Admins;
import Thisiscool.utils.Find;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import mindustry.core.GameState.State;
import mindustry.net.Packets.KickReason;


public class ServerCommands {

    public static void load(CommandHandler handler) {
        serverHandler = handler;

        serverHandler.register("exit", "Exit the server application.", args -> {
            netServer.kickAll(KickReason.serverRestarting);
            app.post(() -> System.exit(0));

            Log.info("Server exited.");
        });

        serverHandler.register("stop", "Stop hosting the server.", args -> {
            net.closeServer();
            state.set(State.menu);

            Log.info("Server stopped.");
        });
        serverHandler.register("say", "<message...>", "Send a message to all players.", args -> {
            Log.info("&fi@: &fr&lw@", "&lcServer", "&lw" + args[0]);
            Bundle.send("commands.say.chat", args[0]);

            LegenderyCum.send(new ServerMessageEvent(config.mode.displayName, "Server", stripDiscord(args[0])));
        });

        serverHandler.register("kick", "<player> <duration> [reason...]", "Kick a player.", args -> {
            var target = Find.player(args[0]);
            if (notFound(target, args[0]))
                return;

            var duration = parseDuration(args[1]);
            if (invalidDuration(duration))
                return;

            var reason = args.length > 2 ? args[2] : "Not Specified";
            Admins.kick(target, "<Console>", duration.toMillis(), reason);

            Log.info("Player @ has been kicked for @ for @.", target.plainName(), Bundle.formatDuration(duration),
                    reason);
        });

        serverHandler.register("unkick", "<player...>", "unkick a player.", args -> {
            var info = Find.playerInfo(args[0]);
            if (notFound(info, args[0]) || notKicked(info))
                return;

            info.lastKicked = 0L;
            netServer.admins.kickedIPs.remove(info.lastIP);
            netServer.admins.dosBlacklist.remove(info.lastIP);

            Log.info("Player @ has been unkicked.", info.plainLastName());
        });

        serverHandler.register("kicks", "List of all kicked players", args -> {
            var kicked = netServer.admins.kickedIPs;
            kicked.each((ip, time) -> {
                if (time < Time.millis())
                    kicked.remove(ip);
            });

            Log.info("Kicked players: (@)", kicked.size);
            kicked.each((ip, time) -> {
                var info = netServer.admins.findByIP(ip);
                Log.info("  Name: @ / UUID: @ / IP: @ / Unban Date: @", info == null ? "unknown" : info.plainLastName(),
                        info == null ? "unknown" : info.id, ip, Bundle.formatDateTime(time));
            });
        });

        serverHandler.register("ban", "<player> <duration> [reason...]", "Ban a player.", args -> {
            var info = Find.playerInfo(args[0]);
            if (notFound(info, args[0]))
                return;

            var duration = parseDuration(args[1]);
            if (invalidDuration(duration))
                return;

            var reason = args.length > 2 ? args[2] : "Not Specified";
            Admins.ban(info, "<Console>", duration.toMillis(), reason);

            Log.info("Player @ has been banned for @ for @.", info.plainLastName(), Bundle.formatDuration(duration),
                    reason);
        });

        serverHandler.register("unban", "<player...>", "Unban a player.", args -> {
            var info = Find.playerInfo(args[0]);
            if (notFound(info, args[0]))
                return;

            var ban = Database.removeBan(info.id, info.lastIP);
            if (notBanned(ban))
                return;

            Log.info("Player @ has been unbanned.", ban.playerName);
        });

        serverHandler.register("bans", "List of all banned players.", args -> {
            var banned = Database.getBans();

            Log.info("Banned players: (@)", banned.size());
            banned.forEach(ban -> Log.info("  Name: @ / UUID: @ / IP: @ / Unban Date: @", ban.playerName, ban.uuid,
                    ban.ip, Bundle.formatDateTime(1, 2, ban.unbanDate)));
        });

        serverHandler.register("admin", "<add/fire> <player...>", "Make a player admin.", args -> {
            var info = Find.playerInfo(args[1]);
            if (notFound(info, args[1]))
                return;

            switch (args[0].toLowerCase()) {
                case "add" -> {
                    var target = Find.playerByUUID(info.id);
                    if (target != null) {
                        target.admin(true);
                        Bundle.send(target, "events.server.admin");
                    }

                    netServer.admins.adminPlayer(info.id, target == null ? info.adminUsid : target.usid());
                    Log.info("Player @ is now admin.", info.plainLastName());
                }
                case "fire" -> {
                    var target = Find.playerByUUID(info.id);
                    if (target != null) {
                        target.admin(false);
                        Bundle.send(target, "events.server.unadmin");
                    }

                    netServer.admins.unAdminPlayer(info.id);
                    Log.info("Player @ is no longer an admin.", info.plainLastName());
                }
                default -> Log.err("The first parameter must be either add or fire.");
            }
        });

        serverHandler.register("admins", "List all admins.", args -> {
            var admins = netServer.admins.getAdmins();

            Log.info("Admins: (@)", admins.size);
            admins.each(admin -> Log.info("  Name: @ / ID: @ / IP: @", admin.plainLastName(), admin.id, admin.lastIP));
        });

        serverHandler.register("stats", "<player...>", "Look up a player stats.", args -> {
            var data = Find.playerData(args[0]);
            if (notFound(data, args[0]))
                return;

            Log.info("Player Stats");
            Log.info("  Name: @", data.plainName());
            Log.info("  UUID: @", data.uuid);
            Log.info("  ID: @", data.id);
            Log.info("  Rank: @", data.rank.name());
            Log.info("  Blocks placed: @", data.blocksPlaced);
            Log.info("  Blocks broken: @", data.blocksBroken);
            Log.info("  Games played: @", data.gamesPlayed);
            Log.info("  Waves survived: @", data.wavesSurvived);
            Log.info("  Wins:");
            Log.info("  - Attack: @", data.attackWins);
            Log.info("  - Towerdefense: @", data.TowerdefenseWins);
            Log.info("  - Football: @", data.FootballWins);
            Log.info("  - HungerGames: @", data.HungerGamesWins);
            Log.info("  - PvP: @", data.pvpWins);
            Log.info("  Total playtime: @", Bundle.formatDuration(Duration.ofMinutes(data.playTime)));
        });

        serverHandler.register("setrank", "<player> <rank>", "Set a player's rank.", args -> {
            var data = Find.playerData(args[0]);
            if (notFound(data, args[0]))
                return;

            var rank = Find.rank(args[1]);
            if (notFound(rank, args[1]))
                return;

            data.rank = rank;

            var target = Find.playerByUUID(data.uuid);
            if (target != null) {
                Cache.put(target, data);
                Ranks.name(target, data);
            }

            Database.savePlayerData(data);
            Log.info("Successfully set rank of @ to @.", data.plainName(), rank.name());
        });
    }
}
