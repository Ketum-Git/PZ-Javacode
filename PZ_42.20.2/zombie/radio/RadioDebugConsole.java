// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.core.Color;
import zombie.input.GameKeyboard;
import zombie.radio.scripting.RadioBroadCast;
import zombie.radio.scripting.RadioChannel;
import zombie.radio.scripting.RadioScript;
import zombie.radio.scripting.RadioScriptManager;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

/**
 * Turbo
 */
public final class RadioDebugConsole {
    private final HashMap<Integer, Boolean> state = new HashMap<>();
    private int channelIndex;
    private final int testcounter = 0;
    private final Color colRed = new Color(255, 0, 0, 255);
    private final Color colGreen = new Color(0, 255, 0, 255);
    private final Color colWhite = new Color(255, 255, 255, 255);
    private final Color colGrey = new Color(150, 150, 150, 255);
    private final Color colDyn = new Color(255, 255, 255, 255);
    private int drawY;
    private int drawX;
    private int drawYLine = 20;

    public RadioDebugConsole() {
        this.state.put(12, false);
        this.state.put(13, false);
        this.state.put(53, false);
        this.state.put(26, false);
    }

    public void update() {
        Map<Integer, RadioChannel> channels = RadioScriptManager.getInstance().getChannels();

        for (Entry<Integer, Boolean> entry : this.state.entrySet()) {
            boolean isdown = GameKeyboard.isKeyDown(entry.getKey());
            if (isdown && entry.getValue() != isdown) {
                switch (entry.getKey()) {
                    case 12:
                        this.channelIndex--;
                        if (this.channelIndex < 0 && channels != null) {
                            this.channelIndex = channels.size() - 1;
                        }
                        break;
                    case 13:
                        this.channelIndex++;
                        if (channels != null && this.channelIndex >= channels.size()) {
                            this.channelIndex = 0;
                        }
                    case 26:
                    case 53:
                }
            }

            entry.setValue(isdown);
        }
    }

