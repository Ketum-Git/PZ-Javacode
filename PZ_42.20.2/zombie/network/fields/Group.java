// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.util.ArrayList;
import java.util.List;
import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;

public class Group implements INetworkPacketField {
    @JSONField
    protected String title;
    @JSONField
    protected String owner;
    @JSONField
    protected final List<String> members = new ArrayList<>();

    public void set(SafeHouse safeHouse) {
        this.title = safeHouse.getTitle();
        this.owner = safeHouse.getOwner();
        this.members.clear();
        this.members.addAll(safeHouse.getPlayers());
    }

    public void set(Faction faction) {
        this.title = faction.getName();
        this.owner = faction.getOwner();
        this.members.clear();
        this.members.addAll(faction.getPlayers());
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.title = b.getUTF();
        this.owner = b.getUTF();
        short membersSize = b.getShort();
        this.members.clear();

        for (int i = 0; i < membersSize; i++) {
            this.members.add(b.getUTF());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.title);
        b.putUTF(this.owner);
        b.putShort(this.members.size());

        for (String member : this.members) {
            b.putUTF(member);
        }
    }
}
