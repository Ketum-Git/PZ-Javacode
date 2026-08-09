// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.meta;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.network.IConnection;

@UsedFromLua
public class MetaTagComponent extends Component {
    private long storedId = Long.MIN_VALUE;

    private MetaTagComponent() {
        super(ComponentType.MetaTag);
    }

    public void setStoredID(long storedId) {
        this.storedId = storedId;
    }

    public long getStoredID() {
        return this.storedId;
    }

    @Override
    protected void reset() {
        super.reset();
        this.storedId = Long.MIN_VALUE;
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
        return false;
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 249);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.putLong(this.storedId);
    }

    @Override
    protected void load(ByteBuffer input, int worldVersion) throws IOException {
        super.load(input, worldVersion);
        this.storedId = input.getLong();
    }
}
