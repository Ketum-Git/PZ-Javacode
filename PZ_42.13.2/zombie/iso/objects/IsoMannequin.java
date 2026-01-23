// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItems;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.skinnedmodel.DeadBodyAtlas;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.advancedanimation.AnimNode;
import zombie.core.skinnedmodel.advancedanimation.AnimState;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.advancedanimation.AnimationSet;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugLog;
import zombie.gameStates.GameLoadingState;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.InventoryContainer;
import zombie.inventory.types.Moveable;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderCorpses;
import zombie.iso.fboRenderChunk.FBORenderObjectHighlight;
import zombie.iso.fboRenderChunk.FBORenderShadows;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.MannequinScript;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;
import zombie.vehicles.BaseVehicle;

@UsedFromLua
public class IsoMannequin extends IsoObject implements IHumanVisual {
    private static final ColorInfo inf = new ColorInfo();
    private boolean init;
    private boolean female;
    private boolean zombie;
    private boolean skeleton;
    private String mannequinScriptName;
    private String modelScriptName;
    private String textureName;
    private String animSet;
    private String animState;
    private String pose;
    private String outfit;
    private final HumanVisual humanVisual = new HumanVisual(this);
    private final ItemVisuals itemVisuals = new ItemVisuals();
    private final WornItems wornItems;
    private MannequinScript mannequinScript;
    private ModelScript modelScript;
    private final IsoMannequin.PerPlayer[] perPlayer = new IsoMannequin.PerPlayer[4];
    private boolean animate;
    private AnimatedModel animatedModel;
    private IsoMannequin.Drawer[] drawers;
    private float screenX;
    private float screenY;
    private static final IsoMannequin.StaticPerPlayer[] staticPerPlayer = new IsoMannequin.StaticPerPlayer[4];

    public IsoMannequin(IsoCell cell) {
        super(cell);
        this.wornItems = new WornItems(BodyLocations.getGroup("Human"));

        for (int i = 0; i < 4; i++) {
            this.perPlayer[i] = new IsoMannequin.PerPlayer();
        }
    }

    public IsoMannequin(IsoCell cell, IsoGridSquare square, IsoSprite sprite) {
        super(cell, square, sprite);
        this.wornItems = new WornItems(BodyLocations.getGroup("Human"));

        for (int i = 0; i < 4; i++) {
            this.perPlayer[i] = new IsoMannequin.PerPlayer();
        }
    }

    @Override
    public String getObjectName() {
        return "Mannequin";
    }

    @Override
    public HumanVisual getHumanVisual() {
        return this.humanVisual;
    }

    @Override
    public void getItemVisuals(ItemVisuals itemVisuals) {
        this.wornItems.getItemVisuals(itemVisuals);
    }

    @Override
    public boolean isFemale() {
        return this.female;
    }

    @Override
    public boolean isZombie() {
        return this.zombie;
    }

    @Override
    public boolean isSkeleton() {
        return this.skeleton;
    }

    @Override
    public boolean isItemAllowedInContainer(ItemContainer container, InventoryItem item) {
        return item instanceof Clothing && item.getBodyLocation() != null ? true : item instanceof InventoryContainer && item.canBeEquipped() != null;
    }

    public String getMannequinScriptName() {
        return this.mannequinScriptName;
    }

    public void setMannequinScriptName(String name) {
        if (!StringUtils.isNullOrWhitespace(name)) {
            if (ScriptManager.instance.getMannequinScript(name) != null) {
                this.mannequinScriptName = name;
                this.init = true;
                this.mannequinScript = null;
                this.textureName = null;
                this.animSet = null;
                this.animState = null;
                this.pose = null;
                this.outfit = null;
                this.humanVisual.clear();
                this.itemVisuals.clear();
                this.wornItems.clear();
                this.initMannequinScript();
                this.initModelScript();
                if (this.outfit == null) {
                    Outfit outfit = OutfitManager.instance.GetRandomNonProfessionalOutfit(this.female);
                    this.humanVisual.dressInNamedOutfit(outfit.name, this.itemVisuals);
                } else if (!"none".equalsIgnoreCase(this.outfit)) {
                    this.humanVisual.dressInNamedOutfit(this.outfit, this.itemVisuals);
                }

                this.humanVisual.setHairModel("");
                this.humanVisual.setBeardModel("");
                this.createInventory(this.itemVisuals);
                this.validateSkinTexture();
                this.validatePose();
                this.syncModel();
            }
        }
    }

