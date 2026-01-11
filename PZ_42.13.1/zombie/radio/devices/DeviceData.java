// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.devices;

import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.IFMODParameterUpdater;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterDeviceVolume;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Color;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.VoiceManager;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemUser;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Radio;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoWaveSignal;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.MediaData;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.ItemTag;
import zombie.vehicles.VehiclePart;

/**
 * Turbo
 *  Stores shared data for devices (used in iso and item)
 */
@UsedFromLua
public final class DeviceData implements Cloneable, IFMODParameterUpdater {
    private static final float deviceSpeakerSoundMod = 1.0F;
    private static final float deviceButtonSoundVol = 1.0F;
    protected String deviceName = "WaveSignalDevice";
    protected boolean twoWay;
    protected int transmitRange = 1000;
    protected int micRange = 5;
    protected boolean micIsMuted;
    protected float baseVolumeRange = 15.0F;
    protected float deviceVolume = 1.0F;
    protected boolean isPortable;
    protected boolean isTelevision;
    protected boolean isHighTier;
    protected boolean isTurnedOn;
    protected int channel = 88000;
    protected int minChannelRange = 200;
    protected int maxChannelRange = 1000000;
    protected DevicePresets presets;
    protected boolean isBatteryPowered = true;
    protected boolean hasBattery = true;
    protected float powerDelta = 1.0F;
    protected float useDelta = 0.001F;
    protected int lastRecordedDistance = -1;
    protected int headphoneType = -1;
    protected WaveSignalDevice parent;
    protected GameTime gameTime;
    protected boolean channelChangedRecently;
    protected BaseSoundEmitter emitter;
    protected FMODParameterList parameterList = new FMODParameterList();
    protected ParameterDeviceVolume parameterDeviceVolume = new ParameterDeviceVolume(this);
    protected short mediaIndex = -1;
    protected byte mediaType = -1;
    protected String mediaItem;
    protected MediaData playingMedia;
    protected boolean isPlayingMedia;
    protected int mediaLineIndex;
    protected float lineCounter;
    protected String currentMediaLine;
    protected Color currentMediaColor;
    protected boolean isStoppingMedia;
    protected float stopMediaCounter;
    protected boolean noTransmit;
    private final float soundCounterStatic = 0.0F;
    protected long radioLoopSound;
    protected boolean doTriggerWorldSound;
    protected long lastMinuteStamp = -1L;
    protected int listenCnt;
    float nextStaticSound;
    protected float voipCounter;
    protected float signalCounter;
    protected float soundCounter;
    float minmod = 1.5F;
    float maxmod = 5.0F;

    public DeviceData() {
        this(null);
    }

    public DeviceData(WaveSignalDevice parent) {
        this.parent = parent;
        this.presets = new DevicePresets();
        this.gameTime = GameTime.getInstance();
        this.parameterList.add(this.parameterDeviceVolume);
    }

