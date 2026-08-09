// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.component;

import zombie.ai.AIBrainPlayerControlVars;
import zombie.characters.IsoPlayer;
import zombie.characters.ecs.ECSComponent;

public class AIComponent extends ECSComponent {
    private final AIBrainPlayerControlVars humanControlVars = new AIBrainPlayerControlVars();

    public boolean doUpdatePlayerControls(IsoPlayer ownerPlayer) {
        ownerPlayer.bannedAttacking = this.humanControlVars.bannedAttacking;
        return this.humanControlVars.melee;
    }

    public void update() {
    }

    public void postUpdatePlayer(IsoPlayer ownerPlayer) {
        ownerPlayer.setInitiateAttack(this.humanControlVars.initiateAttack);
        ownerPlayer.setRunning(this.humanControlVars.running);
        ownerPlayer.setJustMoved(this.humanControlVars.justMoved);
        ownerPlayer.updateMovementRates();
    }

    public IsoPlayer getPlayer() {
        return this.getECSOwnerEntity(IsoPlayer.class);
    }

    public AIBrainPlayerControlVars getHumanControlVars() {
        return this.humanControlVars;
    }
}
