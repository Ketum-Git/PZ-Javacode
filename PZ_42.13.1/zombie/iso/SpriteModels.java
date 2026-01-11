// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.core.math.PZMath;
import zombie.scripting.ScriptManager;

public final class SpriteModels {
    private SpriteModelsFile file;
    private final String mediaAbsPath;

    public SpriteModels(String mediaAbsPath) {
        this.mediaAbsPath = mediaAbsPath;
    }

    public void init() {
        this.file = new SpriteModelsFile();
        this.file.read(this.mediaAbsPath + "/spriteModels.txt");
        this.fromScriptManager();
        this.toScriptManager();
    }

    public void write() {
        this.file.write(this.mediaAbsPath + "/spriteModels.txt");
    }

    public void setTileProperties(String tilesetName, int col, int row, SpriteModel spriteModel) {
        SpriteModelsFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            tileset = new SpriteModelsFile.Tileset();
            tileset.name = tilesetName;
            this.file.tilesets.add(tileset);
        }

        SpriteModelsFile.Tile tile = tileset.getOrCreateTile(col, row);
        tile.spriteModel.set(spriteModel);
    }

    public SpriteModel getTileProperties(String tilesetName, int col, int row) {
        SpriteModelsFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset == null) {
            return null;
        } else {
            SpriteModelsFile.Tile tile = tileset.getTile(col, row);
            return tile == null ? null : tile.spriteModel;
        }
    }

    public void clearTileProperties(String tilesetName, int col, int row) {
        SpriteModelsFile.Tileset tileset = this.findTileset(tilesetName);
        if (tileset != null) {
            SpriteModelsFile.Tile tile = tileset.getTile(col, row);
            if (tile != null) {
                tileset.tiles.remove(tile);
            }
        }
    }

    public SpriteModelsFile.Tileset findTileset(String tilesetName) {
        for (SpriteModelsFile.Tileset tileset : this.file.tilesets) {
            if (tilesetName.equals(tileset.name)) {
                return tileset;
            }
        }

        return null;
    }

    void fromScriptManager() {
        for (SpriteModel spriteModel : ScriptManager.instance.getAllSpriteModels()) {
            String scriptObjectName = spriteModel.getScriptObjectName();
            int p = scriptObjectName.lastIndexOf(95);
            if (p != -1) {
                String tilesetName = scriptObjectName.substring(0, p);
                int tileIndex = PZMath.tryParseInt(scriptObjectName.substring(p + 1), -1);
                if (tileIndex >= 0) {
                    this.setTileProperties(tilesetName, tileIndex % 8, tileIndex / 8, spriteModel);
                }
            }
        }
    }

    public void toScriptManager() {
        for (SpriteModelsFile.Tileset tileset : this.file.tilesets) {
            for (SpriteModelsFile.Tile tile : tileset.tiles) {
                String tileName = String.format("%s_%d", tileset.name, tile.getIndex());
                SpriteModel spriteModel = ScriptManager.instance.getSpriteModel(tileName);
                if (spriteModel == null) {
                    spriteModel = new SpriteModel();
                    spriteModel.set(tile.spriteModel);
                    spriteModel.setModule(ScriptManager.instance.getModule("Base"));
                    spriteModel.InitLoadPP(tileName);
                    ScriptManager.instance.addSpriteModel(spriteModel);
                } else {
                    spriteModel.set(tile.spriteModel);
                }
            }
        }
    }

    public void initSprites() {
        for (SpriteModelsFile.Tileset tileset : this.file.tilesets) {
            tileset.initSprites();
        }
    }

    public void Reset() {
        if (this.file != null) {
            this.file.Reset();
            this.file = null;
        }
    }
}
