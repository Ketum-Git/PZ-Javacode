// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.raknet;

import fmod.FMODRecordPosition;
import fmod.FMODSoundData;
import fmod.FMOD_DriverInfo;
import fmod.FMOD_RESULT;
import fmod.javafmod;
import fmod.javafmodJNI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.Platform;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.input.GameKeyboard;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Radio;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.FakeClientManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.radio.devices.DeviceData;
import zombie.vehicles.VehiclePart;

public class VoiceManager {
    private static final int FMOD_SOUND_MODE = 1154;
    public static final int modePPT = 1;
    public static final int modeVAD = 2;
    public static final int modeMute = 3;
    public static final int VADModeQuality = 1;
    public static final int VADModeLowBitrate = 2;
    public static final int VADModeAggressive = 3;
    public static final int VADModeVeryAggressive = 4;
    public static final int AGCModeAdaptiveAnalog = 1;
    public static final int AGCModeAdaptiveDigital = 2;
    public static final int AGCModeFixedDigital = 3;
    private static final int bufferSize = 192;
    private static final int complexity = 1;
    private static boolean serverVOIPEnable = true;
    private static int sampleRate = 16000;
    private static int period = 300;
    private static int buffering = 8000;
    private static float minDistance;
    private static float maxDistance;
    private static boolean is3D;
    private boolean isEnable = true;
    private boolean isModeVad;
    private boolean isModePpt;
    private int vadMode = 3;
    private int agcMode = 2;
    private int volumeMic;
    private int volumePlayers;
    public static boolean voipDisabled;
    private boolean isServer;
    private static byte[] fmodReceiveBuffer;
    private final FMODSoundData fmodSoundData = new FMODSoundData();
    private final FMODRecordPosition fmodRecordPosition = new FMODRecordPosition();
    private int fmodSoundDataError;
    private int fmodVoiceRecordDriverId;
    private long fmodChannelGroup;
    private long fmodRecordSound;
    private Semaphore recDevSemaphore;
    private boolean initialiseRecDev;
    private boolean initialisedRecDev;
    private long indicatorIsVoice;
    private Thread thread;
    private boolean quit;
    private long timeLast;
    private final boolean isDebug = false;
    private final boolean isDebugLoopback = false;
    private final boolean isDebugLoopbackLong = false;
    public static VoiceManager instance = new VoiceManager();
    byte[] buf = new byte[192];
    private final Object notifier = new Object();
    private boolean isClient;
    private boolean testingMicrophone;
    private long testingMicrophoneMs;
    private static long timestamp;

    public static VoiceManager getInstance() {
        return instance;
    }

    public void DeinitRecSound() {
        this.initialisedRecDev = false;
        if (this.fmodRecordSound != 0L) {
            javafmod.FMOD_RecordSound_Release(this.fmodRecordSound);
            this.fmodRecordSound = 0L;
        }

        fmodReceiveBuffer = null;
    }

    public void ResetRecSound() {
        if (this.initialisedRecDev && this.fmodRecordSound != 0L) {
            int result = javafmod.FMOD_System_RecordStop(this.fmodVoiceRecordDriverId);
            if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
                DebugLog.Voice.warn("FMOD_System_RecordStop result=%d", result);
            }
        }

        this.DeinitRecSound();
        this.fmodRecordSound = javafmod.FMOD_System_CreateRecordSound(this.fmodVoiceRecordDriverId, 1096L, 2L, sampleRate, this.agcMode);
        if (this.fmodRecordSound == 0L) {
            DebugLog.Voice.warn("FMOD_System_CreateSound result=%d", this.fmodRecordSound);
        }

