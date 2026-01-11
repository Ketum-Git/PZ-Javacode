// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.audio.BaseSoundEmitter;
import zombie.core.ImportantAreaManager;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.interfaces.Activatable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.scripting.objects.ItemTag;
import zombie.util.Type;

@UsedFromLua
public class IsoStove extends IsoObject implements Activatable {
    private static final ArrayList<IsoObject> s_tempObjects = new ArrayList<>();
    public static final float LitTemperature = 1.8F;
    public static final float UnlitTemperature = 1.0F;
    private static final int FireStartingEnergy = 10000;
    private static final float TemperatureAdjustmentDivisor = 700.0F;
    private static final float MinimumTemperatureIncrease = 0.05F;
    private static final float MaximumStoveTemperature = 200.0F;
    private static final float MaximumMicrowaveTemperature = 100.0F;
    private static final float BaseTemperature = 100.0F;
    private boolean activated;
    private long soundInstance = -1L;
    private float maxTemperature;
    private double stopTime;
    private double startTime;
    private float currentTemperature;
    private int secondsTimer = -1;
    private boolean firstTurnOn = true;
    private boolean broken;
    private boolean hasMetal;

    public IsoStove(IsoCell cell, IsoGridSquare sq, IsoSprite gid) {
        super(cell, sq, gid);
    }

    @Override
    public String getObjectName() {
        return "Stove";
    }

    public IsoStove(IsoCell cell) {
        super(cell);
    }

    @Override
    public boolean Activated() {
        return this.activated;
    }

    @Override
    public void update() {
        if (this.Activated() && (this.container == null || !this.container.isPowered())) {
            this.setActivated(false);
            if (this.container != null) {
                this.container.addItemsToProcessItems();
            }
        }

        if (this.Activated() && this.isMicrowave() && this.stopTime > 0.0 && this.stopTime < GameTime.instance.getWorldAgeHours()) {
            this.setActivated(false);
        }

        boolean canStartFire = GameServer.server || !GameClient.client;
        if (canStartFire && this.Activated() && this.hasMetal && Rand.Next(Rand.AdjustForFramerate(200)) == 100) {
            this.secondsTimer = -1;
            if (this.emitter != null && this.soundInstance != -1L) {
                this.emitter.stopSound(this.soundInstance);
                this.soundInstance = -1L;
            }

            this.setActivated(false);
            this.setBroken(true);
            IsoFireManager.StartFire(this.container.sourceGrid.getCell(), this.container.sourceGrid, true, 10000);
        }

        if (this.Activated()) {
            if (this.hasMetal || this.stopTime != 0.0 || this.currentTemperature < this.getMaxTemperature() || !this.container.getItems().isEmpty()) {
                ImportantAreaManager.getInstance().updateOrAdd((int)this.getX(), (int)this.getY());
            }

            if (this.isMicrowave() && this.secondsTimer == 0) {
                this.setActivated(false);
            }

            if (this.stopTime > 0.0 && this.stopTime < GameTime.instance.getWorldAgeHours()) {
                if (this.isStove() && this.isSpriteGridOriginObject()) {
                    BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
                    emitter.playSoundImpl("StoveTimerExpired", this);
                }

                this.stopTime = 0.0;
                this.startTime = 0.0;
                this.secondsTimer = -1;
            }

            if (this.getMaxTemperature() > 0.0F && this.currentTemperature < this.getMaxTemperature()) {
                float increase = (this.getMaxTemperature() - this.currentTemperature) / 700.0F;
                if (increase < 0.05F) {
                    increase = 0.05F;
                }

                this.currentTemperature = this.currentTemperature + increase * GameTime.instance.getMultiplier();
                if (this.currentTemperature > this.getMaxTemperature()) {
                    this.currentTemperature = this.getMaxTemperature();
                }
            } else if (this.currentTemperature > this.getMaxTemperature()) {
                this.currentTemperature = this.currentTemperature
                    - (this.currentTemperature - this.getMaxTemperature()) / 1000.0F * GameTime.instance.getMultiplier();
                if (this.currentTemperature < 0.0F) {
                    this.currentTemperature = 0.0F;
                }
            }
        } else if (this.currentTemperature > 0.0F) {
            this.currentTemperature = this.currentTemperature - 0.1F * GameTime.instance.getMultiplier();
            this.currentTemperature = Math.max(this.currentTemperature, 0.0F);
        }

        if (this.container != null && this.isMicrowave()) {
            if (this.Activated()) {
                this.currentTemperature = this.getMaxTemperature();
            } else {
                this.currentTemperature = 0.0F;
            }
        }

        if (this.isSpriteGridOriginObject() && this.emitter != null) {
            if (this.Activated() && this.secondsTimer > 0) {
                if (!this.emitter.isPlaying("StoveTimer")) {
                    this.emitter.playSoundImpl("StoveTimer", this);
                }
            } else if (this.emitter.isPlaying("StoveTimer")) {
                this.emitter.stopSoundByName("StoveTimer");
            }
        }

        this.checkLightSourceActive();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.activated = input.get() == 1;
        this.secondsTimer = input.getInt();
        this.maxTemperature = input.getFloat();
        this.firstTurnOn = input.get() == 1;
        this.broken = input.get() == 1;
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.put((byte)(this.activated ? 1 : 0));
        output.putInt(this.secondsTimer);
        output.putFloat(this.maxTemperature);
        output.put((byte)(this.firstTurnOn ? 1 : 0));
        output.put((byte)(this.broken ? 1 : 0));
    }

