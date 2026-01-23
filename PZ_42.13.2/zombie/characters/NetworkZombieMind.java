// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.network.NetworkVariables;
import zombie.network.fields.character.PFBData;
import zombie.network.packets.character.ZombiePacket;
import zombie.pathfind.PathFindBehavior2;

public class NetworkZombieMind {
    private final IsoZombie zombie;
    private final PFBData pfb = new PFBData();
    private boolean shouldRestorePfbTarget;

    public NetworkZombieMind(IsoZombie zombie) {
        this.zombie = zombie;
    }

    public void set(ZombiePacket packet) {
        PathFindBehavior2 pfb = this.zombie.getPathFindBehavior2();
        if (pfb.getIsCancelled()
            || pfb.isGoalNone()
            || pfb.stopping
            || this.zombie.realState == null
            || NetworkVariables.ZombieState.Idle == this.zombie.realState) {
            packet.pfb.goal = PathFindBehavior2.Goal.None;
        } else if (pfb.isGoalCharacter()) {
            if (pfb.getTargetChar() instanceof IsoPlayer player) {
                packet.pfb.goal = PathFindBehavior2.Goal.Character;
                packet.pfb.target.set(player);
            } else {
                packet.pfb.goal = PathFindBehavior2.Goal.None;
                DebugLog.Multiplayer.error("NetworkZombieMind: goal character is not set");
            }
        } else if (pfb.isGoalLocation()) {
            packet.pfb.goal = PathFindBehavior2.Goal.Location;
            packet.pfb.position.set(pfb.getTargetX(), pfb.getTargetY(), pfb.getTargetZ());
        } else if (pfb.isGoalSound()) {
            packet.pfb.goal = PathFindBehavior2.Goal.Sound;
            packet.pfb.position.set(pfb.getTargetX(), pfb.getTargetY(), pfb.getTargetZ());
        }
    }

    public void parse(ZombiePacket packet) {
        if (!packet.pfb.isCanceled()) {
            this.pfb.copy(packet.pfb);
        }
    }

    public void reset() {
        this.pfb.reset();
    }

    public void restorePFBTarget() {
        this.shouldRestorePfbTarget = true;
    }

    public void zombieIdleUpdate() {
        if (this.shouldRestorePfbTarget) {
            this.doRestorePFBTarget();
            this.shouldRestorePfbTarget = false;
        }
    }

    public void doRestorePFBTarget() {
        if (!this.pfb.isCanceled()) {
            if (this.pfb.goal == PathFindBehavior2.Goal.Character && this.pfb.target.getPlayer() != null) {
                this.zombie.pathToCharacter(this.pfb.target.getPlayer());
                this.zombie.spotted(this.pfb.target.getPlayer(), true);
            } else if (this.pfb.goal == PathFindBehavior2.Goal.Location) {
                this.zombie.pathToLocationF(this.pfb.position.getX(), this.pfb.position.getY(), this.pfb.position.getZ());
            } else if (this.pfb.goal == PathFindBehavior2.Goal.Sound) {
                this.zombie
                    .pathToSound(
                        PZMath.fastfloor(this.pfb.position.getX()), PZMath.fastfloor(this.pfb.position.getY()), PZMath.fastfloor(this.pfb.position.getZ())
                    );
                this.zombie.alerted = false;
                this.zombie
                    .setLastHeardSound(
                        PZMath.fastfloor(this.pfb.position.getX()), PZMath.fastfloor(this.pfb.position.getY()), PZMath.fastfloor(this.pfb.position.getZ())
                    );
                this.zombie.allowRepathDelay = 120.0F;
                this.zombie.timeSinceRespondToSound = 0.0F;
            }
        }
    }
}
