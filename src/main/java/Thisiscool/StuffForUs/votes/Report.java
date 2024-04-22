package Thisiscool.StuffForUs.votes;

import static Thisiscool.utils.Checks.*;

import Thisiscool.MainHelper.Bundle;
import Thisiscool.config.Config;
import mindustry.gen.Player;


public class Report {

    public final Player initiator, target;
    public final String reason;
    public final  String server;

    public Report(Player initiator, Player target, String reason) {
        this.initiator = initiator;
        this.target = target;
        this.reason = reason;
        this.server = Config.config.mode.displayName;
    }

    public void report(Player player, int sign) {
        if (invalidVoteTarget(player, target))
            return;

        Bundle.send(sign == 1 ? "commands.report.yes" : "commands.report.no", player.coloredName(),
                target.coloredName(), reason);
    }
}