// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.CustomizationManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatServerCustomizationDDOS;
import zombie.network.packets.INetworkPacket;

@PacketSetting(
    ordering = 2,
    priority = 1,
    reliability = 3,
    requiredCapability = Capability.None,
    handlingType = 5,
    anticheats = AntiCheat.ServerCustomizationDDOS
)
public class ServerCustomizationPacket implements INetworkPacket, AntiCheatServerCustomizationDDOS.IAntiCheat {
    @JSONField
    private String serverName;
    @JSONField
    private ServerCustomizationPacket.Data data = ServerCustomizationPacket.Data.ServerImageIcon;
    long lastConnect;

    @Override
    public long getLastConnect() {
        return this.lastConnect;
    }

    @Override
    public void setData(Object... values) {
        this.serverName = (String)values[0];
        this.data = (ServerCustomizationPacket.Data)values[1];
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putUTF(this.serverName);
        b.putByte((byte)this.data.ordinal());
        if (GameServer.server) {
            if (this.data == ServerCustomizationPacket.Data.ServerImageIcon) {
                ByteBuffer bb = CustomizationManager.getInstance().getServerImageIcon();
                if (bb != null) {
                    b.putInt(bb.limit());
                    b.bb.put(bb.array(), 0, bb.limit());
                } else {
                    b.putInt(0);
                }
            } else if (this.data == ServerCustomizationPacket.Data.ServerImageLoginScreen) {
                ByteBuffer bb = CustomizationManager.getInstance().getServerImageLoginScreen();
                if (bb != null) {
                    b.putInt(bb.limit());
                    b.bb.put(bb.array(), 0, bb.limit());
                } else {
                    b.putInt(0);
                }
            } else if (this.data == ServerCustomizationPacket.Data.ServerImageLoadingScreen) {
                ByteBuffer bb = CustomizationManager.getInstance().getServerImageLoadingScreen();
                if (bb != null) {
                    b.putInt(bb.limit());
                    b.bb.put(bb.array(), 0, bb.limit());
                } else {
                    b.putInt(0);
                }
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.lastConnect = connection.lastConnection;
        this.serverName = GameWindow.ReadString(b);
        if (this.serverName.contains(File.separator)) {
            DebugLog.General.error("ServerName '" + this.serverName + "' cannot contain a file separator.");
        } else {
            this.data = ServerCustomizationPacket.Data.values()[b.get()];
            if (!GameServer.server) {
                if (this.data == ServerCustomizationPacket.Data.ServerImageIcon) {
                    this.saveFile(b, LuaManager.getLuaCacheDir() + File.separator + this.serverName + "_icon.jpg");
                } else if (this.data == ServerCustomizationPacket.Data.ServerImageLoginScreen) {
                    this.saveFile(b, LuaManager.getLuaCacheDir() + File.separator + this.serverName + "_loginScreen.jpg");
                } else if (this.data == ServerCustomizationPacket.Data.ServerImageLoadingScreen) {
                    this.saveFile(b, LuaManager.getLuaCacheDir() + File.separator + this.serverName + "_loadingScreen.jpg");
                }
            }
        }
    }

    private void saveFile(ByteBuffer b, String filename) {
        int bufSize = b.getInt();
        int position = b.position();
        if (bufSize != 0) {
            File path = new File(filename);
            FileOutputStream output = null;

            try {
                output = new FileOutputStream(path);
                output.getChannel().truncate(0L);
                output.write(b.array(), position, bufSize);
                output.flush();
                output.close();
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        if (this.data == ServerCustomizationPacket.Data.Done) {
            GameClient.askCustomizationData = false;
            LuaEventManager.triggerEvent("OnServerCustomizationDataReceived");
            GameClient.connection.forceDisconnect("receive-CustomizationData");
            INetworkPacket.super.processClientLoading(connection);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToClient(PacketTypes.PacketType.ServerCustomization, connection);
    }

    public static enum Data {
        ServerImageIcon,
        ServerImageLoginScreen,
        ServerImageLoadingScreen,
        Done;
    }
}