    public void render() {
        Map<Integer, RadioChannel> channels = RadioScriptManager.getInstance().getChannels();
        if (channels != null && !channels.isEmpty()) {
            if (this.channelIndex < 0) {
                this.channelIndex = 0;
            }

            if (this.channelIndex >= channels.size()) {
                this.channelIndex = channels.size() - 1;
            }

            this.drawYLine = 20;
            this.drawX = 20;
            this.drawY = 200;
            int tabx = 150;
            this.DrawLine("Scamble once: ", 0, false, this.colGrey);
            this.AddBlancLine();
            this.DrawLine("Radio Script Manager Debug.", 0, true);
            this.DrawLine("Real Time: ", 0, false, this.colGrey);
            this.DrawLine(timeStampToString(RadioScriptManager.getInstance().getCurrentTimeStamp()), 150, true);
            this.AddBlancLine();
            this.AddBlancLine();
            this.DrawLine("Index: " + (this.channelIndex + 1) + " of " + channels.size() + " total channels.", 0, true);
            RadioChannel channel = (RadioChannel)channels.values().toArray()[this.channelIndex];
            if (channel != null) {
                this.DrawLine("Selected channel: ", 0, false, this.colGrey);
                this.DrawLine(channel.GetName(), 150, true);
                this.DrawLine("Type: ", 0, false, this.colGrey);
                this.DrawLine(channel.IsTv() ? "Television" : "Radio", 150, true);
                this.DrawLine("Frequency: ", 0, false, this.colGrey);
                this.DrawLine(Integer.toString(channel.GetFrequency()), 150, true);
                this.DrawLine("Category: ", 0, false, this.colGrey);
                this.DrawLine(channel.GetCategory().toString(), 150, true);
                this.DrawLine("PlayerListening: ", 0, false, this.colGrey);
                if (channel.GetPlayerIsListening()) {
                    this.DrawLine("Yes", 150, true, this.colGreen);
                } else {
                    this.DrawLine("No", 150, true, this.colRed);
                }

                RadioBroadCast broadcast = channel.getAiringBroadcast();
                if (broadcast != null) {
                    this.AddBlancLine();
                    this.DrawLine("Is airing a broadcast:", 0, true, this.colGreen);
                    this.DrawLine("ID: ", 0, false, this.colGrey);
                    this.DrawLine(broadcast.getID(), 150, true);
                    this.DrawLine("StartStamp: ", 0, false, this.colGrey);
                    this.DrawLine(timeStampToString(broadcast.getStartStamp()), 150, true);
                    this.DrawLine("EndStamp: ", 0, false, this.colGrey);
                    this.DrawLine(timeStampToString(broadcast.getEndStamp()), 150, true);
                    if (broadcast.getCurrentLine() != null) {
                        this.colDyn.r = broadcast.getCurrentLine().getR();
                        this.colDyn.g = broadcast.getCurrentLine().getG();
                        this.colDyn.b = broadcast.getCurrentLine().getB();
                        if (broadcast.getCurrentLine().getText() != null) {
                            this.DrawLine("Next line to be aired: ", 0, false, this.colGrey);
                            this.DrawLine(broadcast.PeekNextLineText(), 150, true, this.colDyn);
                        }
                    }
                }

                this.AddBlancLine();
                RadioScript script = channel.getCurrentScript();
                if (script != null) {
                    this.DrawLine("Currently working on RadioScript: ", 0, true);
                    this.DrawLine("Name: ", 0, false, this.colGrey);
                    this.DrawLine(script.GetName(), 150, true);
                    this.DrawLine("Start day: ", 0, false, this.colGrey);
                    this.DrawLine(timeStampToString(script.getStartDayStamp()), 150, true);
                    this.DrawLine("Current loop: ", 0, false, this.colGrey);
                    this.DrawLine(Integer.toString(channel.getCurrentScriptLoop()), 150, true);
                    this.DrawLine("Total loops: ", 0, false, this.colGrey);
                    this.DrawLine(Integer.toString(channel.getCurrentScriptMaxLoops()), 150, true);
                    broadcast = script.getCurrentBroadcast();
                    if (broadcast != null) {
                        this.AddBlancLine();
                        this.DrawLine("Currently active broadcast:", 0, true);
                        this.DrawLine("ID: ", 0, false, this.colGrey);
                        this.DrawLine(broadcast.getID(), 150, true);
                        this.DrawLine("Real StartStamp: ", 0, false, this.colGrey);
                        this.DrawLine(timeStampToString(broadcast.getStartStamp() + script.getStartDayStamp()), 150, true);
                        this.DrawLine("Real EndStamp: ", 0, false, this.colGrey);
                        this.DrawLine(timeStampToString(broadcast.getEndStamp() + script.getStartDayStamp()), 150, true);
                        this.DrawLine("Script StartStamp: ", 0, false, this.colGrey);
                        this.DrawLine(timeStampToString(broadcast.getStartStamp()), 150, true);
                        this.DrawLine("Script EndStamp: ", 0, false, this.colGrey);
                        this.DrawLine(timeStampToString(broadcast.getEndStamp()), 150, true);
                        if (broadcast.getCurrentLine() != null) {
                            this.colDyn.r = broadcast.getCurrentLine().getR();
                            this.colDyn.g = broadcast.getCurrentLine().getG();
                            this.colDyn.b = broadcast.getCurrentLine().getB();
                            if (broadcast.getCurrentLine().getText() != null) {
                                this.DrawLine("Next line to be aired: ", 0, false, this.colGrey);
                                this.DrawLine(broadcast.PeekNextLineText(), 150, true, this.colDyn);
                            }
                        }
                    }
                }
            }
        }
    }

    public static String timeStampToString(int stamp) {
        int days = stamp / 1440;
        int hours = stamp / 60 % 24;
        int mins = stamp % 60;
        return "Day: " + Integer.toString(days) + ", Hour: " + Integer.toString(hours) + ", Minute: " + Integer.toString(mins);
    }

    private void AddBlancLine() {
        this.drawY = this.drawY + this.drawYLine;
    }

    private void DrawLine(String s, int xmod, boolean endline, Color col) {
        TextManager.instance.DrawString(UIFont.Medium, this.drawX + xmod, this.drawY, s, col.r, col.g, col.b, col.a);
        if (endline) {
            this.drawY = this.drawY + this.drawYLine;
        }
    }

    private void DrawLine(String s, int xmod, boolean endline) {
        this.DrawLine(s, xmod, endline, this.colWhite);
    }
}
