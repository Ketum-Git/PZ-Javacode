// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import fmod.fmod.Audio;
import java.util.ArrayList;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.enums.MaterialType;
import zombie.scripting.objects.AmmoType;

public abstract class BaseSoundManager {
    public boolean allowMusic = true;

    public abstract boolean isRemastered();

    public abstract void update1();

    public abstract void update3();

    public abstract void update2();

    public abstract void update4();

    public abstract void CacheSound(String file);

    public abstract void StopSound(Audio SoundEffect);

    public abstract void StopMusic();

    public abstract void Purge();

    public abstract void stop();

    protected abstract boolean HasMusic(Audio arg0);

    public abstract void Update();

    public abstract Audio Start(Audio musicTrack, float f, String PrefMusic);

    public abstract Audio PrepareMusic(String name);

    public abstract void PlayWorldSoundWav(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, int choices, boolean ignoreOutside);

    public abstract Audio PlayWorldSoundWav(String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside);

    public abstract Audio PlayWorldSoundWav(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside);

    public abstract Audio PlayWorldSound(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, int choices, boolean ignoreOutside);

    public abstract Audio PlayWorldSound(String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside);

    public abstract Audio PlayWorldSoundImpl(
        String name, boolean loop, int sx, int sy, int sz, float pitchVar, float radius, float maxGain, boolean ignoreOutside
    );

    public abstract Audio PlayWorldSound(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside);

    public abstract void update3D();

    public abstract Audio PlaySoundWav(String name, int variations, boolean loop, float maxGain);

    public abstract Audio PlaySoundWav(String name, boolean loop, float maxGain);

    public abstract Audio PlaySoundWav(String name, boolean loop, float maxGain, float pitchVar);

    public abstract Audio PlayWorldSoundWavImpl(
        String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside
    );

    public abstract Audio PlayJukeboxSound(String name, boolean loop, float maxGain);

    public abstract Audio PlaySoundEvenSilent(String name, boolean loop, float maxGain);

    public abstract Audio PlaySound(String name, boolean loop, float maxGain);

    public abstract Audio PlaySound(String name, boolean loop, float pitchVar, float maxGain);

    public abstract Audio PlayMusic(String n, String name, boolean loop, float maxGain);

    public abstract void PlayAsMusic(String name, Audio musicTrack, boolean loop, float volume);

    public abstract void setMusicState(String stateName);

    public abstract void setMusicWakeState(IsoPlayer player, String stateName);

    public abstract void DoMusic(String name, boolean bLoop);

    public abstract float getMusicPosition();

    public abstract void CheckDoMusic();

    public abstract void stopMusic(String name);

    public abstract void playMusicNonTriggered(String name, float gain);

    public abstract void playAmbient(String name);

    public abstract void playMusic(String name);

    public abstract boolean isPlayingMusic();

    public abstract boolean IsMusicPlaying();

    public abstract String getCurrentMusicName();

    public abstract String getCurrentMusicLibrary();

    public abstract void PlayAsMusic(String name, Audio musicTrack, float volume, boolean bloop);

    public abstract long playUISound(String name);

    public abstract boolean isPlayingUISound(String name);

    public abstract boolean isPlayingUISound(long eventInstance);

    public abstract void stopUISound(long eventInstance);

    public abstract void FadeOutMusic(String name, int milli);

    public abstract Audio BlendThenStart(Audio musicTrack, float f, String PrefMusic);

    public abstract void BlendVolume(Audio audio, float targetVolume, float blendSpeedAlpha);

    public abstract void BlendVolume(Audio audio, float targetVolume);

    public abstract void setSoundVolume(float volume);

    public abstract float getSoundVolume();

    public abstract void setAmbientVolume(float volume);

    public abstract float getAmbientVolume();

    public abstract void setMusicVolume(float volume);

    public abstract float getMusicVolume();

    public abstract void setVehicleEngineVolume(float volume);

    public abstract float getVehicleEngineVolume();

    public abstract void playNightAmbient(String choice);

    public abstract ArrayList<Audio> getAmbientPieces();

    public abstract void pauseSoundAndMusic();

    public abstract void pauseSoundAndMusic(boolean arg0);

    public abstract void resumeSoundAndMusic();

    public abstract void debugScriptSounds();

    public abstract void registerEmitter(BaseSoundEmitter emitter);

    public abstract void unregisterEmitter(BaseSoundEmitter emitter);

    public abstract boolean isListenerInRange(float x, float y, float range);

    public abstract void playImpactSound(IsoGridSquare var1, AmmoType var2);

    public abstract void playImpactSound(IsoGridSquare var1, AmmoType var2, MaterialType var3);

    public abstract void playDamageSound(IsoGridSquare arg0, MaterialType arg1);

    public abstract void playDestructionSound(IsoGridSquare arg0, MaterialType arg1);

    public abstract void dumpEventInstancesToTextFile();
}
