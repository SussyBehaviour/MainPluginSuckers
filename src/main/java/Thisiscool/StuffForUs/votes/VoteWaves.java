package Thisiscool.StuffForUs.votes;

import static mindustry.Vars.*;

import Thisiscool.MainHelper.Bundle;
import mindustry.gen.Player;


public class VoteWaves extends VoteSession {

    public final int waves;

    public VoteWaves(int waves) {
        this.waves = waves;
    }

    @Override
    public void vote(Player player, int sign) {
        Bundle.send(sign == 1 ? "commands.Waves.yes" : "commands.Waves.no", player.coloredName(), waves, votes() + sign,
                votesRequired());
        super.vote(player, sign);
    }

    @Override
    public void left(Player player) {
        if (votes.remove(player) != 0)
            Bundle.send("commands.Waves.left", player.coloredName(), votes(), votesRequired());
    }

    @Override
    public void success() {
        stop();
        Bundle.send("commands.Waves.success", waves);

        for (int i = 0; i < waves; i++)
            logic.runWave();
    }

    @Override
    public void fail() {
        stop();
        Bundle.send("commands.Waves.fail", waves);
    }
}