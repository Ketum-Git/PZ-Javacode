// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import fmod.javafmod;
import fmod.javafmodJNI;
import fmod.fmod.FMODFootstep;
import fmod.fmod.FMODManager;
import fmod.fmod.FMODSoundBank;
import fmod.fmod.FMODVoice;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.fmod.FMOD_STUDIO_PLAYBACK_STATE;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import zombie.audio.BaseSoundBank;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoPlayer;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.GameSoundScript;
import zombie.util.StringUtils;

@UsedFromLua
public final class GameSounds {
    public static final int VERSION = 1;
    protected static final HashMap<String, GameSound> soundByName = new HashMap<>();
    protected static final ArrayList<GameSound> sounds = new ArrayList<>();
    private static final GameSounds.BankPreviewSound previewBank = new GameSounds.BankPreviewSound();
    private static final GameSounds.FilePreviewSound previewFile = new GameSounds.FilePreviewSound();
    public static boolean soundIsPaused;
    private static GameSounds.IPreviewSound previewSound;
    public static final boolean VCA_VOLUME = true;
    private static int missingEventCount;

    public static void addSound(GameSound sound) {
        initClipEvents(sound);

        assert !sounds.contains(sound);

        int index = sounds.size();
        if (soundByName.containsKey(sound.getName())) {
            index = 0;

            while (index < sounds.size() && !sounds.get(index).getName().equals(sound.getName())) {
                index++;
            }

            sounds.remove(index);
        }

        sounds.add(index, sound);
        soundByName.put(sound.getName(), sound);
    }

    private static void initClipEvents(GameSound sound) {
        if (!GameServer.server) {
            for (GameSoundClip clip : sound.clips) {
                if (clip.event != null && clip.eventDescription == null) {
                    clip.eventDescription = FMODManager.instance.getEventDescription("event:/" + clip.event);
                    if (clip.eventDescription == null) {
                        DebugLog.Sound.println("No such FMOD event \"%s\" for GameSound \"%s\"", clip.event, sound.getName());
                        missingEventCount++;
                    }

                    clip.eventDescriptionMp = FMODManager.instance.getEventDescription("event:/Remote/" + clip.event);
                    if (clip.eventDescriptionMp != null) {
                        DebugLog.Sound.println("MP event %s", clip.eventDescriptionMp.path);
                    }
                }
            }
        }
    }

    public static boolean isKnownSound(String name) {
        return soundByName.containsKey(name);
    }

    public static GameSound getSound(String name) {
        return getOrCreateSound(name);
    }

