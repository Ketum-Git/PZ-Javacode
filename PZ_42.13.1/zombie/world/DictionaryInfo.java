// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import zombie.GameWindow;
import zombie.core.utils.Bits;
import zombie.debug.DebugLog;
import zombie.scripting.entity.GameEntityScript;

public abstract class DictionaryInfo<T extends DictionaryInfo<T>> {
    protected String name;
    protected String moduleName;
    protected String fullType;
    protected short registryId;
    protected boolean existsAsVanilla;
    protected boolean isModded;
    protected String modId;
    protected boolean obsolete;
    protected boolean removed;
    protected boolean isLoaded;
    protected List<String> modOverrides;
    protected GameEntityScript entityScript;

    public abstract String getInfoType();

    public String getName() {
        return this.name;
    }

    public String getFullType() {
        return this.fullType;
    }

    public short getRegistryID() {
        return this.registryId;
    }

    public boolean isExistsAsVanilla() {
        return this.existsAsVanilla;
    }

    public boolean isModded() {
        return this.isModded;
    }

    public String getModID() {
        return this.modId;
    }

    public boolean isObsolete() {
        return this.obsolete;
    }

    public boolean isRemoved() {
        return this.removed;
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    public final GameEntityScript getGameEntityScript() {
        return this.entityScript;
    }

    public List<String> getModOverrides() {
        return this.modOverrides;
    }

    public abstract T copy();

    protected void copyFrom(DictionaryInfo<T> c) {
        this.name = c.name;
        this.moduleName = c.moduleName;
        this.fullType = c.fullType;
        this.registryId = c.registryId;
        this.existsAsVanilla = c.existsAsVanilla;
        this.isModded = c.isModded;
        this.modId = c.modId;
        this.obsolete = c.obsolete;
        this.removed = c.removed;
        this.isLoaded = c.isLoaded;
        if (c.modOverrides != null) {
            this.modOverrides = new ArrayList<>();
            this.modOverrides.addAll(c.modOverrides);
        }

        this.entityScript = c.entityScript;
    }

    public boolean isValid() {
        return !this.obsolete && !this.removed && this.isLoaded;
    }

    public void DebugPrint() {
        DebugLog.log(this.GetDebugString());
    }

    public String GetDebugString() {
        String s = "=== Dictionary "
            + this.getInfoType()
            + " Debug Print ===\nregistryID = "
            + this.registryId
            + ",\nfulltype = \""
            + this.fullType
            + "\",\nmodID = \""
            + this.modId
            + "\",\nexistsAsVanilla = "
            + this.existsAsVanilla
            + ",\nisModded = "
            + this.isModded
            + ",\nobsolete = "
            + this.obsolete
            + ",\nremoved = "
            + this.removed
            + ",\nisModdedOverride = "
            + (this.modOverrides != null ? this.modOverrides.size() : 0)
            + ",\n";
        if (this.modOverrides != null) {
            s = s + "modOverrides = { ";
            if (this.existsAsVanilla) {
                s = s + "PZ-Vanilla, ";
            }

            for (int i = 0; i < this.modOverrides.size(); i++) {
                s = s + "\"" + this.modOverrides.get(i) + "\"";
                if (i < this.modOverrides.size() - 1) {
                    s = s + ", ";
                }
            }

            s = s + " },\n";
        }

        return s + "-----------------------------------\n";
    }

    public String ToString() {
        return "TYPE = "
            + this.getInfoType()
            + ",registryID = "
            + this.registryId
            + ",fulltype = \""
            + this.fullType
            + "\",modID = \""
            + this.modId
            + "\",existsAsVanilla = "
            + this.existsAsVanilla
            + ",isModded = "
            + this.isModded
            + ",obsolete = "
            + this.obsolete
            + ",removed = "
            + this.removed
            + ",modOverrides = "
            + (this.modOverrides != null ? this.modOverrides.size() : 0)
            + ",";
    }

    protected void saveAsText(FileWriter w, String padding) throws IOException {
        w.write(padding + "registryID = " + this.registryId + "," + System.lineSeparator());
        w.write(padding + "fulltype = \"" + this.fullType + "\"," + System.lineSeparator());
        w.write(padding + "modID = \"" + this.modId + "\"," + System.lineSeparator());
        w.write(padding + "existsAsVanilla = " + this.existsAsVanilla + "," + System.lineSeparator());
        w.write(padding + "isModded = " + this.isModded + "," + System.lineSeparator());
        w.write(padding + "obsolete = " + this.obsolete + "," + System.lineSeparator());
        w.write(padding + "removed = " + this.removed + "," + System.lineSeparator());
        if (this.modOverrides != null) {
            String txt = "modOverrides = { ";

            for (int i = 0; i < this.modOverrides.size(); i++) {
                txt = txt + "\"" + this.modOverrides.get(i) + "\"";
                if (i < this.modOverrides.size() - 1) {
                    txt = txt + ", ";
                }
            }

            txt = txt + " },";
            w.write(padding + txt + System.lineSeparator());
        }
    }

    protected void save(ByteBuffer bb, List<String> modIDs, List<String> modules) {
        bb.putShort(this.registryId);
        if (modules.size() > 127) {
            bb.putShort((short)modules.indexOf(this.moduleName));
        } else {
            bb.put((byte)modules.indexOf(this.moduleName));
        }

        GameWindow.WriteString(bb, this.name);
        byte bits = 0;
        int positionBits = bb.position();
        bb.put((byte)0);
        if (this.isModded) {
            bits = Bits.addFlags(bits, 1);
            if (modIDs.size() > 127) {
                bb.putShort((short)modIDs.indexOf(this.modId));
            } else {
                bb.put((byte)modIDs.indexOf(this.modId));
            }
        }

        if (this.existsAsVanilla) {
            bits = Bits.addFlags(bits, 2);
        }

        if (this.obsolete) {
            bits = Bits.addFlags(bits, 4);
        }

        if (this.removed) {
            bits = Bits.addFlags(bits, 8);
        }

        if (this.modOverrides != null) {
            bits = Bits.addFlags(bits, 16);
            if (this.modOverrides.size() == 1) {
                if (modIDs.size() > 127) {
                    bb.putShort((short)modIDs.indexOf(this.modOverrides.get(0)));
                } else {
                    bb.put((byte)modIDs.indexOf(this.modOverrides.get(0)));
                }
            } else {
                bits = Bits.addFlags(bits, 32);
                bb.put((byte)this.modOverrides.size());

                for (int i = 0; i < this.modOverrides.size(); i++) {
                    if (modIDs.size() > 127) {
                        bb.putShort((short)modIDs.indexOf(this.modOverrides.get(i)));
                    } else {
                        bb.put((byte)modIDs.indexOf(this.modOverrides.get(i)));
                    }
                }
            }
        }

        int positionEnd = bb.position();
        bb.position(positionBits);
        bb.put(bits);
        bb.position(positionEnd);
    }

    protected void load(ByteBuffer bb, int Version, List<String> modIDs, List<String> modules) {
        this.registryId = bb.getShort();
        this.moduleName = modules.get(modules.size() > 127 ? bb.getShort() : bb.get());
        this.name = GameWindow.ReadString(bb);
        this.fullType = this.moduleName + "." + this.name;
        byte bits = bb.get();
        if (Bits.hasFlags(bits, 1)) {
            this.modId = modIDs.get(modIDs.size() > 127 ? bb.getShort() : bb.get());
            this.isModded = true;
        } else {
            this.modId = "pz-vanilla";
            this.isModded = false;
        }

        this.existsAsVanilla = Bits.hasFlags(bits, 2);
        this.obsolete = Bits.hasFlags(bits, 4);
        this.removed = Bits.hasFlags(bits, 8);
        if (Bits.hasFlags(bits, 16)) {
            if (this.modOverrides == null) {
                this.modOverrides = new ArrayList<>();
            }

            this.modOverrides.clear();
            if (!Bits.hasFlags(bits, 32)) {
                this.modOverrides.add(modIDs.get(modIDs.size() > 127 ? bb.getShort() : bb.get()));
            } else {
                int count = bb.get();

                for (int i = 0; i < count; i++) {
                    this.modOverrides.add(modIDs.get(modIDs.size() > 127 ? bb.getShort() : bb.get()));
                }
            }
        }
    }
}
