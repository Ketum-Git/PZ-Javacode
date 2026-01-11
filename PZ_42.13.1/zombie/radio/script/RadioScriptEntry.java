// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.script;

public final class RadioScriptEntry {
    private int chanceMin;
    private int chanceMax = 100;
    private String scriptName = "";
    private int delay;

    public RadioScriptEntry(String name, int delay) {
        this(name, delay, 0, 100);
    }

    public RadioScriptEntry(String name, int delay, int min, int max) {
        this.scriptName = name;
        this.setChanceMin(min);
        this.setChanceMax(max);
        this.setDelay(delay);
    }

    public void setChanceMin(int min) {
        this.chanceMin = min < 0 ? 0 : (min > 100 ? 100 : min);
    }

    public int getChanceMin() {
        return this.chanceMin;
    }

    public void setChanceMax(int max) {
        this.chanceMax = max < 0 ? 0 : (max > 100 ? 100 : max);
    }

    public int getChanceMax() {
        return this.chanceMax;
    }

    public String getScriptName() {
        return this.scriptName;
    }

    public void setScriptName(String name) {
        this.scriptName = name;
    }

    public int getDelay() {
        return this.delay;
    }

    public void setDelay(int delay) {
        this.delay = delay >= 0 ? delay : 0;
    }
}