    public static GameSound getOrCreateSound(String name) {
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        } else {
            GameSound gameSound = soundByName.get(name);
            if (gameSound == null) {
                DebugLog.Sound.warn("no GameSound called \"" + name + "\", adding a new one");
                gameSound = new GameSound();
                gameSound.name = name;
                gameSound.category = "AUTO";
                GameSoundClip clip = new GameSoundClip(gameSound);
                gameSound.clips.add(clip);
                sounds.add(gameSound);
                soundByName.put(name.replace(".wav", "").replace(".ogg", ""), gameSound);
                if (BaseSoundBank.instance instanceof FMODSoundBank) {
                    FMOD_STUDIO_EVENT_DESCRIPTION eventDescription = FMODManager.instance.getEventDescription("event:/" + name);
                    if (eventDescription != null) {
                        clip.event = name;
                        clip.eventDescription = eventDescription;
                        clip.eventDescriptionMp = FMODManager.instance.getEventDescription("event:/Remote/" + name);
                    } else {
                        String path = null;
                        if (ZomboidFileSystem.instance.getAbsolutePath("media/sound/" + name + ".ogg") != null) {
                            path = "media/sound/" + name + ".ogg";
                        } else if (ZomboidFileSystem.instance.getAbsolutePath("media/sound/" + name + ".wav") != null) {
                            path = "media/sound/" + name + ".wav";
                        }

                        if (path != null) {
                            long sound = FMODManager.instance.loadSound(path);
                            if (sound != 0L) {
                                clip.file = path;
                            }
                        }
                    }

                    if (clip.event == null && clip.file == null) {
                        DebugLog.Sound.warn("couldn't find an FMOD event or .ogg or .wav file for sound \"" + name + "\"");
                    }
                }
            }

            return gameSound;
        }
    }

    private static void loadNonBankSounds() {
        if (BaseSoundBank.instance instanceof FMODSoundBank) {
            for (GameSound sound : sounds) {
                for (GameSoundClip clip : sound.clips) {
                    if (clip.getFile() != null && clip.getFile().isEmpty()) {
                    }
                }
            }
        }
    }

    public static void ScriptsLoaded() {
        ArrayList<GameSoundScript> scriptSounds = ScriptManager.instance.getAllGameSounds();

        for (int i = 0; i < scriptSounds.size(); i++) {
            GameSoundScript scriptSound = scriptSounds.get(i);
            if (!scriptSound.gameSound.clips.isEmpty()) {
                addSound(scriptSound.gameSound);
            }
        }

        scriptSounds.clear();
        loadNonBankSounds();
        loadINI();
        if (Core.debug && BaseSoundBank.instance instanceof FMODSoundBank bank) {
            HashSet<String> usedEvents = new HashSet<>();

            for (GameSound gameSound : sounds) {
                for (GameSoundClip clip : gameSound.clips) {
                    if (clip.getEvent() != null && !clip.getEvent().isEmpty()) {
                        usedEvents.add(clip.getEvent());
                    }
                }
            }

            for (FMODFootstep footstep : bank.footstepMap.values()) {
                usedEvents.add(footstep.wood);
                usedEvents.add(footstep.concrete);
                usedEvents.add(footstep.grass);
                usedEvents.add(footstep.upstairs);
                usedEvents.add(footstep.woodCreak);
            }

            for (FMODVoice voice : bank.voiceMap.values()) {
                usedEvents.add(voice.sound);
            }

            ArrayList<String> unusedEvents = new ArrayList<>();
            long[] bankList = new long[32];
            long[] eventDescList = new long[1024];
            int bankCount = javafmodJNI.FMOD_Studio_System_GetBankList(bankList);

            for (int ix = 0; ix < bankCount; ix++) {
                int eventDescCount = javafmodJNI.FMOD_Studio_Bank_GetEventList(bankList[ix], eventDescList);

                for (int j = 0; j < eventDescCount; j++) {
                    try {
                        String name = javafmodJNI.FMOD_Studio_EventDescription_GetPath(eventDescList[j]);
                        name = name.replace("event:/", "");
                        if (!usedEvents.contains(name)) {
                            unusedEvents.add(name);
                        }
                    } catch (Exception var11) {
                        DebugLog.Sound.warn("FMOD cannot get path for " + eventDescList[j] + " event");
                    }
                }
            }

            unusedEvents.sort(String::compareTo);
            if (DebugLog.isEnabled(DebugType.Sound)) {
                for (String event : unusedEvents) {
                    DebugLog.Sound.warn("FMOD event \"%s\" not used by any GameSound", event);
                }
            } else {
                DebugLog.Sound.warn("FMOD %s missing events", missingEventCount);
                DebugLog.Sound.warn("FMOD %s events not used by any GameSound", unusedEvents.size());
                DebugLog.Sound.warn("FMOD [Turn on DebugType.Sound for detailed lists of missing and unused events]", unusedEvents.size());
            }
        }
    }

    public static void OnReloadSound(GameSoundScript scriptSound) {
        if (sounds.contains(scriptSound.gameSound)) {
            initClipEvents(scriptSound.gameSound);
        } else if (!scriptSound.gameSound.clips.isEmpty()) {
            addSound(scriptSound.gameSound);
        }
    }

    public static ArrayList<String> getCategories() {
        HashSet<String> categories = new HashSet<>();

        for (GameSound sound : sounds) {
            categories.add(sound.getCategory());
        }

        ArrayList<String> sorted = new ArrayList<>(categories);
        Collections.sort(sorted);
        return sorted;
    }

    public static ArrayList<GameSound> getSoundsInCategory(String category) {
        ArrayList<GameSound> result = new ArrayList<>();

        for (GameSound sound : sounds) {
            if (sound.getCategory().equals(category)) {
                result.add(sound);
            }
        }

        return result;
    }

    public static void loadINI() {
        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "sounds.ini";
        ConfigFile configFile = new ConfigFile();
        if (configFile.read(fileName)) {
            if (configFile.getVersion() <= 1) {
                for (ConfigOption option : configFile.getOptions()) {
                    GameSound gameSound = soundByName.get(option.getName());
                    if (gameSound != null) {
                        gameSound.setUserVolume(PZMath.tryParseFloat(option.getValueAsString(), 1.0F));
                    }
                }
            }
        }
    }

    public static void saveINI() {
        ArrayList<ConfigOption> options = new ArrayList<>();

        for (GameSound gameSound : sounds) {
            DoubleConfigOption option = new DoubleConfigOption(gameSound.getName(), 0.0, 2.0, 0.0);
            option.setValue(gameSound.getUserVolume());
            options.add(option);
        }

        String fileName = ZomboidFileSystem.instance.getCacheDir() + File.separator + "sounds.ini";
        ConfigFile configFile = new ConfigFile();
        if (configFile.write(fileName, 1, options)) {
            options.clear();
        }
    }

    public static void previewSound(String name) {
        if (Core.soundDisabled) {
            DebugLog.Sound.printf("sound is disabled, not playing " + name);
        } else if (!isKnownSound(name)) {
            DebugLog.Sound.warn("sound is not known, not playing " + name);
        } else {
            GameSound gameSound = getSound(name);
            if (gameSound == null) {
                DebugLog.Sound.warn("no such GameSound " + name);
            } else {
                GameSoundClip clip = gameSound.getRandomClip();
                if (clip == null) {
                    DebugLog.Sound.warn("GameSound.clips is empty");
                } else {
                    if (soundIsPaused) {
                        if (!GameClient.client) {
                            long channelGroup = javafmod.FMOD_System_GetMasterChannelGroup();
                            javafmod.FMOD_ChannelGroup_SetVolume(channelGroup, 1.0F);
                        }

                        soundIsPaused = false;
                    }

                    if (previewSound != null) {
                        previewSound.stop();
                    }

                    if (clip.getEvent() != null) {
                        if (previewBank.play(clip)) {
                            previewSound = previewBank;
                        }
                    } else if (clip.getFile() != null && previewFile.play(clip)) {
                        previewSound = previewFile;
                    }
                }
            }
        }
    }

    public static void stopPreview() {
        if (previewSound != null) {
            previewSound.stop();
            previewSound = null;
        }
    }

    public static boolean isPreviewPlaying() {
        if (previewSound == null) {
            return false;
        } else if (previewSound.update()) {
            previewSound = null;
            return false;
        } else {
            return previewSound.isPlaying();
        }
    }

    public static void fix3DListenerPosition(boolean inMenu) {
        if (!Core.soundDisabled) {
            if (inMenu) {
                javafmod.FMOD_Studio_Listener3D(0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
            } else {
                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player != null && !player.hasTrait(CharacterTrait.DEAF)) {
                        javafmod.FMOD_Studio_Listener3D(
                            i,
                            player.getX(),
                            player.getY(),
                            player.getZ() * 3.0F,
                            0.0F,
                            0.0F,
                            0.0F,
                            -1.0F / (float)Math.sqrt(2.0),
                            -1.0F / (float)Math.sqrt(2.0),
                            0.0F,
                            0.0F,
                            0.0F,
                            1.0F
                        );
                    }
                }
            }
        }
    }

    public static void Reset() {
        sounds.clear();
        soundByName.clear();
        if (previewSound != null) {
            previewSound.stop();
            previewSound = null;
        }
    }

    private static final class BankPreviewSound implements GameSounds.IPreviewSound {
        long instance;
        GameSoundClip clip;
        float effectiveGain;

        @Override
        public boolean play(GameSoundClip clip) {
            if (clip.eventDescription == null) {
                DebugLog.Sound.error("failed to get event " + clip.getEvent());
                return false;
            } else {
                this.instance = javafmod.FMOD_Studio_System_CreateEventInstance(clip.eventDescription.address);
                if (this.instance < 0L) {
                    DebugLog.Sound.error("failed to create EventInstance: error=" + this.instance);
                    this.instance = 0L;
                    return false;
                } else {
                    this.clip = clip;
                    this.effectiveGain = clip.getEffectiveVolumeInMenu();
                    javafmod.FMOD_Studio_EventInstance_SetVolume(this.instance, this.effectiveGain);
                    javafmod.FMOD_Studio_EventInstance_SetParameterByName(this.instance, "Occlusion", 0.0F);
                    javafmod.FMOD_Studio_StartEvent(this.instance);
                    if (clip.gameSound.master == GameSound.MasterVolume.Music) {
                        javafmod.FMOD_Studio_EventInstance_SetParameterByName(this.instance, "Volume", 10.0F);
                    }

                    return true;
                }
            }
        }

        @Override
        public boolean isPlaying() {
            if (this.instance == 0L) {
                return false;
            } else {
                int state = javafmod.FMOD_Studio_GetPlaybackState(this.instance);
                return state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPING.index
                    ? true
                    : state != FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPED.index;
            }
        }

        @Override
        public boolean update() {
            if (this.instance == 0L) {
                return false;
            } else {
                int state = javafmod.FMOD_Studio_GetPlaybackState(this.instance);
                if (state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPING.index) {
                    return false;
                } else if (state == FMOD_STUDIO_PLAYBACK_STATE.FMOD_STUDIO_PLAYBACK_STOPPED.index) {
                    javafmod.FMOD_Studio_ReleaseEventInstance(this.instance);
                    this.instance = 0L;
                    this.clip = null;
                    return true;
                } else {
                    float targetGain = this.clip.getEffectiveVolumeInMenu();
                    if (this.effectiveGain != targetGain) {
                        this.effectiveGain = targetGain;
                        javafmod.FMOD_Studio_EventInstance_SetVolume(this.instance, this.effectiveGain);
                    }

                    return false;
                }
            }
        }

        @Override
        public void stop() {
            if (this.instance != 0L) {
                javafmod.FMOD_Studio_EventInstance_Stop(this.instance, false);
                javafmod.FMOD_Studio_ReleaseEventInstance(this.instance);
                this.instance = 0L;
                this.clip = null;
            }
        }
    }

    private static final class FilePreviewSound implements GameSounds.IPreviewSound {
        long channel;
        GameSoundClip clip;
        float effectiveGain;

        @Override
        public boolean play(GameSoundClip clip) {
            GameSound gameSound = clip.gameSound;
            long sound = FMODManager.instance.loadSound(clip.getFile(), gameSound.isLooped());
            if (sound == 0L) {
                return false;
            } else {
                this.channel = javafmod.FMOD_System_PlaySound(sound, true);
                this.clip = clip;
                this.effectiveGain = clip.getEffectiveVolumeInMenu();
                javafmod.FMOD_Channel_SetVolume(this.channel, this.effectiveGain);
                javafmod.FMOD_Channel_SetPitch(this.channel, clip.pitch);
                if (gameSound.isLooped()) {
                    javafmod.FMOD_Channel_SetMode(this.channel, 2L);
                }

                javafmod.FMOD_Channel_SetPaused(this.channel, false);
                return true;
            }
        }

        @Override
        public boolean isPlaying() {
            return this.channel == 0L ? false : javafmod.FMOD_Channel_IsPlaying(this.channel);
        }

        @Override
        public boolean update() {
            if (this.channel == 0L) {
                return false;
            } else if (!javafmod.FMOD_Channel_IsPlaying(this.channel)) {
                this.channel = 0L;
                this.clip = null;
                return true;
            } else {
                float targetGain = this.clip.getEffectiveVolumeInMenu();
                if (this.effectiveGain != targetGain) {
                    this.effectiveGain = targetGain;
                    javafmod.FMOD_Channel_SetVolume(this.channel, this.effectiveGain);
                }

                return false;
            }
        }

        @Override
        public void stop() {
            if (this.channel != 0L) {
                javafmod.FMOD_Channel_Stop(this.channel);
                this.channel = 0L;
                this.clip = null;
            }
        }
    }

    private interface IPreviewSound {
        boolean play(GameSoundClip arg0);

        boolean isPlaying();

        boolean update();

        void stop();
    }
}
