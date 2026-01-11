// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.IntBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.textures.ImageData;
import zombie.core.textures.MipMapLevel;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureID;
import zombie.core.utils.DirectBufferAllocator;
import zombie.debug.DebugType;

public final class ImagePyramid {
    static final int TILE_SIZE = 256;
    String directory;
    String zipFile;
    FileSystem zipFs;
    final HashMap<String, ImagePyramid.PyramidTexture> textures = new HashMap<>();
    final HashSet<String> missing = new HashSet<>();
    HashMap<String, FileTask_LoadImagePyramidTexture> fileTasks = new HashMap<>();
    int requestNumber;
    int minX;
    int minY;
    int maxX;
    int maxY;
    int imageWidth = -1;
    int imageHeight = -1;
    float resolution = 1.0F;
    int clampS = 33071;
    int clampT = 33071;
    int minFilter = 9729;
    int magFilter = 9728;
    int minZ;
    int maxZ;
    int maxTextures = 100;
    static int maxRequestNumber = Core.debug ? 10000 : Integer.MAX_VALUE;
    int texturesLoadedThisFrame;
    boolean destroyed;
    static final ThreadLocal<TIntObjectHashMap<ImagePyramid.ImageKeyXYZ>> TL_imageKeys = ThreadLocal.withInitial(TIntObjectHashMap::new);
    static final HashSet<String> s_required = new HashSet<>();
    static final int[] s_tilesCoveringCell = new int[4];
    final ConcurrentLinkedQueue<ImagePyramid.FileTaskRequest> queueLoading = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<ImagePyramid.FileTaskResult> queueRender = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<FileTask_LoadImagePyramidTexture> queueCalled = new ConcurrentLinkedQueue<>();
    final ConcurrentLinkedQueue<String> queueCancel = new ConcurrentLinkedQueue<>();

    static String getKey(int x, int y, int z) {
        TIntObjectHashMap<ImagePyramid.ImageKeyXYZ> map = TL_imageKeys.get();
        ImagePyramid.ImageKeyXYZ xyz = map.get(x);
        if (xyz == null) {
            map.put(x, xyz = new ImagePyramid.ImageKeyXYZ());
        }

        return xyz.get(x, y, z);
    }

    public void setDirectory(String directory) {
        if (this.zipFile != null) {
            this.zipFile = null;
            if (this.zipFs != null) {
                try {
                    this.zipFs.close();
                } catch (IOException var3) {
                }

                this.zipFs = null;
            }
        }

        this.directory = directory;
    }