    public String getPose() {
        return this.pose;
    }

    public void setRenderDirection(IsoDirections newDir) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        if (newDir != this.perPlayer[playerIndex].renderDirection) {
            this.perPlayer[playerIndex].renderDirection = newDir;
            if (PerformanceSettings.fboRenderChunk) {
                this.invalidateRenderChunkLevel(256L);
            }
        }
    }

    public void rotate(IsoDirections newDir) {
        if (newDir != null && newDir != IsoDirections.Max) {
            this.dir = newDir;

            for (int i = 0; i < 4; i++) {
                this.perPlayer[i].atlasTex = null;
            }

            if (GameServer.server) {
                this.sendObjectChange("rotate");
            }

            this.invalidateRenderChunkLevel(256L);
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("rotate".equals(change)) {
            bb.put((byte)this.dir.index());
        } else {
            super.saveChange(change, tbl, bb);
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("rotate".equals(change)) {
            int dirIndex = bb.get();
            this.rotate(IsoDirections.fromIndex(dirIndex));
        } else {
            super.loadChange(change, bb);
        }
    }

    public void getVariables(Map<String, String> vars) {
        vars.put("Female", this.female ? "true" : "false");
        vars.put("Pose", this.getPose());
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.dir = IsoDirections.fromIndex(input.get());
        this.init = input.get() == 1;
        this.female = input.get() == 1;
        this.zombie = input.get() == 1;
        this.skeleton = input.get() == 1;
        this.mannequinScriptName = GameWindow.ReadString(input);
        this.pose = GameWindow.ReadString(input);
        this.humanVisual.load(input, WorldVersion);
        this.textureName = this.humanVisual.getSkinTexture();
        this.wornItems.clear();
        if (this.container == null) {
            this.container = new ItemContainer("mannequin", this.getSquare(), this);
            this.container.setExplored(true);
        }

        this.container.clear();
        if (input.get() == 1) {
            try {
                this.container.id = input.getInt();
                ArrayList<InventoryItem> savedItems = this.container.load(input, WorldVersion);
                int wornItemCount = input.get();

                for (int i = 0; i < wornItemCount; i++) {
                    ItemBodyLocation itemBodyLocation = ItemBodyLocation.get(ResourceLocation.of(GameWindow.ReadString(input)));
                    short index = input.getShort();
                    if (index >= 0 && index < savedItems.size() && this.wornItems.getBodyLocationGroup().getLocation(itemBodyLocation) != null) {
                        this.wornItems.setItem(itemBodyLocation, savedItems.get(index));
                    }
                }
            } catch (Exception var9) {
                if (this.container != null) {
                    DebugLog.log("Failed to stream in container ID: " + this.container.id);
                }
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        ItemContainer container = this.container;
        this.container = null;
        super.save(output, IS_DEBUG_SAVE);
        this.container = container;
        output.put((byte)this.dir.index());
        output.put((byte)(this.init ? 1 : 0));
        output.put((byte)(this.female ? 1 : 0));
        output.put((byte)(this.zombie ? 1 : 0));
        output.put((byte)(this.skeleton ? 1 : 0));
        GameWindow.WriteString(output, this.mannequinScriptName);
        GameWindow.WriteString(output, this.pose);
        this.humanVisual.save(output);
        if (container != null) {
            output.put((byte)1);
            output.putInt(container.id);
            ArrayList<InventoryItem> savedItems = container.save(output);
            if (this.wornItems.size() > 127) {
                throw new RuntimeException("too many worn items");
            }

            output.put((byte)this.wornItems.size());
            this.wornItems.forEach(wornItem -> {
                GameWindow.WriteString(output, wornItem.getLocation().toString());
                output.putShort((short)savedItems.indexOf(wornItem.getItem()));
            });
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void saveState(ByteBuffer output) throws IOException {
        if (!this.init) {
            this.initOutfit();
        }

        this.save(output);
    }

    @Override
    public void loadState(ByteBuffer input) throws IOException {
        input.get();
        input.get();
        this.load(input, 241);
        this.initOutfit();
        this.validateSkinTexture();
        this.validatePose();
        this.syncModel();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        if (this.square == null || this.square.chunk == null || this.square.chunk.jobType != IsoChunk.JobType.SoftReset) {
            this.initOutfit();
            this.validateSkinTexture();
            this.validatePose();
            this.syncModel();
            if (!FBORenderCell.instance.mannequinList.contains(this)) {
                FBORenderCell.instance.mannequinList.add(this);
            }
        }
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        FBORenderCell.instance.mannequinList.remove(this);
    }

    private void initMannequinScript() {
        if (!StringUtils.isNullOrWhitespace(this.mannequinScriptName)) {
            this.mannequinScript = ScriptManager.instance.getMannequinScript(this.mannequinScriptName);
        }

        if (this.mannequinScript == null) {
            this.modelScriptName = this.female ? "FemaleBody" : "MaleBody";
            this.textureName = this.female ? "F_Mannequin_White" : "M_Mannequin_White";
            this.animSet = "mannequin";
            this.animState = this.female ? "female" : "male";
            this.outfit = null;
        } else {
            this.female = this.mannequinScript.isFemale();
            this.modelScriptName = this.mannequinScript.getModelScriptName();
            if (this.textureName == null) {
                this.textureName = this.mannequinScript.getTexture();
            }

            this.animSet = this.mannequinScript.getAnimSet();
            this.animState = this.mannequinScript.getAnimState();
            if (this.pose == null) {
                this.pose = this.mannequinScript.getPose();
            }

            if (this.outfit == null) {
                this.outfit = this.mannequinScript.getOutfit();
            }
        }
    }

    private void initModelScript() {
        if (!StringUtils.isNullOrWhitespace(this.modelScriptName)) {
            this.modelScript = ScriptManager.instance.getModelScript(this.modelScriptName);
        }
    }

    private void validateSkinTexture() {
    }

    private void validatePose() {
        AnimationSet animSet = AnimationSet.GetAnimationSet(this.animSet, false);
        if (animSet == null) {
            DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" doesn't exist", this.animSet);
            this.pose = "Invalid";
        } else {
            AnimState state = animSet.GetState(this.animState);
            if (state == null) {
                DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" state \"%s\" doesn't exist", this.animSet, this.animState);
                this.pose = "Invalid";
            } else {
                for (AnimNode node : state.nodes) {
                    if (node.name.equalsIgnoreCase(this.pose)) {
                        return;
                    }
                }

                if (state.nodes == null) {
                    DebugLog.General.warn("ERROR: mannequin AnimSet \"%s\" state \"%s\" node \"%s\" doesn't exist", this.animSet, this.animState, this.pose);
                    this.pose = "Invalid";
                } else {
                    AnimNode nodex = PZArrayUtil.pickRandom(state.nodes);
                    this.pose = nodex.name;
                }
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        x += 0.5F;
        y += 0.5F;
        this.calcScreenPos(x, y, z);
        this.renderShadow(x, y, z);
        if (this.animate) {
            this.animatedModel.update();
            IsoMannequin.Drawer drawer = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
            drawer.init(x, y, z);
            SpriteRenderer.instance.drawGeneric(drawer);
        } else {
            boolean bHighlighted = FBORenderObjectHighlight.getInstance().shouldRenderObjectHighlight(this);
            if (FBORenderChunkManager.instance.isCaching()) {
                FBORenderChunk renderChunk = this.square
                    .chunk
                    .getRenderLevels(playerIndex)
                    .getFBOForLevel(this.square.z, Core.getInstance().getZoom(playerIndex));
                IsoDirections dirOld = this.dir;
                IsoMannequin.PerPlayer perPlayer = this.perPlayer[playerIndex];
                if (perPlayer.renderDirection != null && perPlayer.renderDirection != IsoDirections.Max) {
                    this.dir = perPlayer.renderDirection;
                    perPlayer.renderDirection = null;
                    perPlayer.wasRenderDirection = true;
                    perPlayer.atlasTex = null;
                } else if (perPlayer.wasRenderDirection) {
                    perPlayer.wasRenderDirection = false;
                    perPlayer.atlasTex = null;
                }

                if (!FBORenderCell.instance.isBlackedOutBuildingSquare(this.square)) {
                    float oldAlpha = this.getAlpha(playerIndex);
                    float alpha = PZMath.min(IsoWorldInventoryObject.getSurfaceAlpha(this.square, this.getZ() - PZMath.fastfloor(this.getZ())), oldAlpha);
                    if (alpha != 0.0F) {
                        FBORenderCorpses.getInstance().render(renderChunk.index, this);
                        perPlayer.lastRenderDirection = this.dir;
                        this.dir = dirOld;
                    }
                }
            } else if (PerformanceSettings.fboRenderChunk) {
                if (this.animatedModel == null) {
                    this.animatedModel = new AnimatedModel();
                    this.animatedModel.setAnimate(false);
                    this.drawers = new IsoMannequin.Drawer[3];

                    for (int i = 0; i < this.drawers.length; i++) {
                        this.drawers[i] = new IsoMannequin.Drawer();
                    }
                }

                this.animatedModel.setAnimSetName(this.getAnimSetName());
                this.animatedModel.setState(this.getAnimStateName());
                this.animatedModel.setVariable("Female", this.female);
                this.animatedModel.setVariable("Pose", this.getPose());
                IsoDirections dir = this.dir;
                IsoMannequin.PerPlayer perPlayerx = this.perPlayer[playerIndex];
                if (perPlayerx.renderDirection != null && perPlayerx.renderDirection != IsoDirections.Max) {
                    dir = perPlayerx.renderDirection;
                    perPlayerx.renderDirection = null;
                    perPlayerx.wasRenderDirection = true;
                    perPlayerx.atlasTex = null;
                } else if (perPlayerx.wasRenderDirection) {
                    perPlayerx.wasRenderDirection = false;
                    perPlayerx.atlasTex = null;
                }

                this.animatedModel.setAngle(dir.ToVector());
                this.animatedModel.setModelData(this.humanVisual, this.itemVisuals);
                this.animatedModel.update();
                IsoMannequin.Drawer drawer = this.drawers[SpriteRenderer.instance.getMainStateIndex()];
                IsoObject[] objects = this.square.getObjects().getElements();

                for (int i = 0; i < this.square.getObjects().size(); i++) {
                    IsoObject object = objects[i];
                    if (object.isTableSurface()) {
                        z += (object.getSurfaceOffset() + 1.0F) / 96.0F;
                    }
                }

                if (bHighlighted) {
                    this.animatedModel.setTint(this.getHighlightColor(playerIndex));
                } else {
                    this.animatedModel.setTint(1.0F, 1.0F, 1.0F);
                }

                drawer.init(x, y, z);
                SpriteRenderer.instance.drawGeneric(drawer);
            } else {
                IsoDirections dirOldx = this.dir;
                IsoMannequin.PerPlayer perPlayerxx = this.perPlayer[playerIndex];
                if (perPlayerxx.renderDirection != null && perPlayerxx.renderDirection != IsoDirections.Max) {
                    this.dir = perPlayerxx.renderDirection;
                    perPlayerxx.renderDirection = null;
                    perPlayerxx.wasRenderDirection = true;
                    perPlayerxx.atlasTex = null;
                } else if (perPlayerxx.wasRenderDirection) {
                    perPlayerxx.wasRenderDirection = false;
                    perPlayerxx.atlasTex = null;
                }

                if (perPlayerxx.atlasTex == null) {
                    perPlayerxx.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
                    DeadBodyAtlas.instance.render();
                }

                this.dir = dirOldx;
                if (perPlayerxx.atlasTex != null) {
                    if (bHighlighted) {
                        inf.r = this.getHighlightColor(playerIndex).r;
                        inf.g = this.getHighlightColor(playerIndex).g;
                        inf.b = this.getHighlightColor(playerIndex).b;
                        inf.a = this.getHighlightColor(playerIndex).a;
                    } else {
                        inf.r = col.r;
                        inf.g = col.g;
                        inf.b = col.b;
                        inf.a = col.a;
                    }

                    col = inf;
                    if (!bHighlighted) {
                        this.square.interpolateLight(col, x - this.square.getX(), y - this.square.getY());
                    }

                    IndieGL.disableDepthTest();
                    SpriteRenderer.instance.StartShader(0, playerIndex);
                    perPlayerxx.atlasTex.render(x, y, z, (int)this.screenX, (int)this.screenY, col.r, col.g, col.b, this.getAlpha(playerIndex));
                    SpriteRenderer.instance.EndShader();
                    if (Core.debug) {
                    }
                }
            }
        }
    }

    @Override
    public void renderFxMask(float x, float y, float z, boolean bDoAttached) {
    }

    public boolean shouldRenderEachFrame() {
        return this.animate;
    }

    public void checkRenderDirection(int playerIndex) {
        IsoMannequin.PerPlayer perPlayer = this.perPlayer[playerIndex];
        if ((perPlayer.renderDirection == null || perPlayer.renderDirection == IsoDirections.Max) && perPlayer.wasRenderDirection) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    private void calcScreenPos(float x, float y, float z) {
        if (IsoSprite.globalOffsetX == -1.0F) {
            IsoSprite.globalOffsetX = -IsoCamera.frameState.offX;
            IsoSprite.globalOffsetY = -IsoCamera.frameState.offY;
        }

        this.screenX = IsoUtils.XToScreen(x, y, z, 0);
        this.screenY = IsoUtils.YToScreen(x, y, z, 0);
        this.sx = this.screenX;
        this.sy = this.screenY;
        this.screenX = this.sx + IsoSprite.globalOffsetX;
        this.screenY = this.sy + IsoSprite.globalOffsetY;
        IsoObject[] objects = this.square.getObjects().getElements();

        for (int i = 0; i < this.square.getObjects().size(); i++) {
            IsoObject object = objects[i];
            if (object.isTableSurface()) {
                this.screenY = this.screenY - (object.getSurfaceOffset() + 1.0F) * Core.tileScale;
            }
        }
    }

    public DeadBodyAtlas.BodyTexture getAtlasTexture() {
        this.offsetX = this.offsetY = 0.0F;
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoMannequin.PerPlayer perPlayer = this.perPlayer[playerIndex];
        IsoDirections dirOld = this.dir;
        this.dir = perPlayer.lastRenderDirection;
        if (perPlayer.atlasTex == null) {
            perPlayer.atlasTex = DeadBodyAtlas.instance.getBodyTexture(this);
            DeadBodyAtlas.instance.render();
        }

        this.dir = dirOld;
        return perPlayer.atlasTex;
    }

    public void renderShadow(float x, float y, float z) {
        if (!FBORenderChunkManager.instance.isCaching()) {
            float w = 0.45F;
            float fm = 0.4725F;
            float bm = 0.5611F;
            int playerIndex = IsoCamera.frameState.playerIndex;
            ColorInfo lightInfo = this.square.lighting[playerIndex].lightInfo();
            Vector3f forward = BaseVehicle.allocVector3f().set(1.0F, 0.0F, 0.0F);
            if (PerformanceSettings.fboRenderChunk) {
                IsoObject[] objects = this.square.getObjects().getElements();

                for (int i = 0; i < this.square.getObjects().size(); i++) {
                    IsoObject object = objects[i];
                    if (object.isTableSurface()) {
                        z += (object.getSurfaceOffset() + 2.0F) / 96.0F;
                    }
                }

                FBORenderShadows.getInstance()
                    .addShadow(x, y, z, forward, 0.45F, 0.4725F, 0.5611F, lightInfo.r, lightInfo.g, lightInfo.a, 0.8F * this.getAlpha(playerIndex), false);
                BaseVehicle.releaseVector3f(forward);
            } else {
                IsoDeadBody.renderShadow(x, y, z, forward, 0.45F, 0.4725F, 0.5611F, lightInfo, 0.8F * this.getAlpha(playerIndex));
                BaseVehicle.releaseVector3f(forward);
            }
        }
    }

    private void initOutfit() {
        if (this.init) {
            this.initMannequinScript();
            this.initModelScript();
        } else {
            this.init = true;
            this.getPropertiesFromSprite();
            this.getPropertiesFromZone();
            this.initMannequinScript();
            this.initModelScript();
            if (this.outfit == null) {
                Outfit outfit = OutfitManager.instance.GetRandomNonProfessionalOutfit(this.female);
                this.humanVisual.dressInNamedOutfit(outfit.name, this.itemVisuals);
            } else if (!"none".equalsIgnoreCase(this.outfit)) {
                this.humanVisual.dressInNamedOutfit(this.outfit, this.itemVisuals);
            }

            this.humanVisual.setHairModel("");
            this.humanVisual.setBeardModel("");
            this.createInventory(this.itemVisuals);
        }
    }

    private void getPropertiesFromSprite() {
        String var1 = this.sprite.name;
        switch (var1) {
            case "location_shop_mall_01_65":
                this.mannequinScriptName = "FemaleWhite01";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_66":
                this.mannequinScriptName = "FemaleWhite02";
                this.dir = IsoDirections.S;
                break;
            case "location_shop_mall_01_67":
                this.mannequinScriptName = "FemaleWhite03";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_68":
                this.mannequinScriptName = "MaleWhite01";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_69":
                this.mannequinScriptName = "MaleWhite02";
                this.dir = IsoDirections.S;
                break;
            case "location_shop_mall_01_70":
                this.mannequinScriptName = "MaleWhite03";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_73":
                this.mannequinScriptName = "FemaleBlack01";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_74":
                this.mannequinScriptName = "FemaleBlack02";
                this.dir = IsoDirections.S;
                break;
            case "location_shop_mall_01_75":
                this.mannequinScriptName = "FemaleBlack03";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_76":
                this.mannequinScriptName = "MaleBlack01";
                this.dir = IsoDirections.SE;
                break;
            case "location_shop_mall_01_77":
                this.mannequinScriptName = "MaleBlack02";
                this.dir = IsoDirections.S;
                break;
            case "location_shop_mall_01_78":
                this.mannequinScriptName = "MaleBlack03";
                this.dir = IsoDirections.SE;
        }
    }

    private void getPropertiesFromZone() {
        if (this.getObjectIndex() != -1) {
            IsoMetaCell metaCell = IsoWorld.instance.getMetaGrid().getCellData(this.square.x / 256, this.square.y / 256);
            if (metaCell != null && metaCell.mannequinZones != null) {
                ArrayList<IsoMannequin.MannequinZone> zones = metaCell.mannequinZones;
                IsoMannequin.MannequinZone zone = null;

                for (int i = 0; i < zones.size(); i++) {
                    zone = zones.get(i);
                    if (zone.contains(this.square.x, this.square.y, this.square.z)) {
                        break;
                    }

                    zone = null;
                }

                if (zone != null) {
                    if (zone.female != -1) {
                        this.female = zone.female == 1;
                    }

                    if (zone.dir != IsoDirections.Max) {
                        this.dir = zone.dir;
                    }

                    if (zone.mannequinScript != null) {
                        this.mannequinScriptName = zone.mannequinScript;
                    }

                    if (zone.skin != null) {
                        this.textureName = zone.skin;
                    }

                    if (zone.pose != null) {
                        this.pose = zone.pose;
                    }

                    if (zone.outfit != null) {
                        this.outfit = zone.outfit;
                    }
                }
            }
        }
    }

    private void syncModel() {
        this.humanVisual.setForceModelScript(this.modelScriptName);
        String i = this.modelScriptName;
        switch (i) {
            case "FemaleBody":
                this.humanVisual.setForceModel(ModelManager.instance.femaleModel);
                break;
            case "MaleBody":
                this.humanVisual.setForceModel(ModelManager.instance.maleModel);
                break;
            default:
                this.humanVisual.setForceModel(ModelManager.instance.getLoadedModel(this.modelScriptName));
        }

        this.humanVisual.setSkinTextureName(this.textureName);
        this.wornItems.getItemVisuals(this.itemVisuals);

        for (int i = 0; i < 4; i++) {
            this.perPlayer[i].atlasTex = null;
        }

        if (this.animate) {
            if (this.animatedModel == null) {
                this.animatedModel = new AnimatedModel();
                this.drawers = new IsoMannequin.Drawer[3];

                for (int i = 0; i < this.drawers.length; i++) {
                    this.drawers[i] = new IsoMannequin.Drawer();
                }
            }

            this.animatedModel.setAnimSetName(this.getAnimSetName());
            this.animatedModel.setState(this.getAnimStateName());
            this.animatedModel.setVariable("Female", this.female);
            this.animatedModel.setVariable("Pose", this.getPose());
            this.animatedModel.setAngle(this.dir.ToVector());
            this.animatedModel.setModelData(this.humanVisual, this.itemVisuals);
        }
    }

    private void createInventory(ItemVisuals itemVisuals) {
        if (this.container == null) {
            this.container = new ItemContainer("mannequin", this.getSquare(), this);
            this.container.setExplored(true);
        }

        this.container.clear();
        this.wornItems.setFromItemVisuals(itemVisuals);
        this.wornItems.addItemsToItemContainer(this.container);
    }

    public void wearItem(InventoryItem item, IsoGameCharacter chr) {
        if (this.container.contains(item)) {
            ItemVisual itemVisual = item.getVisual();
            if (itemVisual != null) {
                if (item instanceof Clothing && item.getBodyLocation() != null) {
                    this.wornItems.setItem(item.getBodyLocation(), item);
                } else {
                    if (!(item instanceof InventoryContainer) || item.canBeEquipped() == null) {
                        return;
                    }

                    this.wornItems.setItem(item.canBeEquipped(), item);
                }

                if (chr != null) {
                    ArrayList<InventoryItem> items = this.container.getItems();

                    for (int i = 0; i < items.size(); i++) {
                        InventoryItem item1 = items.get(i);
                        if (!this.wornItems.contains(item1)) {
                            this.container.removeItemOnServer(item1);
                            this.container.Remove(item1);
                            chr.getInventory().AddItem(item1);
                            i--;
                        }
                    }
                }

                this.syncModel();
                this.invalidateRenderChunkLevel(256L);
            }
        }
    }

    public void checkClothing(InventoryItem removedItem) {
        for (int i = 0; i < this.wornItems.size(); i++) {
            InventoryItem item = this.wornItems.getItemByIndex(i);
            if (this.container == null || this.container.getItems().indexOf(item) == -1) {
                this.wornItems.remove(item);
                this.syncModel();
                this.invalidateRenderChunkLevel(256L);
                i--;
            }
        }
    }

    public String getAnimSetName() {
        return this.animSet;
    }

    public String getAnimStateName() {
        return this.animState;
    }

    public void getCustomSettingsFromItem(InventoryItem item) throws IOException {
        if (item instanceof Moveable) {
            ByteBuffer input = item.getByteData();
            if (input == null) {
                return;
            }

            input.rewind();
            int WorldVersion = input.getInt();
            input.get();
            input.get();
            this.load(input, WorldVersion);
        }
    }

    public void setCustomSettingsToItem(InventoryItem item) throws IOException {
        if (item instanceof Moveable) {
            synchronized (SliceY.SliceBufferLock) {
                ByteBuffer output = SliceY.SliceBuffer;
                output.clear();
                output.putInt(241);
                this.save(output);
                output.flip();
                item.byteData = ByteBuffer.allocate(output.limit());
                item.byteData.put(output);
            }

            if (this.container != null) {
                item.setActualWeight(item.getActualWeight() + this.container.getContentsWeight());
            }
        }
    }

    public static boolean isMannequinSprite(IsoSprite sprite) {
        return "Mannequin".equals(sprite.getProperties().get("CustomName"));
    }

    private void resetMannequin() {
        this.init = false;
        this.female = false;
        this.zombie = false;
        this.skeleton = false;
        this.mannequinScriptName = null;
        this.modelScriptName = null;
        this.textureName = null;
        this.animSet = null;
        this.animState = null;
        this.pose = null;
        this.outfit = null;
        this.humanVisual.clear();
        this.itemVisuals.clear();
        this.wornItems.clear();
        this.mannequinScript = null;
        this.modelScript = null;
        this.animate = false;
    }

    public static void renderMoveableItem(Moveable item, int x, int y, int z, IsoDirections dir) {
        int playerIndex = IsoCamera.frameState.playerIndex;
        IsoMannequin.StaticPerPlayer perPlayer = staticPerPlayer[playerIndex];
        if (perPlayer == null) {
            perPlayer = staticPerPlayer[playerIndex] = new IsoMannequin.StaticPerPlayer(playerIndex);
        }

        perPlayer.renderMoveableItem(item, x, y, z, dir);
    }

    public static void renderMoveableObject(IsoMannequin mannequin, int x, int y, int z, IsoDirections dir) {
        mannequin.setRenderDirection(dir);
    }

    public static IsoDirections getDirectionFromItem(Moveable item, int playerIndex) {
        IsoMannequin.StaticPerPlayer perPlayer = staticPerPlayer[playerIndex];
        if (perPlayer == null) {
            perPlayer = staticPerPlayer[playerIndex] = new IsoMannequin.StaticPerPlayer(playerIndex);
        }

        return perPlayer.getDirectionFromItem(item);
    }

    public WornItems getWornItems() {
        return this.wornItems;
    }

    private final class Drawer extends TextureDraw.GenericDrawer {
        private float x;
        private float y;
        private float z;
        private float animPlayerAngle;
        private boolean rendered;

        private Drawer() {
            Objects.requireNonNull(IsoMannequin.this);
            super();
        }

        public void init(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rendered = false;
            IsoMannequin.this.animatedModel.renderMain();
            this.animPlayerAngle = IsoMannequin.this.animatedModel.getAnimationPlayer().getRenderedAngle();
        }

        @Override
        public void render() {
            IsoMannequin.this.animatedModel.DoRenderToWorld(this.x, this.y, this.z, this.animPlayerAngle);
            this.rendered = true;
        }

        @Override
        public void postRender() {
            IsoMannequin.this.animatedModel.postRender(this.rendered);
        }
    }

    public static final class MannequinZone extends Zone {
        public int female = -1;
        public IsoDirections dir = IsoDirections.Max;
        public String mannequinScript;
        public String pose;
        public String skin;
        public String outfit;

        public MannequinZone(String name, String type, int x, int y, int z, int w, int h, KahluaTable properties) {
            super(name, type, x, y, z, w, h);
            if (properties != null) {
                Object o = properties.rawget("Female");
                if (o instanceof Boolean) {
                    this.female = o == Boolean.TRUE ? 1 : 0;
                }

                if (properties.rawget("Direction") instanceof String s) {
                    this.dir = IsoDirections.valueOf(s);
                }

                if (properties.rawget("Outfit") instanceof String s) {
                    this.outfit = s;
                }

                if (properties.rawget("Script") instanceof String s) {
                    this.mannequinScript = s;
                }

                if (properties.rawget("Skin") instanceof String s) {
                    this.skin = s;
                }

                if (properties.rawget("Pose") instanceof String s) {
                    this.pose = s;
                }
            }
        }
    }

    private static final class PerPlayer {
        private DeadBodyAtlas.BodyTexture atlasTex;
        IsoDirections renderDirection;
        IsoDirections lastRenderDirection;
        boolean wasRenderDirection;
    }

    private static final class StaticPerPlayer {
        private final int playerIndex;
        private Moveable moveable;
        private Moveable failedItem;
        private IsoMannequin mannequin;

        private StaticPerPlayer(int playerIndex) {
            this.playerIndex = playerIndex;
        }

        private void renderMoveableItem(Moveable item, int x, int y, int z, IsoDirections dir) {
            if (this.checkItem(item)) {
                if (this.moveable != item) {
                    this.moveable = item;

                    try {
                        this.mannequin.getCustomSettingsFromItem(this.moveable);
                    } catch (IOException var7) {
                    }

                    this.mannequin.initOutfit();
                    this.mannequin.validateSkinTexture();
                    this.mannequin.validatePose();
                    this.mannequin.syncModel();
                    this.mannequin.perPlayer[this.playerIndex].atlasTex = null;
                }

                this.mannequin.square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
                if (this.mannequin.square != null) {
                    this.mannequin.perPlayer[this.playerIndex].renderDirection = dir;
                    IsoMannequin.inf.set(1.0F, 1.0F, 1.0F, 1.0F);
                    this.mannequin.render(x, y, z, IsoMannequin.inf, false, false, null);
                }
            }
        }

        private IsoDirections getDirectionFromItem(Moveable item) {
            if (!this.checkItem(item)) {
                return IsoDirections.S;
            } else {
                this.moveable = null;

                try {
                    this.mannequin.getCustomSettingsFromItem(item);
                    return this.mannequin.getDir();
                } catch (Exception var3) {
                    return IsoDirections.S;
                }
            }
        }

        private boolean checkItem(Moveable item) {
            if (item == null) {
                return false;
            } else {
                String spriteName = item.getWorldSprite();
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                if (sprite == null || !IsoMannequin.isMannequinSprite(sprite)) {
                    return false;
                } else if (item.getByteData() == null) {
                    Thread thread = Thread.currentThread();
                    if (thread != GameWindow.gameThread && thread != GameLoadingState.loader && thread == GameServer.mainThread) {
                        return false;
                    } else {
                        if (this.mannequin == null || this.mannequin.getCell() != IsoWorld.instance.currentCell) {
                            this.mannequin = new IsoMannequin(IsoWorld.instance.currentCell);
                        }

                        if (this.failedItem == item) {
                            return false;
                        } else {
                            try {
                                this.mannequin.resetMannequin();
                                this.mannequin.sprite = sprite;
                                this.mannequin.initOutfit();
                                this.mannequin.validateSkinTexture();
                                this.mannequin.validatePose();
                                this.mannequin.syncModel();
                                this.mannequin.setCustomSettingsToItem(item);
                                return true;
                            } catch (IOException var6) {
                                this.failedItem = item;
                                return false;
                            }
                        }
                    }
                } else {
                    if (this.mannequin == null || this.mannequin.getCell() != IsoWorld.instance.currentCell) {
                        this.mannequin = new IsoMannequin(IsoWorld.instance.currentCell);
                    }

                    return true;
                }
            }
        }
    }
}
