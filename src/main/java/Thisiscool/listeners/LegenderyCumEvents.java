package Thisiscool.listeners;

import static Thisiscool.config.Config.*;
import static Thisiscool.config.DiscordConfig.*;
import static Thisiscool.utils.Checks.*;
import static Thisiscool.utils.Utils.*;
import static mindustry.Vars.*;
import static mindustry.server.ServerControl.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.database.Cache;
import Thisiscool.database.Ranks.Rank;
import Thisiscool.database.models.Ban;
import Thisiscool.database.models.PlayerData;
import Thisiscool.discord.DiscordIntegration;
import Thisiscool.listeners.Bus.Request;
import Thisiscool.listeners.Bus.Response;
import Thisiscool.utils.Admins;
import Thisiscool.utils.Find;
import Thisiscool.utils.PageIterator;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Strings;
import arc.util.Timer;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import mindustry.gen.Groups;

public class LegenderyCumEvents {

    public static void load() {
        Events.on(ServerMessageEvent.class, event -> {
            var channel = discordConfig.Chat;
            if (channel == null)
                return;

            DiscordIntegration.sendMessage(channel, "`" + event.name + ": " + event.message + "`");
        });
        Events.on(ServerMessageEmbedEvent.class, event -> {
            var channel = discordConfig.Chat;
            if (channel == null)
                return;

            DiscordIntegration.sendMessageEmbed(channel,
                    EmbedCreateSpec.builder().color(event.color).title(event.title).build());
        });
        Events.on(BanEvent.class, DiscordIntegration::sendBan);
        Events.on(VoteKickEvent.class, DiscordIntegration::sendVoteKick);
        Events.on(AdminRequestEvent.class, DiscordIntegration::sendAdminRequest);
        Timer.schedule(DiscordIntegration::updateActivity, 60f, 60f);
        Events.on(DiscordMessageEvent.class, event -> {
            if (!event.server.equals(config.mode.displayName))
                return;

            if (event.role == null || event.color == null) {
                Log.info("[Discord] @: @", event.name, event.message);
                Bundle.send("discord.chat", event.name, event.message);
            } else {
                Log.info("[Discord] @ | @: @", event.role, event.name, event.message);
                Bundle.send("discord.chat.role", event.color, event.role, event.name, event.message);
            }
        });
        Events.on(BanEvent.class, event -> Groups.player.each(
                player -> player.uuid().equals(event.ban.uuid) || player.ip().equals(event.ban.ip),
                player -> {
                    Admins.kickReason(player, event.ban.remaining(), event.ban.reason, "kick.banned-by-admin",
                            event.ban.adminName).kick();
                    Bundle.send("events.admin.ban", event.ban.adminName, player.coloredName(), event.ban.reason);
                }));
        Events.on(AdminRequestConfirmEvent.class, event -> {
            if (event.server.equals(config.mode.displayName))
                DiscordIntegration.confirm(event.uuid);
        });
        Events.on(AdminRequestDenyEvent.class, event -> {
            if (event.server.equals(config.mode.displayName))
                DiscordIntegration.deny(event.uuid);
        });
        Events.on(ListRequest.class, request -> {
            Log.info("[Discord] List request from @ for type @ on server @ accepted", request.type, request.server);

            switch (request.type) {
                case "maps" -> PageIterator.formatListResponse(request, availableMaps(),
                        (builder, index, map) -> builder
                                .append("**").append(index).append(".** ").append(map.plainName())
                                .append("\n").append("Author: ").append(map.plainAuthor())
                                .append("\n").append(map.width).append("x").append(map.height)
                                .append("\n"));

                case "players" -> PageIterator.formatListResponse(request, Groups.player.copy(new Seq<>()),
                        (builder, index, player) -> builder
                                .append("**").append(index).append(".** ").append(player.plainName())
                                .append("\nID: ").append(Cache.get(player).id)
                                .append("\nLanguage: ").append(player.locale)
                                .append("\n"));

                default -> {
                    Log.warn("[Discord] List request from @ for unknown type @ on server @ rejected", request.type,
                            request.server);
                    throw new IllegalStateException();
                }
            }
        });
        Events.on(ArtvRequest.class, request -> {
            if (noRtv(request))
                return;
            var map = request.map == null ? maps.getNextMap(instance.lastMode, state.map) : Find.map(request.map);
            if (notFound(request, map))
                return;
            Bundle.send("commands.artv.info", request.admin);
            instance.play(false, () -> world.loadMap(map));
        });
    }

