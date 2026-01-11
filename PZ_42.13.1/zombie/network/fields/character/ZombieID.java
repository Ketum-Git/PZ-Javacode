// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.fields.IDShort;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.IPositional;

public class ZombieID extends IDShort implements INetworkPacketField, IPositional {
    protected IsoZombie zombie;

    public void set(IsoZombie zombie) {
        this.setID(zombie.getOnlineID());
        this.zombie = zombie;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (GameServer.server) {
            this.zombie = ServerMap.instance.zombieMap.get(this.getID());
        } else if (GameClient.client) {
            this.zombie = GameClient.IDToZombieMap.get(this.getID());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection) && this.getZombie() != null;
    }

    @Override
    public float getX() {
        return this.zombie != null ? this.zombie.getX() : 0.0F;
    }

    @Override
    public float getY() {
        return this.zombie != null ? this.zombie.getY() : 0.0F;
    }

    @Override
    public float getZ() {
        return this.zombie != null ? this.zombie.getZ() : 0.0F;
    }

    public IsoZombie getZombie() {
        return this.zombie;
    }
}
