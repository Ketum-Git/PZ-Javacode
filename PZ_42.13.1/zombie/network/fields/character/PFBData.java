// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.character;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.JSONField;
import zombie.network.NetworkVariables;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.Position;
import zombie.pathfind.PathFindBehavior2;

public class PFBData implements INetworkPacketField {
    @JSONField
    public PathFindBehavior2.Goal goal = PathFindBehavior2.Goal.None;
    @JSONField
    public final PlayerID target = new PlayerID();
    @JSONField
    public final Position position = new Position();

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.goal = PathFindBehavior2.Goal.fromByte(b.get());
        switch (this.goal) {
            case Character:
                this.target.parse(b, connection);
                break;
            case Location:
            case Sound:
                this.position.set(b.getFloat(), b.getFloat(), b.getFloat());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put((byte)this.goal.ordinal());
        switch (this.goal) {
            case Character:
                this.target.write(b);
                break;
            case Location:
            case Sound:
                this.position.write(b);
        }
    }

    public void copy(PFBData other) {
        this.goal = other.goal;
        this.target.copy(other.target);
        this.position.copy(other.position);
    }

    public boolean isCanceled() {
        return PathFindBehavior2.Goal.None == this.goal;
    }

    public void reset() {
        this.goal = PathFindBehavior2.Goal.None;
    }

    public void set(IsoGameCharacter character) {
        PathFindBehavior2 pfb = character.getPathFindBehavior2();
        character.realState = NetworkVariables.ZombieState.fromString(character.getAdvancedAnimator().getCurrentStateName());
        if (pfb.getIsCancelled() || pfb.isGoalNone() || pfb.stopping || NetworkVariables.ZombieState.Idle == character.realState) {
            this.goal = PathFindBehavior2.Goal.None;
        } else if (pfb.isGoalCharacter()) {
            if (pfb.getTargetChar() instanceof IsoPlayer player) {
                this.goal = PathFindBehavior2.Goal.Character;
                this.target.set(player);
            } else {
                this.goal = PathFindBehavior2.Goal.None;
                DebugLog.Multiplayer.warn("NetworkZombieMind: goal character is not set");
            }
        } else if (pfb.isGoalLocation()) {
            this.goal = PathFindBehavior2.Goal.Location;
            this.position.set(pfb.getTargetX(), pfb.getTargetY(), pfb.getTargetZ());
        } else if (pfb.isGoalSound()) {
            this.goal = PathFindBehavior2.Goal.Sound;
            this.position.set(pfb.getTargetX(), pfb.getTargetY(), pfb.getTargetZ());
        }
    }

    public void restore(IsoGameCharacter character) {
        character.setPath2(null);
        switch (this.goal) {
            case Character:
                character.pathToCharacter(this.target.getPlayer());
                character.spotted(this.target.getPlayer(), true);
                break;
            case Location:
                character.pathToLocationF(this.position.getX(), this.position.getY(), this.position.getZ());
            case Sound:
            case None:
        }
    }
}
