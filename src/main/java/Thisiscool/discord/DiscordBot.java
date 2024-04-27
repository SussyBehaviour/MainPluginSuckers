package Thisiscool.discord;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.DiscordConfig.*;
import static Thisiscool.utils.Checks.*;

import java.util.function.Predicate;

import Thisiscool.config.Config;
import Thisiscool.listeners.LegenderyCumEvents.DiscordMessageEvent;
import arc.Events;
import arc.util.Log;
import discord4j.common.ReactorResources;
import discord4j.common.retry.ReconnectOptions;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.SelectMenuInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import discord4j.core.util.OrderUtil;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.util.AllowedMentions;
import discord4j.rest.util.Color;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.LoopResources;
import reactor.scheduler.forkjoin.ForkJoinPoolScheduler;

public class DiscordBot {

    public static GatewayDiscordClient gateway;

    public static GuildMessageChannel banChannel;
    public static GuildMessageChannel adminChannel;
    public static GuildMessageChannel votekickChannel;
    public static GuildMessageChannel reportChannel;
    public static boolean connected;

    public static Mono<String> getUserNameById(long userId) {
        Snowflake snowflake = Snowflake.of(userId);
        return gateway.getUserById(snowflake)
                .map(User::getUsername)
                .switchIfEmpty(Mono.just("notlinked"));
    }

    public static void connect() {
        try {
            HttpResources.set(LoopResources.create("d4j-http", 4, true));

            gateway = DiscordClientBuilder.create(discordConfig.token)
                    .setDefaultAllowedMentions(AllowedMentions.suppressAll())
                    .setReactorResources(ReactorResources.builder()
                            .timerTaskScheduler(Schedulers.newParallel("d4j-parallel", 4, true))
                            .build())
                    .build()
                    .gateway()
                    .setReconnectOptions(ReconnectOptions.builder()
                            .setBackoffScheduler(Schedulers.newParallel("d4j-backoff", 4, true))
                            .build())
                    .setEventDispatcher(EventDispatcher.builder()
                            .eventScheduler(ForkJoinPoolScheduler.create("d4j-events", 4))
                            .build())
                    .setEnabledIntents(IntentSet.of(Intent.GUILD_MEMBERS, Intent.GUILD_MESSAGES))
                    .login()
                    .blockOptional()
                    .orElseThrow();

            banChannel = gateway.getChannelById(Snowflake.of(discordConfig.banChannelID))
                    .ofType(GuildMessageChannel.class).block();
            adminChannel = gateway.getChannelById(Snowflake.of(discordConfig.adminChannelID))
                    .ofType(GuildMessageChannel.class).block();
            votekickChannel = gateway.getChannelById(Snowflake.of(discordConfig.votekickChannelID))
                    .ofType(GuildMessageChannel.class).block();
            reportChannel = gateway.getChannelById(Snowflake.of(discordConfig.reportChannelID))
                    .ofType(GuildMessageChannel.class).block();
            gateway.on(MessageCreateEvent.class).subscribe(event -> {
                var message = event.getMessage();
                if (message.getContent().isEmpty())
                    return;

                var member = event.getMember().orElse(null);
                if (member == null || member.isBot())
                    return;
                message.getChannel()
                        .map(channel -> new MessageContext(message, member, channel))
                        .subscribe(context -> {
                            var response = discordHandler.handleMessage(message.getContent(), context);
                            switch (response.type) {
                                case fewArguments ->
                                    context.error("Too Few Arguments", "Usage: @**@** @", discordHandler.prefix,
                                            response.runCommand, response.command.paramText).subscribe();
                                case manyArguments ->
                                    context.error("Too Many Arguments", "Usage: @**@** @", discordHandler.prefix,
                                            response.runCommand, response.command.paramText).subscribe();
                                case unknownCommand -> context.error("Unknown Command",
                                        "To see a list of all available commands, use @**help**", discordHandler.prefix)
                                        .subscribe();

                                case valid ->
                                    Log.info("[Discord] @ used @", member.getDisplayName(), message.getContent());
                                default -> throw new IllegalArgumentException("Unexpected value: " + response.type);
                            }
                        });

                // Prevent commands from being sent to the game
                if (message.getContent().startsWith(getPrefix()))
                    return;
                if (message.getChannelId().asLong() != discordConfig.Chat)
                    return;

                var server = discordConfig.Chat;
                if (server == null)
                    return;

                var roles = event.getClient()
                        .getGuildRoles(member.getGuildId())
                        .filter(role -> member.getRoleIds().contains(role.getId()))
                        .sort(OrderUtil.ROLE_ORDER)
                        .cache();

                roles.takeLast(1)
                        .singleOrEmpty()
                        .zipWith(roles.map(Role::getColor)
                                .filter(Predicate.not(Predicate.isEqual(Role.DEFAULT_COLOR)))
                                .last(Color.WHITE))
                        .switchIfEmpty(Mono.fromRunnable(() -> Events.fire(new DiscordMessageEvent(Config.getMode().displayName, member.getDisplayName(),
                                        message.getContent()))))
                        .subscribe(TupleUtils.consumer((role,
                                color) -> Events.fire(new DiscordMessageEvent(Config.getMode().displayName, role.getName(),
                                                Integer.toHexString(color.getRGB()), member.getDisplayName(),
                                                message.getContent()))));
            });


            gateway.on(SelectMenuInteractionEvent.class).subscribe(event -> {
                if (noRole(event, discordConfig.adminRoleIDs))
                    return;

                if (event.getCustomId().equals("admin-request")) {
                    var content = event.getValues().get(0).split("-", 3);
                    if (content.length < 3)
                        return;

                    switch (content[0]) {
                        case "confirm" -> DiscordIntegration.confirm(event, content[1], content[2]);
                        case "deny" -> DiscordIntegration.deny(event, content[1], content[2]);
                    }
                }
            });

            gateway.getSelf()
                    .flatMap(user -> gateway.getGuilds()
                            .flatMap(guild -> guild
                                    .changeSelfNickname("[" +getPrefix() + "] " + user.getUsername()))
                            .then())
                    .subscribe();

            connected = true;

            Log.info("Bot connected.");
        } catch (Exception e) {
            Log.err("Failed to connect bot", e);
        }
    }
}