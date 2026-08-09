// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.util.Objects;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;
import zombie.network.fields.INetworkPacketField;

public class PlayerVariables implements INetworkPacketField {
    byte count = 0;
    PlayerVariables.NetworkPlayerVariable[] variables = new PlayerVariables.NetworkPlayerVariable[2];

    public PlayerVariables() {
        for (byte i = 0; i < this.variables.length; i++) {
            this.variables[i] = new PlayerVariables.NetworkPlayerVariable();
        }
    }

    public void set(IsoPlayer player) {
        String action = player.getActionStateName();
        if (action.equals("idle")) {
            this.variables[0].set(player, PlayerVariables.NetworkPlayerVariableIDs.IdleSpeed);
            this.count = 1;
        } else if (action.equals("maskingleft")
            || action.equals("maskingright")
            || action.equals("movement")
            || action.equals("run")
            || action.equals("sprint")) {
            this.variables[0].set(player, PlayerVariables.NetworkPlayerVariableIDs.WalkInjury);
            this.variables[1].set(player, PlayerVariables.NetworkPlayerVariableIDs.WalkSpeed);
            this.count = 2;
        }
    }

    public void apply(IsoPlayer player) {
        for (byte i = 0; i < this.count; i++) {
            player.setVariable(this.variables[i].id.name(), this.variables[i].value);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.count = b.getByte();

        for (byte i = 0; i < this.count; i++) {
            this.variables[i].id = b.getEnum(PlayerVariables.NetworkPlayerVariableIDs.class);
            this.variables[i].value = b.getFloat();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.count);

        for (byte i = 0; i < this.count; i++) {
            b.putEnum(this.variables[i].id);
            b.putFloat(this.variables[i].value);
        }
    }

    @Override
    public int getPacketSizeBytes() {
        return 1 + this.count * 5;
    }

    @Override
    public String getDescription() {
        String s = "PlayerVariables: ";
        s = s + "count=" + this.count + " | ";

        for (byte i = 0; i < this.count; i++) {
            s = s + "id=" + this.variables[i].id.name() + ", ";
            s = s + "value=" + this.variables[i].value + " | ";
        }

        return s;
    }

    public void copy(PlayerVariables vars) {
        this.count = vars.count;

        for (byte i = 0; i < this.count; i++) {
            this.variables[i].id = vars.variables[i].id;
            this.variables[i].value = vars.variables[i].value;
        }
    }

    private class NetworkPlayerVariable {
        PlayerVariables.NetworkPlayerVariableIDs id;
        float value;

        private NetworkPlayerVariable() {
            Objects.requireNonNull(PlayerVariables.this);
            super();
        }

        public void set(IsoPlayer player, PlayerVariables.NetworkPlayerVariableIDs id) {
            this.id = id;
            this.value = player.getVariableFloat(id.name(), 0.0F);
        }
    }

    private static enum NetworkPlayerVariableIDs {
        IdleSpeed,
        WalkInjury,
        WalkSpeed,
        DeltaX,
        DeltaY,
        AttackVariationX,
        AttackVariationY,
        targetDist,
        autoShootVarX,
        autoShootVarY,
        recoilVarX,
        recoilVarY,
        ShoveAimX,
        ShoveAimY;
    }
}
