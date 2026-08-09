// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.entity.ComponentType;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.Resources;
import zombie.network.IConnection;
import zombie.network.JSONField;

public class ResourceID implements INetworkPacketField {
    protected Resource resource;
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
    public void parse(ByteBufferReader b, IConnection connection) {
        this.entityID.parse(b, connection);
        this.resourceID = b.getUTF();
        if (this.isConsistent(connection)) {
            Resources resources = this.entityID.getGameEntity().getComponent(ComponentType.Resources);
            this.resource = resources.getResource(this.resourceID);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.entityID.write(b);
        b.putUTF(this.resourceID);
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.entityID.isConsistent(connection) && !this.resourceID.isEmpty();
    }
}
