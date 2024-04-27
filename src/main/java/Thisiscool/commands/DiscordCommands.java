package Thisiscool.commands;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.DiscordConfig.*;
import static Thisiscool.utils.Checks.*;
import static arc.Core.*;
import static mindustry.Vars.*;

import java.time.Duration;
import java.util.Base64;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.StuffForUs.Pets;
import Thisiscool.config.Config;
import Thisiscool.config.Config.Gamemode;
import Thisiscool.config.DiscordConfig;
import Thisiscool.database.Database;
import Thisiscool.database.models.Ban;
import Thisiscool.database.models.Petsdata;
import Thisiscool.database.models.PlayerData;
import Thisiscool.discord.DiscordBot;
import Thisiscool.discord.MessageContext;
import Thisiscool.listeners.LegenderyCumEvents.ArtvRequest;
import Thisiscool.listeners.LegenderyCumEvents.EmbedResponse;
import Thisiscool.utils.Admins;
import Thisiscool.utils.Find;
import Thisiscool.utils.MapGenerator;
import Thisiscool.utils.PageIterator;
import Thisiscool.utils.Utils;
import arc.files.Fi;
import arc.math.Mathf;
import arc.struct.IntMap;
import arc.util.CommandHandler;
import arc.util.Http;
import arc.util.Strings;
import arc.util.Structs;
import discord4j.core.object.entity.User;
import discord4j.core.object.reaction.ReactionEmoji;
import mindustry.Vars;
import mindustry.content.Items;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.io.MapIO;
import mindustry.server.ServerControl;
import mindustry.type.Item;

public class DiscordCommands {
    public static final IntMap<User> playerLinkCodes = new IntMap<>();

