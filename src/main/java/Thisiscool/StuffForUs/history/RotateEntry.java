package Thisiscool.StuffForUs.history;

import static Thisiscool.utils.Utils.*;
import static mindustry.Vars.*;

import Thisiscool.MainHelper.Bundle;
import arc.util.Time;
import mindustry.game.EventType.BuildRotateEvent;
import mindustry.gen.Player;


public class RotateEntry implements HistoryEntry {
    public final String uuid;
    public final short blockID;
    public final int rotation;
    public final long timestamp;

    public RotateEntry(BuildRotateEvent event) {
        this.uuid = event.unit.getPlayer().uuid();
        this.blockID = event.build.block.id;
        this.rotation = event.build.rotation;
        this.timestamp = Time.millis();
    }

    public String getMessage(Player player) {
        var info = netServer.admins.getInfo(uuid);
        var block = content.block(blockID);

        return Bundle.format("history.rotate", player, info.lastName, block.emoji(), formatRotation(rotation),
                Bundle.formatRelative(player, timestamp));
    }
}