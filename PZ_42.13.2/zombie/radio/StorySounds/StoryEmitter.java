// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.StorySounds;

import fmod.javafmod;
import fmod.fmod.FMODManager;
import java.util.ArrayList;
import java.util.Stack;
import zombie.GameSounds;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.Vector2;
import zombie.scripting.objects.CharacterTrait;

public final class StoryEmitter {
    public int max = -1;
    public float volumeMod = 1.0F;
    public boolean coordinate3d = true;
    public Stack<StoryEmitter.Sound> soundStack = new Stack<>();
    public ArrayList<StoryEmitter.Sound> instances = new ArrayList<>();
    public ArrayList<StoryEmitter.Sound> toStart = new ArrayList<>();
    private final Vector2 soundVect = new Vector2();
    private final Vector2 playerVect = new Vector2();

    public int stopSound(long channel) {
        return javafmod.FMOD_Channel_Stop(channel);
    }

    public long playSound(String file, float baseVolume, float x, float y, float z, float minRange, float maxRange) {
        if (this.max != -1 && this.max <= this.instances.size() + this.toStart.size()) {
            return 0L;
        } else {
            GameSound gameSound = GameSounds.getSound(file);
            if (gameSound == null) {
                return 0L;
            } else {
                GameSoundClip clip = gameSound.getRandomClip();
                long sound = FMODManager.instance.loadSound(file);
                if (sound == 0L) {
                    return 0L;
                } else {
                    StoryEmitter.Sound s;
                    if (this.soundStack.isEmpty()) {
                        s = new StoryEmitter.Sound();
                    } else {
                        s = this.soundStack.pop();
                    }

                    s.minRange = minRange;
                    s.maxRange = maxRange;
                    s.x = x;
                    s.y = y;
                    s.z = z;
                    s.volume = baseVolume * this.volumeMod;
                    s.sound = sound;
                    s.channel = javafmod.FMOD_System_PlaySound(sound, true);
                    this.toStart.add(s);
                    javafmod.FMOD_Channel_Set3DAttributes(
                        s.channel,
                        s.x - IsoPlayer.getInstance().getX(),
                        s.y - IsoPlayer.getInstance().getY(),
                        s.z - IsoPlayer.getInstance().getZ(),
                        0.0F,
                        0.0F,
                        0.0F
                    );
                    javafmod.FMOD_Channel_Set3DOcclusion(s.channel, 1.0F, 1.0F);
                    if (IsoPlayer.getInstance() != null && IsoPlayer.getInstance().hasTrait(CharacterTrait.DEAF)) {
                        javafmod.FMOD_Channel_SetVolume(s.channel, 0.0F);
                    } else {
                        javafmod.FMOD_Channel_SetVolume(s.channel, s.volume);
                    }

                    return s.channel;
                }
            }
        }
    }

    public void tick() {
        for (int n = 0; n < this.toStart.size(); n++) {
            StoryEmitter.Sound s = this.toStart.get(n);
            javafmod.FMOD_Channel_SetPaused(s.channel, false);
            this.instances.add(s);
        }

        this.toStart.clear();

        for (int n = 0; n < this.instances.size(); n++) {
            StoryEmitter.Sound sound = this.instances.get(n);
            if (!javafmod.FMOD_Channel_IsPlaying(sound.channel)) {
                this.soundStack.push(sound);
                this.instances.remove(sound);
                n--;
            } else {
                float distance = IsoUtils.DistanceManhatten(
                        sound.x, sound.y, IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), sound.z, IsoPlayer.getInstance().getZ()
                    )
                    / sound.maxRange;
                if (distance > 1.0F) {
                    distance = 1.0F;
                }

                if (!this.coordinate3d) {
                    javafmod.FMOD_Channel_Set3DAttributes(
                        sound.channel,
                        Math.abs(sound.x - IsoPlayer.getInstance().getX()),
                        Math.abs(sound.y - IsoPlayer.getInstance().getY()),
                        Math.abs(sound.z - IsoPlayer.getInstance().getZ()),
                        0.0F,
                        0.0F,
                        0.0F
                    );
                } else {
                    javafmod.FMOD_Channel_Set3DAttributes(
                        sound.channel,
                        Math.abs(sound.x - IsoPlayer.getInstance().getX()),
                        Math.abs(sound.z - IsoPlayer.getInstance().getZ()),
                        Math.abs(sound.y - IsoPlayer.getInstance().getY()),
                        0.0F,
                        0.0F,
                        0.0F
                    );
                }

                javafmod.FMOD_System_SetReverbDefault(0, 18);
                javafmod.FMOD_Channel_SetReverbProperties(sound.channel, 0, 1.0F);
                javafmod.FMOD_Channel_Set3DMinMaxDistance(sound.channel, sound.minRange, sound.maxRange);
                float directOcclude = 0.0F;
                float reverbOcclude = 0.0F;
                IsoGridSquare current = IsoPlayer.getInstance().getCurrentSquare();
                this.soundVect.set(sound.x, sound.y);
                this.playerVect.set(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY());
                float angle = (float)Math.toDegrees(this.playerVect.angleTo(this.soundVect));
                float playerAngle = (float)Math.toDegrees(IsoPlayer.getInstance().getForwardDirection().getDirectionNeg());
                if (playerAngle >= 0.0F && playerAngle <= 90.0F) {
                    playerAngle = -90.0F - playerAngle;
                } else if (playerAngle > 90.0F && playerAngle <= 180.0F) {
                    playerAngle = 90.0F + (180.0F - playerAngle);
                } else if (playerAngle < 0.0F && playerAngle >= -90.0F) {
                    playerAngle = 0.0F - (90.0F + playerAngle);
                } else if (playerAngle < 0.0F && playerAngle >= -180.0F) {
                    playerAngle = 90.0F - (180.0F + playerAngle);
                }

                float d = Math.abs(angle - playerAngle) % 360.0F;
                float angle2 = d > 180.0F ? 360.0F - d : d;
                float facingMod = (180.0F - angle2) / 180.0F;
                distance /= 0.4F;
                if (distance > 1.0F) {
                    distance = 1.0F;
                }

                directOcclude = 0.85F * distance * facingMod;
                reverbOcclude = 0.85F * distance * facingMod;
                if (current.getRoom() != null) {
                    directOcclude = 0.75F + 0.1F * distance + 0.1F * facingMod;
                    reverbOcclude = 0.75F + 0.1F * distance + 0.1F * facingMod;
                }

                javafmod.FMOD_Channel_Set3DOcclusion(sound.channel, directOcclude, reverbOcclude);
            }
        }
    }

    public static final class Sound {
        public long sound;
        public long channel;
        public float volume;
        public float x;
        public float y;
        public float z;
        public float minRange;
        public float maxRange;
    }
}
