// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import gnu.trove.map.hash.THashMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import zombie.AmbientStreamManager;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.audio.parameters.ParameterCurrentZone;
import zombie.audio.parameters.ParameterInside;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugOptions;
import zombie.debug.LineDrawer;
import zombie.entity.util.TimSort;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoCamera;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.popman.ObjectPool;
import zombie.util.Type;
import zombie.util.list.PZArrayList;
import zombie.util.list.PZArrayUtil;

public final class ObjectAmbientEmitters {
    private final HashMap<String, ObjectAmbientEmitters.PowerPolicy> powerPolicyMap = new HashMap<>();
    private static ObjectAmbientEmitters instance;
    static final Vector2 tempVector2 = new Vector2();
    private final THashMap<IsoObject, ObjectAmbientEmitters.ObjectWithDistance> added = new THashMap<>();
    private final ObjectPool<ObjectAmbientEmitters.ObjectWithDistance> objectPool = new ObjectPool<>(ObjectAmbientEmitters.ObjectWithDistance::new);
    private final PZArrayList<ObjectAmbientEmitters.ObjectWithDistance> objects = new PZArrayList<>(ObjectAmbientEmitters.ObjectWithDistance.class, 16);
    private final TimSort timSort = new TimSort();
    private final ObjectAmbientEmitters.Slot[] slots;
    private final Comparator<ObjectAmbientEmitters.ObjectWithDistance> comp = new Comparator<ObjectAmbientEmitters.ObjectWithDistance>() {
        {
            Objects.requireNonNull(ObjectAmbientEmitters.this);
        }

        public int compare(ObjectAmbientEmitters.ObjectWithDistance a, ObjectAmbientEmitters.ObjectWithDistance b) {
            return Float.compare(a.distSq, b.distSq);
        }
    };

    public static ObjectAmbientEmitters getInstance() {
        if (instance == null) {
            instance = new ObjectAmbientEmitters();
        }

        return instance;
    }

