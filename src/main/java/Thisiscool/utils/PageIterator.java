package Thisiscool.utils;

import static Thisiscool.PluginVars.*;
import static Thisiscool.utils.Utils.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.menus.MenuHandler;
import Thisiscool.StuffForUs.net.LegenderyCum;
import Thisiscool.config.Config;
import Thisiscool.config.Config.Gamemode;
import Thisiscool.database.Cache;
import Thisiscool.discord.MessageContext;
import Thisiscool.listeners.LegenderyCumEvents.ListRequest;
import Thisiscool.listeners.LegenderyCumEvents.ListResponse;
import arc.func.Cons2;
import arc.func.Cons3;
import arc.func.Prov;
import arc.math.Mathf;
import arc.struct.Seq;
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
        client(args, player, "help", () -> availableCommands(player),
                (builder, index, command) -> builder.append(Bundle.format("commands.help.command", player, command.name,
                        command.params(player), command.description(player))));
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

    public static void maps(String[] args, MessageContext context) {
        discord(args, context, "maps", PageIterator::formatMapsPage);
    }

    public static void players(String[] args, MessageContext context) {
        discord(args, context, "players", PageIterator::formatPlayersPage);
    }

    private static void discord(String[] args, MessageContext context, String type,
            Cons2<Builder, ListResponse> formatter) {
            Gamemode server = Config.getMode();


        LegenderyCum.request(new ListRequest(type, server.displayName, 1), response -> context
                .reply(embed -> formatter.get(embed, response))
                .withComponents(createPageButtons(type, server.displayName, response))
                .subscribe(), context::timeout);
    }

    public static <T> void formatListResponse(ListRequest request, Seq<T> values,
            Cons3<StringBuilder, Integer, T> formatter) {
        int page = request.page;
        int pages = Math.max(1, Mathf.ceil((float) values.size / maxPerPage));

        if (page < 1 || page > pages)
            return;

        LegenderyCum.respond(request, new ListResponse(formatList(values, page, formatter), page, pages, values.size));
    }

    public static void formatMapsPage(Builder embed, ListResponse response) {
        formatDiscordPage(embed, "Maps in Playlist: @", "Page @ / @", response);
    }

    public static void formatPlayersPage(Builder embed, ListResponse response) {
        formatDiscordPage(embed, "Players Online: @", "Page @ / @", response);
    }

    public static void formatDiscordPage(Builder embed, String title, String footer, ListResponse response) {
        embed.title(Strings.format(title, response.total));
        embed.footer(Strings.format(footer, response.page, response.pages), null);

        embed.color(Color.SUMMER_SKY);
        embed.description(response.content);
    }

    public static ActionRow createPageButtons(String type, String server, ListResponse response) {
        return ActionRow.of(
                Button.primary(type + "-" + server + "-" + (response.page - 1), "<--").disabled(response.page <= 1),
                Button.primary(type + "-" + server + "-" + (response.page + 1), "-->")
                        .disabled(response.page >= response.pages));
    }

    // endregion
}