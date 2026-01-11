// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public class DictionaryStringInfo {
    protected String string;
    protected short registryId;
    protected boolean isLoaded;

    public String getString() {
        return this.string;
    }

    public short getRegistryID() {
        return this.registryId;
    }

    public boolean isLoaded() {
        return this.isLoaded;
    }

    protected DictionaryStringInfo copy() {
        DictionaryStringInfo copy = new DictionaryStringInfo();
        copy.string = this.string;
        copy.registryId = this.registryId;
        copy.isLoaded = this.isLoaded;
        return copy;
    }

    protected void saveAsText(FileWriter w, String padding) throws IOException {
        w.write(padding + "registryID = " + this.registryId + "," + System.lineSeparator());
        w.write(padding + "string = \"" + this.string + "\"," + System.lineSeparator());
        w.write(padding + "isLoaded = " + this.isLoaded + "," + System.lineSeparator());
    }

    protected void save(ByteBuffer bb) {
        BitHeaderWrite header = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, bb);
        bb.putShort(this.registryId);
        if (this.string.startsWith("Base.") && this.string.length() > "Base.".length()) {
            header.addFlags(1);
            GameWindow.WriteString(bb, this.string.substring("Base.".length()));
        } else {
            GameWindow.WriteString(bb, this.string);
        }

        if (this.isLoaded) {
            header.addFlags(2);
        }

        header.write();
        header.release();
    }

    protected void load(ByteBuffer bb, int Version) {
        BitHeaderRead header = BitHeader.allocRead(BitHeader.HeaderSize.Byte, bb);
        this.registryId = bb.getShort();
        if (header.hasFlags(1)) {
            this.string = "Base." + GameWindow.ReadString(bb);
        } else {
            this.string = GameWindow.ReadString(bb);
        }

        this.isLoaded = header.hasFlags(2);
        header.release();
    }
}