    private ObjectAmbientEmitters() {
        int numSlots = 16;
        this.slots = PZArrayUtil.newInstance(ObjectAmbientEmitters.Slot.class, 16, ObjectAmbientEmitters.Slot::new);
        this.added.setAutoCompactionFactor(0.0F);
        this.powerPolicyMap.put("FactoryMachineAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("HotdogMachineAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("PayPhoneAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("StreetLightAmbiance", ObjectAmbientEmitters.PowerPolicy.StreetLight);
        this.powerPolicyMap.put("NeonLightAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("NeonSignAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("JukeboxAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("ControlStationAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("ClockAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("GasPumpAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("LightBulbAmbiance", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
        this.powerPolicyMap.put("ArcadeMachineAmbiance", ObjectAmbientEmitters.PowerPolicy.InteriorHydro);
        this.powerPolicyMap.put("PylonTowerAmbience", ObjectAmbientEmitters.PowerPolicy.ExteriorOK);
    }

    private static boolean addObjectLambda(IsoObject object, ObjectAmbientEmitters.PerObjectLogic logic) {
        getInstance().addObject(object, logic);
        return true;
    }

    private void addObject(IsoObject object, ObjectAmbientEmitters.PerObjectLogic logic) {
        if (!GameServer.server) {
            if (!this.added.containsKey(object)) {
                boolean bListener = false;

                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player != null && object.getObjectIndex() != -1) {
                        int maxDist = 15;
                        if (logic instanceof ObjectAmbientEmitters.DoorLogic || logic instanceof ObjectAmbientEmitters.WindowLogic) {
                            maxDist = 10;
                        }

                        if (logic instanceof ObjectAmbientEmitters.AmbientSoundLogic ambientSoundLogic
                            && ambientSoundLogic.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired) {
                            maxDist = 30;
                        }

                        if ((
                                object.square.z == PZMath.fastfloor(player.getZ())
                                    || !(logic instanceof ObjectAmbientEmitters.DoorLogic) && !(logic instanceof ObjectAmbientEmitters.WindowLogic)
                            )
                            && !(player.DistToSquared(object.square.x + 0.5F, object.square.y + 0.5F) > maxDist * maxDist)) {
                            bListener = true;
                            break;
                        }
                    }
                }

                if (bListener) {
                    ObjectAmbientEmitters.ObjectWithDistance owd = this.objectPool.alloc();
                    owd.object = object;
                    owd.logic = logic;
                    this.objects.add(owd);
                    this.added.put(object, owd);
                }
            }
        }
    }

    void removeObject(IsoObject object) {
        if (!GameServer.server) {
            ObjectAmbientEmitters.ObjectWithDistance owd = this.added.remove(object);
            if (owd != null) {
                this.objects.remove(owd);
                this.objectPool.release(owd);
            }
        }
    }

    public void update() {
        if (!GameServer.server) {
            this.addObjectsFromChunks();

            for (int i = 0; i < this.slots.length; i++) {
                this.slots[i].playing = false;
            }

            if (this.objects.isEmpty()) {
                this.stopNotPlaying();
            } else {
                for (int i = 0; i < this.objects.size(); i++) {
                    ObjectAmbientEmitters.ObjectWithDistance owd = this.objects.get(i);
                    IsoObject object = owd.object;
                    ObjectAmbientEmitters.PerObjectLogic logic = this.objects.get(i).logic;
                    if (!this.shouldPlay(object, logic)) {
                        this.added.remove(object);
                        this.objects.remove(i--);
                        this.objectPool.release(owd);
                    } else {
                        object.getFacingPosition(tempVector2);
                        owd.distSq = this.getClosestListener(tempVector2.x, tempVector2.y, object.square.z);
                    }
                }

                this.timSort.doSort(this.objects.getElements(), this.comp, 0, this.objects.size());
                int count = Math.min(this.objects.size(), this.slots.length);

                for (int ix = 0; ix < count; ix++) {
                    IsoObject object = this.objects.get(ix).object;
                    ObjectAmbientEmitters.PerObjectLogic logic = this.objects.get(ix).logic;
                    if (this.shouldPlay(object, logic)) {
                        int j = this.getExistingSlot(object);
                        if (j != -1) {
                            this.slots[j].playSound(object, logic);
                        }
                    }
                }

                for (int ixx = 0; ixx < count; ixx++) {
                    IsoObject object = this.objects.get(ixx).object;
                    ObjectAmbientEmitters.PerObjectLogic logic = this.objects.get(ixx).logic;
                    if (this.shouldPlay(object, logic)) {
                        int j = this.getExistingSlot(object);
                        if (j == -1) {
                            j = this.getFreeSlot();
                            if (this.slots[j].object != null) {
                                this.slots[j].stopPlaying();
                                this.slots[j].object = null;
                            }

                            this.slots[j].playSound(object, logic);
                        }
                    }
                }

                this.stopNotPlaying();
                this.added.clear();
                this.objectPool.release(this.objects);
                this.objects.clear();
            }
        }
    }

    void addObjectsFromChunks() {
        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[i];
            if (!chunkMap.ignore) {
                int midX = IsoChunkMap.chunkGridWidth / 2;
                int midY = IsoChunkMap.chunkGridWidth / 2;

                for (int dy = -3; dy <= 3; dy++) {
                    for (int dx = -3; dx <= 3; dx++) {
                        IsoChunk chunk = chunkMap.getChunk(midX + dx, midY + dy);
                        if (chunk != null && !chunk.objectEmitterData.objects.isEmpty()) {
                            chunk.objectEmitterData.objects.forEachEntry(ObjectAmbientEmitters::addObjectLambda);
                        }
                    }
                }
            }
        }
    }

    float getClosestListener(float soundX, float soundY, float soundZ) {
        float minDist = Float.MAX_VALUE;

        for (int i = 0; i < IsoPlayer.numPlayers; i++) {
            IsoPlayer chr = IsoPlayer.players[i];
            if (chr != null && chr.getCurrentSquare() != null) {
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

    boolean shouldPlay(IsoObject object, ObjectAmbientEmitters.PerObjectLogic logic) {
        if (object == null) {
            return false;
        } else {
            return object.getObjectIndex() == -1 ? false : logic.shouldPlaySound();
        }
    }

    int getExistingSlot(IsoObject object) {
        for (int i = 0; i < this.slots.length; i++) {
            if (this.slots[i].object == object) {
                return i;
            }
        }

        return -1;
    }

    int getFreeSlot() {
        for (int i = 0; i < this.slots.length; i++) {
            if (!this.slots[i].playing) {
                return i;
            }
        }

        return -1;
    }

    void stopNotPlaying() {
        for (int i = 0; i < this.slots.length; i++) {
            ObjectAmbientEmitters.Slot slot = this.slots[i];
            if (!slot.playing) {
                slot.stopPlaying();
                slot.object = null;
            }
        }
    }

    public void render() {
        if (DebugOptions.instance.objectAmbientEmitterRender.getValue()) {
            IsoChunkMap chunkMap = IsoWorld.instance.currentCell.chunkMap[IsoCamera.frameState.playerIndex];
            if (!chunkMap.ignore) {
                int midX = IsoChunkMap.chunkGridWidth / 2;
                int midY = IsoChunkMap.chunkGridWidth / 2;

                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        IsoChunk chunk = chunkMap.getChunk(midX + dx, midY + dy);
                        if (chunk != null) {
                            for (IsoObject object : chunk.objectEmitterData.objects.keySet()) {
                                if (object.square.z == PZMath.fastfloor(IsoCamera.frameState.camCharacterZ)) {
                                    object.getFacingPosition(tempVector2);
                                    float x = tempVector2.x;
                                    float y = tempVector2.y;
                                    float z = object.square.z;
                                    LineDrawer.addLine(x - 0.45F, y - 0.45F, z, x + 0.45F, y + 0.45F, z, 0.5F, 0.5F, 0.5F, null, false);
                                }
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < this.slots.length; i++) {
                ObjectAmbientEmitters.Slot slot = this.slots[i];
                if (slot.playing) {
                    IsoObject objectx = slot.object;
                    objectx.getFacingPosition(tempVector2);
                    float x = tempVector2.x;
                    float y = tempVector2.y;
                    float z = objectx.square.z;
                    LineDrawer.addLine(x - 0.45F, y - 0.45F, z, x + 0.45F, y + 0.45F, z, 0.0F, 0.0F, 1.0F, null, false);
                }
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            for (int i = 0; i < instance.slots.length; i++) {
                instance.slots[i].stopPlaying();
                instance.slots[i].object = null;
                instance.slots[i].playing = false;
            }
        }
    }

    public static final class AmbientSoundLogic extends ObjectAmbientEmitters.PerObjectLogic {
        ObjectAmbientEmitters.PowerPolicy powerPolicy = ObjectAmbientEmitters.PowerPolicy.NotRequired;
        boolean hasGeneratorParameter;

        @Override
        public ObjectAmbientEmitters.PerObjectLogic init(IsoObject object) {
            super.init(object);
            String soundName = this.getSoundName();
            this.powerPolicy = ObjectAmbientEmitters.getInstance().powerPolicyMap.getOrDefault(soundName, ObjectAmbientEmitters.PowerPolicy.NotRequired);
            if (this.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired) {
                GameSound gameSound = GameSounds.getSound(soundName);
                this.hasGeneratorParameter = gameSound != null && gameSound.numClipsUsingParameter("Generator") > 0;
            }

            return this;
        }

        @Override
        public boolean shouldPlaySound() {
            if (this.object instanceof IsoLightSwitch lightSwitch) {
                if (!lightSwitch.isActivated()) {
                    return false;
                }

                if (this.object.hasSpriteGrid()
                    && (
                        this.object.getSpriteGrid().getSpriteGridPosX(this.object.sprite) != 0
                            || this.object.getSpriteGrid().getSpriteGridPosY(this.object.sprite) != 0
                    )) {
                    return false;
                }
            }

            if (this.powerPolicy == ObjectAmbientEmitters.PowerPolicy.InteriorHydro) {
                boolean isPowered = this.object.square.haveElectricity()
                    || this.object.hasGridPower() && (this.object.square.isInARoom() || this.object.square.associatedBuilding != null);
                if (!isPowered) {
                    return false;
                }
            }

            if (this.powerPolicy == ObjectAmbientEmitters.PowerPolicy.ExteriorOK) {
                boolean isPowered = this.object.square.haveElectricity() || this.object.hasGridPower();
                if (!isPowered) {
                    return false;
                }
            }

            if (this.powerPolicy == ObjectAmbientEmitters.PowerPolicy.StreetLight) {
                boolean isPowered = this.object.hasGridPower();
                if (!isPowered || GameTime.getInstance().getNight() < 0.5F) {
                    return false;
                }
            }

            if (this.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired && !this.object.hasGridPower() && !this.hasGeneratorParameter) {
                return false;
            } else {
                PropertyContainer props = this.object.getProperties();
                return props != null && props.has("AmbientSound");
            }
        }

        @Override
        public String getSoundName() {
            return this.object.getProperties().get("AmbientSound");
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
            if (this.powerPolicy != ObjectAmbientEmitters.PowerPolicy.NotRequired) {
                this.setParameterValue1(emitter, instance, "Generator", this.object.hasGridPower() ? 0.0F : 1.0F);
            }
        }
    }

    public static final class ChunkData {
        final THashMap<IsoObject, ObjectAmbientEmitters.PerObjectLogic> objects = new THashMap<>();

        public ChunkData() {
            this.objects.setAutoCompactionFactor(0.0F);
        }

        public boolean hasObject(IsoObject object) {
            return this.objects.containsKey(object);
        }

        public void addObject(IsoObject object, ObjectAmbientEmitters.PerObjectLogic logic) {
            if (!this.objects.containsKey(object)) {
                this.objects.put(object, logic);
            }
        }

        public void removeObject(IsoObject object) {
            this.objects.remove(object);
        }

        public void reset() {
            this.objects.clear();
        }
    }

    public static final class DoorLogic extends ObjectAmbientEmitters.PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return AmbientStreamManager.instance.isParameterInsideTrue() && this.isReachableSquare();
        }

        @Override
        public String getSoundName() {
            return "DoorAmbiance";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
            IsoDoor door1 = Type.tryCastTo(this.object, IsoDoor.class);
            IsoThumpable door2 = Type.tryCastTo(this.object, IsoThumpable.class);
            float value = 0.0F;
            if (door1 != null && door1.IsOpen()) {
                value = 1.0F;
            }

            if (door2 != null && door2.IsOpen()) {
                value = 1.0F;
            }

            this.setParameterValue1(emitter, instance, "DoorWindowOpen", value);
        }

        boolean isReachableSquare() {
            if (this.object.getSquare() == null) {
                return false;
            } else {
                IsoDoor door1 = Type.tryCastTo(this.object, IsoDoor.class);
                IsoThumpable door2 = Type.tryCastTo(this.object, IsoThumpable.class);
                boolean north = door1 == null ? door2.getNorth() : door1.getNorth();
                return ParameterInside.isAdjacentToReachableSquare(this.object.getSquare(), north);
            }
        }
    }

    public static final class FridgeHumLogic extends ObjectAmbientEmitters.PerObjectLogic {
        static String[] soundNames = new String[]{"FridgeHumA", "FridgeHumB", "FridgeHumC", "FridgeHumD", "FridgeHumE", "FridgeHumF"};
        int choice = -1;

        @Override
        public ObjectAmbientEmitters.PerObjectLogic init(IsoObject object) {
            super.init(object);
            this.choice = Rand.Next(6);
            return this;
        }

        @Override
        public boolean shouldPlaySound() {
            ItemContainer container = this.object.getContainerByEitherType("fridge", "freezer");
            return container != null && container.isPowered();
        }

        @Override
        public String getSoundName() {
            return soundNames[this.choice];
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
            this.setParameterValue1(emitter, instance, "Generator", this.object.hasGridPower() ? 0.0F : 1.0F);
        }
    }

    static final class ObjectWithDistance {
        IsoObject object;
        ObjectAmbientEmitters.PerObjectLogic logic;
        float distSq;
    }

    public abstract static class PerObjectLogic {
        public IsoObject object;
        public float parameterValue1 = Float.NaN;

        public ObjectAmbientEmitters.PerObjectLogic init(IsoObject object) {
            this.object = object;
            this.parameterValue1 = Float.NaN;
            return this;
        }

        void setParameterValue1(BaseSoundEmitter emitter, long instance, String paramName, float value) {
            if (value != this.parameterValue1) {
                this.parameterValue1 = value;
                FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription = FMODManager.instance.getParameterDescription(paramName);
                emitter.setParameterValue(instance, parameterDescription, value);
            }
        }

        void setParameterValue1(BaseSoundEmitter emitter, long instance, FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription, float value) {
            if (value != this.parameterValue1) {
                this.parameterValue1 = value;
                emitter.setParameterValue(instance, parameterDescription, value);
            }
        }

        public abstract boolean shouldPlaySound();

        public abstract String getSoundName();

        public abstract void startPlaying(BaseSoundEmitter emitter, long instance);

        public abstract void stopPlaying(BaseSoundEmitter emitter, long instance);

        public abstract void checkParameters(BaseSoundEmitter emitter, long instance);
    }

    static enum PowerPolicy {
        NotRequired,
        InteriorHydro,
        ExteriorOK,
        StreetLight;
    }

    static final class Slot {
        IsoObject object;
        ObjectAmbientEmitters.PerObjectLogic logic;
        BaseSoundEmitter emitter;
        long instance;
        boolean playing;

        void playSound(IsoObject object, ObjectAmbientEmitters.PerObjectLogic logic) {
            if (this.emitter == null) {
                this.emitter = (BaseSoundEmitter)(Core.soundDisabled ? new DummySoundEmitter() : new FMODSoundEmitter());
            }

            object.getFacingPosition(ObjectAmbientEmitters.tempVector2);
            this.emitter.setPos(ObjectAmbientEmitters.tempVector2.getX(), ObjectAmbientEmitters.tempVector2.getY(), object.square.z);
            this.object = object;
            this.logic = logic;
            String soundName = logic.getSoundName();
            if (!this.emitter.isPlaying(soundName)) {
                this.emitter.stopAll();
                if (this.emitter instanceof FMODSoundEmitter fmodSoundEmitter) {
                    fmodSoundEmitter.clearParameters();
                }

                this.instance = this.emitter.playSoundImpl(soundName, (IsoObject)null);
                logic.startPlaying(this.emitter, this.instance);
            }

            logic.checkParameters(this.emitter, this.instance);
            this.playing = true;
            this.emitter.tick();
        }

        void stopPlaying() {
            if (this.emitter != null && this.instance != 0L) {
                this.logic.stopPlaying(this.emitter, this.instance);
                if (this.emitter.hasSustainPoints(this.instance)) {
                    this.emitter.triggerCue(this.instance);
                    this.instance = 0L;
                } else {
                    this.emitter.stopAll();
                    this.instance = 0L;
                }
            }
        }
    }

    public static final class TentAmbianceLogic extends ObjectAmbientEmitters.PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return this.object.sprite != null
                && this.object.sprite.getName() != null
                && this.object.sprite.getName().startsWith("camping_01")
                && (this.object.sprite.tileSheetIndex == 0 || this.object.sprite.tileSheetIndex == 3);
        }

        @Override
        public String getSoundName() {
            return "TentAmbiance";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
        }
    }

    public static final class TreeAmbianceLogic extends ObjectAmbientEmitters.PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return true;
        }

        @Override
        public String getSoundName() {
            return "TreeAmbiance";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
            if (emitter instanceof FMODSoundEmitter fmodSoundEmitter) {
                fmodSoundEmitter.addParameter(new ParameterCurrentZone(this.object));
            }

            emitter.playAmbientLoopedImpl("BirdInTree");
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            emitter.stopOrTriggerSoundByName("BirdInTree");
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
        }
    }

    public static final class WaterDripLogic extends ObjectAmbientEmitters.PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return this.object.sprite != null && this.object.sprite.getProperties().has(IsoFlagType.waterPiped) && this.object.getFluidAmount() > 0.0F;
        }

        @Override
        public String getSoundName() {
            return "WaterDrip";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
            if (this.object.sprite != null && this.object.sprite.getProperties().has("SinkType")) {
                String sinkTypeStr = this.object.sprite.getProperties().get("SinkType");

                int sinkType = switch (sinkTypeStr) {
                    case "Ceramic" -> 1;
                    case "Metal" -> 2;
                    default -> 0;
                };
                this.setParameterValue1(emitter, instance, "SinkType", sinkType);
            }
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
        }
    }

    public static final class WindowLogic extends ObjectAmbientEmitters.PerObjectLogic {
        @Override
        public boolean shouldPlaySound() {
            return AmbientStreamManager.instance.isParameterInsideTrue() && this.isReachableSquare();
        }

        @Override
        public String getSoundName() {
            return "WindowAmbiance";
        }

        @Override
        public void startPlaying(BaseSoundEmitter emitter, long instance) {
        }

        @Override
        public void stopPlaying(BaseSoundEmitter emitter, long instance) {
            this.parameterValue1 = Float.NaN;
        }

        @Override
        public void checkParameters(BaseSoundEmitter emitter, long instance) {
            IsoWindow window = Type.tryCastTo(this.object, IsoWindow.class);
            float value = !window.IsOpen() && !window.isDestroyed() ? 0.0F : 1.0F;
            if (value == 1.0F) {
                IsoBarricade barricade1 = window.getBarricadeOnSameSquare();
                IsoBarricade barricade2 = window.getBarricadeOnOppositeSquare();
                int numPlanks1 = barricade1 == null ? 0 : barricade1.getNumPlanks();
                int numPlanks2 = barricade2 == null ? 0 : barricade2.getNumPlanks();
                if ((barricade1 == null || !barricade1.isMetal()) && (barricade2 == null || !barricade2.isMetal())) {
                    if (numPlanks1 > 0 || numPlanks2 > 0) {
                        value = 1.0F - PZMath.max(numPlanks1, numPlanks2) / 4.0F;
                    }
                } else {
                    value = 0.0F;
                }
            }

            this.setParameterValue1(emitter, instance, "DoorWindowOpen", value);
        }

        boolean isReachableSquare() {
            return this.object.getSquare() != null && ParameterInside.isAdjacentToReachableSquare(this.object.getSquare(), ((IsoWindow)this.object).isNorth());
        }
    }
}
