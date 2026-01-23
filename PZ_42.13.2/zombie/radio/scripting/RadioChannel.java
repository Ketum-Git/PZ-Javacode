// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.scripting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.radio.ChannelCategory;
import zombie.radio.RadioData;
import zombie.radio.ZomboidRadio;

/**
 * Turbo
 */
@UsedFromLua
public class RadioChannel {
    private final String guid;
    private RadioData radioData;
    private boolean isTimeSynced;
    private final Map<String, RadioScript> scripts = new HashMap<>();
    private int frequency = -1;
    private String name = "Unnamed channel";
    private boolean isTv;
    private ChannelCategory category = ChannelCategory.Undefined;
    private boolean playerIsListening;
    private RadioScript currentScript;
    private int currentScriptLoop = 1;
    private int currentScriptMaxLoops = 1;
    private RadioBroadCast airingBroadcast;
    private float airCounter;
    private String lastAiredLine = "";
    private String lastBroadcastId;
    private float airCounterMultiplier = 1.0F;
    private boolean louisvilleObfuscate;
    float minmod = 1.5F;
    float maxmod = 5.0F;

    public RadioChannel(String n, int freq, ChannelCategory c) {
        this(n, freq, c, UUID.randomUUID().toString());
    }

    public RadioChannel(String n, int freq, ChannelCategory c, String guid) {
        this.name = n;
        this.frequency = freq;
        this.category = c;
        this.isTv = this.category == ChannelCategory.Television;
        this.guid = guid;
    }

    public String getGUID() {
        return this.guid;
    }

    public int GetFrequency() {
        return this.frequency;
    }

    public String GetName() {
        return this.name;
    }

    public boolean IsTv() {
        return this.isTv;
    }

    public ChannelCategory GetCategory() {
        return this.category;
    }

    public RadioScript getCurrentScript() {
        return this.currentScript;
    }

    public RadioBroadCast getAiringBroadcast() {
        return this.airingBroadcast;
    }

    public String getLastAiredLine() {
        return this.lastAiredLine;
    }

    public int getCurrentScriptLoop() {
        return this.currentScriptLoop;
    }

    public int getCurrentScriptMaxLoops() {
        return this.currentScriptMaxLoops;
    }

    public String getLastBroadcastID() {
        return this.lastBroadcastId;
    }

    public RadioData getRadioData() {
        return this.radioData;
    }

    public void setRadioData(RadioData radioData) {
        this.radioData = radioData;
    }

    public boolean isTimeSynced() {
        return this.isTimeSynced;
    }

    public void setTimeSynced(boolean isTimeSynced) {
        this.isTimeSynced = isTimeSynced;
    }

    public boolean isVanilla() {
        return this.radioData == null || this.radioData.isVanilla();
    }

    public void setLouisvilleObfuscate(boolean b) {
        this.louisvilleObfuscate = b;
    }

    public void LoadAiringBroadcast(String guid, int line) {
        if (this.currentScript != null) {
            this.airingBroadcast = this.currentScript.getBroadcastWithID(guid);
            if (line < 0) {
                this.lastBroadcastId = guid;
                this.airingBroadcast = null;
            }

            if (this.airingBroadcast != null && line >= 0) {
                this.airingBroadcast.resetLineCounter();
                this.airingBroadcast.setCurrentLineNumber(line);
                this.airCounter = 120.0F;
                this.playerIsListening = true;
            }
        }
    }

    public void SetPlayerIsListening(boolean isListening) {
        this.playerIsListening = isListening;
        if (this.playerIsListening && this.airingBroadcast == null && this.currentScript != null) {
            this.airingBroadcast = this.currentScript.getValidAirBroadcast();
            if (this.airingBroadcast != null) {
                this.airingBroadcast.resetLineCounter();
            }

            this.airCounter = 0.0F;
        }
    }

    public boolean GetPlayerIsListening() {
        return this.playerIsListening;
    }

    public void setActiveScriptNull() {
        this.currentScript = null;
        this.airingBroadcast = null;
    }

    public void setActiveScript(String scriptName, int day) {
        this.setActiveScript(scriptName, day, 1, -1);
    }

