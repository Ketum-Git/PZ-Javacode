// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.znet;

public class GameServerDetails {
    public String address;
    public int port;
    public long steamId;
    public String name;
    public String gamedir;
    public String map;
    public String gameDescription;
    public String tags;
    public int ping;
    public int numPlayers;
    public int maxPlayers;
    public boolean passwordProtected;
    public int version;

    public GameServerDetails() {
    }

    public GameServerDetails(
        String address,
        int port,
        long steamId,
        String name,
        String gamedir,
        String map,
        String gameDescription,
        String tags,
        int ping,
        int numPlayers,
        int maxPlayers,
        boolean passwordProtected,
        int version
    ) {
        this.address = address;
        this.port = port;
        this.steamId = steamId;
        this.name = name;
        this.gamedir = gamedir;
        this.map = map;
        this.gameDescription = gameDescription;
        this.tags = tags;
        this.ping = ping;
        this.numPlayers = numPlayers;
        this.maxPlayers = maxPlayers;
        this.passwordProtected = passwordProtected;
        this.version = version;
    }
}
