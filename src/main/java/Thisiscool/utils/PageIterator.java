package Thisiscool.utils;

import static Thisiscool.PluginVars.*;
import static Thisiscool.utils.Utils.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.menus.MenuHandler;
import Thisiscool.database.Cache;
import arc.func.Cons3;
import arc.func.Prov;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.gen.Groups;
import mindustry.gen.Player;

public class PageIterator {

    // region client

    public static void commands(String[] args, Player player) {
        Log.info("Commands method called for player: " + player.name());
        client(args, player, "help", () -> {
            Log.info("Fetching available commands for player: " + player.name());
            return availableCommands(player);
        }, (builder, index, command) -> {
            Log.info("Appending command details for command: " + command.name);
            builder.append(Bundle.format("commands.help.command", player, command.name,
                    command.params(player), command.description(player)));
        });
        Log.info("Commands method completed for player: " + player.name());
    }

    public static void maps(String[] args, Player player) {
        client(args, player, "maps", Utils::availableMaps, (builder, index, map) -> builder.append(
                Bundle.format("commands.maps.map", player, index, map.name(), map.author(), map.width, map.height)));
    }

    public static void players(String[] args, Player player) {
        client(args, player, "players", () -> Groups.player.copy(new Seq<>()),
                (builder, index, other) -> builder.append(Bundle.format("commands.players.player", player,
                        other.coloredName(), other.admin ? "\uE82C" : "\uE872", Cache.get(other).id, other.locale)));
    }

    private static <T> void client(String[] args, Player player, String name, Prov<Seq<T>> content,
            Cons3<StringBuilder, Integer, T> formatter) {
        int page = args.length > 0 ? Strings.parseInt(args[0]) : 1,
                pages = Math.max(1, Mathf.ceil((float) content.get().size / maxPerPage));
        if (page > pages || page < 1) {
            Bundle.send(player, "commands.invalid-page", pages);
            return;
        }

        MenuHandler.showListMenu(player, page, "commands." + name + ".title", content, formatter);
    }

}