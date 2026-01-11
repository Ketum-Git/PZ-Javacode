// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.objects.DebugClassFields;
import zombie.debug.objects.DebugNonRecursive;
import zombie.entity.events.ComponentEvent;
import zombie.entity.events.ComponentEventType;
import zombie.entity.events.EntityEvent;
import zombie.entity.network.EntityPacketData;
import zombie.entity.network.EntityPacketType;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.entity.ComponentScript;
import zombie.ui.ObjectTooltip;

@DebugClassFields
@UsedFromLua
public abstract class Component {
    @DebugNonRecursive
    protected GameEntity owner;
    private final ComponentType componentType;

    protected Component(ComponentType componentType) {
        this.componentType = Objects.requireNonNull(componentType);
        if (this.componentType == ComponentType.Undefined) {
            throw new IllegalArgumentException("ComponentType error in GameEntity Component.");
        }
    }

    @Override
    public String toString() {
        return "[component: " + this.componentType.toString() + "]";
    }

    public boolean isRunningInMeta() {
        return this.owner != null && this.owner.isMeta();
    }

    public boolean isQualifiesForMetaStorage() {
        return true;
    }

    public final boolean isAddedToEngine() {
        return this.getGameEntity() != null && this.getGameEntity().isValidEngineEntity();
    }

    public final GameEntity getOwner() {
        return this.owner;
    }

    public final GameEntity getGameEntity() {
        return this.owner;
    }

    public final boolean isUsingPlayer(IsoPlayer target) {
        return this.owner != null ? this.owner.isUsingPlayer(target) : false;
    }

    public final IsoPlayer getUsingPlayer() {
        return this.owner != null ? this.owner.getUsingPlayer() : null;
    }

    public final ComponentType getComponentType() {
        return this.componentType;
    }

    protected final void setOwner(GameEntity owner) {
        if (this.owner != null && owner != null) {
            DebugLog.General.error("Setting owner while owner exists.");
        }

        this.owner = owner;
    }

    public final <T extends Component> T getComponent(ComponentType type) {
        if (this.owner != null) {
            return this.owner.getComponent(type);
        } else {
            DebugLog.General.warn("GetComponent owner == null");
            return null;
        }
    }

    public boolean isValid() {
        return this.owner != null && this.componentType.isValidGameEntityType(this.owner.getGameEntityType());
    }

    public void DoTooltip(ObjectTooltip tooltipUI) {
        ObjectTooltip.Layout layout = tooltipUI.beginLayout();
        layout.setMinLabelWidth(80);
        int y = tooltipUI.padTop;
        this.DoTooltip(tooltipUI, layout);
        y = layout.render(tooltipUI.padLeft, y, tooltipUI);
        tooltipUI.endLayout(layout);
        y += tooltipUI.padBottom;
        tooltipUI.setHeight(y);
        if (tooltipUI.getWidth() < 150.0) {
            tooltipUI.setWidth(150.0);
        }
    }

    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
    }

    public final boolean isRenderLast() {
        return this.componentType.isRenderLast();
    }

    public int getRenderLastPriority() {
        return 1000;
    }

    public void dumpContentsInSquare() {
    }

    public boolean isNoContainerOrEmpty() {
        return true;
    }

    protected void renderlast() {
    }

    protected void reset() {
    }

    protected <T extends ComponentScript> void readFromScript(T script) {
    }

    public boolean isValidOwnerType(GameEntityType type) {
        return this.componentType.isValidGameEntityType(type);
    }

    protected void onAddedToOwner() {
    }

    protected void onRemovedFromOwner() {
    }

    protected final void sendComponentEvent(ComponentEventType eventType) {
        if (this.owner != null) {
            this.owner.sendComponentEvent(this, eventType);
        } else if (Core.debug) {
            DebugLog.General.warn("Cannot send component event, no owner.");
        }
    }

    protected final void sendComponentEvent(ComponentEvent event) {
        if (this.owner != null) {
            this.owner.sendComponentEvent(this, event);
        } else if (Core.debug) {
            DebugLog.General.warn("Cannot send component event, no owner.");
        }
    }

    protected void onConnectComponents() {
    }

    protected void onFirstCreation() {
    }

    protected void onComponentEvent(ComponentEvent event) {
    }

    protected void onEntityEvent(EntityEvent event) {
    }

    public final void sendServerPacketTo(IsoPlayer player, EntityPacketData data) {
        if (!GameServer.server) {
            DebugLog.General.warn("Can only send server.");
        }

        GameEntityNetwork.sendPacketDataTo(player, data, this.owner, this);
    }

    protected final void sendClientPacket(EntityPacketData data) {
        if (!GameClient.client) {
            DebugLog.General.warn("Can only send client.");
        }

        GameEntityNetwork.sendPacketData(data, this.owner, this, null, true);
    }

    protected final void sendServerPacket(EntityPacketData data, UdpConnection ignoreConnection) {
        if (!GameServer.server) {
            DebugLog.General.warn("Can only send server.");
        }

        GameEntityNetwork.sendPacketData(data, this.owner, this, ignoreConnection, true);
    }

    protected abstract boolean onReceivePacket(ByteBuffer arg0, EntityPacketType arg1, UdpConnection arg2) throws IOException;

    protected abstract void saveSyncData(ByteBuffer arg0) throws IOException;

    protected abstract void loadSyncData(ByteBuffer arg0) throws IOException;

    protected void save(ByteBuffer output) throws IOException {
    }

    protected void load(ByteBuffer input, int WorldVersion) throws IOException {
    }
}
