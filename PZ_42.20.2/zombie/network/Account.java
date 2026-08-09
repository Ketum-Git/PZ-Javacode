// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.time.LocalDateTime;
import zombie.UsedFromLua;
import zombie.core.secure.PZcrypt;
import zombie.core.textures.Texture;
import zombie.savefile.AccountDBHelper;

@UsedFromLua
public class Account {
    private int id;
    private String userName = "";
    private String pwd = "";
    private int authType = 1;
    private boolean useSteamRelay;
    private boolean savePwd = true;
    private String playerFirstAndLastName;
    private Texture icon;
    private int timePlayed;
    private LocalDateTime lastLogon = LocalDateTime.now();

    public int getID() {
        return this.id;
    }

    public void setID(int id) {
        this.id = id;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPlayerFirstAndLastName() {
        return this.playerFirstAndLastName;
    }

    public void setPlayerFirstAndLastName(String name) {
        this.playerFirstAndLastName = name;
    }

    public String getPwd() {
        return this.pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void encryptPwd(String pwd) {
        this.pwd = PZcrypt.hash(ServerWorldDatabase.encrypt(pwd));
    }

    public boolean getUseSteamRelay() {
        return this.useSteamRelay;
    }

    public void setUseSteamRelay(boolean useSteamRelay) {
        this.useSteamRelay = useSteamRelay;
    }

    public boolean isSavePwd() {
        return this.savePwd;
    }

    public void setSavePwd(boolean savePwd) {
        this.savePwd = savePwd;
    }

    public int getAuthType() {
        return this.authType;
    }

    public void setAuthType(int authType) {
        this.authType = authType;
    }

    public int getTimePlayed() {
        return this.timePlayed;
    }

    public void setTimePlayed(int timePlayed) {
        this.timePlayed = timePlayed;
    }

    public String getLastLogon() {
        return this.lastLogon == null ? null : this.lastLogon.format(AccountDBHelper.formatter);
    }

    public void setLastLogon(LocalDateTime lastLogon) {
        this.lastLogon = lastLogon;
    }

    public void setLastLogonNow() {
        this.lastLogon = LocalDateTime.now();
    }

    public Texture getIcon() {
        return this.icon;
    }

    public void setIcon(Texture icon) {
        this.icon = icon;
    }
}
