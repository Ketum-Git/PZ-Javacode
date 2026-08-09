// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.characters.Faction;
import zombie.core.network.ByteBufferReader;
import zombie.debug.DebugType;
import zombie.network.IConnection;

public class FactionId extends IDString implements INetworkPacketField {
    private Faction faction;

    public void set(Faction faction) {
        this.set(faction.getName());
        this.faction = faction;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.faction = Faction.getFaction(this.get());
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugType.Multiplayer.error("FactionId invalid: incorrect faction id=%s", this.get());
            return false;
        } else if (this.getFaction() == null) {
            DebugType.Multiplayer.error("FactionId invalid: faction with id=%s not found", this.get());
            return false;
        } else {
            return true;
        }
    }

    public Faction getFaction() {
        return this.faction;
    }
}
