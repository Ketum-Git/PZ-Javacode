// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.iso.IsoMovingObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.Type;

@UsedFromLua
public class Role {
    private static final int maxName = 64;
    private static final int maxDescription = 512;
    private int id = -1;
    private String name;
    private String description;
    private Color color;
    private boolean isReadOnly;
    private int position = -1;
    private final HashSet<Capability> capabilities = new HashSet<>();

    public Role(String name) {
        this.name = name;
        this.description = "";
        this.color = Color.white;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name.substring(0, Math.min(name.length(), 64));
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String v) {
        this.description = v.substring(0, Math.min(v.length(), 512));
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color v) {
        this.color = v;
    }

    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    public void setReadOnly() {
        this.isReadOnly = true;
    }

    public int getPosition() {
        return this.position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public ArrayList<String> getDefaults() {
        ArrayList<String> ret = new ArrayList<>();
        if (Roles.getDefaultForBanned() == this) {
            ret.add("Banned");
        }

        if (Roles.getDefaultForUser() == this) {
            ret.add("User");
        }

        if (Roles.getDefaultForNewUser() == this) {
            ret.add("NewUser");
        }

        if (Roles.getDefaultForPriorityUser() == this) {
            ret.add("PriorityUser");
        }

        if (Roles.getDefaultForObserver() == this) {
            ret.add("Observer");
        }

        if (Roles.getDefaultForGM() == this) {
            ret.add("GM");
        }

        if (Roles.getDefaultForOverseer() == this) {
            ret.add("Oversee");
        }

        if (Roles.getDefaultForModerator() == this) {
            ret.add("Moderator");
        }

        if (Roles.getDefaultForAdmin() == this) {
            ret.add("Admin");
        }

        return ret;
    }

    public boolean addCapability(Capability capability) {
        return this.isReadOnly ? false : this.capabilities.add(capability);
    }

    public boolean removeCapability(Capability capability) {
        return this.isReadOnly ? false : this.capabilities.remove(capability);
    }

    public void cleanCapability() {
        if (!this.isReadOnly) {
            this.capabilities.clear();
        }
    }

    public HashSet<Capability> getCapabilities() {
        return this.capabilities;
    }

    public void send(ByteBuffer output) {
        GameWindow.WriteStringUTF(output, this.name);
        GameWindow.WriteStringUTF(output, this.description);
        output.putFloat(this.color.r);
        output.putFloat(this.color.g);
        output.putFloat(this.color.b);
        output.putFloat(this.color.a);
        output.putInt(this.position);
        output.put((byte)this.capabilities.size());

        for (Capability c : this.capabilities) {
            output.put((byte)c.ordinal());
        }

        output.put((byte)(this.isReadOnly ? 1 : 0));
    }

    public void parse(ByteBuffer input) {
        this.setName(GameWindow.ReadString(input));
        this.setDescription(GameWindow.ReadString(input));
        this.color = new Color();
        this.color.r = input.getFloat();
        this.color.g = input.getFloat();
        this.color.b = input.getFloat();
        this.color.a = input.getFloat();
        this.position = input.getInt();
        this.capabilities.clear();
        byte size = input.get();

        for (int i = 0; i < size; i++) {
            byte id = input.get();
            if (id > Capability.values().length) {
                DebugLog.General.printStackTrace("Role.load error. id=" + id);
            }

            Capability c = Capability.values()[id];
            if (c != null) {
                this.capabilities.add(c);
            }
        }

        this.isReadOnly = input.get() > 0;
    }

    public static boolean hasCapability(IsoMovingObject target, Capability capability) {
        if (isUsingDebugMode()) {
            return true;
        } else {
            IsoPlayer player = Type.tryCastTo(target, IsoPlayer.class);
            return player != null && player.getRole() != null ? player.getRole().hasCapability(capability) : false;
        }
    }

    public boolean hasCapability(Capability capability) {
        return isUsingDebugMode() || this.capabilities.contains(capability);
    }

    public static boolean isUsingDebugMode() {
        return Core.debug && !GameClient.client && !GameServer.server;
    }

    public boolean hasAdminTool() {
        return this.hasAdminPower()
            || this.hasCapability(Capability.CanSeePlayersStats)
            || this.hasCapability(Capability.AddItem)
            || this.hasCapability(Capability.SeePublicServerOptions)
            || this.hasCapability(Capability.CanSetupNonPVPZone)
            || this.hasCapability(Capability.FactionCheat)
            || this.hasCapability(Capability.RolesRead)
            || this.hasCapability(Capability.SeeNetworkUsers)
            || this.hasCapability(Capability.CanSetupSafehouses)
            || this.hasCapability(Capability.AnswerTickets)
            || this.hasCapability(Capability.SeePlayersConnected)
            || this.hasCapability(Capability.GetStatistic)
            || this.hasCapability(Capability.SandboxOptions)
            || this.hasCapability(Capability.ClimateManager)
            || this.hasCapability(Capability.GetStatistic)
            || this.hasCapability(Capability.PVPLogTool);
    }

    public boolean hasAdminPower() {
        return this.hasCapability(Capability.ToggleInvisibleHimself)
            || this.hasCapability(Capability.ToggleInvincibleHimself)
            || this.hasCapability(Capability.ToggleGodModHimself)
            || this.hasCapability(Capability.ToggleNoclipHimself)
            || this.hasCapability(Capability.UseTimedActionInstantCheat)
            || this.hasCapability(Capability.ToggleUnlimitedCarry)
            || this.hasCapability(Capability.ToggleUnlimitedEndurance)
            || this.hasCapability(Capability.ToggleKnowAllRecipes)
            || this.hasCapability(Capability.ToggleUnlimitedAmmo)
            || this.hasCapability(Capability.UseFastMoveCheat)
            || this.hasCapability(Capability.UseBuildCheat)
            || this.hasCapability(Capability.UseFarmingCheat)
            || this.hasCapability(Capability.UseFishingCheat)
            || this.hasCapability(Capability.UseHealthCheat)
            || this.hasCapability(Capability.UseMechanicsCheat)
            || this.hasCapability(Capability.UseMovablesCheat)
            || this.hasCapability(Capability.CanSeeAll)
            || this.hasCapability(Capability.CanHearAll)
            || this.hasCapability(Capability.ManipulateZombie)
            || this.hasCapability(Capability.GetStatistic)
            || this.hasCapability(Capability.UseBrushToolManager)
            || this.hasCapability(Capability.UseLootTool);
    }
}
