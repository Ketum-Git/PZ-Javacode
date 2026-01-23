// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.Core;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoWindow;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SmashWindowPacket implements INetworkPacket {
    @JSONField
    NetObject window = new NetObject();
    @JSONField
    SmashWindowPacket.Action action;

    public void setSmashWindow(IsoObject _window) {
        this.window.setObject(_window);
        this.action = SmashWindowPacket.Action.smashWindow;
    }

    public void setRemoveBrokenGlass(IsoObject _window) {
        this.window.setObject(_window);
        this.action = SmashWindowPacket.Action.removeBrokenGlass;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.window.write(b);
        b.putByte((byte)this.action.ordinal());
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.window.parse(b, connection);
        this.action = SmashWindowPacket.Action.values()[b.get()];
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.window.getObject() instanceof IsoWindow isoWindow) {
            if (this.action == SmashWindowPacket.Action.smashWindow) {
                isoWindow.smashWindow(true);
            } else if (this.action == SmashWindowPacket.Action.removeBrokenGlass) {
                isoWindow.setGlassRemoved(true);
            }
        } else if (Core.debug) {
            DebugLog.log("SmashWindow not a window!");
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoObject o = this.window.getObject();
        if (o != null && o instanceof IsoWindow isoWindow) {
            if (this.action == SmashWindowPacket.Action.smashWindow) {
                isoWindow.smashWindow(true);
            } else if (this.action == SmashWindowPacket.Action.removeBrokenGlass) {
                isoWindow.setGlassRemoved(true);
            }

            this.sendToRelativeClients(PacketTypes.PacketType.SmashWindow, null, o.getX(), o.getY());
        }
    }

    static enum Action {
        smashWindow,
        removeBrokenGlass;
    }
}
