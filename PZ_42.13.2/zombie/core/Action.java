// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.character.PlayerID;
import zombie.network.server.AnimEventEmulator;

abstract class Action implements INetworkPacketField {
    protected static int timeoutForInfinitiveActions = 1000000;
    protected static byte lastId;
    @JSONField
    protected byte id;
    @JSONField
    protected Transaction.TransactionState state;
    @JSONField
    PlayerID playerId = new PlayerID();
    @JSONField
    public long duration;
    protected long startTime;
    protected long endTime;

    public void setTimeData() {
        this.startTime = GameTime.getServerTimeMills();
        this.duration = (long)this.getDuration();
        if (this.duration < 0L) {
            this.endTime = this.startTime + AnimEventEmulator.getInstance().getDurationMax();
        } else {
            this.endTime = this.startTime + this.duration;
        }
    }

    public void set(IsoPlayer player) {
        this.state = Transaction.TransactionState.Request;
        if (lastId == 0) {
            lastId++;
        }

        this.id = lastId++;
        this.playerId.set(player);
        this.setTimeData();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.get();
        this.state = Transaction.TransactionState.values()[b.get()];
        if (this.state == Transaction.TransactionState.Request || this.state == Transaction.TransactionState.Reject) {
            this.playerId.parse(b, connection);
        }

        if (this.state == Transaction.TransactionState.Accept) {
            this.duration = b.getLong();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.id);
        b.putByte((byte)this.state.ordinal());
        if (this.state == Transaction.TransactionState.Request || this.state == Transaction.TransactionState.Reject) {
            this.playerId.write(b);
        }

        if (this.state == Transaction.TransactionState.Accept) {
            b.putLong(this.duration);
        }
    }

    public void setState(Transaction.TransactionState state) {
        this.state = state;
    }

    public void setDuration(long duration) {
        this.endTime = this.startTime + duration;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.state == Transaction.TransactionState.Request ? this.playerId.isConsistent(connection) : true;
    }

    public float getProgress() {
        return this.endTime == this.startTime ? 1.0F : (float)(GameTime.getServerTimeMills() - this.startTime) / (float)(this.endTime - this.startTime);
    }

    abstract float getDuration();

    abstract void start();

    abstract void stop();

    abstract boolean isValid();

    abstract void update();

    abstract boolean perform();

    abstract boolean isUsingTimeout();
}
