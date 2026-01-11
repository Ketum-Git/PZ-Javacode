// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import fmod.javafmod;
import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PLAYBACK_STATE;
import java.nio.ByteBuffer;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.WorldSoundManager;
import zombie.audio.GameSound;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;

public class Alarm {
    protected static long inst;
    protected static FMOD_STUDIO_EVENT_DESCRIPTION event;
    public boolean finished;
    private int x;
    private int y;
    private float volume;
    private float occlusion;
    private float endGameTime;

    public Alarm(int x, int y) {
        this.x = x;
        this.y = y;
        int soundDurationSeconds = 49;
        float currentTimeHours = (float)GameTime.instance.getWorldAgeHours();
        this.endGameTime = currentTimeHours + 0.013611111F * (1440.0F / GameTime.instance.getMinutesPerDay());
    }

    public void update() {
        if (!GameClient.client) {
            WorldSoundManager.instance.addSound(this, PZMath.fastfloor((float)this.x), PZMath.fastfloor((float)this.y), 0, 600, 600);
        }

        if (!GameServer.server) {
            this.updateSound();
            this.checkMusicIntensityEvent();
            if (GameTime.getInstance().getWorldAgeHours() >= this.endGameTime) {
                if (inst != 0L) {
                    javafmod.FMOD_Studio_EventInstance_Stop(inst, false);
                    inst = 0L;
                }

                this.finished = true;
            }
        }
    }

    protected void updateSound() {
        if (!GameServer.server && !Core.soundDisabled && !this.finished) {
            if (FMODManager.instance.getNumListeners() != 0) {
                if (inst == 0L) {
                    event = FMODManager.instance.getEventDescription("event:/Meta/HouseAlarm");
                    if (event != null) {
                        javafmod.FMOD_Studio_LoadEventSampleData(event.address);
                        inst = javafmod.FMOD_Studio_System_CreateEventInstance(event.address);
                    }
                }

                if (inst > 0L) {
                    float volume = SoundManager.instance.getSoundVolume();
                    volume = 1.0F;
                    GameSound gameSound = GameSounds.getSound("HouseAlarm");
                    if (gameSound != null) {
                        volume *= gameSound.getUserVolume();
                    }

                    if (volume != this.volume) {
                        javafmod.FMOD_Studio_EventInstance_SetVolume(inst, volume);
                        this.volume = volume;
                    }

                    javafmod.FMOD_Studio_EventInstance3D(inst, this.x, this.y, 0.0F);
                    if (javafmod.FMOD_Studio_GetPlaybackState(inst) != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_PLAYING.index
                        && javafmod.FMOD_Studio_GetPlaybackState(inst) != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STARTING.index) {
                        if (javafmod.FMOD_Studio_GetPlaybackState(inst) == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPING.index) {
                            this.finished = true;
                            return;
                        }

                        javafmod.FMOD_Studio_StartEvent(inst);
                        System.out.println(javafmod.FMOD_Studio_GetPlaybackState(inst));
                    }

                    float occlusion = 0.0F;
                    if (IsoPlayer.numPlayers == 1) {
                        IsoGridSquare sqPlayer = IsoPlayer.getInstance().getCurrentSquare();
                        if (sqPlayer != null && !sqPlayer.has(IsoFlagType.exterior)) {
                            occlusion = 0.2F;
                            IsoGridSquare sqSound = IsoWorld.instance.getCell().getGridSquare(this.x, this.y, 0);
                            if (sqSound != null && sqSound.getBuilding() == sqPlayer.getBuilding()) {
                                occlusion = 0.0F;
                            }
                        }
                    }

                    if (this.occlusion != occlusion) {
                        this.occlusion = occlusion;
                        javafmod.FMOD_Studio_EventInstance_SetParameterByName(inst, "Occlusion", this.occlusion);
                    }
                }
            }
        }
    }

    public void save(ByteBuffer bb) {
        bb.putInt(this.x);
        bb.putInt(this.y);
        bb.putFloat(this.endGameTime);
    }

    public void load(ByteBuffer bb, int worldVersion) {
        this.x = bb.getInt();
        this.y = bb.getInt();
        this.endGameTime = bb.getFloat();
    }

    private void checkMusicIntensityEvent() {
        if (!GameServer.server) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && !player.hasTrait(CharacterTrait.DEAF) && !player.isDead()) {
                    float distSq = IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY());
                    if (!(distSq > 2500.0F)) {
                        player.triggerMusicIntensityEvent("AlarmNearby");
                        break;
                    }
                }
            }
        }
    }
}