    public record DiscordMessageEvent(String server, String role, String color, String name, String message) {
        public DiscordMessageEvent(String server, String name, String message) {
            this(server, null, null, name, message);
        }
    }

    public record ServerMessageEvent(String server, String name, String message) {
    }

    public record ServerMessageEmbedEvent(String server, String title, Color color) {
    }

    public record BanEvent(String server, Ban ban) {
    }

    public record VoteKickEvent(String server, String target,
            String initiator, String reason,
            String votesFor, String votesAgainst) {
    }

    public record AdminRequestEvent(String server, PlayerData data) {
    }

    public record AdminRequestConfirmEvent(String server, String uuid) {
    }

    public record AdminRequestDenyEvent(String server, String uuid) {
    }

    public record SetRankSyncEvent(String uuid, Rank rank) {
    }

    @FunctionalInterface
    public interface ListResponseHandler {
        void handle(ListResponse response);
    }

    @AllArgsConstructor
    public static class ListRequest extends Request<ListResponse> {
        public final String type, server;
        public final int page;
        private final ListResponseHandler responseHandler;

        public void executeResponseHandler(ListResponse response) {
            if (responseHandler != null) {
                responseHandler.handle(response);
            }
        }
    }

    @AllArgsConstructor
    public static class ListResponse extends Response {
        public final String content;
        public final int page, pages, total;
    }

    @AllArgsConstructor
    public static class StatusRequest extends Request<EmbedResponse> {
        public final String server;
    }

    @AllArgsConstructor
    public static class ExitRequest extends Request<EmbedResponse> {
        public final String server;
    }

    @AllArgsConstructor
    public static class ArtvRequest extends Request<EmbedResponse> {
        public final String server, map, admin;
    }

    @AllArgsConstructor
    public static class MapRequest extends Request<EmbedResponse> {
        public final String server, map;
    }

    @AllArgsConstructor
    public static class UploadMapRequest extends Request<EmbedResponse> {
        public final String server, file;
    }

    @AllArgsConstructor
    public static class RemoveMapRequest extends Request<EmbedResponse> {
        public final String server, map;
    }

    @AllArgsConstructor
    public static class KickRequest extends Request<EmbedResponse> {
        public final String server, player, duration, reason, admin;
    }

    @AllArgsConstructor
    public static class unkickRequest extends Request<EmbedResponse> {
        public final String server, player;
    }

    @AllArgsConstructor
    public static class BanRequest extends Request<EmbedResponse> {
        public final String server, player, duration, reason, admin;
    }

    @AllArgsConstructor
    public static class UnbanRequest extends Request<EmbedResponse> {
        public final String server, player;
    }

    @RequiredArgsConstructor
    public static class EmbedResponse extends Response {
        public final Color color;
        public final String title;
        public final Seq<Field> fields = new Seq<>(0);
        public final Seq<String> files = new Seq<>(0);
        public @Nullable String content;
        public @Nullable String footer;
        // Add a field for the image URL
        public @Nullable String imageUrl;

        public static EmbedResponse success(String title) {
            return new EmbedResponse(Color.MEDIUM_SEA_GREEN, title);
        }

        public static EmbedResponse error(String title) {
            return new EmbedResponse(Color.CINNABAR, title);
        }

        public EmbedResponse withField(String name, String value) {
            this.fields.add(new Field(name, value));
            return this;
        }

        public EmbedResponse withFile(String file) {
            this.files.add(file);
            return this;
        }

        public EmbedResponse withContent(String content, Object... args) {
            this.content = Strings.format(content, args);
            return this;
        }

        public EmbedResponse withFooter(String footer, Object... args) {
            this.footer = Strings.format(footer, args);
            return this;
        }

        // Add the withImage method
        public EmbedResponse withImage(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public record Field(String name, String value) {
        }
    }

}
