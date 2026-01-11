// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import zombie.ai.GameCharacterAIBrain;

public class IsoAIModule {
    public static IsoPlayer invisibleCameraPlayer;
    public IsoPlayer player;
    public GameCharacterAIBrain brain;
    private boolean isInvisibleCamera;
    private final ArrayList<IsoZombie> npcSeenZombies = new ArrayList<>();

    public IsoAIModule(IsoPlayer player) {
        this.player = player;
    }

    public IsoAIModule(IsoGameCharacter player) {
        if (player instanceof IsoPlayer isoPlayer) {
            this.player = isoPlayer;
        }
    }

    public GameCharacterAIBrain getBrain() {
        return this.brain;
    }

    public boolean doUpdatePlayerControls(boolean bMelee) {
        if (this.brain != null) {
            bMelee = this.brain.humanControlVars.melee;
            this.player.bannedAttacking = this.brain.humanControlVars.bannedAttacking;
        }

        return bMelee;
    }

    public void update() {
        if (this.brain == null) {
            this.brain = new GameCharacterAIBrain(this.player);
        }

        this.brain.update();
    }

    public void setNPC(boolean newvalue) {
        if (newvalue && this.brain == null) {
            this.brain = new GameCharacterAIBrain(this.player);
        }

        this.player.isNpc = newvalue;
    }

    public void postUpdate() {
        this.brain.postUpdateHuman(this.player);
        this.player.setInitiateAttack(this.brain.humanControlVars.initiateAttack);
        this.player.setRunning(this.brain.humanControlVars.running);
        this.player.setJustMoved(this.brain.humanControlVars.justMoved);
        this.player.updateMovementRates();
    }

    public void initPlayerAI() {
    }
}
