// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.lua;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.network.ByteBufferReader;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.network.IConnection;

@DebugClassFields
@UsedFromLua
public class LuaComponent extends Component {
    private LuaComponent() {
        super(ComponentType.Lua);
    }

    @Override
    protected boolean onReceivePacket(ByteBufferReader input, EntityPacketType type, IConnection senderConnection) throws IOException {
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
        this.load(input, 249);
    }

    public static enum LuaCall {
        OnCanStartReason,
        OnCanStart,
        OnStart,
        OnUpdate,
        OnCancel,
        OnFinish,
        OnEvent,
        OnInfoUI,
        OnInitUI,
        OnUpdateUI,
        OnButtonClickUI;
    }
}
