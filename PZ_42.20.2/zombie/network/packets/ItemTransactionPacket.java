// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.characters.Capability;
import zombie.core.Transaction;
import zombie.core.TransactionManager;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.ItemPickerJava;
import zombie.iso.IsoDirections;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ItemTransactionPacket extends Transaction implements INetworkPacket {
    @JSONField
    public long duration;
    @JSONField
    public byte consistent;

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.id);
        b.putEnum(this.state);
        if (Transaction.TransactionState.Reject != this.state) {
            this.playerId.write(b);
            b.putInt(this.entries.size());

            for (Transaction.TransactionEntry entry : this.entries) {
                b.putInt(entry.itemId);
                entry.sourceId.write(b);
                entry.destinationId.write(b);
            }

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
                b.putByte(this.direction.ordinal());
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
    public void parse(ByteBufferReader b, IConnection connection) {
        this.id = b.getByte();
        this.state = b.getEnum(Transaction.TransactionState.class);
        this.entries.clear();
        if (Transaction.TransactionState.Reject != this.state) {
            this.playerId.parse(b, connection);
            int length = b.getInt();

            for (int i = 0; i < length; i++) {
                Transaction.TransactionEntry entry = new Transaction.TransactionEntry(b.getInt());
                entry.sourceId.parse(b, connection);
                entry.destinationId.parse(b, connection);
                this.entries.add(entry);
            }

            byte flags = b.getByte();
            if ((flags & 1) != 0) {
                this.extra = b.getUTF();
            }

            if ((flags & 2) != 0) {
                this.direction = b.getEnum(IsoDirections.class);
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
            for (Transaction.TransactionEntry entry : this.entries) {
                this.consistent = TransactionManager.isConsistent(
                    entry.itemId, null, entry.sourceId.getContainer(), entry.destinationId.getContainer(), this.extra, this, this.playerId.getPlayer()
                );
                if (this.consistent != 0) {
                    break;
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        TransactionManager.instance.setStateFromPacket(this);
        if (Transaction.TransactionState.Done == this.state) {
            Transaction.TransactionEntry firstEntry = this.entries.getFirst();
            if (firstEntry != null && firstEntry.sourceId.getContainer() != null) {
                ItemPickerJava.updateOverlaySprite(firstEntry.sourceId.getContainer().getParent());
            }

            if (firstEntry != null && firstEntry.destinationId.getContainer() != null) {
                ItemPickerJava.updateOverlaySprite(firstEntry.destinationId.getContainer().getParent());
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
}
