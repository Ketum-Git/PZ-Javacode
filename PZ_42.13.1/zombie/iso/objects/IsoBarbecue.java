// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public class IsoBarbecue extends IsoObject {
    private static final float LitTemperature = 1.8F;
    private static final float UnlitTemperature = 1.0F;
    private static final int SmoulderMinutes = 10;
    private static final int MaxFuelAmount = 1200;
    private static final int SpriteWithPropaneTankOffset = 8;
    private static final int SpriteWithoutPropaneTankOffset = -8;
    private static final short SpriteOffsetX = 0;
    private static final short SpriteOffsetY = -58;
    private static final int IsoHeatSourceRadius = 3;
    private static final int IsoHeatSourceTemperature = 25;
    private static final ColorInfo SmokeTint = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    private boolean hasPropaneTank;
    private int fuelAmountMinutes;
    private boolean lit;
    private boolean isSmouldering;
    protected float lastUpdateTime = -1.0F;
    protected float minuteAccumulator;
    protected int minutesSinceExtinguished = -1;
    private IsoSprite normalIsoSprite;
    private IsoSprite noTankIsoSprite;
    private IsoHeatSource isoHeatSource;
    private long soundInstance;

    public IsoBarbecue(IsoCell cell) {
        super(cell);
    }

    public IsoBarbecue(IsoCell cell, IsoGridSquare isoGridSquare, IsoSprite isoSprite) {
        super(cell, isoGridSquare, isoSprite);
        String containerType = isoSprite != null && isoSprite.getProperties().has(IsoFlagType.container)
            ? isoSprite.getProperties().get("container")
            : "barbecue";
        if (this.sprite != null && this.sprite.getProperties() != null && this.sprite.getProperties().get("ContainerCapacity") != null) {
            this.container = new ItemContainer(containerType, isoGridSquare, this);
            this.container.capacity = Integer.parseInt(this.sprite.getProperties().get("ContainerCapacity"));
        }

        if (isSpriteWithPropaneTank(this.sprite)) {
            this.hasPropaneTank = true;
            this.fuelAmountMinutes = Rand.Next(1201);
            this.normalIsoSprite = this.sprite;
            this.noTankIsoSprite = IsoSprite.getSprite(IsoSpriteManager.instance, this.sprite, 8);
        } else if (isSpriteWithoutPropaneTank(this.sprite)) {
            this.normalIsoSprite = IsoSprite.getSprite(IsoSpriteManager.instance, this.sprite, -8);
            this.noTankIsoSprite = this.sprite;
        }
    }

    @Override
    public String getObjectName() {
        return "Barbecue";
    }

    @Override
    public void load(ByteBuffer input, int worldVersion, boolean isDebugSave) throws IOException {
        super.load(input, worldVersion, isDebugSave);
        this.hasPropaneTank = input.get() == 1;
        this.fuelAmountMinutes = input.getInt();
        this.lit = input.get() == 1;
        this.lastUpdateTime = input.getFloat();
        this.minutesSinceExtinguished = input.getInt();
        if (input.get() == 1) {
            this.normalIsoSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }

        if (input.get() == 1) {
            this.noTankIsoSprite = IsoSprite.getSprite(IsoSpriteManager.instance, input.getInt());
        }
    }

    @Override
    public void save(ByteBuffer output, boolean isDebugSave) throws IOException {
        super.save(output, isDebugSave);
        output.put((byte)(this.hasPropaneTank ? 1 : 0));
        output.putInt(this.fuelAmountMinutes);
        output.put((byte)(this.lit ? 1 : 0));
        output.putFloat(this.lastUpdateTime);
        output.putInt(this.minutesSinceExtinguished);
        if (this.normalIsoSprite != null) {
            output.put((byte)1);
            output.putInt(this.normalIsoSprite.id);
        } else {
            output.put((byte)0);
        }

        if (this.noTankIsoSprite != null) {
            output.put((byte)1);
            output.putInt(this.noTankIsoSprite.id);
        } else {
            output.put((byte)0);
        }
    }

    public void setFuelAmount(int fuelAmount) {
        fuelAmount = Math.max(0, fuelAmount);
        int old = this.getFuelAmount();
        if (fuelAmount != old) {
            this.fuelAmountMinutes = fuelAmount;
        }
    }

    public int getFuelAmount() {
        return this.fuelAmountMinutes;
    }

    public void addFuel(int fuelAmount) {
        this.setFuelAmount(this.getFuelAmount() + fuelAmount);
    }

    public int useFuel(int amount) {
        int availableFuel = this.getFuelAmount();
        int usedFuel;
        if (availableFuel >= amount) {
            usedFuel = amount;
        } else {
            usedFuel = availableFuel;
        }

        this.setFuelAmount(availableFuel - usedFuel);
        return usedFuel;
    }

    public boolean hasFuel() {
        return this.getFuelAmount() > 0;
    }

    @Override
    public boolean hasPropaneTank() {
        return this.isPropaneBBQ() && this.hasPropaneTank;
    }

    @Override
    public boolean isPropaneBBQ() {
        return this.getSprite() != null && this.getProperties().has("propaneTank");
    }

    public static boolean isSpriteWithPropaneTank(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has("propaneTank")) {
            IsoSprite sprite2 = IsoSprite.getSprite(IsoSpriteManager.instance, sprite, 8);
            return sprite2 != null && sprite2.getProperties().has("propaneTank");
        } else {
            return false;
        }
    }

    public static boolean isSpriteWithoutPropaneTank(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has("propaneTank")) {
            IsoSprite sprite2 = IsoSprite.getSprite(IsoSpriteManager.instance, sprite, -8);
            return sprite2 != null && sprite2.getProperties().has("propaneTank");
        } else {
            return false;
        }
    }

    public void setPropaneTank(InventoryItem tank) {
        if (tank.getFullType().equals("Base.PropaneTank")) {
            this.hasPropaneTank = true;
            this.fuelAmountMinutes = Math.round(tank.getCurrentUses() * 0.24F);
            if (Thread.currentThread() == GameWindow.gameThread) {
                this.updateSprite();
                this.invalidateRenderChunkLevel(256L);
            }
        }
    }

    public InventoryItem removePropaneTank() {
        if (!this.hasPropaneTank) {
            return null;
        } else {
            this.hasPropaneTank = false;
            this.lit = false;
            InventoryItem tank = InventoryItemFactory.CreateItem("Base.PropaneTank");
            tank.setCurrentUses(Math.round(this.getFuelAmount() / 0.24F));
            this.fuelAmountMinutes = 0;
            if (Thread.currentThread() == GameWindow.gameThread) {
                this.updateSprite();
                this.invalidateRenderChunkLevel(256L);
            }

            return tank;
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
        return this.isSmouldering;
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

    public void turnOff() {
        if (this.isLit()) {
            this.setLit(false);
        }
    }

    public void toggle() {
        if (this.isLit()) {
            this.turnOff();
        } else {
            this.turnOn();
        }
    }

    public void extinguish() {
        if (this.isLit()) {
            this.setLit(false);
            if (this.hasFuel() && !this.isPropaneBBQ()) {
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

    private void updateSprite() {
        if (this.isPropaneBBQ()) {
            if (this.hasPropaneTank()) {
                this.sprite = this.normalIsoSprite;
            } else {
                this.sprite = this.noTankIsoSprite;
            }
        }
    }

    private void updateHeatSource() {
        if (this.isLit()) {
            if (this.isoHeatSource == null) {
                this.isoHeatSource = new IsoHeatSource(PZMath.fastfloor(this.getX()), PZMath.fastfloor(this.getY()), PZMath.fastfloor(this.getZ()), 3, 25);
                IsoWorld.instance.currentCell.addHeatSource(this.isoHeatSource);
            }
        } else if (this.isoHeatSource != null) {
            IsoWorld.instance.currentCell.removeHeatSource(this.isoHeatSource);
            this.isoHeatSource = null;
        }
    }

    private void updateSound() {
        if (!GameServer.server) {
            if (this.isLit()) {
                if (this.emitter == null) {
                    this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, PZMath.fastfloor(this.getZ()));
                    IsoWorld.instance.setEmitterOwner(this.emitter, this);
                }

                String soundName = this.isPropaneBBQ() ? "BBQPropaneRunning" : "BBQRegularRunning";
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
            float elapsedHours = (float)GameTime.getInstance().getWorldAgeHours();
            if (this.lastUpdateTime < 0.0F) {
                this.lastUpdateTime = elapsedHours;
            } else if (this.lastUpdateTime > elapsedHours) {
                this.lastUpdateTime = elapsedHours;
            }

            if (elapsedHours > this.lastUpdateTime) {
                this.minuteAccumulator = this.minuteAccumulator + (elapsedHours - this.lastUpdateTime) * 60.0F;
                int elapsedMinutes = PZMath.fastfloor(this.minuteAccumulator);
                if (elapsedMinutes > 0) {
                    if (this.isLit()) {
                        DebugLog.log(DebugType.Fireplace, "IsoBarbecue burned " + elapsedMinutes + " minutes (" + this.getFuelAmount() + " remaining)");
                        this.useFuel(elapsedMinutes);
                        if (!this.hasFuel()) {
                            this.extinguish();
                        }
                    } else if (this.minutesSinceExtinguished != -1) {
                        int smolderMinutes = Math.min(elapsedMinutes, 10 - this.minutesSinceExtinguished);
                        DebugLog.log(DebugType.Fireplace, "IsoBarbecue smoldered " + smolderMinutes + " minutes (" + this.getFuelAmount() + " remaining)");
                        this.minutesSinceExtinguished += elapsedMinutes;
                        this.isSmouldering = true;
                        this.useFuel(smolderMinutes);
                        if (!this.hasFuel() || this.minutesSinceExtinguished >= 10) {
                            this.minutesSinceExtinguished = -1;
                            this.isSmouldering = false;
                        }
                    }

                    this.minuteAccumulator -= elapsedMinutes;
                }
            }

            this.lastUpdateTime = elapsedHours;
            if (GameServer.server) {
                if (oldHasFuel != this.hasFuel() || oldIsLit != this.isLit()) {
                    this.sendObjectChange("state");
                }

                return;
            }
        }

        this.updateSprite();
        this.updateHeatSource();
        if (!this.isLit() || this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
            if (!this.isLit() && this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty()) {
                this.RemoveAttachedAnims();
            }
        } else {
            this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 20, 58, true, 0, false, 0.7F, SmokeTint);
            this.attachedAnimSprite.get(0).alpha = this.attachedAnimSprite.get(0).targetAlpha = 1.0F;
            this.attachedAnimSprite.get(0).copyTargetAlpha = false;
        }

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

        this.updateSound();
    }

    /**
     * 
     * @param isoSprite the sprite to set
     */
    @Override
    public void setSprite(IsoSprite isoSprite) {
        if (isSpriteWithPropaneTank(isoSprite)) {
            this.normalIsoSprite = isoSprite;
            this.noTankIsoSprite = IsoSprite.getSprite(IsoSpriteManager.instance, isoSprite, 8);
        } else if (isSpriteWithoutPropaneTank(isoSprite)) {
            this.normalIsoSprite = IsoSprite.getSprite(IsoSpriteManager.instance, isoSprite, -8);
            this.noTankIsoSprite = isoSprite;
        }

        this.invalidateRenderChunkLevel(256L);
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
        if (this.container != null) {
            this.container.addItemsToProcessItems();
        }
    }

    @Override
    public void removeFromWorld() {
        if (this.isoHeatSource != null) {
            IsoWorld.instance.currentCell.removeHeatSource(this.isoHeatSource);
            this.isoHeatSource = null;
        }

        super.removeFromWorld();
    }

    @Override
    public void render(float x, float y, float z, ColorInfo colorInfo, boolean doChild, boolean wallLightingPass, Shader shader) {
        if (this.attachedAnimSprite != null) {
            int tileScale = Core.tileScale;

            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(i);
                IsoSprite sprite = spriteInstance.parentSprite;
                sprite.soffX = (short)(0 * tileScale);
                sprite.soffY = (short)(-58 * tileScale);
                spriteInstance.setScale(tileScale, tileScale);
            }
        }

        if (PerformanceSettings.fboRenderChunk) {
            doChild = false;
        }

        super.render(x, y, z, colorInfo, doChild, wallLightingPass, shader);
    }

    @Override
    public void saveChange(String change, KahluaTable kahluaTable, ByteBuffer byteBuffer) {
        if ("state".equals(change)) {
            byteBuffer.putInt(this.getFuelAmount());
            byteBuffer.put((byte)(this.isLit() ? 1 : 0));
            byteBuffer.put((byte)(this.hasPropaneTank() ? 1 : 0));
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer byteBuffer) {
        if ("state".equals(change)) {
            boolean wasLit = this.isLit();
            this.setFuelAmount(byteBuffer.getInt());
            this.setLit(byteBuffer.get() == 1);
            this.hasPropaneTank = byteBuffer.get() == 1;
            if (!wasLit && this.isLit() && this.getContainer() != null) {
                this.getContainer().addItemsToProcessItems();
            }
        }
    }

    @Override
    public boolean hasAnimatedAttachments() {
        return this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty();
    }

    @Override
    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo colorInfo) {
        if (this.attachedAnimSprite != null) {
            int tileScale = Core.tileScale;

            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(i);
                IsoSprite sprite = spriteInstance.getParentSprite();
                if (sprite.animate) {
                    sprite.soffX = (short)(0 * tileScale);
                    sprite.soffY = (short)(-58 * tileScale);
                    spriteInstance.setScale(tileScale, tileScale);
                    sprite.render(spriteInstance, this, x, y, z, this.dir, this.offsetX, this.offsetY, colorInfo, true);
                }
            }
        }
    }
}
