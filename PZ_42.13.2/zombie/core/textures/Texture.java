// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL21;
import org.lwjgl.system.MemoryUtil;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.asset.Asset;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetType;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.bucket.BucketManager;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.RenderThread;
import zombie.core.utils.BooleanGrid;
import zombie.core.utils.ImageUtils;
import zombie.core.utils.WrappedBuffer;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.LogSeverity;
import zombie.fileSystem.FileSystem;
import zombie.interfaces.IDestroyable;
import zombie.interfaces.ITexture;
import zombie.iso.Vector2;
import zombie.iso.objects.ObjectRenderEffects;
import zombie.network.GameServer;
import zombie.network.ServerGUI;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class Texture extends Asset implements IDestroyable, ITexture, Serializable {
    private static final ThreadLocal<PNGSize> pngSize = ThreadLocal.withInitial(PNGSize::new);
    public static final HashSet<String> nullTextures = new HashSet<>();
    private static final ObjectRenderEffects objRen = ObjectRenderEffects.alloc();
    public static final AssetType ASSET_TYPE = new AssetType("Texture");
    public static int bindCount;
    public static boolean doingQuad;
    public static float lr;
    public static float lg;
    public static float lb;
    public static float la;
    public static int lastlastTextureID = -2;
    public static int totalTextureID;
    private static Texture white;
    private static Texture errorTexture;
    private static Texture mipmap;
    public static int lastTextureID = -1;
    public static boolean warnFailFindTexture = true;
    private static final HashMap<String, Texture> textures = new HashMap<>();
    private static final HashMap<String, Texture> s_sharedTextureTable = new HashMap<>();
    private static final HashMap<Long, Texture> steamAvatarMap = new HashMap<>();
    public boolean flip;
    public float offsetX;
    public float offsetY;
    public boolean bindAlways;
    /**
     * internal texture coordinates
     *  it's used to get the max border of texture...
     */
    public float xEnd = 1.0F;
    /**
     * internal texture coordinates
     *  it's used to get the max border of texture...
     */
    public float yEnd = 1.0F;
    /**
     * internal texture coordinates
     *  it's used to get the max border of texture...
     */
    public float xStart;
    /**
     * internal texture coordinates
     *  it's used to get the max border of texture...
     */
    public float yStart;
    protected TextureID dataid;
    protected Mask mask;
    protected String name;
    protected boolean solid;
    protected int width;
    protected int height;
    protected int heightOrig;
    protected int widthOrig;
    private int realWidth;
    private int realHeight;
    private boolean destroyed;
    private Texture splitIconTex;
    private int splitX = -1;
    private int splitY;
    private int splitW;
    private int splitH;
    protected FileSystem.SubTexture subTexture;
    public Texture.TextureAssetParams assetParams;

    public Texture(AssetPath path, AssetManager manager, Texture.TextureAssetParams params) {
        super(path, manager);
        this.assetParams = params;
        this.name = path == null ? null : path.getPath();
        if (params != null && params.subTexture != null) {
            FileSystem.SubTexture sub = params.subTexture;
            this.splitX = sub.info.x;
            this.splitY = sub.info.y;
            this.splitW = sub.info.w;
            this.splitH = sub.info.h;
            this.width = this.splitW;
            this.height = this.splitH;
            this.offsetX = sub.info.ox;
            this.offsetY = sub.info.oy;
            this.widthOrig = sub.info.fx;
            this.heightOrig = sub.info.fy;
            this.name = sub.info.name;
            this.subTexture = sub;
        }

        TextureID.TextureIDAssetParams assetParams1 = new TextureID.TextureIDAssetParams();
        if (this.assetParams != null && this.assetParams.subTexture != null) {
            assetParams1.subTexture = this.assetParams.subTexture;
            String packName = assetParams1.subTexture.packName;
            String pageName = assetParams1.subTexture.pageName;
            FileSystem fileSystem = this.getAssetManager().getOwner().getFileSystem();
            assetParams1.flags = fileSystem.getTexturePackFlags(packName);
            assetParams1.flags = assetParams1.flags | (fileSystem.getTexturePackAlpha(packName, pageName) ? 8 : 0);
            AssetPath assetPath = new AssetPath("@pack@/" + packName + "/" + pageName);
            this.dataid = (TextureID)TextureIDAssetManager.instance.load(assetPath, assetParams1);
        } else {
            if (this.assetParams == null) {
                assetParams1.flags = assetParams1.flags | (TextureID.useCompressionOption ? 4 : 0);
            } else {
                assetParams1.flags = this.assetParams.flags;
            }

            this.dataid = (TextureID)this.getAssetManager().getOwner().get(TextureID.ASSET_TYPE).load(this.getPath(), assetParams1);
        }

        this.onCreated(Asset.State.EMPTY);
        if (this.dataid != null) {
            this.addDependency(this.dataid);
        }
    }

    public Texture(TextureID data, String name) {
        super(null, TextureAssetManager.instance);
        this.dataid = data;
        this.dataid.referenceCount++;
        if (data.isReady()) {
            this.solid = this.dataid.solid;
            this.width = data.width;
            this.height = data.height;
            this.xEnd = (float)this.width / data.widthHw;
            this.yEnd = (float)this.height / data.heightHw;
        } else {
            assert false;
        }

        this.name = name;
        this.assetParams = null;
        this.onCreated(data.getState());
        this.addDependency(data);
    }

    public Texture(TextureID data, String name, int splitX, int splitY, int splitW, int splitH) {
        super(null, TextureAssetManager.instance);
        this.dataid = data;
        this.dataid.referenceCount++;
        if (data.isReady()) {
            this.solid = this.dataid.solid;
            this.width = data.width;
            this.height = data.height;
            this.xEnd = (float)this.width / data.widthHw;
            this.yEnd = (float)this.height / data.heightHw;
        }

        this.name = name;
        this.splitX = splitX;
        this.splitY = splitY;
        this.splitW = splitW;
        this.splitH = splitH;
        if (data.isReady()) {
            this.setRegion(splitX, splitY, splitW, splitH);
        }

        this.assetParams = null;
        this.onCreated(data.getState());
        this.addDependency(data);
    }

    public void TexDeferedCreation(int w, int h, int flags, int format, int internalFormat) {
        this.dataid = new TextureID(w, h, flags, format, internalFormat);
        TextureID data = this.dataid;
        this.dataid.referenceCount++;
        if (data.isReady()) {
            this.solid = this.dataid.solid;
            this.width = data.width;
            this.height = data.height;
            this.xEnd = (float)this.width / data.widthHw;
            this.yEnd = (float)this.height / data.heightHw;
        } else {
            assert false;
        }

        this.assetParams = null;
        this.onCreated(data.getState());
        this.addDependency(data);
    }

    public void TexDeferedCreation(int w, int h, int flags) {
        this.TexDeferedCreation(w, h, flags, 6408, 6408);
    }

    /**
     * LOADS and crete a texture from a file
     * 
     * @param this relative path
     */
    public Texture(String file) throws Exception {
        this(new TextureID(file), file);
        this.setUseAlphaChannel(true);
    }

    public Texture(String name, BufferedInputStream b, boolean bDoMask) throws Exception {
        this(new TextureID(b, name, bDoMask), name);
        if (bDoMask) {
            this.createMask(this.dataid.mask);
            this.dataid.mask = null;
            this.dataid.data = null;
        }
    }

    public Texture(String file, boolean bDelete, boolean bUseAlpha) throws Exception {
        this(new TextureID(file), file);
        this.setUseAlphaChannel(bUseAlpha);
        if (bDelete) {
            this.dataid.data = null;
        }
    }

    /**
     * LOADS and crete a texture from a file
     * 
     * @param this relative path
     * @param file indicates if the image should use or not the alpha channel
     */
    public Texture(String file, boolean useAlphaChannel) throws Exception {
        this(new TextureID(file), file);
        this.setUseAlphaChannel(useAlphaChannel);
    }

    /**
     * create a new empty texture.
     * 
     * @param this size of texture
     * @param width size of texture
     * @param height
     * @param name
     */
    public Texture(int width, int height, String name, int flags) {
        this(new TextureID(width, height, flags), name);
    }

    /**
     * create a new empty texture.
     * 
     * @param this size of texture
     * @param width size of texture
     * @param height
     */
    public Texture(int width, int height, int flags) {
        this(new TextureID(width, height, flags), null);
    }

    public Texture(int width, int height, int flags, int format, int internalFormat) {
        this(new TextureID(width, height, flags, format, internalFormat), null);
    }

    public Texture(int width, int height, int flags, boolean deferCreation) {
        super(null, TextureAssetManager.instance);
        int widthHW = ImageUtils.getNextPowerOfTwoHW(width);
        int heightHW = ImageUtils.getNextPowerOfTwoHW(height);
        this.xEnd = (float)width / widthHW;
        this.yEnd = (float)height / heightHW;
        this.isDefered = true;
    }

    /**
     * loads and create a texture from a file and cretes as trasparent the section that have the color equal to the
     *  RGB valued
     *  spefied
     * 
     * @param this relative path
     * @param file red value to compare
     * @param red green value to compare
     * @param green blue value to compare
     */
    public Texture(String file, int red, int green, int blue) throws Exception {
        this(new TextureID(file, red, green, blue), file);
    }

    /**
     * creates a copy of an existent texture
     * 
     * @param this original texture
     */
    public Texture(Texture t) {
        this(t.dataid, t.name + "(copy)");
        this.width = t.width;
        this.height = t.height;
        this.name = t.name;
        this.xStart = t.xStart;
        this.yStart = t.yStart;
        this.xEnd = t.xEnd;
        this.yEnd = t.yEnd;
        this.solid = t.solid;
    }

    /**
     * creates an emptiy texture and adds it to the game engine's texture list
     */
    public Texture() {
        super(null, TextureAssetManager.instance);
        this.assetParams = null;
        this.onCreated(Asset.State.EMPTY);
    }

    public static String processFilePath(String filePath) {
        return filePath.replaceAll("\\\\", "/");
    }

    public static void bindNone() {
        IndieGL.glDisable(3553);
        lastTextureID = -1;
        bindCount--;
    }

    public static Texture getWhite() {
        if (white == null) {
            white = new Texture(32, 32, "white", 0);
            RenderThread.invokeOnRenderContext(() -> {
                GL11.glBindTexture(3553, lastTextureID = white.getID());
                GL11.glTexParameteri(3553, 10241, 9728);
                GL11.glTexParameteri(3553, 10240, 9728);
                ByteBuffer pixels = MemoryUtil.memAlloc(white.width * white.height * 4);

                for (int i = 0; i < white.width * white.height * 4; i++) {
                    pixels.put((byte)-1);
                }

                pixels.flip();
                GL11.glTexImage2D(3553, 0, 6408, white.width, white.height, 0, 6408, 5121, pixels);
                MemoryUtil.memFree(pixels);
            });
            s_sharedTextureTable.put("white.png", white);
            s_sharedTextureTable.put("media/white.png", white);
            s_sharedTextureTable.put("media/ui/white.png", white);
        }

        return white;
    }

    public static Texture getErrorTexture() {
        if (errorTexture == null) {
            errorTexture = new Texture(32, 32, "EngineErrorTexture", 0);
            RenderThread.invokeOnRenderContext(() -> {
                GL11.glBindTexture(3553, lastTextureID = errorTexture.getID());
                GL11.glTexParameteri(3553, 10241, 9728);
                GL11.glTexParameteri(3553, 10240, 9728);
                int BPP = 4;
                ByteBuffer pixels = MemoryUtil.memAlloc(errorTexture.width * errorTexture.height * 4);
                pixels.position(errorTexture.width * errorTexture.height * 4);
                int span = errorTexture.width * 4;
                boolean rowStartRed = true;
                boolean red = rowStartRed;
                int NUM_SQUARES = 8;
                int PIXELS_PER_SQUARE = errorTexture.width / 8;

                for (int index = 0; index < 64; index++) {
                    int row = index / 8;
                    int col = index % 8;
                    if (row > 0 && col == 0) {
                        rowStartRed = !rowStartRed;
                        red = rowStartRed;
                    }

                    int rgba = red ? -16776961 : -1;
                    red = !red;

                    for (int y = 0; y < PIXELS_PER_SQUARE; y++) {
                        for (int x = 0; x < PIXELS_PER_SQUARE; x++) {
                            pixels.putInt((row * PIXELS_PER_SQUARE + y) * span + (col * PIXELS_PER_SQUARE + x) * 4, rgba);
                        }
                    }
                }

                pixels.flip();
                GL11.glTexImage2D(3553, 0, 6408, errorTexture.width, errorTexture.height, 0, 6408, 5121, pixels);
                MemoryUtil.memFree(pixels);
            });
            s_sharedTextureTable.put("EngineErrorTexture.png", errorTexture);
        }

        return errorTexture;
    }

    private static void initEngineMipmapTextureLevel(int level, int width, int height, int r, int g, int b, int a) {
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        MemoryUtil.memSet(pixels, 255);

        for (int i = 0; i < width * height; i++) {
            pixels.put((byte)(r & 0xFF));
            pixels.put((byte)(g & 0xFF));
            pixels.put((byte)(b & 0xFF));
            pixels.put((byte)(a & 0xFF));
        }

        pixels.flip();
        GL11.glTexImage2D(3553, level, 6408, width, height, 0, 6408, 5121, pixels);
        MemoryUtil.memFree(pixels);
    }

    public static Texture getEngineMipmapTexture() {
        if (mipmap == null) {
            mipmap = new Texture(256, 256, "EngineMipmapTexture", 0);
            mipmap.dataid.setMinFilter(9984);
            RenderThread.invokeOnRenderContext(() -> {
                GL11.glBindTexture(3553, lastTextureID = mipmap.getID());
                GL11.glTexParameteri(3553, 10241, 9984);
                GL11.glTexParameteri(3553, 10240, 9728);
                GL11.glTexParameteri(3553, 33085, 6);
                initEngineMipmapTextureLevel(0, mipmap.width, mipmap.height, 255, 0, 0, 255);
                initEngineMipmapTextureLevel(1, mipmap.width / 2, mipmap.height / 2, 0, 255, 0, 255);
                initEngineMipmapTextureLevel(2, mipmap.width / 4, mipmap.height / 4, 0, 0, 255, 255);
                initEngineMipmapTextureLevel(3, mipmap.width / 8, mipmap.height / 8, 255, 255, 0, 255);
                initEngineMipmapTextureLevel(4, mipmap.width / 16, mipmap.height / 16, 255, 0, 255, 255);
                initEngineMipmapTextureLevel(5, mipmap.width / 32, mipmap.height / 32, 0, 0, 0, 255);
                initEngineMipmapTextureLevel(6, mipmap.width / 64, mipmap.height / 64, 255, 255, 255, 255);
            });
        }

        return mipmap;
    }

    public static void clearTextures() {
        textures.clear();
    }

    public static Texture getSharedTexture(String name) {
        int flags = 0;
        flags |= TextureID.useCompression ? 4 : 0;
        return getSharedTexture(name, flags);
    }

    public static Texture getSharedTexture(String name, int flags) {
        if (GameServer.server && !ServerGUI.isCreated()) {
            return null;
        } else {
            try {
                return getSharedTextureInternal(name, flags);
            } catch (Exception var3) {
                DebugLog.FileIO.printException(var3, "Failed to load shared texture: " + name, LogSeverity.Error);
                return null;
            }
        }
    }

    public static Texture trygetTexture(String name) {
        if (GameServer.server && !ServerGUI.isCreated()) {
            return null;
        } else {
            Texture texture = getSharedTexture(name);
            if (texture == null) {
                String fileName = "media/textures/" + name;
                if (!name.endsWith(".png")) {
                    fileName = fileName + ".png";
                }

                texture = s_sharedTextureTable.get(fileName);
                if (texture != null) {
                    return texture;
                }

                String path = ZomboidFileSystem.instance.getString(fileName);
                if (!path.equals(fileName)) {
                    int flags = 0;
                    flags |= TextureID.useCompression ? 4 : 0;
                    Texture.TextureAssetParams assetParams = new Texture.TextureAssetParams();
                    assetParams.flags = flags;
                    texture = (Texture)TextureAssetManager.instance.load(new AssetPath(path), assetParams);
                    BucketManager.Shared().AddTexture(fileName, texture);
                    setSharedTextureInternal(fileName, texture);
                }
            }

            return texture;
        }
    }

    private static void onTextureFileChanged(String fileName) {
        DebugLog.General.println("Texture.onTextureFileChanged> " + fileName);
    }

    public static void onTexturePacksChanged() {
        nullTextures.clear();
        s_sharedTextureTable.clear();
    }

    private static void setSharedTextureInternal(String fileName, Texture texture) {
        s_sharedTextureTable.put(fileName, texture);
    }

    private static Texture getSharedTextureInternal(String name, int flags) {
        if (GameServer.server && !ServerGUI.isCreated()) {
            return null;
        } else if (nullTextures.contains(name)) {
            return null;
        } else {
            Texture texture = s_sharedTextureTable.get(name);
            if (texture != null) {
                return texture;
            } else {
                if (!name.endsWith(".txt")) {
                    String subname = name;
                    if (name.endsWith(".png")) {
                        subname = name.substring(0, name.lastIndexOf("."));
                    }

                    subname = subname.substring(name.lastIndexOf("/") + 1);
                    Texture tex = TexturePackPage.getTexture(subname);
                    if (tex != null) {
                        setSharedTextureInternal(name, tex);
                        return tex;
                    }

                    FileSystem.SubTexture fsSubTex = GameWindow.texturePackTextures.get(subname);
                    if (fsSubTex != null) {
                        Texture.TextureAssetParams assetParams = new Texture.TextureAssetParams();
                        assetParams.subTexture = fsSubTex;
                        String assetPath = "@pack/" + fsSubTex.packName + "/" + fsSubTex.pageName + "/" + fsSubTex.info.name;
                        Texture t = (Texture)TextureAssetManager.instance.load(new AssetPath(assetPath), assetParams);
                        if (t == null) {
                            nullTextures.add(name);
                            DebugLog.FileIO.warn(name + " is a null texture.");
                        } else {
                            setSharedTextureInternal(name, t);
                        }

                        return t;
                    }
                }

                if (TexturePackPage.subTextureMap.containsKey(name)) {
                    return TexturePackPage.subTextureMap.get(name);
                } else {
                    FileSystem.SubTexture fsSubTex = GameWindow.texturePackTextures.get(name);
                    if (fsSubTex != null) {
                        Texture.TextureAssetParams assetParams = new Texture.TextureAssetParams();
                        assetParams.subTexture = fsSubTex;
                        String assetPath = "@pack/" + fsSubTex.packName + "/" + fsSubTex.pageName + "/" + fsSubTex.info.name;
                        Texture t = (Texture)TextureAssetManager.instance.load(new AssetPath(assetPath), assetParams);
                        if (t == null) {
                            nullTextures.add(name);
                            DebugLog.FileIO.warn(name + " is a null texture.");
                        } else {
                            setSharedTextureInternal(name, t);
                        }

                        return t;
                    } else if (BucketManager.Shared().HasTexture(name)) {
                        Texture t = BucketManager.Shared().getTexture(name);
                        setSharedTextureInternal(name, t);
                        return t;
                    } else if (name.lastIndexOf(46) == -1) {
                        nullTextures.add(name);
                        return null;
                    } else {
                        String path = ZomboidFileSystem.instance.getString(name);
                        boolean bExists = path != name;
                        if (!bExists && !new File(path).exists()) {
                            nullTextures.add(name);
                            DebugLog.FileIO.warn(name + " is a null texture.");
                            return null;
                        } else {
                            Texture.TextureAssetParams assetParams = new Texture.TextureAssetParams();
                            assetParams.flags = flags;
                            Texture t = (Texture)TextureAssetManager.instance.load(new AssetPath(path), assetParams);
                            BucketManager.Shared().AddTexture(name, t);
                            setSharedTextureInternal(name, t);
                            return t;
                        }
                    }
                }
            }
        }
    }

    /**
     * gets a texture from it's name; If the texture isn't already loaded this method will load it.
     * 
     * @param name the name of texture
     * @return returns the texture from the given name
     */
    public static Texture getTexture(String name) {
        if (!name.contains(".txt")) {
            String ex = name.replace(".png", "");
            ex = ex.substring(name.lastIndexOf("/") + 1);
            Texture tex = TexturePackPage.getTexture(ex);
            if (tex != null) {
                return tex;
            }
        }

        if (BucketManager.Active().HasTexture(name)) {
            return BucketManager.Active().getTexture(name);
        } else {
            try {
                Texture t = new Texture(name);
                BucketManager.Active().AddTexture(name, t);
                return t;
            } catch (Exception var3) {
                DebugLog.FileIO.printException(var3, "Failed to load texture: " + name, LogSeverity.Error);
                return null;
            }
        }
    }

    public static Texture getSteamAvatar(long steamID) {
        if (steamAvatarMap.containsKey(steamID)) {
            return steamAvatarMap.get(steamID);
        } else {
            TextureID textureID = TextureID.createSteamAvatar(steamID);
            if (textureID == null) {
                return null;
            } else {
                Texture texture = new Texture(textureID, "SteamAvatar" + SteamUtils.convertSteamIDToString(steamID));
                steamAvatarMap.put(steamID, texture);
                return texture;
            }
        }
    }

    public static void steamAvatarChanged(long steamID) {
        Texture texture = steamAvatarMap.get(steamID);
        if (texture != null) {
            steamAvatarMap.remove(steamID);
        }
    }

    public static void forgetTexture(String name) {
        BucketManager.Shared().forgetTexture(name);
        s_sharedTextureTable.remove(name);
    }

    public static void reload(String name) {
        if (name != null && !name.isEmpty()) {
            Texture tex = s_sharedTextureTable.get(name);
            if (tex == null) {
                tex = Type.tryCastTo(TextureAssetManager.instance.getAssetTable().get(name), Texture.class);
                if (tex == null) {
                    return;
                }
            }

            tex.reloadFromFile(name);
        }
    }

    public static int[] flipPixels(int[] imgPixels, int imgw, int imgh) {
        int[] flippedPixels = null;
        if (imgPixels != null) {
            flippedPixels = new int[imgw * imgh];

            for (int y = 0; y < imgh; y++) {
                for (int x = 0; x < imgw; x++) {
                    flippedPixels[(imgh - y - 1) * imgw + x] = imgPixels[y * imgw + x];
                }
            }
        }

        return flippedPixels;
    }

    public void reloadFromFile(String name) {
        if (this.dataid != null) {
            TextureID.TextureIDAssetParams assetParams = new TextureID.TextureIDAssetParams();
            assetParams.flags = this.dataid.flags;
            this.dataid.getAssetManager().reload(this.dataid, assetParams);
        } else if (name != null && !name.isEmpty()) {
            File file = new File(name);
            if (file.exists()) {
                try {
                    ImageData imageData = new ImageData(file.getAbsolutePath());
                    if (imageData.getWidthHW() != this.getWidthHW() || imageData.getHeightHW() != this.getHeightHW()) {
                        return;
                    }

                    RenderThread.invokeOnRenderContext(imageData, l_imageData -> {
                        GL11.glBindTexture(3553, lastTextureID = this.dataid.id);
                        int internalFormat = 6408;
                        GL11.glTexImage2D(3553, 0, 6408, this.getWidthHW(), this.getHeightHW(), 0, 6408, 5121, l_imageData.getData().getBuffer());
                    });
                } catch (Throwable var4) {
                    ExceptionLogger.logException(var4, name);
                }
            }
        }
    }

    /**
     * Blinds the image
     */
    @Override
    public void bind() {
        this.bind(3553);
    }

    /**
     * Description copied from interface: ITexture
     * 
     * @param unit the texture unit in witch the current TextureObject will be binded
     */
    @Override
    public void bind(int unit) {
        if (!this.isDestroyed() && this.isValid() && this.isReady()) {
            if (this.bindAlways) {
                this.dataid.bindalways();
            } else {
                this.dataid.bind();
            }
        } else {
            getErrorTexture().bind(unit);
        }
    }

    public void copyMaskRegion(Texture from, int x, int y, int width, int height) {
        if (from.getMask() != null) {
            new Mask(from, this, x, y, width, height);
        }
    }

    /**
     * creates the mask of collisions
     */
    public void createMask() {
        new Mask(this);
    }

    public void createMask(boolean[] mask) {
        new Mask(this, mask);
    }

    public void createMask(BooleanGrid mask) {
        new Mask(this, mask);
    }

    public void createMask(WrappedBuffer buf) {
        new Mask(this, buf);
    }

    /**
     * destroys the image and release all resources
     */
    @Override
    public void destroy() {
        if (!this.destroyed) {
            if (this.dataid != null && --this.dataid.referenceCount == 0) {
                if (lastTextureID == this.dataid.id) {
                    lastTextureID = -1;
                }

                this.dataid.destroy();
            }

            this.destroyed = true;
        }
    }

    public boolean equals(Texture other) {
        return other.xStart == this.xStart
            && other.xEnd == this.xEnd
            && other.yStart == this.yStart
            && other.yEnd == this.yEnd
            && other.width == this.width
            && other.height == this.height
            && other.solid == this.solid
            && (
                this.dataid == null
                    || other.dataid == null
                    || other.dataid.pathFileName == null
                    || this.dataid.pathFileName == null
                    || other.dataid.pathFileName.equals(this.dataid.pathFileName)
            );
    }

    /**
     * returns the texture's pixel in a ByteBuffer
     *  
     *  EXAMPLE:
     *  ByteBuffer bb = getData();
     *  byte r, g, b;
     *  bb.rewind(); //<-- IMPORTANT!!
     *  try {
     *  while (true) {
     *  bb.mark();
     *  r = bb.get();
     *  g = bb.get();
     *  b = bb.get();
     *  bb.reset();
     *  bb.put((byte)(r+red));
     *  bb.put((byte)(g+green));
     *  bb.put((byte)(b+blue));
     *  bb.get(); // alpha
     *  
     *  catch (Exception e) {
     *  
     *  setData(bb);
     */
    @Override
    public WrappedBuffer getData() {
        return this.dataid.getData();
    }

    /**
     * sets the texture's pixel from a ByteBuffer
     *  
     *  EXAMPLE:
     *  ByteBuffer bb = getData();
     *  byte r, g, b;
     *  bb.rewind(); //<-- IMPORTANT!!
     *  try {
     *  while (true) {
     *  bb.mark();
     *  r = bb.get();
     *  g = bb.get();
     *  b = bb.get();
     *  bb.reset();
     *  bb.put((byte)(r+red));
     *  bb.put((byte)(g+green));
     *  bb.put((byte)(b+blue));
     *  bb.get(); // alpha
     *  
     *  catch (Exception e) {
     *  
     *  setData(bb);
     * 
     * @param data texture's pixel data
     */
    @Override
    public void setData(ByteBuffer data) {
        this.dataid.setData(data);
    }

    /**
     * Description copied from interface: ITexture
     * @return the height of image
     */
    @Override
    public int getHeight() {
        if (!this.isReady() && this.height <= 0 && !(this instanceof SmartTexture)) {
            this.syncReadSize();
        }

        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Description copied from interface: ITexture
     */
    @Override
    public int getHeightHW() {
        if (!this.isReady() && this.height <= 0 && !(this instanceof SmartTexture) && this.splitX == -1) {
            this.syncReadSize();
        }

        return this.dataid.heightHw;
    }

    public int getHeightOrig() {
        return this.heightOrig == 0 ? this.getHeight() : this.heightOrig;
    }

    /**
     * Description copied from interface: ITexture
     * @return the ID of image in the Vram
     */
    @Override
    public int getID() {
        return this.dataid == null ? 0 : this.dataid.id;
    }

    /**
     * returns the mask of collisions
     * @return mask of collisions
     */
    @Override
    public Mask getMask() {
        return this.mask;
    }

    /**
     * sets the mask of collisions
     * 
     * @param mask the mask of collisions to set
     */
    @Override
    public void setMask(Mask mask) {
        this.mask = mask;
    }

    public boolean isMaskSet(int x, int y) {
        return this.getMask() == null || this.getMask().get(x - (int)this.getOffsetX(), y - (int)this.getOffsetY());
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (name != null) {
            if (name.equals(this.name)) {
                if (!textures.containsKey(name)) {
                    textures.put(name, this);
                }
            } else {
                textures.remove(this.name);
                this.name = name;
                textures.put(name, this);
            }
        }
    }

    public TextureID getTextureId() {
        return this.dataid;
    }

    /**
     * indicates if the image will use the alpha channel or note
     * @return if the image will use the alpha channel or note
     */
    public boolean getUseAlphaChannel() {
        return !this.solid;
    }

    /**
     * indicates if the texture contains the alpha channel or not
     * 
     * @param value if true, the image will use the alpha channel
     */
    public void setUseAlphaChannel(boolean value) {
        this.dataid.solid = this.solid = !value;
    }

    public int getX() {
        return this.splitX;
    }

    public int getY() {
        return this.splitY;
    }

    /**
     * Description copied from interface: ITexture
     * @return the width of image
     */
    @Override
    public int getWidth() {
        if (!this.isReady() && this.width <= 0 && !(this instanceof SmartTexture)) {
            this.syncReadSize();
        }

        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Description copied from interface: ITexture
     */
    @Override
    public int getWidthHW() {
        if (!this.isReady() && this.width <= 0 && !(this instanceof SmartTexture) && this.splitX == -1) {
            this.syncReadSize();
        }

        return this.dataid.widthHw;
    }

    public int getWidthOrig() {
        return this.widthOrig == 0 ? this.getWidth() : this.widthOrig;
    }

    /**
     * Description copied from interface: ITexture
     * @return the end X-coordinate
     */
    @Override
    public float getXEnd() {
        return this.xEnd;
    }

    /**
     * Description copied from interface: ITexture
     * @return the start X-coordinate
     */
    @Override
    public float getXStart() {
        return this.xStart;
    }

    /**
     * Description copied from interface: ITexture
     * @return the end Y-coordinate
     */
    @Override
    public float getYEnd() {
        return this.yEnd;
    }

    /**
     * Description copied from interface: ITexture
     * @return the start Y-coordinate
     */
    @Override
    public float getYStart() {
        return this.yStart;
    }

    public float getOffsetX() {
        return this.offsetX;
    }

    public void setOffsetX(int offset) {
        this.offsetX = offset;
    }

    public float getOffsetY() {
        return this.offsetY;
    }

    public void setOffsetY(int offset) {
        this.offsetY = offset;
    }

    /**
     * indicates if the texture has a mask of collisions or not
     */
    public boolean isCollisionable() {
        return this.mask != null;
    }

    /**
     * returns if the texture is destroyed or not
     */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /**
     * Description copied from interface: ITexture
     * @return if the texture is solid or not.
     */
    @Override
    public boolean isSolid() {
        return this.solid;
    }

    public boolean isValid() {
        return this.dataid != null;
    }

    /**
     * Description copied from interface: ITexture
     * 
     * @param red color used in the test
     * @param green color used in the test
     * @param blue color used in the test
     */
    @Override
    public void makeTransp(int red, int green, int blue) {
        this.setAlphaForeach(red, green, blue, 0);
    }

    public void render(float x, float y, float width, float height) {
        this.render(x, y, width, height, 1.0F, 1.0F, 1.0F, 1.0F, null);
    }

    public void render(float x, float y) {
        this.render(x, y, this.width, this.height, 1.0F, 1.0F, 1.0F, 1.0F, null);
    }

    public void render(float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier) {
        x += this.offsetX;
        y += this.offsetY;
        SpriteRenderer.instance.render(this, x, y, width, height, r, g, b, a, texdModifier);
    }

    public void render(
        ObjectRenderEffects dr, float x, float y, float width, float height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier
    ) {
        float ox = this.offsetX + x;
        float oy = this.offsetY + y;
        objRen.x1 = ox + dr.x1 * width;
        objRen.y1 = oy + dr.y1 * height;
        objRen.x2 = ox + width + dr.x2 * width;
        objRen.y2 = oy + dr.y2 * height;
        objRen.x3 = ox + width + dr.x3 * width;
        objRen.y3 = oy + height + dr.y3 * height;
        objRen.x4 = ox + dr.x4 * width;
        objRen.y4 = oy + height + dr.y4 * height;
        SpriteRenderer.instance.render(this, objRen.x1, objRen.y1, objRen.x2, objRen.y2, objRen.x3, objRen.y3, objRen.x4, objRen.y4, r, g, b, a, texdModifier);
    }

    public void rendershader2(float x, float y, float width, float height, int texx, int texy, int texWidth, int texHeight, float r, float g, float b, float a) {
        if (a != 0.0F) {
            float sx = (float)texx / this.getWidthHW();
            float ey = (float)texy / this.getHeightHW();
            float ex = (float)(texx + texWidth) / this.getWidthHW();
            float sy = (float)(texy + texHeight) / this.getHeightHW();
            if (this.flip) {
                float temp = ex;
                ex = sx;
                sx = temp;
                x += this.widthOrig - this.offsetX - this.width;
            } else {
                x += this.offsetX;
            }

            y += this.offsetY;
            r = PZMath.clamp(r, 0.0F, 1.0F);
            g = PZMath.clamp(g, 0.0F, 1.0F);
            b = PZMath.clamp(b, 0.0F, 1.0F);
            a = PZMath.clamp(a, 0.0F, 1.0F);
            if (!(x + width <= 0.0F)) {
                if (!(y + height <= 0.0F)) {
                    if (!(x >= Core.getInstance().getScreenWidth())) {
                        if (!(y >= Core.getInstance().getScreenHeight())) {
                            lr = r;
                            lg = g;
                            lb = b;
                            la = a;
                            SpriteRenderer.instance.render(this, x, y, width, height, r, g, b, a, sx, sy, ex, sy, ex, ey, sx, ey);
                        }
                    }
                }
            }
        }
    }

    public void renderdiamond(float x, float y, float width, float height, int l, int u, int r, int d) {
        SpriteRenderer.instance.render(null, x, y, x + width / 2.0F, y - height / 2.0F, x + width, y, x + width / 2.0F, y + height / 2.0F, l, u, r, d);
    }

    public void renderwallnw(float x, float y, float width, float height, int u, int d, int u2, int d2, int r, int r2) {
        lr = -1.0F;
        lg = -1.0F;
        lb = -1.0F;
        la = -1.0F;
        if (this.flip) {
            x += this.widthOrig - this.offsetX - this.width;
        } else {
            x += this.offsetX;
        }

        y += this.offsetY;
        int SCL = Core.tileScale;
        float xPad = 0.0F;
        float yPad = 0.0F;
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingOldDebug.getValue()) {
            d2 = -65536;
            d = -65536;
            u2 = -65536;
            u = -65536;
        }

        float x1 = x - width / 2.0F - 0.0F;
        float y1 = y - 96 * SCL + height / 2.0F - 1.0F - 0.0F;
        float x2 = x + 0.0F;
        float y2 = y - 96 * SCL - 2.0F - 0.0F;
        float x3 = x + 0.0F;
        float y3 = y + 4.0F + 0.0F;
        float x4 = x - width / 2.0F - 0.0F;
        float y4 = y + height / 2.0F + 4.0F + 0.0F;
        SpriteRenderer.instance.render(this, x1, y1, x2, y2, x3, y3, x4, y4, d2, u2, u, d);
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingOldDebug.getValue()) {
            r2 = -256;
            r = -256;
            u2 = -256;
            u = -256;
        }

        x1 = x - 0.0F;
        y1 = y - 96 * SCL - 0.0F;
        x2 = x + width / 2.0F + 0.0F;
        y2 = y - 96 * SCL + height / 2.0F - 0.0F;
        x3 = x + width / 2.0F + 0.0F;
        y3 = y + height / 2.0F + 5.0F + 0.0F;
        x4 = x - 0.0F;
        y4 = y + 5.0F + 0.0F;
        SpriteRenderer.instance.render(this, x1, y1, x2, y2, x3, y3, x4, y4, u2, r2, r, u);
    }

    public void renderwallw(float x, float y, float width, float height, int u, int d, int u2, int d2) {
        lr = -1.0F;
        lg = -1.0F;
        lb = -1.0F;
        la = -1.0F;
        if (this.flip) {
            x += this.widthOrig - this.offsetX - this.width;
        } else {
            x += this.offsetX;
        }

        y += this.offsetY;
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingOldDebug.getValue()) {
            d = -16711936;
            u = -16711936;
            d2 = -16728064;
            u2 = -16728064;
        }

        float xPad = 0.0F;
        float yPad = 0.0F;
        int SCL = Core.tileScale;
        float x1 = x - width / 2.0F - 0.0F;
        float y1 = y - 96 * SCL + height / 2.0F - 1.0F - 0.0F;
        float x2 = x + SCL + 0.0F;
        float y2 = y - 96 * SCL - 3.0F - 0.0F;
        float x3 = x + SCL + 0.0F;
        float y3 = y + 3.0F + 0.0F;
        float x4 = x - width / 2.0F - 0.0F;
        float y4 = y + height / 2.0F + 4.0F + 0.0F;
        SpriteRenderer.instance.render(this, x1, y1, x2, y2, x3, y3, x4, y4, d2, u2, u, d);
    }

    public void renderwalln(float x, float y, float width, float height, int u, int d, int u2, int d2) {
        lr = -1.0F;
        lg = -1.0F;
        lb = -1.0F;
        la = -1.0F;
        if (this.flip) {
            x += this.widthOrig - this.offsetX - this.width;
        } else {
            x += this.offsetX;
        }

        y += this.offsetY;
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.walls.lightingOldDebug.getValue()) {
            d = -16776961;
            u = -16776961;
            d2 = -16777024;
            u2 = -16777024;
        }

        float xPad = 0.0F;
        float yPad = 0.0F;
        int SCL = Core.tileScale;
        float x1 = x - 6.0F - 0.0F;
        float y1 = y - 96 * SCL - 3.0F - 0.0F;
        float x2 = x + width / 2.0F + 0.0F;
        float y2 = y - 96 * SCL + height / 2.0F - 0.0F;
        float x3 = x + width / 2.0F + 0.0F;
        float y3 = y + height / 2.0F + 5.0F + 0.0F;
        float x4 = x - 6.0F - 0.0F;
        float y4 = y + 2.0F + 0.0F;
        SpriteRenderer.instance.render(this, x1, y1, x2, y2, x3, y3, x4, y4, u2, d2, d, u);
    }

    public void renderstrip(int x, int y, int width, int height, float r, float g, float b, float a, Consumer<TextureDraw> texdModifier) {
        if (!(a <= 0.0F)) {
            r = PZMath.clamp(r, 0.0F, 1.0F);
            g = PZMath.clamp(g, 0.0F, 1.0F);
            b = PZMath.clamp(b, 0.0F, 1.0F);
            a = PZMath.clamp(a, 0.0F, 1.0F);
            if (this.flip) {
                x = (int)(x + (this.widthOrig - this.offsetX - this.width));
            } else {
                x = (int)(x + this.offsetX);
            }

            y = (int)(y + this.offsetY);

            try {
                SpriteRenderer.instance.renderi(this, x, y, width, height, r, g, b, a, texdModifier);
            } catch (Exception var11) {
                doingQuad = false;
                Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, null, var11);
            }
        }
    }

    /**
     * Description copied from interface: ITexture
     * 
     * @param red color used in the test
     * @param green color used in the test
     * @param blue color used in the test
     * @param alpha the alpha color that will be setted to the pixel that pass the test
     */
    @Override
    public void setAlphaForeach(int red, int green, int blue, int alpha) {
        ImageData imageData = this.getTextureId().getImageData();
        if (imageData != null) {
            imageData.makeTransp((byte)red, (byte)green, (byte)blue, (byte)alpha);
        } else {
            WrappedBuffer wb = this.getData();
            this.setData(ImageUtils.makeTransp(wb.getBuffer(), red, green, blue, alpha, this.getWidthHW(), this.getHeightHW()));
            wb.dispose();
        }

        AlphaColorIndex alphaColorIndex = new AlphaColorIndex(red, green, blue, alpha);
        if (this.dataid.alphaList == null) {
            this.dataid.alphaList = new ArrayList<>();
        }

        if (!this.dataid.alphaList.contains(alphaColorIndex)) {
            this.dataid.alphaList.add(alphaColorIndex);
        }
    }

    public void setCustomizedTexture() {
        this.dataid.pathFileName = null;
    }

    public void setNameOnly(String name) {
        this.name = name;
    }

    /**
     * Description copied from interface: ITexture
     * 
     * @param x xstart position
     * @param y ystart position
     * @param width width of the region
     * @param height height of the region
     */
    @Override
    public void setRegion(int x, int y, int width, int height) {
        if (x >= 0 && x <= this.getWidthHW() && y >= 0 && y <= this.getHeightHW() && width > 0 && height > 0) {
            if (width + x > this.getWidthHW()) {
                width = this.getWidthHW() - x;
            }

            if (height > this.getHeightHW()) {
                height = this.getHeightHW() - y;
            }

            this.xStart = (float)x / this.getWidthHW();
            this.yStart = (float)y / this.getHeightHW();
            this.xEnd = (float)(x + width) / this.getWidthHW();
            this.yEnd = (float)(y + height) / this.getHeightHW();
            this.width = width;
            this.height = height;
        }
    }

    public Texture splitIcon() {
        if (this.splitIconTex == null) {
            if (!this.dataid.isReady()) {
                this.splitIconTex = new Texture();
                this.splitIconTex.name = this.name + "_Icon";
                this.splitIconTex.dataid = this.dataid;
                this.splitIconTex.dataid.referenceCount++;
                this.splitIconTex.splitX = this.splitX;
                this.splitIconTex.splitY = this.splitY;
                this.splitIconTex.splitW = this.splitW;
                this.splitIconTex.splitH = this.splitH;
                this.splitIconTex.width = this.width;
                this.splitIconTex.height = this.height;
                this.splitIconTex.offsetX = 0.0F;
                this.splitIconTex.offsetY = 0.0F;
                this.splitIconTex.widthOrig = 0;
                this.splitIconTex.heightOrig = 0;
                this.splitIconTex.addDependency(this.dataid);
                setSharedTextureInternal(this.splitIconTex.name, this.splitIconTex);
                return this.splitIconTex;
            }

            this.splitIconTex = new Texture(this.getTextureId(), this.name + "_Icon");
            float tx = this.xStart * this.getWidthHW();
            float ty = this.yStart * this.getHeightHW();
            float tEx = this.xEnd * this.getWidthHW() - tx;
            float tEy = this.yEnd * this.getHeightHW() - ty;
            this.splitIconTex.setRegion((int)tx, (int)ty, (int)tEx, (int)tEy);
            this.splitIconTex.offsetX = 0.0F;
            this.splitIconTex.offsetY = 0.0F;
            setSharedTextureInternal(this.name + "_Icon", this.splitIconTex);
        }

        return this.splitIconTex;
    }

    public Texture split(int xOffset, int yOffset, int width, int height) {
        Texture tex = new Texture(this.getTextureId(), this.name + "_" + xOffset + "_" + yOffset);
        this.splitX = xOffset;
        this.splitY = yOffset;
        this.splitW = width;
        this.splitH = height;
        if (this.getTextureId().isReady()) {
            tex.setRegion(xOffset, yOffset, width, height);
        } else {
            assert false;
        }

        return tex;
    }

    public Texture split(String name, int xOffset, int yOffset, int width, int height) {
        Texture tex = new Texture(this.getTextureId(), name);
        tex.setRegion(xOffset, yOffset, width, height);
        return tex;
    }

    public Texture[] split(int xOffset, int yOffset, int row, int coloumn, int width, int height, int spaceX, int spaceY) {
        Texture[] temp = new Texture[row * coloumn];

        for (int y = 0; y < row; y++) {
            for (int x = 0; x < coloumn; x++) {
                temp[x + y * coloumn] = new Texture(this.getTextureId(), this.name + "_" + row + "_" + coloumn);
                temp[x + y * coloumn].setRegion(xOffset + x * width + spaceX * x, yOffset + y * height + spaceY * y, width, height);
                temp[x + y * coloumn].copyMaskRegion(this, xOffset + x * width + spaceX * x, yOffset + y * height + spaceY * y, width, height);
            }
        }

        return temp;
    }

    public Texture[][] split2D(int[] xstep, int[] ystep) {
        if (xstep != null && ystep != null) {
            Texture[][] texts = new Texture[xstep.length][ystep.length];
            float oldy = 0.0F;
            float newH = 0.0F;

            for (int y = 0; y < ystep.length; y++) {
                oldy += newH;
                newH = (float)ystep[y] / this.getHeightHW();
                float oldx = 0.0F;

                for (int x = 0; x < xstep.length; x++) {
                    float newW = (float)xstep[x] / this.getWidthHW();
                    Texture ttext = texts[x][y] = new Texture(this);
                    ttext.width = xstep[x];
                    ttext.height = ystep[y];
                    ttext.xStart = oldx;
                    ttext.xEnd = oldx += newW;
                    ttext.yStart = oldy;
                    ttext.yEnd = oldy + newH;
                }
            }

            return texts;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{ name:\"" + this.name + "\", w:" + this.getWidth() + ", h:" + this.getHeight() + " }";
    }

    public void saveMask(String name) {
        this.mask.save(name);
    }

    public void saveToZomboidDirectory(String filename) {
        if (!StringUtils.containsDoubleDot(filename)) {
            String filename2 = ZomboidFileSystem.instance.getCacheDirSub(filename);
            RenderThread.invokeOnRenderContext(() -> this.saveOnRenderThread(filename2));
        }
    }

    public void saveToCurrentSavefileDirectory(String filename) {
        if (!StringUtils.containsDoubleDot(filename)) {
            String filename2 = ZomboidFileSystem.instance.getFileNameInCurrentSave(filename);
            RenderThread.invokeOnRenderContext(() -> this.saveOnRenderThread(filename2));
        }
    }

    public void saveOnRenderThread(String filename) {
        if (this.getID() == -1) {
            throw new IllegalStateException("texture hasn't been uploaded to the GPU");
        } else {
            GL11.glPixelStorei(3333, 1);
            GL13.glActiveTexture(33984);
            GL11.glEnable(3553);
            this.bind();
            int width = this.getWidth();
            int height = this.getHeight();
            int widthHW = this.getWidthHW();
            int heightHW = this.getHeightHW();
            int bpp = 4;
            ByteBuffer buffer = MemoryUtil.memAlloc(widthHW * heightHW * 4);
            GL21.glGetTexImage(3553, 0, 6408, 5121, buffer);
            int[] pixels = new int[width * height];
            int x1 = (int)PZMath.floor(this.getXStart() * widthHW);
            int y1 = (int)PZMath.floor(this.getYStart() * heightHW);

            for (int i = 0; i < pixels.length; i++) {
                int col = x1 + i % width;
                int row = y1 + i / width;
                int bindex = (col + row * widthHW) * 4;
                pixels[i] = (buffer.get(bindex + 3) & 255) << 24
                    | (buffer.get(bindex) & 255) << 16
                    | (buffer.get(bindex + 1) & 255) << 8
                    | (buffer.get(bindex + 2) & 255) << 0;
            }

            MemoryUtil.memFree(buffer);
            BufferedImage image = new BufferedImage(width, height, 2);
            image.setRGB(0, 0, width, height, pixels, 0, width);

            try {
                File file = new File(filename);
                file.getParentFile().mkdirs();
                ImageIO.write(image, "png", file);
            } catch (IOException var15) {
                ExceptionLogger.logException(var15);
            }

            SpriteRenderer.ringBuffer.restoreBoundTextures = true;
        }
    }

    public void loadMaskRegion(ByteBuffer cache) {
        if (cache != null) {
            this.mask = new Mask();
            this.mask.mask = new BooleanGrid(this.width, this.height);
            this.mask.mask.LoadFromByteBuffer(cache);
        }
    }

    public void saveMaskRegion(ByteBuffer cache) {
        if (cache != null) {
            this.mask.mask.PutToByteBuffer(cache);
        }
    }

    public int getRealWidth() {
        return this.realWidth;
    }

    public void setRealWidth(int realWidth) {
        this.realWidth = realWidth;
    }

    public int getRealHeight() {
        return this.realHeight;
    }

    public void setRealHeight(int realHeight) {
        this.realHeight = realHeight;
    }

    public Vector2 getUVScale(Vector2 uvScale) {
        uvScale.set(1.0F, 1.0F);
        if (this.dataid == null) {
            return uvScale;
        } else {
            if (this.dataid.heightHw != this.dataid.height || this.dataid.widthHw != this.dataid.width) {
                uvScale.x = (float)this.dataid.width / this.dataid.widthHw;
                uvScale.y = (float)this.dataid.height / this.dataid.heightHw;
            }

            return uvScale;
        }
    }

    private void syncReadSize() {
        PNGSize pngSize = Texture.pngSize.get();
        pngSize.readSize(this.name);
        this.width = pngSize.width;
        this.height = pngSize.height;
    }

    @Override
    public AssetType getType() {
        return ASSET_TYPE;
    }

    @Override
    public void onBeforeReady() {
        if (this.assetParams != null) {
            this.assetParams.subTexture = null;
            this.assetParams = null;
        }

        this.solid = this.dataid.solid;
        if (this.splitX == -1) {
            this.width = this.dataid.width;
            this.height = this.dataid.height;
            this.xEnd = (float)this.width / this.dataid.widthHw;
            this.yEnd = (float)this.height / this.dataid.heightHw;
            if (this.dataid.mask != null) {
                this.createMask(this.dataid.mask);
            }
        } else {
            this.setRegion(this.splitX, this.splitY, this.splitW, this.splitH);
            if (this.dataid.mask != null) {
                this.mask = new Mask(this.dataid.mask, this.splitX, this.splitY, this.splitW, this.splitH);
            }
        }
    }

    public static void collectAllIcons(HashMap<String, String> map, HashMap<String, String> mapFull) {
        for (Entry<String, Texture> entry : s_sharedTextureTable.entrySet()) {
            if (entry.getKey().startsWith("media/ui/Container_") || entry.getKey().startsWith("Item_")) {
                String val = "";
                if (entry.getKey().startsWith("Item_")) {
                    val = entry.getKey().replaceFirst("Item_", "");
                } else if (entry.getKey().startsWith("media/ui/Container_")) {
                    String var5 = entry.getKey().replaceFirst("media/ui/Container_", "");
                    val = var5.replaceAll("\\.png", "");
                    DebugLog.log("Adding " + val.toLowerCase() + ", value = " + entry.getKey());
                }

                map.put(val.toLowerCase(), val);
                mapFull.put(val.toLowerCase(), entry.getKey());
            }
        }
    }

    public static final class TextureAssetParams extends AssetManager.AssetParams {
        int flags = 0;
        FileSystem.SubTexture subTexture;
    }
}
