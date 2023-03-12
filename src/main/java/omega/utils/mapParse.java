package omega.utils;

import arc.func.Prov;
import arc.graphics.Pixmap;
import arc.graphics.PixmapIO.PngWriter;
import arc.util.io.CounterInputStream;
import omega.OmegaPlugin;
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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

import static arc.graphics.Color.blackRgba;
import static arc.util.io.Streams.OptimizedByteArrayOutputStream;
import static arc.util.io.Streams.emptyBytes;
import static omega.utils.Logger.discLog;
import static omega.utils.Logger.discLogErr;
import static omega.utils.Utils.notNullElse;
import static mindustry.Vars.*;

public class mapParse {

    public static void load() {
        try {
            var pixmap = new Pixmap(mods.getMod(OmegaPlugin.class).root.child("block_colors.png"));

            for (int i = 0; i < pixmap.width; i++) {
                var block = content.block(i);
                if (!(block instanceof OreBlock))
                    block.mapColor.rgba8888(pixmap.get(i, 0)).a(1f);
            }

            pixmap.dispose();

           discLog("Loaded @ block colors.", pixmap.width);
        } catch (Exception e) {
            discLogErr("Error reading file block_colors.png: @", e);
        }
    }

    public static byte[] renderMap(Map map) {
        try {
            return parseImage(generatePreview(map), true);
        } catch (Exception e) {
            return emptyBytes;
        }
    }

    public static byte[] renderMinimap() {
        return parseImage(MapIO.generatePreview(world.tiles), false);
    }

    public static byte[] renderMinimapLive() {
        return parseImage(MapIO.generatePreview(world.tiles), false);
    }

    public static byte[] parseImage(Pixmap pixmap, boolean flip) {
        var writer = new PngWriter(pixmap.width * pixmap.height);
        var stream = new OptimizedByteArrayOutputStream(pixmap.width * pixmap.height);

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
        try (var counter = new CounterInputStream(new InflaterInputStream(map.file.read(bufferSize))); var stream = new DataInputStream(counter)) {
            SaveIO.readHeader(stream);

            var version = new FixedSave(stream.readInt());

            var pixmap = new Pixmap(map.width, map.height);
            var tile = new ContainerTile() {
                @Override
                public void setBlock(Block block) {
                    super.setBlock(block);

                    int color = MapIO.colorFor(block, Blocks.air, Blocks.air, notNullElse(team, Team.derelict));
                    if (color != blackRgba) pixmap.set(x, y, color);
                }
            };

            version.region("meta", stream, counter, version::readStringMap);
            version.region("content", stream, counter, version::readContentHeader);
            version.region("preview_map", stream, counter, input -> version.readMap(input, new WorldContext() {
                @Override
                public void resize(int width, int height) {}

                @Override
                public boolean isGenerating() {
                    return false;
                }

                @Override
                public void begin() {}

                @Override
                public void end() {}

                @Override
                public void onReadBuilding() {
                    if (tile.team == null) return;

                    var block = tile.block();

                    for (int x = 0; x < block.size; x++)
                        for (int y = 0; y < block.size; y++)
                            pixmap.set(tile.x + x + block.sizeOffset, tile.y + y + block.sizeOffset, tile.team.color.rgba8888());
                }

                @Override
                public Tile tile(int index) {
                    tile.x = (short) (index % map.width);
                    tile.y = (short) (index / map.width);
                    return tile;
                }

                @Override
                public Tile create(int x, int y, int floorID, int overlayID, int wallID) {
                    pixmap.set(x, y, MapIO.colorFor(Blocks.air, content.block(floorID), content.block(overlayID), Team.derelict));
                    return null;
                }
            }));

            return pixmap;
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
        protected void changeBuild(Team team, Prov<Building> prov, int rotation) {}

        @Override
        protected void changed() {}
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

                for (int consecutive = i + stream.readUnsignedByte(); i <= consecutive; i++)
                    context.create(i % width, i / width, floorID, oreID, 0);

                i--;
            }

            for (int i = 0; i < width * height; i++) {
                var block = notNullElse(content.block(stream.readShort()), Blocks.air);
                var tile = context.tile(i);

                byte packed = stream.readByte();
                boolean hadBuild = (packed & 1) != 0,
                        hadData = (packed & 2) != 0,
                        isCenter = !hadBuild || stream.readBoolean();

                if (isCenter)
                    tile.setBlock(block);

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
                    for (int consecutive = i + stream.readUnsignedByte(); i <= consecutive; i++)
                        context.tile(i + 1).setBlock(block);

                    i--;
                }
            }
        }
    }
}