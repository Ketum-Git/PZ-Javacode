// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.PersistentOutfits;
import zombie.SharedDescriptors;
import zombie.characters.Capability;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.network.ConnectionDetails;
import zombie.network.ConnectionManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.RequestDataManager;
import zombie.network.ServerWorldDatabase;
import zombie.radio.ZomboidRadio;
import zombie.radio.media.RecordedMedia;
import zombie.worldMap.network.WorldMapClient;
import zombie.worldMap.network.WorldMapServer;

@PacketSetting(ordering = 4, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 7)
public class RequestDataPacket implements INetworkPacket {
    RequestDataPacket.RequestType type;
    RequestDataPacket.RequestID id;
    ByteBuffer buffer;
    int dataSize;
    int dataSent;
    int partSize;
    public static ByteBuffer largeFileBb;

    public void allocateLargeFileBB() {
        if (largeFileBb == null) {
            largeFileBb = ByteBuffer.allocate(52428800);
        }
    }

    public void setRequest() {
        this.type = RequestDataPacket.RequestType.Request;
        this.id = RequestDataPacket.RequestID.ZombieOutfitDescriptors;
    }

    public void setRequest(RequestDataPacket.RequestID id) {
        this.type = RequestDataPacket.RequestType.Request;
        this.id = id;
    }

    public void setPartData(RequestDataPacket.RequestID id, ByteBuffer bb) {
        this.type = RequestDataPacket.RequestType.PartData;
        this.buffer = bb;
        this.id = id;
        this.dataSize = bb.limit();
    }

    public void setPartDataParameters(int bytesSent, int partSize) {
        this.dataSent = bytesSent;
        this.partSize = partSize;
    }

    public void setACK(RequestDataPacket.RequestID id) {
        this.type = RequestDataPacket.RequestType.PartDataACK;
        this.id = id;
    }

