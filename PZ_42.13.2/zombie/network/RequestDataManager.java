// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.Translator;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.gameStates.GameLoadingState;
import zombie.network.packets.RequestDataPacket;

public class RequestDataManager {
    public static final int smallFileSize = 1024;
    public static final int maxLargeFileSize = 52428800;
    public static final int packSize = 204800;
    private final ArrayList<RequestDataManager.RequestData> requests = new ArrayList<>();
    private static RequestDataManager instance;

    private RequestDataManager() {
    }

    public static RequestDataManager getInstance() {
        if (instance == null) {
            instance = new RequestDataManager();
        }

        return instance;
    }

    public void ACKWasReceived(RequestDataPacket.RequestID id, UdpConnection connection, int bytesTransmitted) {
        RequestDataManager.RequestData data = null;

        for (int i = 0; i <= this.requests.size(); i++) {
            if (this.requests.get(i).connectionGuid == connection.getConnectedGUID()) {
                data = this.requests.get(i);
                break;
            }
        }

        if (data != null && data.id == id) {
            this.sendData(data);
        }
    }

    public void putDataForTransmit(RequestDataPacket.RequestID id, UdpConnection connection, ByteBuffer bb) {
        RequestDataManager.RequestData data = new RequestDataManager.RequestData(id, bb, connection.getConnectedGUID());
        this.requests.add(data);
        this.sendData(data);
    }

    public void disconnect(UdpConnection connection) {
        long currentTime = System.currentTimeMillis();
        this.requests.removeIf(requestData -> currentTime - requestData.creationTime > 60000L || requestData.connectionGuid == connection.getConnectedGUID());
    }

    public void clear() {
        this.requests.clear();
    }

    private void sendData(RequestDataManager.RequestData data) {
        data.creationTime = System.currentTimeMillis();
        int fileSize = data.bb.limit();
        data.realTransmittedFromLastAck = 0;
        UdpConnection connection = GameServer.udpEngine.getActiveConnection(data.connectionGuid);
        RequestDataPacket packet = new RequestDataPacket();
        packet.setPartData(data.id, data.bb);

        while (data.realTransmittedFromLastAck < 204800) {
            int toSend = Math.min(1024, fileSize - data.realTransmitted);
            if (toSend == 0) {
                break;
            }

            packet.setPartDataParameters(data.realTransmitted, toSend);
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.RequestData.send(connection);
            data.realTransmittedFromLastAck += toSend;
            data.realTransmitted += toSend;
        }

        if (data.realTransmitted == fileSize) {
            this.requests.remove(data);
        }
    }

    public ByteBuffer receiveClientData(RequestDataPacket.RequestID id, ByteBuffer bb, int fileSize, int bytesTransmitted) {
        RequestDataManager.RequestData data = null;

        for (int i = 0; i < this.requests.size(); i++) {
            if (this.requests.get(i).id == id) {
                data = this.requests.get(i);
                break;
            }
        }

        if (data == null) {
            data = new RequestDataManager.RequestData(id, fileSize, 0L);
            this.requests.add(data);
        }

        data.bb.position(bytesTransmitted);
        data.bb.put(bb.array(), 0, bb.limit());
        data.realTransmitted = data.realTransmitted + bb.limit();
        data.realTransmittedFromLastAck = data.realTransmittedFromLastAck + bb.limit();
        if (data.realTransmittedFromLastAck >= 204800) {
            data.realTransmittedFromLastAck = 0;
            RequestDataPacket packet = new RequestDataPacket();
            packet.setACK(data.id);
            ByteBufferWriter b = GameClient.connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(b);
            packet.write(b);
            PacketTypes.PacketType.RequestData.send(GameClient.connection);
        }

        GameLoadingState.gameLoadingString = Translator.getText("IGUI_MP_DownloadedLargeFile", data.realTransmitted * 100 / fileSize, data.id.getDescriptor());
        if (data.realTransmitted == fileSize) {
            this.requests.remove(data);
            data.bb.position(0);
            return data.bb;
        } else {
            return null;
        }
    }

    static class RequestData {
        private final RequestDataPacket.RequestID id;
        private final ByteBuffer bb;
        private final long connectionGuid;
        private long creationTime = System.currentTimeMillis();
        private int realTransmitted;
        private int realTransmittedFromLastAck;

        public RequestData(RequestDataPacket.RequestID id, ByteBuffer bb, long connectionGuid) {
            this.id = id;
            this.bb = ByteBuffer.allocate(bb.position());
            this.bb.put(bb.array(), 0, this.bb.limit());
            this.connectionGuid = connectionGuid;
            this.realTransmitted = 0;
            this.realTransmittedFromLastAck = 0;
        }

        public RequestData(RequestDataPacket.RequestID id, int bufferSize, long connectionGuid) {
            this.id = id;
            this.bb = ByteBuffer.allocate(bufferSize);
            this.bb.clear();
            this.connectionGuid = connectionGuid;
            this.realTransmitted = 0;
            this.realTransmittedFromLastAck = 0;
        }
    }
}
