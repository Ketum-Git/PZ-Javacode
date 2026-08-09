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
public final class DevicePresets {
    private int maxPresets = 10;
    private ArrayList<PresetEntry> presets = new ArrayList<>();

    public DevicePresets() {
    }

    public DevicePresets(DevicePresets other) {
        this.maxPresets = other.maxPresets;

        for (int i = 0; i < other.presets.size(); i++) {
            this.presets.add(new PresetEntry(other.presets.get(i)));
        }
    }

    public KahluaTable getPresetsLua() {
        KahluaTable table = LuaManager.platform.newTable();

        for (int i = 0; i < this.presets.size(); i++) {
            PresetEntry p = this.presets.get(i);
            KahluaTable subtable = LuaManager.platform.newTable();
            subtable.rawset("name", p.getName());
            subtable.rawset("frequency", p.getFrequency());
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
        return !this.presets.isEmpty() && id >= 0 && id < this.presets.size() ? this.presets.get(id).getName() : "";
    }

    public int getPresetFreq(int id) {
        return !this.presets.isEmpty() && id >= 0 && id < this.presets.size() ? this.presets.get(id).getFrequency() : -1;
    }

    public void setPresetName(int id, String name) {
        if (name == null) {
            name = "name-is-null";
        }

        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            PresetEntry p = this.presets.get(id);
            p.setName(name);
        }
    }

    public void setPresetFreq(int id, int frequency) {
        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            PresetEntry p = this.presets.get(id);
            p.setFrequency(frequency);
        }
    }

    public void setPreset(int id, String name, int frequency) {
        if (name == null) {
            name = "name-is-null";
        }

        if (!this.presets.isEmpty() && id >= 0 && id < this.presets.size()) {
            PresetEntry p = this.presets.get(id);
            p.setName(name);
            p.setFrequency(frequency);
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
            GameWindow.WriteString(output, entry.getName());
            output.putInt(entry.getFrequency());
        }
    }

    public void load(ByteBuffer input, int worldVersion, boolean net) throws IOException {
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
