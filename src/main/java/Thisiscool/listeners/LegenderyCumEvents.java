package Thisiscool.listeners;

import static Thisiscool.config.Config.*;
import static Thisiscool.config.DiscordConfig.*;
import static Thisiscool.utils.Checks.*;
import static Thisiscool.utils.Utils.*;
import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.server.ServerControl.*;

import java.util.Base64;

import Thisiscool.Cancer.EventBus.Request;
import Thisiscool.Cancer.EventBus.Response;
import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.net.LegenderyCum;
import Thisiscool.database.Cache;
import Thisiscool.database.Database;
import Thisiscool.database.Ranks;
import Thisiscool.database.Ranks.Rank;
import Thisiscool.database.models.Ban;
import Thisiscool.database.models.PlayerData;
import Thisiscool.discord.DiscordIntegration;
import Thisiscool.utils.Admins;
import Thisiscool.utils.Find;
import Thisiscool.utils.MapGenerator;
import Thisiscool.utils.PageIterator;
import arc.files.Fi;
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
import mindustry.io.MapIO;
import mindustry.net.Packets.KickReason;


public class LegenderyCumEvents {

    public static void load() {
            LegenderyCum.on(ServerMessageEvent.class, event -> {
                var channel = discordConfig.serverToChannel.get(event.server);
                if (channel == null)
                    return;

                DiscordIntegration.sendMessage(channel, "`" + event.name + ": " + event.message + "`");
            });
            LegenderyCum.on(ServerMessageEmbedEvent.class, event -> {
                var channel = discordConfig.serverToChannel.get(event.server);
                if (channel == null)
                    return;

                DiscordIntegration.sendMessageEmbed(channel,
                        EmbedCreateSpec.builder().color(event.color).title(event.title).build());
            });
            LegenderyCum.on(BanEvent.class, DiscordIntegration::sendBan);
            LegenderyCum.on(VoteKickEvent.class, DiscordIntegration::sendVoteKick);
            LegenderyCum.on(AdminRequestEvent.class, DiscordIntegration::sendAdminRequest);
            Timer.schedule(DiscordIntegration::updateActivity, 60f, 60f);
        LegenderyCum.on(DiscordMessageEvent.class, event -> {
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
        LegenderyCum.on(BanEvent.class, event -> Groups.player.each(
                player -> player.uuid().equals(event.ban.uuid) || player.ip().equals(event.ban.ip),
                player -> {
                    Admins.kickReason(player, event.ban.remaining(), event.ban.reason, "kick.banned-by-admin",
                            event.ban.adminName).kick();
                    Bundle.send("events.admin.ban", event.ban.adminName, player.coloredName(), event.ban.reason);
                }));
        LegenderyCum.on(AdminRequestConfirmEvent.class, event -> {
            if (event.server.equals(config.mode.displayName))
                DiscordIntegration.confirm(event.uuid);
        });
        LegenderyCum.on(AdminRequestDenyEvent.class, event -> {
            if (event.server.equals(config.mode.displayName))
                DiscordIntegration.deny(event.uuid);
        });
        LegenderyCum.on(SetRankSyncEvent.class, event -> {
            var player = Find.playerByUUID(event.uuid);
            if (player == null)
                return;
            var data = Cache.get(player);
            data.rank = event.rank;
            Ranks.name(player, data);
        });

        LegenderyCum.on(ListRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

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

                default -> throw new IllegalStateException();
            }
        });

        LegenderyCum.on(StatusRequest.class, request -> {
            if (request.server.equals(config.mode.displayName))
                LegenderyCum.respond(request, state.isPlaying() ? EmbedResponse.success("Server Running")
                        .withField("Players:", String.valueOf(Groups.player.size()))
                        .withField("Units:", String.valueOf(Groups.unit.size()))
                        .withField("Map:", state.map.plainName())
                        .withField("Wave:", String.valueOf(state.wave))
                        .withField("TPS:", String.valueOf(graphics.getFramesPerSecond()))
                        .withField("RAM usage:", app.getJavaHeap() / 1024 / 1024 + " MB")
                        : EmbedResponse.error("Server Offline")
                                .withField("TPS:", String.valueOf(graphics.getFramesPerSecond()))
                                .withField("RAM usage:", app.getJavaHeap() / 1024 / 1024 + " MB"));
        });

        LegenderyCum.on(ExitRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            netServer.kickAll(KickReason.serverRestarting);
            app.post(() -> System.exit(0));

            LegenderyCum.respond(request, EmbedResponse.success("Server Exited"));
        });

