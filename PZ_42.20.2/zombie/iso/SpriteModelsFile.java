// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Locale;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class SpriteModelsFile {
    final ArrayList<SpriteModelsFile.Tileset> tilesets = new ArrayList<>();
    public static final int VERSION1 = 1;
    public static final int VERSION_LATEST = 1;
    int version = 1;

    void read(String fileName) {
        File file = new File(fileName);

        try (
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
        ) {
            CharBuffer totalFile = CharBuffer.allocate((int)file.length());
            br.read(totalFile);
            totalFile.flip();
            this.parseFile(totalFile.toString());
        } catch (FileNotFoundException var14) {
        } catch (Exception var15) {
            ExceptionLogger.logException(var15);
        }
    }

    void parseFile(String totalFile) throws IOException {
        totalFile = ScriptParser.stripComments(totalFile);
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        ScriptParser.Value value = block.getValue("VERSION");
        if (value == null) {
            throw new IOException("missing VERSION in spriteModels.txt");
        } else {
            this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
            if (this.version >= 1 && this.version <= 1) {
                for (ScriptParser.Block child : block.children) {
                    if ("tileset".equals(child.type)) {
                        SpriteModelsFile.Tileset tileset = this.parseTileset(child);
                        if (tileset != null) {
                            this.tilesets.add(tileset);
                        }
                    }
                }
            } else {
                throw new IOException(String.format("unknown spriteModels.txt VERSION \"%s\"", value.getValue().trim()));
            }
        }
    }

    SpriteModelsFile.Tileset parseTileset(ScriptParser.Block block) {
        SpriteModelsFile.Tileset tileset = new SpriteModelsFile.Tileset();
        ScriptParser.Value value = block.getValue("name");
        tileset.name = value.getValue().trim();

        for (ScriptParser.Block child : block.children) {
            if ("tile".equals(child.type)) {
                SpriteModelsFile.Tile tile = this.parseTile(tileset, child);
                if (tile != null) {
                    int index = tileset.tiles.size();

                    for (int i = 0; i < tileset.tiles.size(); i++) {
                        SpriteModelsFile.Tile tile2 = tileset.tiles.get(i);
                        if (tile2.getIndex() > tile.getIndex()) {
                            index = i;
                            break;
                        }
                    }

                    tileset.tiles.add(index, tile);
                }
            }
        }

        return tileset;
    }

    SpriteModelsFile.Tile parseTile(SpriteModelsFile.Tileset tileset, ScriptParser.Block block) {
        ScriptParser.Value value2 = block.getValue("runtime");
        if (value2 != null) {
            return this.parseRuntimeTile(tileset, block);
        } else {
            SpriteModelsFile.Tile tile = new SpriteModelsFile.Tile();
            ScriptParser.Value value = block.getValue("xy");
            String[] ss = value.getValue().trim().split(" ");
            tile.col = Integer.parseInt(ss[0]);
            tile.row = Integer.parseInt(ss[1]);
            tile.spriteModel.modelScriptName = block.getValue("modelScript").getValue().trim();
            if ((value = block.getValue("texture")) != null) {
                tile.spriteModel.textureName = StringUtils.discardNullOrWhitespace(value.getValue().trim());
            }

            this.parseVector3f(block.getValue("translate").getValue().trim(), tile.spriteModel.translate);
            this.parseVector3f(block.getValue("rotate").getValue().trim(), tile.spriteModel.rotate);
            tile.spriteModel.scale = PZMath.tryParseFloat(block.getValue("scale").getValue().trim(), 1.0F);
            if ((value = block.getValue("animation")) != null) {
                String animationName = value.getValue().trim();
                tile.spriteModel.animationName = StringUtils.discardNullOrWhitespace(animationName);
            }

            if ((value = block.getValue("animationTime")) != null) {
                String animationTime = value.getValue();
                tile.spriteModel.animationTime = PZMath.tryParseFloat(animationTime, -1.0F);
            }

            for (ScriptParser.Block child : block.children) {
                if ("xxx".equals(child.type)) {
                }
            }

            return tile;
        }
    }

    SpriteModelsFile.Tile parseRuntimeTile(SpriteModelsFile.Tileset tileset, ScriptParser.Block block) {
        SpriteModelsFile.Tile tile = new SpriteModelsFile.Tile();
        ScriptParser.Value value = block.getValue("xy");
        String[] ss = value.getValue().trim().split(" ");
        tile.col = Integer.parseInt(ss[0]);
        tile.row = Integer.parseInt(ss[1]);
        value = block.getValue("runtime");
        tile.spriteModel.parseRuntimeString(tileset.name, tile.col, tile.row, value.getValue());
        tile.spriteModel.setModule(ScriptManager.instance.getModule("Base"));
        tile.spriteModel.InitLoadPP(tile.spriteModel.getModelScriptName().substring("Base.".length()));
        return tile;
    }

    void parseVector3f(String str, Vector3f v) {
        String[] ss = str.split(" ");
        v.setComponent(0, PZMath.tryParseFloat(ss[0], 0.0F));
        v.setComponent(1, PZMath.tryParseFloat(ss[1], 0.0F));
        v.setComponent(2, PZMath.tryParseFloat(ss[2], 0.0F));
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "spriteModel";
        blockOG.setValue("VERSION", String.valueOf(1));
        StringBuilder sb = new StringBuilder();

        for (SpriteModelsFile.Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);

            for (SpriteModelsFile.Tile tile : tileset.tiles) {
                ScriptParser.Block blockT = new ScriptParser.Block();
                blockT.type = "tile";
                blockT.setValue("xy", String.format("%d %d", tile.col, tile.row));
                if (tile.spriteModel.runtimeString != null) {
                    blockT.setValue("runtime", tile.spriteModel.runtimeString);
                } else {
                    SpriteModel sm = tile.spriteModel;
                    blockT.setValue("modelScript", sm.modelScriptName);
                    if (sm.textureName != null) {
                        blockT.setValue("texture", sm.textureName);
                    }

                    blockT.setValue("translate", String.format(Locale.US, "%.4f %.4f %.4f", sm.translate.x, sm.translate.y, sm.translate.z));
                    blockT.setValue("rotate", String.format(Locale.US, "%.4f %.4f %.4f", sm.rotate.x, sm.rotate.y, sm.rotate.z));
                    blockT.setValue("scale", String.format(Locale.US, "%.4f", sm.scale));
                    if (sm.animationName != null) {
                        blockT.setValue("animation", sm.animationName);
                    }

                    if (sm.animationTime >= 0.0F) {
                        blockT.setValue("animationTime", String.format(Locale.US, "%.4f", sm.animationTime));
                    }
                }

                blockTS.elements.add(blockT);
                blockTS.children.add(blockT);
            }

            blockOG.elements.add(blockTS);
            blockOG.children.add(blockTS);
        }

        sb.setLength(0);
        String eol = System.lineSeparator();
        blockOG.prettyPrint(0, sb, eol);
        this.write(fileName, sb.toString());
    }

    void write(String fileName, String totalFile) {
        File file = new File(fileName);

        try (
            FileWriter fw = new FileWriter(file);
            BufferedWriter br = new BufferedWriter(fw);
        ) {
            br.write(totalFile);
        } catch (Throwable var12) {
            ExceptionLogger.logException(var12);
        }
    }

    void Reset() {
    }

    public static final class Tile {
        int col;
        int row;
        public final SpriteModel spriteModel = new SpriteModel();

        int getIndex() {
            return this.col + this.row * 8;
        }
    }

    public static final class Tileset {
        String name;
        final ArrayList<SpriteModelsFile.Tile> tiles = new ArrayList<>();

        public String getName() {
            return this.name;
        }

        public SpriteModelsFile.Tile getTile(int col, int row) {
            for (SpriteModelsFile.Tile tile : this.tiles) {
                if (tile.col == col && tile.row == row) {
                    return tile;
                }
            }

            return null;
        }

        SpriteModelsFile.Tile getOrCreateTile(int col, int row) {
            SpriteModelsFile.Tile tile = this.getTile(col, row);
            if (tile != null) {
                return tile;
            } else {
                tile = new SpriteModelsFile.Tile();
                tile.col = col;
                tile.row = row;
                int index = this.tiles.size();

                for (int i = 0; i < this.tiles.size(); i++) {
                    SpriteModelsFile.Tile tile2 = this.tiles.get(i);
                    if (tile2.getIndex() > tile.getIndex()) {
                        index = i;
                        break;
                    }
                }

                this.tiles.add(index, tile);
                return tile;
            }
        }

        void initSprites() {
            for (int i = 0; i < this.tiles.size(); i++) {
                SpriteModelsFile.Tile tile = this.tiles.get(i);
                if (tile != null) {
                    String tileName = String.format("%s_%d", this.name, tile.getIndex());
                    IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(tileName);
                    if (sprite != null && sprite.spriteModel == null) {
                        sprite.spriteModel = tile.spriteModel;
                    }
                }
            }
        }
    }
}
