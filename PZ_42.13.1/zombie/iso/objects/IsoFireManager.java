// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import fmod.fmod.FMODSoundEmitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Stack;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.DummySoundEmitter;
import zombie.audio.parameters.ParameterFireSize;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.debug.DebugLog;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class IsoFireManager {
    private static final Stack<IsoFire> updateStack = new Stack<>();
    private static final HashSet<IsoGameCharacter> charactersOnFire = new HashSet<>();
    public static double redOscilator;
    public static double greenOscilator;
    public static double blueOscilator;
    public static double redOscilatorRate = 0.1F;
    public static double greenOscilatorRate = 0.13F;
    public static double blueOscilatorRate = 0.0876F;
    public static double redOscilatorVal;
    public static double greenOscilatorVal;
    public static double blueOscilatorVal;
    public static double oscilatorSpeedScalar = 15.6F;
    public static double oscilatorEffectScalar = 0.0039F;
    public static int maxFireObjects = 75;
    public static int fireRecalcDelay = 25;
    public static int fireRecalc = fireRecalcDelay;
    public static boolean lightCalcFromBurningCharacters;
    public static float fireAlpha = 1.0F;
    public static float smokeAlpha = 0.3F;
    @UsedFromLua
    public static final float FIRE_ANIM_DELAY = 0.5F;
    public static float smokeAnimDelay = 0.5F;
    @UsedFromLua
    public static final ColorInfo FIRE_TINT_MOD = new ColorInfo(1.0F, 1.0F, 1.0F, 1.0F);
    public static ColorInfo smokeTintMod = new ColorInfo(0.5F, 0.5F, 0.5F, 1.0F);
    public static final ArrayList<IsoFire> FireStack = new ArrayList<>();
    public static final ArrayList<IsoGameCharacter> CharactersOnFire_Stack = new ArrayList<>();
    private static final IsoFireManager.FireSounds fireSounds = new IsoFireManager.FireSounds(20);

    public static void Add(IsoFire NewFire) {
        if (FireStack.contains(NewFire)) {
            System.out.println("IsoFireManager.Add already added fire, ignoring");
        } else {
            if (FireStack.size() < maxFireObjects) {
                FireStack.add(NewFire);
            } else {
                IsoFire OldestFireObject = null;
                int OldestAge = 0;

                for (int i = 0; i < FireStack.size(); i++) {
                    if (FireStack.get(i).age > OldestAge) {
                        OldestAge = FireStack.get(i).age;
                        OldestFireObject = FireStack.get(i);
                    }
                }

                if (OldestFireObject != null && OldestFireObject.square != null) {
                    OldestFireObject.square.getProperties().unset(IsoFlagType.burning);
                    OldestFireObject.square.getProperties().unset(IsoFlagType.smoke);
                    OldestFireObject.RemoveAttachedAnims();
                    OldestFireObject.removeFromWorld();
                    OldestFireObject.removeFromSquare();
                }

                FireStack.add(NewFire);
            }
        }
    }

    public static void AddBurningCharacter(IsoGameCharacter BurningCharacter) {
        for (int i = 0; i < CharactersOnFire_Stack.size(); i++) {
            if (CharactersOnFire_Stack.get(i) == BurningCharacter) {
                return;
            }
        }

        CharactersOnFire_Stack.add(BurningCharacter);
    }

    public static void Fire_LightCalc(IsoGridSquare FireSquare, IsoGridSquare TestSquare, int playerIndex) {
        if (TestSquare != null && FireSquare != null) {
            int Dist = 0;
            int FireAreaOfEffect = 8;
            Dist += Math.abs(TestSquare.getX() - FireSquare.getX());
            Dist += Math.abs(TestSquare.getY() - FireSquare.getY());
            Dist += Math.abs(TestSquare.getZ() - FireSquare.getZ());
            if (Dist <= 8) {
                float LightInfluence = 0.024875F * (8 - Dist);
                float LightInfluenceG = LightInfluence * 0.6F;
                float LightInfluenceB = LightInfluence * 0.4F;
                if (TestSquare.getLightInfluenceR() == null) {
                    TestSquare.setLightInfluenceR(new ArrayList<>());
                }

                TestSquare.getLightInfluenceR().add(LightInfluence);
                if (TestSquare.getLightInfluenceG() == null) {
                    TestSquare.setLightInfluenceG(new ArrayList<>());
                }

                TestSquare.getLightInfluenceG().add(LightInfluenceG);
                if (TestSquare.getLightInfluenceB() == null) {
                    TestSquare.setLightInfluenceB(new ArrayList<>());
                }

                TestSquare.getLightInfluenceB().add(LightInfluenceB);
                ColorInfo lightInfo = TestSquare.lighting[playerIndex].lightInfo();
                lightInfo.r += LightInfluence;
                lightInfo.g += LightInfluenceG;
                lightInfo.b += LightInfluenceB;
                if (lightInfo.r > 1.0F) {
                    lightInfo.r = 1.0F;
                }

                if (lightInfo.g > 1.0F) {
                    lightInfo.g = 1.0F;
                }

                if (lightInfo.b > 1.0F) {
                    lightInfo.b = 1.0F;
                }
            }
        }
    }

    public static void LightTileWithFire(IsoGridSquare TestSquare) {
    }

    public static void explode(IsoCell cell, IsoGridSquare gridSquare, int power) {
        if (gridSquare != null) {
            fireRecalc = 1;

            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = 0; z <= 1; z++) {
                        IsoGridSquare surround = cell.getGridSquare(gridSquare.getX() + x, gridSquare.getY() + y, gridSquare.getZ() + z);
                        if (surround != null && Rand.Next(100) < power && IsoFire.CanAddFire(surround, true)) {
                            StartFire(cell, surround, true, Rand.Next(100, 250 + power));
                            surround.BurnWalls(true);
                        }
                    }
                }
            }
        }
    }

    @Deprecated
    public static void MolotovSmash(IsoCell cell, IsoGridSquare gridSquare) {
    }

    public static void Remove(IsoFire DyingFire) {
        if (!FireStack.contains(DyingFire)) {
            System.out.println("IsoFireManager.Remove unknown fire, ignoring");
        } else {
            FireStack.remove(DyingFire);
        }
    }

    public static void RemoveBurningCharacter(IsoGameCharacter BurningCharacter) {
        CharactersOnFire_Stack.remove(BurningCharacter);
    }

    public static void StartFire(IsoCell cell, IsoGridSquare gridSquare, boolean IgniteOnAny, int FireStartingEnergy, int Life) {
        if (FireStartingEnergy > 0) {
            if (gridSquare.getFloor() != null && gridSquare.getFloor().getSprite() != null) {
                FireStartingEnergy -= gridSquare.getFloor().getSprite().firerequirement;
            }

            if (FireStartingEnergy < 5) {
                FireStartingEnergy = 5;
            }

            if (IsoFire.CanAddFire(gridSquare, IgniteOnAny)) {
                if (GameClient.client) {
                    DebugLog.General.warn("The StartFire function was called on Client");
                } else {
                    if (GameServer.server) {
                        INetworkPacket.sendToRelative(
                            PacketTypes.PacketType.StartFire, gridSquare.getX(), gridSquare.getY(), gridSquare, IgniteOnAny, FireStartingEnergy, Life, false
                        );
                    }

                    IsoFire NewFire = new IsoFire(cell, gridSquare, IgniteOnAny, FireStartingEnergy, Life);
                    Add(NewFire);
                    gridSquare.getObjects().add(NewFire);
                    if (Rand.Next(5) == 0) {
                        WorldSoundManager.instance.addSound(NewFire, gridSquare.getX(), gridSquare.getY(), gridSquare.getZ(), 20, 20);
                    }

                    if (PerformanceSettings.fboRenderChunk) {
                        gridSquare.invalidateRenderChunkLevel(64L);
                    }
                }
            }
        }
    }

    public static void StartSmoke(IsoCell cell, IsoGridSquare gridSquare, boolean IgniteOnAny, int FireStartingEnergy, int Life) {
        if (IsoFire.CanAddSmoke(gridSquare, IgniteOnAny)) {
            if (GameClient.client) {
                ByteBufferWriter b = GameClient.connection.startPacket();
                PacketTypes.PacketType.StartFire.doPacket(b);
                b.putInt(gridSquare.getX());
                b.putInt(gridSquare.getY());
                b.putInt(gridSquare.getZ());
                b.putInt(FireStartingEnergy);
                b.putBoolean(IgniteOnAny);
                b.putInt(Life);
                b.putBoolean(true);
                PacketTypes.PacketType.StartFire.send(GameClient.connection);
            } else if (GameServer.server) {
                INetworkPacket.sendToRelative(
                    PacketTypes.PacketType.StartFire, gridSquare.getX(), gridSquare.getY(), gridSquare, IgniteOnAny, FireStartingEnergy, Life, true
                );
            } else {
                IsoFire NewFire = new IsoFire(cell, gridSquare, IgniteOnAny, FireStartingEnergy, Life, true);
                Add(NewFire);
                gridSquare.getObjects().add(NewFire);
                if (PerformanceSettings.fboRenderChunk) {
                    gridSquare.invalidateRenderChunkLevel(64L);
                }
            }
        }
    }

    public static void StartFire(IsoCell cell, IsoGridSquare gridSquare, boolean IgniteOnAny, int FireStartingEnergy) {
        if (!GameClient.client) {
            StartFire(cell, gridSquare, IgniteOnAny, FireStartingEnergy, 0);
        }
    }

    public static void addCharacterOnFire(IsoGameCharacter character) {
        synchronized (charactersOnFire) {
            charactersOnFire.add(character);
        }
    }

    public static void deleteCharacterOnFire(IsoGameCharacter character) {
        synchronized (charactersOnFire) {
            charactersOnFire.remove(character);
        }
    }

    public static void Update() {
        synchronized (charactersOnFire) {
            charactersOnFire.forEach(IsoGameCharacter::SpreadFireMP);
        }

        redOscilatorVal = Math.sin(redOscilator = redOscilator + blueOscilatorRate * oscilatorSpeedScalar);
        greenOscilatorVal = Math.sin(greenOscilator = greenOscilator + blueOscilatorRate * oscilatorSpeedScalar);
        blueOscilatorVal = Math.sin(blueOscilator = blueOscilator + blueOscilatorRate * oscilatorSpeedScalar);
        redOscilatorVal = (redOscilatorVal + 1.0) / 2.0;
        greenOscilatorVal = (greenOscilatorVal + 1.0) / 2.0;
        blueOscilatorVal = (blueOscilatorVal + 1.0) / 2.0;
        redOscilatorVal = redOscilatorVal * oscilatorEffectScalar;
        greenOscilatorVal = greenOscilatorVal * oscilatorEffectScalar;
        blueOscilatorVal = blueOscilatorVal * oscilatorEffectScalar;
        updateStack.clear();
        updateStack.addAll(FireStack);

        for (int i = 0; i < updateStack.size(); i++) {
            IsoFire fire = updateStack.get(i);
            if (fire.getObjectIndex() != -1 && FireStack.contains(fire)) {
                fire.update();
            }
        }

        fireRecalc--;
        if (fireRecalc < 0) {
            fireRecalc = fireRecalcDelay;
        }

        fireSounds.update();
    }

    public static void updateSound(IsoFire fire) {
        fireSounds.addFire(fire);
    }

    public static void stopSound(IsoFire fire) {
        fireSounds.removeFire(fire);
    }

    public static void RemoveAllOn(IsoGridSquare sq) {
        for (int n = FireStack.size() - 1; n >= 0; n--) {
            IsoFire fire = FireStack.get(n);
            if (fire.square == sq) {
                fire.extinctFire();
            }
        }
    }

    public static void Reset() {
        FireStack.clear();
        CharactersOnFire_Stack.clear();
        fireSounds.Reset();
    }

    private static final class FireSounds {
        private final ArrayList<IsoFire> fires = new ArrayList<>();
        private final IsoFireManager.FireSounds.Slot[] slots;
        private final Comparator<IsoFire> comp = new Comparator<IsoFire>() {
            {
                Objects.requireNonNull(FireSounds.this);
            }

            public int compare(IsoFire a, IsoFire b) {
                float aScore = FireSounds.this.getClosestListener(a.square.x + 0.5F, a.square.y + 0.5F, (float)a.square.z);
                float bScore = FireSounds.this.getClosestListener(b.square.x + 0.5F, b.square.y + 0.5F, (float)b.square.z);
                if (aScore > bScore) {
                    return 1;
                } else {
                    return aScore < bScore ? -1 : 0;
                }
            }
        };

        private FireSounds(int numSlots) {
            this.slots = PZArrayUtil.newInstance(IsoFireManager.FireSounds.Slot.class, numSlots, IsoFireManager.FireSounds.Slot::new);
        }

        private void addFire(IsoFire fire) {
            if (!this.fires.contains(fire)) {
                this.fires.add(fire);
            }
        }

        private void removeFire(IsoFire fire) {
            this.fires.remove(fire);
        }

        private void update() {
            if (!GameServer.server) {
                for (int i = 0; i < this.slots.length; i++) {
                    this.slots[i].playing = false;
                }

                if (this.fires.isEmpty()) {
                    this.stopNotPlaying();
                } else {
                    Collections.sort(this.fires, this.comp);
                    int count = Math.min(this.fires.size(), this.slots.length);

                    for (int i = 0; i < count; i++) {
                        IsoFire fire = this.fires.get(i);
                        if (this.shouldPlay(fire)) {
                            int j = this.getExistingSlot(fire);
                            if (j != -1) {
                                this.slots[j].playSound(fire);
                            }
                        }
                    }

                    for (int ix = 0; ix < count; ix++) {
                        IsoFire fire = this.fires.get(ix);
                        if (this.shouldPlay(fire)) {
                            int j = this.getExistingSlot(fire);
                            if (j == -1) {
                                j = this.getFreeSlot();
                                this.slots[j].playSound(fire);
                            }
                        }
                    }

                    this.stopNotPlaying();
                    this.fires.clear();
                }
            }
        }

        private float getClosestListener(float soundX, float soundY, float soundZ) {
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

        private boolean shouldPlay(IsoFire fire) {
            if (fire == null) {
                return false;
            } else if (fire.getObjectIndex() == -1) {
                return false;
            } else {
                return fire.smoke ? true : fire.lifeStage <= 4;
            }
        }

        private int getExistingSlot(IsoFire fire) {
            for (int i = 0; i < this.slots.length; i++) {
                if (this.slots[i].fire == fire) {
                    return i;
                }
            }

            return -1;
        }

        private int getFreeSlot() {
            for (int i = 0; i < this.slots.length; i++) {
                if (!this.slots[i].playing) {
                    return i;
                }
            }

            return -1;
        }

        private void stopNotPlaying() {
            for (int i = 0; i < this.slots.length; i++) {
                IsoFireManager.FireSounds.Slot slot = this.slots[i];
                if (!slot.playing) {
                    slot.stopPlaying();
                    slot.fire = null;
                }
            }
        }

        private void Reset() {
            for (int i = 0; i < this.slots.length; i++) {
                this.slots[i].stopPlaying();
                this.slots[i].fire = null;
                this.slots[i].playing = false;
            }
        }

        private static final class Slot {
            private IsoFire fire;
            private BaseSoundEmitter emitter;
            private final ParameterFireSize parameterFireSize = new ParameterFireSize();
            private long instance;
            private boolean playing;

            private void playSound(IsoFire fire) {
                if (this.emitter == null) {
                    if (Core.soundDisabled) {
                        this.emitter = new DummySoundEmitter();
                    } else {
                        FMODSoundEmitter emitter1 = new FMODSoundEmitter();
                        emitter1.addParameter(this.parameterFireSize);
                        this.emitter = emitter1;
                    }
                }

                this.emitter.setPos(fire.square.x + 0.5F, fire.square.y + 0.5F, fire.square.z);

                int parameter = switch (fire.lifeStage) {
                    case 0, 1 -> 0;
                    case 2, 4 -> 1;
                    case 3 -> 2;
                    default -> 0;
                };
                this.parameterFireSize.setSize(parameter);
                if (fire.isCampfire()) {
                    if (!this.emitter.isPlaying("CampfireRunning")) {
                        this.instance = this.emitter.playSoundImpl("CampfireRunning", (IsoObject)null);
                    }
                } else if (!fire.smoke && !this.emitter.isPlaying("Fire")) {
                    this.instance = this.emitter.playSoundImpl("Fire", (IsoObject)null);
                }

                this.fire = fire;
                this.playing = true;
                this.emitter.tick();
            }

            void stopPlaying() {
                if (this.emitter != null && this.instance != 0L) {
                    if (this.emitter.hasSustainPoints(this.instance)) {
                        this.emitter.triggerCue(this.instance);
                        this.instance = 0L;
                    } else {
                        this.emitter.stopAll();
                        this.instance = 0L;
                    }
                } else {
                    if (this.emitter != null && !this.emitter.isEmpty()) {
                        this.emitter.tick();
                    }
                }
            }
        }
    }
}
