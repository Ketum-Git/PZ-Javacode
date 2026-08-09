// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.util.StringUtils;

@UsedFromLua
public final class TileDepthTextureManager {
    private static TileDepthTextureManager instance;
    public static final boolean DELAYED_LOADING = true;
    private int remainingLoadTasks;
    private boolean loadedTileDefinitions;
    private final ArrayList<TileDepthTextureManager.ModData> modData = new ArrayList<>();
    private final ArrayList<TileDepthTextureManager.ModData> previouslyLoadedModData = new ArrayList<>();
    private TilesetDepthTexture defaultDepthTextureTileset;
    private TilesetDepthTexture billboardDepthTextureTileset;
    private TilesetDepthTexture presetDepthTextureTileset;
    private TileDepthTextures mergedTilesets;
    private final HashSet<String> nullTilesets = new HashSet<>();
    private final ArrayList<Texture> emptyDepthTextures = new ArrayList<>();

    public static TileDepthTextureManager getInstance() {
        if (instance == null) {
            instance = new TileDepthTextureManager();
        }

        return instance;
    }

    private TileDepthTextureManager() {
    }

    public void init() {
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                File file = new File(mod.mediaFile.common.absoluteFile, "tileGeometry.txt");
                if (file.exists()) {
                    this.initModData(mod);
                }
            }
        }

        this.initGameData();
    }

    public void initGameData() {
        TileDepthTextureManager.ModData data = this.getModData("game", this.previouslyLoadedModData);
        if (data == null) {
            data = new TileDepthTextureManager.ModData("game", ZomboidFileSystem.instance.getMediaRootPath());
            data.textures.loadDepthTextureImages();
            this.modData.add(data);
            data.textures.hackAddPresetTilesetDepthTexture();
        } else {
            this.modData.add(data);
        }
    }

    public void initModData(ChooseGameInfo.Mod mod) {
        TileDepthTextureManager.ModData data = this.getModData(mod.getId(), this.previouslyLoadedModData);
        if (data == null) {
            data = new TileDepthTextureManager.ModData(mod.getId(), mod.mediaFile.common.absoluteFile.getAbsolutePath());
            data.textures.loadDepthTextureImages();
        }

        this.modData.add(data);
    }

    private void initMergedTilesets() {
        this.mergedTilesets = new TileDepthTextures(null, null);

        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureManager.ModData data = this.modData.get(i);
            this.mergedTilesets.mergeTilesets(data.textures);
        }
    }

    public void mergeAfterEditing(String tilesetName) {
        TilesetDepthTexture tilesetMerged = this.mergedTilesets.getExistingTileset(tilesetName);
        if (tilesetMerged != null) {
            tilesetMerged.clearTiles();
        }

        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureManager.ModData data = this.modData.get(i);
            TilesetDepthTexture tilesetOther = data.textures.getExistingTileset(tilesetName);
            if (tilesetOther != null) {
                this.mergedTilesets.mergeTileset(tilesetOther);
            }
        }
    }

    public void reloadTileset(String modID, String tilesetName) throws Exception {
        TilesetDepthTexture tileset = this.getModData(modID).textures.getExistingTileset(tilesetName);
        if (tileset != null) {
            tileset.reload();
        }
    }

    TileDepthTextureManager.ModData getModData(String modID) {
        return this.getModData(modID, this.modData);
    }

    TileDepthTextureManager.ModData getModData(String modID, ArrayList<TileDepthTextureManager.ModData> modData) {
        for (int i = 0; i < modData.size(); i++) {
            TileDepthTextureManager.ModData data = modData.get(i);
            if (StringUtils.equals(modID, data.modId)) {
                return data;
            }
        }

        return null;
    }

    public void loadTilesetPixelsIfNeeded(String modID, String tilesetName) {
        TilesetDepthTexture tileset = this.getModData(modID).textures.getExistingTileset(tilesetName);
        if (tileset != null) {
            if (!tileset.isKeepPixels()) {
                tileset.setKeepPixels(true);

                try {
                    tileset.reload();
                } catch (Exception var5) {
                    ExceptionLogger.logException(var5);
                }
            }
        }
    }

    public void saveTileset(String modID, String tilesetName) throws Exception {
        TileDepthTextureManager.ModData modData = this.getModData(modID);
        TilesetDepthTexture tileset = modData.textures.getExistingTileset(tilesetName);
        if (tileset != null) {
            if (tileset.isKeepPixels()) {
                modData.textures.saveTileset(tilesetName);
            }
        }
    }

    public TileDepthTexture getTexture(String modID, String tilesetName, int tileIndex) {
        return this.getModData(modID).textures.getTexture(tilesetName, tileIndex);
    }

    public TileDepthTexture getTextureFromTileName(String modID, String tileName) {
        return this.getModData(modID).textures.getTextureFromTileName(tileName);
    }

    public TileDepthTexture getTexture(String tilesetName, int tileIndex) {
        if (this.nullTilesets.contains(tilesetName)) {
            return null;
        } else {
            TilesetDepthTexture tileset = this.mergedTilesets.getExistingTileset(tilesetName);
            if (tileset == null) {
                this.nullTilesets.add(tilesetName);
                return null;
            } else {
                return tileset.getOrCreateTile(tileIndex);
            }
        }
    }

    public TileDepthTexture getTextureFromTileName(String tileName) {
        int p = tileName.lastIndexOf(95);
        if (p == -1) {
            return null;
        } else {
            String tilesetName = tileName.substring(0, p);
            if (this.nullTilesets.contains(tilesetName)) {
                return null;
            } else {
                int tileIndex = PZMath.tryParseInt(tileName.substring(p + 1), -1);
                if (tileIndex == -1) {
                    return null;
                } else {
                    TilesetDepthTexture tileset = this.mergedTilesets.getExistingTileset(tilesetName);
                    if (tileset == null) {
                        this.nullTilesets.add(tilesetName);
                        return null;
                    } else {
                        return tileset.getOrCreateTile(tileIndex);
                    }
                }
            }
        }
    }

    private void initDefaultDepthTexture() {
        if (this.defaultDepthTextureTileset == null) {
            TilesetDepthTexture tileset = new TilesetDepthTexture(this.getModData("game").textures, "whole_tile", 1, 1, true);
            if (tileset.fileExists()) {
                try {
                    tileset.load();
                } catch (Exception var3) {
                    ExceptionLogger.logException(var3);
                }
            }

            this.defaultDepthTextureTileset = tileset;
        }
    }

    public TileDepthTexture getDefaultDepthTexture() {
        this.initDefaultDepthTexture();
        return this.defaultDepthTextureTileset == null ? null : this.defaultDepthTextureTileset.getOrCreateTile(0, 0);
    }

    private void initBillboardDepthTexture() {
        if (this.billboardDepthTextureTileset == null) {
            TilesetDepthTexture tileset = new TilesetDepthTexture(this.getModData("game").textures, "billboard", 1, 1, true);
            if (tileset.fileExists()) {
                try {
                    tileset.load();
                } catch (Exception var3) {
                    ExceptionLogger.logException(var3);
                }
            }

            this.billboardDepthTextureTileset = tileset;
        }
    }

    public TileDepthTexture getBillboardDepthTexture() {
        this.initBillboardDepthTexture();
        return this.billboardDepthTextureTileset == null ? null : this.billboardDepthTextureTileset.getOrCreateTile(0, 0);
    }

    private void initPresetDepthTexture() {
        if (this.presetDepthTextureTileset == null) {
            TilesetDepthTexture tileset = new TilesetDepthTexture(this.getModData("game").textures, "preset_depthmaps_01", 8, 1, true);
            if (tileset.fileExists()) {
                try {
                    tileset.load();
                } catch (Exception var3) {
                    ExceptionLogger.logException(var3);
                }
            }

            this.presetDepthTextureTileset = tileset;
        }
    }

    public TilesetDepthTexture getPresetTilesetDepthTexture() {
        this.initPresetDepthTexture();
        return this.presetDepthTextureTileset;
    }

    public TileDepthTexture getPresetDepthTexture(int col, int row) {
        this.initPresetDepthTexture();
        return this.presetDepthTextureTileset == null ? null : this.presetDepthTextureTileset.getOrCreateTile(col, row);
    }

    public void initSprites() {
        for (IsoSprite sprite : IsoSpriteManager.instance.namedMap.values()) {
            sprite.depthTexture = null;
        }

        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureManager.ModData data = this.modData.get(i);
            data.textures.initSprites();
        }
    }

    public void initSprites(String tilesetName) {
        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureManager.ModData data = this.modData.get(i);
            data.textures.initSprites(tilesetName);
        }
    }

    public void Reset() {
        for (int i = 0; i < this.modData.size(); i++) {
            TileDepthTextureManager.ModData data = this.modData.get(i);
            if (!this.previouslyLoadedModData.contains(data)) {
                this.previouslyLoadedModData.add(data);
            }
        }

        this.modData.clear();
        this.mergedTilesets = null;
        this.nullTilesets.clear();
        this.loadedTileDefinitions = false;
    }

    public void addedLoadTask() {
        this.remainingLoadTasks++;
    }

    public void finishedLoadTask() {
        this.remainingLoadTasks--;
        if (this.remainingLoadTasks == 0) {
            this.initMergedTilesets();
            if (this.loadedTileDefinitions) {
                this.initSprites();
                TileDepthTextureAssignmentManager.getInstance().initSprites();
            }
        }
    }

    public void loadedTileDefinitions() {
        this.loadedTileDefinitions = true;
        if (this.remainingLoadTasks <= 0) {
            if (this.mergedTilesets == null) {
                this.initMergedTilesets();
            }

            this.initSprites();
            TileDepthTextureAssignmentManager.getInstance().initSprites();
        }
    }

    public boolean isLoadingFinished() {
        return this.loadedTileDefinitions && this.mergedTilesets != null;
    }

    public Texture getEmptyDepthTexture(int width, int height) {
        for (int i = 0; i < this.emptyDepthTextures.size(); i++) {
            Texture texture = this.emptyDepthTextures.get(i);
            if (texture.getWidth() == width && texture.getHeight() == height) {
                return texture;
            }
        }

        Texture texture = new Texture(width, height, "DEPTH_Empty_%dx%d".formatted(width, height), 0);
        this.emptyDepthTextures.add(texture);
        return texture;
    }

    static final class ModData {
        final String modId;
        final String mediaAbsPath;
        final TileDepthTextures textures;

        ModData(String modID, String mediaAbsPath) {
            this.modId = modID;
            this.mediaAbsPath = mediaAbsPath;
            this.textures = new TileDepthTextures(modID, mediaAbsPath);
        }

        public void Reset() {
            this.textures.Reset();
        }
    }
}
