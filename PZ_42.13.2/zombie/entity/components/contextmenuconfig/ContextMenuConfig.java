// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.contextmenuconfig;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.entity.components.contextmenuconfig.ContextMenuConfigScript;

@DebugClassFields
@UsedFromLua
public class ContextMenuConfig extends Component {
    private ContextMenuConfig() {
        super(ComponentType.ContextMenuConfig);
    }

    @Override
    public boolean isQualifiesForMetaStorage() {
        return false;
    }

    @Override
    protected void readFromScript(ComponentScript script) {
        super.readFromScript(script);
    }

    public ArrayList<ContextMenuConfigScript.EntryScript> getEntries() {
        if (this.owner.getEntityScript() != null) {
            GameEntityScript entityScript = this.owner.getEntityScript();
            if (entityScript != null) {
                ContextMenuConfigScript compScript = entityScript.getComponentScriptFor(ComponentType.ContextMenuConfig);
                if (compScript != null) {
                    return compScript.getEntries();
                }
            }
        }

        return new ArrayList<>();
    }

    @Override
    protected void reset() {
        super.reset();
    }

    @Override
    public boolean isValid() {
        return super.isValid();
    }

    @Override
    protected void onAddedToOwner() {
    }

    @Override
    protected void onRemovedFromOwner() {
    }

    @Override
    protected void onConnectComponents() {
    }

    @Override
    protected void onFirstCreation() {
    }

    @Override
    protected void onComponentEvent(ComponentEvent event) {
    }

    @Override
    protected void onEntityEvent(EntityEvent event) {
    }

    @Override
    protected boolean onReceivePacket(ByteBuffer input, EntityPacketType type, UdpConnection senderConnection) throws IOException {
        switch (type) {
            default:
                return false;
        }
    }

    @Override
    protected void saveSyncData(ByteBuffer output) throws IOException {
        this.save(output);
    }

    @Override
    protected void loadSyncData(ByteBuffer input) throws IOException {
        this.load(input, 241);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
    }
}
