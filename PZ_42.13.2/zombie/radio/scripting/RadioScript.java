// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.scripting;

import java.util.ArrayList;
import java.util.UUID;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;

/**
 * Turbo
 */
@UsedFromLua
public final class RadioScript {
    private final ArrayList<RadioBroadCast> broadcasts = new ArrayList<>();
    private final ArrayList<RadioScript.ExitOption> exitOptions = new ArrayList<>();
    private final String guid;
    private String name = "Unnamed radioscript";
    private int startDay;
    private int startDayStamp;
    private int loopMin = 1;
    private int loopMax = 1;
    private int internalStamp;
    private RadioBroadCast currentBroadcast;
    private boolean currentHasAired;

    public RadioScript(String n, int loopmin, int loopmax) {
        this(n, loopmin, loopmax, UUID.randomUUID().toString());
    }

    public RadioScript(String n, int loopmin, int loopmax, String guid) {
        this.name = n;
        this.loopMin = loopmin;
        this.loopMax = loopmax;
        this.guid = guid;
    }

    public String GetGUID() {
        return this.guid;
    }

    public String GetName() {
        return this.name;
    }

    public int getStartDayStamp() {
        return this.startDayStamp;
    }

    public int getStartDay() {
        return this.startDay;
    }

    public int getLoopMin() {
        return this.loopMin;
    }

    public int getLoopMax() {
        return this.loopMax;
    }

    public RadioBroadCast getCurrentBroadcast() {
        return this.currentBroadcast;
    }

    public ArrayList<RadioBroadCast> getBroadcastList() {
        return this.broadcasts;
    }

    public void clearExitOptions() {
        this.exitOptions.clear();
    }

    public void setStartDayStamp(int day) {
        this.startDay = day;
        this.startDayStamp = day * 24 * 60;
    }

    public RadioBroadCast getValidAirBroadcast() {
        if (!this.currentHasAired
            && this.currentBroadcast != null
            && this.internalStamp >= this.currentBroadcast.getStartStamp()
            && this.internalStamp < this.currentBroadcast.getEndStamp()) {
            this.currentHasAired = true;
            return this.currentBroadcast;
        } else {
            return null;
        }
    }

    public void Reset() {
        this.currentBroadcast = null;
        this.currentHasAired = false;
    }

    private RadioBroadCast getNextBroadcast() {
        if (this.currentBroadcast != null && this.currentBroadcast.getEndStamp() > this.internalStamp) {
            return this.currentBroadcast;
        } else {
            for (int i = 0; i < this.broadcasts.size(); i++) {
                RadioBroadCast broadCast = this.broadcasts.get(i);
                if (broadCast.getEndStamp() > this.internalStamp) {
                    this.currentHasAired = false;
                    return broadCast;
                }
            }

            return null;
        }
    }

    public RadioBroadCast getBroadcastWithID(String guid) {
        for (int i = 0; i < this.broadcasts.size(); i++) {
            RadioBroadCast broadCast = this.broadcasts.get(i);
            if (broadCast.getID().equals(guid)) {
                this.currentBroadcast = broadCast;
                this.currentHasAired = true;
                return broadCast;
            }
        }

        return null;
    }

    public boolean UpdateScript(int timeStamp) {
        this.internalStamp = timeStamp - this.startDayStamp;
        this.currentBroadcast = this.getNextBroadcast();
        return this.currentBroadcast != null;
    }

    public RadioScript.ExitOption getNextScript() {
        int chanceStart = 0;
        int randval = Rand.Next(100);

        for (RadioScript.ExitOption exitOption : this.exitOptions) {
            if (randval >= chanceStart && randval < chanceStart + exitOption.getChance()) {
                return exitOption;
            }

            chanceStart += exitOption.getChance();
        }

        return null;
    }

    public void AddBroadcast(RadioBroadCast broadcast) {
        this.AddBroadcast(broadcast, false);
    }

    public void AddBroadcast(RadioBroadCast broadcast, boolean ignoreTimestamps) {
        boolean added = false;
        if (broadcast != null && broadcast.getID() != null) {
            if (ignoreTimestamps) {
                this.broadcasts.add(broadcast);
                added = true;
            } else if (broadcast.getStartStamp() >= 0 && broadcast.getEndStamp() > broadcast.getStartStamp()) {
                if (this.broadcasts.isEmpty() || this.broadcasts.get(this.broadcasts.size() - 1).getEndStamp() <= broadcast.getStartStamp()) {
                    this.broadcasts.add(broadcast);
                    added = true;
                } else if (!this.broadcasts.isEmpty()) {
                    DebugLog.log(
                        DebugType.Radio,
                        "startstamp = '"
                            + broadcast.getStartStamp()
                            + "', endstamp = '"
                            + broadcast.getEndStamp()
                            + "', previous endstamp = '"
                            + this.broadcasts.get(this.broadcasts.size() - 1).getEndStamp()
                            + "'."
                    );
                }
            } else {
                DebugLog.log(DebugType.Radio, "startstamp = '" + broadcast.getStartStamp() + "', endstamp = '" + broadcast.getEndStamp() + "'.");
            }
        }

        if (!added) {
            String id = broadcast != null ? broadcast.getID() : "null";
            DebugLog.log(DebugType.Radio, "Error cannot add broadcast ID: '" + id + "' to script '" + this.name + "', null or timestamp error");
        }
    }

    public void AddExitOption(String scriptname, int chance, int startdelay) {
        int totchance = chance;

        for (RadioScript.ExitOption exitOption : this.exitOptions) {
            totchance += exitOption.getChance();
        }

        if (totchance <= 100) {
            this.exitOptions.add(new RadioScript.ExitOption(scriptname, chance, startdelay));
        } else {
            DebugLog.log(
                DebugType.Radio, "Error cannot add exitoption with scriptname '" + scriptname + "' to script '" + this.name + "', total chance exceeding 100"
            );
        }
    }

    public RadioBroadCast getValidAirBroadcastDebug() {
        if (this.currentBroadcast != null && this.currentBroadcast.getEndStamp() > this.internalStamp) {
            return this.currentBroadcast;
        } else {
            for (int i = 0; i < this.broadcasts.size(); i++) {
                RadioBroadCast broadCast = this.broadcasts.get(i);
                if (broadCast.getEndStamp() > this.internalStamp) {
                    return broadCast;
                }
            }

            return null;
        }
    }

    public ArrayList<RadioScript.ExitOption> getExitOptions() {
        return this.exitOptions;
    }

    @UsedFromLua
    public static final class ExitOption {
        private String scriptname = "";
        private int chance;
        private int startDelay;

        public ExitOption(String name, int rollchance, int startdelay) {
            this.scriptname = name;
            this.chance = rollchance;
            this.startDelay = startdelay;
        }

        public String getScriptname() {
            return this.scriptname;
        }

        public int getChance() {
            return this.chance;
        }

        public int getStartDelay() {
            return this.startDelay;
        }
    }
}
