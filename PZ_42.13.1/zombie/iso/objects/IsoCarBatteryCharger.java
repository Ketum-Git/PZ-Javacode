// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.network.ByteBufferWriter;
import zombie.core.opengl.Shader;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.fboRenderChunk.FBORenderCell;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;

@UsedFromLua
public class IsoCarBatteryCharger extends IsoObject implements IItemProvider {
    protected InventoryItem item;
    protected InventoryItem battery;
    protected boolean activated;
    protected float lastUpdate = -1.0F;
    protected float chargeRate = 0.16666667F;
    protected IsoSprite chargerSprite;
    protected IsoSprite batterySprite;
    protected long sound;

    public IsoCarBatteryCharger(IsoCell cell) {
        super(cell);
    }

    public IsoCarBatteryCharger(InventoryItem item, IsoCell cell, IsoGridSquare square) {
        super(cell, square, (IsoSprite)null);
        if (item == null) {
            throw new NullPointerException("item is null");
        } else {
            this.item = item;
        }
    }

    @Override
    public String getObjectName() {
        return "IsoCarBatteryCharger";
    }

    @Override
    public void load(ByteBuffer bb, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(bb, WorldVersion, IS_DEBUG_SAVE);
        if (bb.get() == 1) {
            try {
                this.item = InventoryItem.loadItem(bb, WorldVersion);
            } catch (Exception var6) {
                var6.printStackTrace();
            }
        }

        if (bb.get() == 1) {
            try {
                this.battery = InventoryItem.loadItem(bb, WorldVersion);
            } catch (Exception var5) {
                var5.printStackTrace();
            }
        }

        this.activated = bb.get() == 1;
        this.lastUpdate = bb.getFloat();
        this.chargeRate = bb.getFloat();
    }

