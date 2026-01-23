// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.entity.ComponentType;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.Resources;
import zombie.network.JSONField;

public class ResourceID implements INetworkPacketField {
    protected Resource resource = null;
    @JSONField
    protected String resourceID = "";
    @JSONField
    protected final GameEntityID entityID = new GameEntityID();

    public void set(Resource resource) {
        this.resource = resource;
        this.resourceID = resource.getId();
        this.entityID.set(resource.getResourcesComponent().getOwner());
    }

    public Resource getResource() {
        return this.resource;
    }

    public void clear() {
        this.resourceID = "";
        this.resource = null;
        this.entityID.clear();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.entityID.parse(b, connection);
        this.resourceID = GameWindow.ReadString(b);
        if (this.isConsistent(connection)) {
            Resources resources = this.entityID.getGameEntity().getComponent(ComponentType.Resources);
            this.resource = resources.getResource(this.resourceID);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.entityID.write(b);
        GameWindow.WriteString(b.bb, this.resourceID);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.entityID.isConsistent(connection) && !this.resourceID.isEmpty();
    }
}
