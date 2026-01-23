// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.seating;

import java.util.HashMap;
import org.joml.Vector3f;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

public final class SeatingData {
    private SeatingFile file;
    private final String mediaAbsPath;

    public SeatingData(String mediaAbsPath) {
        this.mediaAbsPath = mediaAbsPath;
    }

    public void init() {
        this.file = new SeatingFile();
        this.file.read(this.mediaAbsPath + "/seating.txt");
    }

    public void initMerged() {
        this.file = new SeatingFile();
    }

    public void write() {
        this.file.write(this.mediaAbsPath + "/seating.txt");
    }

    public void setProperty(String tilesetName, int col, int row, String key, String value) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            SeatingFile.Tileset tileset = this.findTileset(tilesetName);
            if (tileset == null) {
                if (value == null) {
                    return;
                }

                tileset = new SeatingFile.Tileset();
                tileset.name = tilesetName;
                this.file.tilesets.add(tileset);
            }

            SeatingFile.Tile tile = tileset.getOrCreateTile(col, row);
            if (value == null) {
                tile.properties.remove(key.trim());
            } else {
                tile.properties.put(key.trim(), value.trim());
            }
        }
    }

    public String getProperty(String tilesetName, int col, int row, String key) {
        if (StringUtils.isNullOrWhitespace(key)) {
            return null;
        } else {
            SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
            return tile == null ? null : tile.properties.get(key.trim());
        }
    }

    public int addPosition(String tilesetName, int col, int row, String id) {
        SeatingFile.Tile tile = this.getOrCreateTile(tilesetName, col, row);
        SeatingFile.Position position = new SeatingFile.Position();
        position.id = id.trim();
        tile.positions.add(position);
        return tile.positions.size() - 1;
    }

    public void removePosition(String tilesetName, int col, int row, int index) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        tile.positions.remove(index);
    }

    public int getPositionCount(String tilesetName, int col, int row) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        return tile == null ? 0 : tile.positions.size();
    }

    public SeatingFile.Position getPositionByIndex(String tilesetName, int col, int row, int index) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        return tile == null ? null : tile.positions.get(index);
    }

    public String getPositionID(String tilesetName, int col, int row, int index) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        SeatingFile.Position position = tile.positions.get(index);
        return position.id;
    }

    public SeatingFile.Position getPositionWithID(String tilesetName, int col, int row, String id) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            for (SeatingFile.Position position : tile.positions) {
                if (position.id.equalsIgnoreCase(id)) {
                    return position;
                }
            }

            return null;
        }
    }

    public boolean hasPositionWithID(String tilesetName, int col, int row, String id) {
        return this.getPositionWithID(tilesetName, col, row, id) != null;
    }

    public Vector3f getPositionTranslate(String tilesetName, int col, int row, int index) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        SeatingFile.Position position = tile.positions.get(index);
        return position.translate;
    }

    public HashMap<String, String> getPositionProperties(String tilesetName, int col, int row, int index) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            SeatingFile.Position position = tile.positions.get(index);
            return position.properties;
        }
    }

    public String getPositionProperty(String tilesetName, int col, int row, int index, String key) {
        SeatingFile.Tile tile = this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            SeatingFile.Position position = tile.getPositionByIndex(index);
            return position == null ? null : position.properties.get(key);
        }
    }

    SeatingFile.Tileset findTileset(String tilesetName) {
        for (SeatingFile.Tileset tileset : this.file.tilesets) {
            if (tilesetName.equals(tileset.name)) {
                return tileset;
            }
        }

        return null;
    }

    SeatingFile.Tileset getOrCreateTileset(String tilesetName) {
        SeatingFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            tileset = new SeatingFile.Tileset();
            tileset.name = tilesetName;
            this.file.tilesets.add(tileset);
        }

        return tileset;
    }

    SeatingFile.Tile getTile(String tilesetName, int col, int row) {
        SeatingFile.Tileset tileset = this.findTileset(tilesetName);
        return tileset == null ? null : tileset.getTile(col, row);
    }

    SeatingFile.Tile getOrCreateTile(String tilesetName, int col, int row) {
        SeatingFile.Tileset tileset = this.getOrCreateTileset(tilesetName);
        return tileset.getOrCreateTile(col, row);
    }

    void mergeTilesets(SeatingData other) {
        for (SeatingFile.Tileset tileset2 : other.file.tilesets) {
            SeatingFile.Tileset tileset1 = this.getOrCreateTileset(tileset2.name);
            tileset1.merge(tileset2);
        }
    }

    public void Reset() {
        if (this.file != null) {
            this.file.Reset();
            this.file = null;
        }
    }

    public void fixDefaultPositions() {
        for (SeatingFile.Tileset tileset : this.file.tilesets) {
            for (SeatingFile.Tile tile : tileset.tiles) {
                for (SeatingFile.Position position : tile.positions) {
                    if ("default".equalsIgnoreCase(position.id)) {
                        String spriteName = String.format("%s_%d", tileset.name, tile.col + tile.row * 8);
                        IsoSprite sprite = IsoSpriteManager.instance.namedMap.get(spriteName);
                        if (sprite != null) {
                            String Facing = sprite.getProperties().get("Facing");
                            if (Facing != null) {
                                position.id = Facing;
                            }
                        }
                    }
                }
            }
        }
    }
}
