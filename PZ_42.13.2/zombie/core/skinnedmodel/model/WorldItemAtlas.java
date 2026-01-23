// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Stack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import zombie.characterTextures.ItemSmartTexture;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.ImmutableColor;
import zombie.core.PerformanceSettings;
import zombie.core.ShaderHelper;
import zombie.core.SpriteRenderer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.VBORenderer;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.ClothingItem;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TextureCombiner;
import zombie.core.textures.TextureDraw;
import zombie.core.textures.TextureFBO;
import zombie.debug.DebugOptions;
import zombie.entity.components.fluids.FluidContainer;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponPart;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoGridSquare;
import zombie.iso.PlayerCamera;
import zombie.popman.ObjectPool;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ModelWeaponPart;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class WorldItemAtlas {
    public static final int ATLAS_SIZE = 512;
    public static final int MATRIX_SIZE = 1024;
    private static final float MAX_ZOOM = 2.5F;
    private TextureFBO fbo;
    public static final WorldItemAtlas instance = new WorldItemAtlas();
    private final HashMap<String, WorldItemAtlas.ItemTexture> itemTextureMap = new HashMap<>();
    private final ArrayList<WorldItemAtlas.Atlas> atlasList = new ArrayList<>();
    private final WorldItemAtlas.ItemParams itemParams = new WorldItemAtlas.ItemParams();
    private final WorldItemAtlas.Checksummer checksummer = new WorldItemAtlas.Checksummer();
    private static final Stack<WorldItemAtlas.RenderJob> JobPool = new Stack<>();
    private final ArrayList<WorldItemAtlas.RenderJob> renderJobs = new ArrayList<>();
    private final ObjectPool<WorldItemAtlas.ItemTextureDrawer> itemTextureDrawerPool = new ObjectPool<>(WorldItemAtlas.ItemTextureDrawer::new);
    private final ObjectPool<WorldItemAtlas.ItemTextureDepthDrawer> itemTextureDepthDrawerPool = new ObjectPool<>(WorldItemAtlas.ItemTextureDepthDrawer::new);
    private final ObjectPool<WorldItemAtlas.WeaponPartParams> weaponPartParamPool = new ObjectPool<>(WorldItemAtlas.WeaponPartParams::new);
    private static final Matrix4f s_attachmentXfrm = new Matrix4f();
    private static final ImmutableColor ROTTEN_FOOD_COLOR = new ImmutableColor(0.5F, 0.5F, 0.5F);

    public WorldItemAtlas.ItemTexture getItemTexture(InventoryItem item, boolean bHighRes) {
        return this.itemParams.init(item, bHighRes) ? this.getItemTexture(this.itemParams) : null;
    }

    public WorldItemAtlas.ItemTexture getItemTexture(WorldItemAtlas.ItemParams params) {
        String key = this.getItemKey(params);
        WorldItemAtlas.ItemTexture itemTexture = this.itemTextureMap.get(key);
        if (itemTexture != null) {
            return itemTexture;
        } else {
            WorldItemAtlas.AtlasEntry entry = new WorldItemAtlas.AtlasEntry();
            entry.key = key;
            itemTexture = new WorldItemAtlas.ItemTexture();
            itemTexture.itemParams.copyFrom(params);
            itemTexture.entry = entry;
            this.itemTextureMap.put(key, itemTexture);
            this.renderJobs.add(WorldItemAtlas.RenderJob.getNew().init(params, entry));
            return itemTexture;
        }
    }

    private void assignEntryToAtlas(WorldItemAtlas.AtlasEntry entry, int entryW, int entryH) {
        if (entry.atlas == null) {
            for (int i = 0; i < this.atlasList.size(); i++) {
                WorldItemAtlas.Atlas atlas = this.atlasList.get(i);
                if (!atlas.isFull() && atlas.entryWidth == entryW && atlas.entryHeight == entryH) {
                    atlas.addEntry(entry);
                    return;
                }
            }

            WorldItemAtlas.Atlas atlas = new WorldItemAtlas.Atlas(512, 512, entryW, entryH);
            atlas.addEntry(entry);
            this.atlasList.add(atlas);
        }
    }

    private String getItemKey(WorldItemAtlas.ItemParams params) {
        try {
            this.checksummer.reset();
            this.checksummer.update(params.model.name);
            if (params.weaponParts != null) {
                for (int i = 0; i < params.weaponParts.size(); i++) {
                    WorldItemAtlas.WeaponPartParams partParams = params.weaponParts.get(i);
                    this.checksummer.update(partParams.model.name);
                }
            }

            this.checksummer.update((int)(params.worldScale * params.modelScriptScale * 1000.0F));
            this.checksummer.update((byte)(params.tintR * 255.0F));
            this.checksummer.update((byte)(params.tintG * 255.0F));
            this.checksummer.update((byte)(params.tintB * 255.0F));
            this.checksummer.update((int)(params.angle.x * 1000.0F));
            this.checksummer.update((int)(params.angle.y * 1000.0F));
            this.checksummer.update((int)(params.angle.z * 1000.0F));
            this.checksummer.update((byte)params.foodState.ordinal());
            this.checksummer.update(params.maxZoomIsOne);
            this.checksummer.update(params.fluidFilled);
            this.checksummer.update((int)(params.bloodLevel * 1000.0F));
            this.checksummer.update((int)(params.fluidLevel * 100.0F));
            this.checksummer.update((byte)params.fluidTint.getRedByte());
            this.checksummer.update((byte)params.fluidTint.getBlueByte());
            this.checksummer.update((byte)params.fluidTint.getGreenByte());
            return this.checksummer.checksumToString();
        } catch (Throwable var4) {
            ExceptionLogger.logException(var4);
            return "bogus";
        }
    }

    public void render() {
        for (int i = 0; i < this.atlasList.size(); i++) {
            WorldItemAtlas.Atlas atlas = this.atlasList.get(i);
            if (atlas.clear) {
                SpriteRenderer.instance.drawGeneric(new WorldItemAtlas.ClearAtlasTexture(atlas));
            }
        }

        if (!this.renderJobs.isEmpty()) {
            for (int ix = 0; ix < this.renderJobs.size(); ix++) {
                WorldItemAtlas.RenderJob job = this.renderJobs.get(ix);
                if (job.done != 1 || job.renderRefCount <= 0) {
                    if (job.done == 1 && job.renderRefCount == 0) {
                        this.renderJobs.remove(ix--);

                        assert !JobPool.contains(job);

                        JobPool.push(job);
                    } else {
                        job.entry.renderMainOk = job.renderMain();
                        if (job.entry.renderMainOk) {
                            job.renderRefCount++;
                            SpriteRenderer.instance.drawGeneric(job);
                        }
                    }
                }
            }
        }
    }

    public void renderUI() {
        if (DebugOptions.instance.worldItemAtlas.render.getValue() && GameKeyboard.isKeyPressed(209)) {
            this.Reset();
        }

        if (DebugOptions.instance.worldItemAtlas.render.getValue()) {
            boolean bDepth = false;
            int d = 2560 / Core.tileScale;
            d /= 2;
            int x = 0;
            int y = 0;

            for (int i = 0; i < this.atlasList.size(); i++) {
                WorldItemAtlas.Atlas atlas = this.atlasList.get(i);
                Texture tex = atlas.tex;
                SpriteRenderer.instance.renderi(null, x, y, d, d, 1.0F, 1.0F, 1.0F, 0.75F, null);
                SpriteRenderer.instance.renderi(tex, x, y, d, d, 1.0F, 1.0F, 1.0F, 1.0F, null);
                float scale = (float)d / tex.getWidth();

                for (int xx = 0; xx <= tex.getWidth() / atlas.entryWidth; xx++) {
                    SpriteRenderer.instance
                        .renderline(null, (int)(x + xx * atlas.entryWidth * scale), y, (int)(x + xx * atlas.entryWidth * scale), y + d, 0.5F, 0.5F, 0.5F, 1.0F);
                }

                for (int yy = 0; yy <= tex.getHeight() / atlas.entryHeight; yy++) {
                    SpriteRenderer.instance
                        .renderline(
                            null,
                            x,
                            (int)(y + d - yy * atlas.entryHeight * scale),
                            x + d,
                            (int)(y + d - yy * atlas.entryHeight * scale),
                            0.5F,
                            0.5F,
                            0.5F,
                            1.0F
                        );
                }

                y += d;
                if (y + d > Core.getInstance().getScreenHeight()) {
                    y = 0;
                    x += d;
                }
            }
        }
    }

    public void Reset() {
        if (this.fbo != null) {
            this.fbo.destroyLeaveTexture();
            this.fbo = null;
        }

        this.atlasList.forEach(WorldItemAtlas.Atlas::Reset);
        this.atlasList.clear();
        this.itemTextureMap.values().forEach(WorldItemAtlas.ItemTexture::Reset);
        this.itemTextureMap.clear();
        JobPool.forEach(WorldItemAtlas.RenderJob::Reset);
        JobPool.clear();
        this.renderJobs.clear();
    }

    private final class Atlas {
        public final int entryWidth;
        public final int entryHeight;
        public Texture tex;
        public Texture depth;
        public final ArrayList<WorldItemAtlas.AtlasEntry> entryList;
        public boolean clear;

        public Atlas(final int w, final int h, final int entryW, final int entryH) {
            Objects.requireNonNull(WorldItemAtlas.this);
            super();
            this.entryList = new ArrayList<>();
            this.clear = true;
            this.entryWidth = entryW;
            this.entryHeight = entryH;
            this.tex = new Texture(w, h, 16);
            this.depth = new Texture(w, h, 512);
            if (WorldItemAtlas.this.fbo == null) {
                WorldItemAtlas.this.fbo = new TextureFBO(this.tex, this.depth, false);
            }
        }

        public boolean isFull() {
            int columns = this.tex.getWidth() / this.entryWidth;
            int rows = this.tex.getHeight() / this.entryHeight;
            return this.entryList.size() >= columns * rows;
        }

        public WorldItemAtlas.AtlasEntry addItem(String key) {
            int columns = this.tex.getWidth() / this.entryWidth;
            int index = this.entryList.size();
            int col = index % columns;
            int row = index / columns;
            WorldItemAtlas.AtlasEntry entry = new WorldItemAtlas.AtlasEntry();
            entry.atlas = this;
            entry.key = key;
            entry.x = col * this.entryWidth;
            entry.y = row * this.entryHeight;
            entry.w = this.entryWidth;
            entry.h = this.entryHeight;
            entry.tex = this.tex.split(key, entry.x, this.tex.getHeight() - (entry.y + this.entryHeight), entry.w, entry.h);
            entry.tex.setName(key);
            this.entryList.add(entry);
            return entry;
        }

        public void addEntry(WorldItemAtlas.AtlasEntry entry) {
            int columns = this.tex.getWidth() / this.entryWidth;
            int index = this.entryList.size();
            int col = index % columns;
            int row = index / columns;
            entry.atlas = this;
            entry.x = col * this.entryWidth;
            entry.y = row * this.entryHeight;
            entry.w = this.entryWidth;
            entry.h = this.entryHeight;
            entry.tex = this.tex.split(entry.key, entry.x, this.tex.getHeight() - (entry.y + this.entryHeight), entry.w, entry.h);
            entry.tex.setName(entry.key);
            this.entryList.add(entry);
        }

        public void Reset() {
            this.entryList.forEach(WorldItemAtlas.AtlasEntry::Reset);
            this.entryList.clear();
            if (!this.tex.isDestroyed()) {
                RenderThread.invokeOnRenderContext(() -> GL11.glDeleteTextures(this.tex.getID()));
            }

            this.tex = null;
            if (!this.depth.isDestroyed()) {
                RenderThread.invokeOnRenderContext(() -> GL11.glDeleteTextures(this.depth.getID()));
            }

            this.depth = null;
        }
    }

    private static final class AtlasEntry {
        public WorldItemAtlas.Atlas atlas;
        public String key;
        public int x;
        public int y;
        public int w;
        public int h;
        public float offsetX;
        public float offsetY;
        public Texture tex;
        public boolean ready;
        public boolean renderMainOk;
        public boolean tooBig;

        public void Reset() {
            this.atlas = null;
            this.tex.destroy();
            this.tex = null;
            this.ready = false;
            this.renderMainOk = false;
            this.tooBig = false;
        }
    }

    private static final class Checksummer {
        private MessageDigest md;
        private final StringBuilder sb = new StringBuilder();

        public void reset() throws NoSuchAlgorithmException {
            if (this.md == null) {
                this.md = MessageDigest.getInstance("MD5");
            }

            this.md.reset();
        }

        public void update(byte b) {
            this.md.update(b);
        }

        public void update(boolean b) {
            this.md.update((byte)(b ? 1 : 0));
        }

        public void update(int i) {
            this.md.update((byte)(i & 0xFF));
            this.md.update((byte)(i >> 8 & 0xFF));
            this.md.update((byte)(i >> 16 & 0xFF));
            this.md.update((byte)(i >> 24 & 0xFF));
        }

        public void update(String add) {
            if (add != null && !add.isEmpty()) {
                this.md.update(add.getBytes());
            }
        }

        public void update(ImmutableColor color) {
            this.update((byte)(color.r * 255.0F));
            this.update((byte)(color.g * 255.0F));
            this.update((byte)(color.b * 255.0F));
        }

        public void update(IsoGridSquare.ResultLight ls, float x, float y, float z) {
            if (ls != null && ls.radius > 0) {
                this.update((int)(ls.x - x));
                this.update((int)(ls.y - y));
                this.update((int)(ls.z - z));
                this.update((byte)(ls.r * 255.0F));
                this.update((byte)(ls.g * 255.0F));
                this.update((byte)(ls.b * 255.0F));
                this.update((byte)ls.radius);
            }
        }

        public String checksumToString() {
            byte[] mdbytes = this.md.digest();
            this.sb.setLength(0);

            for (int i = 0; i < mdbytes.length; i++) {
                this.sb.append(mdbytes[i] & 255);
            }

            return this.sb.toString();
        }
    }

    private static final class ClearAtlasTexture extends TextureDraw.GenericDrawer {
        WorldItemAtlas.Atlas atlas;

        ClearAtlasTexture(WorldItemAtlas.Atlas atlas) {
            this.atlas = atlas;
        }

        @Override
        public void render() {
            TextureFBO fbo = WorldItemAtlas.instance.fbo;
            if (fbo != null && this.atlas.tex != null) {
                if (this.atlas.clear) {
                    if (fbo.getTexture() != this.atlas.tex) {
                        fbo.setTexture(this.atlas.tex);
                    }

                    fbo.startDrawing(false, false);
                    GL11.glPushAttrib(2048);
                    GL11.glViewport(0, 0, fbo.getWidth(), fbo.getHeight());
                    Matrix4f PROJECTION = Core.getInstance().projectionMatrixStack.alloc();
                    int texWid = this.atlas.tex.getWidth();
                    int texHgt = this.atlas.tex.getHeight();
                    PROJECTION.setOrtho2D(0.0F, texWid, texHgt, 0.0F);
                    Core.getInstance().projectionMatrixStack.push(PROJECTION);
                    Matrix4f MODELVIEW = Core.getInstance().modelViewMatrixStack.alloc();
                    MODELVIEW.identity();
                    Core.getInstance().modelViewMatrixStack.push(MODELVIEW);
                    GL11.glDisable(3089);
                    GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
                    GL11.glClear(16640);
                    GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
                    fbo.endDrawing();
                    GL11.glEnable(3089);
                    Core.getInstance().projectionMatrixStack.pop();
                    Core.getInstance().modelViewMatrixStack.pop();
                    GL11.glPopAttrib();
                    this.atlas.clear = false;
                }
            }
        }
    }

    private static final class ItemParams {
        float worldScale = 1.0F;
        float worldZRotation;
        float worldYRotation;
        float worldXRotation;
        WorldItemAtlas.ItemParams.FoodState foodState = WorldItemAtlas.ItemParams.FoodState.Normal;
        private Model model;
        private float modelScriptScale = 1.0F;
        private ArrayList<WorldItemAtlas.WeaponPartParams> weaponParts;
        private float hue;
        private float tintR;
        private float tintG;
        private float tintB;
        private final Vector3f angle = new Vector3f();
        private final Matrix4f transform = new Matrix4f();
        private float ambientR = 1.0F;
        private float ambientG = 1.0F;
        private float ambientB = 1.0F;
        private final float alpha = 1.0F;
        private float bloodLevel;
        private float fluidLevel;
        private boolean maxZoomIsOne;
        private boolean fluidFilled;
        private String modelTextureName;
        private String fluidTextureMask;
        private String tintMask;
        private final ItemSmartTexture smartTexture = new ItemSmartTexture(null);
        private final Color fluidTint = new Color(Color.white);

        ItemParams() {
        }

        void copyFrom(WorldItemAtlas.ItemParams params) {
            this.worldScale = params.worldScale;
            this.worldZRotation = params.worldZRotation;
            this.worldYRotation = params.worldYRotation;
            this.worldXRotation = params.worldXRotation;
            this.foodState = params.foodState;
            this.fluidFilled = params.fluidFilled;
            this.modelTextureName = params.modelTextureName;
            this.fluidTextureMask = params.fluidTextureMask;
            this.tintMask = params.tintMask;
            this.model = params.model;
            this.modelScriptScale = params.modelScriptScale;
            if (this.weaponParts != null) {
                WorldItemAtlas.instance.weaponPartParamPool.release(this.weaponParts);
                this.weaponParts.clear();
            }

            if (params.weaponParts != null) {
                if (this.weaponParts == null) {
                    this.weaponParts = new ArrayList<>();
                }

                for (int i = 0; i < params.weaponParts.size(); i++) {
                    WorldItemAtlas.WeaponPartParams partParams = params.weaponParts.get(i);
                    this.weaponParts.add(WorldItemAtlas.instance.weaponPartParamPool.alloc().init(partParams));
                }
            }

            this.hue = params.hue;
            this.tintR = params.tintR;
            this.tintG = params.tintG;
            this.tintB = params.tintB;
            this.angle.set(params.angle);
            this.transform.set(params.transform);
            this.bloodLevel = params.bloodLevel;
            this.fluidLevel = params.fluidLevel;
            this.fluidTint.set(params.fluidTint);
            this.maxZoomIsOne = params.maxZoomIsOne;
        }

        boolean init(InventoryItem item, boolean bHighRes) {
            this.Reset();
            this.worldScale = item.worldScale;
            this.worldZRotation = item.worldZRotation;
            this.worldYRotation = item.worldYRotation;
            this.worldXRotation = item.worldXRotation;
            this.maxZoomIsOne = bHighRes;
            float flipAngle = 0.0F;
            String worldStaticItem = StringUtils.discardNullOrWhitespace(item.getWorldStaticItem());
            if (worldStaticItem != null) {
                ModelScript modelScript = ScriptManager.instance.getModelScript(worldStaticItem);
                if (modelScript == null) {
                    return false;
                } else {
                    String meshName = modelScript.getMeshName();
                    String texName = modelScript.getTextureName();
                    String shaderName = modelScript.getShaderName();
                    ImmutableColor tint = new ImmutableColor(item.getColorRed(), item.getColorGreen(), item.getColorBlue(), 1.0F);
                    float hue = 1.0F;
                    this.initFluidContainer(item);
                    if (this.fluidFilled) {
                        ModelScript modelScriptFluid = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "_Fluid");
                        if (modelScriptFluid != null) {
                            texName = modelScriptFluid.getTextureName();
                            meshName = modelScriptFluid.getMeshName();
                            shaderName = modelScriptFluid.getShaderName();
                            modelScript = modelScriptFluid;
                        }
                    }

                    if (item instanceof Food food) {
                        this.foodState = this.getFoodState(food);
                        if (food.isCooked()) {
                            ModelScript modelScriptCooked = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "Cooked");
                            if (modelScriptCooked != null) {
                                texName = modelScriptCooked.getTextureName();
                                meshName = modelScriptCooked.getMeshName();
                                shaderName = modelScriptCooked.getShaderName();
                                modelScript = modelScriptCooked;
                            }
                        }

                        if (food.isBurnt()) {
                            ModelScript modelScriptBurnt = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "Burnt");
                            if (modelScriptBurnt != null) {
                                texName = modelScriptBurnt.getTextureName();
                                meshName = modelScriptBurnt.getMeshName();
                                shaderName = modelScriptBurnt.getShaderName();
                                modelScript = modelScriptBurnt;
                            }
                        }

                        if (food.isRotten()) {
                            ModelScript modelScriptRotten = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "Rotten");
                            if (modelScriptRotten != null) {
                                texName = modelScriptRotten.getTextureName();
                                meshName = modelScriptRotten.getMeshName();
                                shaderName = modelScriptRotten.getShaderName();
                                modelScript = modelScriptRotten;
                            } else {
                                tint = WorldItemAtlas.ROTTEN_FOOD_COLOR;
                            }
                        }
                    }

                    Clothing clothing = Type.tryCastTo(item, Clothing.class);
                    if (clothing != null || item.getClothingItem() != null) {
                        String clothingTexture = modelScript.getTextureName(true);
                        ItemVisual itemVisual = item.getVisual();
                        ClothingItem clothingItem = item.getClothingItem();
                        ImmutableColor clothingTint = itemVisual.getTint(clothingItem);
                        if (clothingTexture == null) {
                            if (clothingItem.textureChoices.isEmpty()) {
                                clothingTexture = itemVisual.getBaseTexture(clothingItem);
                            } else {
                                clothingTexture = itemVisual.getTextureChoice(clothingItem);
                            }
                        }

                        if (clothingTexture != null) {
                            texName = clothingTexture;
                            tint = clothingTint;
                        }
                    }

                    this.modelTextureName = this.initTextureName(texName);
                    boolean bStatic = modelScript.isStatic;
                    Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, true);
                    if (model == null) {
                        ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                    }

                    model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
                    if (model != null && model.isReady() && model.mesh != null && model.mesh.isReady()) {
                        String tintMask = this.initTextureName(texName, "TINT");
                        Texture tintTexture = Texture.getSharedTexture(tintMask);
                        if (tintTexture != null) {
                            if (!tintTexture.isReady()) {
                                return false;
                            }

                            this.tintMask = tintMask;
                        }

                        if (this.fluidLevel > 0.0F && model.tex != null) {
                            Texture texture = Texture.getSharedTexture("media/textures/FullAlpha.png");
                            if (texture != null && !texture.isReady()) {
                                return false;
                            }

                            String textureMask = this.initTextureName(texName, "FLUIDTINT");
                            texture = Texture.getSharedTexture(textureMask);
                            if (texture != null) {
                                if (!texture.isReady()) {
                                    return false;
                                }

                                this.fluidTextureMask = textureMask;
                            }
                        }

                        this.init(item, model, modelScript, 1.0F, tint, 0.0F, false);
                        if (this.worldScale != 1.0F) {
                            this.transform.scale(modelScript.scale * this.worldScale);
                        } else if (modelScript.scale != 1.0F) {
                            this.transform.scale(modelScript.scale);
                        }

                        this.angle.x = this.worldXRotation;
                        this.angle.y = this.worldZRotation;
                        this.angle.z = this.worldYRotation;
                        return true;
                    } else {
                        return false;
                    }
                }
            } else if (item instanceof Clothing clothingx) {
                ClothingItem clothingItemx = item.getClothingItem();
                ItemVisual itemVisualx = item.getVisual();
                boolean bFemale = false;
                String meshNamex = clothingItemx.getModel(false);
                if (clothingItemx != null
                    && itemVisualx != null
                    && !StringUtils.isNullOrWhitespace(meshNamex)
                    && "Bip01_Head".equalsIgnoreCase(clothingItemx.attachBone)
                    && !clothingx.isCosmetic()
                    && !item.isBodyLocation(ItemBodyLocation.EYES)) {
                    String texNamex = itemVisualx.getTextureChoice(clothingItemx);
                    boolean bStaticx = clothingItemx.isStatic;
                    String shaderNamex = clothingItemx.shader;
                    this.modelTextureName = this.initTextureName(texNamex);
                    Model modelx = ModelManager.instance.tryGetLoadedModel(meshNamex, texNamex, bStaticx, shaderNamex, false);
                    if (modelx == null) {
                        ModelManager.instance.loadAdditionalModel(meshNamex, texNamex, bStaticx, shaderNamex);
                    }

                    modelx = ModelManager.instance.getLoadedModel(meshNamex, texNamex, bStaticx, shaderNamex);
                    if (modelx != null && modelx.isReady() && modelx.mesh != null && modelx.mesh.isReady()) {
                        float huex = itemVisualx.getHue(clothingItemx);
                        ImmutableColor tintx = itemVisualx.getTint(clothingItemx);
                        this.init(item, modelx, null, huex, tintx, 0.0F, false);
                        this.angle.x = 0.0F + this.worldXRotation;
                        if (this.angle.x > 360.0F) {
                            this.angle.x -= 360.0F;
                        }

                        this.angle.y = this.worldZRotation;
                        this.angle.z = this.worldYRotation;
                        if (this.angle.z > 360.0F) {
                            this.angle.z -= 360.0F;
                        }

                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (item instanceof HandWeapon handWeapon) {
                String weaponStaticModel = StringUtils.discardNullOrWhitespace(handWeapon.getStaticModel());
                if (weaponStaticModel == null) {
                    return false;
                } else {
                    ModelScript modelScript = ScriptManager.instance.getModelScript(weaponStaticModel);
                    if (modelScript == null) {
                        return false;
                    } else {
                        String meshNamex = modelScript.getMeshName();
                        String texNamexx = modelScript.getTextureName();
                        String shaderNamexx = modelScript.getShaderName();
                        boolean bStaticxx = modelScript.isStatic;
                        this.modelTextureName = this.initTextureName(texNamexx);
                        this.initFluidContainer(item);
                        if (this.fluidFilled) {
                            ModelScript modelScriptFluid = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "_Fluid");
                            if (modelScriptFluid != null) {
                                texNamexx = modelScriptFluid.getTextureName();
                                meshNamex = modelScriptFluid.getMeshName();
                                shaderNamexx = modelScriptFluid.getShaderName();
                                modelScript = modelScriptFluid;
                            }
                        }

                        Model modelxx = ModelManager.instance.tryGetLoadedModel(meshNamex, texNamexx, bStaticxx, shaderNamexx, false);
                        if (modelxx == null) {
                            ModelManager.instance.loadAdditionalModel(meshNamex, texNamexx, bStaticxx, shaderNamexx);
                        }

                        modelxx = ModelManager.instance.getLoadedModel(meshNamex, texNamexx, bStaticxx, shaderNamexx);
                        if (modelxx != null && modelxx.isReady() && modelxx.mesh != null && modelxx.mesh.isReady()) {
                            this.bloodLevel = item.getBloodLevel();
                            float huexx = 1.0F;
                            ImmutableColor tintxx = new ImmutableColor(item.getColorRed(), item.getColorGreen(), item.getColorBlue(), 1.0F);
                            this.init(item, modelxx, modelScript, 1.0F, tintxx, 0.0F, true);
                            if (this.worldScale != 1.0F) {
                                this.transform.scale(modelScript.scale * this.worldScale);
                            } else if (modelScript.scale != 1.0F) {
                                this.transform.scale(modelScript.scale);
                            }

                            this.angle.x = 0.0F;
                            this.angle.y = this.worldZRotation;
                            this.angle.x = this.worldXRotation;
                            this.angle.z = this.worldYRotation;
                            return this.initWeaponParts(handWeapon, modelScript);
                        } else {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        }

        private void initFluidContainer(InventoryItem item) {
            FluidContainer fluidContainer = item.getFluidContainer();
            if (fluidContainer == null && item.getWorldItem() != null) {
                fluidContainer = item.getWorldItem().getFluidContainer();
            }

            if (fluidContainer != null) {
                this.fluidFilled = this.getFluidRatio(fluidContainer);
                this.fluidLevel = fluidContainer.getFilledRatio();
                this.fluidTint.set(fluidContainer.getColor());
            }
        }

        private boolean hasFluidContainerModel(InventoryItem item) {
            ModelScript modelScriptFluid = ScriptManager.instance.getModelScript(item.getWorldStaticItem() + "_Fluid");
            return modelScriptFluid != null;
        }

        private String initTextureName(String textureName) {
            return !textureName.contains("media/") && !textureName.contains("media\\") ? "media/textures/" + textureName + ".png" : textureName;
        }

        private String initTextureName(String textureName, String suffix) {
            if (textureName.endsWith(".png")) {
                textureName = textureName.substring(0, textureName.length() - 4);
            }

            return !textureName.contains("media/") && !textureName.contains("media\\")
                ? "media/textures/" + textureName + suffix + ".png"
                : textureName + suffix + ".png";
        }

        boolean initWeaponParts(HandWeapon weapon, ModelScript parentModelScript) {
            ArrayList<ModelWeaponPart> modelWeaponParts = weapon.getModelWeaponPart();
            if (modelWeaponParts == null) {
                return true;
            } else {
                List<WeaponPart> parts = weapon.getAllWeaponParts();

                for (int i = 0; i < parts.size(); i++) {
                    WeaponPart part = parts.get(i);

                    for (int j = 0; j < modelWeaponParts.size(); j++) {
                        ModelWeaponPart mwp = modelWeaponParts.get(j);
                        if (part.getFullType().equals(mwp.partType)) {
                            if (!this.initWeaponPart(mwp, parentModelScript)) {
                                return false;
                            }
                            break;
                        }
                    }
                }

                return true;
            }
        }

        boolean initWeaponPart(ModelWeaponPart mwp, ModelScript parentModelScript) {
            String weaponStaticModel = StringUtils.discardNullOrWhitespace(mwp.modelName);
            if (weaponStaticModel == null) {
                return false;
            } else {
                ModelScript modelScript = ScriptManager.instance.getModelScript(weaponStaticModel);
                if (modelScript == null) {
                    return false;
                } else {
                    String meshName = modelScript.getMeshName();
                    String texName = modelScript.getTextureName();
                    String shaderName = modelScript.getShaderName();
                    boolean bStatic = modelScript.isStatic;
                    Model model = ModelManager.instance.tryGetLoadedModel(meshName, texName, bStatic, shaderName, false);
                    if (model == null) {
                        ModelManager.instance.loadAdditionalModel(meshName, texName, bStatic, shaderName);
                    }

                    model = ModelManager.instance.getLoadedModel(meshName, texName, bStatic, shaderName);
                    if (model != null && model.isReady() && model.mesh != null && model.mesh.isReady()) {
                        WorldItemAtlas.WeaponPartParams partParams = WorldItemAtlas.instance.weaponPartParamPool.alloc();
                        partParams.model = model;
                        partParams.attachmentNameSelf = mwp.attachmentNameSelf;
                        partParams.attachmentNameParent = mwp.attachmentParent;
                        partParams.initTransform(parentModelScript, modelScript);
                        if (this.weaponParts == null) {
                            this.weaponParts = new ArrayList<>();
                        }

                        this.weaponParts.add(partParams);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        }

        void init(InventoryItem item, Model model, ModelScript modelScript, float hue, ImmutableColor tint, float flipAngle, boolean bWeaponFix) {
            this.model = model;
            this.modelScriptScale = modelScript == null ? 1.0F : modelScript.scale;
            this.tintR = tint.r;
            this.tintG = tint.g;
            this.tintB = tint.b;
            this.hue = hue;
            this.angle.set(0.0F);
            this.transform.identity();
            this.ambientR = this.ambientG = this.ambientB = 1.0F;
            if (bWeaponFix) {
                this.transform.rotateXYZ(0.0F, (float) Math.PI, (float) (Math.PI / 2));
            }

            if (modelScript != null) {
                ModelAttachment attachment = modelScript.getAttachmentById("world");
                if (attachment != null) {
                    ModelInstanceRenderData.makeAttachmentTransform(attachment, WorldItemAtlas.s_attachmentXfrm);
                    WorldItemAtlas.s_attachmentXfrm.invert();
                    this.transform.mul(WorldItemAtlas.s_attachmentXfrm);
                }
            } else {
                ClothingItem clothingItem = item.getClothingItem();
                ItemVisual itemVisual = item.getVisual();
                if (clothingItem != null
                    && itemVisual != null
                    && "Bip01_Head".equalsIgnoreCase(clothingItem.attachBone)
                    && (!((Clothing)item).isCosmetic() || item.isBodyLocation(ItemBodyLocation.EYES))) {
                    WorldItemAtlas.s_attachmentXfrm.translation(model.mesh.minXyz.x, 0.0F, 0.0F);
                    WorldItemAtlas.s_attachmentXfrm.rotateXYZ((float) (Math.PI / 2), 0.0F, (float) (-Math.PI / 2));
                    WorldItemAtlas.s_attachmentXfrm.invert();
                    this.transform.mul(WorldItemAtlas.s_attachmentXfrm);
                }
            }

            ModelInstanceRenderData.postMultiplyMeshTransform(this.transform, model.mesh);
        }

        Boolean getFluidRatio(FluidContainer fluidContainer) {
            return fluidContainer.getFilledRatio() > 0.5F ? true : false;
        }

        WorldItemAtlas.ItemParams.FoodState getFoodState(Food food) {
            WorldItemAtlas.ItemParams.FoodState state = WorldItemAtlas.ItemParams.FoodState.Normal;
            if (food.isCooked()) {
                state = WorldItemAtlas.ItemParams.FoodState.Cooked;
            }

            if (food.isBurnt()) {
                state = WorldItemAtlas.ItemParams.FoodState.Burnt;
            }

            if (food.isRotten()) {
                state = WorldItemAtlas.ItemParams.FoodState.Rotten;
            }

            return state;
        }

        boolean isStillValid(InventoryItem item, boolean bMaxZoomIsOne) {
            if (item.worldScale != this.worldScale
                || item.worldZRotation != this.worldZRotation
                || item.worldYRotation != this.worldYRotation
                || item.worldXRotation != this.worldXRotation) {
                return false;
            } else if (bMaxZoomIsOne != this.maxZoomIsOne) {
                return false;
            } else {
                FluidContainer fluidContainer = item.getFluidContainer();
                if (fluidContainer == null && item.getWorldItem() != null) {
                    fluidContainer = item.getWorldItem().getFluidContainer();
                }

                if (fluidContainer != null) {
                    if (this.getFluidRatio(fluidContainer) != this.fluidFilled) {
                        return false;
                    }

                    if ((int)(fluidContainer.getFilledRatio() * 100.0F) != (int)(this.fluidLevel * 100.0F)) {
                        return false;
                    }

                    if (!fluidContainer.getColor().equalBytes(this.fluidTint)) {
                        return false;
                    }
                }

                return item instanceof Food food && this.getFoodState(food) != this.foodState
                    ? false
                    : !(item instanceof HandWeapon weapon && weapon.getBloodLevel() != this.bloodLevel);
            }
        }

        void Reset() {
            this.model = null;
            this.modelScriptScale = 1.0F;
            this.foodState = WorldItemAtlas.ItemParams.FoodState.Normal;
            this.fluidFilled = false;
            this.modelTextureName = null;
            this.fluidTextureMask = null;
            this.tintMask = null;
            this.bloodLevel = 0.0F;
            this.fluidLevel = 0.0F;
            this.fluidTint.set(Color.white);
            if (this.weaponParts != null) {
                WorldItemAtlas.instance.weaponPartParamPool.release(this.weaponParts);
                this.weaponParts.clear();
            }
        }

        static enum FoodState {
            Normal,
            Cooked,
            Burnt,
            Rotten;
        }
    }

    public static final class ItemTexture {
        final WorldItemAtlas.ItemParams itemParams = new WorldItemAtlas.ItemParams();
        WorldItemAtlas.AtlasEntry entry;

        public boolean isStillValid(InventoryItem item, boolean bHighRes) {
            return this.entry == null ? false : this.itemParams.isStillValid(item, bHighRes);
        }

        public boolean isRenderMainOK() {
            return this.entry.renderMainOk;
        }

        public boolean isTooBig() {
            return this.entry.tooBig;
        }

        public void render(float originX, float originY, float originZ, float screenX, float screenY, float r, float g, float b, float a) {
            if (PerformanceSettings.fboRenderChunk) {
                WorldItemAtlas.ItemTextureDepthDrawer drawer = WorldItemAtlas.instance.itemTextureDepthDrawerPool.alloc();
                drawer.init(this, originX, originY, originZ, screenX, screenY, r, g, b, a);
                SpriteRenderer.instance.drawGeneric(drawer);
            } else {
                float MAX_ZOOM = 2.5F;
                if (this.itemParams.maxZoomIsOne) {
                    MAX_ZOOM = 1.0F;
                }

                SpriteRenderer.instance
                    .states
                    .getPopulatingActiveState()
                    .render(
                        this.entry.tex,
                        screenX - (this.entry.w / 2.0F - this.entry.offsetX) / MAX_ZOOM,
                        screenY - (this.entry.h / 2.0F - this.entry.offsetY) / MAX_ZOOM,
                        this.entry.w / MAX_ZOOM,
                        this.entry.h / MAX_ZOOM,
                        r,
                        g,
                        b,
                        a,
                        null
                    );
            }
        }

        public Texture getTexture() {
            return this.entry == null ? null : this.entry.tex;
        }

        public float getRenderX(float sx) {
            float MAX_ZOOM = 2.5F;
            if (this.itemParams.maxZoomIsOne) {
                MAX_ZOOM = 1.0F;
            }

            return sx - (this.entry.w / 2.0F - this.entry.offsetX) / MAX_ZOOM;
        }

        public float getRenderY(float sy) {
            float MAX_ZOOM = 2.5F;
            if (this.itemParams.maxZoomIsOne) {
                MAX_ZOOM = 1.0F;
            }

            return sy - (this.entry.h / 2.0F - this.entry.offsetY) / MAX_ZOOM;
        }

        public float getRenderWidth() {
            float MAX_ZOOM = 2.5F;
            if (this.itemParams.maxZoomIsOne) {
                MAX_ZOOM = 1.0F;
            }

            return this.entry.w / MAX_ZOOM;
        }

        public float getRenderHeight() {
            float MAX_ZOOM = 2.5F;
            if (this.itemParams.maxZoomIsOne) {
                MAX_ZOOM = 1.0F;
            }

            return this.entry.h / MAX_ZOOM;
        }

        void Reset() {
            this.itemParams.Reset();
            this.entry = null;
        }
    }

    private static final class ItemTextureDepthDrawer extends TextureDraw.GenericDrawer {
        WorldItemAtlas.ItemTexture itemTexture;
        float ox;
        float oy;
        float oz;
        float x;
        float y;
        float r;
        float g;
        float b;
        float a;

        WorldItemAtlas.ItemTextureDepthDrawer init(
            WorldItemAtlas.ItemTexture itemTexture, float originX, float originY, float originZ, float x, float y, float r, float g, float b, float a
        ) {
            this.itemTexture = itemTexture;
            this.ox = originX;
            this.oy = originY;
            this.oz = originZ;
            this.x = x;
            this.y = y;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        @Override
        public void render() {
            WorldItemAtlas.AtlasEntry entry = this.itemTexture.entry;
            if (entry != null && entry.ready && entry.tex.isReady()) {
                if (DeadBodyAtlas.deadBodyAtlasShader == null) {
                    DeadBodyAtlas.deadBodyAtlasShader = new DeadBodyAtlas.DeadBodyAtlasShader("DeadBodyAtlas");
                }

                if (DeadBodyAtlas.deadBodyAtlasShader.getShaderProgram().isCompiled()) {
                    float MAX_ZOOM = 2.5F;
                    if (this.itemTexture.itemParams.maxZoomIsOne) {
                        MAX_ZOOM = 1.0F;
                    }

                    float x1 = this.x - (entry.w / 2.0F - entry.offsetX) / MAX_ZOOM;
                    float y1 = this.y - (entry.h / 2.0F - entry.offsetY) / MAX_ZOOM;
                    float w = entry.w / MAX_ZOOM;
                    float h = entry.h / MAX_ZOOM;
                    GL13.glActiveTexture(33985);
                    GL11.glEnable(3553);
                    GL11.glBindTexture(3553, entry.atlas.depth.getID());
                    GL13.glActiveTexture(33984);
                    GL11.glDepthMask(true);
                    GL11.glDepthFunc(515);
                    GL11.glEnable(3042);
                    GL11.glBlendFunc(770, 771);
                    float playerX = Core.getInstance().floatParamMap.get(0);
                    float playerY = Core.getInstance().floatParamMap.get(1);
                    int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
                    PlayerCamera camera = SpriteRenderer.instance.getRenderingPlayerCamera(playerIndex);
                    float jx = camera.fixJigglyModelsSquareX;
                    float jy = camera.fixJigglyModelsSquareY;
                    float offsetX = entry.offsetX;
                    float offsetY = entry.offsetY;
                    float wx = -(offsetX + 2.0F * offsetY) / (64.0F * Core.tileScale);
                    float wy = -(offsetX - 2.0F * offsetY) / (-64.0F * Core.tileScale);
                    wy = 0.0F;
                    wx = 0.0F;
                    float F = 137.0F;
                    if (this.itemTexture.itemParams.maxZoomIsOne) {
                        F *= 2.5F;
                    }

                    float depthNear = IsoDepthHelper.getSquareDepthData(
                            PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.ox + jx + wx + 0.5F * F, this.oy + jy + wy + 0.5F * F, this.oz + 1.0F
                        )
                        .depthStart;
                    float depthFar = IsoDepthHelper.getSquareDepthData(
                            PZMath.fastfloor(playerX), PZMath.fastfloor(playerY), this.ox + jx + wx - 0.5F * F, this.oy + jy + wy - 0.5F * F, this.oz
                        )
                        .depthStart;
                    float F2 = 0.00145F;
                    if (this.itemTexture.itemParams.maxZoomIsOne) {
                        F2 *= 1.0F;
                    }

                    depthNear += F2;
                    depthFar += F2;
                    VBORenderer vbor = VBORenderer.getInstance();
                    vbor.startRun(vbor.formatPositionColorUv);
                    vbor.setMode(7);
                    vbor.setDepthTest(true);
                    vbor.setShaderProgram(DeadBodyAtlas.deadBodyAtlasShader.getShaderProgram());
                    Texture tex = entry.tex;
                    vbor.setTextureID(tex.getTextureId());
                    vbor.cmdUseProgram(DeadBodyAtlas.deadBodyAtlasShader.getShaderProgram());
                    vbor.cmdShader1f("zDepthBlendZ", depthNear);
                    vbor.cmdShader1f("zDepthBlendToZ", depthFar);
                    vbor.addQuad(x1, y1, tex.getXStart(), tex.getYStart(), x1 + w, y1 + h, tex.getXEnd(), tex.getYEnd(), 0.0F, this.r, this.g, this.b, this.a);
                    vbor.endRun();
                    vbor.flush();
                    GL13.glActiveTexture(33985);
                    GL11.glBindTexture(3553, 0);
                    GL11.glDisable(3553);
                    GL13.glActiveTexture(33984);
                    ShaderHelper.glUseProgramObjectARB(0);
                    Texture.lastTextureID = -1;
                    SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                    GLStateRenderThread.restore();
                }
            }
        }

        @Override
        public void postRender() {
            this.itemTexture = null;
            WorldItemAtlas.instance.itemTextureDepthDrawerPool.release(this);
        }
    }

    private static final class ItemTextureDrawer extends TextureDraw.GenericDrawer {
        WorldItemAtlas.ItemTexture itemTexture;
        float x;
        float y;
        float r;
        float g;
        float b;
        float a;

        WorldItemAtlas.ItemTextureDrawer init(WorldItemAtlas.ItemTexture itemTexture, float x, float y, float r, float g, float b, float a) {
            this.itemTexture = itemTexture;
            this.x = x;
            this.y = y;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
            return this;
        }

        @Override
        public void render() {
            WorldItemAtlas.AtlasEntry entry = this.itemTexture.entry;
            if (entry != null && entry.ready && entry.tex.isReady()) {
                float MAX_ZOOM = 2.5F;
                if (this.itemTexture.itemParams.maxZoomIsOne) {
                    MAX_ZOOM = 1.0F;
                }

                int x = (int)(this.x - (entry.w / 2.0F - entry.offsetX) / MAX_ZOOM);
                int y = (int)(this.y - (entry.h / 2.0F - entry.offsetY) / MAX_ZOOM);
                int w = (int)(entry.w / MAX_ZOOM);
                int h = (int)(entry.h / MAX_ZOOM);
                entry.tex.bind();
                GL11.glBegin(7);
                GL11.glColor4f(this.r, this.g, this.b, this.a);
                GL11.glTexCoord2f(entry.tex.xStart, entry.tex.yStart);
                GL11.glVertex2i(x, y);
                GL11.glTexCoord2f(entry.tex.xEnd, entry.tex.yStart);
                GL11.glVertex2i(x + w, y);
                GL11.glTexCoord2f(entry.tex.xEnd, entry.tex.yEnd);
                GL11.glVertex2i(x + w, y + h);
                GL11.glTexCoord2f(entry.tex.xStart, entry.tex.yEnd);
                GL11.glVertex2i(x, y + h);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glEnd();
                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
            }
        }

        @Override
        public void postRender() {
            this.itemTexture = null;
            WorldItemAtlas.instance.itemTextureDrawerPool.release(this);
        }
    }

    private static final class RenderJob extends TextureDraw.GenericDrawer {
        public final WorldItemAtlas.ItemParams itemParams = new WorldItemAtlas.ItemParams();
        public WorldItemAtlas.AtlasEntry entry;
        public int done;
        public int renderRefCount;
        public boolean clearThisSlotOnly;
        int entryW;
        int entryH;
        final int[] viewport = new int[4];
        final Matrix4f matri4f = new Matrix4f();
        final Matrix4f projection = new Matrix4f();
        final Matrix4f modelView = new Matrix4f();
        final Vector3f scenePos = new Vector3f();
        final float[] bounds = new float[4];
        static final Vector3f tempVector3f = new Vector3f(0.0F, 5.0F, -2.0F);
        static final Matrix4f tempMatrix4f_1 = new Matrix4f();
        static final Matrix4f tempMatrix4f_2 = new Matrix4f();
        static final float[] xs = new float[8];
        static final float[] ys = new float[8];

        public static WorldItemAtlas.RenderJob getNew() {
            return WorldItemAtlas.JobPool.isEmpty() ? new WorldItemAtlas.RenderJob() : WorldItemAtlas.JobPool.pop();
        }

        public WorldItemAtlas.RenderJob init(WorldItemAtlas.ItemParams params, WorldItemAtlas.AtlasEntry entry) {
            this.itemParams.copyFrom(params);
            this.entry = entry;
            this.clearThisSlotOnly = false;
            this.entryW = 0;
            this.entryH = 0;
            this.done = 0;
            this.renderRefCount = 0;
            return this;
        }

        public boolean renderMain() {
            Model model = this.itemParams.model;
            return model != null && model.isReady() && model.mesh != null && model.mesh.isReady();
        }

        @Override
        public void render() {
            if (this.done != 1) {
                Model model = this.itemParams.model;
                if (model != null && model.mesh != null && model.mesh.isReady()) {
                    float offsetX = 0.0F;
                    float offsetY = 0.0F;
                    this.calcMatrices(this.projection, this.modelView, 0.0F, 0.0F);
                    this.calcModelBounds(this.bounds);
                    this.calcModelOffset();
                    this.calcEntrySize();
                    if (this.entryW > 0 && this.entryH > 0) {
                        if (this.entryW <= 512 && this.entryH <= 512) {
                            WorldItemAtlas.instance.assignEntryToAtlas(this.entry, this.entryW, this.entryH);
                            GL11.glPushAttrib(1048575);
                            GL11.glPushClientAttrib(-1);
                            GL11.glDepthMask(true);
                            GL11.glColorMask(true, true, true, true);
                            GL11.glDisable(3089);
                            TextureFBO fbo = WorldItemAtlas.instance.fbo;
                            if (fbo.getTexture() != this.entry.atlas.tex) {
                                fbo.setTextureAndDepth(this.entry.atlas.tex, this.entry.atlas.depth);
                            }

                            fbo.startDrawing(this.entry.atlas.clear, this.entry.atlas.clear);
                            if (this.entry.atlas.clear) {
                                this.entry.atlas.clear = false;
                            }

                            this.clearColorAndDepth();
                            int x = this.entry.x - (int)this.entry.offsetX - (1024 - this.entry.w) / 2;
                            int y = -((int)this.entry.offsetY) - (1024 - this.entry.h) / 2;
                            y += 512 - (this.entry.y + this.entry.h);
                            GL11.glViewport(x, y, 1024, 1024);
                            boolean bDepthDebugBox = false;
                            boolean bRendered = this.renderModel(this.itemParams.model, null, false);
                            if (this.itemParams.weaponParts != null && !this.itemParams.weaponParts.isEmpty()) {
                                for (int i = 0; i < this.itemParams.weaponParts.size(); i++) {
                                    WorldItemAtlas.WeaponPartParams partParams = this.itemParams.weaponParts.get(i);
                                    if (!this.renderModel(partParams.model, partParams.transform, true)) {
                                        bRendered = false;
                                        break;
                                    }
                                }
                            }

                            fbo.endDrawing();
                            if (!bRendered) {
                                GL11.glPopAttrib();
                                GL11.glPopClientAttrib();
                            } else {
                                this.entry.ready = true;
                                this.done = 1;
                                Texture.lastTextureID = -1;
                                SpriteRenderer.ringBuffer.restoreBoundTextures = true;
                                SpriteRenderer.ringBuffer.restoreVbos = true;
                                GL11.glPopAttrib();
                                GL11.glPopClientAttrib();
                            }
                        } else {
                            this.entry.tooBig = true;
                            this.done = 1;
                        }
                    }
                }
            }
        }

        @Override
        public void postRender() {
            if (this.entry != null) {
                assert this.renderRefCount > 0;

                this.renderRefCount--;
            }
        }

        void clearColorAndDepth() {
            GL11.glEnable(3089);
            GL11.glScissor(this.entry.x, 512 - (this.entry.y + this.entry.h), this.entry.w, this.entry.h);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            GL11.glClearDepth(1.0);
            GL11.glClear(16640);
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            this.restoreScreenStencil();
            GL11.glDisable(3089);
        }

        void restoreScreenStencil() {
            int playerIndex = SpriteRenderer.instance.getRenderingPlayerIndex();
            int x = playerIndex != 0 && playerIndex != 2 ? Core.getInstance().getOffscreenTrueWidth() / 2 : 0;
            int y = playerIndex != 0 && playerIndex != 1 ? Core.getInstance().getOffscreenTrueHeight() / 2 : 0;
            int w = Core.getInstance().getOffscreenTrueWidth();
            int h = Core.getInstance().getOffscreenTrueHeight();
            if (IsoPlayer.numPlayers > 1) {
                w /= 2;
            }

            if (IsoPlayer.numPlayers > 2) {
                h /= 2;
            }

            GL11.glScissor(x, y, w, h);
        }

        boolean renderModel(Model model, Matrix4f tranform2, boolean bWeaponPart) {
            if (!model.isStatic) {
                return false;
            } else {
                if (model.effect == null) {
                    model.CreateShader("basicEffect");
                }

                Shader Effect = model.effect;
                if (Effect != null && model.mesh != null && model.mesh.isReady()) {
                    boolean bUseSmartTexture = !bWeaponPart && this.checkSmartTexture(this.itemParams.modelTextureName);
                    if (model.tex != null && !model.tex.isReady()) {
                        return false;
                    } else {
                        PZGLUtil.pushAndLoadMatrix(5889, this.projection);
                        Matrix4f modelView = tempMatrix4f_1.set(this.modelView);
                        Matrix4f transform = tempMatrix4f_2.set(this.itemParams.transform).invert();
                        modelView.mul(transform);
                        PZGLUtil.pushAndLoadMatrix(5888, modelView);
                        GL11.glBlendFunc(770, 771);
                        GL11.glDepthFunc(513);
                        GL11.glDepthMask(true);
                        GL11.glDepthRange(0.0, 1.0);
                        GL11.glEnable(2929);
                        if (Core.debug && DebugOptions.instance.model.render.wireframe.getValue()) {
                            GL11.glPolygonMode(1032, 6913);
                            GL11.glEnable(2848);
                            GL11.glLineWidth(0.75F);
                            Shader effect2 = ShaderManager.instance.getOrCreateShader("vehicle_wireframe", model.isStatic, false);
                            if (effect2 != null) {
                                effect2.Start();
                                if (model.isStatic) {
                                    effect2.setTransformMatrix(this.itemParams.transform, true);
                                }

                                model.mesh.Draw(effect2);
                                effect2.End();
                            }

                            GL11.glPolygonMode(1032, 6914);
                            GL11.glDisable(2848);
                            PZGLUtil.popMatrix(5889);
                            PZGLUtil.popMatrix(5888);
                            GLStateRenderThread.restore();
                            return true;
                        } else {
                            GL11.glColor3f(1.0F, 1.0F, 1.0F);
                            Effect.Start();
                            if (bUseSmartTexture) {
                                Effect.setTexture(this.itemParams.smartTexture, "Texture", 0);
                            } else if (model.tex == null) {
                                Effect.setTexture(Texture.getErrorTexture(), "Texture", 0);
                            } else {
                                Effect.setTexture(model.tex, "Texture", 0);
                            }

                            Effect.setDepthBias(0.0F);
                            Effect.setTargetDepth(0.5F);
                            Effect.setAmbient(this.itemParams.ambientR * 0.4F, this.itemParams.ambientG * 0.4F, this.itemParams.ambientB * 0.4F);
                            Effect.setLightingAmount(1.0F);
                            Effect.setHueShift(this.itemParams.hue);
                            Effect.setTint(1.0F, 1.0F, 1.0F);
                            if (this.itemParams.tintMask == null) {
                                Effect.setTint(this.itemParams.tintR, this.itemParams.tintG, this.itemParams.tintB);
                            }

                            Effect.setAlpha(1.0F);

                            for (int i = 0; i < 5; i++) {
                                Effect.setLight(i, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Float.NaN, 0.0F, 0.0F, 0.0F, null);
                            }

                            Vector3f xyz = tempVector3f;
                            xyz.x = 0.0F;
                            xyz.y = 5.0F;
                            xyz.z = -2.0F;
                            xyz.rotateY(this.itemParams.angle.y * (float) (Math.PI / 180.0));
                            float lightMul = 1.5F;
                            Effect.setLight(
                                4,
                                xyz.x,
                                xyz.z,
                                xyz.y,
                                this.itemParams.ambientR / 4.0F * 1.5F,
                                this.itemParams.ambientG / 4.0F * 1.5F,
                                this.itemParams.ambientB / 4.0F * 1.5F,
                                5000.0F,
                                Float.NaN,
                                0.0F,
                                0.0F,
                                0.0F,
                                null
                            );
                            if (tranform2 == null) {
                                Effect.setTransformMatrix(this.itemParams.transform, false);
                            } else {
                                tempMatrix4f_1.set(this.itemParams.transform);
                                tempMatrix4f_1.mul(tranform2);
                                Effect.setTransformMatrix(tempMatrix4f_1, false);
                            }

                            model.mesh.Draw(Effect);
                            Effect.End();
                            if (bUseSmartTexture) {
                                if (this.itemParams.smartTexture.result != null) {
                                    TextureCombiner.instance.releaseTexture(this.itemParams.smartTexture.result);
                                    this.itemParams.smartTexture.result = null;
                                }

                                this.itemParams.smartTexture.clear();
                            }

                            if (Core.debug && DebugOptions.instance.model.render.axis.getValue()) {
                                Model.debugDrawAxis(0.0F, 0.0F, 0.0F, 0.5F, 1.0F);
                            }

                            PZGLUtil.popMatrix(5889);
                            PZGLUtil.popMatrix(5888);
                            GLStateRenderThread.restore();
                            return true;
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        boolean checkSmartTexture(String modelTextureName) {
            if (modelTextureName == null) {
                return false;
            } else {
                boolean bUseSmartTexture = false;
                if (this.itemParams.bloodLevel > 0.0F) {
                    bUseSmartTexture = true;
                }

                String textureMask = null;
                String tintTextureMask = null;
                if (this.itemParams.fluidLevel > 0.0F && this.itemParams.fluidTextureMask != null) {
                    bUseSmartTexture = true;
                    textureMask = this.itemParams.fluidTextureMask;
                }

                if (this.itemParams.tintMask != null) {
                    bUseSmartTexture = true;
                    tintTextureMask = this.itemParams.tintMask;
                }

                if (bUseSmartTexture) {
                    this.itemParams.smartTexture.clear();
                    this.itemParams.smartTexture.add(modelTextureName);
                    if (this.itemParams.tintMask != null) {
                        ImmutableColor temp = new ImmutableColor(this.itemParams.tintR, this.itemParams.tintG, this.itemParams.tintB, 1.0F);
                        this.itemParams.smartTexture.setTintMask(tintTextureMask, "media/textures/FullAlpha.png", 300, temp.toMutableColor());
                    }

                    if (this.itemParams.bloodLevel > 0.0F) {
                        this.itemParams
                            .smartTexture
                            .setBlood(
                                "media/textures/BloodTextures/BloodOverlayWeapon.png",
                                "media/textures/BloodTextures/BloodOverlayWeaponMask.png",
                                this.itemParams.bloodLevel,
                                300
                            );
                    }

                    if (textureMask != null) {
                        String maskPath = "media/textures/FullAlpha.png";
                        if (Texture.getTexture(textureMask) != null) {
                            this.itemParams
                                .smartTexture
                                .setFluid(textureMask, "media/textures/FullAlpha.png", this.itemParams.fluidLevel, 302, this.itemParams.fluidTint);
                        }
                    }

                    this.itemParams.smartTexture.calculate();
                }

                return bUseSmartTexture;
            }
        }

        void calcMatrices(Matrix4f projection, Matrix4f modelView, float offsetX, float offsetY) {
            double screenWidth = 0.53333336F;
            double screenHeight = 0.53333336F;
            projection.setOrtho(-0.26666668F, 0.26666668F, 0.26666668F, -0.26666668F, -10.0F, 10.0F);
            float MAX_ZOOM = 2.5F;
            if (this.itemParams.maxZoomIsOne) {
                MAX_ZOOM = 1.0F;
            }

            modelView.identity();
            modelView.scale(Core.scale * Core.tileScale / 2.0F);
            boolean bIsometric = true;
            modelView.rotate((float) (Math.PI / 6), 1.0F, 0.0F, 0.0F);
            modelView.rotate((float) (Math.PI * 3.0 / 4.0), 0.0F, 1.0F, 0.0F);
            modelView.scale(-1.5F * MAX_ZOOM, 1.5F * MAX_ZOOM, 1.5F * MAX_ZOOM);
            modelView.rotateXYZ(
                this.itemParams.angle.x * (float) (Math.PI / 180.0),
                this.itemParams.angle.y * (float) (Math.PI / 180.0),
                this.itemParams.angle.z * (float) (Math.PI / 180.0)
            );
            modelView.translate(offsetX, 0.0F, offsetY);
            modelView.mul(this.itemParams.transform);
        }

        void calcModelBounds(float[] bounds) {
            bounds[0] = Float.MAX_VALUE;
            bounds[1] = Float.MAX_VALUE;
            bounds[2] = -Float.MAX_VALUE;
            bounds[3] = -Float.MAX_VALUE;
            this.calcModelBounds(this.itemParams.model, this.modelView, bounds);
            if (this.itemParams.weaponParts != null) {
                for (int i = 0; i < this.itemParams.weaponParts.size(); i++) {
                    WorldItemAtlas.WeaponPartParams partParams = this.itemParams.weaponParts.get(i);
                    Matrix4f modelView = tempMatrix4f_1.set(this.modelView).mul(partParams.transform);
                    this.calcModelBounds(partParams.model, modelView, bounds);
                }
            }

            float scale = 2.0F;
            bounds[0] *= 2.0F;
            bounds[1] *= 2.0F;
            bounds[2] *= 2.0F;
            bounds[3] *= 2.0F;
        }

        void calcModelBounds(Model model, Matrix4f modelView, float[] bounds) {
            Vector3f min = model.mesh.minXyz;
            Vector3f max = model.mesh.maxXyz;
            xs[0] = min.x;
            ys[0] = min.y;
            xs[1] = min.x;
            ys[1] = max.y;
            xs[2] = max.x;
            ys[2] = max.y;
            xs[3] = max.x;
            ys[3] = min.y;

            for (int i = 0; i < 4; i++) {
                this.sceneToUI(xs[i], ys[i], min.z, this.projection, modelView, this.scenePos);
                bounds[0] = PZMath.min(bounds[0], this.scenePos.x);
                bounds[2] = PZMath.max(bounds[2], this.scenePos.x);
                bounds[1] = PZMath.min(bounds[1], this.scenePos.y);
                bounds[3] = PZMath.max(bounds[3], this.scenePos.y);
                this.sceneToUI(xs[i], ys[i], max.z, this.projection, modelView, this.scenePos);
                bounds[0] = PZMath.min(bounds[0], this.scenePos.x);
                bounds[2] = PZMath.max(bounds[2], this.scenePos.x);
                bounds[1] = PZMath.min(bounds[1], this.scenePos.y);
                bounds[3] = PZMath.max(bounds[3], this.scenePos.y);
            }
        }

        void calcModelOffset() {
            float minX = this.bounds[0];
            float minY = this.bounds[1];
            float maxX = this.bounds[2];
            float maxY = this.bounds[3];
            this.entry.offsetX = minX + (maxX - minX) / 2.0F - 512.0F;
            this.entry.offsetY = minY + (maxY - minY) / 2.0F - 512.0F;
        }

        void calcEntrySize() {
            float minX = this.bounds[0];
            float minY = this.bounds[1];
            float maxX = this.bounds[2];
            float maxY = this.bounds[3];
            float pad = 2.0F;
            minX -= 2.0F;
            minY -= 2.0F;
            maxX += 2.0F;
            maxY += 2.0F;
            int div = 16;
            minX = (float)Math.floor(minX / 16.0F) * 16.0F;
            maxX = (float)Math.ceil(maxX / 16.0F) * 16.0F;
            minY = (float)Math.floor(minY / 16.0F) * 16.0F;
            maxY = (float)Math.ceil(maxY / 16.0F) * 16.0F;
            this.entryW = (int)(maxX - minX);
            this.entryH = (int)(maxY - minY);
        }

        Vector3f sceneToUI(float sceneX, float sceneY, float sceneZ, Matrix4f projection, Matrix4f modelView, Vector3f out) {
            Matrix4f matrix4f = this.matri4f;
            matrix4f.set(projection);
            matrix4f.mul(modelView);
            this.viewport[0] = 0;
            this.viewport[1] = 0;
            this.viewport[2] = 512;
            this.viewport[3] = 512;
            matrix4f.project(sceneX, sceneY, sceneZ, this.viewport, out);
            return out;
        }

        public void Reset() {
            this.itemParams.Reset();
            this.entry = null;
        }
    }

    private static final class WeaponPartParams {
        Model model;
        String attachmentNameSelf;
        String attachmentNameParent;
        final Matrix4f transform = new Matrix4f();

        WorldItemAtlas.WeaponPartParams init(WorldItemAtlas.WeaponPartParams other) {
            this.model = other.model;
            this.attachmentNameSelf = other.attachmentNameSelf;
            this.attachmentNameParent = other.attachmentNameParent;
            this.transform.set(other.transform);
            return this;
        }

        void initTransform(ModelScript parentModelScript, ModelScript modelScript) {
            this.transform.identity();
            Matrix4f attachmentXfrm = WorldItemAtlas.s_attachmentXfrm;
            ModelAttachment parentAttachment = parentModelScript.getAttachmentById(this.attachmentNameParent);
            if (parentAttachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(parentAttachment, attachmentXfrm);
                this.transform.mul(attachmentXfrm);
            }

            ModelAttachment selfAttachment = modelScript.getAttachmentById(this.attachmentNameSelf);
            if (selfAttachment != null) {
                ModelInstanceRenderData.makeAttachmentTransform(selfAttachment, attachmentXfrm);
                if (ModelInstanceRenderData.invertAttachmentSelfTransform) {
                    attachmentXfrm.invert();
                }

                this.transform.mul(attachmentXfrm);
            }
        }
    }
}
