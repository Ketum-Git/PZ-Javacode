// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.characters.Roles;
import zombie.chat.ChatElement;
import zombie.chat.ChatMessage;
import zombie.core.Color;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.VoiceManagerData;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.inventory.types.Radio;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.radio.StorySounds.SLSoundManager;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.WaveSignalDevice;
import zombie.radio.media.RecordedMedia;
import zombie.radio.scripting.RadioChannel;
import zombie.radio.scripting.RadioScript;
import zombie.radio.scripting.RadioScriptManager;

@UsedFromLua
public final class ZomboidRadio {
    public static final String SAVE_FILE = "RADIO_SAVE.txt";
    private final ArrayList<WaveSignalDevice> devices = new ArrayList<>();
    private final ArrayList<WaveSignalDevice> broadcastDevices = new ArrayList<>();
    private RadioScriptManager scriptManager;
    private int daysSinceStart;
    private int lastRecordedHour;
    private final String[] playerLastLine = new String[4];
    private final Map<Integer, String> channelNames = new HashMap<>();
    private final Map<String, Map<Integer, String>> categorizedChannels = new HashMap<>();
    private final List<Integer> knownFrequencies = new ArrayList<>();
    private RadioDebugConsole debugConsole;
    private boolean hasRecievedServerData;
    private final SLSoundManager storySoundManager = null;
    private static final String[] staticSounds = new String[]{"<bzzt>", "<fzzt>", "<wzzt>", "<szzt>"};
    public static final boolean DEBUG_MODE = false;
    public static final boolean DEBUG_XML = false;
    public static final boolean DEBUG_SOUND = false;
    public static boolean postRadioSilence;
    public static boolean disableBroadcasting;
    private static ZomboidRadio instance;
    private static RecordedMedia recordedMedia;
    public static boolean louisvilleObfuscation;
    private String lastSaveFile;
    private String lastSaveContent;
    private final HashMap<Integer, ZomboidRadio.FreqListEntry> freqlist = new HashMap<>();
    private boolean hasAppliedRangeDistortion;
    private final StringBuilder stringBuilder = new StringBuilder();
    private boolean hasAppliedInterference;
    private static final int[] obfuscateChannels = new int[]{200, 201, 204, 93200, 98000, 101200};

    public static boolean hasInstance() {
        return instance != null;
    }

    public static ZomboidRadio getInstance() {
        if (instance == null) {
            instance = new ZomboidRadio();
        }

        return instance;
    }

    private ZomboidRadio() {
        this.lastRecordedHour = GameTime.instance.getHour();
        SLSoundManager.debug = false;

        for (int i = 0; i < staticSounds.length; i++) {
            ChatElement.addNoLogText(staticSounds[i]);
        }

        ChatElement.addNoLogText("~");
        recordedMedia = new RecordedMedia();
    }

    public static boolean isStaticSound(String str) {
        if (str != null) {
            for (String s : staticSounds) {
                if (str.equals(s)) {
                    return true;
                }
            }
        }

        return false;
    }

    public RadioScriptManager getScriptManager() {
        return this.scriptManager;
    }

    public int getDaysSinceStart() {
        return this.daysSinceStart;
    }

    public ArrayList<WaveSignalDevice> getDevices() {
        return this.devices;
    }

    public ArrayList<WaveSignalDevice> getBroadcastDevices() {
        return this.broadcastDevices;
    }

    public void setHasRecievedServerData(boolean state) {
        this.hasRecievedServerData = state;
    }

    public void addChannelName(String name, int frequency, String category) {
        this.addChannelName(name, frequency, category, true);
    }

    public void addChannelName(String name, int frequency, String category, boolean overwrite) {
        if (overwrite || !this.channelNames.containsKey(frequency)) {
            if (!this.categorizedChannels.containsKey(category)) {
                this.categorizedChannels.put(category, new HashMap<>());
            }

            this.categorizedChannels.get(category).put(frequency, name);
            this.channelNames.put(frequency, name);
            this.knownFrequencies.add(frequency);
        }
    }

    public void removeChannelName(int frequency) {
        if (this.channelNames.containsKey(frequency)) {
            this.channelNames.remove(frequency);

            for (Entry<String, Map<Integer, String>> Entry : this.categorizedChannels.entrySet()) {
                if (Entry.getValue().containsKey(frequency)) {
                    Entry.getValue().remove(frequency);
                }
            }
        }
    }

