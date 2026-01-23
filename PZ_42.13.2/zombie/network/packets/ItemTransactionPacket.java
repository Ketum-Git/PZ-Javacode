// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.Transaction;
import zombie.core.TransactionManager;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoDirections;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatTransaction;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3, anticheats = AntiCheat.Transaction)
public class ItemTransactionPacket extends Transaction implements INetworkPacket, AntiCheatTransaction.IAntiCheat {
    @JSONField
    public long duration;
    @JSONField
    public byte consistent;

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.id);
        b.putByte((byte)this.state.ordinal());
        if (Transaction.TransactionState.Reject != this.state) {
            this.playerId.write(b);
            b.putInt(this.itemId);
            this.sourceId.write(b);
            this.destinationId.write(b);
            byte flags = 0;
            if (this.extra != null) {
                flags = (byte)(flags | 1);
            }

            if (this.direction != null) {
                flags = (byte)(flags | 2);
            }

            if (GameServer.server) {
                flags = (byte)(flags | 4);
            }

            if (this.xoff != 0.0F || this.yoff != 0.0F || this.zoff != 0.0F) {
                flags = (byte)(flags | 8);
            }

            b.putByte(flags);
            if ((flags & 1) != 0) {
                b.putUTF(this.extra);
            }

            if ((flags & 2) != 0) {
                b.putByte((byte)this.direction.index());
            }

            if ((flags & 4) != 0) {
                b.putLong(this.endTime - this.startTime);
            }

            if ((flags & 8) != 0) {
                b.putFloat(this.xoff);
                b.putFloat(this.yoff);
                b.putFloat(this.zoff);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.get();
        this.state = Transaction.TransactionState.values()[b.get()];
        if (Transaction.TransactionState.Reject != this.state) {
            this.playerId.parse(b, connection);
            this.itemId = b.getInt();
            this.sourceId.parse(b, connection);
            this.destinationId.parse(b, connection);
            byte flags = b.get();
            if ((flags & 1) != 0) {
                this.extra = GameWindow.ReadString(b);
            }

            if ((flags & 2) != 0) {
                this.direction = IsoDirections.fromIndex(b.get());
            }

            if ((flags & 4) != 0) {
                this.setDuration(b.getLong());
            }

            if ((flags & 8) != 0) {
                this.xoff = b.getFloat();
                this.yoff = b.getFloat();
                this.zoff = b.getFloat();
            }
        }

        if (GameServer.server) {
            this.consistent = TransactionManager.isConsistent(
                this.itemId, null, this.sourceId.getContainer(), this.destinationId.getContainer(), this.extra, this, this.playerId.getPlayer()
            );
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        TransactionManager.instance.setStateFromPacket(this);
        if (Transaction.TransactionState.Done == this.state) {
            if (this.sourceId.getContainer() != null) {
                ItemPickerJava.updateOverlaySprite(this.sourceId.getContainer().getParent());
            }

            if (this.destinationId.getContainer() != null) {
                ItemPickerJava.updateOverlaySprite(this.destinationId.getContainer().getParent());
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.state == Transaction.TransactionState.Request) {
            if (this.consistent == 0) {
                this.setTimeData();
                TransactionManager.add(this);
                this.setState(Transaction.TransactionState.Accept);
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.ItemTransaction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.ItemTransaction.send(connection);
            } else {
                this.setState(Transaction.TransactionState.Reject);
                ByteBufferWriter bbw = connection.startPacket();
                PacketTypes.PacketType.ItemTransaction.doPacket(bbw);
                this.write(bbw);
                PacketTypes.PacketType.ItemTransaction.send(connection);
            }
        } else if (Transaction.TransactionState.Reject == this.state) {
            TransactionManager.removeItemTransaction(this.id, true);
        }
    }

    @Override
    public void setDuration(long duration) {
        super.setDuration(duration);
        this.duration = duration;
    }

    @Override
    public Transaction.TransactionState getState() {
        return this.state;
    }

    @Override
    public byte getConsistent() {
        return this.consistent;
    }
}
