// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.CharacterTimedActions;

import java.util.ArrayList;
import java.util.Arrays;
import zombie.GameTime;
import zombie.ai.states.PlayerActionsState;
import zombie.ai.states.StateManager;
import zombie.characters.CharacterActionAnims;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.Core;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.WeaponType;
import zombie.network.GameClient;
import zombie.ui.UIManager;
import zombie.util.StringUtils;
import zombie.util.Type;

public class BaseAction {
    public long soundEffect = -1L;
    public float currentTime = -2.0F;
    public float lastTime = -1.0F;
    public int maxTime = 60;
    public float prevLastTime;
    public boolean useProgressBar = true;
    public boolean forceProgressBar;
    public IsoGameCharacter chr;
    public boolean stopOnWalk = true;
    public boolean stopOnRun = true;
    public boolean stopOnAim;
    public float caloriesModifier = 1.0F;
    public float delta;
    public boolean blockMovementEtc;
    public boolean overrideAnimation;
    public final ArrayList<String> animVariables = new ArrayList<>();
    public boolean loopAction;
    public boolean started;
    public boolean forceStop;
    public boolean forceComplete;
    public boolean waitForFinished;
    public boolean pathfinding;
    public boolean allowedWhileDraggingCorpses;
    private static final ArrayList<String> specificNetworkAnim = new ArrayList<>(
        Arrays.asList("Reload", "Bandage", "Loot", "AttachItem", "Drink", "Eat", "Pour", "Read", "fill_container_tap", "drink_tap", "WearClothing")
    );
    private InventoryItem primaryHandItem;
    private InventoryItem secondaryHandItem;
    private String primaryHandMdl;
    private String secondaryHandMdl;
    public boolean overrideHandModels;

    public BaseAction(IsoGameCharacter chr) {
        this.chr = chr;
    }

    public void forceStop() {
        this.forceStop = true;
    }

    public void forceComplete() {
        this.forceComplete = true;
    }

    public boolean isForceComplete() {
        return this.forceComplete;
    }

    public void PlayLoopedSoundTillComplete(String name, int radius, float maxGain) {
        this.soundEffect = this.chr.getEmitter().playSound(name);
    }

    public boolean hasStalled() {
        return !this.started
            ? false
            : this.lastTime == this.currentTime && this.lastTime == this.prevLastTime && this.lastTime < 0.0F || this.currentTime < 0.0F;
    }

    public float getJobDelta() {
        return this.delta;
    }

    public void setJobDelta(float delta) {
        this.currentTime = this.maxTime * delta;
        this.delta = delta;
    }

    public void setWaitForFinished(boolean val) {
        this.waitForFinished = val;
    }

    public void resetJobDelta() {
        this.delta = 0.0F;
        this.currentTime = 0.0F;
    }

    public void waitToStart() {
        if (!this.chr.shouldWaitToStartTimedAction()) {
            this.started = true;
            this.start();
        }
    }

    public void update() {
        this.prevLastTime = this.lastTime;
        this.lastTime = this.currentTime;
        this.currentTime = this.currentTime + GameTime.instance.getMultiplier();
        if (this.currentTime < 0.0F) {
            this.currentTime = 0.0F;
        }

        boolean bUseProgressBar = (Core.getInstance().isOptionProgressBar() || this.forceProgressBar)
            && this.useProgressBar
            && this.chr instanceof IsoPlayer isoPlayer
            && isoPlayer.isLocalPlayer();
        if (this.maxTime == -1) {
            if (bUseProgressBar) {
                UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(Float.POSITIVE_INFINITY);
            }
        } else {
            if (this.maxTime == 0) {
                this.delta = 0.0F;
            } else {
                this.delta = Math.min(this.currentTime / this.maxTime, 1.0F);
            }

            if (bUseProgressBar) {
                UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(this.delta);
            }
        }
    }

    public void start() {
        this.forceComplete = false;
        this.forceStop = false;
        if (this.chr.isCurrentState(PlayerActionsState.instance())) {
            InventoryItem primaryItem = this.chr.getPrimaryHandItem();
            InventoryItem secondaryItem = this.chr.getSecondaryHandItem();
            this.chr.setHideWeaponModel(!(primaryItem instanceof HandWeapon) && !(secondaryItem instanceof HandWeapon));
        }
    }

    public void reset() {
        this.currentTime = 0.0F;
        this.forceComplete = false;
        this.forceStop = false;
    }

    public float getCurrentTime() {
        return this.currentTime;
    }

    public void stop() {
        UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(0.0F);
        if (this.soundEffect > -1L) {
            this.chr.getEmitter().stopSound(this.soundEffect);
            this.soundEffect = -1L;
        }

        this.stopTimedActionAnim();
    }

    public boolean valid() {
        return true;
    }

    public boolean isStarted() {
        return this.started;
    }

    public boolean finished() {
        return !this.waitForFinished && this.currentTime >= this.maxTime && this.maxTime != -1;
    }

