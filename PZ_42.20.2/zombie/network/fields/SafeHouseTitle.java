// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.iso.areas.SafeHouse;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.character.PlayerID;
import zombie.util.StringUtils;

public class SafeHouseTitle extends PlayerID implements INetworkPacketField {
    @JSONField
    private String title;

    public void set(IsoPlayer player, String title) {
        super.set(player);
        this.title = title;
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        super.parse(b, connection);
        this.title = b.getUTF();
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putUTF(this.title);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (!super.isConsistent(connection)) {
            DebugType.Multiplayer.error("player is not set");
            return false;
        } else if (StringUtils.isNullOrEmpty(this.title)) {
            DebugType.Multiplayer.error("title is not set");
            return false;
        } else if (SafeHouse.hasNotSurvivedEnoughToClaim(this.getPlayer())) {
            DebugType.Multiplayer.error("player is not survived enough");
            return false;
        } else {
            return true;
        }
    }

    public String getTitle() {
        return this.title;
    }

    public String getUsername() {
        return this.getPlayer().getUsername();
    }
}
