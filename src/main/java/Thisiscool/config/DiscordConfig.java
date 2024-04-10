package Thisiscool.config;

import static Thisiscool.PluginVars.*;
import static mindustry.Vars.*;

import Thisiscool.MainHelper.ConfigLoader;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;

public class DiscordConfig {

    public static DiscordConfig discordConfig;

    public static void load() {
        discordConfig = ConfigLoader.load(DiscordConfig.class, discordConfigFile);
        Log.info("Discord Config loaded. (@)", dataDirectory.child(discordConfigFile).absolutePath());
    }

    public String token = "token";

    public String prefix = "prefix";

    public long banChannelID = 0L;

    public long adminChannelID = 0L;

    public long votekickChannelID = 0L;
    public long reportChannelID = 0L;
    public Seq<Long> adminRoleIDs = Seq.with(0L);

    public Seq<Long> mapReviewerRoleIDs = Seq.with(0L);

    public Long ReportRoleID= 0L;
    
    public ObjectMap<String, Long> serverToChannel = ObjectMap.of("server", 0L);
}