    public void sendConnectingDetails(UdpConnection connection, ServerWorldDatabase.LogonResult logonResult) {
        if (GameServer.server) {
            this.id = RequestDataPacket.RequestID.ConnectionDetails;
            this.allocateLargeFileBB();
            largeFileBb.clear();
            ConnectionDetails.write(connection, logonResult, largeFileBb);
            this.doSendRequest(connection);
            DebugLog.Multiplayer.debugln("%s %db", this.id.name(), largeFileBb.position());
            ConnectionManager.log("send-packet", "connection-details", connection);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        try {
            this.type = RequestDataPacket.RequestType.values()[b.get()];
        } catch (Exception var4) {
            DebugLog.Multiplayer.printException(var4, "RequestData packet parse failed", LogSeverity.Error);
            this.type = RequestDataPacket.RequestType.None;
        }

        this.id = RequestDataPacket.RequestID.values()[b.get()];
        if (GameClient.client) {
            if (this.type == RequestDataPacket.RequestType.FullData) {
                int size = b.limit() - b.position();
                this.allocateLargeFileBB();
                largeFileBb.clear();
                largeFileBb.limit(size);
                largeFileBb.put(b.array(), b.position(), size);
                this.buffer = largeFileBb;
            } else if (this.type == RequestDataPacket.RequestType.PartData) {
                this.dataSize = b.getInt();
                this.dataSent = b.getInt();
                this.partSize = b.getInt();
                this.allocateLargeFileBB();
                largeFileBb.clear();
                largeFileBb.limit(this.partSize);
                largeFileBb.put(b.array(), b.position(), this.partSize);
                this.buffer = largeFileBb;
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.type.ordinal());
        b.putByte((byte)this.id.ordinal());
        if (GameServer.server) {
            if (this.type == RequestDataPacket.RequestType.FullData) {
                b.bb.put(this.buffer.array(), 0, this.buffer.position());
            } else if (this.type == RequestDataPacket.RequestType.PartData) {
                b.putInt(this.dataSize);
                b.putInt(this.dataSent);
                b.putInt(this.partSize);
                b.bb.put(this.buffer.array(), this.dataSent, this.partSize);
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (!connection.wasInLoadingQueue && this.id != RequestDataPacket.RequestID.ConnectionDetails) {
            GameServer.kick(connection, "UI_Policy_Kick", "The server received an invalid request");
        }

        if (this.type == RequestDataPacket.RequestType.Request) {
            this.doProcessRequest(connection);
        } else if (this.type == RequestDataPacket.RequestType.PartDataACK) {
            RequestDataManager.getInstance().ACKWasReceived(this.id, connection, this.dataSent);
        }
    }

    private void doSendRequest(UdpConnection connection) {
        this.allocateLargeFileBB();
        if (largeFileBb.position() < 1024) {
            this.type = RequestDataPacket.RequestType.FullData;
            this.buffer = largeFileBb;
            ByteBufferWriter b = connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(b);
            this.write(b);
            PacketTypes.PacketType.RequestData.send(connection);
        } else {
            RequestDataManager.getInstance().putDataForTransmit(this.id, connection, largeFileBb);
        }
    }

    private void doProcessRequest(UdpConnection connection) {
        if (this.id == RequestDataPacket.RequestID.ZombieOutfitDescriptors) {
            try {
                largeFileBb.clear();
                PersistentOutfits.instance.save(largeFileBb);
            } catch (Exception var9) {
                var9.printStackTrace();
            }

            this.doSendRequest(connection);
        }

        if (this.id == RequestDataPacket.RequestID.PlayerZombieDescriptors) {
            SharedDescriptors.Descriptor[] descs = SharedDescriptors.getPlayerZombieDescriptors();
            int count = 0;

            for (int i = 0; i < descs.length; i++) {
                if (descs[i] != null) {
                    count++;
                }
            }

            if (count * 2 * 1024 > largeFileBb.capacity()) {
                largeFileBb = ByteBuffer.allocate(count * 2 * 1024);
            }

            try {
                largeFileBb.clear();
                largeFileBb.putShort((short)count);

                for (SharedDescriptors.Descriptor desc : descs) {
                    if (desc != null) {
                        desc.save(largeFileBb);
                    }
                }

                this.doSendRequest(connection);
            } catch (Exception var10) {
                var10.printStackTrace();
            }
        }

        if (this.id == RequestDataPacket.RequestID.RadioData) {
            largeFileBb.clear();
            ZomboidRadio.getInstance().getRecordedMedia().sendRequestData(largeFileBb);
            this.doSendRequest(connection);
        }

        if (this.id == RequestDataPacket.RequestID.WorldMap) {
            try {
                largeFileBb.clear();
                WorldMapServer.instance.sendRequestData(largeFileBb);
                this.doSendRequest(connection);
            } catch (IOException var8) {
                DebugLog.Multiplayer.printException(var8, "shared map symbols could not be saved", LogSeverity.Error);
                GameServer.kick(connection, "UI_Policy_KickReason", "shared map symbols could not be saved.");
                connection.forceDisconnect("save-shared-map-symbols");
                GameServer.addDisconnect(connection);
            }
        }

        DebugLog.Multiplayer.debugln("%s %db", this.id.name(), largeFileBb.position());
    }

    @Override
    public void processClientLoading(UdpConnection connection) {
        this.processClient(connection);
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.type == RequestDataPacket.RequestType.FullData) {
            largeFileBb.position(0);
            this.doProcessData(largeFileBb);
        } else if (this.type == RequestDataPacket.RequestType.PartData) {
            largeFileBb.position(0);
            this.doProcessPart(largeFileBb);
        }
    }

    private void doProcessPart(ByteBuffer bb) {
        ByteBuffer bb2 = RequestDataManager.getInstance().receiveClientData(this.id, bb, this.dataSize, this.dataSent);
        if (bb2 != null) {
            this.doProcessData(bb2);
        }
    }

    private void doProcessData(ByteBuffer bb) {
        if (this.id == RequestDataPacket.RequestID.ConnectionDetails) {
            ConnectionDetails.parse(bb);
        }

        if (this.id == RequestDataPacket.RequestID.ZombieOutfitDescriptors) {
            try {
                DebugLog.Multiplayer.debugln("received zombie descriptors");
                PersistentOutfits.instance.load(bb);
            } catch (IOException var6) {
                DebugLog.Multiplayer.printException(var6, "PersistentOutfits loading IO error", LogSeverity.Error);
                ExceptionLogger.logException(var6);
            } catch (Exception var7) {
                DebugLog.Multiplayer.printException(var7, "PersistentOutfits loading error", LogSeverity.Error);
            }
        }

        if (this.id == RequestDataPacket.RequestID.PlayerZombieDescriptors) {
            try {
                this.receivePlayerZombieDescriptors(bb);
            } catch (Exception var5) {
                DebugLog.Multiplayer.printException(var5, "Player zombie descriptors loading error", LogSeverity.Error);
                ExceptionLogger.logException(var5);
            }
        }

        if (this.id == RequestDataPacket.RequestID.RadioData) {
            try {
                RecordedMedia.receiveRequestData(bb);
            } catch (Exception var4) {
                DebugLog.Multiplayer.printException(var4, "Radio data loading error", LogSeverity.Error);
                ExceptionLogger.logException(var4);
            }
        }

        if (this.id == RequestDataPacket.RequestID.WorldMap) {
            try {
                WorldMapClient.instance.receiveRequestData(bb);
            } catch (IOException var3) {
                DebugLog.Multiplayer.printException(var3, "Shared map symbols loading error", LogSeverity.Error);
                ExceptionLogger.logException(var3);
            }
        }

        this.sendNextRequest(this.id);
    }

    private void sendNextRequest(RequestDataPacket.RequestID id) {
        switch (id) {
            case ZombieOutfitDescriptors:
                this.setRequest(RequestDataPacket.RequestID.PlayerZombieDescriptors);
                break;
            case PlayerZombieDescriptors:
                this.setRequest(RequestDataPacket.RequestID.RadioData);
                break;
            case RadioData:
                this.setRequest(RequestDataPacket.RequestID.WorldMap);
                break;
            case WorldMap:
                GameClient.instance.setRequest(GameClient.RequestState.Complete);
        }

        if (id != RequestDataPacket.RequestID.WorldMap) {
            ByteBufferWriter bbw = GameClient.connection.startPacket();
            PacketTypes.PacketType.RequestData.doPacket(bbw);
            this.write(bbw);
            PacketTypes.PacketType.RequestData.send(GameClient.connection);
        }
    }

    private void receivePlayerZombieDescriptors(ByteBuffer bb) throws IOException {
        short count = bb.getShort();
        DebugLog.Multiplayer.debugln("received " + count + " player-zombie descriptors");

        for (short i = 0; i < count; i++) {
            SharedDescriptors.Descriptor sharedDesc = new SharedDescriptors.Descriptor();
            sharedDesc.load(bb, 241);
            SharedDescriptors.registerPlayerZombieDescriptor(sharedDesc);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.type != RequestDataPacket.RequestType.None;
    }

    @Override
    public String getDescription() {
        String s = "\n\tRequestDataPacket [";
        s = s + "type=" + this.type.name() + " | ";
        if (this.type == RequestDataPacket.RequestType.Request || this.type == RequestDataPacket.RequestType.PartDataACK) {
            s = s + "id=" + this.id.name() + "] ";
        }

        if (this.type == RequestDataPacket.RequestType.FullData) {
            s = s + "id=" + this.id.name() + " | ";
            s = s + "data=(size:" + this.buffer.limit() + ", data=";
            this.buffer.position(0);

            for (int i = 0; i < Math.min(15, this.buffer.limit()); i++) {
                s = s + " 0x" + Integer.toHexString(this.buffer.get() & 255);
            }

            s = s + ".. ] ";
        }

        if (this.type == RequestDataPacket.RequestType.PartData) {
            s = s + "id=" + this.id.name() + " | ";
            s = s + "dataSize=" + this.dataSize + " | ";
            s = s + "dataSent=" + this.dataSent + " | ";
            s = s + "partSize=" + this.partSize + " | ";
            s = s + "data=(size:" + this.buffer.limit() + ", data=";
            if (this.buffer.limit() >= this.dataSize) {
                this.buffer.position(this.dataSent);
            } else {
                this.buffer.position(0);
            }

            for (int i = 0; i < Math.min(15, this.buffer.limit() - this.buffer.position()); i++) {
                s = s + " " + Integer.toHexString(this.buffer.get() & 255);
            }

            s = s + ".. ] ";
        }

        return s;
    }

    public static enum RequestID {
        ConnectionDetails,
        ZombieOutfitDescriptors,
        PlayerZombieDescriptors,
        RadioData,
        WorldMap;

        public String getDescriptor() {
            return Translator.getText("IGUI_RequestID_" + this.name());
        }
    }

    static enum RequestType {
        None,
        Request,
        FullData,
        PartData,
        PartDataACK;
    }
}