    public void generatePresets() {
        if (this.presets == null) {
            this.presets = new DevicePresets();
        }

        this.presets.clearPresets();
        if (this.isTelevision) {
            Map<Integer, String> category = ZomboidRadio.getInstance().GetChannelList("Television");
            if (category != null) {
                for (Entry<Integer, String> entry : category.entrySet()) {
                    if (entry.getKey() >= this.minChannelRange && entry.getKey() <= this.maxChannelRange) {
                        this.presets.addPreset(entry.getValue(), entry.getKey());
                    }
                }
            }
        } else {
            int radiochance = this.twoWay ? 100 : 300;
            if (this.isHighTier) {
                radiochance = 800;
            }

            Map<Integer, String> category = ZomboidRadio.getInstance().GetChannelList("Emergency");
            if (category != null) {
                for (Entry<Integer, String> entryx : category.entrySet()) {
                    if (entryx.getKey() >= this.minChannelRange && entryx.getKey() <= this.maxChannelRange && Rand.Next(1000) < radiochance) {
                        this.presets.addPreset(entryx.getValue(), entryx.getKey());
                    }
                }
            }

            radiochance = this.twoWay ? 100 : 800;
            category = ZomboidRadio.getInstance().GetChannelList("Radio");
            if (category != null) {
                for (Entry<Integer, String> entryxx : category.entrySet()) {
                    if (entryxx.getKey() >= this.minChannelRange && entryxx.getKey() <= this.maxChannelRange && Rand.Next(1000) < radiochance) {
                        this.presets.addPreset(entryxx.getValue(), entryxx.getKey());
                    }
                }
            }

            if (this.twoWay) {
                category = ZomboidRadio.getInstance().GetChannelList("Amateur");
                if (category != null) {
                    for (Entry<Integer, String> entryxxx : category.entrySet()) {
                        if (entryxxx.getKey() >= this.minChannelRange && entryxxx.getKey() <= this.maxChannelRange && Rand.Next(1000) < radiochance) {
                            this.presets.addPreset(entryxxx.getValue(), entryxxx.getKey());
                        }
                    }
                }
            }

            if (this.isHighTier) {
                category = ZomboidRadio.getInstance().GetChannelList("Military");
                if (category != null) {
                    for (Entry<Integer, String> entryxxxx : category.entrySet()) {
                        if (entryxxxx.getKey() >= this.minChannelRange && entryxxxx.getKey() <= this.maxChannelRange && Rand.Next(1000) < 10) {
                            this.presets.addPreset(entryxxxx.getValue(), entryxxxx.getKey());
                        }
                    }
                }
            }
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        DeviceData c = (DeviceData)super.clone();
        c.setDevicePresets((DevicePresets)this.presets.clone());
        c.setParent(null);
        c.emitter = null;
        c.parameterDeviceVolume = new ParameterDeviceVolume(c);
        c.parameterList = new FMODParameterList();
        c.parameterList.add(c.parameterDeviceVolume);
        return c;
    }

    public DeviceData getClone() {
        DeviceData d;
        try {
            d = (DeviceData)this.clone();
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
            d = new DeviceData();
        }

        return d;
    }

    public WaveSignalDevice getParent() {
        return this.parent;
    }

    public void setParent(WaveSignalDevice p) {
        this.parent = p;
    }

    public DevicePresets getDevicePresets() {
        return this.presets;
    }

    public void setDevicePresets(DevicePresets p) {
        if (p == null) {
            p = new DevicePresets();
        }

        this.presets = p;
    }

    public void cloneDevicePresets(DevicePresets p) throws CloneNotSupportedException {
        this.presets.clearPresets();
        if (p != null) {
            for (int i = 0; i < p.presets.size(); i++) {
                PresetEntry entry = p.presets.get(i);
                this.presets.addPreset(entry.name, entry.frequency);
            }
        }
    }

    public int getMinChannelRange() {
        return this.minChannelRange;
    }

    public void setMinChannelRange(int i) {
        this.minChannelRange = i >= 200 && i <= 1000000 ? i : 200;
    }

    public int getMaxChannelRange() {
        return this.maxChannelRange;
    }

    public void setMaxChannelRange(int i) {
        this.maxChannelRange = i >= 200 && i <= 1000000 ? i : 1000000;
    }

    public boolean getIsHighTier() {
        return this.isHighTier;
    }

    public void setIsHighTier(boolean b) {
        this.isHighTier = b;
    }

    public boolean getIsBatteryPowered() {
        return this.isBatteryPowered;
    }

    public void setIsBatteryPowered(boolean b) {
        this.isBatteryPowered = b;
    }

    public boolean getHasBattery() {
        return this.hasBattery;
    }

    public void setHasBattery(boolean b) {
        this.hasBattery = b;
    }

    public void addBattery(DrainableComboItem bat) {
        if (!this.hasBattery && bat != null && bat.getFullType().equals("Base.Battery")) {
            ItemContainer container = bat.getContainer();
            if (container != null) {
                if (container.getType().equals("floor") && bat.getWorldItem() != null && bat.getWorldItem().getSquare() != null) {
                    bat.getWorldItem().getSquare().transmitRemoveItemFromSquare(bat.getWorldItem());
                    bat.getWorldItem().getSquare().getWorldObjects().remove(bat.getWorldItem());
                    bat.getWorldItem().getSquare().getObjects().remove(bat.getWorldItem());
                    bat.setWorldItem(null);
                }

                this.powerDelta = bat.getCurrentUsesFloat();
                container.DoRemoveItem(bat);
                this.hasBattery = true;
                this.transmitDeviceDataState((short)2);
            }
        }
    }

    public InventoryItem getBattery(ItemContainer inventory) {
        if (this.hasBattery) {
            DrainableComboItem bat = InventoryItemFactory.CreateItem("Base.Battery");
            bat.setCurrentUses((int)(bat.getMaxUses() * this.powerDelta));
            this.powerDelta = 0.0F;
            inventory.AddItem(bat);
            this.hasBattery = false;
            this.transmitDeviceDataState((short)2);
            return bat;
        } else {
            return null;
        }
    }

    public void transmitBattryChange() {
        this.transmitDeviceDataState((short)2);
    }

    public void addHeadphones(InventoryItem headphones) {
        if (this.headphoneType < 0 && (headphones.getFullType().equals("Base.Headphones") || headphones.getFullType().equals("Base.Earbuds"))) {
            ItemContainer container = headphones.getContainer();
            if (container != null) {
                if (container.getType().equals("floor") && headphones.getWorldItem() != null && headphones.getWorldItem().getSquare() != null) {
                    headphones.getWorldItem().getSquare().transmitRemoveItemFromSquare(headphones.getWorldItem());
                    headphones.getWorldItem().getSquare().getWorldObjects().remove(headphones.getWorldItem());
                    headphones.getWorldItem().getSquare().getObjects().remove(headphones.getWorldItem());
                    headphones.setWorldItem(null);
                }

                int type = headphones.getFullType().equals("Base.Headphones") ? 0 : 1;
                container.DoRemoveItem(headphones);
                this.setHeadphoneType(type);
                this.transmitDeviceDataState((short)6);
            }
        }
    }

    public InventoryItem getHeadphones(ItemContainer inventory) {
        if (this.headphoneType >= 0) {
            InventoryItem headphones = null;
            if (this.headphoneType == 0) {
                headphones = InventoryItemFactory.CreateItem("Base.Headphones");
            } else if (this.headphoneType == 1) {
                headphones = InventoryItemFactory.CreateItem("Base.Earbuds");
            }

            if (headphones != null) {
                inventory.AddItem(headphones);
            }

            this.setHeadphoneType(-1);
            this.transmitDeviceDataState((short)6);
        }

        return null;
    }

    public int getMicRange() {
        return this.micRange;
    }

    public void setMicRange(int i) {
        this.micRange = i;
    }

    public boolean getMicIsMuted() {
        return this.micIsMuted;
    }

    public void setMicIsMuted(boolean b) {
        this.micIsMuted = b;
        if (this.getParent() != null
            && this.getParent() instanceof Radio
            && ((Radio)this.getParent()).getEquipParent() != null
            && ((Radio)this.getParent()).getEquipParent() instanceof IsoPlayer parent) {
            parent.updateEquippedRadioFreq();
        }
    }

    public int getHeadphoneType() {
        return this.headphoneType;
    }

    public void setHeadphoneType(int i) {
        this.headphoneType = i;
    }

    public float getBaseVolumeRange() {
        return this.baseVolumeRange;
    }

    public void setBaseVolumeRange(float f) {
        this.baseVolumeRange = f;
    }

    public float getDeviceVolume() {
        return this.deviceVolume;
    }

    public void setDeviceVolume(float f) {
        this.deviceVolume = f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
        this.transmitDeviceDataState((short)4);
    }

    public void setDeviceVolumeRaw(float f) {
        this.deviceVolume = f < 0.0F ? 0.0F : (f > 1.0F ? 1.0F : f);
    }

    public boolean getIsTelevision() {
        return this.isTelevision;
    }

    public boolean isTelevision() {
        return this.getIsTelevision();
    }

    public void setIsTelevision(boolean b) {
        this.isTelevision = b;
    }

    public boolean canPlayerRemoteInteract(IsoGameCharacter character) {
        if (!this.isTelevision() || character == null || this.getIsoObject() == null) {
            return false;
        } else if (!character.CanSee(this.getIsoObject())) {
            return false;
        } else {
            return character.getPrimaryHandItem() != null && character.getPrimaryHandItem().hasTag(ItemTag.TVREMOTE)
                ? true
                : character.getSecondaryHandItem() != null && character.getSecondaryHandItem().hasTag(ItemTag.TVREMOTE);
        }
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String name) {
        this.deviceName = name;
    }

    public boolean getIsTwoWay() {
        return this.twoWay;
    }

    public void setIsTwoWay(boolean b) {
        this.twoWay = b;
    }

    public int getTransmitRange() {
        return this.transmitRange;
    }

    public void setTransmitRange(int range) {
        this.transmitRange = range > 0 ? range : 0;
    }

    public boolean getIsPortable() {
        return this.isPortable;
    }

    public void setIsPortable(boolean b) {
        this.isPortable = b;
    }

    public boolean getIsTurnedOn() {
        return this.isTurnedOn;
    }

    public void setIsTurnedOn(boolean b) {
        if (this.canBePoweredHere()) {
            if (this.isBatteryPowered && !(this.powerDelta > 0.0F)) {
                this.isTurnedOn = false;
            } else {
                this.isTurnedOn = b;
            }

            this.transmitDeviceDataState((short)0);
        } else if (this.isTurnedOn) {
            this.isTurnedOn = false;
            this.transmitDeviceDataState((short)0);
        }

        if (this.getParent() != null
            && this.getParent() instanceof Radio
            && ((Radio)this.getParent()).getEquipParent() != null
            && ((Radio)this.getParent()).getEquipParent() instanceof IsoPlayer parent) {
            parent.updateEquippedRadioFreq();
        }

        IsoGenerator.updateGenerator(this.getParent().getSquare());
    }

    public void setTurnedOnRaw(boolean b) {
        this.isTurnedOn = b;
        if (this.getParent() != null
            && this.getParent() instanceof Radio
            && ((Radio)this.getParent()).getEquipParent() != null
            && ((Radio)this.getParent()).getEquipParent() instanceof IsoPlayer parent) {
            parent.updateEquippedRadioFreq();
        }
    }

    public boolean canBePoweredHere() {
        if (this.isBatteryPowered) {
            return true;
        } else if (this.parent instanceof VehiclePart part) {
            return part.isInventoryItemUninstalled() ? false : part.hasDevicePower();
        } else {
            boolean canBePoweredHere = false;
            if (this.parent.getSquare().hasGridPower()) {
                canBePoweredHere = true;
            }

            if (this.parent == null || this.parent.getSquare() == null) {
                canBePoweredHere = false;
            } else if (this.parent.getSquare().haveElectricity()) {
                canBePoweredHere = true;
            } else if (this.parent.getSquare().getRoom() == null) {
                canBePoweredHere = false;
            }

            return canBePoweredHere;
        }
    }

    public void setRandomChannel() {
        if (this.presets != null && !this.presets.getPresets().isEmpty()) {
            int r = Rand.Next(0, this.presets.getPresets().size());
            this.channel = this.presets.getPresets().get(r).getFrequency();
        } else {
            this.channel = Rand.Next(this.minChannelRange, this.maxChannelRange);
            this.channel = this.channel - this.channel % 200;
        }
    }

    public int getChannel() {
        return this.channel;
    }

    public void setChannel(int c) {
        this.setChannel(c, true);
    }

    public void setChannel(int chan, boolean setislistening) {
        if (chan >= this.minChannelRange && chan <= this.maxChannelRange) {
            this.channel = chan;
            if (this.isTelevision) {
                this.playSoundSend("TelevisionZap", true);
            } else if (this.isVehicleDevice()) {
                this.playSoundSend("VehicleRadioZap", true);
            } else {
                this.playSoundSend("RadioZap", true);
            }

            if (this.radioLoopSound > 0L) {
                this.emitter.stopSound(this.radioLoopSound);
                this.radioLoopSound = 0L;
            }

            this.transmitDeviceDataState((short)1);
            if (setislistening) {
                this.TriggerPlayerListening(true);
            }
        }
    }

    public void setChannelRaw(int chan) {
        this.channel = chan;
    }

    public float getUseDelta() {
        return this.useDelta;
    }

    public void setUseDelta(float f) {
        this.useDelta = f / 60.0F;
    }

    public float getPower() {
        return this.powerDelta;
    }

    public void setPower(float p) {
        if (p > 1.0F) {
            p = 1.0F;
        }

        if (p < 0.0F) {
            p = 0.0F;
        }

        this.powerDelta = p;
    }

    public void setInitialPower() {
        this.lastMinuteStamp = this.gameTime.getMinutesStamp();
        this.setPower(this.powerDelta - this.useDelta * (float)this.lastMinuteStamp);
    }

    public void TriggerPlayerListening(boolean listening) {
        if (this.isTurnedOn) {
            ZomboidRadio.getInstance().PlayerListensChannel(this.channel, true, this.isTelevision);
        }
    }

    public void playSoundSend(String soundname, boolean useDeviceVolume) {
        this.playSound(soundname, useDeviceVolume ? this.deviceVolume * 1.0F : 1.0F, true);
    }

    public void playSoundLocal(String soundname, boolean useDeviceVolume) {
        this.playSound(soundname, useDeviceVolume ? this.deviceVolume * 1.0F : 1.0F, false);
    }

    public void playSound(String soundname, float volume, boolean transmit) {
        if (!GameServer.server) {
            this.setEmitterAndPos();
            if (this.emitter != null) {
                long id = transmit ? this.emitter.playSound(soundname) : this.emitter.playSoundImpl(soundname, (IsoObject)null);
                this.setSoundVolume(id, volume);
            }
        }
    }

    private void setSoundVolume(long eventInstance, float volume) {
        if (!this.emitter.isUsingParameter(eventInstance, "DeviceVolume")) {
            this.emitter.setVolume(eventInstance, volume);
        }
    }

    public void stopOrTriggerSoundByName(String soundName) {
        if (this.emitter != null) {
            this.emitter.stopOrTriggerSoundByName(soundName);
        }
    }

    public void cleanSoundsAndEmitter() {
        if (this.emitter != null) {
            this.emitter.stopAll();
            if (this.emitter instanceof FMODSoundEmitter fmodSoundEmitter) {
                fmodSoundEmitter.parameterUpdater = null;
            }

            IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
            this.emitter = null;
            this.radioLoopSound = 0L;
        }
    }

    public IsoObject getIsoObject() {
        if (this.parent == null) {
            return null;
        } else if (this.parent instanceof IsoObject object) {
            return object;
        } else if (this.parent instanceof Radio item) {
            ItemContainer container = item.getOutermostContainer();
            return container == null ? null : container.getParent();
        } else {
            return this.parent instanceof VehiclePart vehiclePart ? vehiclePart.getVehicle() : null;
        }
    }

    protected void setEmitterAndPos() {
        IsoObject source = this.getIsoObject();
        if (source != null) {
            float emitterX = source.getX() + (!this.isVehicleDevice() && !(source instanceof IsoGameCharacter) ? 0.5F : 0.0F);
            float emitterY = source.getY() + (!this.isVehicleDevice() && !(source instanceof IsoGameCharacter) ? 0.5F : 0.0F);
            if (this.emitter == null) {
                this.emitter = IsoWorld.instance.getFreeEmitter(emitterX, emitterY, PZMath.fastfloor(source.getZ()));
                IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
                if (this.emitter instanceof FMODSoundEmitter fmodSoundEmitter) {
                    fmodSoundEmitter.parameterUpdater = this;
                }
            } else {
                this.emitter.setPos(emitterX, emitterY, PZMath.fastfloor(source.getZ()));
            }

            if (this.radioLoopSound != 0L) {
                this.setSoundVolume(this.radioLoopSound, this.deviceVolume * 1.0F);
            }
        }
    }

    private float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr != null && !chr.hasTrait(CharacterTrait.DEAF) && chr.getCurrentSquare() != null) {
                float px = chr.getX();
                float py = chr.getY();
                float pz = chr.getZ();
                float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0F, soundX, soundY, soundZ * 3.0F);
                distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0F);
                if (distSq < minDist) {
                    minDist = distSq;
                }
            }
        }

        return minDist;
    }

    protected void updateEmitter() {
        if (!GameServer.server) {
            this.parameterList.update();
            IsoObject isoObject = this.getIsoObject();
            float distSq = isoObject == null ? Float.MAX_VALUE : this.getClosestListener(isoObject.getX(), isoObject.getY(), isoObject.getZ());
            if (this.isTurnedOn && !(distSq > 256.0F)) {
                this.setEmitterAndPos();
                if (this.emitter != null) {
                    String soundName = "RadioTalk";
                    if (this.isVehicleDevice()) {
                        soundName = "VehicleRadioProgram";
                    }

                    if (this.isEmergencyBroadcast()) {
                        soundName = "BroadcastEmergency";
                    }

                    if (this.signalCounter > 0.0F && !this.emitter.isPlaying(soundName)) {
                        if (this.radioLoopSound > 0L) {
                            this.emitter.stopSound(this.radioLoopSound);
                        }

                        this.radioLoopSound = this.emitter.playSoundImpl(soundName, (IsoObject)null);
                        this.setSoundVolume(this.radioLoopSound, this.deviceVolume * 1.0F);
                    }

                    String loopsound = !this.isTelevision ? "RadioStatic" : "TelevisionTestBeep";
                    if (this.isVehicleDevice()) {
                        loopsound = "VehicleRadioStatic";
                    }

                    if (this.radioLoopSound == 0L || this.signalCounter <= 0.0F && !this.emitter.isPlaying(loopsound)) {
                        if (this.radioLoopSound > 0L) {
                            this.emitter.stopOrTriggerSound(this.radioLoopSound);
                            if (!this.isTelevision) {
                                if (this.isVehicleDevice()) {
                                    this.playSoundSend("VehicleRadioZap", true);
                                } else {
                                    this.playSoundLocal("RadioZap", true);
                                }
                            }
                        }

                        this.radioLoopSound = this.emitter.playSoundImpl(loopsound, (IsoObject)null);
                        this.setSoundVolume(this.radioLoopSound, this.deviceVolume * 1.0F);
                    }

                    this.emitter.tick();
                }
            } else if (this.emitter != null
                && (this.emitter.isPlaying("RadioButton") || this.emitter.isPlaying("TelevisionOff") || this.emitter.isPlaying("VehicleRadioButton"))) {
                if (this.radioLoopSound > 0L) {
                    this.emitter.stopSound(this.radioLoopSound);
                }

                this.setEmitterAndPos();
                this.emitter.tick();
            } else {
                this.cleanSoundsAndEmitter();
            }
        }
    }

    public BaseSoundEmitter getEmitter() {
        return this.emitter;
    }

    public void update(boolean isIso, boolean playerInRange) {
        if (this.lastMinuteStamp == -1L) {
            this.lastMinuteStamp = this.gameTime.getMinutesStamp();
        }

        if (this.gameTime.getMinutesStamp() > this.lastMinuteStamp) {
            long diff = this.gameTime.getMinutesStamp() - this.lastMinuteStamp;
            this.lastMinuteStamp = this.gameTime.getMinutesStamp();
            this.listenCnt = (int)(this.listenCnt + diff);
            if (this.listenCnt >= 10) {
                this.listenCnt = 0;
            }

            if (!GameServer.server && this.isTurnedOn && playerInRange && (this.listenCnt == 0 || this.listenCnt == 5)) {
                this.TriggerPlayerListening(true);
            }

            if (this.isTurnedOn && this.isBatteryPowered && this.powerDelta > 0.0F) {
                float pdmod = this.powerDelta - this.powerDelta % 0.01F;
                this.setPower(this.powerDelta - this.useDelta * (float)diff);
                if (this.listenCnt == 0 || this.powerDelta == 0.0F || this.powerDelta < pdmod) {
                    if (isIso && GameServer.server) {
                        this.transmitDeviceDataStateServer((short)3, null);
                    } else if (!isIso && GameClient.client) {
                        this.transmitDeviceDataState((short)3);
                    }
                }
            }
        }

        if (this.isTurnedOn && (this.isBatteryPowered && this.powerDelta <= 0.0F || !this.canBePoweredHere())) {
            this.isTurnedOn = false;
            if (isIso && GameServer.server) {
                this.transmitDeviceDataStateServer((short)0, null);
            } else if (!isIso && GameClient.client) {
                this.transmitDeviceDataState((short)0);
            }
        }

        this.updateMediaPlaying();
        this.updateEmitter();
        this.updateSimple();
    }

    public void updateSimple() {
        if (this.voipCounter >= 0.0F) {
            this.voipCounter = this.voipCounter - 1.25F * GameTime.getInstance().getMultiplier();
        }

        if (this.signalCounter >= 0.0F) {
            this.signalCounter = this.signalCounter - 1.25F * GameTime.getInstance().getMultiplier();
        }

        if (this.soundCounter >= 0.0F) {
            this.soundCounter = this.soundCounter - 1.25F * GameTime.getInstance().getMultiplier();
        }

        if (this.signalCounter <= 0.0F && this.voipCounter <= 0.0F && this.lastRecordedDistance >= 0) {
            this.lastRecordedDistance = -1;
        }

        this.updateStaticSounds();
        if (GameClient.client) {
            this.updateEmitter();
        }

        if (this.doTriggerWorldSound && this.soundCounter <= 0.0F) {
            if (this.isTurnedOn
                && this.deviceVolume > 0.0F
                && (!this.isInventoryDevice() || this.headphoneType < 0)
                && (!GameClient.client && !GameServer.server || GameClient.client && this.isInventoryDevice() || GameServer.server && !this.isInventoryDevice())
                )
             {
                IsoObject source = null;
                if (this.parent != null && this.parent instanceof IsoObject isoObject) {
                    source = isoObject;
                } else if (this.parent != null && this.parent instanceof Radio) {
                    source = IsoPlayer.getInstance();
                } else if (this.parent instanceof VehiclePart vehiclePart) {
                    source = vehiclePart.getVehicle();
                }

                if (source != null) {
                    int volume = (int)(100.0F * this.deviceVolume);
                    int range = this.getDeviceSoundVolumeRange();
                    WorldSoundManager.instance
                        .addSoundRepeating(
                            source,
                            PZMath.fastfloor(source.getX()),
                            PZMath.fastfloor(source.getY()),
                            PZMath.fastfloor(source.getZ()),
                            range,
                            volume,
                            volume > 50
                        );
                }
            }

            this.doTriggerWorldSound = false;
            this.soundCounter = 300 + Rand.Next(0, 300);
        }
    }

    private void updateStaticSounds() {
        if (this.isTurnedOn) {
            float delta = GameTime.getInstance().getMultiplier();
            this.nextStaticSound -= delta;
            if (this.nextStaticSound <= 0.0F) {
                if (this.parent != null && this.signalCounter <= 0.0F && !this.isNoTransmit() && !this.isPlayingMedia()) {
                    this.parent.AddDeviceText(ZomboidRadio.getInstance().getRandomBzztFzzt(), 1.0F, 1.0F, 1.0F, null, null, -1);
                    this.doTriggerWorldSound = true;
                }

                this.setNextStaticSound();
            }
        }
    }

    private void setNextStaticSound() {
        this.nextStaticSound = Rand.Next(250.0F, 1500.0F);
    }

    public int getDeviceVolumeRange() {
        return 5 + (int)(this.baseVolumeRange * this.deviceVolume);
    }

    public int getDeviceSoundVolumeRange() {
        if (this.isInventoryDevice()) {
            Radio device = (Radio)this.getParent();
            return device.getPlayer() != null && device.getPlayer().getSquare() != null && device.getPlayer().getSquare().getRoom() != null
                ? 3 + (int)(this.baseVolumeRange * 0.4F * this.deviceVolume)
                : 5 + (int)(this.baseVolumeRange * this.deviceVolume);
        } else if (this.isIsoDevice()) {
            IsoWaveSignal device = (IsoWaveSignal)this.getParent();
            return device.getSquare() != null && device.getSquare().getRoom() != null
                ? 3 + (int)(this.baseVolumeRange * 0.5F * this.deviceVolume)
                : 5 + (int)(this.baseVolumeRange * 0.75F * this.deviceVolume);
        } else {
            return 5 + (int)(this.baseVolumeRange / 2.0F * this.deviceVolume);
        }
    }

    public void doReceiveSignal(int distance) {
        if (this.isTurnedOn) {
            this.lastRecordedDistance = distance;
            if (this.deviceVolume > 0.0F && (this.isIsoDevice() || this.headphoneType < 0)) {
                IsoObject source = null;
                if (this.parent != null && this.parent instanceof IsoObject isoObject) {
                    source = isoObject;
                } else if (this.parent != null && this.parent instanceof Radio) {
                    source = IsoPlayer.getInstance();
                } else if (this.parent instanceof VehiclePart vehiclePart) {
                    source = vehiclePart.getVehicle();
                }

                if (source != null && this.soundCounter <= 0.0F) {
                    int volume = (int)(100.0F * this.deviceVolume);
                    int range = this.getDeviceSoundVolumeRange();
                    WorldSoundManager.instance.addSound(source, source.getXi(), source.getYi(), source.getZi(), range, volume, volume > 50);
                    this.soundCounter = 120.0F;
                }
            }

            this.signalCounter = 300.0F;
            this.doTriggerWorldSound = true;
            this.setNextStaticSound();
        }
    }

    public void doReceiveMPSignal(float distance) {
        this.lastRecordedDistance = (int)distance;
        this.voipCounter = 10.0F;
    }

    public boolean isReceivingSignal() {
        return this.signalCounter > 0.0F || this.voipCounter > 0.0F;
    }

    public int getLastRecordedDistance() {
        return this.lastRecordedDistance;
    }

    public boolean isIsoDevice() {
        return this.getParent() != null && this.getParent() instanceof IsoWaveSignal;
    }

    public boolean isInventoryDevice() {
        return this.getParent() != null && this.getParent() instanceof Radio;
    }

    public boolean isVehicleDevice() {
        return this.getParent() instanceof VehiclePart;
    }

    public void transmitPresets() {
        this.transmitDeviceDataState((short)5);
    }

    private void transmitDeviceDataState(short type) {
        if (GameClient.client) {
            try {
                VoiceManager.getInstance().UpdateChannelsRoaming(GameClient.connection);
                this.sendDeviceDataStatePacket(GameClient.connection, type);
            } catch (Exception var3) {
                System.out.print(var3.getMessage());
            }
        }
    }

    private void transmitDeviceDataStateServer(short type, UdpConnection ignoreConnection) {
        if (GameServer.server) {
            try {
                for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
                    UdpConnection c = GameServer.udpEngine.connections.get(n);
                    if (ignoreConnection == null || ignoreConnection != c) {
                        this.sendDeviceDataStatePacket(c, type);
                    }
                }
            } catch (Exception var5) {
                System.out.print(var5.getMessage());
            }
        }
    }

    private void sendDeviceDataStatePacket(UdpConnection connection, short type) {
        ByteBufferWriter bb = connection.startPacket();
        PacketTypes.PacketType.RadioDeviceDataState.doPacket(bb);
        boolean validInfoHeader = false;
        if (this.isIsoDevice()) {
            IsoWaveSignal isoWave = (IsoWaveSignal)this.getParent();
            IsoGridSquare square = isoWave.getSquare();
            if (square != null) {
                bb.putByte((byte)1);
                bb.putInt(square.getX());
                bb.putInt(square.getY());
                bb.putInt(square.getZ());
                bb.putInt(square.getObjects().indexOf(isoWave));
                validInfoHeader = true;
            }
        } else if (this.isInventoryDevice()) {
            Radio radio = (Radio)this.getParent();
            IsoPlayer player = null;
            if (radio.getEquipParent() != null && radio.getEquipParent() instanceof IsoPlayer) {
                player = (IsoPlayer)radio.getEquipParent();
            }

            if (player != null) {
                bb.putByte((byte)0);
                if (GameServer.server) {
                    bb.putShort(player != null ? player.onlineId : -1);
                } else {
                    bb.putByte((byte)player.playerIndex);
                }

                if (player.getPrimaryHandItem() == radio) {
                    bb.putByte((byte)1);
                } else if (player.getSecondaryHandItem() == radio) {
                    bb.putByte((byte)2);
                } else {
                    bb.putByte((byte)0);
                }

                validInfoHeader = true;
            }
        } else if (this.isVehicleDevice()) {
            VehiclePart part = (VehiclePart)this.getParent();
            bb.putByte((byte)2);
            bb.putShort(part.getVehicle().vehicleId);
            bb.putShort((short)part.getIndex());
            validInfoHeader = true;
        }

        if (validInfoHeader) {
            bb.putShort(type);
            switch (type) {
                case 0:
                    bb.putByte((byte)(this.isTurnedOn ? 1 : 0));
                    break;
                case 1:
                    bb.putInt(this.channel);
                    break;
                case 2:
                    bb.putByte((byte)(this.hasBattery ? 1 : 0));
                    bb.putFloat(this.powerDelta);
                    break;
                case 3:
                    bb.putFloat(this.powerDelta);
                    break;
                case 4:
                    bb.putFloat(this.deviceVolume);
                    break;
                case 5:
                    bb.putInt(this.presets.getPresets().size());

                    for (PresetEntry preset : this.presets.getPresets()) {
                        GameWindow.WriteString(bb.bb, preset.getName());
                        bb.putInt(preset.getFrequency());
                    }
                    break;
                case 6:
                    bb.putInt(this.headphoneType);
                    break;
                case 7:
                    bb.putShort(this.mediaIndex);
                    bb.putByte((byte)(this.mediaItem != null ? 1 : 0));
                    if (this.mediaItem != null) {
                        GameWindow.WriteString(bb.bb, this.mediaItem);
                    }
                    break;
                case 8:
                    if (GameServer.server) {
                        bb.putShort(this.mediaIndex);
                        bb.putByte((byte)(this.mediaItem != null ? 1 : 0));
                        if (this.mediaItem != null) {
                            GameWindow.WriteString(bb.bb, this.mediaItem);
                        }
                    }
                case 9:
                default:
                    break;
                case 10:
                    if (GameServer.server) {
                        bb.putShort(this.mediaIndex);
                        bb.putInt(this.mediaLineIndex);
                    }
            }

            PacketTypes.PacketType.RadioDeviceDataState.send(connection);
        } else {
            connection.cancelPacket();
        }
    }

    public void receiveDeviceDataStatePacket(ByteBuffer bb, UdpConnection ignoreConnection) throws IOException {
        if (GameClient.client || GameServer.server) {
            boolean isServer = GameServer.server;
            boolean isIso = this.isIsoDevice() || this.isVehicleDevice();
            short type = bb.getShort();
            switch (type) {
                case 0:
                    if (isServer && isIso) {
                        this.setIsTurnedOn(bb.get() == 1);
                    } else {
                        this.isTurnedOn = bb.get() == 1;
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 1:
                    int chan = bb.getInt();
                    if (isServer && isIso) {
                        this.setChannel(chan);
                    } else {
                        this.channel = chan;
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 2:
                    boolean hasbat = bb.get() == 1;
                    float pwr = bb.getFloat();
                    if (isServer && isIso) {
                        this.hasBattery = hasbat;
                        this.setPower(pwr);
                    } else {
                        this.hasBattery = hasbat;
                        this.powerDelta = pwr;
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 3:
                    float delta = bb.getFloat();
                    if (isServer && isIso) {
                        this.setPower(delta);
                    } else {
                        this.powerDelta = delta;
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 4:
                    float volume = bb.getFloat();
                    if (isServer && isIso) {
                        this.setDeviceVolume(volume);
                    } else {
                        this.deviceVolume = volume;
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 5:
                    int size = bb.getInt();

                    for (int i = 0; i < size; i++) {
                        String name = GameWindow.ReadString(bb);
                        int freq = bb.getInt();
                        if (i < this.presets.getPresets().size()) {
                            PresetEntry presetEntry = this.presets.getPresets().get(i);
                            if (!presetEntry.getName().equals(name) || presetEntry.getFrequency() != freq) {
                                presetEntry.setName(name);
                                presetEntry.setFrequency(freq);
                            }
                        } else {
                            this.presets.addPreset(name, freq);
                        }
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer((short)5, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 6:
                    this.headphoneType = bb.getInt();
                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 7:
                    this.mediaIndex = bb.getShort();
                    if (bb.get() == 1) {
                        this.mediaItem = GameWindow.ReadString(bb);
                    }

                    if (isServer) {
                        this.transmitDeviceDataStateServer(type, !isIso ? ignoreConnection : null);
                    }
                    break;
                case 8:
                    if (GameServer.server) {
                        this.StartPlayMedia();
                    } else {
                        this.mediaLineIndex = 0;
                        this.mediaIndex = bb.getShort();
                        if (bb.get() == 1) {
                            this.mediaItem = GameWindow.ReadString(bb);
                        }

                        this.isPlayingMedia = true;
                        if (this.isInventoryDevice()) {
                            this.playingMedia = ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.mediaIndex);
                        }

                        this.televisionMediaSwitch();
                    }
                    break;
                case 9:
                    if (GameServer.server) {
                        this.StopPlayMedia();
                    } else {
                        this.isPlayingMedia = false;
                        this.televisionMediaSwitch();
                    }
                    break;
                case 10:
                    if (GameClient.client) {
                        this.mediaIndex = bb.getShort();
                        int lineIndex = bb.getInt();
                        MediaData data = this.getMediaData();
                        if (data != null && lineIndex >= 0 && lineIndex < data.getLineCount()) {
                            MediaData.MediaLineData line = data.getLine(lineIndex);
                            String text = line.getTranslatedText();
                            Color color = line.getColor();
                            String guid = line.getTextGuid();
                            String codes = line.getCodes();
                            this.parent.AddDeviceText(text, color.r, color.g, color.b, guid, codes, 0);
                        }
                    }
            }
        }
    }

    public void save(ByteBuffer output, boolean net) throws IOException {
        GameWindow.WriteString(output, this.deviceName);
        output.put((byte)(this.twoWay ? 1 : 0));
        output.putInt(this.transmitRange);
        output.putInt(this.micRange);
        output.put((byte)(this.micIsMuted ? 1 : 0));
        output.putFloat(this.baseVolumeRange);
        output.putFloat(this.deviceVolume);
        output.put((byte)(this.isPortable ? 1 : 0));
        output.put((byte)(this.isTelevision ? 1 : 0));
        output.put((byte)(this.isHighTier ? 1 : 0));
        output.put((byte)(this.isTurnedOn ? 1 : 0));
        output.putInt(this.channel);
        output.putInt(this.minChannelRange);
        output.putInt(this.maxChannelRange);
        output.put((byte)(this.isBatteryPowered ? 1 : 0));
        output.put((byte)(this.hasBattery ? 1 : 0));
        output.putFloat(this.powerDelta);
        output.putFloat(this.useDelta);
        output.putInt(this.headphoneType);
        if (this.presets != null) {
            output.put((byte)1);
            this.presets.save(output, net);
        } else {
            output.put((byte)0);
        }

        output.putShort(this.mediaIndex);
        output.put(this.mediaType);
        output.put((byte)(this.mediaItem != null ? 1 : 0));
        if (this.mediaItem != null) {
            GameWindow.WriteString(output, this.mediaItem);
        }

        output.put((byte)(this.noTransmit ? 1 : 0));
    }

    public void load(ByteBuffer input, int WorldVersion, boolean net) throws IOException {
        if (this.presets == null) {
            this.presets = new DevicePresets();
        }

        this.deviceName = GameWindow.ReadString(input);
        this.twoWay = input.get() == 1;
        this.transmitRange = input.getInt();
        this.micRange = input.getInt();
        this.micIsMuted = input.get() == 1;
        this.baseVolumeRange = input.getFloat();
        this.deviceVolume = input.getFloat();
        this.isPortable = input.get() == 1;
        this.isTelevision = input.get() == 1;
        this.isHighTier = input.get() == 1;
        this.isTurnedOn = input.get() == 1;
        this.channel = input.getInt();
        this.minChannelRange = input.getInt();
        this.maxChannelRange = input.getInt();
        this.isBatteryPowered = input.get() == 1;
        this.hasBattery = input.get() == 1;
        this.powerDelta = input.getFloat();
        this.useDelta = input.getFloat();
        this.headphoneType = input.getInt();
        if (input.get() == 1) {
            this.presets.load(input, WorldVersion, net);
        }

        this.mediaIndex = input.getShort();
        this.mediaType = input.get();
        if (input.get() == 1) {
            this.mediaItem = GameWindow.ReadString(input);
        }

        this.noTransmit = input.get() == 1;
    }

    public boolean hasMedia() {
        return this.mediaIndex >= 0;
    }

    public short getMediaIndex() {
        return this.mediaIndex;
    }

    public void setMediaIndex(short mediaIndex) {
        this.mediaIndex = mediaIndex;
    }

    public byte getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(byte mediaType) {
        this.mediaType = mediaType;
    }

    public void addMediaItem(InventoryItem media) {
        if (this.mediaIndex < 0 && media.isRecordedMedia() && media.getMediaType() == this.mediaType) {
            ItemContainer container = media.getContainer();
            if (container != null) {
                this.mediaIndex = media.getRecordedMediaIndex();
                this.mediaItem = media.getFullType();
                ItemUser.RemoveItem(media);
                this.transmitDeviceDataState((short)7);
            }
        }
    }

    public InventoryItem removeMediaItem(ItemContainer inventory) {
        if (this.hasMedia()) {
            InventoryItem media = InventoryItemFactory.CreateItem(this.mediaItem);
            media.setRecordedMediaIndex(this.mediaIndex);
            inventory.AddItem(media);
            this.mediaIndex = -1;
            this.mediaItem = null;
            if (this.isPlayingMedia()) {
                this.StopPlayMedia();
            }

            this.transmitDeviceDataState((short)7);
            return media;
        } else {
            return null;
        }
    }

    public boolean isPlayingMedia() {
        return this.isPlayingMedia;
    }

    public void StartPlayMedia() {
        if (GameClient.client) {
            this.transmitDeviceDataState((short)8);
        } else if (!this.isPlayingMedia() && this.getIsTurnedOn() && this.hasMedia()) {
            this.playingMedia = ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.mediaIndex);
            if (this.playingMedia != null) {
                this.isPlayingMedia = true;
                this.mediaLineIndex = 0;
                this.prePlayingMedia();
                if (GameServer.server) {
                    this.transmitDeviceDataStateServer((short)8, null);
                }
            }
        }
    }

    private void prePlayingMedia() {
        this.lineCounter = 60.0F * this.maxmod * 0.5F;
        this.televisionMediaSwitch();
    }

    private void postPlayingMedia() {
        this.isStoppingMedia = true;
        this.stopMediaCounter = 60.0F * this.maxmod * 0.5F;
        this.televisionMediaSwitch();
    }

    private void televisionMediaSwitch() {
        if (this.mediaType == 1) {
            ZomboidRadio.getInstance().getRandomBzztFzzt();
            this.parent.AddDeviceText(ZomboidRadio.getInstance().getRandomBzztFzzt(), 0.5F, 0.5F, 0.5F, null, null, 0);
            this.playSoundLocal("TelevisionZap", true);
        }
    }

    public void StopPlayMedia() {
        if (GameClient.client) {
            this.transmitDeviceDataState((short)9);
        } else {
            if (GameServer.server) {
                this.isPlayingMedia = false;
            }

            this.playingMedia = null;
            this.postPlayingMedia();
            if (GameServer.server) {
                this.transmitDeviceDataStateServer((short)9, null);
            }
        }
    }

    public void updateMediaPlaying() {
        if (!GameClient.client || this.isTurnedOn && this.deviceVolume > 0.0F && this.isInventoryDevice() && this.headphoneType >= 0) {
            if (this.isStoppingMedia) {
                this.stopMediaCounter = this.stopMediaCounter - 1.25F * GameTime.getInstance().getMultiplier();
                if (this.stopMediaCounter <= 0.0F) {
                    this.isPlayingMedia = false;
                    this.isStoppingMedia = false;
                }
            } else {
                if (this.hasMedia() && this.isPlayingMedia()) {
                    if (!this.getIsTurnedOn()) {
                        this.StopPlayMedia();
                        return;
                    }

                    if (this.playingMedia != null) {
                        this.lineCounter = this.lineCounter - 1.25F * GameTime.getInstance().getMultiplier();
                        if (this.lineCounter <= 0.0F) {
                            MediaData.MediaLineData line = this.playingMedia.getLine(this.mediaLineIndex);
                            if (line != null) {
                                String text = line.getTranslatedText();
                                Color color = line.getColor();
                                this.lineCounter = text.length() / 10.0F * 60.0F;
                                if (this.lineCounter < 60.0F * this.minmod) {
                                    this.lineCounter = 60.0F * this.minmod;
                                } else if (this.lineCounter > 60.0F * this.maxmod) {
                                    this.lineCounter = 60.0F * this.maxmod;
                                }

                                String guid = line.getTextGuid();
                                String codes = line.getCodes();
                                if (GameServer.server) {
                                    this.currentMediaLine = text;
                                    this.currentMediaColor = color;
                                    this.transmitDeviceDataStateServer((short)10, null);
                                    if (this.getIsTurnedOn() && this.getDeviceVolume() > 0.0F && codes != null) {
                                        LuaEventManager.triggerEvent(
                                            "OnDeviceText", guid, codes, this.parent.getX(), this.parent.getY(), this.parent.getZ(), text, this.parent
                                        );
                                    }
                                } else {
                                    this.parent.AddDeviceText(text, color.r, color.g, color.b, guid, codes, 0);
                                }

                                this.mediaLineIndex++;
                            } else {
                                this.StopPlayMedia();
                            }
                        }
                    }
                }
            }
        }
    }

    public MediaData getMediaData() {
        return this.mediaIndex >= 0 ? ZomboidRadio.getInstance().getRecordedMedia().getMediaDataFromIndex(this.mediaIndex) : null;
    }

    public boolean isNoTransmit() {
        return this.noTransmit;
    }

    public void setNoTransmit(boolean noTransmit) {
        this.noTransmit = noTransmit;
    }

    public boolean isEmergencyBroadcast() {
        if (this.isTelevision) {
            return false;
        } else {
            int channel = this.getChannel();
            Map<Integer, String> category = ZomboidRadio.getInstance().GetChannelList("Emergency");
            if (category == null) {
                return false;
            } else {
                for (Entry<Integer, String> entry : category.entrySet()) {
                    if (entry.getKey() == channel) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    @Override
    public FMODParameterList getFMODParameters() {
        return this.parameterList;
    }

    @Override
    public void startEvent(long eventInstance, GameSoundClip clip, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        ArrayList<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.eventDescription.parameters;

        for (int i = 0; i < eventParameters.size(); i++) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (!parameterSet.get(eventParameter.globalIndex)) {
                FMODParameter fmodParameter = myParameters.get(eventParameter);
                if (fmodParameter != null) {
                    fmodParameter.startEventInstance(eventInstance);
                }
            }
        }
    }

    @Override
    public void updateEvent(long eventInstance, GameSoundClip clip) {
    }

    @Override
    public void stopEvent(long eventInstance, GameSoundClip clip, BitSet parameterSet) {
        FMODParameterList myParameters = this.getFMODParameters();
        ArrayList<FMOD_STUDIO_PARAMETER_DESCRIPTION> eventParameters = clip.eventDescription.parameters;

        for (int i = 0; i < eventParameters.size(); i++) {
            FMOD_STUDIO_PARAMETER_DESCRIPTION eventParameter = eventParameters.get(i);
            if (!parameterSet.get(eventParameter.globalIndex)) {
                FMODParameter fmodParameter = myParameters.get(eventParameter);
                if (fmodParameter != null) {
                    fmodParameter.stopEventInstance(eventInstance);
                }
            }
        }
    }

    public void addEmergencyChannel() {
        if (!this.isTelevision) {
            Map<Integer, String> category = ZomboidRadio.getInstance().GetChannelList("Emergency");
            if (category != null) {
                for (Entry<Integer, String> entry : category.entrySet()) {
                    this.presets.addPreset(entry.getValue(), entry.getKey());
                }
            }
        }
    }
}