    public void setZipFile(String zipFile) {
        this.directory = null;
        this.zipFile = zipFile;
        this.zipFs = this.openZipFile();
        this.readInfoFile();
        if (this.imageWidth == -1) {
            this.imageWidth = this.maxX - this.minX;
            this.imageHeight = this.maxY - this.minY;
        }

        this.resolution = (float)(this.maxX - this.minX) / this.imageWidth;
        this.minZ = Integer.MAX_VALUE;
        this.maxZ = Integer.MIN_VALUE;
        if (this.zipFs != null) {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(this.zipFs.getPath("/"))) {
                for (Path path : dstrm) {
                    if (Files.isDirectory(path)) {
                        int z = PZMath.tryParseInt(path.getFileName().toString(), -1);
                        this.minZ = PZMath.min(this.minZ, z);
                        this.maxZ = PZMath.max(this.maxZ, z);
                    }
                }
            } catch (IOException var8) {
                ExceptionLogger.logException(var8);
            }
        }
    }

    public boolean isValidTile(int x, int y, int z) {
        if (z < this.minZ || z > this.maxZ) {
            return false;
        } else if (x >= 0 && y >= 0) {
            int scaledDownImageWidth = this.imageWidth / (1 << z);
            int scaledDownImageHeight = this.imageHeight / (1 << z);
            return x <= scaledDownImageWidth / 256 && y <= scaledDownImageHeight / 256;
        } else {
            return false;
        }
    }

    public Texture getImage(int x, int y, int z) {
        String key = getKey(x, y, z);
        if (this.missing.contains(key)) {
            return null;
        } else {
            File file = new File(this.directory, String.format(Locale.ENGLISH, "%s%d%stile%dx%d.png", File.separator, z, File.separator, x, y));
            if (!file.exists()) {
                this.missing.add(key);
                return null;
            } else {
                return Texture.getSharedTexture(file.getAbsolutePath());
            }
        }
    }

    public ImagePyramid.PyramidTexture getTexture(int x, int y, int z) {
        this.checkQueue();
        if (!this.isValidTile(x, y, z)) {
            return null;
        } else {
            String key = getKey(x, y, z);
            if (this.textures.containsKey(key)) {
                ImagePyramid.PyramidTexture pyramidTexture = this.textures.get(key);
                pyramidTexture.requestNumber = this.requestNumber++;
                pyramidTexture.requiredThisFrame = true;
                if (this.requestNumber >= maxRequestNumber) {
                    this.resetRequestNumbers();
                }

                if (!pyramidTexture.isReady()
                    && (pyramidTexture.state == ImagePyramid.TextureState.Init || pyramidTexture.state == ImagePyramid.TextureState.Cancelled)) {
                    Path path = this.zipFs.getPath(String.valueOf(z), String.format(Locale.ENGLISH, "tile%dx%d.png", x, y));
                    this.startLoadingFromZip(pyramidTexture, path);
                }

                return pyramidTexture;
            } else if (this.missing.contains(key)) {
                return null;
            } else if (this.zipFile != null) {
                if (this.zipFs != null && this.zipFs.isOpen()) {
                    try {
                        Path path = this.zipFs.getPath(String.valueOf(z), String.format(Locale.ENGLISH, "tile%dx%d.png", x, y));
                        if (!Files.exists(path)) {
                            this.missing.add(key);
                            return null;
                        } else {
                            ImagePyramid.PyramidTexture pyramidTexturex = this.checkTextureCache(x, y, z, key);
                            if (pyramidTexturex == null) {
                                return null;
                            } else {
                                this.startLoadingFromZip(pyramidTexturex, path);
                                return pyramidTexturex;
                            }
                        }
                    } catch (Exception var7) {
                        this.missing.add(key);
                        ExceptionLogger.logException(var7);
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }

    private void startLoadingFromZip(ImagePyramid.PyramidTexture pyramidTexture, Path path) {
        if (pyramidTexture.state != ImagePyramid.TextureState.Loading) {
            pyramidTexture.state = ImagePyramid.TextureState.Loading;
            ImagePyramid.FileTaskRequest fileTaskRequest = new ImagePyramid.FileTaskRequest(this, pyramidTexture.key, path);
            this.queueLoading.add(fileTaskRequest);
        }
    }

    private void cancelFileTask(FileTask_LoadImagePyramidTexture fileTask) {
        fileTask.cancelled = true;
        if (fileTask.asyncOp == -1) {
            this.fileTasks.remove(fileTask.key);
        } else {
            GameWindow.fileSystem.cancelAsync(fileTask.asyncOp);
            fileTask.asyncOp = -1;
        }
    }

    public void checkCalledQueue() {
        for (FileTask_LoadImagePyramidTexture fileTask = this.queueCalled.poll(); fileTask != null; fileTask = this.queueCalled.poll()) {
            if (fileTask.cancelled) {
                if (fileTask == this.fileTasks.get(fileTask.key)) {
                    this.fileTasks.remove(fileTask.key);
                }

                this.disposeImageData(fileTask.imageData);
                fileTask.imageData = null;
            }
        }
    }

    public void checkCancelQueue() {
        for (String key = this.queueCancel.poll(); key != null; key = this.queueCancel.poll()) {
            FileTask_LoadImagePyramidTexture fileTask = this.fileTasks.get(key);
            if (fileTask != null) {
                this.cancelFileTask(fileTask);
            }
        }
    }

    public void checkLoadingQueue() {
        for (ImagePyramid.FileTaskRequest fileTaskRequest = this.queueLoading.poll(); fileTaskRequest != null; fileTaskRequest = this.queueLoading.poll()) {
            FileTask_LoadImagePyramidTexture old = this.fileTasks.get(fileTaskRequest.key);
            if (old != null) {
                this.cancelFileTask(old);
            }

            FileTask_LoadImagePyramidTexture fileTask = new FileTask_LoadImagePyramidTexture(
                this, fileTaskRequest.path, fileTaskRequest.key, GameWindow.fileSystem, null
            );
            this.fileTasks.put(fileTask.key, fileTask);
            fileTask.asyncOp = GameWindow.fileSystem.runAsync(fileTask);
        }
    }

    public ImagePyramid.PyramidTexture getReadyTexture(int x, int y, int z) {
        this.checkQueue();
        String key = getKey(x, y, z);
        if (this.textures.containsKey(key)) {
            ImagePyramid.PyramidTexture pyramidTexture = this.textures.get(key);
            if (!pyramidTexture.isReady()) {
                return null;
            } else {
                pyramidTexture.requestNumber = this.requestNumber++;
                pyramidTexture.requiredThisFrame = true;
                if (this.requestNumber >= maxRequestNumber) {
                    this.resetRequestNumbers();
                }

                return pyramidTexture;
            }
        } else {
            return null;
        }
    }

    public ImagePyramid.PyramidTexture getLowerResTexture(int x, int y, int z) {
        int div = 2;

        for (int z1 = z + 1; z1 <= this.maxZ; z1++) {
            int x1 = PZMath.fastfloor((float)x / div);
            int y1 = PZMath.fastfloor((float)y / div);
            ImagePyramid.PyramidTexture pyramidTexture = this.getReadyTexture(x1, y1, z1);
            if (pyramidTexture != null && pyramidTexture.isReady()) {
                return pyramidTexture;
            }

            div *= 2;
        }

        div /= 2;
        int x1 = PZMath.fastfloor((float)x / div);
        int y1 = PZMath.fastfloor((float)y / div);
        if (this.isValidTile(x1, y1, this.maxZ)) {
            ImagePyramid.PyramidTexture pyramidTexture = this.getTexture(x1, y1, this.maxZ);
            if (pyramidTexture != null && pyramidTexture.isReady()) {
                return pyramidTexture;
            }
        }

        return null;
    }

    public int getMinFilter() {
        return this.minFilter;
    }

    public int getMagFilter() {
        return this.magFilter;
    }

    public int getClampS() {
        return this.clampS;
    }

    public int getClampT() {
        return this.clampT;
    }

    private void replaceTextureData(ImagePyramid.PyramidTexture pyramidTexture, ImageData imageData) {
        if (GL.getCapabilities().GL_ARB_texture_compression) {
        }

        int internalFormat = 6408;
        GL11.glBindTexture(3553, Texture.lastTextureID = pyramidTexture.textureId.getID());
        SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        GL11.glTexImage2D(3553, 0, internalFormat, imageData.getWidthHW(), imageData.getHeightHW(), 0, 6408, 5121, imageData.getData().getBuffer());
        imageData.dispose();
    }

    public void generateFiles(String imageFile, String outputDirectory) throws Exception {
        ImageData imageData = new ImageData(imageFile);
        if (imageData != null) {
            int tileSize = 256;
            int levels = 5;

            for (int level = 0; level < 5; level++) {
                MipMapLevel mipMapLevel = imageData.getMipMapData(level);
                float width = (float)imageData.getWidth() / (1 << level);
                float height = (float)imageData.getHeight() / (1 << level);
                int columns = (int)Math.ceil(width / 256.0F);
                int rows = (int)Math.ceil(height / 256.0F);

                for (int row = 0; row < rows; row++) {
                    for (int col = 0; col < columns; col++) {
                        BufferedImage bufferedImage = this.getBufferedImage(mipMapLevel, col, row, 256);
                        this.writeImageToFile(bufferedImage, outputDirectory, col, row, level);
                    }
                }
            }
        }
    }

    public FileSystem openZipFile() {
        try {
            return FileSystems.newFileSystem(Paths.get(this.zipFile));
        } catch (IOException var2) {
            ExceptionLogger.logException(var2);
            return null;
        }
    }

    public void generateZip(String imageFile, String zipFile) throws Exception {
        ImageData imageData = new ImageData(imageFile);
        if (imageData != null) {
            int tileSize = 256;

            try (
                FileOutputStream fos = new FileOutputStream(zipFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                ZipOutputStream zos = new ZipOutputStream(bos);
            ) {
                int levels = 5;

                for (int level = 0; level < 5; level++) {
                    MipMapLevel mipMapLevel = imageData.getMipMapData(level);
                    float width = (float)imageData.getWidth() / (1 << level);
                    float height = (float)imageData.getHeight() / (1 << level);
                    int columns = (int)Math.ceil(width / 256.0F);
                    int rows = (int)Math.ceil(height / 256.0F);

                    for (int row = 0; row < rows; row++) {
                        for (int col = 0; col < columns; col++) {
                            BufferedImage bufferedImage = this.getBufferedImage(mipMapLevel, col, row, 256);
                            this.writeImageToZip(bufferedImage, zos, col, row, level);
                        }
                    }

                    if (width <= 256.0F && height <= 256.0F) {
                        break;
                    }
                }
            }
        }
    }

    BufferedImage getBufferedImage(MipMapLevel mipMapLevel, int col, int row, int tileSize) {
        BufferedImage bufferedImage = new BufferedImage(tileSize, tileSize, 2);
        int[] rowABGR = new int[tileSize];
        IntBuffer intBuffer = mipMapLevel.getBuffer().asIntBuffer();

        for (int y = 0; y < tileSize; y++) {
            intBuffer.get(col * tileSize + (row * tileSize + y) * mipMapLevel.width, rowABGR);

            for (int x = 0; x < tileSize; x++) {
                int abgr = rowABGR[x];
                int r = abgr & 0xFF;
                int g = abgr >> 8 & 0xFF;
                int b = abgr >> 16 & 0xFF;
                int a = abgr >> 24 & 0xFF;
                rowABGR[x] = a << 24 | r << 16 | g << 8 | b;
            }

            bufferedImage.setRGB(0, y, tileSize, 1, rowABGR, 0, tileSize);
        }

        return bufferedImage;
    }

    void writeImageToFile(BufferedImage bufferedImage, String outputDirectory, int col, int row, int z) throws Exception {
        File file = new File(outputDirectory + File.separator + z);
        if (file.exists() || file.mkdirs()) {
            file = new File(file, String.format(Locale.ENGLISH, "tile%dx%d.png", col, row));
            ImageIO.write(bufferedImage, "png", file);
        }
    }

    void writeImageToZip(BufferedImage bufferedImage, ZipOutputStream zos, int col, int row, int z) throws Exception {
        zos.putNextEntry(new ZipEntry(String.format(Locale.ENGLISH, "%d/tile%dx%d.png", z, col, row)));
        ImageIO.write(bufferedImage, "PNG", zos);
        zos.closeEntry();
    }

    void startFrame() {
        this.texturesLoadedThisFrame = 0;

        for (ImagePyramid.PyramidTexture pyramidTexture : this.textures.values()) {
            pyramidTexture.requiredThisFrame = false;
        }
    }

    double calculateMetersPerTile(int z) {
        double originalFullResImageWidth = Math.ceil(this.imageWidth / 16.0) * 16.0;
        double fudge = originalFullResImageWidth / this.imageWidth;
        double scaledDownImageWidth = originalFullResImageWidth / (1 << z);
        scaledDownImageWidth = (int)scaledDownImageWidth;
        double resolution = fudge * (this.maxX - this.minX) / scaledDownImageWidth;
        return 256.0 * resolution;
    }

    void calculateRequiredTiles(TIntArrayList rasterizeXY, int ptz) {
        s_required.clear();
        if (ptz < this.maxZ) {
            for (int i = 0; i < rasterizeXY.size() - 1; i += 2) {
                int ptx = rasterizeXY.getQuick(i);
                int pty = rasterizeXY.getQuick(i + 1);
                if (this.isValidTile(ptx, pty, ptz)) {
                    String key = getKey(ptx, pty, ptz);
                    s_required.add(key);
                    ImagePyramid.PyramidTexture pyramidTexture = this.textures.get(key);
                    if (pyramidTexture != null) {
                        pyramidTexture.requiredThisFrame = true;
                    }
                }
            }
        }

        int required = s_required.size();
        int scaledDownImageWidth = this.imageWidth / (1 << this.maxZ);
        int scaledDownImageHeight = this.imageHeight / (1 << this.maxZ);
        int tileSpanX = (int)PZMath.ceil(scaledDownImageWidth / 256.0F);
        int tileSpanY = (int)PZMath.ceil(scaledDownImageHeight / 256.0F);
        required += tileSpanX * tileSpanY;
        this.maxTextures = PZMath.max(this.maxTextures, required);
    }

    boolean calculateTilesCoveringCellF(int cellX, int cellY, float metersPerTile, int[] out) {
        int clipX1 = PZMath.clamp(this.minX, cellX * 256, (cellX + 1) * 256);
        int clipY1 = PZMath.clamp(this.minY, cellY * 256, (cellY + 1) * 256);
        int clipX2 = PZMath.clamp(this.maxX, cellX * 256, (cellX + 1) * 256);
        int clipY2 = PZMath.clamp(this.maxY, cellY * 256, (cellY + 1) * 256);
        if (clipX1 != clipX2 && clipY1 != clipY2) {
            out[0] = PZMath.fastfloor((cellX * 256 - this.minX) / metersPerTile);
            out[1] = PZMath.fastfloor((cellY * 256 - this.minY) / metersPerTile);
            out[2] = PZMath.fastfloor(((cellX + 1) * 256 - this.minX) / metersPerTile);
            out[3] = PZMath.fastfloor(((cellY + 1) * 256 - this.minY) / metersPerTile);
            return true;
        } else {
            return false;
        }
    }

    boolean calculateTilesCoveringCell(int cellX, int cellY, int ptz, int[] out) {
        float metersPerTile = (float)this.calculateMetersPerTile(ptz);
        return this.calculateTilesCoveringCellF(cellX, cellY, metersPerTile, out);
    }

    void calculateRequiredTilesForCells(TIntArrayList rasterizeXY, int ptz) {
        s_required.clear();
        if (ptz < this.maxZ) {
            float metersPerTile = (float)this.calculateMetersPerTile(ptz);

            for (int i = 0; i < rasterizeXY.size() - 1; i += 2) {
                int cellX = rasterizeXY.get(i);
                int cellY = rasterizeXY.get(i + 1);
                if (this.calculateTilesCoveringCellF(cellX, cellY, metersPerTile, s_tilesCoveringCell)) {
                    int tileMinX = s_tilesCoveringCell[0];
                    int tileMinY = s_tilesCoveringCell[1];
                    int tileMaxX = s_tilesCoveringCell[2];
                    int tileMaxY = s_tilesCoveringCell[3];

                    for (int pty = tileMinY; pty <= tileMaxY; pty++) {
                        for (int ptx = tileMinX; ptx <= tileMaxX; ptx++) {
                            if (this.isValidTile(ptx, pty, ptz)) {
                                String key = getKey(ptx, pty, ptz);
                                s_required.add(key);
                                ImagePyramid.PyramidTexture pyramidTexture = this.textures.get(key);
                                if (pyramidTexture != null) {
                                    pyramidTexture.requiredThisFrame = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        int required = s_required.size();
        int scaledDownImageWidth = this.imageWidth / (1 << this.maxZ);
        int scaledDownImageHeight = this.imageHeight / (1 << this.maxZ);
        int tileSpanX = (int)PZMath.ceil(scaledDownImageWidth / 256.0F);
        int tileSpanY = (int)PZMath.ceil(scaledDownImageHeight / 256.0F);
        required += tileSpanX * tileSpanY;
        this.maxTextures = PZMath.max(this.maxTextures, required);
    }

    void endFrame() {
        for (ImagePyramid.PyramidTexture pyramidTexture : this.textures.values()) {
            if (pyramidTexture.z != this.maxZ && !pyramidTexture.requiredThisFrame && pyramidTexture.state == ImagePyramid.TextureState.Loading) {
                pyramidTexture.state = ImagePyramid.TextureState.Cancelled;
                this.queueCancel.add(pyramidTexture.key);
            }
        }
    }

    ImagePyramid.PyramidTexture checkTextureCache(int x, int y, int z, String key) {
        if (this.textures.size() < this.maxTextures) {
            ImagePyramid.PyramidTexture pyramidTexture = new ImagePyramid.PyramidTexture();
            pyramidTexture.x = x;
            pyramidTexture.y = y;
            pyramidTexture.z = z;
            pyramidTexture.key = key;
            pyramidTexture.requestNumber = this.requestNumber++;
            pyramidTexture.requiredThisFrame = true;
            this.textures.put(key, pyramidTexture);
            if (this.requestNumber >= maxRequestNumber) {
                this.resetRequestNumbers();
            }

            return pyramidTexture;
        } else {
            ImagePyramid.PyramidTexture oldest = null;
            int required = 0;

            for (ImagePyramid.PyramidTexture pyramidTexture : this.textures.values()) {
                if (pyramidTexture.z == this.maxZ) {
                    required++;
                } else if (pyramidTexture.requiredThisFrame) {
                    if (!s_required.contains(pyramidTexture.key)) {
                        boolean var9 = true;
                    }

                    required++;
                } else if (oldest == null || oldest.requestNumber > pyramidTexture.requestNumber) {
                    oldest = pyramidTexture;
                }
            }

            if (oldest != null) {
                this.textures.remove(oldest.key);
                oldest.x = x;
                oldest.y = y;
                oldest.z = z;
                oldest.key = key;
                oldest.requestNumber = this.requestNumber++;
                oldest.requiredThisFrame = true;
                oldest.state = ImagePyramid.TextureState.Init;
                this.textures.put(oldest.key, oldest);
                if (!s_required.contains(oldest.key)) {
                    boolean var11 = true;
                }

                if (this.requestNumber >= maxRequestNumber) {
                    this.resetRequestNumbers();
                }

                return oldest;
            } else if (z == this.maxZ) {
                this.maxTextures++;
                ImagePyramid.PyramidTexture pyramidTexturex = new ImagePyramid.PyramidTexture();
                pyramidTexturex.x = x;
                pyramidTexturex.y = y;
                pyramidTexturex.z = z;
                pyramidTexturex.key = key;
                pyramidTexturex.requestNumber = this.requestNumber++;
                pyramidTexturex.requiredThisFrame = true;
                this.textures.put(key, pyramidTexturex);
                if (this.requestNumber >= maxRequestNumber) {
                    this.resetRequestNumbers();
                }

                return pyramidTexturex;
            } else {
                return null;
            }
        }
    }

    void resetRequestNumbers() {
        ArrayList<ImagePyramid.PyramidTexture> sorted = new ArrayList<>(this.textures.values());
        sorted.sort(Comparator.comparingInt(o -> o.requestNumber));
        this.requestNumber = 1;

        for (ImagePyramid.PyramidTexture pyramidTexture : sorted) {
            pyramidTexture.requestNumber = this.requestNumber++;
        }

        sorted.clear();
    }

    private void readInfoFile() {
        if (this.zipFs != null && this.zipFs.isOpen()) {
            int VERSION = -1;
            Path path = this.zipFs.getPath("pyramid.txt");

            String line;
            try (
                InputStream is = Files.newInputStream(path);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
            ) {
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("VERSION=")) {
                        line = line.substring("VERSION=".length());
                        VERSION = PZMath.tryParseInt(line, -1);
                    } else if (line.startsWith("bounds=")) {
                        line = line.substring("bounds=".length());
                        String[] ss = line.split(" ");
                        if (ss.length == 4) {
                            this.minX = PZMath.tryParseInt(ss[0], -1);
                            this.minY = PZMath.tryParseInt(ss[1], -1);
                            this.maxX = PZMath.tryParseInt(ss[2], -1);
                            this.maxY = PZMath.tryParseInt(ss[3], -1);
                        }
                    } else if (line.startsWith("clampS=")) {
                        String value = line.substring("clampS=".length()).trim();
                        if ("clamp_to_edge".equalsIgnoreCase(value)) {
                            this.clampS = 33071;
                        } else if ("repeat".equalsIgnoreCase(value)) {
                            this.clampS = 10497;
                        }
                    } else if (line.startsWith("clampT=")) {
                        String value = line.substring("clampT=".length()).trim();
                        if ("clamp_to_edge".equalsIgnoreCase(value)) {
                            this.clampT = 33071;
                        } else if ("repeat".equalsIgnoreCase(value)) {
                            this.clampT = 10497;
                        }
                    } else if (line.startsWith("minFilter=")) {
                        String value = line.substring("minFilter=".length()).trim();
                        if ("linear".equalsIgnoreCase(value)) {
                            this.minFilter = 9729;
                        } else if ("nearest".equalsIgnoreCase(value)) {
                            this.minFilter = 9728;
                        }
                    } else if (line.startsWith("magFilter=")) {
                        String value = line.substring("magFilter=".length()).trim();
                        if ("linear".equalsIgnoreCase(value)) {
                            this.magFilter = 9729;
                        } else if ("nearest".equalsIgnoreCase(value)) {
                            this.magFilter = 9728;
                        }
                    } else if (line.startsWith("imageSize=")) {
                        line = line.substring("imageSize=".length());
                        String[] ss = line.split(" ");
                        if (ss.length == 2) {
                            this.imageWidth = PZMath.tryParseInt(ss[0], -1);
                            this.imageHeight = PZMath.tryParseInt(ss[1], -1);
                        }
                    } else if (line.startsWith("resolution=")) {
                        line = line.substring("resolution=".length());
                        this.resolution = PZMath.tryParseFloat(line, 1.0F);
                    }
                }
            } catch (Exception var14) {
                ExceptionLogger.logException(var14);
            }
        }
    }

    public boolean isDestroyed() {
        return this.destroyed;
    }

    public void destroy() {
        this.destroyed = true;

        for (FileTask_LoadImagePyramidTexture fileTask : this.fileTasks.values()) {
            fileTask.cancelled = true;
            GameWindow.fileSystem.cancelAsync(fileTask.asyncOp);
            fileTask.asyncOp = -1;
        }

        DebugType.ExitDebug.debugln("ImagePyramid.destroy() 1");

        for (; !this.fileTasks.isEmpty(); Thread.onSpinWait()) {
            GameWindow.fileSystem.updateAsyncTransactions();
            this.checkCalledQueue();
            this.checkQueue();

            try {
                Thread.sleep(10L);
            } catch (InterruptedException var4) {
            }
        }

        DebugType.ExitDebug.debugln("ImagePyramid.destroy() 2");

        for (ImagePyramid.FileTaskResult fileTask = this.queueRender.poll(); fileTask != null; fileTask = this.queueRender.poll()) {
            this.disposeImageData(fileTask.imageData);
        }

        if (this.zipFs != null) {
            try {
                this.zipFs.close();
            } catch (IOException var3) {
            }

            this.zipFs = null;
        }

        RenderThread.invokeOnRenderContext(() -> {
            for (ImagePyramid.PyramidTexture pyramidTexture : this.textures.values()) {
                if (pyramidTexture.textureId != null) {
                    pyramidTexture.textureId.destroy();
                    pyramidTexture.textureId = null;
                }
            }
        });
        this.missing.clear();
        this.textures.clear();
    }

    void checkQueue() {
        for (ImagePyramid.FileTaskResult fileTaskResult = this.queueRender.poll(); fileTaskResult != null; fileTaskResult = this.queueRender.poll()) {
            ImageData imageData = fileTaskResult.imageData;
            ImagePyramid.PyramidTexture pyramidTexture = this.textures.get(fileTaskResult.key);
            if (pyramidTexture == null) {
                this.disposeImageData(imageData);
            } else if (pyramidTexture.state == ImagePyramid.TextureState.Cancelled) {
                this.disposeImageData(imageData);
            } else if (imageData == null) {
                pyramidTexture.requestNumber = 0;
                this.missing.add(pyramidTexture.key);
            } else {
                if (pyramidTexture.textureId == null) {
                    pyramidTexture.textureId = new TextureID(imageData);
                } else {
                    this.replaceTextureData(pyramidTexture, imageData);
                }

                pyramidTexture.state = ImagePyramid.TextureState.Ready;
                if (++this.texturesLoadedThisFrame >= 5) {
                    break;
                }
            }
        }
    }

    void disposeImageData(ImageData imageData) {
        if (imageData != null) {
            imageData.dispose();
            DirectBufferAllocator.destroyDisposed();
        }
    }

    void onFileTaskFinished(Object result) {
        FileTask_LoadImagePyramidTexture fileTask = (FileTask_LoadImagePyramidTexture)result;
        if (fileTask == this.fileTasks.get(fileTask.key)) {
            this.fileTasks.remove(fileTask.key);
            ImagePyramid.FileTaskResult fileTaskResult = new ImagePyramid.FileTaskResult(fileTask.key, fileTask.imageData);
            fileTask.imageData = null;
            this.queueRender.add(fileTaskResult);
        } else {
            this.disposeImageData(fileTask.imageData);
            fileTask.imageData = null;
        }
    }

    void onFileTaskCancelled(FileTask_LoadImagePyramidTexture fileTask) {
        if (fileTask == this.fileTasks.get(fileTask.key)) {
            this.fileTasks.remove(fileTask.key);
        }

        this.disposeImageData(fileTask.imageData);
        fileTask.imageData = null;
    }

    void onFileTaskCalled(FileTask_LoadImagePyramidTexture fileTask) {
        this.queueCalled.add(fileTask);
    }

    static final class FileTaskRequest {
        ImagePyramid pyramid;
        String key;
        Path path;

        FileTaskRequest(ImagePyramid pyramid, String key, Path path) {
            this.pyramid = pyramid;
            this.key = key;
            this.path = path;
        }
    }

    static final class FileTaskResult {
        String key;
        ImageData imageData;

        FileTaskResult(String key, ImageData imageData) {
            this.key = key;
            this.imageData = imageData;
        }
    }

    static final class ImageKeyXYZ {
        final TIntObjectHashMap<ImagePyramid.ImageKeyYZ> yz = new TIntObjectHashMap<>();

        String get(int x, int y, int z) {
            ImagePyramid.ImageKeyYZ yz = this.yz.get(y);
            if (yz == null) {
                this.yz.put(y, yz = new ImagePyramid.ImageKeyYZ());
            }

            return yz.get(x, y, z);
        }
    }

    static final class ImageKeyYZ {
        TIntObjectHashMap<String> z = new TIntObjectHashMap<>();

        String get(int x, int y, int z) {
            String s = this.z.get(z);
            if (s == null) {
                this.z.put(z, s = String.format(Locale.ENGLISH, "%dx%dx%d", x, y, z));
            }

            return s;
        }
    }

    public static final class PyramidTexture {
        int x;
        int y;
        int z;
        String key;
        int requestNumber;
        boolean requiredThisFrame;
        TextureID textureId;
        ImagePyramid.TextureState state = ImagePyramid.TextureState.Init;

        public boolean isReady() {
            return this.state == ImagePyramid.TextureState.Ready;
        }

        public TextureID getTextureID() {
            return this.textureId;
        }
    }

    static enum TextureState {
        Init,
        Loading,
        Cancelled,
        Ready;
    }
}
