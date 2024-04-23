package Thisiscool.discord;

import static Thisiscool.discord.DiscordBot.*;
import static arc.Core.*;
import static discord4j.common.util.TimestampFormat.*;
import static mindustry.Vars.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Random;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.net.LegenderyCum;
import Thisiscool.config.Config;
import Thisiscool.config.Config.Gamemode;
import Thisiscool.config.DiscordConfig;
import Thisiscool.database.Database;
import Thisiscool.listeners.LegenderyCumEvents.AdminRequestConfirmEvent;
import Thisiscool.listeners.LegenderyCumEvents.AdminRequestDenyEvent;
import Thisiscool.listeners.LegenderyCumEvents.AdminRequestEvent;
import Thisiscool.listeners.LegenderyCumEvents.BanEvent;
import Thisiscool.listeners.LegenderyCumEvents.VoteKickEvent;
import Thisiscool.utils.Find;
import arc.util.Log;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.component.SelectMenu.Option;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.object.presence.Status;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import mindustry.gen.Groups;
import mindustry.gen.Player;


public class DiscordIntegration {

    public static void sendBan(BanEvent event) {
        if (!connected)
            return;

        banChannel.createMessage(EmbedCreateSpec.builder()
                .color(getRandomColor())
                .title("Ban")
                .addField("Player:", event.ban().playerName + " [" + event.ban().playerID + "]", false)
                .addField("Admin:", event.ban().adminName, false)
                .addField("Reason:", event.ban().reason, false)
                .addField("Unban Date:", LONG_DATE.format(event.ban().unbanDate.toInstant()), false)
                .footer(Gamemode.getDisplayName(event.server()), null)
                .timestamp(Instant.now()).build()).subscribe();
    }

    public static void sendVoteKick(VoteKickEvent event) {
        if (!connected)
            return;

        votekickChannel.createMessage(EmbedCreateSpec.builder()
                .color(getRandomColor())
                .title("Vote Kick")
                .addField("Kicked Player:", event.target(), false)
                .addField("Kick started By:", event.initiator(), false)
                .addField("Reason:", event.reason(), false)
                .addField("Votes For:", event.votesFor(), false)
                .addField("Votes Against:", event.votesAgainst(), false)
                .footer(Gamemode.getDisplayName(event.server()), null)
                .timestamp(Instant.now()).build()).subscribe();
    }
    public static void sendReportTo(Player initiator, Player target, String reason) {
        if (!connected) {
            Log.info("DiscordIntegration: sendReport called but not connected.");
            return;
        }
        long h = DiscordConfig.discordConfig.ReportRoleID;
        reportChannel.createMessage("<@&" + h + ">").subscribe();
        reportChannel.createMessage(EmbedCreateSpec.builder()
                .color(getRandomColor())
                .title("Reported")
                .addField("Reported player:", target.plainName(), false)
                .addField("Reported by:", initiator.plainName(), false)
                .addField("Reason:", reason, false)
                .footer(Config.getMode().displayName, null)
                .timestamp(Instant.now()).build())
                .subscribe();
    
        adminChannel.createMessage("<@&" + h + ">"   + "Slaves Get to Work").subscribe();
        adminChannel.createMessage("Reported player: " + target.plainName()+ target.uuid()).subscribe();
        adminChannel.createMessage("Reported by: " + initiator.plainName()+ initiator.uuid()).subscribe();
    }

    public static void sendAdminRequest(AdminRequestEvent event) {
        if (!connected)
            return;

        adminChannel.createMessage(EmbedCreateSpec.builder()
                .color(getRandomColor())
                .title("Admin Request")
                .description("Select the desired option to confirm or deny the request. Confirm only your requests!")
                .addField("Player:", event.data().plainName(), false)
                .addField("ID:", String.valueOf(event.data().id), false)
                .footer(Gamemode.getDisplayName(event.server()), null)
                .timestamp(Instant.now())
                .build()).withComponents(ActionRow.of(
                        SelectMenu.of("admin-request",
                                Option.of("Confirm", "confirm-" + event.server() + "-" + event.data().uuid)
                                        .withDescription("Confirm this request.").withEmoji(ReactionEmoji.unicode("✅")),
                                Option.of("Deny", "deny-" + event.server() + "-" + event.data().uuid)
                                        .withDescription("Deny this request.").withEmoji(ReactionEmoji.unicode("❌")))))
                .subscribe();
    }

    public static void confirm(SelectMenuInteractionEvent event, String server, String uuid) {
        LegenderyCum.send(new AdminRequestConfirmEvent(server, uuid));

        var data = Database.getPlayerData(uuid);
        if (data == null)
            return; // Just in case

        event.edit().withEmbeds(EmbedCreateSpec.builder()
                .color(getRandomColor())
                .title("Admin Request Confirmed")
                .addField("Player:", data.plainName(), false)
                .addField("ID:", String.valueOf(data.id), false)
                .addField("Administrator:", event.getInteraction().getUser().getMention(), false)
                .footer(Gamemode.getDisplayName(server), null)
                .timestamp(Instant.now())
                .build()).withComponents(Collections.emptyList()).subscribe();
    }

    public static void deny(SelectMenuInteractionEvent event, String server, String uuid) {
        LegenderyCum.send(new AdminRequestDenyEvent(server, uuid));

        var data = Database.getPlayerData(uuid);
        if (data == null)
            return; // Just in case

        event.edit().withEmbeds(EmbedCreateSpec.builder()
                .color(getRandomColor())
                .title("Admin Request Denied")
                .addField("Player:", data.plainName(), false)
                .addField("ID:", String.valueOf(data.id), false)
                .addField("Administrator:", event.getInteraction().getUser().getMention(), false)
                .footer(Gamemode.getDisplayName(server), null)
                .timestamp(Instant.now())
                .build()).withComponents(Collections.emptyList()).subscribe();
    }

    public static void confirm(String uuid) {
        var info = netServer.admins.getInfoOptional(uuid);
        if (info == null)
            return;

        var player = Find.playerByUUID(info.id);
        if (player != null) {
            player.admin(true);
            Bundle.send(player, "commands.login.success");
        }

        netServer.admins.adminPlayer(info.id, player == null ? info.adminUsid : player.usid());
    }

    public static void deny(String uuid) {
        var info = netServer.admins.getInfoOptional(uuid);
        if (info == null)
            return;

        var player = Find.playerByUUID(info.id);
        if (player != null) {
            player.admin(false);
            Bundle.send(player, "commands.login.fail");
        }

        netServer.admins.unAdminPlayer(info.id);
    }

    public static void updateActivity() {
        if (connected)
            updateActivity("at " + settings.getInt("totalPlayers", Groups.player.size()) + " players on Thisiscool");
    }

    public static void updateActivity(String activity) {
        if (connected)
            gateway.updatePresence(ClientPresence.of(Status.ONLINE, ClientActivity.watching(activity))).subscribe();
    }

    public static void sendMessage(long channelId, String message) {
        if (connected)
            gateway.getChannelById(Snowflake.of(channelId))
                    .ofType(GuildMessageChannel.class)
                    .flatMap(channel -> channel.createMessage(message))
                    .subscribe();
    }

    public static void sendMessageEmbed(long channelId, EmbedCreateSpec embed) {
        if (connected)
            gateway.getChannelById(Snowflake.of(channelId))
                    .ofType(GuildMessageChannel.class)
                    .flatMap(channel -> channel.createMessage(embed))
                    .subscribe();
    }
    public static Color getRandomColor() {
        Random random = new Random();
        return Color.of(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }
}