    public Map<Integer, String> GetChannelList(String category) {
        return this.categorizedChannels.containsKey(category) ? this.categorizedChannels.get(category) : null;
    }

    public String getChannelName(int frequency) {
        return this.channelNames.containsKey(frequency) ? this.channelNames.get(frequency) : null;
    }

    public int getRandomFrequency() {
        return this.getRandomFrequency(88000, 108000);
    }

    public int getRandomFrequency(int rangemin, int rangemax) {
        int freq = 91100;

        do {
            freq = Rand.Next(rangemin, rangemax);
            freq /= 200;
            freq *= 200;
        } while (this.knownFrequencies.contains(freq));

        return freq;
    }

    public Map<String, Map<Integer, String>> getFullChannelList() {
        return this.categorizedChannels;
    }

    public void WriteRadioServerDataPacket(ByteBufferWriter bb) {
        bb.putInt(this.categorizedChannels.size());

        for (Entry<String, Map<Integer, String>> Entry : this.categorizedChannels.entrySet()) {
            GameWindow.WriteString(bb.bb, Entry.getKey());
            bb.putInt(Entry.getValue().size());

            for (Entry<Integer, String> Entry2 : Entry.getValue().entrySet()) {
                bb.putInt(Entry2.getKey());
                GameWindow.WriteString(bb.bb, Entry2.getValue());
            }
        }

        bb.putByte((byte)(postRadioSilence ? 1 : 0));
    }

    public void Init(int savedWorldVersion) {
        postRadioSilence = false;
        boolean success = false;
        boolean bDebugEnabled = DebugLog.isEnabled(DebugType.Radio);
        if (bDebugEnabled) {
            DebugLog.Radio.println();
            DebugLog.Radio.println("################## Radio Init ##################");
        }

        RadioAPI.getInstance();
        recordedMedia.init();
        this.lastRecordedHour = GameTime.instance.getHour();
        GameMode mode = this.getGameMode();
        if (mode == GameMode.Client) {
            GameClient.sendRadioServerDataRequest();
            if (bDebugEnabled) {
                DebugLog.Radio.println("Radio (Client) loaded.");
                DebugLog.Radio.println("################################################");
            }

            this.scriptManager = null;
        } else {
            this.scriptManager = RadioScriptManager.getInstance();
            this.scriptManager.init(savedWorldVersion);

            try {
                if (!Core.getInstance().isNoSave()) {
                    ZomboidFileSystem.instance.getFileInCurrentSave("radio", "data").mkdirs();
                }

                for (RadioData radioData : RadioData.fetchAllRadioData()) {
                    for (RadioChannel channel : radioData.getRadioChannels()) {
                        ObfuscateChannelCheck(channel);
                        RadioChannel found = null;
                        if (this.scriptManager.getChannels().containsKey(channel.GetFrequency())) {
                            found = this.scriptManager.getChannels().get(channel.GetFrequency());
                        }

                        if (found != null && (!found.getRadioData().isVanilla() || channel.getRadioData().isVanilla())) {
                            if (bDebugEnabled) {
                                DebugLog.Radio.println("Unable to add channel: " + channel.GetName() + ", frequency '" + channel.GetFrequency() + "' taken.");
                            }
                        } else {
                            this.scriptManager.AddChannel(channel, true);
                        }
                    }
                }

                LuaEventManager.triggerEvent("OnLoadRadioScripts", this.scriptManager, savedWorldVersion == -1);
                if (savedWorldVersion == -1) {
                    if (bDebugEnabled) {
                        DebugLog.Radio.println("Radio setting new game start times");
                    }

                    SandboxOptions options = SandboxOptions.instance;
                    int months = options.timeSinceApo.getValue() - 1;
                    if (months < 0) {
                        months = 0;
                    }

                    if (bDebugEnabled) {
                        DebugLog.log(DebugType.Radio, "Time since the apocalypse: " + options.timeSinceApo.getValue());
                    }

                    if (months > 0) {
                        this.daysSinceStart = (int)(months * 30.5F);
                        if (bDebugEnabled) {
                            DebugLog.Radio.println("Time since the apocalypse in days: " + this.daysSinceStart);
                        }

                        this.scriptManager.simulateScriptsUntil(this.daysSinceStart, true);
                    }

                    this.checkGameModeSpecificStart();
                } else {
                    boolean isLoaded = this.Load();
                    if (!isLoaded) {
                        SandboxOptions optionsx = SandboxOptions.instance;
                        int monthsx = optionsx.timeSinceApo.getValue() - 1;
                        if (monthsx < 0) {
                            monthsx = 0;
                        }

                        this.daysSinceStart = (int)(monthsx * 30.5F);
                        this.daysSinceStart = this.daysSinceStart + GameTime.instance.getNightsSurvived();
                    }

                    if (this.daysSinceStart > 0) {
                        this.scriptManager.simulateScriptsUntil(this.daysSinceStart, false);
                    }
                }

                success = true;
            } catch (Exception var11) {
                ExceptionLogger.logException(var11);
            }

            if (bDebugEnabled) {
                if (success) {
                    DebugLog.Radio.println("Radio loaded.");
                }

                DebugLog.Radio.println("################################################");
                DebugLog.Radio.println();
            }
        }
    }

