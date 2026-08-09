// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.debug.DebugType;
import zombie.iso.WorldStreamer;
import zombie.network.ClientChunkRequest;
import zombie.network.GameServer;
import zombie.network.IConnection;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 4, priority = 0, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 1)
public class RequestZipListPacket implements INetworkPacket {
    private ArrayList<WorldStreamer.ChunkRequest> requests;

    public void set(ArrayList<WorldStreamer.ChunkRequest> tempRequests) {
        this.requests = tempRequests;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.requests.size());

        for (int i = 0; i < this.requests.size(); i++) {
            WorldStreamer.ChunkRequest request = this.requests.get(i);
            b.putInt(request.requestNumber);
            b.putInt(request.chunk.wx);
            b.putInt(request.chunk.wy);
            b.putLong(request.crc);
            if (DebugType.NetworkFileDebug.isEnabled()) {
                DebugType.NetworkFileDebug.debugln("requested %d, %d crc=%d", request.chunk.wx, request.chunk.wy, request.crc);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        if (!connection.wasInLoadingQueue()) {
            GameServer.kick(connection, "UI_Policy_Kick", "The server received an invalid request");
        }

        if (connection.getPlayerDownloadServer() != null) {
            ClientChunkRequest ccr = connection.getPlayerDownloadServer().getClientChunkRequest(false);
            connection.getPlayerDownloadServer().ccrWaiting.add(ccr);
            int count = b.getInt();

            for (int n = 0; n < count; n++) {
                if (ccr.isChunksFilled()) {
                    ccr = connection.getPlayerDownloadServer().getClientChunkRequest(false);
                    connection.getPlayerDownloadServer().ccrWaiting.add(ccr);
                }

                ClientChunkRequest.Chunk chunk = ccr.getChunk();
                chunk.requestNumber = b.getInt();
                chunk.wx = b.getInt();
                chunk.wy = b.getInt();
                chunk.crc = b.getLong();
                ccr.chunks.add(chunk);
            }
        }
    }
}
