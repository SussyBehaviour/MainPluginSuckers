package Thisiscool.utils;

import static arc.util.io.Streams.*;
import static mindustry.Vars.*;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO;
import arc.struct.StringMap;
import arc.util.Log;
import arc.util.io.CounterInputStream;
import arc.util.io.Streams;
import mindustry.content.Blocks;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.io.MapIO;
import mindustry.io.SaveIO;
import mindustry.io.SaveVersion;
import mindustry.maps.Map;
import mindustry.world.Block;
import mindustry.world.CachedTile;
import mindustry.world.Tile;
import mindustry.world.WorldContext;
import mindustry.world.blocks.environment.OreBlock;
import mindustry.world.blocks.storage.CoreBlock;

public class MapGenerator {

    public static void init() {
        try {
            var pixmap = new Pixmap(Utils.getRootFolder().child("block-colors.png"));

            for (int i = 0; i < pixmap.width; i++) {
                var block = content.block(i);
                if (!(block instanceof OreBlock)) {
                    block.mapColor.rgba8888(pixmap.get(i, 0)).a(1f);
                }
            }

            pixmap.dispose();

        } catch (Exception e) {
            Log.err("Error reading block colours.", e);
        }
    }

    public static byte[] renderMap(Map map) {
        try {
            return parseImage(generatePreview(map), false);
        } catch (Exception e) {
            return emptyBytes;
        }
    }

    public static byte[] renderMinimap() {
        return parseImage(MapIO.generatePreview(world.tiles), false);
    }


    public static byte[] parseImage(Pixmap pixmap, boolean flip) {
        var writer = new PixmapIO.PngWriter(pixmap.width * pixmap.height);
        var stream = new Streams.OptimizedByteArrayOutputStream(pixmap.width * pixmap.height);
    
        try {
            writer.setFlipY(flip);
            writer.write(stream, pixmap);
            return stream.toByteArray();
        } catch (Exception e) {
            return emptyBytes;
        } finally {
            writer.dispose();
        }
    }

      private static Pixmap generatePreview(Map map) throws IOException {
        try (var counter = new CounterInputStream(new InflaterInputStream(map.file.read(bufferSize)));
             var stream = new DataInputStream(counter)) {
            SaveIO.readHeader(stream);
            int version = stream.readInt();
            SaveVersion ver = SaveIO.getSaveWriter(version);
            StringMap tags = new StringMap();
            ver.region("meta", stream, counter, in -> tags.putAll(ver.readStringMap(in)));

            Pixmap floors = new Pixmap(map.width, map.height);
            Pixmap walls = new Pixmap(map.width, map.height);
            int black = 255;
            int shade = Color.rgba8888(0f, 0f, 0f, 0.5f);
            CachedTile tile = new CachedTile() {
                @Override
                public void setBlock(Block type) {
                    super.setBlock(type);

                    int c = MapIO.colorFor(block(), Blocks.air, Blocks.air, team());
                    if (c != black) {
                        walls.setRaw(x, floors.height - 1 - y, c);
                        floors.set(x, floors.height - 1 - y + 1, shade);
                    }
                }
            };

            ver.region("content", stream, counter, ver::readContentHeader);
            ver.region("preview_map", stream, counter, in -> ver.readMap(in, new WorldContext() {
                @Override
                public void resize(int width, int height) {
                }

                @Override
                public boolean isGenerating() {
                    return false;
                }

                @Override
                public void begin() {
                    world.setGenerating(true);
                }

                @Override
                public void end() {
                    world.setGenerating(false);
                }

                @Override
                public void onReadBuilding() {
                    if (tile.build != null) {
                        int c = tile.build.team.color.rgba8888();
                        int size = tile.block().size;
                        int offsetx = -(size - 1) / 2;
                        int offsety = -(size - 1) / 2;
                        for (int dx = 0; dx < size; dx++) {
                            for (int dy = 0; dy < size; dy++) {
                                int drawx = tile.x + dx + offsetx, drawy = tile.y + dy + offsety;
                                walls.set(drawx, floors.height - 1 - drawy, c);
                            }
                        }

                        if (tile.build.block instanceof CoreBlock) {
                            map.teams.add(tile.build.team.id);
                        }
                    }
                }

                @Override
                public Tile tile(int index) {
                    tile.x = (short) (index % map.width);
                    tile.y = (short) (index / map.width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID) {
                    if (overlayID != 0) {
                        floors.set(x, floors.height - 1 - y, MapIO.colorFor(Blocks.air, Blocks.air, content.block(overlayID), Team.derelict));
                    } else {
                        floors.set(x, floors.height - 1 - y, MapIO.colorFor(Blocks.air, content.block(floorID), Blocks.air, Team.derelict));
                    }
                    if (content.block(overlayID) == Blocks.spawn) {
                        map.spawns++;
                    }
                    return tile;
                }
            }));

            floors.draw(walls, true);
            walls.dispose();
            return floors;
        } finally {
            content.setTemporaryMapper(null);
        }
    }



    public static class ContainerTile extends CachedTile {
        public Team team;

        @Override
        public void setTeam(Team team) {
            this.team = team;
        }

        @Override
        public void setBlock(Block block) {
            this.block = block;
        }

        @Override
        protected void changeBuild(Team team, Prov<Building> prov, int rotation) {
        }

        @Override
        protected void changed() {
        }
    }

    public static class FixedSave extends SaveVersion {

        public FixedSave(int version) {
            super(version);
        }

        @Override
        public void readMap(DataInput stream, WorldContext context) throws IOException {
            int width = stream.readUnsignedShort(), height = stream.readUnsignedShort();

            for (int i = 0; i < width * height; i++) {
                short floorID = stream.readShort(), oreID = stream.readShort();

                for (int consecutive = i + stream.readUnsignedByte(); i <= consecutive; i++) {
                    context.create(i % width, i / width, floorID, oreID, 0);
                }

                i--;
            }

            for (int i = 0; i < width * height; i++) {
                var block = content.block(stream.readShort()) == null ? Blocks.air : content.block(stream.readShort());
                var tile = context.tile(i);

                byte packed = stream.readByte();
                boolean hadBuild = (packed & 1) != 0, hadData = (packed & 2) != 0,
                        isCenter = !hadBuild || stream.readBoolean();

                if (isCenter) {
                    tile.setBlock(block);
                }

                if (hadBuild) {
                    if (isCenter) {
                        try {
                            readChunk(stream, true, input -> {
                                input.skipBytes(6);
                                tile.setTeam(Team.get(input.readByte()));
                                input.skipBytes(lastRegionLength - 7);
                            });
                        } catch (Exception e) {
                            continue;
                        }

                        context.onReadBuilding();
                    }
                } else if (hadData) {
                    tile.setBlock(block);
                    stream.skipBytes(1);
                } else {
                    for (int consecutive = i + stream.readUnsignedByte(); i <= consecutive; i++) {
                        context.tile(i + 1).setBlock(block);
                    }

                    i--;
                }
            }
        }
    }
}