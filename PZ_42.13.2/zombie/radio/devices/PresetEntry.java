// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.devices;

import zombie.UsedFromLua;

/**
 * turbo
 */
@UsedFromLua
public final class PresetEntry {
    public String name = "New preset";
    public int frequency = 93200;

    public PresetEntry() {
    }

    public PresetEntry(String n, int f) {
        this.name = n;
        this.frequency = f;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public int getFrequency() {
        return this.frequency;
    }

    public void setFrequency(int f) {
        this.frequency = f;
    }
}
