// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.Vector3;
import zombie.network.anticheats.PacketValidator;
import zombie.network.packets.INetworkPacket;

public interface IConnection {
    INetworkPacket getPacket(PacketTypes.PacketType var1);

    boolean isHashEquals(PacketTypes.PacketType var1, Integer var2);

    boolean isLimitExceeded(PacketTypes.PacketType var1);

    IsoPlayer getPlayerAt(int var1);

    void setPlayerAt(int var1, IsoPlayer var2);

    ByteBufferWriter startPacket();

    void endPacket(int var1, int var2, byte var3);

    void cancelPacket();

    String getUserName();

    String getDescription();

    boolean hasPlayer(String var1);

    boolean hasPlayer(short var1);

    Role getRole();

    void setRole(Role var1);

    boolean isRelevantTo(float var1, float var2);

    long getConnectedGUID();

    String getIDStr();

    boolean wasInLoadingQueue();

    PlayerDownloadServer getPlayerDownloadServer();

    long getLastConnection();

    int getMTUSize();

    void setConnectionTimestamp(long var1);

    PacketValidator getValidator();

    int getChunkGridWidth();

    void setChunkGridWidth(int var1);

    String getUserName(int var1);

    void setUserName(int var1, String var2);

    Vector3 getRelevantPos(int var1);

    void setRelevantPos(int var1, Vector3 var2);

    void setConnectArea(int var1, Vector3 var2);

    short getPlayerId(int var1);

    void setPlayerId(int var1, short var2);

    void setRelevantRange(byte var1);

    void forceDisconnect(String var1);

    void setFullyConnected();

    long getSteamId();

    String getIP();

    void setLoadedCells(int var1, ClientServerMap var2);

    ClientServerMap getLoadedCell(int var1);

    default void addChunkObjectState(short w) {
    }

    default int getIndex() {
        return 0;
    }
}
