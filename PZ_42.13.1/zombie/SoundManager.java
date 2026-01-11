// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import fmod.javafmod;
import fmod.javafmodJNI;
import fmod.fmod.Audio;
import fmod.fmod.FMODAudio;
import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundEmitter;
import fmod.fmod.FMOD_STUDIO_EVENT_CALLBACK;
import fmod.fmod.FMOD_STUDIO_EVENT_CALLBACK_TYPE;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PLAYBACK_STATE;
import fmod.fmod.IFMODParameterUpdater;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import zombie.audio.BaseSoundEmitter;
import zombie.audio.FMODParameter;
import zombie.audio.FMODParameterList;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.audio.parameters.ParameterBulletHitSurface;
import zombie.audio.parameters.ParameterMusicActionStyle;
import zombie.audio.parameters.ParameterMusicIntensity;
import zombie.audio.parameters.ParameterMusicLibrary;
import zombie.audio.parameters.ParameterMusicState;
import zombie.audio.parameters.ParameterMusicThreat;
import zombie.audio.parameters.ParameterMusicToggleMute;
import zombie.audio.parameters.ParameterMusicWakeState;
import zombie.audio.parameters.ParameterMusicZombiesTargeting;
import zombie.audio.parameters.ParameterMusicZombiesVisible;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.gameStates.MainScreenState;
import zombie.input.GameKeyboard;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.enums.MaterialType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ScriptModule;
import zombie.util.StringUtils;

@UsedFromLua
public final class SoundManager extends BaseSoundManager implements IFMODParameterUpdater {
    public float soundVolume = 0.8F;
    public float musicVolume = 0.36F;
    public float ambientVolume = 0.8F;
    public float vehicleEngineVolume = 0.5F;
    private final ParameterMusicActionStyle parameterMusicActionStyle = new ParameterMusicActionStyle();
    private final ParameterMusicIntensity parameterMusicIntensity = new ParameterMusicIntensity();
    private final ParameterMusicThreat parameterMusicThreat = new ParameterMusicThreat();
    private final ParameterMusicLibrary parameterMusicLibrary = new ParameterMusicLibrary();
    private final ParameterMusicState parameterMusicState = new ParameterMusicState();
    private final ParameterMusicToggleMute parameterMusicToggleMute = new ParameterMusicToggleMute();
    private final ParameterMusicWakeState parameterMusicWakeState = new ParameterMusicWakeState();
    private final ParameterMusicZombiesTargeting parameterMusicZombiesTargeting = new ParameterMusicZombiesTargeting();
    private final ParameterMusicZombiesVisible parameterMusicZombiesVisible = new ParameterMusicZombiesVisible();
    private final FMODParameterList fmodParameters = new FMODParameterList();
    private boolean initialized;
    private long inGameGroupBus;
    private long musicGroupBus;
    private FMODSoundEmitter musicEmitter;
    private long musicCombinedEvent;
    private FMODSoundEmitter uiEmitter;
    private boolean uiSoundMuted = false;
    private final String bulletImpactSound = "BulletImpact";
    private final String bulletHitSurfaceSound = "BulletHitSurface";
    private final SoundManager.Music music = new SoundManager.Music();
    public ArrayList<Audio> ambientPieces = new ArrayList<>();
    private boolean muted;
    private long[] bankList = new long[32];
    private long[] eventDescList = new long[256];
    private long[] eventInstList = new long[256];
    private long[] pausedEventInstances = new long[128];
    private float[] pausedEventVolumes = new float[128];
    private int pausedEventCount;
    private final HashSet<BaseSoundEmitter> emitters = new HashSet<>();
    private static final ArrayList<SoundManager.AmbientSoundEffect> ambientSoundEffects = new ArrayList<>();
    public static BaseSoundManager instance;
    private String currentMusicName;
    private String currentMusicLibrary;
    private final FMOD_STUDIO_EVENT_CALLBACK musicEventCallback = new FMOD_STUDIO_EVENT_CALLBACK() {
        {
            Objects.requireNonNull(SoundManager.this);
        }

        @Override
        public void timelineMarker(long eventInstance, String name, int position) {
            DebugLog.Sound.debugln("timelineMarker %s %d", name, position);
            if ("Lightning".equals(name)) {
                MainScreenState.getInstance().lightningTimelineMarker = true;
            }
        }
    };
    private final ArrayList<SoundManager.ImpactSound> impactSounds = new ArrayList<>();

