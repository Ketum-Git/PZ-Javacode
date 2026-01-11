// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.seams;

import java.io.File;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.gameStates.ChooseGameInfo;
import zombie.util.StringUtils;

@UsedFromLua
public final class SeamManager {
    private static SeamManager instance;
    private final ArrayList<SeamManager.ModData> modData = new ArrayList<>();

    public static SeamManager getInstance() {
        if (instance == null) {
            instance = new SeamManager();
        }

        return instance;
    }

    private SeamManager() {
    }

    public void init() {
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.mediaFile.common.absoluteFile, "seams.txt");
                if (file.exists()) {
                    this.initModData(mod);
                }
            }
        }

        this.initGameData();
    }

    public void initGameData() {
        SeamManager.ModData data = new SeamManager.ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
        data.data.init();
        this.modData.add(data);
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        SeamManager.ModData data = new SeamManager.ModData(mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath());
        data.data.init();
        this.modData.add(data);
    }

    public ArrayList<String> getModIDs() {
        ArrayList<String> modIDs = new ArrayList<>();

        for (int i = 0; i < this.modData.size(); i++) {
            SeamManager.ModData data = this.modData.get(i);
            modIDs.add(data.modId);
        }

        return modIDs;
    }

    SeamManager.ModData getModData(String modID) {
        for (int i = 0; i < this.modData.size(); i++) {
            SeamManager.ModData data = this.modData.get(i);
            if (StringUtils.equals(modID, data.modId)) {
                return data;
            }
        }

        return null;
    }

    public SeamFile.Tile getHighestPriorityTile(String tilesetName, int col, int row) {
        for (int i = 0; i < this.modData.size(); i++) {
            SeamManager.ModData data = this.modData.get(i);
            SeamFile.Tile otherTile = data.data.getTile(tilesetName, col, row);
            if (otherTile != null) {
                return otherTile;
            }
        }

        return null;
    }

    public SeamFile.Tile getHighestPriorityTileFromName(String tileName) {
        int p = tileName.lastIndexOf("_");
        String tilesetName = tileName.substring(0, p);
        int tileIndex = PZMath.tryParseInt(tileName.substring(p + 1), -1);
        return tileIndex < 0 ? null : this.getHighestPriorityTile(tilesetName, tileIndex % 8, tileIndex / 8);
    }

    public String getTileProperty(String modID, String tilesetName, int col, int row, String key) {
        return this.getModData(modID).data.getProperty(tilesetName, col, row, key);
    }

    public void setTileProperty(String modID, String tilesetName, int col, int row, String key, String value) {
        this.getModData(modID).data.setProperty(tilesetName, col, row, key, value);
    }

    public ArrayList<String> getTileJoinE(String modID, String tilesetName, int col, int row, boolean bAllocate) {
        return this.getModData(modID).data.getTileJoinE(tilesetName, col, row, bAllocate);
    }

    public ArrayList<String> getTileJoinS(String modID, String tilesetName, int col, int row, boolean bAllocate) {
        return this.getModData(modID).data.getTileJoinS(tilesetName, col, row, bAllocate);
    }

    public ArrayList<String> getTileJoinBelowE(String modID, String tilesetName, int col, int row, boolean bAllocate) {
        return this.getModData(modID).data.getTileJoinBelowE(tilesetName, col, row, bAllocate);
    }

    public ArrayList<String> getTileJoinBelowS(String modID, String tilesetName, int col, int row, boolean bAllocate) {
        return this.getModData(modID).data.getTileJoinBelowS(tilesetName, col, row, bAllocate);
    }

    public SeamFile.Tile getTile(String modID, String tilesetName, int col, int row) {
        return this.getModData(modID).data.getTile(tilesetName, col, row);
    }

    public SeamFile.Tile getTileFromName(String modID, String tileName) {
        int p = tileName.lastIndexOf("_");
        String tilesetName = tileName.substring(0, p);
        int tileIndex = PZMath.tryParseInt(tileName.substring(p + 1), -1);
        return tileIndex < 0 ? null : this.getModData(modID).data.getTile(tilesetName, tileIndex % 8, tileIndex / 8);
    }

    public SeamFile.Tile getOrCreateTile(String modID, String tilesetName, int col, int row) {
        return this.getModData(modID).data.getOrCreateTile(tilesetName, col, row);
    }

    public boolean isMasterTile(String modID, String tilesetName, int col, int row) {
        SeamFile.Tile tile = this.getTile(modID, tilesetName, col, row);
        return tile != null && tile.isMasterTile();
    }

    public String getMasterTileName(String modID, String tilesetName, int col, int row) {
        SeamFile.Tile tile = this.getTile(modID, tilesetName, col, row);
        return tile == null ? null : tile.getMasterTileName();
    }

    public void write(String modID) {
        this.getModData(modID).data.write();
    }

    public void Reset() {
        for (SeamManager.ModData data : this.modData) {
            data.data.Reset();
        }

        this.modData.clear();
    }

    static final class ModData {
        final String modId;
        final String mediaAbsPath;
        final SeamData data;

        ModData(String modID, String mediaAbsPath) {
            this.modId = modID;
            this.mediaAbsPath = mediaAbsPath;
            this.data = new SeamData(mediaAbsPath);
        }
    }
}
