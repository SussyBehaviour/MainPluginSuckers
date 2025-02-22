package Thisiscool.commands;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.Config.*;
import static Thisiscool.utils.Checks.*;
import static mindustry.Vars.*;
import static mindustry.server.ServerControl.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.MainHelper.Commands;
import Thisiscool.StuffForUs.Pets;
import Thisiscool.StuffForUs.menus.MenuHandler;
import Thisiscool.StuffForUs.net.Translator;
import Thisiscool.StuffForUs.votes.Report;
import Thisiscool.StuffForUs.votes.VoteKick;
import Thisiscool.StuffForUs.votes.VoteRtv;
import Thisiscool.StuffForUs.votes.VoteSurrender;
import Thisiscool.database.Cache;
import Thisiscool.database.Database;
import Thisiscool.database.models.Petsdata;
import Thisiscool.listeners.LegenderyCumEvents.AdminRequestEvent;
import Thisiscool.utils.Find;
import Thisiscool.utils.PageIterator;
import Thisiscool.utils.Utils;
import arc.Events;
import arc.struct.Seq;
import arc.util.Structs;
import mindustry.gen.Call;

public class ClientCommands {

    public static void load() {
        Commands.create("help")
                .welcomeMessage(true)
                .register(PageIterator::commands);
        Commands.create("discord")
                .welcomeMessage(true)
                .register((args, player) -> Call.openURI(player.con, discordServerUrl));

        Commands.create("sync")
                .cooldown(15000L)
                .register((args, player) -> {
                    Call.worldDataBegin(player.con);
                    netServer.sendWorldData(player);
                });

        Commands.create("t").register((args, player) -> Translator.translate(other -> other.team() == player.team(),
                player, args[0], "commands.t.chat", player.team().color, player.coloredName()));
        Commands.create("players").register(PageIterator::players);
        Commands.create("settings")
                .welcomeMessage(true)
                .register((args, player) -> MenuHandler.showSettingsMenu(player));
                Commands.create("trail")
                .welcomeMessage(true)
                .register((args, player) -> MenuHandler.showTrailMenu(player));
        Commands.create("stats")
                .welcomeMessage(true)
                .register((args, player) -> {
                    var target = args.length > 0 ? Find.player(args[0]) : player;
                    if (notFound(player, target))
                        return;

                    MenuHandler.showStatsMenu(player, target, Cache.get(target));
                });

        Commands.create("votekick")
                .cooldown(300000L)
                .register((args, player) -> {
                    if (votekickDisabled(player) || alreadyVoting(player, voteKick))
                        return;

                    var target = Find.player(args[0]);
                    if (notFound(player, target) || invalidVotekickTarget(player, target))
                        return;

                    voteKick = new VoteKick(player, target, args[1]);
                    voteKick.vote(player, 1);
                });

        Commands.create("vote")
                .register((args, player) -> {
                    if (notVoting(player, voteKick))
                        return;

                    if (args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("cancel")) {
                        if (notAdmin(player))
                            return;

                        voteKick.cancel(player);
                        return;
                    }

                    int sign = Utils.voteChoice(args[0]);
                    if (invalidVoteSign(player, sign))
                        return;

                    voteKick.vote(player, sign);
                });

        Commands.hidden("wink")
                .cooldown(300000L)
                .register((args, player) -> {
                    if (alreadyAdmin(player))
                        return;

                    MenuHandler.showConfirmMenu(player, "commands.login.confirm", () -> {
                        Events.fire(new AdminRequestEvent(config.mode.displayName, Cache.get(player)));
                        Bundle.send(player, "commands.login.sent");
                    });
                });

        Commands.create("rtv")
                .enabled(config.mode.enableRtv)
                .cooldown(60000L)
                .welcomeMessage(true)
                .register((args, player) -> {
                    if (alreadyVoting(player, vote))
                        return;

                    var map = args.length > 0 ? Find.map(args[0]) : maps.getNextMap(instance.lastMode, state.map);
                    if (notFound(player, map))
                        return;

                    vote = new VoteRtv(map);
                    vote.vote(player, 1);
                });
        Commands.create("maps")
                .enabled(config.mode.enableRtv)
                .register(PageIterator::maps);

        Commands.create("surrender")
                .enabled(config.mode.enableSurrender)
                .cooldown(180000L)
                .welcomeMessage(true)
                .register((args, player) -> {
                    if (alreadyVoting(player, vote) || invalidSurrenderTeam(player))
                        return;

                    vote = new VoteSurrender(player.team());
                    vote.vote(player, 1);
                });
        Commands.create("report")
                .welcomeMessage(true)
                .cooldown(60000L)
                .register((args, player) -> {
                    var target = Find.player(args[0]);
                    if (notFound(player, target)) {
                        return;
                    }
                    report = new Report(player, target, args[1]);
                    Bundle.send(player, "commands.report.yes");
                });
        Commands.create("link")
                .cooldown(1000)
                .welcomeMessage(true)
                .register((args, player) -> {
                    int code = Integer.parseInt(args[0]);
                    if (Database.getPlayerData(player).DiscordId != 0) {
                        Call.sendMessage("[red]", "You are already linked to a discord account.", player);
                        return;
                    }
                    if (!DiscordCommands.playerLinkCodes.containsKey(code)) {
                        Call.sendMessage("[red]", "Wrong code.", player);
                    } else {
                        Database.getPlayerData(player).DiscordId = DiscordCommands.playerLinkCodes.get(code).getId()
                                .asLong();
                        Database.savePlayerData(Database.getPlayerData(player));
                        Call.sendMessage("[green]", "You are linked to a discord account.", player);
                        DiscordCommands.playerLinkCodes.remove(code);
                    }
                });
        Commands.create("pet")
                .welcomeMessage(true)
                .register((args, player) -> {
                    var pets = Petsdata.getPets(player.uuid());
                    if (pets == null || pets.length == 0) {
                        Call.sendMessage(player.con,"[red]", "You have no pets to spawn.", player);
                        return;
                    }
                    var pet = args.length == 0 ? null : Structs.find(pets, p -> p.name.equalsIgnoreCase(args[0]));
                    if (pet == null) {
                        Call.sendMessage(player.con,"[red]", "Pet not found.", player);
                        return;
                    }

                    var alreadySpawned = Pets.spawnedPets.get(player.uuid(), new Seq<>());
                    if (alreadySpawned.contains(pet.name)) {
                        Call.sendMessage(player.con,"[red]", "This pet is already spawned.", player);
                        return;
                    }

                    if (!Pets.spawnPet(pet, player)) {
                        Call.sendMessage(player.con,"[red]", "Failed to spawn pet.", player);
                        return;
                    }

                    alreadySpawned.add(pet.name);
                    Pets.spawnedPets.put(player.uuid(), alreadySpawned);
                    Call.sendMessage(player.con,"[green]", "Pet spawned successfully.", player);
                });
        Commands.create("petdespawn")
                .welcomeMessage(true)
                .register((args, player) -> {
                    var spawned = Pets.spawnedPets.get(player.uuid());
                    if (spawned == null) {
                        Call.sendMessage("[red]", "You have no pets to despawn.", player);
                        return;
                    }
                    if (!spawned.contains(args[0])) {
                        Call.sendMessage("[red]", "Pet not found or not spawned.", player);
                        return;
                    }
                    spawned.remove(args[0]);
                    Call.sendMessage("[green]", "Pet despawned successfully.", player);
                });
    }

}