// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.devices;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;

/**
 * Turrubo
 */
@UsedFromLua
public final class DevicePresets implements Cloneable {
    protected int maxPresets = 10;
    protected ArrayList<PresetEntry> presets = new ArrayList<>();

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public KahluaTable getPresetsLua() {
        KahluaTable table = LuaManager.platform.newTable();

        for (int i = 0; i < this.presets.size(); i++) {
            PresetEntry p = this.presets.get(i);
            KahluaTable subtable = LuaManager.platform.newTable();
            subtable.rawset("name", p.name);
            subtable.rawset("frequency", p.frequency);
            table.rawset(i, subtable);
        }

        return table;
    }

    public ArrayList<PresetEntry> getPresets() {
        return this.presets;
    }

    public void setPresets(ArrayList<PresetEntry> p) {
        this.presets = p;
    }

    public int getMaxPresets() {
        return this.maxPresets;
    }

    public void setMaxPresets(int m) {
        this.maxPresets = m;
    }

    public void addPreset(String name, int frequency) {
        new PresetEntry(name, frequency);

        for (int i = 0; i < this.presets.size(); i++) {
            PresetEntry p = this.presets.get(i);
            if (p.getFrequency() == frequency || Objects.equals(p.getName(), name)) {
                return;
            }
        }

        if (this.presets.size() < this.maxPresets) {
            this.presets.add(new PresetEntry(name, frequency));
        }
    }

    public void removePreset(int id) {
        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            this.presets.remove(id);
        }
    }

    public String getPresetName(int id) {
        return !this.presets.isEmpty() && id >= 0 && id < this.presets.size() ? this.presets.get(id).name : "";
    }

    public int getPresetFreq(int id) {
        return !this.presets.isEmpty() && id >= 0 && id < this.presets.size() ? this.presets.get(id).frequency : -1;
    }

    public void setPresetName(int id, String name) {
        if (name == null) {
            name = "name-is-null";
        }

        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            PresetEntry p = this.presets.get(id);
            p.name = name;
        }
    }

    public void setPresetFreq(int id, int frequency) {
        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            PresetEntry p = this.presets.get(id);
            p.frequency = frequency;
        }
    }

    public void setPreset(int id, String name, int frequency) {
        if (name == null) {
            name = "name-is-null";
        }

        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            PresetEntry p = this.presets.get(id);
            p.name = name;
            p.frequency = frequency;
        }
    }

    public void clearPresets() {
        this.presets.clear();
    }

    public void save(ByteBuffer output, boolean net) throws IOException {
        output.putInt(this.maxPresets);
        output.putInt(this.presets.size());

        for (int i = 0; i < this.presets.size(); i++) {
            PresetEntry entry = this.presets.get(i);
            GameWindow.WriteString(output, entry.name);
            output.putInt(entry.frequency);
        }
    }

    public void load(ByteBuffer input, int WorldVersion, boolean net) throws IOException {
        this.clearPresets();
        this.maxPresets = input.getInt();
        int entries = input.getInt();

        for (int i = 0; i < entries; i++) {
            String name = GameWindow.ReadString(input);
            int freq = input.getInt();
            if (this.presets.size() < this.maxPresets) {
                this.presets.add(new PresetEntry(name, freq));
            }
        }
    }
}
