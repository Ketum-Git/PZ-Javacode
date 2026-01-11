// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.hit.Zombie;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class StopFirePacket implements INetworkPacket {
    @JSONField
    StopFirePacket.Type type = StopFirePacket.Type.GridSquare;
    @JSONField
    PlayerID playerId;
    @JSONField
    Zombie zombieId;
    @JSONField
    Square squareId;

    @Override
    public void setData(Object... values) {
        if (values[0] instanceof IsoPlayer) {
            this.type = StopFirePacket.Type.Player;
            this.playerId = new PlayerID();
            this.playerId.set((IsoPlayer)values[0]);
        }

        if (values[0] instanceof IsoZombie) {
            this.type = StopFirePacket.Type.Zombie;
            this.zombieId = new Zombie();
            this.zombieId.set((IsoZombie)values[0]);
        }

        if (values[0] instanceof IsoGridSquare) {
            this.type = StopFirePacket.Type.GridSquare;
            this.squareId = new Square();
            this.squareId.set((IsoGridSquare)values[0]);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.type == StopFirePacket.Type.GridSquare) {
            this.squareId.getSquare().stopFire();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.type == StopFirePacket.Type.Player) {
            this.playerId.getPlayer().sendObjectChange("StopBurning");
        }

        if (this.type == StopFirePacket.Type.Zombie) {
            this.zombieId.getCharacter().StopBurning();
        }

        if (this.type == StopFirePacket.Type.GridSquare) {
            this.squareId.getSquare().stopFire();
            this.sendToRelativeClients(PacketTypes.PacketType.StopFire, connection, this.squareId.getX(), this.squareId.getY());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.type.ordinal());
        if (this.type == StopFirePacket.Type.Player) {
            this.playerId.write(b);
        }

        if (this.type == StopFirePacket.Type.Zombie) {
            this.zombieId.write(b);
        }

        if (this.type == StopFirePacket.Type.GridSquare) {
            this.squareId.write(b);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.type = StopFirePacket.Type.values()[b.get()];
        if (this.type == StopFirePacket.Type.Player) {
            this.playerId = new PlayerID();
            this.playerId.parse(b, connection);
        }

        if (this.type == StopFirePacket.Type.Zombie) {
            this.zombieId = new Zombie();
            this.zombieId.parse(b, connection);
        }

        if (this.type == StopFirePacket.Type.GridSquare) {
            this.squareId = new Square();
            this.squareId.parse(b, connection);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (this.type == StopFirePacket.Type.Player) {
            return this.playerId.getPlayer() != null;
        } else if (this.type == StopFirePacket.Type.Zombie) {
            return this.zombieId.getCharacter() != null;
        } else {
            return this.type == StopFirePacket.Type.GridSquare ? this.squareId.getSquare() != null : false;
        }
    }

    static enum Type {
        GridSquare,
        Player,
        Zombie;
    }
}
