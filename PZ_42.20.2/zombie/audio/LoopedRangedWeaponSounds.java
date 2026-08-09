// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.HashMap;
import java.util.Map;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.util.StringUtils;

public final class LoopedRangedWeaponSounds {
    public static final LoopedRangedWeaponSounds INSTANCE = new LoopedRangedWeaponSounds();
    private final Map<Object, LoopedRangedWeaponSounds.Sound> soundMap = new HashMap<>();
    private final Map<Short, Object> ownerMap = new HashMap<>();

    private LoopedRangedWeaponSounds.Sound getOrCreateSound(Object owner) {
        LoopedRangedWeaponSounds.Sound sound = this.soundMap.get(owner);
        if (sound == null) {
            sound = new LoopedRangedWeaponSounds.Sound();
            sound.owner = owner;
            this.soundMap.put(owner, sound);
        }

        return sound;
    }

    public void startAttackLoopSound(Object owner, String soundName) {
        LoopedRangedWeaponSounds.Sound sound = this.getOrCreateSound(owner);
        sound.startAttackLoopSound(soundName);
    }

    public void stopAttackLoopSound(Object owner, boolean cancelPrevious) {
        LoopedRangedWeaponSounds.Sound sound = this.soundMap.get(owner);
        if (sound != null) {
            sound.stopAttackLoopSound(cancelPrevious);
        }
    }

    public boolean isPlaying(Object owner) {
        LoopedRangedWeaponSounds.Sound sound = this.soundMap.get(owner);
        return sound == null ? false : sound.isPlaying();
    }

    public boolean isPlaying(Object owner, String soundName) {
        LoopedRangedWeaponSounds.Sound sound = this.soundMap.get(owner);
        return sound == null ? false : sound.isPlaying(soundName);
    }

    public void setPosition(Object owner, float x, float y, float z) {
        LoopedRangedWeaponSounds.Sound sound = this.getOrCreateSound(owner);
        sound.setPosition(x, y, z);
    }

    public void setParameters(Object owner, float firearmInside, float firearmRoomSize) {
        LoopedRangedWeaponSounds.Sound sound = this.getOrCreateSound(owner);
        sound.setParameters(firearmInside, firearmRoomSize);
    }

    public void update() {
        for (LoopedRangedWeaponSounds.Sound sound : this.soundMap.values()) {
            sound.update();
        }
    }

    public Object getOwnerObjectForRemotePlayer(short playerId) {
        return this.ownerMap.computeIfAbsent(playerId, Short::valueOf);
    }

    private static boolean isOwnerRemotePlayer(Object owner) {
        return owner instanceof Short;
    }

    public void disconnectPlayer(short playerId) {
        Object owner = this.ownerMap.get(playerId);
        if (owner != null) {
            LoopedRangedWeaponSounds.Sound sound = this.soundMap.get(owner);
            if (sound != null) {
                sound.stopAttackLoopSound(true);
            }
        }
    }

    private static final class Parameter {
        final String name;
        final FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription;
        float value;
        float valueSet = Float.NaN;

        Parameter(String name) {
            this.name = name;
            this.parameterDescription = FMODManager.instance.getParameterDescription(name);
        }

        void update(BaseSoundEmitter emitter, long instance) {
            if (Float.compare(this.value, this.valueSet) != 0) {
                this.valueSet = this.value;
                emitter.setParameterValue(instance, this.parameterDescription, this.value);
            }
        }
    }

    private static final class Sound {
        Object owner;
        float x;
        float y;
        float z;
        final LoopedRangedWeaponSounds.Parameter firearmInside = new LoopedRangedWeaponSounds.Parameter("FirearmInside");
        final LoopedRangedWeaponSounds.Parameter firearmRoomSize = new LoopedRangedWeaponSounds.Parameter("FirearmRoomSize");
        final LoopedRangedWeaponSounds.Parameter localPlayer = new LoopedRangedWeaponSounds.Parameter("LocalPlayer");
        final LoopedRangedWeaponSounds.Parameter occlusion = new LoopedRangedWeaponSounds.Parameter("Occlusion");
        String attackLoopSoundName;
        long attackLoopInstance;
        long attackLoopInstancePrev;
        BaseSoundEmitter emitter;

        BaseSoundEmitter getEmitter() {
            if (this.emitter == null) {
                this.emitter = IsoWorld.instance.getFreeEmitter();
                if (LoopedRangedWeaponSounds.isOwnerRemotePlayer(this.owner)) {
                    this.emitter.setPlayRemoteEvents(true);
                }

                IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            }

            return this.emitter;
        }

        void setPosition(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        void setParameters(float firearmInside, float firearmRoomSize) {
            this.firearmInside.value = firearmInside;
            this.firearmRoomSize.value = firearmRoomSize;
        }

        boolean isPlaying() {
            return this.emitter != null && this.emitter.isPlaying(this.attackLoopInstance);
        }

        boolean isPlaying(String soundName) {
            return StringUtils.equalsIgnoreCase(soundName, this.attackLoopSoundName) && this.isPlaying();
        }

        void startAttackLoopSound(String soundName) {
            this.stopAttackLoopSound(true);
            this.resetParameters();
            this.attackLoopSoundName = soundName;
            this.attackLoopInstance = this.getEmitter().playSoundImpl(soundName, (IsoObject)null);
        }

        void stopAttackLoopSound(boolean cancelPrevious) {
            this.attackLoopSoundName = null;
            if (cancelPrevious && this.attackLoopInstancePrev != 0L) {
                this.getEmitter().setParameterValueByName(this.attackLoopInstancePrev, "FirearmCancelShot", 1.0F);
                this.attackLoopInstancePrev = 0L;
            }

            if (this.attackLoopInstance != 0L) {
                this.getEmitter().stopSoundDelayRelease(this.attackLoopInstance);
                this.attackLoopInstancePrev = this.attackLoopInstance;
                this.attackLoopInstance = 0L;
            }
        }

        void updateParameters() {
            if (this.attackLoopInstance != 0L) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
                IsoPlayer listener = FMODParameterUtils.getClosestListener(this.x, this.y, this.z);
                if (listener != null && square != null) {
                    this.occlusion.value = square.isCouldSee(listener.getIndex()) ? 0.0F : 1.0F;
                } else {
                    this.occlusion.value = 1.0F;
                }

                this.localPlayer.value = IsoPlayer.isLocalPlayer(this.owner) ? 1.0F : 0.0F;
                this.firearmInside.update(this.emitter, this.attackLoopInstance);
                this.firearmRoomSize.update(this.emitter, this.attackLoopInstance);
                this.localPlayer.update(this.emitter, this.attackLoopInstance);
                this.occlusion.update(this.emitter, this.attackLoopInstance);
            }
        }

        void resetParameters() {
            this.firearmInside.valueSet = Float.NaN;
            this.firearmRoomSize.valueSet = Float.NaN;
            this.localPlayer.valueSet = Float.NaN;
            this.occlusion.valueSet = Float.NaN;
        }

        void update() {
            if (this.emitter != null) {
                this.emitter.setPos(this.x, this.y, this.z);
                this.updateParameters();
                this.emitter.tick();
                if (this.emitter.isEmpty()) {
                    this.resetParameters();
                    this.emitter.setPlayRemoteEvents(false);
                    IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
                    this.emitter = null;
                }
            }
        }
    }
}