    private void checkGameModeSpecificStart() {
        if (Core.gameMode.equals("Initial Infection")) {
            for (Entry<Integer, RadioChannel> entry : this.scriptManager.getChannels().entrySet()) {
                RadioScript initInfectionScript = entry.getValue().getRadioScript("init_infection");
                if (initInfectionScript != null) {
                    initInfectionScript.clearExitOptions();
                    initInfectionScript.AddExitOption(entry.getValue().getCurrentScript().GetName(), 100, 0);
                    entry.getValue().setActiveScript("init_infection", this.daysSinceStart);
                } else {
                    entry.getValue().getCurrentScript().setStartDayStamp(this.daysSinceStart + 1);
                }
            }
        } else if (Core.gameMode.equals("Six Months Later")) {
            for (Entry<Integer, RadioChannel> entryx : this.scriptManager.getChannels().entrySet()) {
                if (entryx.getValue().GetName().equals("Classified M1A1")) {
                    entryx.getValue().setActiveScript("numbers", this.daysSinceStart);
                } else if (entryx.getValue().GetName().equals("NNR Radio")) {
                    entryx.getValue().setActiveScript("pastor", this.daysSinceStart);
                }
            }
        }
    }

    public void Save() throws FileNotFoundException, IOException {
        if (!Core.getInstance().isNoSave()) {
            GameMode mode = this.getGameMode();
            if ((mode == GameMode.Server || mode == GameMode.SinglePlayer) && this.scriptManager != null) {
                File path = ZomboidFileSystem.instance.getFileInCurrentSave("radio", "data");
                if (path.exists() && path.isDirectory()) {
                    String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("radio", "data", "RADIO_SAVE.txt");

                    String content;
                    try (StringWriter w = new StringWriter(1024)) {
                        w.write("DaysSinceStart = " + this.daysSinceStart + System.lineSeparator());
                        w.write("LvObfuscation = " + louisvilleObfuscation + System.lineSeparator());
                        this.scriptManager.Save(w);
                        content = w.toString();
                    } catch (IOException var15) {
                        ExceptionLogger.logException(var15);
                        return;
                    }

                    if (fileName.equals(this.lastSaveFile) && content.equals(this.lastSaveContent)) {
                        return;
                    }

                    this.lastSaveFile = fileName;
                    this.lastSaveContent = content;
                    File f = new File(fileName);
                    if (DebugLog.isEnabled(DebugType.Radio)) {
                        DebugLog.Radio.println("Saving radio: " + fileName);
                    }

                    try (FileWriter w = new FileWriter(f, false)) {
                        w.write(content);
                    } catch (Exception var13) {
                        ExceptionLogger.logException(var13);
                    }
                }
            }

            if (recordedMedia != null) {
                try {
                    recordedMedia.save();
                } catch (Exception var10) {
                    var10.printStackTrace();
                }
            }
        }
    }