    public void setActiveScript(String scriptName, int day, int loop, int maxloops) {
        if (scriptName != null && this.scripts.containsKey(scriptName)) {
            this.currentScript = this.scripts.get(scriptName);
            if (this.currentScript != null) {
                this.currentScript.Reset();
                this.currentScript.setStartDayStamp(day);
                this.currentScriptLoop = loop;
                if (maxloops == -1) {
                    int min = this.currentScript.getLoopMin();
                    int max = this.currentScript.getLoopMax();
                    if (min != max && min <= max) {
                        maxloops = Rand.Next(min, max);
                    } else {
                        maxloops = min;
                    }
                }

                this.currentScriptMaxLoops = maxloops;
                if (DebugLog.isEnabled(DebugType.Radio)) {
                    DebugLog.Radio
                        .debugln(
                            "Script: "
                                + scriptName
                                + ", day = "
                                + day
                                + ", minloops = "
                                + this.currentScript.getLoopMin()
                                + ", maxloops = "
                                + this.currentScriptMaxLoops
                        );
                }
            }
        }
    }

    private void getNextScript(int day) {
        if (this.currentScript != null) {
            if (this.currentScriptLoop < this.currentScriptMaxLoops) {
                this.currentScriptLoop++;
                this.currentScript.Reset();
                this.currentScript.setStartDayStamp(day);
            } else {
                RadioScript.ExitOption exitOption = this.currentScript.getNextScript();
                this.currentScript = null;
                if (exitOption != null) {
                    this.setActiveScript(exitOption.getScriptname(), day + exitOption.getStartDelay());
                }
            }
        }
    }

    public void UpdateScripts(int timestamp, int day) {
        this.playerIsListening = false;
        if (this.currentScript != null && !this.currentScript.UpdateScript(timestamp)) {
            this.getNextScript(day + 1);
        }
    }

    public void update() {
        if (this.airingBroadcast != null) {
            this.airCounter = this.airCounter - 1.25F * GameTime.getInstance().getMultiplier();
            if (this.airCounter < 0.0F) {
                RadioLine line = this.airingBroadcast.getNextLine();
                if (line == null) {
                    this.lastBroadcastId = this.airingBroadcast.getID();
                    this.airingBroadcast = null;
                    this.playerIsListening = false;
                } else {
                    this.lastAiredLine = line.getText();
                    if (!ZomboidRadio.disableBroadcasting) {
                        String lineText = line.getText();
                        if (this.louisvilleObfuscate && ZomboidRadio.louisvilleObfuscation) {
                            lineText = ZomboidRadio.getInstance().scrambleString(lineText, 85, true, null);
                            ZomboidRadio.getInstance().SendTransmission(0, 0, this.frequency, lineText, null, "", 0.7F, 0.5F, 0.5F, -1, this.isTv);
                        } else {
                            ZomboidRadio.getInstance()
                                .SendTransmission(
                                    0, 0, this.frequency, lineText, null, line.getEffectsString(), line.getR(), line.getG(), line.getB(), -1, this.isTv
                                );
                        }
                    }

                    if (line.isCustomAirTime()) {
                        this.airCounter = line.getAirTime() * 60.0F;
                    } else {
                        this.airCounter = line.getText().length() / 10.0F * 60.0F;
                        if (this.airCounter < 60.0F * this.minmod) {
                            this.airCounter = 60.0F * this.minmod;
                        } else if (this.airCounter > 60.0F * this.maxmod) {
                            this.airCounter = 60.0F * this.maxmod;
                        }

                        this.airCounter = this.airCounter * this.airCounterMultiplier;
                    }
                }
            }
        }
    }

    public void AddRadioScript(RadioScript script) {
        if (script != null && !this.scripts.containsKey(script.GetName())) {
            this.scripts.put(script.GetName(), script);
        } else {
            String scriptname = script != null ? script.GetName() : "null";
            DebugLog.log(DebugType.Radio, "Error while attempting to add script (" + scriptname + "), null or name already exists.");
        }
    }

    public RadioScript getRadioScript(String script) {
        return script != null && this.scripts.containsKey(script) ? this.scripts.get(script) : null;
    }

    public void setAiringBroadcast(RadioBroadCast bc) {
        this.airingBroadcast = bc;
    }

    public float getAirCounterMultiplier() {
        return this.airCounterMultiplier;
    }

    public void setAirCounterMultiplier(float airCounterMultiplier) {
        this.airCounterMultiplier = PZMath.clamp(airCounterMultiplier, 0.1F, 10.0F);
    }
}
