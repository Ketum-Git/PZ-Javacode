// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.seams;

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
import java.util.Comparator;
import java.util.HashMap;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;

public final class SeamFile {
    final ArrayList<SeamFile.Tileset> tilesets = new ArrayList<>();
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
            throw new IOException("missing VERSION in seams.txt");
        } else {
            this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
            if (this.version >= 1 && this.version <= 1) {
                for (ScriptParser.Block child : block.children) {
                    if ("tileset".equals(child.type)) {
                        SeamFile.Tileset tileset = this.parseTileset(child);
                        if (tileset != null) {
                            this.tilesets.add(tileset);
                        }
                    }
                }
            } else {
                throw new IOException(String.format("unknown seams.txt VERSION \"%s\"", value.getValue().trim()));
            }
        }
    }

    SeamFile.Tileset parseTileset(ScriptParser.Block block) {
        SeamFile.Tileset tileset = new SeamFile.Tileset();
        ScriptParser.Value value = block.getValue("name");
        tileset.name = value.getValue().trim();

        for (ScriptParser.Block child : block.children) {
            if ("tile".equals(child.type)) {
                SeamFile.Tile tile = this.parseTile(tileset, child);
                if (tile != null) {
                    int index = tileset.tiles.size();

                    for (int i = 0; i < tileset.tiles.size(); i++) {
                        SeamFile.Tile tile2 = tileset.tiles.get(i);
                        if (tile2.col + tile2.row * 8 > tile.col + tile.row * 8) {
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

    SeamFile.Tile parseTile(SeamFile.Tileset tileset, ScriptParser.Block block) {
        SeamFile.Tile tile = new SeamFile.Tile();
        ScriptParser.Value value = block.getValue("xy");
        String[] ss = value.getValue().trim().split("x");
        tile.col = Integer.parseInt(ss[0]);
        tile.row = Integer.parseInt(ss[1]);
        tile.tileName = String.format("%s_%d", tileset.name, tile.col + tile.row * 8);

        for (ScriptParser.Block child : block.children) {
            if ("east".equals(child.type)) {
                tile.joinE = this.parseTileNameList(child);
            } else if ("south".equals(child.type)) {
                tile.joinS = this.parseTileNameList(child);
            } else if ("belowEast".equals(child.type)) {
                tile.joinBelowE = this.parseTileNameList(child);
            } else if ("belowSouth".equals(child.type)) {
                tile.joinBelowS = this.parseTileNameList(child);
            } else if ("properties".equals(child.type)) {
                tile.properties = this.parseTileProperties(child);
            }
        }

        return tile.isNull() ? null : tile;
    }

    ArrayList<String> parseTileNameList(ScriptParser.Block block) {
        if (block.values.isEmpty()) {
            return null;
        } else {
            ArrayList<String> tileNames = new ArrayList<>();

            for (ScriptParser.Value value : block.values) {
                String key = value.getKey().trim();
                if (!key.isEmpty()) {
                    tileNames.add(key);
                }
            }

            return tileNames;
        }
    }

    HashMap<String, String> parseTileProperties(ScriptParser.Block block) {
        if (block.values.isEmpty()) {
            return null;
        } else {
            HashMap<String, String> result = new HashMap<>();

            for (ScriptParser.Value value : block.values) {
                String key = value.getKey().trim();
                String val = value.getValue().trim();
                if (!key.isEmpty()) {
                    result.put(key, val);
                }
            }

            return result;
        }
    }

    float parseCoord(String str) {
        return PZMath.tryParseFloat(str, 0.0F);
    }

    float parseCoord(ScriptParser.Block block, String valueName, float defaultValue) {
        ScriptParser.Value value = block.getValue(valueName);
        if (value == null) {
            return defaultValue;
        } else {
            String str = value.getValue().trim();
            return this.parseCoord(str);
        }
    }

    Vector2f parseVector2(String str, Vector2f v) {
        String[] ss = str.trim().split("x");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        return v;
    }

    Vector3f parseVector3(String str, Vector3f v) {
        String[] ss = str.trim().split("x");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        v.z = this.parseCoord(ss[2]);
        return v;
    }

    boolean parseVector3(ScriptParser.Block block, String valueName, Vector3f v) {
        ScriptParser.Value value = block.getValue(valueName);
        if (value == null) {
            return false;
        } else {
            String str = value.getValue().trim();
            this.parseVector3(str, v);
            return true;
        }
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "seams";
        blockOG.setValue("VERSION", String.valueOf(1));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys = new ArrayList<>();

        for (SeamFile.Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);

            for (SeamFile.Tile tile : tileset.tiles) {
                if (!tile.isNull()) {
                    ScriptParser.Block blockT = new ScriptParser.Block();
                    blockT.type = "tile";
                    blockT.setValue("xy", String.format("%dx%d", tile.col, tile.row));
                    this.writeTileNames(blockT, tile.joinE, "east");
                    this.writeTileNames(blockT, tile.joinS, "south");
                    this.writeTileNames(blockT, tile.joinBelowE, "belowEast");
                    this.writeTileNames(blockT, tile.joinBelowS, "belowSouth");
                    if (tile.properties != null && !tile.properties.isEmpty()) {
                        ScriptParser.Block blockP = new ScriptParser.Block();
                        blockP.type = "properties";
                        keys.clear();
                        keys.addAll(tile.properties.keySet());
                        keys.sort(Comparator.naturalOrder());

                        for (int i = 0; i < keys.size(); i++) {
                            String key = keys.get(i);
                            blockP.setValue(key, tile.properties.get(key));
                        }

                        blockT.elements.add(blockP);
                        blockT.children.add(blockP);
                    }

                    blockT.comment = String.format("/* %s_%d */", tileset.name, tile.col + tile.row * 8);
                    blockTS.elements.add(blockT);
                    blockTS.children.add(blockT);
                }
            }

            blockOG.elements.add(blockTS);
            blockOG.children.add(blockTS);
        }

        sb.setLength(0);
        String eol = System.lineSeparator();
        blockOG.prettyPrint(0, sb, eol);
        this.write(fileName, sb.toString());
    }

    void writeTileNames(ScriptParser.Block blockT, ArrayList<String> tileNames, String type) {
        if (tileNames != null && !tileNames.isEmpty()) {
            ScriptParser.Block blockJ = new ScriptParser.Block();
            blockJ.type = type;

            for (int i = 0; i < tileNames.size(); i++) {
                String tileName = tileNames.get(i);
                blockJ.setValue(tileName, "");
            }

            blockT.elements.add(blockJ);
            blockT.children.add(blockJ);
        }
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
        public String tileName;
        public int col;
        public int row;
        public ArrayList<String> joinE;
        public ArrayList<String> joinS;
        public ArrayList<String> joinBelowE;
        public ArrayList<String> joinBelowS;
        public HashMap<String, String> properties;

        public boolean isMasterTile() {
            return this.joinE != null && !this.joinE.isEmpty()
                || this.joinS != null && !this.joinS.isEmpty()
                || this.joinBelowE != null && !this.joinBelowE.isEmpty()
                || this.joinBelowS != null && !this.joinBelowS.isEmpty();
        }

        public String getMasterTileName() {
            return this.properties == null ? null : this.properties.get("master");
        }

        public boolean isNull() {
            if (this.joinE != null && !this.joinE.isEmpty()) {
                return false;
            } else if (this.joinS != null && !this.joinS.isEmpty()) {
                return false;
            } else if (this.joinBelowE != null && !this.joinBelowE.isEmpty()) {
                return false;
            } else {
                return this.joinBelowS != null && !this.joinBelowS.isEmpty() ? false : this.properties == null || this.properties.isEmpty();
            }
        }
    }

    public static final class Tileset {
        String name;
        final ArrayList<SeamFile.Tile> tiles = new ArrayList<>();

        SeamFile.Tile getTile(int col, int row) {
            for (SeamFile.Tile tile : this.tiles) {
                if (tile.col == col && tile.row == row) {
                    return tile;
                }
            }

            return null;
        }

        SeamFile.Tile getOrCreateTile(int col, int row) {
            SeamFile.Tile tile = this.getTile(col, row);
            if (tile != null) {
                return tile;
            } else {
                tile = new SeamFile.Tile();
                tile.col = col;
                tile.row = row;
                int index = this.tiles.size();

                for (int i = 0; i < this.tiles.size(); i++) {
                    SeamFile.Tile tile2 = this.tiles.get(i);
                    if (tile2.col + tile2.row * 8 > col + row * 8) {
                        index = i;
                        break;
                    }
                }

                this.tiles.add(index, tile);
                return tile;
            }
        }
    }
}
