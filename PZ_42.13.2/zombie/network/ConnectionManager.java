// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.util.ArrayDeque;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.core.secure.PZcrypt;
import zombie.debug.DebugLog;

public class ConnectionManager {
    private static final ConnectionManager instance = new ConnectionManager();
    final ArrayDeque<ConnectionManager.Request> connectionRequests = new ArrayDeque<>();

    public static ConnectionManager getInstance() {
        return instance;
    }

    public void ping(String username, String pwd, String ip, String port, boolean doHash) {
        synchronized (this.connectionRequests) {
            this.connectionRequests
                .add(new ConnectionManager.Request(ConnectionManager.RequestType.askPing, username, pwd, ip, "", port, "", "", false, doHash, 1, ""));
        }

        getInstance().process();
    }

    public void stopPing() {
        GameClient.askPing = false;
    }

    public void getCustomizationData(String username, String pwd, String ip, String port, String serverPassword, String serverName, boolean doHash) {
        synchronized (this.connectionRequests) {
            for (ConnectionManager.Request r : this.connectionRequests) {
                if (r.server.equals(ip)) {
                    return;
                }
            }

            this.connectionRequests
                .push(
                    new ConnectionManager.Request(
                        ConnectionManager.RequestType.askCustomizationData, username, pwd, ip, "", port, serverPassword, serverName, false, doHash, 1, ""
                    )
                );
        }

        getInstance().process();
    }

    public void sendSecretKey(String username, String pwd, String ip, int port, String serverPassword, boolean doHash, int authType, String secretKey) {
        synchronized (this.connectionRequests) {
            this.connectionRequests.removeIf(r -> r.type == ConnectionManager.RequestType.askCustomizationData);
            this.connectionRequests
                .push(
                    new ConnectionManager.Request(
                        ConnectionManager.RequestType.sendQR,
                        username,
                        pwd,
                        ip,
                        "",
                        String.valueOf(port),
                        serverPassword,
                        "",
                        false,
                        doHash,
                        authType,
                        secretKey
                    )
                );
        }

        getInstance().process();
    }

    public void serverConnect(
        String username,
        String pwd,
        String server,
        String localIP,
        String port,
        String serverPassword,
        String serverName,
        boolean useSteamRelay,
        boolean doHash,
        int authType,
        String secretKey
    ) {
        synchronized (this.connectionRequests) {
            this.connectionRequests.removeIf(r -> r.type == ConnectionManager.RequestType.askCustomizationData);
            this.connectionRequests
                .push(
                    new ConnectionManager.Request(
                        ConnectionManager.RequestType.connect,
                        username,
                        pwd,
                        server,
                        localIP,
                        port,
                        serverPassword,
                        serverName,
                        useSteamRelay,
                        doHash,
                        authType,
                        secretKey
                    )
                );
        }

        getInstance().process();
    }

    public void serverConnectCoop(String serverSteamID) {
        synchronized (this.connectionRequests) {
            this.connectionRequests.removeIf(r -> r.type == ConnectionManager.RequestType.askCustomizationData);
            this.connectionRequests
                .push(new ConnectionManager.Request(ConnectionManager.RequestType.connectCoop, "", "", serverSteamID, "", "", "", "", true, false, 1, ""));
        }

        getInstance().process();
    }

    public void clearQueue() {
        synchronized (this.connectionRequests) {
            this.connectionRequests.clear();
        }
    }

    public void process() {
        if (GameClient.connection == null) {
            ConnectionManager.Request r;
            synchronized (this.connectionRequests) {
                if (this.connectionRequests.isEmpty()) {
                    return;
                }

                r = this.connectionRequests.poll();
            }

            switch (r.type) {
                case connect:
                    GameClient.askPing = false;
                    GameClient.askCustomizationData = false;
                    GameClient.sendQR = false;
                    doServerConnect(
                        r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey
                    );
                    break;
                case connectCoop:
                    GameClient.askPing = false;
                    GameClient.askCustomizationData = false;
                    GameClient.sendQR = false;
                    doServerConnectCoop(r.server);
                    break;
                case askPing:
                    GameClient.askPing = true;
                    GameClient.askCustomizationData = false;
                    GameClient.sendQR = false;
                    doServerConnect(
                        r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey
                    );
                    break;
                case askCustomizationData:
                    GameClient.askPing = false;
                    GameClient.askCustomizationData = true;
                    GameClient.sendQR = false;
                    doServerConnect(
                        r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey
                    );
                    break;
                case sendQR:
                    GameClient.askPing = false;
                    GameClient.askCustomizationData = false;
                    GameClient.sendQR = true;
                    doServerConnect(
                        r.user, r.pass, r.server, r.localIp, r.port, r.serverPassword, r.serverName, r.useSteamRelay, r.doHash, r.authtype, r.secretKey
                    );
            }
        }
    }

    public static void log(String event, String message, UdpConnection connection) {
        DebugLog.Multiplayer.println("connection: %s [%s] \"%s\"", connection, event, message);
    }

    public static void doServerConnect(
        String user,
        String pass,
        String server,
        String localIP,
        String port,
        String serverPassword,
        String serverName,
        boolean useSteamRelay,
        boolean doHash,
        int authtype,
        String secretKey
    ) {
        Core.getInstance().setGameMode("Multiplayer");
        Core.setDifficulty("Hardcore");
        if (GameClient.connection != null) {
            GameClient.connection.forceDisconnect("lua-connect");
        }

        if (!GameClient.askCustomizationData) {
            GameClient.instance.resetDisconnectTimer();
        }

        GameClient.client = true;
        GameClient.clientSave = true;
        GameClient.coopInvite = false;
        ZomboidFileSystem.instance.cleanMultiplayerSaves();
        if (doHash) {
            GameClient.instance
                .doConnect(
                    user,
                    PZcrypt.hash(ServerWorldDatabase.encrypt(pass)),
                    server,
                    localIP,
                    port,
                    serverPassword,
                    serverName,
                    useSteamRelay,
                    authtype,
                    secretKey
                );
        } else {
            GameClient.instance.doConnect(user, pass, server, localIP, port, serverPassword, serverName, useSteamRelay, authtype, secretKey);
        }
    }

    public static void doServerConnectCoop(String serverSteamID) {
        Core.getInstance().setGameMode("Multiplayer");
        Core.setDifficulty("Hardcore");
        if (GameClient.connection != null) {
            GameClient.connection.forceDisconnect("lua-connect-coop");
        }

        GameClient.client = true;
        GameClient.clientSave = true;
        GameClient.coopInvite = true;
        GameClient.instance.doConnectCoop(serverSteamID);
    }

    private static class Request {
        ConnectionManager.RequestType type;
        String user;
        String pass;
        String server;
        String localIp;
        String port;
        String serverPassword;
        String serverName;
        boolean useSteamRelay;
        boolean doHash;
        int authtype;
        String secretKey;

        public Request(
            ConnectionManager.RequestType type,
            String user,
            String pass,
            String server,
            String localIp,
            String port,
            String serverPassword,
            String serverName,
            boolean useSteamRelay,
            boolean doHash,
            int authtype,
            String secretKey
        ) {
            this.type = type;
            this.user = user;
            this.pass = pass;
            this.server = server;
            this.localIp = localIp;
            this.port = port;
            this.serverPassword = serverPassword;
            this.serverName = serverName;
            this.useSteamRelay = useSteamRelay;
            this.doHash = doHash;
            this.authtype = authtype;
            this.secretKey = secretKey;
        }
    }

    private static enum RequestType {
        connect,
        connectCoop,
        askPing,
        askCustomizationData,
        sendQR;
    }
}