        LegenderyCum.on(ArtvRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName) || noRtv(request))
                return;

            var map = request.map == null ? maps.getNextMap(instance.lastMode, state.map) : Find.map(request.map);
            if (notFound(request, map))
                return;

            Bundle.send("commands.artv.info", request.admin);
            instance.play(false, () -> world.loadMap(map));

            LegenderyCum.respond(request, EmbedResponse.success("Map Changed").withField("Map:", map.plainName()));
        });

        LegenderyCum.on(MapRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;
        
            var map = Find.map(request.map);
            byte[] mapImageData = MapGenerator.renderMap(map);
            if (notFound(request, map))
                return;
            String base64ImageData = Base64.getEncoder().encodeToString(mapImageData);
            String imageDataUrl = "data:image/png;base64," + base64ImageData;
            LegenderyCum.respond(request, EmbedResponse.success(map.plainName())
                .withImage(imageDataUrl) 
                .withField("Author:", map.plainAuthor())
                .withField("Description:", map.plainDescription())
                .withFooter("@x@", map.width, map.height)
                .withFile(map.file.absolutePath()));
        });
        LegenderyCum.on(UploadMapRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            var source = Fi.get(request.file);
            var file = customMapDirectory.child(source.name());

            file.writeBytes(source.readBytes());

            try {
                var map = MapIO.createMap(file, true);
                maps.reload();

                LegenderyCum.respond(request, EmbedResponse.success("Map Uploaded")
                        .withField("Map:", map.name())
                        .withField("File:", file.name()));
            } catch (Exception error) {
                file.delete();
                LegenderyCum.respond(request,
                        EmbedResponse.error("Invalid Map").withContent("**@** is not a valid map.", file.name()));
            }
        });

        LegenderyCum.on(RemoveMapRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            var map = Find.map(request.map);
            if (notFound(request, map) || notRemoved(request, map))
                return;

            maps.removeMap(map);
            maps.reload();

            LegenderyCum.respond(request, EmbedResponse.success("Map Removed")
                    .withField("Map:", map.name())
                    .withField("File:", map.file.name()));
        });

        LegenderyCum.on(KickRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            var target = Find.player(request.player);
            if (notFound(request, target))
                return;

            var duration = parseDuration(request.duration);
            if (invalidDuration(request, duration))
                return;

            Admins.kick(target, request.admin, duration.toMillis(), request.reason);
            LegenderyCum.respond(request, EmbedResponse.success("Player Kicked")
                    .withField("Player:", target.plainName())
                    .withField("Duration:", Bundle.formatDuration(duration))
                    .withField("Reason:", request.reason));
        });

        LegenderyCum.on(unkickRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            var info = Find.playerInfo(request.player);
            if (notFound(request, info) || notKicked(request, info))
                return;

            info.lastKicked = 0L;
            netServer.admins.kickedIPs.remove(info.lastIP);
            netServer.admins.dosBlacklist.remove(info.lastIP);

            LegenderyCum.respond(request,
                    EmbedResponse.success("Player unkicked").withField("Player:", info.plainLastName()));
        });

        LegenderyCum.on(BanRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            var info = Find.playerInfo(request.player);
            if (notFound(request, info))
                return;

            var duration = parseDuration(request.duration);
            if (invalidDuration(request, duration))
                return;

            Admins.ban(info, request.admin, duration.toMillis(), request.reason);
            LegenderyCum.respond(request, EmbedResponse.success("Player Banned")
                    .withField("Player:", info.plainLastName())
                    .withField("Duration:", Bundle.formatDuration(duration))
                    .withField("Reason:", request.reason));
        });

        LegenderyCum.on(UnbanRequest.class, request -> {
            if (!request.server.equals(config.mode.displayName))
                return;

            var info = Find.playerInfo(request.player);
            if (notFound(request, info))
                return;

            var ban = Database.removeBan(info.id, info.lastIP);
            if (notBanned(request, ban))
                return;

            LegenderyCum.respond(request, EmbedResponse.success("Player Unbanned").withField("Player:", ban.playerName));
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

    @AllArgsConstructor
    public static class ListRequest extends Request<ListResponse> {
        public final String type, server;
        public final int page;
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
