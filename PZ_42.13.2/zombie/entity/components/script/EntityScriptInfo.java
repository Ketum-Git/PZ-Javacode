// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.script;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;
import zombie.scripting.objects.Item;

@DebugClassFields
@UsedFromLua
public class EntityScriptInfo extends Component {
    private GameEntityScript script;
    private String originalScript;
    private boolean originalIsItem;

    private EntityScriptInfo() {
        super(ComponentType.Script);
    }

    public void setOriginalScript(GameEntityScript entityScript) {
        this.originalIsItem = entityScript instanceof Item;
        this.originalScript = entityScript.getScriptObjectFullType();
        this.script = entityScript;
    }

    public boolean isOriginalIsItem() {
        return this.originalIsItem;
    }

    public String getOriginalScript() {
        return this.originalScript;
    }

    public GameEntityScript getScript() {
        return this.script;
    }

    @Override
    protected void reset() {
        super.reset();
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
        output.put((byte)(this.originalIsItem ? 1 : 0));
        output.put((byte)(this.originalScript != null ? 1 : 0));
        if (this.originalScript != null) {
            GameWindow.WriteString(output, this.originalScript);
        }
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.originalIsItem = input.get() == 1;
        if (input.get() == 1) {
            this.originalScript = GameWindow.ReadString(input);
        } else {
            this.originalScript = null;
        }

        if (this.originalScript != null) {
            if (this.originalIsItem) {
                this.script = ScriptManager.instance.getItem(this.originalScript);
            } else {
                this.script = ScriptManager.instance.getGameEntityScript(this.originalScript);
            }
        }
    }
}
