// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.savefile;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.characters.IsoPlayer;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class ClientPlayerDB {
    private static ClientPlayerDB instance;
    private static boolean allow;
    public ClientPlayerDB.NetworkCharacterProfile networkProfile;

    public static void setAllow(boolean en) {
        allow = en;
    }

    public static boolean isAllow() {
        return allow;
    }

    public static synchronized ClientPlayerDB getInstance() {
        if (instance == null && allow) {
            instance = new ClientPlayerDB();
        }

        return instance;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public void close() {
        instance = null;
        allow = false;
    }

    public ArrayList<IsoPlayer> getAllNetworkPlayers() {
        ArrayList<IsoPlayer> players = new ArrayList<>();

        for (int i = 1; i < this.networkProfile.playerCount; i++) {
            byte[] buf = this.getClientLoadNetworkPlayerData(i + 1);
            if (buf != null) {
                ByteBuffer BufferForLoadPlayer = ByteBuffer.allocate(buf.length);
                BufferForLoadPlayer.rewind();
                BufferForLoadPlayer.put(buf);
                BufferForLoadPlayer.rewind();

                try {
                    IsoPlayer player = new IsoPlayer(IsoWorld.instance.currentCell);
                    player.serverPlayerIndex = i + 1;
                    player.load(BufferForLoadPlayer, this.networkProfile.worldVersion[i]);
                    if (this.networkProfile.isDead[i]) {
                        player.getBodyDamage().setOverallBodyHealth(0.0F);
                        player.setHealth(0.0F);
                    }

                    players.add(player);
                } catch (Exception var6) {
                    ExceptionLogger.logException(var6);
                }
            }
        }

        return players;
    }

    private boolean isClientLoadNetworkCharacterCompleted() {
        return this.networkProfile != null && this.networkProfile.isLoaded;
    }

    public boolean isAliveMainNetworkPlayer() {
        return !this.networkProfile.isDead[0];
    }

    public boolean clientLoadNetworkPlayer() {
        if (this.networkProfile != null
            && this.networkProfile.isLoaded
            && this.networkProfile.username.equals(GameClient.username)
            && this.networkProfile.server.equals(GameClient.ip)) {
            return this.networkProfile.playerCount > 0;
        } else if (GameClient.connection == null) {
            return false;
        } else {
            if (this.networkProfile != null) {
                this.networkProfile = null;
            }

            INetworkPacket.send(PacketTypes.PacketType.LoadPlayerProfile);
            int timeout = 200;

            while (timeout-- > 0) {
                if (this.isClientLoadNetworkCharacterCompleted()) {
                    return this.networkProfile.playerCount > 0;
                }

                try {
                    Thread.sleep(50L);
                } catch (InterruptedException var3) {
                    ExceptionLogger.logException(var3);
                }
            }

            return false;
        }
    }

    public byte[] getClientLoadNetworkPlayerData(int playerIndex) {
        if (this.networkProfile != null
            && this.networkProfile.isLoaded
            && this.networkProfile.username.equals(GameClient.username)
            && this.networkProfile.server.equals(GameClient.ip)) {
            switch (playerIndex) {
                case 1:
                case 2:
                case 3:
                case 4:
                    return this.networkProfile.character[playerIndex - 1];
                default:
                    return null;
            }
        } else if (!this.clientLoadNetworkPlayer()) {
            return null;
        } else {
            switch (playerIndex) {
                case 1:
                case 2:
                case 3:
                case 4:
                    return this.networkProfile.character[playerIndex - 1];
                default:
                    return null;
            }
        }
    }

    public boolean loadNetworkPlayer() {
        try {
            byte[] buffer = this.getClientLoadNetworkPlayerData(1);
            if (buffer != null) {
                ByteBuffer BufferForLoadPlayer = ByteBuffer.allocate(buffer.length);
                BufferForLoadPlayer.rewind();
                BufferForLoadPlayer.put(buffer);
                BufferForLoadPlayer.rewind();
                if (IsoPlayer.getInstance() == null) {
                    IsoPlayer.setInstance(new IsoPlayer(IsoCell.getInstance()));
                    IsoPlayer.players[0] = IsoPlayer.getInstance();
                }

                IsoPlayer.getInstance().serverPlayerIndex = 1;
                IsoPlayer.getInstance().load(BufferForLoadPlayer, this.networkProfile.worldVersion[0]);
                return true;
            }
        } catch (Exception var3) {
            ExceptionLogger.logException(var3);
        }

        return false;
    }

    public boolean loadNetworkPlayerInfo(int playerIndex) {
        if (this.networkProfile != null
            && this.networkProfile.isLoaded
            && this.networkProfile.username.equals(GameClient.username)
            && this.networkProfile.server.equals(GameClient.ip)
            && playerIndex >= 1
            && playerIndex <= 4
            && playerIndex <= this.networkProfile.playerCount) {
            int WorldX = PZMath.fastfloor(this.networkProfile.x[playerIndex - 1] / 8.0F) + IsoWorld.saveoffsetx * 30;
            int WorldY = PZMath.fastfloor(this.networkProfile.y[playerIndex - 1] / 8.0F) + IsoWorld.saveoffsety * 30;
            IsoChunkMap.worldXa = PZMath.fastfloor(this.networkProfile.x[playerIndex - 1]);
            IsoChunkMap.worldYa = PZMath.fastfloor(this.networkProfile.y[playerIndex - 1]);
            IsoChunkMap.worldZa = PZMath.fastfloor(this.networkProfile.z[playerIndex - 1]);
            IsoChunkMap.worldXa = IsoChunkMap.worldXa + 300 * IsoWorld.saveoffsetx;
            IsoChunkMap.worldYa = IsoChunkMap.worldYa + 300 * IsoWorld.saveoffsety;
            IsoChunkMap.SWorldX[0] = WorldX;
            IsoChunkMap.SWorldY[0] = WorldY;
            IsoChunkMap.SWorldX[0] = IsoChunkMap.SWorldX[0] + 30 * IsoWorld.saveoffsetx;
            IsoChunkMap.SWorldY[0] = IsoChunkMap.SWorldY[0] + 30 * IsoWorld.saveoffsety;
            return true;
        } else {
            return false;
        }
    }

    public void forgetPlayer(int serverPlayerIndex) {
        if (this.networkProfile != null && serverPlayerIndex >= 1 && serverPlayerIndex <= 4) {
            this.networkProfile.character[serverPlayerIndex - 1] = null;
            this.networkProfile.isDead[serverPlayerIndex - 1] = true;
        }
    }

    public int getNextServerPlayerIndex() {
        if (this.networkProfile != null
            && this.networkProfile.isLoaded
            && this.networkProfile.username.equals(GameClient.username)
            && this.networkProfile.server.equals(GameClient.ip)) {
            for (int i = 1; i < 4; i++) {
                if (this.networkProfile.character[i] == null || this.networkProfile.isDead[i]) {
                    return i + 1;
                }
            }
        }

        return 2;
    }

    public static final class NetworkCharacterProfile {
        public boolean isLoaded = false;
        public final byte[][] character;
        public String username;
        public String server;
        public int playerCount = 0;
        public final int[] worldVersion;
        public final float[] x;
        public final float[] y;
        public final float[] z;
        public final boolean[] isDead;

        public NetworkCharacterProfile() {
            this.character = new byte[4][];
            this.worldVersion = new int[4];
            this.x = new float[4];
            this.y = new float[4];
            this.z = new float[4];
            this.isDead = new boolean[4];
        }
    }
}
