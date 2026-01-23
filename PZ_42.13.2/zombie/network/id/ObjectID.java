// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.id;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public abstract class ObjectID implements INetworkPacketField {
    @JSONField
    protected long id;
    @JSONField
    protected ObjectIDType type;

    ObjectID(ObjectIDType type) {
        this.type = type;
        this.reset();
    }

    public long getObjectID() {
        return this.id;
    }

    ObjectIDType getType() {
        return this.type;
    }

    public IIdentifiable getObject() {
        return ObjectIDManager.get(this);
    }

    void set(long id, ObjectIDType type) {
        this.id = id;
        this.type = type;
    }

    public void set(ObjectID other) {
        this.set(other.id, other.type);
    }

    public void reset() {
        this.id = -1L;
    }

    public void load(ByteBuffer input) {
        this.type = ObjectIDType.valueOf(input.get());
    }

    public void save(ByteBuffer output) {
        output.put(this.type.index);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.load(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.save(b.bb);
    }

    @Override
    public String toString() {
        return this.type.name() + "-" + this.id;
    }

    @Override
    public int hashCode() {
        return (int)(this.id * 10L + this.type.index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            ObjectID objectID = (ObjectID)o;
            return this.id == objectID.id && this.type == objectID.type;
        } else {
            return false;
        }
    }

    static class ObjectIDInteger extends ObjectID {
        ObjectIDInteger(ObjectIDType type) {
            super(type);
        }

        @Override
        public void load(ByteBuffer input) {
            this.id = input.getInt();
            super.load(input);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putInt((int)this.id);
            super.save(output);
        }

        @Override
        public int getPacketSizeBytes() {
            return 5;
        }
    }

    static class ObjectIDShort extends ObjectID {
        ObjectIDShort(ObjectIDType type) {
            super(type);
        }

        @Override
        public void load(ByteBuffer input) {
            this.id = input.getShort();
            super.load(input);
        }

        @Override
        public void save(ByteBuffer output) {
            output.putShort((short)this.id);
            super.save(output);
        }

        @Override
        public int getPacketSizeBytes() {
            return 3;
        }
    }
}
