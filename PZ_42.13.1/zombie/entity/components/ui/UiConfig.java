// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity.components.ui;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.network.EntityPacketType;
import zombie.scripting.entity.ComponentScript;
import zombie.scripting.entity.components.ui.UiConfigScript;
import zombie.scripting.ui.XuiManager;
import zombie.scripting.ui.XuiSkin;

@DebugClassFields
@UsedFromLua
public class UiConfig extends Component {
    private String xuiSkinName;
    private XuiSkin skin;
    private String entityStyleName;
    private boolean uiEnabled = true;

    private UiConfig() {
        super(ComponentType.UiConfig);
    }

    @Override
    protected void readFromScript(ComponentScript componentScript) {
        super.readFromScript(componentScript);
        UiConfigScript script = (UiConfigScript)componentScript;
        this.setSkin(script.getXuiSkinName());
        this.entityStyleName = script.getEntityStyle();
        this.uiEnabled = script.isUiEnabled();
    }

    private void setSkin(String skinName) {
        this.xuiSkinName = skinName;
        if (this.xuiSkinName != null) {
            this.skin = XuiManager.GetSkin(skinName);
            if (this.skin == null) {
                DebugLog.General.warn("Could not find skin: " + skinName);
                this.skin = XuiManager.GetDefaultSkin();
            }
        } else {
            this.skin = XuiManager.GetDefaultSkin();
        }
    }

    public XuiSkin getSkin() {
        return this.getSkin(false);
    }

    public XuiSkin getSkinOrDefault() {
        return this.getSkin(true);
    }

    public XuiSkin getSkin(boolean doDefault) {
        if (this.skin == null) {
            return XuiManager.GetDefaultSkin();
        } else {
            if (this.skin.isInvalidated()) {
                this.setSkin(this.xuiSkinName);
            }

            return this.skin;
        }
    }

    public XuiSkin.EntityUiStyle getEntityUiStyle() {
        XuiSkin skin = this.getSkinOrDefault();
        return skin.getEntityUiStyle(this.entityStyleName);
    }

    public String getEntityStyleName() {
        return this.entityStyleName;
    }

    public boolean isUiEnabled() {
        return this.uiEnabled;
    }

    public String getEntityDisplayName() {
        XuiSkin skin = this.getSkinOrDefault();
        return skin != null ? skin.getEntityDisplayName(this.entityStyleName) : GameEntity.getDefaultEntityDisplayName();
    }

    @Override
    protected void reset() {
        super.reset();
        this.skin = null;
        this.xuiSkinName = null;
        this.entityStyleName = null;
        this.uiEnabled = true;
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
        this.load(input, 240);
    }

    @Override
    protected void save(ByteBuffer output) throws IOException {
        super.save(output);
        output.put((byte)(this.xuiSkinName != null ? 1 : 0));
        if (this.xuiSkinName != null) {
            GameWindow.WriteString(output, this.xuiSkinName);
        }

        output.put((byte)(this.entityStyleName != null ? 1 : 0));
        if (this.entityStyleName != null) {
            GameWindow.WriteString(output, this.entityStyleName);
        }

        output.put((byte)(this.uiEnabled ? 1 : 0));
    }

    @Override
    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.xuiSkinName = null;
        this.entityStyleName = null;
        if (input.get() == 1) {
            this.xuiSkinName = GameWindow.ReadString(input);
        }

        if (input.get() == 1) {
            this.entityStyleName = GameWindow.ReadString(input);
        }

        this.setSkin(this.xuiSkinName);
        this.uiEnabled = input.get() == 1;
    }
}
