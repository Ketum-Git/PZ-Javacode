// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.seams;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.util.StringUtils;

public final class SeamData {
    private SeamFile file;
    private final String mediaAbsPath;

    public SeamData(String mediaAbsPath) {
        this.mediaAbsPath = mediaAbsPath;
    }

    public void init() {
        this.file = new SeamFile();
        this.file.read(this.mediaAbsPath + "/seams.txt");
    }

    public void write() {
        this.file.write(this.mediaAbsPath + "/seams.txt");
    }

    public void setProperty(String tilesetName, int col, int row, String key, String value) {
        if (!StringUtils.isNullOrWhitespace(key)) {
            SeamFile.Tileset tileset = this.findTileset(tilesetName);
            if (tileset == null) {
                if (value == null) {
                    return;
                }

                tileset = new SeamFile.Tileset();
                tileset.name = tilesetName;
                this.file.tilesets.add(tileset);
            }

            SeamFile.Tile tile = tileset.getOrCreateTile(col, row);
            if (tile != null) {
                if (tile.properties == null) {
                    if (value == null) {
                        return;
                    }

                    tile.properties = new HashMap<>();
                }

                if (value == null) {
                    tile.properties.remove(key.trim());
                } else {
                    tile.properties.put(key.trim(), value.trim());
                }
            }
        }
    }

    public String getProperty(String tilesetName, int col, int row, String key) {
        if (StringUtils.isNullOrWhitespace(key)) {
            return null;
        } else {
            SeamFile.Tileset tileset = this.findTileset(tilesetName);
            if (tileset == null) {
                return null;
            } else {
                SeamFile.Tile tile = tileset.getTile(col, row);
                if (tile == null) {
                    return null;
                } else {
                    return tile.properties == null ? null : tile.properties.get(key.trim());
                }
            }
        }
    }

    public ArrayList<String> getTileJoinE(String tilesetName, int col, int row, boolean bAllocate) {
        SeamFile.Tile tile = bAllocate ? this.getOrCreateTile(tilesetName, col, row) : this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            if (bAllocate && tile.joinE == null) {
                tile.joinE = new ArrayList<>();
            }

            return tile.joinE;
        }
    }

    public ArrayList<String> getTileJoinS(String tilesetName, int col, int row, boolean bAllocate) {
        SeamFile.Tile tile = bAllocate ? this.getOrCreateTile(tilesetName, col, row) : this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            if (bAllocate && tile.joinS == null) {
                tile.joinS = new ArrayList<>();
            }

            return tile.joinS;
        }
    }

    public ArrayList<String> getTileJoinBelowE(String tilesetName, int col, int row, boolean bAllocate) {
        SeamFile.Tile tile = bAllocate ? this.getOrCreateTile(tilesetName, col, row) : this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            if (bAllocate && tile.joinBelowE == null) {
                tile.joinBelowE = new ArrayList<>();
            }

            return tile.joinBelowE;
        }
    }

    public ArrayList<String> getTileJoinBelowS(String tilesetName, int col, int row, boolean bAllocate) {
        SeamFile.Tile tile = bAllocate ? this.getOrCreateTile(tilesetName, col, row) : this.getTile(tilesetName, col, row);
        if (tile == null) {
            return null;
        } else {
            if (bAllocate && tile.joinBelowS == null) {
                tile.joinBelowS = new ArrayList<>();
            }

            return tile.joinBelowS;
        }
    }

    SeamFile.Tileset findTileset(String tilesetName) {
        for (SeamFile.Tileset tileset : this.file.tilesets) {
            if (tilesetName.equals(tileset.name)) {
                return tileset;
            }
        }

        return null;
    }

    SeamFile.Tile getTile(String tilesetName, int col, int row) {
        SeamFile.Tileset tileset = this.findTileset(tilesetName);
        return tileset == null ? null : tileset.getTile(col, row);
    }

    SeamFile.Tile getOrCreateTile(String tilesetName, int col, int row) {
        SeamFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            tileset = new SeamFile.Tileset();
            tileset.name = tilesetName;
            this.file.tilesets.add(tileset);
        }

        return tileset.getOrCreateTile(col, row);
    }

    public void Reset() {
        this.file.Reset();
        this.file = null;
    }
}