    public boolean Load() throws FileNotFoundException, IOException {
        boolean result = false;
        GameMode mode = this.getGameMode();
        if (mode == GameMode.Server || mode == GameMode.SinglePlayer) {
            for (Entry<Integer, RadioChannel> entry : this.scriptManager.getChannels().entrySet()) {
                entry.getValue().setActiveScriptNull();
            }

            List<String> channelLines = new ArrayList<>();
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("radio", "data", "RADIO_SAVE.txt");
            File file = new File(fileName);
            if (!file.exists()) {
                return false;
            } else {
                if (DebugLog.isEnabled(DebugType.Radio)) {
                    DebugLog.log(DebugType.Radio, "Loading radio save:" + fileName);
                }

                String line;
                try (
                    FileReader fr = new FileReader(file);
                    BufferedReader r = new BufferedReader(fr);
                ) {
                    while ((line = r.readLine()) != null) {
                        line = line.trim();
                        if (!line.startsWith("DaysSinceStart") && !line.startsWith("LvObfuscation")) {
                            channelLines.add(line);
                        } else {
                            if (line.startsWith("DaysSinceStart")) {
                                String[] s = line.split("=");
                                this.daysSinceStart = Integer.parseInt(s[1].trim());
                            }

                            if (line.startsWith("LvObfuscation")) {
                                String[] s = line.split("=");
                                louisvilleObfuscation = Boolean.parseBoolean(s[1].trim());
                            }
                        }
                    }
                } catch (Exception var24) {
                    var24.printStackTrace();
                    return false;
                }

                boolean var28;
                try {
                    DebugLog.log("Radio Loading channels...");
                    this.scriptManager.Load(channelLines);
                    return result;
                } catch (Exception var20) {
                    var20.printStackTrace();
                    var28 = false;
                } finally {
                    result = true;
                }

                return var28;
            }
        } else {
            return result;
        }
    }

    public void Reset() {
        instance = null;
        if (this.scriptManager != null) {
            this.scriptManager.reset();
        }
    }

    public void UpdateScripts(int hour, int mins) {
        GameMode mode = this.getGameMode();
        if (mode == GameMode.Server || mode == GameMode.SinglePlayer) {
            if (hour == 0 && this.lastRecordedHour != 0) {
                this.daysSinceStart++;
            }

            this.lastRecordedHour = hour;
            if (this.scriptManager != null) {
                this.scriptManager.UpdateScripts(this.daysSinceStart, hour, mins);
            }

            try {
                this.Save();
            } catch (Exception var6) {
                System.out.println(var6.getMessage());
            }
        }

        if (mode == GameMode.Client || mode == GameMode.SinglePlayer) {
            for (int i = 0; i < this.devices.size(); i++) {
                WaveSignalDevice device = this.devices.get(i);
                if (device.getDeviceData().getIsTurnedOn() && device.HasPlayerInRange()) {
                    device.getDeviceData().TriggerPlayerListening(true);
                }
            }
        }

        if (mode == GameMode.Client && !this.hasRecievedServerData) {
            GameClient.sendRadioServerDataRequest();
        }
    }

    public void render() {
        GameMode mode = this.getGameMode();
        if (mode != GameMode.Server && this.storySoundManager != null) {
            this.storySoundManager.render();
        }
    }

    private void addFrequencyListEntry(boolean isinvitem, DeviceData devicedata, int x, int y) {
        if (devicedata != null) {
            if (!this.freqlist.containsKey(devicedata.getChannel())) {
                this.freqlist.put(devicedata.getChannel(), new ZomboidRadio.FreqListEntry(isinvitem, devicedata, x, y));
            } else if (this.freqlist.get(devicedata.getChannel()).deviceData.getTransmitRange() < devicedata.getTransmitRange()) {
                ZomboidRadio.FreqListEntry fe = this.freqlist.get(devicedata.getChannel());
                fe.isInvItem = isinvitem;
                fe.deviceData = devicedata;
                fe.sourceX = x;
                fe.sourceY = y;
            }
        }
    }

