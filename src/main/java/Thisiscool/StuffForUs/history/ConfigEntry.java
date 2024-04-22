package Thisiscool.StuffForUs.history;

import static mindustry.Vars.*;

import Thisiscool.MainHelper.Bundle;
import arc.math.geom.Point2;
import arc.struct.Seq;
import arc.util.Structs;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.ai.UnitCommand;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.ConfigEvent;
import mindustry.gen.Player;
import mindustry.world.blocks.logic.CanvasBlock;
import mindustry.world.blocks.logic.CanvasBlock.CanvasBuild;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.logic.LogicBlock.LogicBuild;
import mindustry.world.blocks.power.LightBlock;
import mindustry.world.blocks.units.UnitFactory.UnitFactoryBuild;


public class ConfigEntry implements HistoryEntry {
    public final String uuid;
    public final short blockID;
    public final Object config;
    public final long timestamp;

    public ConfigEntry(ConfigEvent event) {
        this.uuid = event.player.uuid();
        this.blockID = event.tile.block.id;
        this.config = getConfig(event);
        this.timestamp = Time.millis();
    }

    @Override
    public String getMessage(Player player) {
        var info = netServer.admins.getInfo(uuid);
        var block = content.block(blockID);
        String message;

        if (config instanceof UnlockableContent) {
            UnlockableContent content = (UnlockableContent) config;
            message = Bundle.format("history.config", player, info.lastName, block.emoji(), content.emoji(),
                    Bundle.formatRelative(player, timestamp));
        } else if (config instanceof Boolean) {
            Boolean enabled = (Boolean) config;
            message = enabled
                    ? Bundle.format("history.config.on", player, info.lastName, block.emoji(),
                            Bundle.formatRelative(player, timestamp))
                    : Bundle.format("history.config.off", player, info.lastName, block.emoji(),
                            Bundle.formatRelative(player, timestamp));
        } else if (config instanceof String) {
            String text = (String) config;
            message = text.isBlank()
                    ? Bundle.format("history.config.default", player, info.lastName, block.emoji(),
                            Bundle.formatRelative(player, timestamp))
                    : Bundle.format("history.config.text", player, info.lastName, block.emoji(),
                            text.replaceAll("\n", " "), Bundle.formatRelative(player, timestamp));
        } else if (config instanceof UnitCommand) {
            UnitCommand command = (UnitCommand) config;
            message = Bundle.format("history.config.command", player, info.lastName, block.emoji(), command.getEmoji(),
                    Bundle.formatRelative(player, timestamp));
        } else if (config instanceof Point2) {
            Point2 point = (Point2) config;
            message = point.pack() == -1
                    ? Bundle.format("history.config.disconnect", player, info.lastName, block.emoji(),
                            Bundle.formatRelative(player, timestamp))
                    : Bundle.format("history.config.connect", player, info.lastName, block.emoji(), point,
                            Bundle.formatRelative(player, timestamp));
        } else if (config instanceof Point2[]) {
            Point2[] points = (Point2[]) config;
            message = points.length == 0
                    ? Bundle.format("history.config.disconnect", player, info.lastName, block.emoji(),
                            Bundle.formatRelative(player, timestamp))
                    : Bundle.format("history.config.connect", player, info.lastName, block.emoji(),
                            Seq.with(points).toString(", "), Bundle.formatRelative(player, timestamp));
        } else {
            if (block instanceof LightBlock) {
                message = Bundle.format("history.config.color", player, info.lastName, block.emoji(),
                        Tmp.c1.set((int) config), Bundle.formatRelative(player, timestamp));
            } else if (block instanceof LogicBlock) {
                message = Bundle.format("history.config.code", player, info.lastName, block.emoji(),
                        Bundle.formatRelative(player, timestamp));
            } else if (block instanceof CanvasBlock) {
                message = Bundle.format("history.config.image", player, info.lastName, block.emoji(),
                        Bundle.formatRelative(player, timestamp));
            } else {
                message = Bundle.format("history.config.default", player, info.lastName, block.emoji(),
                        Bundle.formatRelative(player, timestamp));
            }
        }

        return message;
    }

    public Object getConfig(ConfigEvent event) {
        if (event.tile instanceof LogicBuild || event.tile instanceof CanvasBuild)
            return null;

        if (event.tile instanceof UnitFactoryBuild factory)
            return factory.unit();

        if (event.tile.config() instanceof Point2 point)
            return point.add(event.tile.tileX(), event.tile.tileY());

        if (event.tile.config() instanceof Point2[] points) {
            Structs.each(point -> point.add(event.tile.tileX(), event.tile.tileY()), points);
            return points;
        }

        return event.tile.config();
    }
}