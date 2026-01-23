// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.util.HashMap;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.fileSystem.FileSystem;
import zombie.fileSystem.FileTask;

@UsedFromLua
public final class TileDepthTextures {
    final String modId;
    final String mediaAbsPath;
    private final HashMap<String, Integer> tilesetRows = new HashMap<>();
    private final HashMap<String, TilesetDepthTexture> tilesets = new HashMap<>();
    private final HashSet<String> nullTilesets = new HashSet<>();

    public TileDepthTextures(String modID, String mediaAbsPath) {
        this.modId = modID;
        this.mediaAbsPath = mediaAbsPath;
    }

    public void saveTileset(String tilesetName) throws Exception {
        TilesetDepthTexture tileset = this.tilesets.get(tilesetName);
        if (tileset != null) {
            tileset.save();
        }
    }

    public TileDepthTexture getTexture(String tilesetName, int tileIndex) {
        TilesetDepthTexture tileset = this.tilesets.get(tilesetName);
        if (tileset == null) {
            tileset = this.createTileset(tilesetName, false);
            if (tileset == null) {
                this.nullTilesets.add(tilesetName);
                return null;
            }
        }

        return tileset.getOrCreateTile(tileIndex);
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
                    TilesetDepthTexture tileset = this.tilesets.get(tilesetName);
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

    private TilesetDepthTexture createTileset(String tilesetName, boolean bUseCachedValue) {
        int numColumns = 8;
        int numRows = this.getTilesetRows(tilesetName, bUseCachedValue);
        if (numRows == 0) {
            return null;
        } else {
            TilesetDepthTexture tileset = new TilesetDepthTexture(this, tilesetName, 8, numRows, true);
            if (tileset.fileExists()) {
                try {
                    tileset.load();
                } catch (Exception var7) {
                    ExceptionLogger.logException(var7);
                }
            }

            this.tilesets.put(tilesetName, tileset);
            return tileset;
        }
    }

    public TilesetDepthTexture getExistingTileset(String tilesetName) {
        return this.tilesets.get(tilesetName);
    }

    private int getTilesetRows(String tilesetName, boolean bUseCachedValue) {
        if (bUseCachedValue) {
            return this.tilesetRows.getOrDefault(tilesetName, 0);
        } else {
            int numColumns = 8;

            for (int row = 63; row >= 0; row--) {
                for (int col = 0; col < 8; col++) {
                    int index = col + row * 8;
                    Texture texture = Texture.getSharedTexture(tilesetName + "_" + index);
                    if (texture != null) {
                        return row + 1;
                    }
                }
            }

            return 0;
        }
    }

    public void loadDepthTextureImages() {
        Path dir = FileSystems.getDefault().getPath(this.mediaAbsPath, "depthmaps");
        if (Files.exists(dir)) {
            Filter<Path> filter = entry -> Files.isRegularFile(entry) && entry.toString().endsWith(".png");

            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, filter)) {
                for (Path path : dstrm) {
                    String fileName = path.toFile().getName();
                    if (fileName.startsWith("DEPTH_")
                        && fileName.endsWith(".png")
                        && !"DEPTH_whole_tile.png".equalsIgnoreCase(fileName)
                        && !"DEPTH_preset_depthmaps_01.png".equalsIgnoreCase(fileName)) {
                        String tilesetName = fileName.substring(6, fileName.length() - 4);
                        this.tilesetRows.put(tilesetName, this.getTilesetRows(tilesetName, false));
                        TileDepthTextureManager.getInstance().addedLoadTask();
                        FileSystem fileSystem = GameWindow.fileSystem;
                        TileDepthTextures.LoadTask loadTask = new TileDepthTextures.LoadTask(this, path, fileSystem);
                        loadTask.setPriority(4);
                        fileSystem.runAsync(loadTask);
                    }
                }
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
            }
        }
    }

    protected void hackAddPresetTilesetDepthTexture() {
        TilesetDepthTexture depthTexture = TileDepthTextureManager.getInstance().getPresetTilesetDepthTexture();
        if (depthTexture != null) {
            this.tilesets.put(depthTexture.getName(), depthTexture);
        }
    }

    public void mergeTilesets(TileDepthTextures other) {
        for (TilesetDepthTexture tileset : other.tilesets.values()) {
            this.mergeTileset(tileset);
        }
    }

    public void mergeTileset(TilesetDepthTexture other) {
        TilesetDepthTexture mergedTileset = this.tilesets.get(other.getName());
        if (mergedTileset == null) {
            mergedTileset = new TilesetDepthTexture(
                this, other.getName(), other.getWidth() / other.getTileWidth(), other.getHeight() / other.getTileHeight(), other.is2x()
            );
            this.tilesets.put(mergedTileset.getName(), mergedTileset);
        }

        mergedTileset.mergeTileset(other);
    }

    public void initSprites() {
        for (TilesetDepthTexture tileset : this.tilesets.values()) {
            tileset.initSprites();
        }
    }

    public void initSprites(String tilesetName) {
        TilesetDepthTexture tileset = this.tilesets.get(tilesetName);
        if (tileset != null) {
            tileset.initSprites();
        }
    }

    public void Reset() {
        for (TilesetDepthTexture tileset : this.tilesets.values()) {
            tileset.Reset();
        }

        this.tilesets.clear();
        this.nullTilesets.clear();
    }

    static final class LoadTask extends FileTask {
        final TileDepthTextures textures;
        final Path path;

        public LoadTask(TileDepthTextures textures, Path path, FileSystem fileSystem) {
            super(fileSystem);
            this.textures = textures;
            this.path = path;
        }

        @Override
        public void done() {
            TileDepthTextureManager.getInstance().finishedLoadTask();
        }

        @Override
        public Object call() throws Exception {
            String fileName = this.path.toFile().getName();
            String tilesetName = fileName.replaceFirst("DEPTH_", "").replace(".png", "");
            synchronized (this.textures) {
                TilesetDepthTexture tileset = this.textures.tilesets.get(tilesetName);
                if (tileset == null) {
                    tileset = this.textures.createTileset(tilesetName, true);
                }

                return null;
            }
        }
    }
}
