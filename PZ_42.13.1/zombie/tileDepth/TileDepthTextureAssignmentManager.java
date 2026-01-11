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
public final class TileDepthTextureAssignmentManager {
    private static TileDepthTextureAssignmentManager instance;
    private final ArrayList<TileDepthTextureAssignmentManager.ModData> modData = new ArrayList<>();

    public static TileDepthTextureAssignmentManager getInstance() {
        if (instance == null) {
            instance = new TileDepthTextureAssignmentManager();
        }

        return instance;
    }

    private TileDepthTextureAssignmentManager() {
    }

    public void init() {
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.mediaFile.common.absoluteFile, "tileDepthTextureAssignments.txt");
                if (file.exists()) {
                    this.initModData(mod);
                }
            }
        }

        this.initGameData();
    }

    public void initGameData() {
        TileDepthTextureAssignmentManager.ModData data = new TileDepthTextureAssignmentManager.ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
        data.assignments.load();
        this.modData.add(data);
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        TileDepthTextureAssignmentManager.ModData data = new TileDepthTextureAssignmentManager.ModData(
            mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath()
        );
        data.assignments.load();
        this.modData.add(data);
    }

    public void save(String modID) {
        this.getModData(modID).assignments.save();
    }

    TileDepthTextureAssignmentManager.ModData getModData(String modID) {
        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureAssignmentManager.ModData data = this.modData.get(i);
            if (StringUtils.equals(modID, data.modId)) {
                return data;
            }
        }

        return null;
    }

    public void initSprites() {
        for (IsoSprite sprite : IsoSpriteManager.instance.namedMap.values()) {
            if (sprite.depthTexture == null && sprite.tilesetName != null) {
                String otherTile = this.getAssignedTileName(sprite.name);
                if (otherTile != null) {
                    TileDepthTexture depthTexture = TileDepthTextureManager.getInstance().getTextureFromTileName(otherTile);
                    if (depthTexture != null && !depthTexture.isEmpty()) {
                        sprite.depthTexture = depthTexture;
                    }
                }
            }
        }
    }

    public void assignTileName(String modID, String assignTo, String otherTile) {
        this.getModData(modID).assignments.assignTileName(assignTo, otherTile);
    }

    public String getAssignedTileName(String modID, String tileName) {
        return this.getModData(modID).assignments.getAssignedTileName(tileName);
    }

    public void clearAssignedTileName(String modID, String assignTo) {
        this.getModData(modID).assignments.clearAssignedTileName(assignTo);
    }

    public void assignDepthTextureToSprite(String modID, String tileName) {
        this.getModData(modID).assignments.assignDepthTextureToSprite(tileName);
    }

    String getAssignedTileName(String tileName) {
        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureAssignmentManager.ModData data = this.modData.get(i);
            String otherTile = data.assignments.getAssignedTileName(tileName);
            if (otherTile != null) {
                return otherTile;
            }
        }

        return null;
    }

    static final class ModData {
        final String modId;
        final String mediaAbsPath;
        final TileDepthTextureAssignments assignments;

        ModData(String modID, String mediaAbsPath) {
            this.modId = modID;
            this.mediaAbsPath = mediaAbsPath;
            this.assignments = new TileDepthTextureAssignments(mediaAbsPath);
        }
    }
}
