package Thisiscool.utils;

import static Thisiscool.PluginVars.*;
import static Thisiscool.utils.Utils.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.menus.MenuHandler;
import Thisiscool.config.Config;
import Thisiscool.config.Config.Gamemode;
import Thisiscool.database.Cache;
import Thisiscool.discord.MessageContext;
import Thisiscool.listeners.LegenderyCumEvents.ListRequest;
import Thisiscool.listeners.LegenderyCumEvents.ListResponse;
import arc.Events;
import arc.func.Cons2;
import arc.func.Cons3;
import arc.func.Prov;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.spec.EmbedCreateSpec.Builder;
import discord4j.rest.util.Color;
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

    // endregion
    // region discord

    public static void maps(MessageContext context) {
        discord(context, "maps", PageIterator::formatMapsPage);
    }

    public static void players(MessageContext context) {
        discord(context, "players", PageIterator::formatPlayersPage);
    }

    private static void discord(MessageContext context, String type,
            Cons2<Builder, ListResponse> formatter) {
        Gamemode server = Config.getMode();
        Log.info("Discord method called for type: " + type + ", server: " + server.displayName);
        Events.fire(new ListRequest(type, server.displayName, 1, response -> {
            Log.info("Sending response for type: " + type + ", server: " + server.displayName);
            Log.info("Sending reply for type: " + type + ", server: " + server.displayName);
            context.reply(embed -> formatter.get(embed, response))
                    .withComponents(createPageButtons(type, server.displayName, response))
                    .subscribe();
            Log.info("Reply sent for type: " + type + ", server: " + server.displayName);
        }));
    }

    public static <T> void formatListResponse(ListRequest request, Seq<T> values,
            Cons3<StringBuilder, Integer, T> formatter) {
        Log.info("Formatting list response for request: " + request);
        int page = request.page;
        int pages = Math.max(1, Mathf.ceil((float) values.size / maxPerPage));

        if (page < 1 || page > pages) {
            Log.info("Invalid page number for request: " + request);
            return;
        }

        Log.info("Creating ListResponse for request: " + request);
        Events.fire(new ListResponse(formatList(values, page, formatter), page, pages, values.size));
    }

    public static void formatMapsPage(Builder embed, ListResponse response) {
        Log.info("Formatting maps page with response: " + response);
        formatDiscordPage(embed, "Maps in Playlist: @", "Page @ / @", response);
    }

    public static void formatPlayersPage(Builder embed, ListResponse response) {
        Log.info("Formatting players page with response: " + response);
        formatDiscordPage(embed, "Players Online: @", "Page @ / @", response);
    }

    public static void formatDiscordPage(Builder embed, String title, String footer, ListResponse response) {
        Log.info(
                "Formatting Discord page with title: " + title + ", footer: " + footer + ", and response: " + response);
        embed.title(Strings.format(title, response.total));
        embed.footer(Strings.format(footer, response.page, response.pages), null);

        embed.color(Color.SUMMER_SKY);
        embed.description(response.content);
    }

    public static ActionRow createPageButtons(String type, String server, ListResponse response) {
        Log.info("Creating page buttons for type: " + type + ", server: " + server + ", and response: " + response);
        return ActionRow.of(
                Button.primary(type + "-" + server + "-" + (response.page - 1), "<--").disabled(response.page <= 1),
                Button.primary(type + "-" + server + "-" + (response.page + 1), "-->")
                        .disabled(response.page >= response.pages));
    }

    // endregion
}