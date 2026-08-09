// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.scripting;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.radio.ZomboidRadio;

/**
 * Turbo
 */
@UsedFromLua
public final class RadioScriptManager {
    private final Map<Integer, RadioChannel> channels = new LinkedHashMap<>();
    private static RadioScriptManager instance;
    private int currentTimeStamp;
    private final ArrayList<RadioChannel> channelsList = new ArrayList<>();

    public static boolean hasInstance() {
        return instance != null;
    }

    public static RadioScriptManager getInstance() {
        if (instance == null) {
            instance = new RadioScriptManager();
        }

        return instance;
    }

    private RadioScriptManager() {
    }

    public void init(int savedWorldVersion) {
    }

    public Map<Integer, RadioChannel> getChannels() {
        return this.channels;
    }

    public ArrayList<RadioChannel> getChannelsList() {
        this.channelsList.clear();

        for (Entry<Integer, RadioChannel> entry : this.channels.entrySet()) {
            this.channelsList.add(entry.getValue());
        }

        return this.channelsList;
    }

    public RadioChannel getRadioChannel(String uuid) {
        for (Entry<Integer, RadioChannel> entry : this.channels.entrySet()) {
            if (entry.getValue().getGUID().equals(uuid)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public void simulateScriptsUntil(int days, boolean force) {
        for (Entry<Integer, RadioChannel> entry : this.channels.entrySet()) {
            this.simulateChannelUntil(entry.getValue().GetFrequency(), days, force);
        }
    }

    public void simulateChannelUntil(int frequency, int days, boolean force) {
        if (this.channels.containsKey(frequency)) {
            RadioChannel chan = this.channels.get(frequency);
            if (chan.isTimeSynced() && !force) {
                return;
            }

            for (int i = 0; i < days; i++) {
                int timeStamp = i * 24 * 60;
                chan.UpdateScripts(this.currentTimeStamp, timeStamp);
            }

            chan.setTimeSynced(true);
        }
    }

    public int getCurrentTimeStamp() {
        return this.currentTimeStamp;
    }

    public void PlayerListensChannel(int chanfrequency, boolean mode, boolean sourceIsTV) {
        if (this.channels.containsKey(chanfrequency) && this.channels.get(chanfrequency).IsTv() == sourceIsTV) {
            this.channels.get(chanfrequency).SetPlayerIsListening(mode);
        }
    }

    public void AddChannel(RadioChannel channel, boolean overwrite) {
        if (channel == null || !overwrite && this.channels.containsKey(channel.GetFrequency())) {
            String channame = channel != null ? channel.GetName() : "null";
            DebugLog.log(DebugType.Radio, "Error adding radiochannel (" + channame + "), channel is null or frequency key already exists");
        } else {
            this.channels.put(channel.GetFrequency(), channel);
            String categoryName = channel.GetCategory().name();
            ZomboidRadio.getInstance().addChannelName(channel.GetName(), channel.GetFrequency(), categoryName, overwrite);
        }
    }

    public void RemoveChannel(int frequency) {
        if (this.channels.containsKey(frequency)) {
            this.channels.remove(frequency);
            ZomboidRadio.getInstance().removeChannelName(frequency);
        }
    }

    public void UpdateScripts(int day, int hour, int mins) {
        this.currentTimeStamp = day * 24 * 60 + hour * 60 + mins;

        for (Entry<Integer, RadioChannel> entry : this.channels.entrySet()) {
            entry.getValue().UpdateScripts(this.currentTimeStamp, day);
        }
    }

    public void update() {
        for (Entry<Integer, RadioChannel> entry : this.channels.entrySet()) {
            entry.getValue().update();
        }
    }

    public void reset() {
        instance = null;
    }

    public void Save(Writer w) throws IOException {
        for (Entry<Integer, RadioChannel> entry : this.channels.entrySet()) {
            RadioScript currentScript = entry.getValue().getCurrentScript();
            RadioBroadCast broadcast = entry.getValue().getAiringBroadcast();
            String bcid = broadcast != null ? broadcast.getID() : "none";
            if (broadcast == null && entry.getValue().getLastBroadcastID() != null) {
                bcid = entry.getValue().getLastBroadcastID();
            }

            w.write(
                String.join(
                    ",",
                    Integer.toString(entry.getKey()),
                    Integer.toString(entry.getValue().getCurrentScriptLoop()),
                    Integer.toString(entry.getValue().getCurrentScriptMaxLoops()),
                    currentScript == null ? "none" : currentScript.GetName(),
                    currentScript == null ? "-1" : Integer.toString(currentScript.getStartDay()),
                    bcid,
                    broadcast == null ? "-1" : Integer.toString(broadcast.getCurrentLineNumber())
                )
            );
            w.write(System.lineSeparator());
        }
    }

    public void Load(List<String> channelLines) throws IOException, NumberFormatException {
        for (String line : channelLines) {
            if (line != null && !line.isBlank()) {
                line = line.trim();
                String[] s = line.split(",");
                RadioChannel chan = this.channels.get(Integer.parseInt(s[0]));
                if (chan != null) {
                    chan.setTimeSynced(true);
                    if (s.length == 7) {
                        if (!"none".equals(s[3])) {
                            chan.setActiveScript(s[3], Integer.parseInt(s[4]), Integer.parseInt(s[1]), Integer.parseInt(s[2]));
                        }

                        if (!"none".equals(s[5])) {
                            chan.LoadAiringBroadcast(s[5], Integer.parseInt(s[6]));
                        }
                    }
                }
            }
        }
    }
}
