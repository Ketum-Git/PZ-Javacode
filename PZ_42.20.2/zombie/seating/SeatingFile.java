// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.seating;

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
import java.util.Locale;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptParser;
import zombie.util.StringUtils;

public final class SeatingFile {
    final ArrayList<SeatingFile.Tileset> tilesets = new ArrayList<>();
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION3 = 3;
    public static final int VERSION_LATEST = 3;
    int version = 3;
    private static final int COORD_MULT = 10000;

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
            throw new IOException("missing VERSION in seating.txt");
        } else {
            this.version = PZMath.tryParseInt(value.getValue().trim(), -1);
            if (this.version >= 1 && this.version <= 3) {
                for (ScriptParser.Block child : block.children) {
                    if ("tileset".equals(child.type)) {
                        SeatingFile.Tileset tileset = this.parseTileset(child);
                        if (tileset != null) {
                            this.tilesets.add(tileset);
                        }
                    }
                }
            } else {
                throw new IOException(String.format("unknown seating.txt VERSION \"%s\"", value.getValue().trim()));
            }
        }
    }

    SeatingFile.Tileset parseTileset(ScriptParser.Block block) {
        SeatingFile.Tileset tileset = new SeatingFile.Tileset();
        ScriptParser.Value value = block.getValue("name");
        tileset.name = value.getValue().trim();

        for (ScriptParser.Block child : block.children) {
            if ("tile".equals(child.type)) {
                SeatingFile.Tile tile = this.parseTile(tileset, child);
                if (tile != null) {
                    int index = tileset.tiles.size();

                    for (int i = 0; i < tileset.tiles.size(); i++) {
                        SeatingFile.Tile tile2 = tileset.tiles.get(i);
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

    SeatingFile.Tile parseTile(SeatingFile.Tileset tileset, ScriptParser.Block block) {
        SeatingFile.Tile tile = new SeatingFile.Tile();
        ScriptParser.Value value = block.getValue("xy");
        String[] ss = value.getValue().trim().split(this.version < 3 ? "x" : "\\s+");
        tile.col = Integer.parseInt(ss[0]);
        tile.row = Integer.parseInt(ss[1]);
        if (this.version == 1) {
            tile.col--;
            tile.row--;
        }

        for (ScriptParser.Block child : block.children) {
            if ("position".equals(child.type)) {
                SeatingFile.Position position = this.parsePosition(child);
                if (position != null) {
                    tile.positions.add(position);
                }
            } else if ("properties".equals(child.type)) {
                this.parseProperties(child, tile.properties);
                if (this.version < 3) {
                    this.propertiesToPositions(tile);
                }
            }
        }

        return tile;
    }

    SeatingFile.Position parsePosition(ScriptParser.Block block) {
        SeatingFile.Position position = new SeatingFile.Position();
        ScriptParser.Value value = block.getValue("id");
        if (value != null && !StringUtils.isNullOrWhitespace(value.getValue())) {
            position.id = value.getValue().trim();
            this.parseVector3(block, "translate", position.translate);

            for (ScriptParser.Block child : block.children) {
                if ("properties".equals(child.type)) {
                    this.parseProperties(child, position.properties);
                }
            }

            return position;
        } else {
            return null;
        }
    }

    void parseProperties(ScriptParser.Block block, HashMap<String, String> properties) {
        if (!block.values.isEmpty()) {
            for (ScriptParser.Value value : block.values) {
                String key = value.getKey().trim().intern();
                String val = value.getValue().trim().intern();
                if (!key.isEmpty()) {
                    properties.put(key, val);
                }
            }
        }
    }

    float parseCoord(String str) {
        return this.version < 3 ? PZMath.tryParseFloat(str, 0.0F) : PZMath.tryParseInt(str, 0) / 10000.0F;
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
        String[] ss = str.trim().split(this.version < 3 ? "x" : "\\s+");
        v.x = this.parseCoord(ss[0]);
        v.y = this.parseCoord(ss[1]);
        return v;
    }

    Vector3f parseVector3(String str, Vector3f v) {
        String[] ss = str.trim().split(this.version < 3 ? "x" : "\\s+");
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

    void propertiesToPositions(SeatingFile.Tile tile) {
        if (tile.properties != null) {
            String translateX = tile.properties.remove("translateX");
            String translateY = tile.properties.remove("translateY");
            String translateZ = tile.properties.remove("translateZ");
            if (translateX != null && translateY != null && translateZ != null) {
                float x = this.parseCoord(translateX);
                float y = this.parseCoord(translateY);
                float z = this.parseCoord(translateZ);
                SeatingFile.Position position = new SeatingFile.Position();
                position.id = "default";
                position.translate.set(x, y, z);
                tile.positions.add(position);
            }
        }
    }

    void write(String fileName) {
        ScriptParser.Block blockOG = new ScriptParser.Block();
        blockOG.type = "seating";
        blockOG.setValue("VERSION", String.valueOf(3));
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keys = new ArrayList<>();

        for (SeatingFile.Tileset tileset : this.tilesets) {
            ScriptParser.Block blockTS = new ScriptParser.Block();
            blockTS.type = "tileset";
            blockTS.setValue("name", tileset.name);

            for (SeatingFile.Tile tile : tileset.tiles) {
                if (!tile.isEmpty()) {
                    ScriptParser.Block blockT = new ScriptParser.Block();
                    blockT.type = "tile";
                    blockT.setValue("xy", String.format("%d %d", tile.col, tile.row));

                    for (SeatingFile.Position position : tile.positions) {
                        ScriptParser.Block blockP = new ScriptParser.Block();
                        blockP.type = "position";
                        blockP.setValue("id", position.id);
                        blockP.setValue("translate", this.formatVector3(position.translate));
                        if (!position.properties.isEmpty()) {
                            ScriptParser.Block blockProperties = this.propertiesToBlock(position.properties, keys);
                            blockP.elements.add(blockProperties);
                            blockP.children.add(blockProperties);
                        }

                        blockT.elements.add(blockP);
                        blockT.children.add(blockP);
                    }

                    if (tile.properties != null && !tile.properties.isEmpty()) {
                        ScriptParser.Block blockP = this.propertiesToBlock(tile.properties, keys);
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

    ScriptParser.Block propertiesToBlock(HashMap<String, String> properties, ArrayList<String> keys) {
        ScriptParser.Block blockP = new ScriptParser.Block();
        blockP.type = "properties";
        keys.clear();
        keys.addAll(properties.keySet());
        keys.sort(Comparator.naturalOrder());

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            blockP.setValue(key, properties.get(key));
        }

        return blockP;
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

    int coordInt(float f) {
        return Math.round(f * 10000.0F);
    }

    String formatFloat(float f) {
        return String.format(Locale.US, "%d", this.coordInt(f));
    }

    String formatVector3(float x, float y, float z) {
        return String.format(Locale.US, "%d %d %d", this.coordInt(x), this.coordInt(y), this.coordInt(z));
    }

    String formatVector3(Vector3f v) {
        return this.formatVector3(v.x, v.y, v.z);
    }

    void Reset() {
        this.tilesets.clear();
    }

    static final class Position {
        String id;
        final Vector3f translate = new Vector3f();
        final HashMap<String, String> properties = new HashMap<>();

        SeatingFile.Position set(SeatingFile.Position other) {
            this.id = other.id;
            this.translate.set(other.translate);
            this.properties.clear();
            this.properties.putAll(other.properties);
            return this;
        }

        String getProperty(String key) {
            return this.properties.get(key);
        }

        void setProperty(String key, String value) {
            if (value == null) {
                this.properties.remove(key);
            } else {
                this.properties.put(key, value);
            }
        }
    }

    static final class Tile {
        int col;
        int row;
        final ArrayList<SeatingFile.Position> positions = new ArrayList<>();
        final HashMap<String, String> properties = new HashMap<>();

        boolean isEmpty() {
            return this.properties.isEmpty() && this.positions.isEmpty();
        }

        SeatingFile.Position getPositionByIndex(int index) {
            return index >= 0 && index < this.positions.size() ? this.positions.get(index) : null;
        }

        SeatingFile.Tile set(SeatingFile.Tile other) {
            this.positions.clear();

            for (SeatingFile.Position position : other.positions) {
                this.positions.add(new SeatingFile.Position().set(position));
            }

            this.properties.clear();
            this.properties.putAll(other.properties);
            return this;
        }
    }

    static final class Tileset {
        String name;
        final ArrayList<SeatingFile.Tile> tiles = new ArrayList<>();

        SeatingFile.Tile getTile(int col, int row) {
            for (SeatingFile.Tile tile : this.tiles) {
                if (tile.col == col && tile.row == row) {
                    return tile;
                }
            }

            return null;
        }

        SeatingFile.Tile getOrCreateTile(int col, int row) {
            SeatingFile.Tile tile = this.getTile(col, row);
            if (tile != null) {
                return tile;
            } else {
                tile = new SeatingFile.Tile();
                tile.col = col;
                tile.row = row;
                int index = this.tiles.size();

                for (int i = 0; i < this.tiles.size(); i++) {
                    SeatingFile.Tile tile2 = this.tiles.get(i);
                    if (tile2.col + tile2.row * 8 > col + row * 8) {
                        index = i;
                        break;
                    }
                }

                this.tiles.add(index, tile);
                return tile;
            }
        }

        void merge(SeatingFile.Tileset other) {
            for (SeatingFile.Tile tile2 : other.tiles) {
                SeatingFile.Tile tile1 = this.getTile(tile2.col, tile2.row);
                if (tile1 == null || tile1.isEmpty()) {
                    tile1 = this.getOrCreateTile(tile2.col, tile2.row);
                    tile1.set(tile2);
                }
            }
        }
    }
}
