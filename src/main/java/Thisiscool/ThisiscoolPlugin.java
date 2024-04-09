package Thisiscool;

import static Thisiscool.config.Config.*;
import static Thisiscool.utils.Utils.*;
import static mindustry.Vars.*;

import Thisiscool.commands.AdminCommands;
import Thisiscool.commands.ClientCommands;
import Thisiscool.commands.DiscordCommands;
import Thisiscool.commands.ServerCommands;
import Thisiscool.config.Config;
import Thisiscool.config.DiscordConfig;
import Thisiscool.database.Database;
import Thisiscool.discord.DiscordBot;
import Thisiscool.features.Alerts;
import Thisiscool.features.Console;
import Thisiscool.features.SchemeSize;
import Thisiscool.features.menus.MenuHandler;
import Thisiscool.features.net.LegenderyCum;
import Thisiscool.listeners.LegenderyCumEvents;
import Thisiscool.listeners.NetHandlers;
import Thisiscool.listeners.PluginEvents;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Time;
import mindustry.core.Version;
import mindustry.gen.AdminRequestCallPacket;
import mindustry.mod.Plugin;
import mindustry.net.Packets.Connect;
import mindustry.net.Packets.ConnectPacket;
import useful.Bundle;
import useful.Commands;

public class ThisiscoolPlugin extends Plugin {

    @Override
    public void init() {
        Log.info("Loading Thisiscool plugin.");
        Time.mark();

        Console.load();
        Config.load();
        DiscordConfig.load();

        Bundle.load(getClass());
        Commands.load();

        Alerts.load();
        MenuHandler.load();
        SchemeSize.load();

        Database.connect();
        LegenderyCum.connect();

        PluginEvents.load();
        LegenderyCumEvents.load();

        if (config.mode.isMainServer) {
            DiscordBot.connect();
            DiscordCommands.load();
        }

        Version.build = -1;

        net.handleServer(Connect.class, NetHandlers::connect);
        net.handleServer(ConnectPacket.class, NetHandlers::connect);
        net.handleServer(AdminRequestCallPacket.class, NetHandlers::adminRequest);

        netServer.admins.addChatFilter(NetHandlers::chat);
        netServer.invalidHandler = NetHandlers::invalidResponse;

        maps.setMapProvider((mode, map) -> availableMaps().random(map));

        Log.info("Thisiscool plugin loaded in @ ms.", Time.elapsed());
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        ClientCommands.load();
        AdminCommands.load();
    }

    @Override
    public void registerServerCommands(CommandHandler handler) {
        ServerCommands.load(handler);
    }
}
