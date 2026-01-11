// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.secure.PZcrypt;
import zombie.core.textures.Texture;

@UsedFromLua
public class Server {
    private int id;
    private String name = "My Server";
    private String ip = "127.0.0.1";
    private String host = "127.0.0.1";
    private String localIp = "";
    private int port = 16262;
    private String serverpwd = "";
    private String description = "";
    private int lastUpdate;
    private String players;
    private String maxPlayers;
    private boolean open;
    private boolean isPublic = true;
    private String version;
    private String mods;
    private boolean passwordProtected;
    private String steamId;
    private String ping = "-1";
    private boolean hosted;
    private boolean needSave;
    private String mapName = "Default";
    private LocalDateTime lastOnline = LocalDateTime.now();
    private LocalDateTime lastDataUpdate = LocalDateTime.now();
    private final ArrayList<Account> accounts = new ArrayList<>();
    private Texture serverIcon;
    private Texture serverLoginScreen;
    private Texture serverLoadingScreen;
    private int serverCustomizationLastUpdate;
    private boolean isFeatured;
    private boolean isResponded;

    public int getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public boolean getNeedSave() {
        return this.needSave;
    }

    public void setNeedSave(boolean needSave) {
        this.needSave = needSave;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp2() {
        if (this.ip == null) {
            InetAddress IpAddress = null;

            try {
                IpAddress = InetAddress.getByName(this.host);
            } catch (UnknownHostException var3) {
                throw new RuntimeException(var3);
            }

            this.ip = IpAddress.getHostAddress();
        }

        return this.ip;
    }

    public String getIp() {
        return this.host;
    }

    public void setIp(String ip) {
        this.ip = null;
        this.host = ip;
    }

    public String getLocalIP() {
        return this.localIp;
    }

    public void setLocalIP(String ip) {
        this.localIp = ip;
    }

    public String getServerPassword() {
        return this.serverpwd;
    }

    public void setServerPassword(String pwd) {
        this.serverpwd = pwd == null ? "" : pwd;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getLastOnline() {
        return this.lastOnline;
    }

    public void setLastOnline(LocalDateTime lastOnline) {
        this.lastOnline = lastOnline;
    }

    public void setLastOnlineNow() {
        this.lastOnline = LocalDateTime.now();
    }

    public LocalDateTime getLastDataUpdate() {
        return this.lastDataUpdate;
    }

    public void setLastDataUpdate(LocalDateTime lastDataUpdate) {
        this.lastDataUpdate = lastDataUpdate;
    }

    public void setLastDataUpdateNow() {
        this.lastDataUpdate = LocalDateTime.now();
    }

    public ArrayList<Account> getAccounts() {
        return this.accounts;
    }

    public void addAccount(Account account) {
        this.accounts.add(account);
    }

    public void addAccount(String username, String password, boolean savePwd, boolean userSteamRelay, int authType) {
        Account account = new Account();
        account.setUserName(username);
        account.setPwd(password);
        account.setSavePwd(savePwd);
        account.setUseSteamRelay(userSteamRelay);
        account.setAuthType(authType);
        this.accounts.add(account);
    }

    public void removeAccount(Account account) {
        this.accounts.remove(account);
    }

    @Deprecated
    public String getUserName() {
        return !this.accounts.isEmpty() ? this.accounts.get(0).getUserName() : "";
    }

    @Deprecated
    public void setUserName(String userName) {
        if (this.accounts.isEmpty()) {
            Account account = new Account();
            this.accounts.add(account);
        }

        this.accounts.get(0).setUserName(userName);
    }

    @Deprecated
    public String getPwd() {
        return !this.accounts.isEmpty() ? this.accounts.get(0).getPwd() : "";
    }

    @Deprecated
    public void setPwd(String pwd) {
        if (this.accounts.isEmpty()) {
            Account account = new Account();
            this.accounts.add(account);
        }

        this.accounts.get(0).setPwd(pwd);
    }

    public void setPwd(String pwd, boolean hashed) {
        if (hashed) {
            this.setPwd(PZcrypt.hash(ServerWorldDatabase.encrypt(pwd)));
        } else {
            this.setPwd(pwd);
        }
    }

    @Deprecated
    public boolean getUseSteamRelay() {
        return !this.accounts.isEmpty() ? this.accounts.get(0).getUseSteamRelay() : true;
    }

    @Deprecated
    public void setUseSteamRelay(boolean useSteamRelay) {
        if (this.accounts.isEmpty()) {
            Account account = new Account();
            this.accounts.add(account);
        }

        this.accounts.get(0).setUseSteamRelay(useSteamRelay);
    }

    public int getLastUpdate() {
        return this.lastUpdate;
    }

    public void setLastUpdate(int lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getPlayers() {
        return this.players;
    }

    public void setPlayers(String players) {
        this.players = players;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public boolean isPublic() {
        return this.isPublic;
    }

    public void setPublic(boolean bPublic) {
        this.isPublic = bPublic;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMaxPlayers() {
        return this.maxPlayers;
    }

    public void setMaxPlayers(String maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getMods() {
        return this.mods;
    }

    public void setMods(String mods) {
        this.mods = mods;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPing() {
        return this.ping;
    }

    public void setPing(String ping) {
        this.ping = ping;
    }

    public boolean isPasswordProtected() {
        return this.passwordProtected;
    }

    public void setPasswordProtected(boolean pp) {
        this.passwordProtected = pp;
    }

    public String getSteamId() {
        return this.steamId;
    }

    public void setSteamId(String steamId) {
        this.steamId = steamId;
    }

    public boolean isHosted() {
        return this.hosted;
    }

    public void setHosted(boolean hosted) {
        this.hosted = hosted;
    }

    @Deprecated
    public boolean isSavePwd() {
        return !this.accounts.isEmpty() ? this.accounts.get(0).isSavePwd() : true;
    }

    @Deprecated
    public void setSavePwd(boolean savePwd) {
        if (this.accounts.isEmpty()) {
            Account account = new Account();
            this.accounts.add(account);
        }

        this.accounts.get(0).setSavePwd(savePwd);
    }

    @Deprecated
    public int getAuthType() {
        return !this.accounts.isEmpty() ? this.accounts.get(0).getAuthType() : 1;
    }

    @Deprecated
    public void setAuthType(int authType) {
        if (this.accounts.isEmpty()) {
            Account account = new Account();
            this.accounts.add(account);
        }

        this.accounts.get(0).setAuthType(authType);
    }

    public void setServerIcon(Texture serverIcon) {
        this.serverIcon = serverIcon;
    }

    public void setServerLoadingScreen(Texture serverLoadingScreen) {
        this.serverLoadingScreen = serverLoadingScreen;
    }

    public void setServerLoginScreen(Texture serverLoginScreen) {
        this.serverLoginScreen = serverLoginScreen;
    }

    public String getMapName() {
        return this.mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public Texture getServerIcon() {
        return this.serverIcon;
    }

    public Texture getServerLoadingScreen() {
        return this.serverLoadingScreen;
    }

    public Texture getServerLoginScreen() {
        return this.serverLoginScreen;
    }

    public int getServerCustomizationLastUpdate() {
        return this.serverCustomizationLastUpdate;
    }

    public int getTimeFromServerCustomizationLastUpdate() {
        return this.serverCustomizationLastUpdate == 0 ? 1000000 : (int)(System.currentTimeMillis() / 1000L - this.serverCustomizationLastUpdate);
    }

    public void setServerCustomizationLastUpdate(int serverCustomizationLastUpdate) {
        this.serverCustomizationLastUpdate = serverCustomizationLastUpdate;
    }

    public void updateServerCustomizationLastUpdate() {
        this.serverCustomizationLastUpdate = (int)(System.currentTimeMillis() / 1000L);
    }

    public boolean isFeatured() {
        return this.isFeatured;
    }

    public void setFeatured(boolean featured) {
        this.isFeatured = featured;
    }

    public boolean isResponded() {
        return this.isResponded;
    }

    public void setResponded(boolean responded) {
        this.isResponded = responded;
    }
}
