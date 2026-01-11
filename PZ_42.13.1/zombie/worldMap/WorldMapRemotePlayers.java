// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.characters.IsoPlayer;

public final class WorldMapRemotePlayers {
    public static final WorldMapRemotePlayers instance = new WorldMapRemotePlayers();
    private final ArrayList<WorldMapRemotePlayer> playerList = new ArrayList<>();
    private final HashMap<Short, WorldMapRemotePlayer> playerLookup = new HashMap<>();

    public WorldMapRemotePlayer getOrCreatePlayerByID(short playerID) {
        WorldMapRemotePlayer remotePlayer = this.playerLookup.get(playerID);
        if (remotePlayer == null) {
            remotePlayer = new WorldMapRemotePlayer(playerID);
            this.playerList.add(remotePlayer);
            this.playerLookup.put(playerID, remotePlayer);
        }

        return remotePlayer;
    }

    public WorldMapRemotePlayer getOrCreatePlayer(IsoPlayer player) {
        return this.getOrCreatePlayerByID(player.onlineId);
    }

    public WorldMapRemotePlayer getPlayerByID(short ID) {
        return this.playerLookup.get(ID);
    }

    public ArrayList<WorldMapRemotePlayer> getPlayers() {
        return this.playerList;
    }

    public void removePlayerByID(short playerID) {
        this.playerList.removeIf(remotePlayer -> remotePlayer.getOnlineID() == playerID);
        this.playerLookup.remove(playerID);
    }

    public void Reset() {
        this.playerList.clear();
        this.playerLookup.clear();
    }
}
