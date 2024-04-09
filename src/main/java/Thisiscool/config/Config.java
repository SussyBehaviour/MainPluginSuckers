package Thisiscool.config;

import static Thisiscool.PluginVars.*;
import static Thisiscool.config.Config.Gamemode.*;
import static mindustry.Vars.*;
import static mindustry.net.Administration.Config.*;

import arc.util.Log;
import useful.ConfigLoader;

public class Config {

    public static Config config;

    public static void load() {
        config = ConfigLoader.load(Config.class, configFile);
        Log.info("Config loaded. (@)", dataDirectory.child(configFile).absolutePath());

        allowCustomClients.set(true);
        showConnectMessages.set(false);
        antiSpam.set(true);
        autoPause.set(false);

        interactRateWindow.set(1);
        interactRateLimit.set(25);
        interactRateKick.set(50);
        messageRateLimit.set(1);
        messageSpamKick.set(5);
        packetSpamLimit.set(500);
        snapshotInterval.set(200);
        roundExtraTime.set(10);
        maxLogLength.set(1024 * 1024);

        strict.set(config.mode.enableStrict);
        enableVotekick.set(config.mode.enableVotekick);
    }

    public String hubIp = "Thisiscool.net";

    public int hubPort = 6567;

    public int sockPort = 8306;

    public String mongoUrl = "url";

    public Gamemode mode = hub;

    public enum Gamemode {
        attack("Attack"),

        Towerdefense("Towerdefense Wars") {
            {
                isDefault = false;
                enableWaves = false;
                enableSurrender = true;
            }
        },

        crawler("Crawler Arena") {
            {
                isDefault = false;
                enableWaves = false;
            }
        },

        Football("Football") {
            {
                enableWaves = false;
                enableSurrender = true;
            }
        },

        hexed("Hexed PvP") {
            {
                isDefault = false;
                enableRtv = false;
                enableWaves = false;
                enableVotekick = false;
            }
        },

        hub("Hub") {
            {
                isDefault = false;
                isMainServer = true;
                enableRtv = false;
                enableWaves = false;
                enableVotekick = false;
            }
        },

        msgo("HungerGames") {
            {
                isDefault = false;
                enableRtv = false;
                enableWaves = false;
            }
        },

        pvp("PvP") {
            {
                enableWaves = false;
                enableSurrender = true;
            }
        },

        sandbox("Sandbox") {
            {
                enableStrict = false;
            }
        },

        survival("Survival"),
        tower("Tower Defense");

        public final String displayName;

        public boolean isDefault = true;
        public boolean isMainServer = false;

        public boolean enableRtv = true;
        public boolean enableWaves = true;
        public boolean enableStrict = true;
        public boolean enableVotekick = true;
        public boolean enableSurrender = false;

        Gamemode(String displayName) {
            this.displayName = displayName;
        }

        public static String getDisplayName(String name) {
            return Gamemode.valueOf(name).displayName;
        }
    }
}