    public static void load() {
        discordHandler = new CommandHandler(DiscordConfig.prefix);
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
            boolean isServerRunning = state.isPlaying();
            int playerCount = Groups.player.size();
            int unitCount = Groups.unit.size();
            String mapName = state.map.plainName();
            int wave = state.wave;
            int tps = graphics.getFramesPerSecond();
            long ramUsage = app.getJavaHeap() / 1024 / 1024;
            String response = String.format(
                    "Server Status:\n" +
                            "Server Running: %b\n" +
                            "Players: %d\n" +
                            "Units: %d\n" +
                            "Map: %s\n" +
                            "Wave: %d\n" +
                            "TPS: %.2f\n" +
                            "RAM usage: %d MB",
                    isServerRunning, playerCount, unitCount, mapName, wave, tps, ramUsage);
            context.info("Server Status", response, new Object[0]).subscribe();
        });

        discordHandler.<MessageContext>register("artv", "[map...]", "Force map change.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            Gamemode server = Config.getMode();
            String mapName = args.length > 0 ? args[0] : null;
            if (mapName != null) {
                new ArtvRequest(server.displayName, mapName, context.member().getDisplayName());
                context.info("Map Change Requested", "Map change to " + mapName + " has been requested.", new Object[0])
                        .subscribe();
            } else {
                context.info("Invalid Request", "Please specify a map name.", new Object[0]).subscribe();
            }
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
            var map = Find.map(args[0]);
            if (map == null) {
                context.error("Error", "Map not found.").subscribe();
                return;
            }
            byte[] mapImageData = MapGenerator.renderMap(map);
            String base64ImageData = Base64.getEncoder().encodeToString(mapImageData);
            String imageDataUrl = "data:image/png;base64," + base64ImageData;
            context.info(embed -> embed
                    .title("Map Information")
                    .addField("Map:", map.plainName(), false)
                    .addField("Author:", map.plainAuthor(), false)
                    .addField("Description:", map.plainDescription(), false)
                    .addField("Size:", map.width + "x" + map.height, false)
                    .image(imageDataUrl))
                    .subscribe();
        });
        discordHandler.<MessageContext>register("uploadmap", "Upload a map to the server.",
                (args, context) -> {
                    if (noRole(context, discordConfig.mapReviewerRoleIDs) || notMap(context))
                        return;
                    context.message()
                            .getAttachments()
                            .stream()
                            .filter(attachment -> attachment.getFilename().endsWith(mapExtension))
                            .forEach(attachment -> Http.get(attachment.getUrl(), response -> {
                                var file = tmpDirectory.child(attachment.getFilename());
                                file.writeBytes(response.getResult());
                                var source = Fi.get(file.absolutePath());
                                var mapFile = customMapDirectory.child(source.name());
                                try {
                                    var map = MapIO.createMap(mapFile, true);
                                    maps.reload();
                                    context.reply(EmbedResponse.success("Map Uploaded")
                                            .withField("Map:", map.name())
                                            .withField("File:", mapFile.name()));
                                } catch (Exception error) {
                                    mapFile.delete();
                                    context.reply(EmbedResponse.error("Invalid Map")
                                            .withContent("**@** is not a valid map.", mapFile.name()));
                                }
                            }));
                });
        discordHandler.<MessageContext>register("removemap", "<map...>", "Remove a map from the server.",
                (args, context) -> {
                    if (noRole(context, discordConfig.mapReviewerRoleIDs))
                        return;
                    var map = Find.map(args[0]);
                    if (map == null) {
                        context.reply(embed -> embed.title("Error").description("Map not found.")).subscribe();
                        return;
                    }
                    if (notRemoved(map)) {
                        context.reply(embed -> embed.title("Error").description("Failed to remove map.")).subscribe();
                        return;
                    }
                    maps.removeMap(map);
                    maps.reload();
                    context.reply(embed -> embed
                            .title("Map Removed")
                            .addField("Map:", map.name(), false)
                            .addField("File:", map.file.name(), false))
                            .subscribe();
                });
        discordHandler.<MessageContext>register("kick", "<player> <duration> [reason...]", "Kick a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;
                    var player = Find.player(args[0]);
                    if (player == null) {
                        context.reply(embed -> embed
                                .title("Error")
                                .description("Player not found.")).subscribe();
                        return;
                    }
                    var duration = Thisiscool.utils.Utils.parseDuration(args[1]);
                    if (duration == null) {
                        context.reply(embed -> embed
                                .title("Error")
                                .description("Invalid duration.")).subscribe();
                        return;
                    }
                    String reason = args.length > 2 ? args[2] : "Not Specified";
                    Admins.kick(player, context.member().getDisplayName(), duration.toMillis(), reason);
                    context.reply(embed -> embed
                            .title("Player Kicked")
                            .addField("Player:", player.name(), false)
                            .addField("Duration:", duration.toString(), false)
                            .addField("Reason:", reason, false))
                            .subscribe();
                });
        discordHandler.<MessageContext>register("unkick", "<player...>", "unkick a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;
                    var player = Find.playerInfo(args[0]);
                    if (player == null) {
                        context.reply(embed -> embed
                                .title("Error")
                                .description("Player not found.")).subscribe();
                        return;
                    }
                    player.lastKicked = 0L;
                    netServer.admins.kickedIPs.remove(player.lastIP);
                    netServer.admins.dosBlacklist.remove(player.lastIP);
                    context.reply(embed -> embed
                            .title("Player Unkicked")
                            .addField("Player:", player.plainLastName(), false))
                            .subscribe();
                });
        discordHandler.<MessageContext>register("ban", "<player> <duration> [reason...]", "Ban a player.",
                (args, context) -> {
                    if (noRole(context, discordConfig.adminRoleIDs))
                        return;
                    var player = Find.playerInfo(args[0]);
                    if (player == null) {
                        context.reply(embed -> embed
                                .title("Error")
                                .description("Player not found.")).subscribe();
                        return;
                    }
                    var duration = Thisiscool.utils.Utils.parseDuration(args[1]);
                    if (duration == null) {
                        context.reply(embed -> embed
                                .title("Error")
                                .description("Invalid duration.")).subscribe();
                        return;
                    }
                    String reason = args.length > 2 ? args[2] : "Not Specified";
                    Admins.ban(player, context.member().getDisplayName(), duration.toMillis(), reason);
                    context.reply(embed -> embed
                            .title("Player Banned")
                            .addField("Player:", player.plainLastName(), false)
                            .addField("Duration:", duration.toString(), false)
                            .addField("Reason:", reason, false))
                            .subscribe();
                });

        discordHandler.<MessageContext>register("unban", "<player...>", "Unban a player.", (args, context) -> {
            if (noRole(context, discordConfig.adminRoleIDs))
                return;
            var player = Find.playerInfo(args[0]);
            if (player == null) {
                context.reply(embed -> embed
                        .title("Error")
                        .description("Player not found.")).subscribe();
                return;
            }
            player.lastKicked = 0L;
            netServer.admins.kickedIPs.remove(player.lastIP);
            netServer.admins.unbanPlayerID(player.id);
            netServer.admins.unbanPlayerIP(player.lastIP);
            netServer.admins.dosBlacklist.remove(player.lastIP);
            context.reply(embed -> embed
                    .title("Player UnBanned")
                    .addField("Player:", player.plainLastName(), false))
                    .subscribe();
        });
        discordHandler.<MessageContext>register("info", "<player...>", "Look up a player stats.", (args, context) -> {
            var data = Find.playerData(args[0]);
            if (notFound(context, data))
                return;

            // Start building the embed message
            context.info(embed -> {
                embed.title("Player Stats")
                        .addField("Player:", data.plainName(), false)
                        .addField("DiscordName", DiscordBot.getUserNameById(data.DiscordId).block(), false)
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
                            Call.sendMessage("[accent]" + context.member().getDisplayName(),
                                    " wants to link with you. If you wish to link your account, type [accent] /link "
                                            + code
                                            + " to link.",
                                    p);
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
                    context.success(embed -> embed
                            .title("Rank Changed")
                            .addField("Player:", data.plainName(), false)
                            .addField("Rank:", rank.name(), false)).subscribe();
                });
        discordHandler.<MessageContext>register("addpet", "<species> <color> <name...>",
                (args, context) -> {
                    PlayerData pd = Database.getPlayerDataByDiscordId(context.member().getId().asLong());
                    if (pd == null) {
                        context.error("Not in database", "You have not linked your discord account. Type **"
                                + DiscordConfig.prefix + "link** to link.").subscribe();
                        return;
                    }
                    String petName = args[2];
                    if (Strings.stripColors(petName).length() > 30 || petName.length() > 300) {
                        context.error("Pet name is too long", "Please choose a shorter name for your pet").subscribe();
                        return;
                    }

                    var pets = Petsdata.getPets(pd.uuid);
                    if (pets.length >= Pets.maxPets(pd.rank.toString())) {
                        context.error("Too Many Pets",
                                "You currently have " + pets.length + " pets, but a " + pd.rank.toString()
                                        + " can only have " + Pets.maxPets(pd.rank.toString())
                                        + " pets. Increase your rank for more pets.")
                                .subscribe();
                        return;
                    }
                    for (var pet : pets) {
                        if (pet.name.equalsIgnoreCase(petName)) {
                            context.error("Pet already exists", "You already have a pet named '" + pet.name + "'")
                                    .subscribe();
                            return;
                        }
                    }

                    var pet = new Petsdata.Pet(pd.uuid, petName);
                    pet.color = Utils.getColorByName(args[1]);
                    if (pet.color == null) {
                        context.error("Not a valid color", "Make sure you are using deafult mindustry format")
                                .subscribe();
                        return;
                    }
                    pet.species = Vars.content.units().find(u -> u.name.equalsIgnoreCase(args[0]));
                    if (pet.species == null) {
                        context.error("Invalid Species", "'" + args[0] + "' is not a valid unit")
                                .subscribe();
                        return;
                    }

                    int tier = Pets.tierOf(pet.species);
                    if (tier < 0) {
                        context.error("Unsupported Species",
                                "Species must be T1-4, not be a naval unit, and not be the antumbra").subscribe();
                        return;
                    }

                    if (tier > Pets.maxTier(pd.rank.toString())) {
                        context.error("Insufficient Rank", pet.species.name + " is tier " + tier + ", but a "
                                + pd.rank.toString() + " can only have tier " + Pets.maxTier(pd.rank.toString())
                                + " pets.")
                                .subscribe();
                        return;
                    }

                    Petsdata.addPet(pet);
                    context.success("Created pet", "Successfully created " + pet.name + ". Type in-game **/pet "
                            + pet.name + "** to spawn your pet.").subscribe();
                });
        discordHandler.<MessageContext>register("pet", "<name...>", "Show pet information", (args, context) -> {
            PlayerData pd = Database.getPlayerDataByDiscordId(context.member().getId().asLong());
            if (pd == null) {
                context.error("Not in database", "You have not linked your discord account. Use /redeem to link.");
                return;
            }

            String name = args[0];
            var pets = Petsdata.getPets(pd.uuid);
            var pet = Structs.find(pets, p -> p.name.equalsIgnoreCase(name));
            if (pet == null) {
                context.error("No such pet", "You don't have a pet named '" + name + "'");
                return;
            }
            String foodEaten = "";
            Item[] foods = Pets.possibleFoods(pet.species);
            if (Structs.contains(foods, Items.coal)) {
                foodEaten += Items.coal.emoji() + " " + pet.eatenCoal + "\n";
            }
            if (Structs.contains(foods, Items.copper)) {
                foodEaten += Items.copper.emoji() + " " + pet.eatenCopper + "\n";
            }
            if (Structs.contains(foods, Items.lead)) {
                foodEaten += Items.lead.emoji() + " " + pet.eatenLead + "\n";
            }
            if (Structs.contains(foods, Items.titanium)) {
                foodEaten += Items.titanium.emoji() + " " + pet.eatenTitanium + "\n";
            }
            if (Structs.contains(foods, Items.thorium)) {
                foodEaten += Items.thorium.emoji() + " " + pet.eatenThorium + "\n";
            }
            if (Structs.contains(foods, Items.beryllium)) {
                foodEaten += Items.beryllium.emoji() + " " + pet.eatenBeryllium + "\n";
            }

            final String trimmedFoodEaten = foodEaten.trim();
            context.success(embed -> embed
                    .title("Pet: " + pet.name)
                    .addField("Species", pet.species.localizedName, true)
                    .addField("Color", "#" + pet.color.toString(), true)
                    .addField("Food Eaten", trimmedFoodEaten, false)
                    .addField("Rank", pd.rank.toString(), true)
                    .addField("Owner", pd.plainName(), true));
        });
    }
}