    @Override
    public void save(ByteBuffer bb, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(bb, IS_DEBUG_SAVE);
        if (this.item == null) {
            assert false;

            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            this.item.saveWithSize(bb, false);
        }

        if (this.battery == null) {
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            this.battery.saveWithSize(bb, false);
        }

        bb.put((byte)(this.activated ? 1 : 0));
        bb.putFloat(this.lastUpdate);
        bb.putFloat(this.chargeRate);
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public void removeFromWorld() {
        this.stopChargingSound();
        super.removeFromWorld();
    }

    @Override
    public void update() {
        super.update();
        if (!(this.battery instanceof DrainableComboItem)) {
            this.battery = null;
        }

        if (this.battery == null) {
            this.lastUpdate = -1.0F;
            this.setActivated(false);
            this.stopChargingSound();
        } else {
            boolean isPowered = this.square != null && (this.square.haveElectricity() || this.square.hasGridPower() && this.square.getRoom() != null);
            if (!isPowered) {
                this.setActivated(false);
            }

            if (!this.activated) {
                this.lastUpdate = -1.0F;
                this.stopChargingSound();
            } else {
                this.startChargingSound();
                DrainableComboItem battery = (DrainableComboItem)this.battery;
                if (!(battery.getCurrentUsesFloat() >= 1.0F)) {
                    float worldAgeHours = (float)GameTime.getInstance().getWorldAgeHours();
                    if (this.lastUpdate < 0.0F) {
                        this.lastUpdate = worldAgeHours;
                    }

                    if (this.lastUpdate > worldAgeHours) {
                        this.lastUpdate = worldAgeHours;
                    }

                    float elapsedHours = worldAgeHours - this.lastUpdate;
                    if (elapsedHours > 0.0F) {
                        battery.setCurrentUses((int)(battery.getMaxUses() * Math.min(1.0F, battery.getCurrentUsesFloat() + this.chargeRate * elapsedHours)));
                        this.lastUpdate = worldAgeHours;
                    }
                }
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (!PerformanceSettings.fboRenderChunk || !Core.getInstance().isOption3DGroundItem()) {
            this.chargerSprite = this.configureSprite(this.item, this.chargerSprite);
            if (!this.chargerSprite.hasNoTextures()) {
                Texture tex = this.chargerSprite.getTextureForCurrentFrame(this.dir);
                if (tex != null) {
                    float dx = tex.getWidthOrig() * this.chargerSprite.def.getScaleX() / 2.0F;
                    float dy = tex.getHeightOrig() * this.chargerSprite.def.getScaleY() * 3.0F / 4.0F;
                    this.offsetX = this.offsetY = 0.0F;
                    this.setAlpha(IsoCamera.frameState.playerIndex, 1.0F);
                    float xoff = 0.5F;
                    float yoff = 0.5F;
                    float zoff = 0.0F;
                    this.sx = 0.0F;
                    this.item.setWorldZRotation(315.0F);
                    ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(
                        this.getItem(),
                        this.getSquare(),
                        this.getRenderSquare(),
                        this.getX() + 0.5F,
                        this.getY() + 0.5F,
                        this.getZ() + 0.0F,
                        -1.0F,
                        -1.0F,
                        true
                    );
                    if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
                        this.chargerSprite
                            .render(
                                this,
                                x + 0.5F,
                                y + 0.5F,
                                z + 0.0F,
                                this.dir,
                                this.offsetX + dx + 8 * Core.tileScale,
                                this.offsetY + dy + 4 * Core.tileScale,
                                col,
                                true
                            );
                    }

                    if (status == ItemModelRenderer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                        FBORenderCell.instance.handleDelayedLoading(this);
                    }

                    if (this.battery != null) {
                        this.batterySprite = this.configureSprite(this.battery, this.batterySprite);
                        if (this.batterySprite != null && !this.batterySprite.hasNoTextures()) {
                            this.sx = 0.0F;
                            this.getBattery().setWorldZRotation(90.0F);
                            status = WorldItemModelDrawer.renderMain(
                                this.getBattery(),
                                this.getSquare(),
                                this.getRenderSquare(),
                                this.getX() + 0.75F,
                                this.getY() + 0.75F,
                                this.getZ() + 0.0F,
                                -1.0F,
                                -1.0F,
                                true
                            );
                            if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
                                this.batterySprite
                                    .render(
                                        this,
                                        x + 0.5F,
                                        y + 0.5F,
                                        z + 0.0F,
                                        this.dir,
                                        this.offsetX + dx - 8.0F + Core.tileScale,
                                        this.offsetY + dy - 4 * Core.tileScale,
                                        col,
                                        true
                                    );
                            }

                            if (status == ItemModelRenderer.RenderStatus.Loading
                                && PerformanceSettings.fboRenderChunk
                                && FBORenderChunkManager.instance.isCaching()) {
                                FBORenderCell.instance.handleDelayedLoading(this);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void renderObjectPicker(float x, float y, float z, ColorInfo lightInfo) {
    }

    @Override
    public boolean hasAnimatedAttachments() {
        return Core.getInstance().isOption3DGroundItem();
    }

    @Override
    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo col) {
        this.chargerSprite = this.configureSprite(this.item, this.chargerSprite);
        if (!this.chargerSprite.hasNoTextures()) {
            Texture tex = this.chargerSprite.getTextureForCurrentFrame(this.dir);
            if (tex != null) {
                float dx = tex.getWidthOrig() * this.chargerSprite.def.getScaleX() / 2.0F;
                float dy = tex.getHeightOrig() * this.chargerSprite.def.getScaleY() * 3.0F / 4.0F;
                this.offsetX = this.offsetY = 0.0F;
                this.setAlpha(IsoCamera.frameState.playerIndex, 1.0F);
                float xoff = 0.5F;
                float yoff = 0.5F;
                float zoff = 0.0F;
                this.sx = 0.0F;
                this.item.setWorldZRotation(315.0F);
                ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(
                    this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.5F, this.getY() + 0.5F, this.getZ() + 0.0F, -1.0F, -1.0F, true
                );
                if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
                    this.chargerSprite
                        .render(
                            this,
                            x + 0.5F,
                            y + 0.5F,
                            z + 0.0F,
                            this.dir,
                            this.offsetX + dx + 8 * Core.tileScale,
                            this.offsetY + dy + 4 * Core.tileScale,
                            col,
                            true
                        );
                }

                if (status == ItemModelRenderer.RenderStatus.Loading && PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                    FBORenderCell.instance.handleDelayedLoading(this);
                }

                if (this.battery != null) {
                    this.batterySprite = this.configureSprite(this.battery, this.batterySprite);
                    if (this.batterySprite != null && !this.batterySprite.hasNoTextures()) {
                        this.sx = 0.0F;
                        this.getBattery().setWorldZRotation(90.0F);
                        status = WorldItemModelDrawer.renderMain(
                            this.getBattery(),
                            this.getSquare(),
                            this.getRenderSquare(),
                            this.getX() + 0.75F,
                            this.getY() + 0.75F,
                            this.getZ() + 0.0F,
                            -1.0F,
                            -1.0F,
                            true
                        );
                        if (status == ItemModelRenderer.RenderStatus.NoModel || status == ItemModelRenderer.RenderStatus.Failed) {
                            this.batterySprite
                                .render(
                                    this,
                                    x + 0.5F,
                                    y + 0.5F,
                                    z + 0.0F,
                                    this.dir,
                                    this.offsetX + dx - 8.0F + Core.tileScale,
                                    this.offsetY + dy - 4 * Core.tileScale,
                                    col,
                                    true
                                );
                        }

                        if (status == ItemModelRenderer.RenderStatus.Loading
                            && PerformanceSettings.fboRenderChunk
                            && FBORenderChunkManager.instance.isCaching()) {
                            FBORenderCell.instance.handleDelayedLoading(this);
                        }
                    }
                }
            }
        }
    }

    private IsoSprite configureSprite(InventoryItem item, IsoSprite sprite) {
        String name = item.getWorldTexture();

        try {
            Texture tex = Texture.getSharedTexture(name);
            if (tex == null) {
                name = item.getTex().getName();
            }
        } catch (Exception var7) {
            name = "media/inventory/world/WItem_Sack.png";
        }

        Texture tex = Texture.getSharedTexture(name);
        boolean setScale = false;
        if (sprite == null) {
            sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        }

        if (sprite.currentAnim != null || sprite.texture != tex) {
            sprite.LoadSingleTexture(name);
            setScale = true;
        }

        if (setScale) {
            if (item.getScriptItem() == null) {
                sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * Core.tileScale, 16 * Core.tileScale);
            } else if (this.battery != null && this.battery.getScriptItem() != null) {
                float var10001 = Core.tileScale;
                float scale = this.battery.getScriptItem().scaleWorldIcon * (var10001 / 2.0F);
                sprite.def.setScale(scale, scale);
            }
        }

        return sprite;
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        byte index = (byte)this.getObjectIndex();
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        b.putByte(index);
        b.putByte((byte)1);
        b.putByte((byte)0);
        if (this.battery == null) {
            b.putByte((byte)0);
        } else {
            b.putByte((byte)1);

            try {
                this.battery.saveWithSize(b.bb, false);
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        }

        b.putBoolean(this.activated);
        b.putFloat(this.chargeRate);
    }

    @Override
    public void syncIsoObjectReceive(ByteBuffer bb) {
        if (bb.get() == 1) {
            try {
                this.battery = InventoryItem.loadItem(bb, 240);
            } catch (Exception var3) {
                var3.printStackTrace();
            }
        } else {
            this.battery = null;
        }

        this.activated = bb.get() == 1;
        this.chargeRate = bb.getFloat();
    }

    @Override
    public InventoryItem getItem() {
        return this.item;
    }

    public InventoryItem getBattery() {
        return this.battery;
    }

    public void setBattery(InventoryItem battery) {
        if (battery != null) {
            if (!(battery instanceof DrainableComboItem)) {
                throw new IllegalArgumentException("battery isn't DrainableComboItem");
            }

            if (this.battery != null) {
                throw new IllegalStateException("battery already inserted");
            }
        }

        this.battery = battery;
        this.invalidateRenderChunkLevel(256L);
    }

    public boolean isActivated() {
        return this.activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
        IsoGenerator.updateGenerator(this.square);
    }

    public float getChargeRate() {
        return this.chargeRate;
    }

    public void setChargeRate(float chargeRate) {
        if (chargeRate <= 0.0F) {
            throw new IllegalArgumentException("chargeRate <= 0.0f");
        } else {
            this.chargeRate = chargeRate;
        }
    }

    private void startChargingSound() {
        if (!GameServer.server) {
            if (this.getObjectIndex() != -1) {
                if (this.sound != -1L) {
                    if (this.emitter == null) {
                        this.emitter = IsoWorld.instance.getFreeEmitter(this.square.x + 0.5F, this.square.y + 0.5F, this.square.z);
                        IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
                    }

                    if (!this.emitter.isPlaying(this.sound)) {
                        this.sound = this.emitter.playSound("CarBatteryChargerRunning");
                        if (this.sound == 0L) {
                            this.sound = -1L;
                        }
                    }

                    this.emitter.tick();
                }
            }
        }
    }

    private void stopChargingSound() {
        if (!GameServer.server) {
            if (this.emitter != null) {
                this.emitter.stopOrTriggerSound(this.sound);
                this.sound = 0L;
                IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
                this.emitter = null;
            }
        }
    }
}