    public void update() {
        this.LouisvilleObfuscationCheck();
        GameMode mode = this.getGameMode();
        if ((mode == GameMode.Server || mode == GameMode.SinglePlayer) && this.daysSinceStart > 14 && !postRadioSilence) {
            postRadioSilence = true;
            if (GameServer.server) {
                GameServer.sendRadioPostSilence();
            }
        }

        if (mode != GameMode.Server && this.storySoundManager != null) {
            this.storySoundManager.update(this.daysSinceStart, GameTime.instance.getHour(), GameTime.instance.getMinutes());
        }

        if ((mode == GameMode.Server || mode == GameMode.SinglePlayer) && this.scriptManager != null) {
            this.scriptManager.update();
        }

        if (mode == GameMode.SinglePlayer || mode == GameMode.Client) {
            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null
                    && player.getLastSpokenLine() != null
                    && (this.playerLastLine[i] == null || !this.playerLastLine[i].equals(player.getLastSpokenLine()))) {
                    String lastChatMessage = player.getLastSpokenLine();
                    this.playerLastLine[i] = lastChatMessage;
                    if (mode != GameMode.Client
                        || (
                                player.role != Roles.getDefaultForAdmin()
                                        && player.role != Roles.getDefaultForGM()
                                        && player.role != Roles.getDefaultForModerator()
                                    || !ServerOptions.instance.disableRadioStaff.getValue()
                                        && (!ServerOptions.instance.disableRadioAdmin.getValue() || player.role != Roles.getDefaultForAdmin())
                                        && (!ServerOptions.instance.disableRadioGm.getValue() || player.role != Roles.getDefaultForGM())
                                        && (!ServerOptions.instance.disableRadioOverseer.getValue() || player.role != Roles.getDefaultForOverseer())
                                        && (!ServerOptions.instance.disableRadioModerator.getValue() || player.role != Roles.getDefaultForModerator())
                            )
                            && (!ServerOptions.instance.disableRadioInvisible.getValue() || !player.isInvisible())) {
                        this.freqlist.clear();
                        if (!GameClient.client && !GameServer.server) {
                            for (int index = 0; index < IsoPlayer.numPlayers; index++) {
                                this.checkPlayerForDevice(IsoPlayer.players[index], player);
                            }
                        } else if (GameClient.client) {
                            ArrayList<IsoPlayer> players = GameClient.instance.getPlayers();

                            for (int j = 0; j < players.size(); j++) {
                                this.checkPlayerForDevice(players.get(j), player);
                            }
                        }

                        for (WaveSignalDevice device : this.broadcastDevices) {
                            if (device != null
                                && device.getDeviceData() != null
                                && device.getDeviceData().getIsTurnedOn()
                                && device.getDeviceData().getIsTwoWay()
                                && device.HasPlayerInRange()
                                && !device.getDeviceData().getMicIsMuted()
                                && this.GetDistance(
                                        PZMath.fastfloor(player.getX()),
                                        PZMath.fastfloor(player.getY()),
                                        PZMath.fastfloor(device.getX()),
                                        PZMath.fastfloor(device.getY())
                                    )
                                    < device.getDeviceData().getMicRange()) {
                                this.addFrequencyListEntry(true, device.getDeviceData(), PZMath.fastfloor(device.getX()), PZMath.fastfloor(device.getY()));
                            }
                        }

                        if (!this.freqlist.isEmpty()) {
                            Color col = player.getSpeakColour();

                            for (Entry<Integer, ZomboidRadio.FreqListEntry> entry : this.freqlist.entrySet()) {
                                ZomboidRadio.FreqListEntry d = entry.getValue();
                                this.SendTransmission(
                                    d.sourceX,
                                    d.sourceY,
                                    entry.getKey(),
                                    this.playerLastLine[i],
                                    null,
                                    null,
                                    col.r,
                                    col.g,
                                    col.b,
                                    d.deviceData.getTransmitRange(),
                                    false
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private void checkPlayerForDevice(IsoPlayer plr, IsoPlayer selfPlayer) {
        boolean playerIsSelf = plr == selfPlayer;
        if (plr != null) {
            Radio radio = plr.getEquipedRadio();
            if (radio != null
                && radio.getDeviceData() != null
                && radio.getDeviceData().getIsPortable()
                && radio.getDeviceData().getIsTwoWay()
                && radio.getDeviceData().getIsTurnedOn()
                && !radio.getDeviceData().getMicIsMuted()
                && (
                    playerIsSelf
                        || this.GetDistance(
                                PZMath.fastfloor(selfPlayer.getX()),
                                PZMath.fastfloor(selfPlayer.getY()),
                                PZMath.fastfloor(plr.getX()),
                                PZMath.fastfloor(plr.getY())
                            )
                            < radio.getDeviceData().getMicRange()
                )) {
                this.addFrequencyListEntry(true, radio.getDeviceData(), PZMath.fastfloor(plr.getX()), PZMath.fastfloor(plr.getY()));
            }
        }
    }

    private boolean DeviceInRange(int dx, int dy, int sx, int sy, int ss) {
        return dx > sx - ss && dx < sx + ss && dy > sy - ss && dy < sy + ss && Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0)) < ss;
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    private void DistributeToPlayerOnClient(
        IsoPlayer player,
        int sourceX,
        int sourceY,
        int channel,
        String msg,
        String guid,
        String codes,
        float r,
        float g,
        float b,
        int signalStrength,
        boolean isTV
    ) {
        if (player != null && player.getOnlineID() != -1) {
            VoiceManagerData myRadioData = VoiceManagerData.get(player.getOnlineID());
            synchronized (myRadioData.radioData) {
                for (VoiceManagerData.RadioData radio : myRadioData.radioData) {
                    if (radio.isReceivingAvailable(channel)) {
                        this.DistributeToPlayerInternal(radio.getDeviceData().getParent(), player, sourceX, sourceY, msg, guid, codes, r, g, b, signalStrength);
                    }
                }
            }
        }
    }

    private void DistributeToPlayer(
        IsoPlayer player,
        int sourceX,
        int sourceY,
        int channel,
        String msg,
        String guid,
        String codes,
        float r,
        float g,
        float b,
        int signalStrength,
        boolean isTV
    ) {
        if (player != null) {
            Radio radio = player.getEquipedRadio();
            if (radio != null
                && radio.getDeviceData() != null
                && radio.getDeviceData().getIsPortable()
                && radio.getDeviceData().getIsTurnedOn()
                && radio.getDeviceData().getChannel() == channel) {
                if (radio.getDeviceData().getDeviceVolume() <= 0.0F) {
                    return;
                }

                if (radio.getDeviceData().isPlayingMedia() || radio.getDeviceData().isNoTransmit()) {
                    return;
                }

                this.DistributeToPlayerInternal(radio, player, sourceX, sourceY, msg, guid, codes, r, g, b, signalStrength);
            }
        }
    }

    private void DistributeToPlayerInternal(
        WaveSignalDevice radio,
        IsoPlayer player,
        int sourceX,
        int sourceY,
        String msg,
        String guid,
        String codes,
        float r,
        float g,
        float b,
        int signalStrength
    ) {
        boolean pass = false;
        int dist = -1;
        if (signalStrength < 0) {
            pass = true;
        } else {
            dist = this.GetDistance((int)player.getX(), (int)player.getY(), sourceX, sourceY);
            if (dist > 3 && dist < signalStrength) {
                pass = true;
            }
        }

        if (pass) {
            if (signalStrength > 0) {
                this.hasAppliedRangeDistortion = false;
                msg = this.doDeviceRangeDistortion(msg, signalStrength, dist);
            }

            if (!this.hasAppliedRangeDistortion) {
                radio.AddDeviceText(player, msg, r, g, b, guid, codes, dist);
            } else {
                radio.AddDeviceText(msg, 0.5F, 0.5F, 0.5F, guid, codes, dist);
            }
        }
    }

    public void DistributeTransmission(
        int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV
    ) {
        if (!isTV) {
            if (!GameClient.client && !GameServer.server) {
                for (int index = 0; index < IsoPlayer.numPlayers; index++) {
                    this.DistributeToPlayer(IsoPlayer.players[index], sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
                }
            } else if (GameClient.client) {
                for (IsoPlayer player : IsoPlayer.players) {
                    this.DistributeToPlayerOnClient(player, sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
                }

                return;
            }
        }

        if (!this.devices.isEmpty()) {
            for (int i = 0; i < this.devices.size(); i++) {
                WaveSignalDevice device = this.devices.get(i);
                if (device != null
                    && device.getDeviceData() != null
                    && device.getDeviceData().getIsTurnedOn()
                    && isTV == device.getDeviceData().getIsTelevision()) {
                    if (device.getDeviceData().isPlayingMedia() || device.getDeviceData().isNoTransmit()) {
                        return;
                    }

                    if (channel == device.getDeviceData().getChannel()) {
                        boolean pass = false;
                        if (signalStrength == -1) {
                            pass = true;
                        } else if (sourceX != PZMath.fastfloor(device.getX()) && sourceY != PZMath.fastfloor(device.getY())) {
                            pass = true;
                        }

                        if (pass) {
                            int dist = -1;
                            if (signalStrength > 0) {
                                this.hasAppliedRangeDistortion = false;
                                dist = this.GetDistance(PZMath.fastfloor(device.getX()), PZMath.fastfloor(device.getY()), sourceX, sourceY);
                                msg = this.doDeviceRangeDistortion(msg, signalStrength, dist);
                            }

                            if (!this.hasAppliedRangeDistortion) {
                                if (GameServer.server) {
                                    if (device.getDeviceData().getDeviceVolume() > 0.0F && codes != null) {
                                        LuaEventManager.triggerEvent("OnDeviceText", guid, codes, device.getX(), device.getY(), device.getZ(), msg, device);
                                    }
                                } else {
                                    device.AddDeviceText(msg, r, g, b, guid, codes, dist);
                                }
                            } else if (GameServer.server) {
                                if (device.getDeviceData().getDeviceVolume() > 0.0F && codes != null) {
                                    LuaEventManager.triggerEvent("OnDeviceText", guid, codes, device.getX(), device.getY(), device.getZ(), msg, device);
                                }
                            } else {
                                device.AddDeviceText(msg, 0.5F, 0.5F, 0.5F, guid, codes, dist);
                            }
                        }
                    }
                }
            }
        }
    }

    private String doDeviceRangeDistortion(String msg, int signalStrength, int dist) {
        float distortRange = signalStrength * 0.9F;
        if (distortRange < signalStrength && dist > distortRange) {
            float scambleIntensity = 100.0F * ((dist - distortRange) / (signalStrength - distortRange));
            msg = this.scrambleString(msg, (int)scambleIntensity, false);
            this.hasAppliedRangeDistortion = true;
        }

        return msg;
    }

    public GameMode getGameMode() {
        if (!GameClient.client && !GameServer.server) {
            return GameMode.SinglePlayer;
        } else {
            return GameServer.server ? GameMode.Server : GameMode.Client;
        }
    }

    public String getRandomBzztFzzt() {
        int r = Rand.Next(staticSounds.length);
        return staticSounds[r];
    }

    private String applyWeatherInterference(String msg, int signalStrength) {
        if (ClimateManager.getInstance().getWeatherInterference() <= 0.0F) {
            return msg;
        } else {
            int intensity = (int)(ClimateManager.getInstance().getWeatherInterference() * 100.0F);
            return this.scrambleString(msg, intensity, signalStrength == -1);
        }
    }

    private String scrambleString(String msg, int intensity, boolean ignoreBBcode) {
        return this.scrambleString(msg, intensity, ignoreBBcode, null);
    }

    public String scrambleString(String msg, int intensity, boolean ignoreBBcode, String customScramble) {
        this.hasAppliedInterference = false;
        StringBuilder newMsg = this.stringBuilder;
        newMsg.setLength(0);
        if (intensity <= 0) {
            return msg;
        } else if (intensity >= 100) {
            return customScramble != null ? customScramble : this.getRandomBzztFzzt();
        } else {
            this.hasAppliedInterference = true;
            if (ignoreBBcode) {
                char[] chars = msg.toCharArray();
                boolean scrmbl = false;
                boolean hasOpened = false;
                String word = "";

                for (int i = 0; i < chars.length; i++) {
                    char c = chars[i];
                    if (hasOpened) {
                        word = word + c;
                        if (c == ']') {
                            newMsg.append(word);
                            word = "";
                            hasOpened = false;
                        }
                    } else if (c == '[' || Character.isWhitespace(c) && i > 0 && !Character.isWhitespace(chars[i - 1])) {
                        int r = Rand.Next(100);
                        if (r > intensity) {
                            newMsg.append(word).append(" ");
                            scrmbl = false;
                        } else if (!scrmbl) {
                            newMsg.append(customScramble != null ? customScramble : this.getRandomBzztFzzt()).append(" ");
                            scrmbl = true;
                        }

                        if (c == '[') {
                            word = "[";
                            hasOpened = true;
                        } else {
                            word = "";
                        }
                    } else {
                        word = word + c;
                    }
                }

                if (word != null && !word.isEmpty()) {
                    newMsg.append(word);
                }
            } else {
                boolean scrmbl = false;
                String[] words = msg.split("\\s+");

                for (int ix = 0; ix < words.length; ix++) {
                    String word = words[ix];
                    int rx = Rand.Next(100);
                    if (rx > intensity) {
                        newMsg.append(word).append(" ");
                        scrmbl = false;
                    } else if (!scrmbl) {
                        newMsg.append(customScramble != null ? customScramble : this.getRandomBzztFzzt()).append(" ");
                        scrmbl = true;
                    }
                }
            }

            return newMsg.toString();
        }
    }

    public void SendTransmission(int sourceX, int sourceY, ChatMessage msg, int signalStrength) {
        Color color = msg.getTextColor();
        int channel = msg.getRadioChannel();
        this.SendTransmission(sourceX, sourceY, channel, msg.getText(), null, null, color.r, color.g, color.b, signalStrength, false);
    }

    public void SendTransmission(
        int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV
    ) {
        this.SendTransmission(-1L, sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
    }

    public void SendTransmission(
        long source, int sourceX, int sourceY, int channel, String msg, String guid, String codes, float r, float g, float b, int signalStrength, boolean isTV
    ) {
        GameMode mode = this.getGameMode();
        if (!isTV && (mode == GameMode.Server || mode == GameMode.SinglePlayer)) {
            this.hasAppliedInterference = false;
            msg = this.applyWeatherInterference(msg, signalStrength);
            if (this.hasAppliedInterference) {
                r = 0.5F;
                g = 0.5F;
                b = 0.5F;
                codes = "";
            }
        }

        if (mode == GameMode.SinglePlayer) {
            this.DistributeTransmission(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        } else if (mode == GameMode.Server) {
            this.DistributeTransmission(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
            GameServer.sendIsoWaveSignal(source, sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        } else if (mode == GameMode.Client) {
            GameClient.sendIsoWaveSignal(sourceX, sourceY, channel, msg, guid, codes, r, g, b, signalStrength, isTV);
        }
    }

    public void PlayerListensChannel(int channel, boolean listenmode, boolean isTV) {
        GameMode mode = this.getGameMode();
        if (mode != GameMode.SinglePlayer && mode != GameMode.Server) {
            if (mode == GameMode.Client) {
                GameClient.sendPlayerListensChannel(channel, listenmode, isTV);
            }
        } else if (this.scriptManager != null) {
            this.scriptManager.PlayerListensChannel(channel, listenmode, isTV);
        }
    }

    public void RegisterDevice(WaveSignalDevice device) {
        if (device != null) {
            if (!this.devices.contains(device)) {
                this.devices.add(device);
            }

            if (!GameServer.server && device.getDeviceData().getIsTwoWay() && !this.broadcastDevices.contains(device)) {
                this.broadcastDevices.add(device);
            }
        }
    }

    public void UnRegisterDevice(WaveSignalDevice device) {
        if (device != null) {
            if (this.devices.contains(device)) {
                this.devices.remove(device);
            }

            if (!GameServer.server && device.getDeviceData().getIsTwoWay() && this.broadcastDevices.contains(device)) {
                this.broadcastDevices.remove(device);
            }
        }
    }

    @Override
    public Object clone() {
        return null;
    }

    public String computerize(String str) {
        StringBuilder sb = this.stringBuilder;
        sb.setLength(0);

        for (char c : str.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Rand.NextBool(2) ? Character.toLowerCase(c) : Character.toUpperCase(c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public RecordedMedia getRecordedMedia() {
        return recordedMedia;
    }

    public void setDisableBroadcasting(boolean b) {
        disableBroadcasting = b;
    }

    public boolean getDisableBroadcasting() {
        return disableBroadcasting;
    }

    public void setDisableMediaLineLearning(boolean b) {
        RecordedMedia.disableLineLearning = b;
    }

    public boolean getDisableMediaLineLearning() {
        return RecordedMedia.disableLineLearning;
    }

    private void LouisvilleObfuscationCheck() {
        if (!GameClient.client && !GameServer.server) {
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null && player.getY() < 3550.0F) {
                louisvilleObfuscation = true;
            }
        }
    }

    public static void ObfuscateChannelCheck(RadioChannel channel) {
        if (channel.isVanilla()) {
            int freq = channel.GetFrequency();

            for (int i = 0; i < obfuscateChannels.length; i++) {
                if (freq == obfuscateChannels[i]) {
                    channel.setLouisvilleObfuscate(true);
                }
            }
        }
    }

    private static final class FreqListEntry {
        public boolean isInvItem;
        public DeviceData deviceData;
        public int sourceX;
        public int sourceY;

        public FreqListEntry(boolean isinvitem, DeviceData devicedata, int x, int y) {
            this.isInvItem = isinvitem;
            this.deviceData = devicedata;
            this.sourceX = x;
            this.sourceY = y;
        }
    }
}
