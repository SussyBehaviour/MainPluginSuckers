package Thisiscool.StuffForUs.votes;

import Thisiscool.config.Config;
import arc.util.Log;
import mindustry.gen.Player;
public class Report {

    public final Player initiator, target;
    public final String reason;
    public final String server;

    public Report(Player initiator, Player target, String reason) {
        this.initiator = initiator;
        this.target = target;
        this.reason = reason;
        this.server = Config.config.mode.displayName;
        Log.info("Report created by " + initiator.name() + " for " + target.name() + " with reason: " + reason);
        Thisiscool.discord.DiscordIntegration.sendReportTo(initiator,target,reason);
    }
}