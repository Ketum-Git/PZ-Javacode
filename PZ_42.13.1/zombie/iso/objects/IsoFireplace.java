// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.entity.ComponentType;
import zombie.entity.components.sounds.CraftBenchSounds;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public class IsoFireplace extends IsoObject {
    private static final int SmoulderMinutes = 10;
    private static final int IsoHeatSourceTemperature = 35;
    private int fuelAmountMinutes;
    private boolean lit;
    private boolean smouldering;
    protected float lastUpdateTime = -1.0F;
    protected float minuteAccumulator;
    protected int minutesSinceExtinguished = -1;
    protected IsoSprite fuelSprite;
    protected int fuelSpriteIndex = -1;
    protected int fireSpriteIndex = -1;
    private boolean fireSpriteUsesOurDepthTexture = true;
    protected IsoLightSource lightSource;
    protected IsoHeatSource heatSource;
    private long soundInstance;

    public IsoFireplace(IsoCell cell) {
        super(cell);
    }

    public IsoFireplace(IsoCell cell, IsoGridSquare sq, IsoSprite gid) {
        super(cell, sq, gid);
        String containerType = gid != null && gid.getProperties().has(IsoFlagType.container) ? gid.getProperties().get("container") : "fireplace";
        if (this.sprite != null && this.sprite.getProperties() != null && this.sprite.getProperties().get("ContainerCapacity") != null) {
            this.container = new ItemContainer(containerType, sq, this);
            this.container.capacity = Integer.parseInt(this.sprite.getProperties().get("ContainerCapacity"));
        }
    }

    @Override
    public String getObjectName() {
        return "Fireplace";
    }

    @Override
    public Vector2 getFacingPosition(Vector2 pos) {
        if (this.square == null) {
            return pos.set(0.0F, 0.0F);
        } else if (this.getContainer() != null && "campfire".equalsIgnoreCase(this.getContainer().getType())) {
            return pos.set(this.getX() + 0.5F, this.getY() + 0.5F);
        } else {
            return this.getProperties() != null && this.getProperties().has(IsoFlagType.collideN)
                ? pos.set(this.getX() + 0.5F, this.getY())
                : pos.set(this.getX(), this.getY() + 0.5F);
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.fuelAmountMinutes = input.getInt();
        this.lit = input.get() == 1;
        this.lastUpdateTime = input.getFloat();
        this.minutesSinceExtinguished = input.getInt();
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.putInt(this.fuelAmountMinutes);
        output.put((byte)(this.lit ? 1 : 0));
        output.putFloat(this.lastUpdateTime);
        output.putInt(this.minutesSinceExtinguished);
    }

    public void setFuelAmount(int units) {
        units = Math.max(0, units);
        int old = this.getFuelAmount();
        if (units != old) {
            this.fuelAmountMinutes = units;
        }
    }

    public int getFuelAmount() {
        return this.fuelAmountMinutes;
    }

    public void addFuel(int units) {
        this.setFuelAmount(this.getFuelAmount() + units);
    }

    public int useFuel(int amount) {
        int avail = this.getFuelAmount();
        int used = 0;
        if (avail >= amount) {
            used = amount;
        } else {
            used = avail;
        }

        this.setFuelAmount(avail - used);
        return used;
    }

    public boolean hasFuel() {
        return this.getFuelAmount() > 0;
    }

    @Override
    public void turnOn() {
        if (!this.isLit()) {
            this.setLit(true);
            if (this.getContainer() != null) {
                this.getContainer().addItemsToProcessItems();
            }
        }
    }

    @Override
    public void setLit(boolean lit) {
        this.lit = lit;
    }

    @Override
    public boolean isLit() {
        return this.lit;
    }

    public boolean isSmouldering() {
        return this.smouldering;
    }

    public void extinguish() {
        if (this.isLit()) {
            this.setLit(false);
            if (this.hasFuel()) {
                this.minutesSinceExtinguished = 0;
            }
        }
    }

    public float getTemperature() {
        return this.isLit() ? 1.8F : 1.0F;
    }

    public boolean isTemperatureChanging() {
        return this.getTemperature() != (this.isLit() ? 1.8F : 0.0F);
    }

    private void updateFuelSprite() {
        if (!this.getSprite().getProperties().has("CustomName") || !this.getSprite().getProperties().get("CustomName").equals("Metal Drum")) {
            if (this.container == null || !"woodstove".equals(this.container.getType()) && !"campfire".equals(this.container.getType())) {
                if (this.hasFuel() && !this.isLit() && Objects.equals(this.getSprite().getName(), "crafted_02_42")) {
                    this.setOverlaySprite("crafted_02_43");
                } else if (this.hasFuel()
                    && this.container != null
                    && this.getProperties() != null
                    && (this.getProperties().has(IsoFlagType.collideW) || this.getProperties().has(IsoFlagType.collideN))) {
                    if (this.fuelSprite == null) {
                        this.fuelSprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                        Texture var1 = this.fuelSprite.LoadSingleTexture("Item_Logs");
                    }

                    if (this.fuelSpriteIndex == -1) {
                        DebugLog.log(DebugType.Fireplace, "fireplace: added fuel sprite");
                        this.fuelSpriteIndex = this.attachedAnimSprite != null ? this.attachedAnimSprite.size() : 0;
                        if (this.getProperties() != null && this.getProperties().has(IsoFlagType.collideW)) {
                            this.AttachExistingAnim(this.fuelSprite, -10 * Core.tileScale, -90 * Core.tileScale, false, 0, false, 0.0F);
                        } else if (this.getProperties() != null && this.getProperties().has(IsoFlagType.collideN)) {
                            this.AttachExistingAnim(this.fuelSprite, -35 * Core.tileScale, -90 * Core.tileScale, false, 0, false, 0.0F);
                        }

                        if (Core.tileScale == 1) {
                            this.attachedAnimSprite.get(this.fuelSpriteIndex).setScale(0.5F, 0.5F);
                        }

                        this.invalidateRenderChunkLevel(256L);
                    }
                } else if (this.fuelSpriteIndex != -1) {
                    DebugLog.log(DebugType.Fireplace, "fireplace: removed fuel sprite");
                    this.attachedAnimSprite.remove(this.fuelSpriteIndex);
                    if (this.fireSpriteIndex > this.fuelSpriteIndex) {
                        this.fireSpriteIndex--;
                    }

                    this.fuelSpriteIndex = -1;
                    this.invalidateRenderChunkLevel(256L);
                }
            }
        }
    }

    private void updateFireSprite() {
        if (this.isLit()) {
            if (this.fireSpriteIndex == -1) {
                DebugLog.log(DebugType.Fireplace, "fireplace: added fire sprite");
                this.fireSpriteIndex = this.attachedAnimSprite != null ? this.attachedAnimSprite.size() : 0;
                float SCL = 0.5F * (Core.tileScale / 2.0F);
                if (Objects.equals(this.getSprite().getName(), "camping_03_16")) {
                    this.setOverlaySprite("camping_03_17");
                    this.AttachAnim("Fire", "01", 30, 0.5F, 2 * Core.tileScale, -13 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD);
                    this.fireSpriteUsesOurDepthTexture = false;
                } else if (Objects.equals(this.getSprite().getName(), "camping_03_19")) {
                    this.setOverlaySprite("camping_03_21");
                    this.AttachAnim("Fire", "01", 30, 0.5F, 2 * Core.tileScale, -7 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD);
                    this.fireSpriteUsesOurDepthTexture = false;
                } else if (Objects.equals(this.getSprite().getName(), "crafted_02_42")) {
                    this.setOverlaySprite("crafted_02_44");
                    this.AttachAnim("Fire", "01", 30, 0.5F, 2 * Core.tileScale, -2 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD);
                    this.fireSpriteUsesOurDepthTexture = false;
                } else if (Objects.equals(this.getSprite().getName(), "crafted_05_6")) {
                    this.setOverlaySprite("crafted_05_14");
                } else if (Objects.equals(this.getSprite().getName(), "crafted_05_7")) {
                    this.setOverlaySprite("crafted_05_15");
                } else if (Objects.equals(this.getSprite().getName(), "appliances_cooking_01_16")) {
                    String[] split = this.getSprite().getName().split("_");
                    String sprite = split[0] + "_" + split[1] + "_" + split[2] + "_on_" + split[3];
                    this.setOverlaySprite(sprite);
                } else if (Objects.equals(this.getSprite().getName(), "appliances_cooking_01_17")) {
                    String[] split = this.getSprite().getName().split("_");
                    String sprite = split[0] + "_" + split[1] + "_" + split[2] + "_on_" + split[3];
                    this.setOverlaySprite(sprite);
                } else if (this.getSprite().getProperties().has("CustomName") && this.getSprite().getProperties().get("CustomName").equals("Metal Drum")) {
                    if (Objects.equals(this.getSprite().getName(), "crafted_01_26")) {
                        this.setOverlaySprite("crafted_01_27");
                    } else if (Objects.equals(this.getSprite().getName(), "crafted_01_30")) {
                        this.setOverlaySprite("crafted_01_31");
                    }

                    this.AttachAnim("Fire", "01", 30, 0.5F, 0 * Core.tileScale, 10 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD);
                    this.fireSpriteUsesOurDepthTexture = false;
                } else if (this.getProperties() != null && this.getProperties().has(IsoFlagType.collideW)) {
                    this.AttachAnim("Fire", "01", 30, 0.5F, 15 * Core.tileScale, -8 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD);
                } else if (this.getProperties() != null && this.getProperties().has(IsoFlagType.collideN)) {
                    this.AttachAnim("Fire", "01", 30, 0.5F, -11 * Core.tileScale, -8 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD);
                }

                if (this.attachedAnimSprite != null && this.fireSpriteIndex < this.attachedAnimSprite.size()) {
                    IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(this.fireSpriteIndex);
                    IsoSprite sprite1 = spriteInstance.getParentSprite();
                    if (sprite1.hasAnimation()) {
                        spriteInstance.frame = Rand.Next(0.0F, (float)sprite1.currentAnim.frames.size());
                    }

                    spriteInstance.setScale(SCL, SCL);
                    Texture tex = sprite1.getTextureForCurrentFrame(this.getDir());
                    if (tex != null) {
                        sprite1.soffX = (short)(sprite1.soffX + (short)(this.offsetX - tex.getWidthOrig() * SCL / 2.0F));
                        sprite1.soffY = (short)(sprite1.soffY + (short)(this.offsetY - tex.getHeightOrig() * SCL));
                    }
                }
            }
        } else if (this.fireSpriteIndex != -1) {
            DebugLog.log(DebugType.Fireplace, "fireplace: removed fire sprite");
            if (this.attachedAnimSprite != null) {
                this.attachedAnimSprite.remove(this.fireSpriteIndex);
            }

            if (this.fuelSpriteIndex > this.fireSpriteIndex) {
                this.fuelSpriteIndex--;
            }

            this.fireSpriteIndex = -1;
            this.setOverlaySprite(null);
        }
    }

    private int calcLightRadius() {
        return (int)GameTime.instance.Lerp(1.0F, 8.0F, Math.min((float)this.getFuelAmount(), 60.0F) / 60.0F);
    }

    private void updateLightSource() {
        if (this.isLit()) {
            int radius = this.calcLightRadius();
            if (this.lightSource != null && this.lightSource.getRadius() != radius) {
                this.lightSource.life = 0;
                this.lightSource = null;
            }

            if (this.lightSource == null) {
                this.lightSource = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), fireColor.r, fireColor.g, fireColor.b, radius);
                IsoWorld.instance.currentCell.addLamppost(this.lightSource);
                IsoGridSquare.recalcLightTime = -1.0F;
                Core.dirtyGlobalLightsCount++;
                GameTime.instance.lightSourceUpdate = 100.0F;
            }
        } else if (this.lightSource != null) {
            IsoWorld.instance.currentCell.removeLamppost(this.lightSource);
            this.lightSource = null;
        }
    }

    private void updateHeatSource() {
        if (this.isLit()) {
            int radius = this.calcLightRadius();
            if (this.heatSource == null) {
                this.heatSource = new IsoHeatSource(this.getXi(), this.getYi(), this.getZi(), radius, 35);
                IsoWorld.instance.currentCell.addHeatSource(this.heatSource);
            } else if (radius != this.heatSource.getRadius()) {
                this.heatSource.setRadius(radius);
            }
        } else if (this.heatSource != null) {
            IsoWorld.instance.currentCell.removeHeatSource(this.heatSource);
            this.heatSource = null;
        }
    }

    private void updateSound() {
        if (!GameServer.server) {
            if (this.isLit()) {
                if (this.emitter == null) {
                    this.emitter = IsoWorld.instance.getFreeEmitter(this.getXi() + 0.5F, this.getYi() + 0.5F, this.getZi());
                    IsoWorld.instance.setEmitterOwner(this.emitter, this);
                }

                String soundName = "FireplaceRunning";
                CraftBenchSounds craftBenchSounds = this.getComponent(ComponentType.CraftBenchSounds);
                if (craftBenchSounds != null) {
                    String soundName2 = craftBenchSounds.getSoundName("Running", null);
                    if (soundName2 != null) {
                        soundName = soundName2;
                    }
                }

                if (!this.emitter.isPlaying(soundName)) {
                    this.soundInstance = this.emitter.playSoundLoopedImpl(soundName);
                }
            } else if (this.emitter != null && this.soundInstance != 0L) {
                this.emitter.stopOrTriggerSound(this.soundInstance);
                this.emitter = null;
                this.soundInstance = 0L;
            }
        }
    }

    @Override
    public void update() {
        if (!GameClient.client) {
            boolean oldHasFuel = this.hasFuel();
            boolean oldIsLit = this.isLit();
            int oldRadius = this.calcLightRadius();
            float elapsedHours = (float)GameTime.getInstance().getWorldAgeHours();
            if (this.lastUpdateTime < 0.0F) {
                this.lastUpdateTime = elapsedHours;
            } else if (this.lastUpdateTime > elapsedHours) {
                this.lastUpdateTime = elapsedHours;
            }

            if (elapsedHours > this.lastUpdateTime) {
                this.minuteAccumulator = this.minuteAccumulator + (elapsedHours - this.lastUpdateTime) * 60.0F;
                int elapsedMinutes = (int)Math.floor(this.minuteAccumulator);
                if (elapsedMinutes > 0) {
                    if (this.isLit()) {
                        DebugLog.log(DebugType.Fireplace, "IsoFireplace burned " + elapsedMinutes + " minutes (" + this.getFuelAmount() + " remaining)");
                        this.useFuel(elapsedMinutes);
                        if (!this.hasFuel()) {
                            this.extinguish();
                        }
                    } else if (this.minutesSinceExtinguished != -1) {
                        int smolderMinutes = Math.min(elapsedMinutes, 10 - this.minutesSinceExtinguished);
                        DebugLog.log(DebugType.Fireplace, "IsoFireplace smoldered " + smolderMinutes + " minutes (" + this.getFuelAmount() + " remaining)");
                        this.minutesSinceExtinguished += elapsedMinutes;
                        this.useFuel(smolderMinutes);
                        this.smouldering = true;
                        if (!this.hasFuel() || this.minutesSinceExtinguished >= 10) {
                            this.minutesSinceExtinguished = -1;
                            this.smouldering = false;
                        }
                    }

                    this.minuteAccumulator -= elapsedMinutes;
                }
            }

            this.lastUpdateTime = elapsedHours;
            if (GameServer.server) {
                if (oldHasFuel != this.hasFuel() || oldIsLit != this.isLit() || oldRadius != this.calcLightRadius()) {
                    this.sendObjectChange("state");
                }

                return;
            }
        }

        this.updateFuelSprite();
        this.updateFireSprite();
        this.updateLightSource();
        this.updateHeatSource();
        this.updateSound();
        if (this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
            int n = this.attachedAnimSprite.size();

            for (int i = 0; i < n; i++) {
                IsoSpriteInstance s = this.attachedAnimSprite.get(i);
                IsoSprite sp = s.parentSprite;
                s.update();
                if (sp.hasAnimation()) {
                    float dt = GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F;
                    s.frame = s.frame + s.animFrameIncrease * dt;
                    if ((int)s.frame >= sp.currentAnim.frames.size() && sp.loop && s.looped) {
                        s.frame = 0.0F;
                    }
                }
            }
        }
    }

    @Override
    public void addToWorld() {
        IsoCell cell = this.getCell();
        cell.addToProcessIsoObject(this);
        if (this.sprite != null && this.sprite.getProperties() != null && this.sprite.getProperties().get("ContainerCapacity") != null) {
            this.container.addItemsToProcessItems();
        }
    }

    @Override
    public void removeFromWorld() {
        if (this.lightSource != null) {
            IsoWorld.instance.currentCell.removeLamppost(this.lightSource);
            this.lightSource = null;
        }

        if (this.heatSource != null) {
            IsoWorld.instance.currentCell.removeHeatSource(this.heatSource);
            this.heatSource = null;
        }

        super.removeFromWorld();
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        super.render(x, y, z, col, false, bWallLightingPass, shader);
        if (this.attachedAnimSprite != null) {
            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(i);
                if (!PerformanceSettings.fboRenderChunk || !spriteInstance.getParentSprite().animate) {
                    this.sx = 0.0F;
                    float offsetX = this.offsetX;
                    float offsetY = this.offsetY;
                    spriteInstance.getParentSprite().render(spriteInstance, this, x, y, z, this.dir, offsetX, offsetY, col, true);
                    this.sx = 0.0F;
                }
            }
        }
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        if ("state".equals(change)) {
            bb.putInt(this.getFuelAmount());
            bb.put((byte)(this.isLit() ? 1 : 0));
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        if ("state".equals(change)) {
            boolean wasLit = this.isLit();
            this.setFuelAmount(bb.getInt());
            this.setLit(bb.get() == 1);
            if (!wasLit && this.isLit() && this.getContainer() != null) {
                this.getContainer().addItemsToProcessItems();
            }
        }
    }

    public boolean isFireSpriteUsingOurDepthTexture() {
        return this.fireSpriteUsesOurDepthTexture;
    }

    @Override
    public boolean hasAnimatedAttachments() {
        return this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty();
    }

    @Override
    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo col) {
        if (!this.alphaForced && this.isUpdateAlphaDuringRender()) {
            this.updateAlpha(IsoCamera.frameState.playerIndex);
        }

        if (this.attachedAnimSprite != null) {
            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(i);
                if (spriteInstance.getParentSprite().animate) {
                    this.sx = 0.0F;
                    float offsetX = this.offsetX;
                    float offsetY = this.offsetY;
                    float r = col.a;
                    float g = col.g;
                    float b = col.b;
                    if (i == this.fireSpriteIndex) {
                        col.r = col.g = col.b = 1.0F;
                    }

                    spriteInstance.getParentSprite().render(spriteInstance, this, x, y, z, this.dir, offsetX, offsetY, col, true);
                    col.set(r, g, b, col.a);
                    this.sx = 0.0F;
                }
            }
        }
    }

    @Override
    public void afterRotated() {
        super.afterRotated();
        this.fireSpriteIndex = -1;
    }
}
