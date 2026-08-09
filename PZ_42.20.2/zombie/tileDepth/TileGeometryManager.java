// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.io.File;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

@UsedFromLua
public final class TileGeometryManager {
    private static TileGeometryManager instance;
    public static final boolean ONE_PIXEL_OFFSET = false;
    private final ArrayList<TileGeometryManager.ModData> modData = new ArrayList<>();

    public static TileGeometryManager getInstance() {
        if (instance == null) {
            instance = new TileGeometryManager();
        }

        return instance;
    }

    private TileGeometryManager() {
    }

    public void init() {
        this.initGameData();

        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.mediaFile.common.absoluteFile, "tileGeometry.txt");
                if (file.exists()) {
                    this.initModData(mod);
                }
            }
        }
    }

    public void initGameData() {
        TileGeometryManager.ModData data = new TileGeometryManager.ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
        data.geometry.init();
        this.modData.add(data);
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        TileGeometryManager.ModData data = new TileGeometryManager.ModData(mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath());
        data.geometry.init();
        this.modData.add(data);
    }

    public void loadedTileDefinitions() {
        this.initSpriteProperties();
    }

    public void initSpriteProperties() {
        for (IsoSprite sprite : IsoSpriteManager.instance.namedMap.values()) {
            sprite.depthFlags = 0;
            sprite.clearCurtainOffset();
        }

        for (int i = 0; i < this.modData.size(); i++) {
            TileGeometryManager.ModData data = this.modData.get(i);
            data.geometry.initSpriteProperties();
        }
    }

    public ArrayList<String> getModIDs() {
        ArrayList<String> modIDs = new ArrayList<>();

        for (int i = 0; i < this.modData.size(); i++) {
            TileGeometryManager.ModData data = this.modData.get(i);
            modIDs.add(data.modId);
        }

        return modIDs;
    }

    TileGeometryManager.ModData getModData(String modID) {
        for (int i = 0; i < this.modData.size(); i++) {
            TileGeometryManager.ModData data = this.modData.get(i);
            if (StringUtils.equals(modID, data.modId)) {
                return data;
            }
        }

        return null;
    }

    public void setGeometry(String modID, String tilesetName, int col, int row, ArrayList<TileGeometryFile.Geometry> geometry) {
        this.getModData(modID).geometry.setGeometry(tilesetName, col, row, geometry);
    }

    public void copyGeometry(String modID, String tilesetName, int col, int row, ArrayList<TileGeometryFile.Geometry> geometries) {
        this.getModData(modID).geometry.copyGeometry(tilesetName, col, row, geometries);
    }

    public ArrayList<TileGeometryFile.Geometry> getGeometry(String modID, String tilesetName, int col, int row) {
        return this.getModData(modID).geometry.getGeometry(tilesetName, col, row);
    }

    public String getTileProperty(String modID, String tilesetName, int col, int row, String key) {
        return this.getModData(modID).geometry.getProperty(tilesetName, col, row, key);
    }

    public void setTileProperty(String modID, String tilesetName, int col, int row, String key, String value) {
        this.getModData(modID).geometry.setProperty(tilesetName, col, row, key, value);
    }

    public TileGeometryFile.Tile getTile(String modID, String tilesetName, int col, int row) {
        return this.getModData(modID).geometry.getTile(tilesetName, col, row);
    }

    public TileGeometryFile.Tile getOrCreateTile(String modID, String tilesetName, int col, int row) {
        return this.getModData(modID).geometry.getOrCreateTile(tilesetName, col, row);
    }

    public void write(String modID) {
        this.getModData(modID).geometry.write();
    }

    public void Reset() {
        for (TileGeometryManager.ModData data : this.modData) {
            data.geometry.Reset();
        }

        this.modData.clear();
    }

    static final class ModData {
        final String modId;
        final String mediaAbsPath;
        final TileGeometry geometry;

        ModData(String modID, String mediaAbsPath) {
            this.modId = modID;
            this.mediaAbsPath = mediaAbsPath;
            this.geometry = new TileGeometry(mediaAbsPath);
        }
    }
}