    @Override
    public FMODParameterList getFMODParameters() {
        return this.fmodParameters;
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

    public void setUiSoundMuted(boolean uiSoundMuted) {
        this.uiSoundMuted = uiSoundMuted;
    }

    public boolean isUiSoundMuted() {
        return this.uiSoundMuted;
    }

    @Override
    public boolean isRemastered() {
        int library = Core.getInstance().getOptionMusicLibrary();
        return library == 1 || library == 3 && Rand.Next(2) == 0;
    }

    @Override
    public void BlendVolume(Audio audio, float targetVolume) {
    }

    @Override
    public void BlendVolume(Audio audio, float targetVolume, float blendSpeedAlpha) {
    }

    @Override
    public Audio BlendThenStart(Audio musicTrack, float f, String PrefMusic) {
        return null;
    }

    @Override
    public void FadeOutMusic(String name, int milli) {
    }

    @Override
    public void PlayAsMusic(String name, Audio musicTrack, float volume, boolean bloop) {
    }

    @Override
    public long playUISound(String name) {
        if (this.uiSoundMuted) {
            return 0L;
        } else {
            GameSound gameSound = GameSounds.getSound(name);
            if (gameSound != null && !gameSound.clips.isEmpty()) {
                GameSoundClip clip = gameSound.getRandomClip();
                DebugLog.Sound.println("playUISound %s", name);
                long eventInstance = this.uiEmitter.playClip(clip, null);
                if (eventInstance != 0L && IsoPlayer.getInstance() != null) {
                    if (IsoPlayer.getInstance().getReanimatedCorpse() != null) {
                        this.uiEmitter
                            .setPos(
                                IsoPlayer.getInstance().getReanimatedCorpse().getX(),
                                IsoPlayer.getInstance().getReanimatedCorpse().getY(),
                                IsoPlayer.getInstance().getReanimatedCorpse().getZ()
                            );
                    } else {
                        this.uiEmitter.setPos(IsoPlayer.getInstance().getX(), IsoPlayer.getInstance().getY(), IsoPlayer.getInstance().getZ());
                    }

                    this.uiEmitter.setParameterValue(eventInstance, FMODManager.instance.getParameterDescription("FootstepMaterial"), 2.0F);
                    this.uiEmitter.setParameterValue(eventInstance, FMODManager.instance.getParameterDescription("FootstepMaterial2"), 0.0F);
                    this.uiEmitter.setParameterValue(eventInstance, FMODManager.instance.getParameterDescription("Inside"), 0.0F);
                    this.uiEmitter.setParameterValue(eventInstance, FMODManager.instance.getParameterDescription("RainIntensity"), 0.0F);
                }

                this.uiEmitter.tick();
                javafmod.FMOD_System_Update();
                return eventInstance;
            } else {
                return 0L;
            }
        }
    }

    @Override
    public boolean isPlayingUISound(String name) {
        return this.uiEmitter.isPlaying(name);
    }

    @Override
    public boolean isPlayingUISound(long eventInstance) {
        return this.uiEmitter.isPlaying(eventInstance);
    }

    @Override
    public void stopUISound(long eventInstance) {
        this.uiEmitter.stopSound(eventInstance);
    }

    @Override
    public boolean IsMusicPlaying() {
        return false;
    }

    @Override
    public boolean isPlayingMusic() {
        return this.music.isPlaying();
    }

    @Override
    public ArrayList<Audio> getAmbientPieces() {
        return this.ambientPieces;
    }

    private void gatherInGameEventInstances() {
        this.pausedEventCount = 0;
        int bankCount = javafmodJNI.FMOD_Studio_System_GetBankCount();
        if (this.bankList.length < bankCount) {
            this.bankList = new long[bankCount];
        }

        bankCount = javafmodJNI.FMOD_Studio_System_GetBankList(this.bankList);

        for (int i = 0; i < bankCount; i++) {
            int eventDescCount = javafmodJNI.FMOD_Studio_Bank_GetEventCount(this.bankList[i]);
            if (this.eventDescList.length < eventDescCount) {
                this.eventDescList = new long[eventDescCount];
            }

            eventDescCount = javafmodJNI.FMOD_Studio_Bank_GetEventList(this.bankList[i], this.eventDescList);

            for (int j = 0; j < eventDescCount; j++) {
                int eventInstCount = javafmodJNI.FMOD_Studio_EventDescription_GetInstanceCount(this.eventDescList[j]);
                if (this.eventInstList.length < eventInstCount) {
                    this.eventInstList = new long[eventInstCount];
                }

                eventInstCount = javafmodJNI.FMOD_Studio_EventDescription_GetInstanceList(this.eventDescList[j], this.eventInstList);

                for (int k = 0; k < eventInstCount; k++) {
                    int state = javafmod.FMOD_Studio_GetPlaybackState(this.eventInstList[k]);
                    if (state != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPED.index) {
                        boolean paused = javafmodJNI.FMOD_Studio_EventInstance_GetPaused(this.eventInstList[k]);
                        if (!paused) {
                            if (this.pausedEventInstances.length < this.pausedEventCount + 1) {
                                this.pausedEventInstances = Arrays.copyOf(this.pausedEventInstances, this.pausedEventCount + 128);
                                this.pausedEventVolumes = Arrays.copyOf(this.pausedEventVolumes, this.pausedEventInstances.length);
                            }

                            this.pausedEventInstances[this.pausedEventCount] = this.eventInstList[k];
                            this.pausedEventVolumes[this.pausedEventCount] = javafmodJNI.FMOD_Studio_EventInstance_GetVolume(this.eventInstList[k]);
                            this.pausedEventCount++;
                        }
                    }
                }
            }
        }
    }

    @Override
    public void pauseSoundAndMusic() {
        this.pauseSoundAndMusic(false);
    }

    @Override
    public void pauseSoundAndMusic(boolean bOptionallyKeepMusicPlaying) {
        boolean NOISEWORKS = true;
        boolean bPauseMusic = !bOptionallyKeepMusicPlaying;
        if (GameClient.client) {
            this.muted = true;
            javafmod.FMOD_Studio_Bus_SetMute(this.inGameGroupBus, true);
            javafmod.FMOD_Studio_Bus_SetMute(this.musicGroupBus, bPauseMusic);
            GameSounds.soundIsPaused = true;
        } else {
            javafmod.FMOD_Studio_Bus_SetPaused(this.inGameGroupBus, true);
            javafmod.FMOD_Channel_SetPaused(FMODManager.instance.channelGroupInGameNonBankSounds, true);
            GameSounds.soundIsPaused = true;
        }
    }

    @Override
    public void resumeSoundAndMusic() {
        boolean NOISEWORKS = true;
        if (this.muted) {
            this.muted = false;
            javafmod.FMOD_Studio_Bus_SetMute(this.inGameGroupBus, false);
            javafmod.FMOD_Studio_Bus_SetMute(this.musicGroupBus, false);
            javafmod.FMOD_ChannelGroup_SetPaused(FMODManager.instance.channelGroupInGameNonBankSounds, false);
            GameSounds.soundIsPaused = false;
        } else {
            javafmod.FMOD_Studio_Bus_SetPaused(this.inGameGroupBus, false);
            javafmod.FMOD_ChannelGroup_SetPaused(FMODManager.instance.channelGroupInGameNonBankSounds, false);
            GameSounds.soundIsPaused = false;
        }
    }

    private void debugScriptSound(Item item, String sound) {
        if (sound != null && !sound.isEmpty()) {
            if (!GameSounds.isKnownSound(sound)) {
                DebugLog.General.warn("no such sound \"" + sound + "\" in item " + item.getFullName());
            }
        }
    }

    @Override
    public void debugScriptSounds() {
        if (Core.debug) {
            for (ScriptModule module : ScriptManager.instance.moduleMap.values()) {
                for (Item item : module.items.getScriptMap().values()) {
                    this.debugScriptSound(item, item.getBreakSound());
                    this.debugScriptSound(item, item.getBulletOutSound());
                    this.debugScriptSound(item, item.getCloseSound());
                    this.debugScriptSound(item, item.getCustomEatSound());
                    this.debugScriptSound(item, item.getDoorHitSound());
                    this.debugScriptSound(item, item.getCountDownSound());
                    this.debugScriptSound(item, item.getExplosionSound());
                    this.debugScriptSound(item, item.getImpactSound());
                    this.debugScriptSound(item, item.getOpenSound());
                    this.debugScriptSound(item, item.getPutInSound());
                    this.debugScriptSound(item, item.getPlaceOneSound());
                    this.debugScriptSound(item, item.getPlaceMultipleSound());
                    this.debugScriptSound(item, item.getShellFallSound());
                    this.debugScriptSound(item, item.getSwingSound());
                    this.debugScriptSound(item, item.getInsertAmmoSound());
                    this.debugScriptSound(item, item.getInsertAmmoStartSound());
                    this.debugScriptSound(item, item.getInsertAmmoStopSound());
                    this.debugScriptSound(item, item.getEjectAmmoSound());
                    this.debugScriptSound(item, item.getEjectAmmoStartSound());
                    this.debugScriptSound(item, item.getEjectAmmoStopSound());
                }
            }
        }
    }

    @Override
    public void registerEmitter(BaseSoundEmitter emitter) {
        this.emitters.add(emitter);
    }

    @Override
    public void unregisterEmitter(BaseSoundEmitter emitter) {
        this.emitters.remove(emitter);
    }

    @Override
    public boolean isListenerInRange(float x, float y, float range) {
        if (GameServer.server) {
            return false;
        } else {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && !player.hasTrait(CharacterTrait.DEAF) && IsoUtils.DistanceToSquared(player.getX(), player.getY(), x, y) < range * range) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public void playNightAmbient(String choice) {
        DebugLog.Sound.println("playNightAmbient: " + choice);

        for (int i = 0; i < ambientSoundEffects.size(); i++) {
            SoundManager.AmbientSoundEffect snd = ambientSoundEffects.get(i);
            if (snd.getName().equals(choice)) {
                snd.setVolume(Rand.Next(700, 1500) / 1000.0F);
                snd.start();
                this.ambientPieces.add(snd);
                return;
            }
        }

        SoundManager.AmbientSoundEffect snd = new SoundManager.AmbientSoundEffect(choice);
        snd.setVolume(Rand.Next(700, 1500) / 1000.0F);
        snd.setName(choice);
        snd.start();
        this.ambientPieces.add(snd);
        ambientSoundEffects.add(snd);
    }

    @Override
    public void playMusic(String name) {
        this.DoMusic(name, false);
    }

    @Override
    public void playAmbient(String name) {
    }

    @Override
    public void playMusicNonTriggered(String name, float gain) {
    }

    @Override
    public void stopMusic(String name) {
        if (this.isPlayingMusic()) {
            if (StringUtils.isNullOrWhitespace(name) || name.equalsIgnoreCase(this.getCurrentMusicName())) {
                this.StopMusic();
            }
        }
    }

    @Override
    public void CheckDoMusic() {
    }

    @Override
    public float getMusicPosition() {
        return this.isPlayingMusic() ? this.music.getPosition() : 0.0F;
    }

    @Override
    public void DoMusic(String name, boolean bLoop) {
        if (this.allowMusic && Core.getInstance().getOptionMusicVolume() != 0) {
            if (this.isPlayingMusic()) {
                this.StopMusic();
            }

            int library = Core.getInstance().getOptionMusicLibrary();
            boolean bRemastered = library == 1;
            GameSound gameSound = GameSounds.getSound(name);
            GameSoundClip clip = null;
            if (gameSound != null && !gameSound.clips.isEmpty()) {
                clip = gameSound.getRandomClip();
            }

            if (clip != null && clip.getEvent() != null) {
                if (clip.eventDescription != null) {
                    long musicEvent = clip.eventDescription.address;
                    javafmod.FMOD_Studio_LoadEventSampleData(musicEvent);
                    this.music.instance = javafmod.FMOD_Studio_System_CreateEventInstance(musicEvent);
                    this.music.clip = clip;
                    this.music.effectiveVolume = clip.getEffectiveVolume();
                    javafmod.FMOD_Studio_EventInstance_SetParameterByName(this.music.instance, "Volume", 10.0F);
                    javafmod.FMOD_Studio_EventInstance_SetVolume(this.music.instance, this.music.effectiveVolume);
                    javafmod.FMOD_Studio_StartEvent(this.music.instance);
                }
            } else if (clip != null && clip.getFile() != null) {
                long sound = FMODManager.instance.loadSound(clip.getFile());
                if (sound > 0L) {
                    this.music.channel = javafmod.FMOD_System_PlaySound(sound, true);
                    this.music.clip = clip;
                    this.music.effectiveVolume = clip.getEffectiveVolume();
                    javafmod.FMOD_Channel_SetVolume(this.music.channel, this.music.effectiveVolume);
                    javafmod.FMOD_Channel_SetPitch(this.music.channel, clip.pitch);
                    javafmod.FMOD_Channel_SetPaused(this.music.channel, false);
                }
            }

            this.currentMusicName = name;
            this.currentMusicLibrary = bRemastered ? "official" : "earlyaccess";
        }
    }

    @Override
    public void PlayAsMusic(String name, Audio musicTrack, boolean loop, float volume) {
    }

    @Override
    public void setMusicState(String stateName) {
        switch (stateName) {
            case "MainMenu":
                this.parameterMusicState.setState(ParameterMusicState.State.MainMenu);
                break;
            case "Loading":
                this.parameterMusicState.setState(ParameterMusicState.State.Loading);
                break;
            case "InGame":
                this.parameterMusicState.setState(ParameterMusicState.State.InGame);
                break;
            case "PauseMenu":
                this.parameterMusicState.setState(ParameterMusicState.State.PauseMenu);
                break;
            case "Tutorial":
                this.parameterMusicState.setState(ParameterMusicState.State.Tutorial);
                break;
            default:
                DebugLog.General.warn("unknown MusicState \"%s\"", stateName);
        }
    }

    @Override
    public void setMusicWakeState(IsoPlayer player, String stateName) {
        switch (stateName) {
            case "Awake":
                this.parameterMusicWakeState.setState(player, ParameterMusicWakeState.State.Awake);
                break;
            case "Sleeping":
                this.parameterMusicWakeState.setState(player, ParameterMusicWakeState.State.Sleeping);
                break;
            case "WakeNormal":
                this.parameterMusicWakeState.setState(player, ParameterMusicWakeState.State.WakeNormal);
                break;
            case "WakeNightmare":
                this.parameterMusicWakeState.setState(player, ParameterMusicWakeState.State.WakeNightmare);
                break;
            case "WakeZombies":
                this.parameterMusicWakeState.setState(player, ParameterMusicWakeState.State.WakeZombies);
                break;
            default:
                DebugLog.General.warn("unknown MusicWakeState \"%s\"", stateName);
        }
    }

    @Override
    public Audio PlayMusic(String n, String name, boolean loop, float maxGain) {
        return null;
    }

    @Override
    public Audio PlaySound(String name, boolean loop, float maxGain, float pitchVar) {
        return null;
    }

    @Override
    public Audio PlaySound(String name, boolean loop, float maxGain) {
        if (GameServer.server) {
            return null;
        } else if (IsoWorld.instance == null) {
            return null;
        } else {
            BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter();
            emitter.setPos(0.0F, 0.0F, 0.0F);
            long inst = emitter.playSound(name);
            return inst != 0L ? new FMODAudio(emitter) : null;
        }
    }

    @Override
    public Audio PlaySoundEvenSilent(String name, boolean loop, float maxGain) {
        return null;
    }

    @Override
    public Audio PlayJukeboxSound(String name, boolean loop, float maxGain) {
        return null;
    }

    @Override
    public Audio PlaySoundWav(String name, boolean loop, float maxGain, float pitchVar) {
        return null;
    }

    @Override
    public Audio PlaySoundWav(String name, boolean loop, float maxGain) {
        return null;
    }

    @Override
    public Audio PlaySoundWav(String name, int variations, boolean loop, float maxGain) {
        return null;
    }

    @Override
    public void update3D() {
    }

    @Override
    public Audio PlayWorldSound(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        return this.PlayWorldSound(name, false, source, pitchVar, radius, maxGain, ignoreOutside);
    }

    @Override
    public Audio PlayWorldSound(String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        if (!GameServer.server && source != null) {
            if (GameClient.client) {
                GameClient.instance.PlayWorldSound(name, source.x, source.y, (byte)source.z);
            }

            return this.PlayWorldSoundImpl(name, loop, source.getX(), source.getY(), source.getZ(), pitchVar, radius, maxGain, ignoreOutside);
        } else {
            return null;
        }
    }

    @Override
    public Audio PlayWorldSoundImpl(String name, boolean loop, int sx, int sy, int sz, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(sx + 0.5F, sy + 0.5F, sz);
        long eventInstance = emitter.playSoundImpl(name, (IsoObject)null);
        return new FMODAudio(emitter);
    }

    @Override
    public Audio PlayWorldSound(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, int choices, boolean ignoreOutside) {
        return this.PlayWorldSound(name, source, pitchVar, radius, maxGain, ignoreOutside);
    }

    @Override
    public Audio PlayWorldSoundWav(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        return this.PlayWorldSoundWav(name, false, source, pitchVar, radius, maxGain, ignoreOutside);
    }

    @Override
    public Audio PlayWorldSoundWav(String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        if (!GameServer.server && source != null) {
            if (GameClient.client) {
                GameClient.instance.PlayWorldSound(name, source.getX(), source.getY(), (byte)source.getZ());
            }

            return this.PlayWorldSoundWavImpl(name, loop, source, pitchVar, radius, maxGain, ignoreOutside);
        } else {
            return null;
        }
    }

    @Override
    public Audio PlayWorldSoundWavImpl(String name, boolean loop, IsoGridSquare source, float pitchVar, float radius, float maxGain, boolean ignoreOutside) {
        BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter(source.getX() + 0.5F, source.getY() + 0.5F, source.getZ());
        emitter.playSound(name);
        return new FMODAudio(emitter);
    }

    @Override
    public void PlayWorldSoundWav(String name, IsoGridSquare source, float pitchVar, float radius, float maxGain, int choices, boolean ignoreOutside) {
        Integer choice = Rand.Next(choices) + 1;
        this.PlayWorldSoundWav(name + choice.toString(), source, pitchVar, radius, maxGain, ignoreOutside);
    }

    @Override
    public Audio PrepareMusic(String name) {
        return null;
    }

    @Override
    public Audio Start(Audio musicTrack, float f, String PrefMusic) {
        return null;
    }

    @Override
    public void Update() {
        if (!this.initialized) {
            this.initialized = true;
            this.inGameGroupBus = javafmod.FMOD_Studio_System_GetBus("bus:/InGame");
            this.musicGroupBus = javafmod.FMOD_Studio_System_GetBus("bus:/Music");
            this.musicEmitter = new FMODSoundEmitter();
            this.unregisterEmitter(this.musicEmitter);
            this.musicEmitter.parameterUpdater = this;
            this.fmodParameters.add(this.parameterMusicActionStyle);
            this.fmodParameters.add(this.parameterMusicIntensity);
            this.fmodParameters.add(this.parameterMusicThreat);
            this.fmodParameters.add(this.parameterMusicLibrary);
            this.fmodParameters.add(this.parameterMusicState);
            this.fmodParameters.add(this.parameterMusicToggleMute);
            this.fmodParameters.add(this.parameterMusicWakeState);
            this.fmodParameters.add(this.parameterMusicZombiesTargeting);
            this.fmodParameters.add(this.parameterMusicZombiesVisible);
            if (this.uiEmitter == null) {
                this.uiEmitter = new FMODSoundEmitter();
            }
        }

        FMODSoundEmitter.update();
        this.updateMusic();
        this.uiEmitter.tick();
        if (this.uiEmitter.isEmpty()) {
            this.uiEmitter.clearParameters();
        }

        for (int n = 0; n < this.ambientPieces.size(); n++) {
            Audio soundEffect = this.ambientPieces.get(n);
            if (IsoPlayer.allPlayersDead()) {
                soundEffect.stop();
            }

            if (!soundEffect.isPlaying()) {
                soundEffect.stop();
                this.ambientPieces.remove(soundEffect);
                n--;
            } else if (soundEffect instanceof SoundManager.AmbientSoundEffect ambientSoundEffect) {
                ambientSoundEffect.update();
            }
        }

        AmbientStreamManager.instance.update();
        if (!this.allowMusic) {
            this.StopMusic();
        }

        if (this.music.isPlaying()) {
            this.music.update();
        }

        FMODManager.instance.tick();
    }

    @Override
    protected boolean HasMusic(Audio musicTrack) {
        return false;
    }

    @Override
    public void Purge() {
    }

    @Override
    public void stop() {
        for (BaseSoundEmitter emitter : this.emitters) {
            emitter.stopAll();
        }

        this.emitters.clear();
        long channelGroup = javafmod.FMOD_System_GetMasterChannelGroup();
        javafmod.FMOD_ChannelGroup_Stop(channelGroup);
        this.pausedEventCount = 0;
    }

    @Override
    public void StopMusic() {
        this.music.stop();
    }

    @Override
    public void StopSound(Audio SoundEffect) {
        SoundEffect.stop();
    }

    @Override
    public void CacheSound(String file) {
    }

    @Override
    public void update4() {
    }

    @Override
    public void update2() {
    }

    @Override
    public void update3() {
    }

    @Override
    public void update1() {
    }

    @Override
    public void setSoundVolume(float volume) {
        this.soundVolume = volume;
        long vca = javafmodJNI.FMOD_Studio_System_GetVCA("vca:/Settings_Sfx");
        if (vca != 0L) {
            javafmodJNI.FMOD_Studio_VCA_SetVolume(vca, volume);
        }
    }

    @Override
    public float getSoundVolume() {
        return this.soundVolume;
    }

    @Override
    public void setAmbientVolume(float volume) {
        volume = 1.0F;
        this.ambientVolume = volume;
        long vca = javafmodJNI.FMOD_Studio_System_GetVCA("vca:/Settings_Ambience");
        if (vca != 0L) {
            javafmodJNI.FMOD_Studio_VCA_SetVolume(vca, volume);
        }
    }

    @Override
    public float getAmbientVolume() {
        return 1.0F;
    }

    @Override
    public void setMusicVolume(float volume) {
        this.musicVolume = volume;
        long vca = javafmodJNI.FMOD_Studio_System_GetVCA("vca:/Settings_Music");
        if (vca != 0L) {
            javafmodJNI.FMOD_Studio_VCA_SetVolume(vca, volume);
        }
    }

    @Override
    public float getMusicVolume() {
        return this.musicVolume;
    }

    @Override
    public void setVehicleEngineVolume(float volume) {
        this.vehicleEngineVolume = volume;
        long vca = javafmodJNI.FMOD_Studio_System_GetVCA("vca:/Settings_VehicleEngines");
        if (vca != 0L) {
            javafmodJNI.FMOD_Studio_VCA_SetVolume(vca, volume);
        }
    }

    @Override
    public float getVehicleEngineVolume() {
        return this.vehicleEngineVolume;
    }

    @Override
    public String getCurrentMusicName() {
        return this.isPlayingMusic() ? this.currentMusicName : null;
    }

    @Override
    public String getCurrentMusicLibrary() {
        return this.isPlayingMusic() ? this.currentMusicLibrary : null;
    }

    private void updateMusic() {
        this.fmodParameters.update();
        if (GameKeyboard.isKeyPressed("Toggle Music")) {
            this.allowMusic = !this.allowMusic;
            if (!this.allowMusic) {
                this.StopMusic();
            }
        }

        if (!this.musicEmitter.isPlaying(this.musicCombinedEvent)) {
            this.musicCombinedEvent = this.musicEmitter.playSoundImpl("MusicCombined", (IsoObject)null);
            if (this.musicCombinedEvent != 0L) {
                javafmod.FMOD_Studio_EventInstance_SetCallback(
                    this.musicCombinedEvent, this.musicEventCallback, FMOD_STUDIO_EVENT_CALLBACK_TYPE.FMOD_STUDIO_EVENT_CALLBACK_TIMELINE_MARKER.bit
                );
            }
        }

        if (this.musicEmitter.isPlaying(this.musicCombinedEvent)) {
            this.musicEmitter.setVolume(this.musicCombinedEvent, this.allowMusic ? 1.0F : 0.0F);
        }

        this.musicEmitter.tick();
    }

    public FMODSoundEmitter getUIEmitter() {
        if (this.uiEmitter == null) {
            this.uiEmitter = new FMODSoundEmitter();
        }

        return this.uiEmitter;
    }

    @Override
    public void playImpactSound(IsoGridSquare isoGridSquare, AmmoType ammoType) {
        PropertyContainer propertyContainer = isoGridSquare.getProperties();
        String materialTypeString = propertyContainer.get("MaterialType");
        this.playImpactSound(isoGridSquare, ammoType, materialTypeString);
    }

    @Override
    public void playImpactSound(IsoGridSquare isoGridSquare, AmmoType ammoType, MaterialType materialType) {
        String materialTypeString = materialType.toString();
        this.playImpactSound(isoGridSquare, ammoType, materialTypeString);
    }

    @Override
    public void playDamageSound(IsoGridSquare isoGridSquare, MaterialType materialType) {
        String materialTypeString = materialType.toString();
        this.playDamageSound(isoGridSquare, materialTypeString);
    }

    @Override
    public void playDestructionSound(IsoGridSquare isoGridSquare, MaterialType materialType) {
        String materialTypeString = materialType.toString();
        this.playDestructionSound(isoGridSquare, materialTypeString);
    }

    private void playImpactSound(IsoGridSquare isoGridSquare, AmmoType ammoType, String materialTypeString) {
        if (!StringUtils.isNullOrWhitespace(materialTypeString)) {
            if (StringUtils.isNullOrWhitespace(materialTypeString)) {
                DebugLog.Sound.debugln("MaterialType not defined for : " + materialTypeString);
                materialTypeString = MaterialType.Concrete.name();
            }

            boolean bLimitInstances = ammoType == AmmoType.SHOTGUN_SHELLS;
            if (!bLimitInstances || !this.isPlayingImpactSound(materialTypeString, isoGridSquare.x, isoGridSquare.y, isoGridSquare.z)) {
                DebugLog.Sound.debugln("playing impact sound %s at %d,%d,%d", materialTypeString, isoGridSquare.x, isoGridSquare.y, isoGridSquare.z);
                if (bLimitInstances) {
                    this.recordImpactSound(materialTypeString, isoGridSquare.x, isoGridSquare.y, isoGridSquare.z);
                }

                BaseSoundEmitter emitter = IsoWorld.instance.getFreeEmitter();
                long inst = emitter.playSound("BulletImpact", isoGridSquare);
                float value = StringUtils.tryParseEnum(ParameterBulletHitSurface.Material.class, materialTypeString, ParameterBulletHitSurface.Material.Default)
                    .label;
                emitter.setParameterValueByName(inst, "BulletHitSurface", value);
            }
        }
    }

    private void playDamageSound(IsoGridSquare isoGridSquare, String materialTypeString) {
    }

    private void playDestructionSound(IsoGridSquare isoGridSquare, String materialTypeString) {
    }

    private void recordImpactSound(String soundName, int x, int y, int z) {
        long currentTimeMS = System.currentTimeMillis();
        SoundManager.ImpactSound impactSound = new SoundManager.ImpactSound(soundName, x, y, z, currentTimeMS);
        this.impactSounds.add(impactSound);
    }

    private boolean isPlayingImpactSound(String soundName, int x, int y, int z) {
        long currentTimeMS = System.currentTimeMillis();

        for (int i = 0; i < this.impactSounds.size(); i++) {
            SoundManager.ImpactSound impactSound = this.impactSounds.get(i);
            if (impactSound.startTimeMs < currentTimeMS - 250L) {
                this.impactSounds.remove(i--);
            } else if (impactSound.matches(soundName, x, y, z) && impactSound.startTimeMs >= currentTimeMS - 100L) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void dumpEventInstancesToTextFile() {
        int bankCount = javafmodJNI.FMOD_Studio_System_GetBankCount();
        if (this.bankList.length < bankCount) {
            this.bankList = new long[bankCount];
        }

        bankCount = javafmodJNI.FMOD_Studio_System_GetBankList(this.bankList);
        HashMap<String, Integer> counts = new HashMap<>();
        int maxLength = 0;

        for (int i = 0; i < bankCount; i++) {
            int eventDescCount = javafmodJNI.FMOD_Studio_Bank_GetEventCount(this.bankList[i]);
            if (this.eventDescList.length < eventDescCount) {
                this.eventDescList = new long[eventDescCount];
            }

            eventDescCount = javafmodJNI.FMOD_Studio_Bank_GetEventList(this.bankList[i], this.eventDescList);

            for (int j = 0; j < eventDescCount; j++) {
                int eventInstCount = javafmodJNI.FMOD_Studio_EventDescription_GetInstanceCount(this.eventDescList[j]);
                if (eventInstCount != 0) {
                    if (this.eventInstList.length < eventInstCount) {
                        this.eventInstList = new long[eventInstCount];
                    }

                    String eventPath = javafmodJNI.FMOD_Studio_EventDescription_GetPath(this.eventDescList[j]);
                    eventInstCount = javafmodJNI.FMOD_Studio_EventDescription_GetInstanceList(this.eventDescList[j], this.eventInstList);

                    for (int k = 0; k < eventInstCount; k++) {
                        int state = javafmod.FMOD_Studio_GetPlaybackState(this.eventInstList[k]);
                        if (eventPath.contains("LightBulb")) {
                            boolean var11 = true;
                        }
                    }

                    counts.put(eventPath, eventInstCount);
                    maxLength = PZMath.max(maxLength, eventPath.length());
                }
            }
        }

        ArrayList<String> sorted = new ArrayList<>(counts.keySet());
        sorted.sort(String::compareTo);
        String path = ZomboidFileSystem.instance.getCacheDir() + File.separator + "sound-event-instances.txt";

        try (
            FileOutputStream fos = new FileOutputStream(path, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter(osw, true);
        ) {
            pw.print('\ufeff');

            for (String eventPath : sorted) {
                pw.format("%" + (maxLength + 4) + "s %d", eventPath, counts.get(eventPath)).println();
            }
        } catch (Exception var18) {
            ExceptionLogger.logException(var18);
        }
    }

    @UsedFromLua
    public static final class AmbientSoundEffect implements Audio {
        public String name;
        public long eventInstance;
        public float gain;
        public GameSoundClip clip;
        public float effectiveVolume;

        public AmbientSoundEffect(String name) {
            GameSound gameSound = GameSounds.getSound(name);
            if (gameSound != null && !gameSound.clips.isEmpty()) {
                GameSoundClip clip = gameSound.getRandomClip();
                if (clip.getEvent() != null) {
                    if (clip.eventDescription != null) {
                        this.eventInstance = javafmod.FMOD_Studio_System_CreateEventInstance(clip.eventDescription.address);
                        if (this.eventInstance >= 0L) {
                            this.clip = clip;
                        }
                    }
                }
            }
        }

        @Override
        public void setVolume(float volume) {
            if (this.eventInstance > 0L) {
                this.gain = volume;
                this.effectiveVolume = this.clip.getEffectiveVolume();
                javafmod.FMOD_Studio_EventInstance_SetVolume(this.eventInstance, this.gain * this.effectiveVolume);
            }
        }

        @Override
        public void start() {
            if (this.eventInstance > 0L) {
                javafmod.FMOD_Studio_StartEvent(this.eventInstance);
            }
        }

        @Override
        public void pause() {
        }

        @Override
        public void stop() {
            DebugLog.Sound.println("stop ambient " + this.name);
            if (this.eventInstance > 0L) {
                javafmod.FMOD_Studio_EventInstance_Stop(this.eventInstance, false);
            }
        }

        @Override
        public boolean isPlaying() {
            if (this.eventInstance <= 0L) {
                return false;
            } else {
                int state = javafmod.FMOD_Studio_GetPlaybackState(this.eventInstance);
                return state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STARTING.index
                    || state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_PLAYING.index
                    || state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_SUSTAINING.index;
            }
        }

        @Override
        public void setName(String choice) {
            this.name = choice;
        }

        @Override
        public String getName() {
            return this.name;
        }

        public void update() {
            if (this.clip != null) {
                this.clip = this.clip.checkReloaded();
                float targetVolume = this.clip.getEffectiveVolume();
                if (this.effectiveVolume != targetVolume) {
                    this.effectiveVolume = targetVolume;
                    javafmod.FMOD_Studio_EventInstance_SetVolume(this.eventInstance, this.gain * this.effectiveVolume);
                }
            }
        }
    }

    private static final class ImpactSound {
        String soundName;
        int x;
        int y;
        int z;
        long startTimeMs;

        ImpactSound(String soundName, int x, int y, int z, long startTimeMs) {
            this.soundName = soundName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.startTimeMs = startTimeMs;
        }

        boolean matches(String soundName, int x, int y, int z) {
            return this.soundName.equalsIgnoreCase(soundName) && this.x == x && this.y == y && this.z == z;
        }
    }

    private static final class Music {
        public GameSoundClip clip;
        public long instance;
        public long channel;
        public long sound;
        public float effectiveVolume;

        public boolean isPlaying() {
            if (this.instance != 0L) {
                int state = javafmod.FMOD_Studio_GetPlaybackState(this.instance);
                return state != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPED.index
                    && state != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPING.index;
            } else {
                return this.channel != 0L && javafmod.FMOD_Channel_IsPlaying(this.channel);
            }
        }

        public void update() {
            this.clip = this.clip.checkReloaded();
            float targetVolume = this.clip.getEffectiveVolume();
            if (this.effectiveVolume != targetVolume) {
                this.effectiveVolume = targetVolume;
                if (this.instance != 0L) {
                    javafmod.FMOD_Studio_EventInstance_SetVolume(this.instance, this.effectiveVolume);
                }

                if (this.channel != 0L) {
                    javafmod.FMOD_Channel_SetVolume(this.channel, this.effectiveVolume);
                }
            }
        }

        public float getPosition() {
            if (this.instance != 0L) {
                long pos = javafmod.FMOD_Studio_GetTimelinePosition(this.instance);
                return (float)pos;
            } else if (this.channel != 0L) {
                long pos = javafmod.FMOD_Channel_GetPosition(this.channel, 1);
                return (float)pos;
            } else {
                return 0.0F;
            }
        }

        public void stop() {
            if (this.instance != 0L) {
                javafmod.FMOD_Studio_EventInstance_Stop(this.instance, false);
                javafmod.FMOD_Studio_ReleaseEventInstance(this.instance);
                this.instance = 0L;
            }

            if (this.channel != 0L) {
                javafmod.FMOD_Channel_Stop(this.channel);
                this.channel = 0L;
                javafmod.FMOD_Sound_Release(this.sound);
                this.sound = 0L;
            }
        }
    }
}