    public void perform() {
        UIManager.getProgressBar(((IsoPlayer)this.chr).getPlayerNum()).setValue(1.0F);
        if (!this.loopAction) {
            this.stopTimedActionAnim();
        }
    }

    public void complete() {
    }

    public void setUseProgressBar(boolean use) {
        this.useProgressBar = use;
    }

    public void setBlockMovementEtc(boolean block) {
        this.blockMovementEtc = block;
    }

    public void setPathfinding(boolean b) {
        this.pathfinding = b;
    }

    public boolean isPathfinding() {
        return this.pathfinding;
    }

    public void setAllowedWhileDraggingCorpses(boolean in_val) {
        this.allowedWhileDraggingCorpses = in_val;
    }

    public boolean isAllowedWhileDraggingCorpses() {
        return this.allowedWhileDraggingCorpses;
    }

    public void setOverrideAnimation(boolean override) {
        this.overrideAnimation = override;
    }

    public void stopTimedActionAnim() {
        for (int i = 0; i < this.animVariables.size(); i++) {
            String key = this.animVariables.get(i);
            this.chr.clearVariable(key);
        }

        this.chr.setVariable("IsPerformingAnAction", false);
        if (this.overrideHandModels) {
            this.overrideHandModels = false;
            this.chr.resetEquippedHandsModels();
        }

        if (GameClient.client && this.chr instanceof IsoPlayer player && player.isLocalPlayer()) {
            StateManager.exitSubState(this.chr, PlayerActionsState.instance());
        }
    }

    public void setAnimVariable(String key, String val) {
        if (!this.animVariables.contains(key)) {
            this.animVariables.add(key);
        }

        this.chr.setVariable(key, val);
    }

    public void setAnimVariable(String key, boolean val) {
        if (!this.animVariables.contains(key)) {
            this.animVariables.add(key);
        }

        this.chr.setVariable(key, String.valueOf(val));
    }

    public String getPrimaryHandMdl() {
        return this.primaryHandMdl;
    }

    public String getSecondaryHandMdl() {
        return this.secondaryHandMdl;
    }

    public InventoryItem getPrimaryHandItem() {
        return this.primaryHandItem;
    }

    public InventoryItem getSecondaryHandItem() {
        return this.secondaryHandItem;
    }

    public void setActionAnim(CharacterActionAnims act) {
        this.setActionAnim(act.toString());
    }

    public void setActionAnim(String animNode) {
        this.setAnimVariable("PerformingAction", animNode);
        this.chr.setVariable("IsPerformingAnAction", true);
        if (Core.debug) {
            this.chr.advancedAnimator.printDebugCharacterActions(animNode);
        }

        if (GameClient.client && this.chr instanceof IsoPlayer player && player.isLocalPlayer()) {
            StateManager.enterSubState(this.chr, PlayerActionsState.instance());
        }
    }

    public void setOverrideHandModels(InventoryItem primaryHand, InventoryItem secondaryHand) {
        this.setOverrideHandModels(primaryHand, secondaryHand, true);
    }

    public void setOverrideHandModels(InventoryItem primaryHand, InventoryItem secondaryHand, boolean resetModel) {
        this.setOverrideHandModelsObject(primaryHand, secondaryHand, resetModel);
    }

    public void setOverrideHandModelsString(String primaryHand, String secondaryHand) {
        this.setOverrideHandModelsString(primaryHand, secondaryHand, true);
    }

    public void setOverrideHandModelsString(String primaryHand, String secondaryHand, boolean resetModel) {
        this.setOverrideHandModelsObject(primaryHand, secondaryHand, resetModel);
    }

    public void setOverrideHandModelsObject(Object primaryHand, Object secondaryHand, boolean resetModel) {
        this.overrideHandModels = true;
        this.primaryHandItem = Type.tryCastTo(primaryHand, InventoryItem.class);
        this.secondaryHandItem = Type.tryCastTo(secondaryHand, InventoryItem.class);
        this.primaryHandMdl = StringUtils.discardNullOrWhitespace(Type.tryCastTo(primaryHand, String.class));
        this.secondaryHandMdl = StringUtils.discardNullOrWhitespace(Type.tryCastTo(secondaryHand, String.class));
        if (resetModel) {
            this.chr.resetEquippedHandsModels();
        }
    }

    public void overrideWeaponType() {
        WeaponType weaponType = WeaponType.getWeaponType(this.chr, this.primaryHandItem, this.secondaryHandItem);
        this.chr.setVariable("Weapon", weaponType.getType());
    }

    public void restoreWeaponType() {
        WeaponType weaponType = WeaponType.getWeaponType(this.chr);
        this.chr.setVariable("Weapon", weaponType.getType());
    }

    public void OnAnimEvent(AnimEvent event) {
    }

    public void setLoopedAction(boolean looped) {
        this.loopAction = looped;
    }

    public void getDeltaModifiers(MoveDeltaModifiers modifiers) {
    }
}
