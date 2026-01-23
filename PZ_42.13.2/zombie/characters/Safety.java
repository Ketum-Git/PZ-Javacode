// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.iso.areas.NonPvpZone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;

@UsedFromLua
public class Safety {
    protected boolean enabled;
    protected boolean last;
    protected float cooldown;
    protected float toggle;
    protected IsoGameCharacter character;

    public Safety() {
    }

    public Safety(IsoGameCharacter character) {
        this.character = character;
        this.enabled = true;
        this.last = true;
        this.cooldown = 0.0F;
        this.toggle = 0.0F;
    }

    public void copyFrom(Safety other) {
        this.enabled = other.enabled;
        this.last = other.last;
        this.cooldown = other.cooldown;
        this.toggle = other.toggle;
    }

    public Object getCharacter() {
        return this.character;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLast() {
        return this.last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public float getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(float cooldown) {
        this.cooldown = cooldown;
    }

    public float getToggle() {
        return this.toggle;
    }

    public void setToggle(float toggle) {
        this.toggle = toggle;
    }

    public boolean isToggleAllowed() {
        return ServerOptions.getInstance().pvp.getValue()
            && NonPvpZone.getNonPvpZone(PZMath.fastfloor(this.character.getX()), PZMath.fastfloor(this.character.getY())) == null
            && (!ServerOptions.getInstance().safetySystem.getValue() || this.getCooldown() == 0.0F && this.getToggle() == 0.0F);
    }

    public void toggleSafety() {
        if (this.isToggleAllowed()) {
            if (GameClient.client) {
                GameClient.sendChangeSafety(this);
            } else {
                if (ServerOptions.getInstance().safetyToggleTimer.getValue() == 0) {
                    this.setToggle(1.0E-4F);
                } else {
                    this.setToggle(ServerOptions.getInstance().safetyToggleTimer.getValue());
                }

                this.setLast(this.isEnabled());
                if (this.isEnabled()) {
                    this.setEnabled(!this.isEnabled());
                }

                if (GameServer.server) {
                    GameServer.sendChangeSafety(this);
                }
            }
        }
    }

    public void load(ByteBuffer input, int WorldVersion) {
        this.enabled = input.get() != 0;
        this.last = input.get() != 0;
        this.cooldown = input.getFloat();
        this.toggle = input.getFloat();
    }

    public void save(ByteBuffer output) {
        output.put((byte)(this.enabled ? 1 : 0));
        output.put((byte)(this.last ? 1 : 0));
        output.putFloat(this.cooldown);
        output.putFloat(this.toggle);
    }

    public String getDescription() {
        return "enabled=" + this.enabled + " last=" + this.last + " cooldown=" + this.cooldown + " toggle=" + this.toggle;
    }
}