    @Override
    public void addToWorld() {
        if (this.container != null) {
            IsoCell cell = this.getCell();
            cell.addToProcessIsoObject(this);
            this.container.addItemsToProcessItems();
            this.setActivated(this.activated);
            this.addLightSourceToWorld();
        }
    }

    /**
     * Turn on or off the stove, if no electricity it won't work
     */
    @Override
    public void Toggle() {
        this.setActivated(!this.activated);
        this.container.addItemsToProcessItems();
        IsoGenerator.updateGenerator(this.square);
    }

    public void PlayToggleSound() {
        SoundManager.instance.PlayWorldSound(this.isMicrowave() ? "ToggleMicrowave" : "ToggleStove", this.getSquare(), 1.0F, 1.0F, 1.0F, false);
    }

    @Override
    public void sync() {
        this.syncIsoObject(false, (byte)(this.activated ? 1 : 0), null, null);
    }

    private void doSound() {
        if (GameServer.server) {
            this.hasMetal();
        } else if (this.isSpriteGridOriginObject()) {
            if (this.isMicrowave()) {
                if (this.activated) {
                    if (this.emitter != null) {
                        if (this.soundInstance != -1L) {
                            this.emitter.stopSound(this.soundInstance);
                        }

                        this.emitter.stopSoundByName("StoveTimer");
                    }

                    this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, PZMath.fastfloor(this.getZ()));
                    IsoWorld.instance.setEmitterOwner(this.emitter, this);
                    if (this.hasMetal()) {
                        this.soundInstance = this.emitter.playSoundLoopedImpl("MicrowaveCookingMetal");
                    } else {
                        this.soundInstance = this.emitter.playSoundLoopedImpl("MicrowaveRunning");
                    }
                } else if (this.soundInstance != -1L) {
                    if (this.emitter != null) {
                        this.emitter.stopSound(this.soundInstance);
                        this.emitter.stopSoundByName("StoveTimer");
                        this.emitter = null;
                    }

                    this.soundInstance = -1L;
                    if (this.container != null && this.container.isPowered()) {
                        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, PZMath.fastfloor(this.getZ()));
                        emitter.playSoundImpl("MicrowaveTimerExpired", this);
                    }
                }
            } else if (this.isStove()) {
                if (this.Activated()) {
                    if (this.emitter == null) {
                        this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
                        IsoWorld.instance.setEmitterOwner(this.emitter, this);
                        this.soundInstance = this.emitter.playSoundLoopedImpl("StoveRunning");
                    } else if (!this.emitter.isPlaying("StoveRunning")) {
                        this.soundInstance = this.emitter.playSoundLoopedImpl("StoveRunning");
                    }
                } else if (this.soundInstance != -1L) {
                    if (this.emitter != null) {
                        this.emitter.stopSound(this.soundInstance);
                        this.emitter.stopSoundByName("StoveTimer");
                        this.emitter = null;
                    }

                    this.soundInstance = -1L;
                }
            }
        }
    }

    private boolean hasMetal() {
        if (this.container == null) {
            return false;
        } else {
            int size = this.getContainer().getItems().size();

            for (int i = 0; i < size; i++) {
                InventoryItem item = this.getContainer().getItems().get(i);
                if (item.getMetalValue() > 0.0F || item.hasTag(ItemTag.HAS_METAL)) {
                    this.hasMetal = true;
                    return true;
                }
            }

            this.hasMetal = false;
            return false;
        }
    }

    @Override
    public String getActivatableType() {
        return "stove";
    }

    @Override
    public void syncIsoObjectSend(ByteBufferWriter b) {
        b.putInt(this.square.getX());
        b.putInt(this.square.getY());
        b.putInt(this.square.getZ());
        byte i = (byte)this.square.getObjects().indexOf(this);
        b.putByte(i);
        b.putByte((byte)1);
        b.putByte((byte)(this.activated ? 1 : 0));
        b.putInt(this.secondsTimer);
        b.putFloat(this.maxTemperature);
    }

    @Override
    public void syncIsoObject(boolean bRemote, byte val, UdpConnection source, ByteBuffer bb) {
        if (this.square == null) {
            System.out.println("ERROR: " + this.getClass().getSimpleName() + " square is null");
        } else if (this.getObjectIndex() == -1) {
            System.out
                .println(
                    "ERROR: "
                        + this.getClass().getSimpleName()
                        + " not found on square "
                        + this.square.getX()
                        + ","
                        + this.square.getY()
                        + ","
                        + this.square.getZ()
                );
        } else {
            if (GameClient.client && !bRemote) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                this.syncIsoObjectSend(b);
                PacketTypes.PacketType.SyncIsoObject.send(GameClient.connection);
            } else if (bRemote) {
                boolean enable = val == 1;
                this.secondsTimer = bb.getInt();
                this.maxTemperature = bb.getFloat();
                this.setActivated(enable);
                this.container.addItemsToProcessItems();
            }

            if (GameServer.server && GameServer.udpEngine != null) {
                for (UdpConnection connection : GameServer.udpEngine.connections) {
                    if (source == null || connection.getConnectedGUID() != source.getConnectedGUID()) {
                        ByteBufferWriter b = connection.startPacket();
                        PacketTypes.PacketType.SyncIsoObject.doPacket(b);
                        this.syncIsoObjectSend(b);
                        PacketTypes.PacketType.SyncIsoObject.send(connection);
                    }
                }
            }

            this.flagForHotSave();
        }
    }

    public void setActivated(boolean b) {
        if (!this.isBroken()) {
            this.activated = b;
            if (this.firstTurnOn && this.getMaxTemperature() == 0.0F) {
                if (this.isMicrowave() && this.secondsTimer < 0.0F) {
                    this.maxTemperature = 100.0F;
                }

                if (this.isStove() && this.secondsTimer < 0.0F) {
                    this.maxTemperature = 200.0F;
                }
            }

            if (this.firstTurnOn) {
                this.firstTurnOn = false;
            }

            if (this.activated) {
                if (this.isMicrowave() && this.secondsTimer <= 0) {
                    this.secondsTimer = 60;
                }

                if (this.secondsTimer > 0) {
                    this.startTime = GameTime.instance.getWorldAgeHours();
                    this.stopTime = this.startTime + this.secondsTimer / 3600.0F;
                }
            } else {
                this.secondsTimer = 0;
                this.stopTime = 0.0;
                this.startTime = 0.0;
                this.hasMetal = false;
            }

            this.doSound();
            if (this.getProperties() != null && this.getProperties().has(IsoFlagType.HasLightOnSprite)) {
                this.invalidateRenderChunkLevel(256L);
            }

            if (GameServer.server) {
                this.sync();
                this.syncSpriteGridObjects(true, true);
            } else if (!GameClient.client) {
                this.syncSpriteGridObjects(true, false);
            }
        }
    }

    private void doOverlay() {
        if (this.Activated() && this.getOverlaySprite() == null) {
            String[] split = this.getSprite().getName().split("_");
            String sprite = split[0] + "_" + split[1] + "_ON_" + split[2] + "_" + split[3];
            this.setOverlaySprite(sprite);
        } else if (!this.Activated()) {
            this.setOverlaySprite(null);
        }
    }

    public void setTimer(int seconds) {
        this.secondsTimer = seconds;
        if (this.activated && this.secondsTimer > 0) {
            this.startTime = GameTime.instance.getWorldAgeHours();
            this.stopTime = this.startTime + this.secondsTimer / 3600.0F;
        }
    }

    public int getTimer() {
        return this.secondsTimer;
    }

    public float getMaxTemperature() {
        return this.maxTemperature;
    }

    public void setMaxTemperature(float maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public boolean isMicrowave() {
        return this.getContainer() != null && this.getContainer().isMicrowave();
    }

    private boolean isStove() {
        return this.getContainer() != null && this.getContainer().isStove();
    }

    public int isRunningFor() {
        return this.startTime == 0.0 ? 0 : (int)((GameTime.instance.getWorldAgeHours() - this.startTime) * 3600.0);
    }

    public float getCurrentTemperature() {
        return (this.currentTemperature + 100.0F) / 100.0F;
    }

    public boolean isTemperatureChanging() {
        return this.currentTemperature != (this.activated ? this.maxTemperature : 0.0F);
    }

    public boolean isBroken() {
        return this.broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    private boolean isSpriteGridOriginObject() {
        IsoSprite sprite = this.getSprite();
        if (sprite == null) {
            return false;
        } else {
            IsoSpriteGrid spriteGrid = sprite.getSpriteGrid();
            if (spriteGrid == null) {
                return true;
            } else {
                int gridX = spriteGrid.getSpriteGridPosX(sprite);
                int gridY = spriteGrid.getSpriteGridPosY(sprite);
                return gridX == 0 && gridY == 0;
            }
        }
    }

    public void syncSpriteGridObjects(boolean toggle, boolean network) {
        this.getSpriteGridObjects(s_tempObjects);

        for (int i = s_tempObjects.size() - 1; i >= 0; i--) {
            IsoStove stove = Type.tryCastTo(s_tempObjects.get(i), IsoStove.class);
            if (stove != null && stove != this) {
                stove.activated = this.activated;
                stove.maxTemperature = this.maxTemperature;
                stove.firstTurnOn = this.firstTurnOn;
                stove.secondsTimer = this.secondsTimer;
                stove.startTime = this.startTime;
                stove.stopTime = this.stopTime;
                stove.hasMetal = this.hasMetal;
                stove.doSound();
                if (toggle) {
                    if (stove.container != null) {
                        stove.container.addItemsToProcessItems();
                    }

                    IsoGenerator.updateGenerator(stove.square);
                    if (!GameServer.server) {
                        this.invalidateRenderChunkLevel(1024L);
                    }
                }

                if (network) {
                    stove.sync();
                }
            }
        }
    }

    @Override
    public boolean shouldShowOnOverlay() {
        if (!this.Activated()) {
            return false;
        } else {
            int playerIndex = IsoCamera.frameState.playerIndex;
            return this.getSquare() != null && this.getSquare().isSeen(playerIndex);
        }
    }

    @Override
    protected boolean shouldLightSourceBeActive() {
        return this.Activated();
    }

    @Override
    public void afterRotated() {
        super.afterRotated();
        this.setOnOverlay(null);
    }
}
