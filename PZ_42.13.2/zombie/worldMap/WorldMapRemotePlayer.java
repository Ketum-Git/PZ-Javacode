// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap;

import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.network.GameClient;
import zombie.network.ServerOptions;

public final class WorldMapRemotePlayer {
    private short changeCount;
    private final short onlineId;
    private String username = "???";
    private String forename = "???";
    private String surname = "???";
    private String accessLevel = "None";
    private int rolePower;
    private float x;
    private float y;
    private boolean invisible;
    private boolean disguised;
    private boolean hasFullData;

    public WorldMapRemotePlayer(short onlineId) {
        this.onlineId = onlineId;
    }

    public void setPlayer(IsoPlayer player) {
        boolean changed = false;
        if (!this.username.equals(player.username)) {
            this.username = player.username;
            changed = true;
        }

        if (!this.forename.equals(player.getDescriptor().getForename())) {
            this.forename = player.getDescriptor().getForename();
            changed = true;
        }

        if (!this.surname.equals(player.getDescriptor().getSurname())) {
            this.surname = player.getDescriptor().getSurname();
            changed = true;
        }

        if (!this.accessLevel.equals(player.getRole().getName())) {
            this.accessLevel = player.getRole().getName();
            this.rolePower = player.getRole().getCapabilities().size();
            changed = true;
        }

        this.x = player.getX();
        this.y = player.getY();
        if (this.invisible != player.isInvisible()) {
            this.invisible = player.isInvisible();
            changed = true;
        }

        if (this.disguised != player.usernameDisguised) {
            this.disguised = player.usernameDisguised;
            changed = true;
        }

        if (changed) {
            this.changeCount++;
        }
    }

    public void setFullData(
        short changeCount,
        String username,
        String forename,
        String surname,
        String accessLevel,
        int rolePower,
        float x,
        float y,
        boolean invisible,
        boolean disguised
    ) {
        this.changeCount = changeCount;
        this.username = username;
        this.forename = forename;
        this.surname = surname;
        this.accessLevel = accessLevel;
        this.rolePower = rolePower;
        this.x = x;
        this.y = y;
        this.invisible = invisible;
        this.disguised = disguised;
        this.hasFullData = true;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public short getOnlineID() {
        return this.onlineId;
    }

    public String getForename() {
        return this.forename;
    }

    public String getSurname() {
        return this.surname;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public short getChangeCount() {
        return this.changeCount;
    }

    public boolean isInvisible() {
        return this.invisible;
    }

    public boolean isDisguised() {
        return this.disguised;
    }

    public boolean hasFullData() {
        return this.hasFullData;
    }

    public String getUsername(Boolean canShowFirstname) {
        String nameStr = this.username;
        if (this.disguised) {
            nameStr = Translator.getText("IGUI_Disguised_Player_Name");
        } else if (canShowFirstname && GameClient.client && ServerOptions.instance.showFirstAndLastName.getValue() && this.isAccessLevel("None")) {
            nameStr = this.forename + " " + this.surname;
            if (ServerOptions.instance.displayUserName.getValue()) {
                nameStr = nameStr + " (" + this.username + ")";
            }
        }

        return nameStr;
    }

    public String getUsername() {
        return this.getUsername(false);
    }

    public String getAccessLevel() {
        String var1 = this.accessLevel;

        return switch (var1) {
            case "admin" -> "Admin";
            case "moderator" -> "Moderator";
            case "overseer" -> "Overseer";
            case "gm" -> "GM";
            case "observer" -> "Observer";
            default -> "None";
        };
    }

    public String getAccessLevel2() {
        return this.accessLevel;
    }

    public int getRolePower() {
        return this.rolePower;
    }

    public boolean isAccessLevel(String level) {
        return this.getAccessLevel().equalsIgnoreCase(level);
    }
}
