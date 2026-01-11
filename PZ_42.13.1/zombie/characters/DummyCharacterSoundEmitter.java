// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.IsoObject;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

@UsedFromLua
public final class DummyCharacterSoundEmitter extends BaseCharacterSoundEmitter {
    public float x;
    public float y;
    public float z;
    private final HashMap<Long, String> sounds = new HashMap<>();

    public DummyCharacterSoundEmitter(IsoGameCharacter chr) {
        super(chr);
    }

    @Override
    public void register() {
    }

    @Override
    public void unregister() {
    }

    @Override
    public long playVocals(String file) {
        return 0L;
    }

    @Override
    public void playFootsteps(String file, float volume) {
    }

    @Override
    public long playSound(String file) {
        long id = Rand.Next(Integer.MAX_VALUE);
        this.sounds.put(id, file);
        if (GameClient.client) {
            INetworkPacket.send(PacketTypes.PacketType.PlaySound, file, false, this.character);
        }

        return id;
    }

    @Override
    public long playSound(String file, IsoObject proxy) {
        return this.playSound(file);
    }

    @Override
    public long playSoundImpl(String file, IsoObject proxy) {
        long id = Rand.Next(Long.MAX_VALUE);
        this.sounds.put(id, file);
        return id;
    }

    @Override
    public void tick() {
    }

    @Override
    public void set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isClear() {
        return this.sounds.isEmpty();
    }

    @Override
    public void setPitch(long handle, float pitch) {
    }

    @Override
    public void setVolume(long handle, float volume) {
    }

    @Override
    public int stopSound(long channel) {
        if (GameClient.client) {
            GameClient.instance.StopSound(this.character, this.sounds.get(channel), false);
        }

        this.sounds.remove(channel);
        return 0;
    }

    @Override
    public int stopSoundDelayRelease(long channel) {
        if (GameClient.client) {
            GameClient.instance.StopSound(this.character, this.sounds.get(channel), false);
        }

        this.sounds.remove(channel);
        return 0;
    }

    @Override
    public void stopSoundLocal(long handle) {
        this.sounds.remove(handle);
    }

    @Override
    public void stopOrTriggerSoundLocal(long handle) {
        this.sounds.remove(handle);
    }

    @Override
    public void stopOrTriggerSound(long handle) {
        if (GameClient.client) {
            GameClient.instance.StopSound(this.character, this.sounds.get(handle), true);
        }

        this.sounds.remove(handle);
    }

    @Override
    public void stopOrTriggerSoundByName(String name) {
        this.sounds.values().remove(name);
    }

    @Override
    public void stopAll() {
        if (GameClient.client) {
            for (String name : this.sounds.values()) {
                GameClient.instance.StopSound(this.character, name, false);
            }
        }

        this.sounds.clear();
    }

    @Override
    public int stopSoundByName(String soundName) {
        this.sounds.values().remove(soundName);
        return 0;
    }

    @Override
    public boolean hasSoundsToStart() {
        return false;
    }

    @Override
    public boolean isPlaying(long channel) {
        return this.sounds.containsKey(channel);
    }

    @Override
    public boolean isPlaying(String alias) {
        return this.sounds.containsValue(alias);
    }

    @Override
    public void setParameterValue(long soundRef, FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription, float value) {
    }

    @Override
    public void setParameterValueByName(long soundRef, String parameterName, float value) {
    }

    public boolean hasSustainPoints(long handle) {
        return false;
    }
}
