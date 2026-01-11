// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.WorldStreamer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;

@PacketSetting(ordering = 4, priority = 0, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class NotRequiredInZipPacket implements INetworkPacket {
    boolean networkFileDebug = DebugType.NetworkFileDebug.isEnabled();
    private ArrayList<WorldStreamer.ChunkRequest> requests;

    public void set(ArrayList<WorldStreamer.ChunkRequest> tempRequests) {
        this.requests = tempRequests;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.requests.size());

        for (int i = 0; i < this.requests.size(); i++) {
            WorldStreamer.ChunkRequest request = this.requests.get(i);
            if (this.networkFileDebug) {
                DebugLog.NetworkFileDebug.debugln("cancelled " + request.chunk.wx + "," + request.chunk.wy);
            }

            b.putInt(request.requestNumber);
            request.flagsMain |= 2;
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        if (connection.playerDownloadServer != null) {
            int numRequests = b.getInt();

            for (int i = 0; i < numRequests; i++) {
                int requestNumber = b.getInt();
                connection.playerDownloadServer.workerThread.cancelQ.add(requestNumber);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
    }
}
