// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import zombie.ZomboidFileSystem;
import zombie.core.Core;
import zombie.core.logger.LimitSizeFileOutputStream;
import zombie.core.znet.PortMapper;
import zombie.core.znet.SteamGameServer;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;

public class CoopSlave {
    private static PrintStream stdout;
    private static PrintStream stderr;
    private final Pattern serverMessageParser;
    private long nextPing = -1L;
    private long lastPong = -1L;
    public static CoopSlave instance;
    public String hostUser;
    public long hostSteamId;
    private boolean masterLost;
    private final HashSet<Long> invites = new HashSet<>();
    private Long serverSteamId;

    public static void init() throws FileNotFoundException {
        instance = new CoopSlave();
    }

    public static void initStreams() throws FileNotFoundException {
        String path = ZomboidFileSystem.instance.getCacheDir() + File.separator + "coop-console.txt";
        LimitSizeFileOutputStream fileOut = new LimitSizeFileOutputStream(new File(path), Core.getInstance().getConsoleDotTxtSizeKB());
        stdout = System.out;
        stderr = System.err;
        System.setOut(new PrintStream(fileOut));
        System.setErr(System.out);
    }

    private CoopSlave() {
        this.serverMessageParser = Pattern.compile("^([\\-\\w]+)(\\[(\\d+)\\])?@(.*)$");
        this.notify("coop mode enabled");
        if (System.getProperty("hostUser") != null) {
            this.hostUser = System.getProperty("zomboid.hostUser").trim();
        }
    }

    public synchronized void notify(String notification) {
        this.sendMessage("info", null, notification);
    }

    public synchronized void sendStatus(String status) {
        this.sendMessage("status", null, status);
    }

    public static void status(String status) {
        if (instance != null) {
            instance.sendStatus(status);
        }
    }

    public synchronized void sendMessage(String message) {
        this.sendMessage("message", null, message);
    }

    public synchronized void sendMessage(String tag, String cookie, String payload) {
        if (cookie != null) {
            stdout.println(tag + "[" + cookie + "]@" + payload);
        } else {
            stdout.println(tag + "@" + payload);
        }
    }

    public void sendExternalIPAddress(String cookie) {
        this.sendMessage("get-parameter", cookie, PortMapper.getExternalAddress());
    }

    public synchronized void sendSteamID(String cookie) {
        if (this.serverSteamId == null && SteamUtils.isSteamModeEnabled()) {
            this.serverSteamId = SteamGameServer.GetSteamID();
        }

        this.sendMessage("get-parameter", cookie, this.serverSteamId.toString());
    }

    public boolean handleCommand(String command) {
        Matcher matcher = this.serverMessageParser.matcher(command);
        if (matcher.find()) {
            String tag = matcher.group(1);
            String cookie = matcher.group(3);
            String payload = matcher.group(4);
            if (Objects.equals(tag, "set-host-user")) {
                System.out.println("Set host user:" + payload);
                this.hostUser = payload;
            }

            if (Objects.equals(tag, "set-host-steamid")) {
                this.hostSteamId = SteamUtils.convertStringToSteamID(payload);
            }

            if (Objects.equals(tag, "invite-add")) {
                Long friendSteamID = SteamUtils.convertStringToSteamID(payload);
                if (friendSteamID != -1L) {
                    this.invites.add(friendSteamID);
                }
            }

            if (Objects.equals(tag, "invite-remove")) {
                Long friendSteamID = SteamUtils.convertStringToSteamID(payload);
                if (friendSteamID != -1L) {
                    this.invites.remove(friendSteamID);
                }
            }

            if (Objects.equals(tag, "get-parameter")) {
                DebugLog.log("Got get-parameter command: tag = " + tag + " payload = " + payload);
                if (Objects.equals(payload, "external-ip")) {
                    this.sendExternalIPAddress(cookie);
                } else if (Objects.equals(payload, "steam-id")) {
                    this.sendSteamID(cookie);
                }
            }

            if (Objects.equals(tag, "ping")) {
                this.lastPong = System.currentTimeMillis();
            }

            if (Objects.equals(tag, "process-status") && Objects.equals(payload, "eof")) {
                DebugLog.log("Master connection lost: EOF");
                this.masterLost = true;
            }

            return true;
        } else {
            DebugLog.log("Got malformed command: " + command);
            return false;
        }
    }

    public String getHostUser() {
        return this.hostUser;
    }

    public void update() {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= this.nextPing) {
            this.sendMessage("ping", null, "ping");
            this.nextPing = currentTime + 5000L;
        }

        long timeout = 60000L;
        if (this.lastPong == -1L) {
            this.lastPong = currentTime;
        }

        this.masterLost = this.masterLost || currentTime - this.lastPong > 60000L;
    }

    public boolean masterLost() {
        return this.masterLost;
    }

    public boolean isHost(long steamID) {
        return steamID == this.hostSteamId;
    }

    public boolean isInvited(long friendSteamID) {
        return this.invites.contains(friendSteamID);
    }
}