        javafmod.FMOD_System_SetRecordVolume(1L - Math.round(Math.pow(1.4, 11 - this.volumeMic)));
        if (this.initialiseRecDev) {
            int result = javafmod.FMOD_System_RecordStart(this.fmodVoiceRecordDriverId, this.fmodRecordSound, true);
            if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
                DebugLog.Voice.warn("FMOD_System_RecordStart result=%d", result);
            }
        }

        javafmod.FMOD_System_SetVADMode(this.vadMode - 1);
        fmodReceiveBuffer = new byte[2048];
        this.initialisedRecDev = true;
    }

    public void VoiceRestartClient(boolean isEnable) {
        if (GameClient.connection != null) {
            if (isEnable) {
                this.loadConfig();
                this.VoiceConnectReq(GameClient.connection.getConnectedGUID());
            } else {
                this.threadSafeCode(this::DeinitRecSound);
                this.VoiceConnectClose(GameClient.connection.getConnectedGUID());
                this.loadConfig();
            }
        } else {
            this.loadConfig();
            if (isEnable) {
                this.InitRecDeviceForTest();
            } else {
                this.threadSafeCode(this::DeinitRecSound);
            }
        }
    }

    void VoiceInitClient() {
        this.isServer = false;
        this.recDevSemaphore = new Semaphore(1);
        fmodReceiveBuffer = null;
        RakVoice.RVInit(192);
        RakVoice.SetComplexity(1);
    }

    void VoiceInitServer(boolean enable, int sampleRate, int period, int complexity, int buffering, double minDistance, double maxDistance, boolean is3D) {
        this.isServer = true;
        if (!(period == 2 | period == 5 | period == 10 | period == 20 | period == 40 | period == 60)) {
            DebugLog.Voice.error("Invalid period=%d", period);
        } else if (!(sampleRate == 8000 | sampleRate == 16000 | sampleRate == 24000)) {
            DebugLog.Voice.error("Invalid sample rate=%d", sampleRate);
        } else if (complexity < 0 | complexity > 10) {
            DebugLog.Voice.error("Invalid quality=%d", complexity);
        } else if (buffering < 0 | buffering > 32000) {
            DebugLog.Voice.error("Invalid buffering=%d", buffering);
        } else {
            VoiceManager.sampleRate = sampleRate;
            RakVoice.RVInitServer(enable, sampleRate, period, complexity, buffering, (float)minDistance, (float)maxDistance, is3D);
        }
    }

    void VoiceConnectAccept(long uuid) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%x", uuid);
        }
    }

    void InitRecDeviceForTest() {
        this.threadSafeCode(this::ResetRecSound);
    }

    void VoiceOpenChannelReply(long uuid, ByteBuffer buf) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%d", uuid);
            if (this.isServer) {
                return;
            }

            try {
                if (GameClient.client) {
                    serverVOIPEnable = buf.getInt() != 0;
                    sampleRate = buf.getInt();
                    period = buf.getInt();
                    buf.getInt();
                    buffering = buf.getInt();
                    minDistance = buf.getFloat();
                    maxDistance = buf.getFloat();
                    is3D = buf.getInt() != 0;
                } else {
                    serverVOIPEnable = RakVoice.GetServerVOIPEnable();
                    sampleRate = RakVoice.GetSampleRate();
                    period = RakVoice.GetSendFramePeriod();
                    buffering = RakVoice.GetBuffering();
                    minDistance = RakVoice.GetMinDistance();
                    maxDistance = RakVoice.GetMaxDistance();
                    is3D = RakVoice.GetIs3D();
                }
            } catch (Exception var8) {
                DebugLog.Voice.printException(var8, "RakVoice params set failed", LogSeverity.Error);
                return;
            }

            DebugLog.Voice
                .debugln(
                    "enabled=%b, sample-rate=%d, period=%d, complexity=%d, buffering=%d, is3D=%b", serverVOIPEnable, sampleRate, period, 1, buffering, is3D
                );

            try {
                this.recDevSemaphore.acquire();
            } catch (InterruptedException var7) {
                var7.printStackTrace();
            }

            int mode = is3D ? 1170 : 1154;

            for (VoiceManagerData d : VoiceManagerData.data) {
                if (d.userplaysound != 0L) {
                    javafmod.FMOD_Sound_SetMode(d.userplaysound, mode);
                }
            }

            long result = javafmod.FMOD_System_SetRawPlayBufferingPeriod(buffering);
            if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
                DebugLog.Voice.warn("FMOD_System_SetRawPlayBufferingPeriod result=%d", result);
            }

            this.ResetRecSound();
            this.recDevSemaphore.release();
        }
    }

    public void VoiceConnectReq(long uuid) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%x", uuid);
            VoiceManagerData.data.clear();
            RakVoice.RequestVoiceChannel(uuid);
        }
    }

    public void VoiceConnectClose(long uuid) {
        if (this.isEnable) {
            DebugLog.Voice.debugln("uuid=%x", uuid);
            RakVoice.CloseVoiceChannel(uuid);
        }
    }

    public void setMode(int mode) {
        if (mode == 3) {
            this.isModeVad = false;
            this.isModePpt = false;
        } else if (mode == 1) {
            this.isModeVad = false;
            this.isModePpt = true;
        } else if (mode == 2) {
            this.isModeVad = true;
            this.isModePpt = false;
        }
    }

    public void setVADMode(int mode) {
        if (!(mode < 1 | mode > 4)) {
            this.vadMode = mode;
            if (this.initialisedRecDev) {
                this.threadSafeCode(() -> javafmod.FMOD_System_SetVADMode(this.vadMode - 1));
            }
        }
    }

    public void setAGCMode(int mode) {
        if (!(mode < 1 | mode > 3)) {
            this.agcMode = mode;
            if (this.initialisedRecDev) {
                this.threadSafeCode(this::ResetRecSound);
            }
        }
    }

    public void setVolumePlayers(int volume) {
        if (!(volume < 0 | volume > 11)) {
            if (volume <= 10) {
                this.volumePlayers = volume;
            } else {
                this.volumePlayers = 12;
            }

            if (this.initialisedRecDev) {
                ArrayList<VoiceManagerData> data = VoiceManagerData.data;

                for (int i = 0; i < data.size(); i++) {
                    VoiceManagerData d = data.get(i);
                    if (d != null && d.userplaychannel != 0L) {
                        javafmod.FMOD_Channel_SetVolume(d.userplaychannel, (float)(this.volumePlayers * 0.2));
                    }
                }
            }
        }
    }

    public void setVolumeMic(int volume) {
        if (!(volume < 0 | volume > 11)) {
            if (volume <= 10) {
                this.volumeMic = volume;
            } else {
                this.volumeMic = 12;
            }

            if (this.initialisedRecDev) {
                this.threadSafeCode(() -> javafmod.FMOD_System_SetRecordVolume(1L - Math.round(Math.pow(1.4, 11 - this.volumeMic))));
            }
        }
    }

    public static void playerSetMute(String username) {
        ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();

        for (int i1 = 0; i1 < players.size(); i1++) {
            IsoPlayer player = players.get(i1);
            if (username.equals(player.username)) {
                VoiceManagerData d = VoiceManagerData.get(player.onlineId);
                d.userplaymute = !d.userplaymute;
                player.isVoiceMute = d.userplaymute;
                break;
            }
        }
    }

    public static boolean playerGetMute(String username) {
        ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();

        for (int i1 = 0; i1 < players.size(); i1++) {
            IsoPlayer player = players.get(i1);
            if (username.equals(player.username)) {
                return VoiceManagerData.get(player.onlineId).userplaymute;
            }
        }

        return true;
    }

    public void LuaRegister(Platform platform, KahluaTable environment) {
        KahluaTable table = platform.newTable();
        table.rawset("playerSetMute", new JavaFunction() {
            {
                Objects.requireNonNull(VoiceManager.this);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object arg1 = callFrame.get(1);
                VoiceManager.playerSetMute((String)arg1);
                return 1;
            }
        });
        table.rawset("playerGetMute", new JavaFunction() {
            {
                Objects.requireNonNull(VoiceManager.this);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                Object arg1 = callFrame.get(1);
                callFrame.push(VoiceManager.playerGetMute((String)arg1));
                return 1;
            }
        });
        table.rawset("RecordDevices", new JavaFunction() {
            {
                Objects.requireNonNull(VoiceManager.this);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                if (!Core.soundDisabled && !VoiceManager.voipDisabled) {
                    int num_devices = javafmod.FMOD_System_GetRecordNumDrivers();
                    KahluaTable record_devices = callFrame.getPlatform().newTable();

                    for (int i = 0; i < num_devices; i++) {
                        FMOD_DriverInfo info = new FMOD_DriverInfo();
                        javafmod.FMOD_System_GetRecordDriverInfo(i, info);
                        record_devices.rawset(i + 1, info.name);
                    }

                    callFrame.push(record_devices);
                    return 1;
                } else {
                    KahluaTable record_devices = callFrame.getPlatform().newTable();
                    callFrame.push(record_devices);
                    return 1;
                }
            }
        });
        environment.rawset("VoiceManager", table);
    }

    private void setUserPlaySound(long userPlayChannel, float volume) {
        volume = IsoUtils.clamp(volume * IsoUtils.lerp(this.volumePlayers, 0.0F, 12.0F), 0.0F, 1.0F);
        javafmod.FMOD_Channel_SetVolume(userPlayChannel, volume);
    }

    private long getUserPlaySound(short onlineId) {
        VoiceManagerData d = VoiceManagerData.get(onlineId);
        if (d.userplaychannel == 0L) {
            d.userplaysound = 0L;
            int mode = is3D ? 1170 : 1154;
            d.userplaysound = javafmod.FMOD_System_CreateRAWPlaySound(mode, 2L, sampleRate);
            if (d.userplaysound == 0L) {
                DebugLog.Voice.warn("FMOD_System_CreateSound result=%d", d.userplaysound);
            }

            d.userplaychannel = javafmod.FMOD_System_PlaySound(d.userplaysound, false);
            if (d.userplaychannel == 0L) {
                DebugLog.Voice.warn("FMOD_System_PlaySound result=%d", d.userplaychannel);
            }

            javafmod.FMOD_Channel_SetVolume(d.userplaychannel, (float)(this.volumePlayers * 0.2));
            if (is3D) {
                javafmod.FMOD_Channel_Set3DMinMaxDistance(d.userplaychannel, minDistance / 2.0F, maxDistance);
            }

            javafmod.FMOD_Channel_SetChannelGroup(d.userplaychannel, this.fmodChannelGroup);
        }

        return d.userplaysound;
    }

    public void InitVMClient() {
        if (!Core.soundDisabled && !voipDisabled) {
            int num_devices = javafmod.FMOD_System_GetRecordNumDrivers();
            this.fmodVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
            if (this.fmodVoiceRecordDriverId < 0 && num_devices > 0) {
                Core.getInstance().setOptionVoiceRecordDevice(1);
                this.fmodVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
            }

            if (num_devices < 1) {
                DebugLog.Voice.debugln("Microphone not found");
                this.initialiseRecDev = false;
            } else if (this.fmodVoiceRecordDriverId < 0 | this.fmodVoiceRecordDriverId >= num_devices) {
                DebugLog.Voice.warn("Invalid record device");
                this.initialiseRecDev = false;
            } else {
                this.initialiseRecDev = true;
            }

            this.isEnable = Core.getInstance().getOptionVoiceEnable();
            this.setMode(Core.getInstance().getOptionVoiceMode());
            this.vadMode = Core.getInstance().getOptionVoiceVADMode();
            this.volumeMic = Core.getInstance().getOptionVoiceVolumeMic();
            this.volumePlayers = Core.getInstance().getOptionVoiceVolumePlayers();
            this.fmodChannelGroup = javafmod.FMOD_System_CreateChannelGroup("VOIP");
            this.VoiceInitClient();
            this.fmodRecordSound = 0L;
            if (this.isEnable) {
                this.InitRecDeviceForTest();
            }

            this.timeLast = System.currentTimeMillis();
            this.quit = false;
            this.thread = new Thread() {
                {
                    Objects.requireNonNull(VoiceManager.this);
                }

                @Override
                public void run() {
                    while (!VoiceManager.this.quit) {
                        try {
                            VoiceManager.this.UpdateVMClient();
                            sleep(VoiceManager.period / 2);
                        } catch (Exception var2) {
                            var2.printStackTrace();
                        }
                    }
                }
            };
            this.thread.setName("VoiceManagerClient");
            this.thread.start();
        } else {
            this.isEnable = false;
            this.initialiseRecDev = false;
            this.initialisedRecDev = false;
            DebugLog.Voice.debugln("Disabled");
        }
    }

    public void loadConfig() {
        this.isEnable = Core.getInstance().getOptionVoiceEnable();
        this.setMode(Core.getInstance().getOptionVoiceMode());
        this.vadMode = Core.getInstance().getOptionVoiceVADMode();
        this.volumeMic = Core.getInstance().getOptionVoiceVolumeMic();
        this.volumePlayers = Core.getInstance().getOptionVoiceVolumePlayers();
    }

    public void UpdateRecordDevice() {
        if (this.initialisedRecDev) {
            this.threadSafeCode(this::UpdateRecordDeviceInternal);
        }
    }

    private void UpdateRecordDeviceInternal() {
        int result = javafmod.FMOD_System_RecordStop(this.fmodVoiceRecordDriverId);
        if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
            DebugLog.Voice.warn("FMOD_System_RecordStop result=%d", result);
        }

        this.fmodVoiceRecordDriverId = Core.getInstance().getOptionVoiceRecordDevice() - 1;
        if (this.fmodVoiceRecordDriverId < 0) {
            DebugLog.Voice.error("No record device found");
        } else {
            result = javafmod.FMOD_System_RecordStart(this.fmodVoiceRecordDriverId, this.fmodRecordSound, true);
            if (result != FMOD_RESULT.FMOD_OK.ordinal()) {
                DebugLog.Voice.warn("FMOD_System_RecordStart result=%d", result);
            }
        }
    }

    public void DeinitVMClient() {
        if (this.thread != null) {
            this.quit = true;
            synchronized (this.notifier) {
                this.notifier.notify();
            }

            while (this.thread.isAlive()) {
                try {
                    Thread.sleep(10L);
                } catch (InterruptedException var4) {
                }
            }

            this.thread = null;
        }

        this.DeinitRecSound();
        ArrayList<VoiceManagerData> data = VoiceManagerData.data;

        for (int i = 0; i < data.size(); i++) {
            VoiceManagerData d = data.get(i);
            if (d.userplaychannel != 0L) {
                javafmod.FMOD_Channel_Stop(d.userplaychannel);
            }

            if (d.userplaysound != 0L) {
                javafmod.FMOD_RAWPlaySound_Release(d.userplaysound);
                d.userplaysound = 0L;
            }
        }

        VoiceManagerData.data.clear();
    }

    public void setTestingMicrophone(boolean testing) {
        if (testing) {
            this.testingMicrophoneMs = System.currentTimeMillis();
        }

        if (testing != this.testingMicrophone) {
            this.testingMicrophone = testing;
            this.notifyThread();
        }
    }

    public void notifyThread() {
        synchronized (this.notifier) {
            this.notifier.notify();
        }
    }

    public void update() {
        if (!GameServer.server) {
            if (this.testingMicrophone) {
                long ms = System.currentTimeMillis();
                if (ms - this.testingMicrophoneMs > 1000L) {
                    this.setTestingMicrophone(false);
                }
            }

            if ((!GameClient.client || GameClient.connection == null) && !FakeClientManager.isVOIPEnabled()) {
                if (this.isClient) {
                    this.isClient = false;
                    this.notifyThread();
                }
            } else if (!this.isClient) {
                this.isClient = true;
                this.notifyThread();
            }
        }
    }

    private float getCanHearAllVolume(float range) {
        return range > minDistance ? IsoUtils.clamp(1.0F - IsoUtils.lerp(range, minDistance, maxDistance), 0.2F, 1.0F) : 1.0F;
    }

    private void threadSafeCode(Runnable runnable) {
        while (true) {
            try {
                this.recDevSemaphore.acquire();
            } catch (InterruptedException var7) {
                continue;
            }

            try {
                runnable.run();
            } finally {
                this.recDevSemaphore.release();
            }

            return;
        }
    }

    synchronized void UpdateVMClient() throws InterruptedException {
        while (!this.quit && !this.isClient && !this.testingMicrophone) {
            synchronized (this.notifier) {
                try {
                    this.notifier.wait();
                } catch (InterruptedException var13) {
                }
            }
        }

        if (serverVOIPEnable) {
            if (IsoPlayer.getInstance() != null) {
                IsoPlayer.getInstance().isSpeek = System.currentTimeMillis() - this.indicatorIsVoice <= 300L;
            }

            if (this.initialiseRecDev) {
                this.recDevSemaphore.acquire();
                javafmod.FMOD_System_GetRecordPosition(this.fmodVoiceRecordDriverId, this.fmodRecordPosition);
                if (fmodReceiveBuffer != null) {
                    while ((this.fmodSoundDataError = javafmod.FMOD_Sound_GetData(this.fmodRecordSound, fmodReceiveBuffer, this.fmodSoundData)) == 0) {
                        if ((IsoPlayer.getInstance() != null && GameClient.connection != null || FakeClientManager.isVOIPEnabled())
                            && (!is3D || !IsoPlayer.getInstance().isDead())) {
                            if (this.isModePpt) {
                                if (GameKeyboard.isKeyDown("Enable voice transmit")) {
                                    RakVoice.SendFrame(
                                        GameClient.connection.connectedGuid, IsoPlayer.getInstance().getOnlineID(), fmodReceiveBuffer, this.fmodSoundData.size
                                    );
                                    this.indicatorIsVoice = System.currentTimeMillis();
                                } else if (FakeClientManager.isVOIPEnabled()) {
                                    RakVoice.SendFrame(
                                        FakeClientManager.getConnectedGUID(), FakeClientManager.getOnlineID(), fmodReceiveBuffer, this.fmodSoundData.size
                                    );
                                    this.indicatorIsVoice = System.currentTimeMillis();
                                }
                            }

                            if (this.isModeVad && this.fmodSoundData.vad != 0L) {
                                RakVoice.SendFrame(
                                    GameClient.connection.connectedGuid, IsoPlayer.getInstance().getOnlineID(), fmodReceiveBuffer, this.fmodSoundData.size
                                );
                                this.indicatorIsVoice = System.currentTimeMillis();
                            }
                        }
                    }
                }

                this.recDevSemaphore.release();
            }

            ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();
            ArrayList<VoiceManagerData> data = VoiceManagerData.data;

            for (int i = 0; i < data.size(); i++) {
                VoiceManagerData d = data.get(i);
                boolean online = false;

                for (int pn = 0; pn < players.size(); pn++) {
                    IsoPlayer player = players.get(pn);
                    if (player.onlineId == d.index) {
                        online = true;
                        break;
                    }
                }

                if (false & d.index == 0) {
                    break;
                }

                if (d.userplaychannel != 0L & !online) {
                    javafmod.FMOD_Channel_Stop(d.userplaychannel);
                    d.userplaychannel = 0L;
                }
            }

            long currentTime = System.currentTimeMillis() - this.timeLast;
            if (currentTime >= period) {
                this.timeLast += currentTime;
                if (IsoPlayer.getInstance() == null) {
                    return;
                }

                VoiceManagerData.VoiceDataSource lastSource = VoiceManagerData.VoiceDataSource.Unknown;
                int lastFreq = 0;

                for (IsoPlayer player : players) {
                    IsoPlayer me = IsoPlayer.getInstance();
                    if (player != me && player.getOnlineID() != -1) {
                        VoiceManagerData d = VoiceManagerData.get(player.getOnlineID());

                        while (RakVoice.ReceiveFrame(player.getOnlineID(), this.buf)) {
                            d.voicetimeout = 10L;
                            if (!d.userplaymute) {
                                float range = IsoUtils.DistanceTo(me.getX(), me.getY(), player.getX(), player.getY());
                                if (me.canHearAll()) {
                                    javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0F);
                                    javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0F, 0.0F, 0.0F);
                                    this.setUserPlaySound(d.userplaychannel, this.getCanHearAllVolume(range));
                                    lastSource = VoiceManagerData.VoiceDataSource.Cheat;
                                    int var23 = false;
                                } else {
                                    VoiceManagerData.RadioData rdata = this.checkForNearbyRadios(d);
                                    if (rdata != null && rdata.deviceData != null) {
                                        javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0F);
                                        javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0F, 0.0F, 0.0F);
                                        this.setUserPlaySound(d.userplaychannel, rdata.deviceData.getDeviceVolume());
                                        rdata.deviceData.doReceiveMPSignal(rdata.lastReceiveDistance);
                                        lastSource = VoiceManagerData.VoiceDataSource.Radio;
                                        lastFreq = rdata.freq;
                                    } else {
                                        if (rdata == null) {
                                            javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0F);
                                            javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0F, 0.0F, 0.0F);
                                            javafmod.FMOD_Channel_SetVolume(d.userplaychannel, 0.0F);
                                            lastSource = VoiceManagerData.VoiceDataSource.Unknown;
                                        } else {
                                            if (is3D) {
                                                javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, IsoUtils.lerp(range, 0.0F, minDistance));
                                                javafmod.FMOD_Channel_Set3DAttributes(
                                                    d.userplaychannel, player.getX(), player.getY(), player.getZ(), 0.0F, 0.0F, 0.0F
                                                );
                                            } else {
                                                javafmodJNI.FMOD_Channel_Set3DLevel(d.userplaychannel, 0.0F);
                                                javafmod.FMOD_Channel_Set3DAttributes(d.userplaychannel, me.getX(), me.getY(), me.getZ(), 0.0F, 0.0F, 0.0F);
                                            }

                                            this.setUserPlaySound(d.userplaychannel, IsoUtils.smoothstep(maxDistance, minDistance, rdata.lastReceiveDistance));
                                            lastSource = VoiceManagerData.VoiceDataSource.Voice;
                                        }

                                        int var24 = false;
                                        if (range > maxDistance) {
                                            logFrame(me, player, range);
                                        }
                                    }
                                }

                                javafmod.FMOD_System_RAWPlayData(this.getUserPlaySound(player.getOnlineID()), this.buf, (long)this.buf.length);
                            }
                        }

                        if (d.voicetimeout == 0L) {
                            player.isSpeek = false;
                        } else {
                            d.voicetimeout--;
                            player.isSpeek = true;
                        }
                    }
                }
            }
        }
    }

    private static void logFrame(IsoPlayer me, IsoPlayer player, float distance) {
        long currentTime = System.currentTimeMillis();
        if (currentTime > timestamp) {
            timestamp = currentTime + 5000L;
            DebugLog.Multiplayer
                .warn(
                    String.format(
                        "\"%s\" (%b) received VOIP frame from \"%s\" (%b) at distance=%f",
                        me.getUsername(),
                        me.canHearAll(),
                        player.getUsername(),
                        player.canHearAll(),
                        distance
                    )
                );
        }
    }

    private VoiceManagerData.RadioData checkForNearbyRadios(VoiceManagerData radioData) {
        IsoPlayer me = IsoPlayer.getInstance();
        VoiceManagerData myRadioData = VoiceManagerData.get(me.onlineId);
        if (myRadioData.isCanHearAll) {
            myRadioData.radioData.get(0).lastReceiveDistance = 0.0F;
            return myRadioData.radioData.get(0);
        } else {
            synchronized (myRadioData.radioData) {
                for (int i = 1; i < myRadioData.radioData.size(); i++) {
                    synchronized (radioData.radioData) {
                        for (int j = 1; j < radioData.radioData.size(); j++) {
                            if (myRadioData.radioData.get(i).freq == radioData.radioData.get(j).freq) {
                                float dx = myRadioData.radioData.get(i).x - radioData.radioData.get(j).x;
                                float dy = myRadioData.radioData.get(i).y - radioData.radioData.get(j).y;
                                myRadioData.radioData.get(i).lastReceiveDistance = (float)Math.sqrt(dx * dx + dy * dy);
                                if (myRadioData.radioData.get(i).lastReceiveDistance < radioData.radioData.get(j).distance) {
                                    return myRadioData.radioData.get(i);
                                }
                            }
                        }
                    }
                }
            }

            synchronized (myRadioData.radioData) {
                synchronized (radioData.radioData) {
                    if (!radioData.radioData.isEmpty() && !myRadioData.radioData.isEmpty()) {
                        float dx = myRadioData.radioData.get(0).x - radioData.radioData.get(0).x;
                        float dy = myRadioData.radioData.get(0).y - radioData.radioData.get(0).y;
                        myRadioData.radioData.get(0).lastReceiveDistance = (float)Math.sqrt(dx * dx + dy * dy);
                        if (myRadioData.radioData.get(0).lastReceiveDistance < radioData.radioData.get(0).distance) {
                            return myRadioData.radioData.get(0);
                        }
                    }
                }

                return null;
            }
        }
    }

    public void UpdateChannelsRoaming(UdpConnection connection) {
        IsoPlayer me = IsoPlayer.getInstance();
        if (me.onlineId != -1) {
            VoiceManagerData myRadioData = VoiceManagerData.get(me.onlineId);
            boolean isCanHearAll = false;
            synchronized (myRadioData.radioData) {
                myRadioData.radioData.clear();
                Set<Integer> tmpDeviceIDs = new HashSet<>();

                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player != null) {
                        isCanHearAll |= player.canHearAll();
                        myRadioData.radioData.add(new VoiceManagerData.RadioData(RakVoice.GetMaxDistance(), player.getX(), player.getY()));

                        for (int j = 0; j < player.getInventory().getItems().size(); j++) {
                            InventoryItem item = player.getInventory().getItems().get(j);
                            if (item instanceof Radio radio) {
                                DeviceData deviceData = radio.getDeviceData();
                                if (deviceData != null && deviceData.getIsTurnedOn()) {
                                    myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData, player.getX(), player.getY()));
                                }
                            }
                        }

                        for (int x = (int)player.getX() - 4; x < player.getX() + 5.0F; x++) {
                            for (int y = (int)player.getY() - 4; y < player.getY() + 5.0F; y++) {
                                for (int z = player.getZi() - 1; z < player.getZi() + 1; z++) {
                                    IsoGridSquare sq = IsoCell.getInstance().getGridSquare(x, y, z);
                                    if (sq != null) {
                                        if (sq.getObjects() != null) {
                                            for (int jx = 0; jx < sq.getObjects().size(); jx++) {
                                                IsoObject item = sq.getObjects().get(jx);
                                                if (item instanceof IsoRadio isoRadio) {
                                                    DeviceData deviceData = isoRadio.getDeviceData();
                                                    if (deviceData != null && deviceData.getIsTurnedOn()) {
                                                        myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData, sq.x, sq.y));
                                                        if (!item.getModData().isEmpty()) {
                                                            Object id = item.getModData().rawget("RadioItemID");
                                                            if (id != null && id instanceof Double d) {
                                                                tmpDeviceIDs.add(d.intValue());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (sq.getWorldObjects() != null) {
                                            for (int jxx = 0; jxx < sq.getWorldObjects().size(); jxx++) {
                                                IsoWorldInventoryObject item = sq.getWorldObjects().get(jxx);
                                                if (item.getItem() != null && item.getItem() instanceof Radio && !tmpDeviceIDs.contains(item.getItem().getID())
                                                    )
                                                 {
                                                    DeviceData deviceData = ((Radio)item.getItem()).getDeviceData();
                                                    if (deviceData != null && deviceData.getIsTurnedOn()) {
                                                        myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData, sq.x, sq.y));
                                                    }
                                                }
                                            }
                                        }

                                        if (sq.getVehicleContainer() != null && sq == sq.getVehicleContainer().getSquare()) {
                                            VehiclePart part = sq.getVehicleContainer().getPartById("Radio");
                                            if (part != null) {
                                                DeviceData deviceData = part.getDeviceData();
                                                if (deviceData != null && deviceData.getIsTurnedOn()) {
                                                    myRadioData.radioData.add(new VoiceManagerData.RadioData(deviceData, sq.x, sq.y));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.SyncRadioData.doPacket(b);
            b.putByte((byte)(isCanHearAll ? 1 : 0));
            b.putInt(myRadioData.radioData.size() * 4);

            for (VoiceManagerData.RadioData data : myRadioData.radioData) {
                b.putInt(data.freq);
                b.putInt((int)data.distance);
                b.putInt(data.x);
                b.putInt(data.y);
            }

            PacketTypes.PacketType.SyncRadioData.send(connection);
        }
    }

    void InitVMServer() {
        this.VoiceInitServer(
            ServerOptions.instance.voiceEnable.getValue(),
            24000,
            20,
            5,
            8000,
            ServerOptions.instance.voiceMinDistance.getValue(),
            ServerOptions.instance.voiceMaxDistance.getValue(),
            ServerOptions.instance.voice3d.getValue()
        );
    }

    public int getMicVolumeIndicator() {
        return fmodReceiveBuffer == null ? 0 : (int)this.fmodSoundData.loudness;
    }

    public boolean getMicVolumeError() {
        return fmodReceiveBuffer == null ? true : this.fmodSoundDataError == -1;
    }

    public boolean getServerVOIPEnable() {
        return serverVOIPEnable;
    }

    public void VMServerBan(short player_id, boolean is_ban) {
        RakVoice.SetVoiceBan(player_id, is_ban);
    }
}
