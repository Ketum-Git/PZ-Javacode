// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import zombie.GameTime;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
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
    protected final PlayerID playerId = new PlayerID();
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

    public void copyFrom(Action act) {
        this.id = act.id;
        this.state = act.state;
        if (act.state == Transaction.TransactionState.Request
            || act.state == Transaction.TransactionState.Reject
            || act.state == Transaction.TransactionState.Done) {
            this.playerId.set(act.playerId.getPlayer());
        }

        if (act.state == Transaction.TransactionState.Accept) {
            this.duration = act.duration;
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.id = b.getByte();
        this.state = b.getEnum(Transaction.TransactionState.class);
        if (this.state == Transaction.TransactionState.Request
            || this.state == Transaction.TransactionState.Reject
            || this.state == Transaction.TransactionState.Done) {
            this.playerId.parse(b, connection);
        }

        if (this.state == Transaction.TransactionState.Accept) {
            this.duration = b.getLong();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.id);
        b.putEnum(this.state);
        if (this.state == Transaction.TransactionState.Request
            || this.state == Transaction.TransactionState.Reject
            || this.state == Transaction.TransactionState.Done) {
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
    public boolean isConsistent(IConnection connection) {
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
