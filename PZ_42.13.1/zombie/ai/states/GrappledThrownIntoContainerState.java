// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.Lua.LuaManager;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.debug.DebugType;
import zombie.inventory.ItemContainer;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.network.GameClient;
import zombie.util.StringUtils;
import zombie.util.Type;

public final class GrappledThrownIntoContainerState extends State {
    private static final GrappledThrownIntoContainerState _instance = new GrappledThrownIntoContainerState();

    public static GrappledThrownIntoContainerState instance() {
        return _instance;
    }

    private GrappledThrownIntoContainerState() {
        this.addAnimEventListener("PlayerVoiceSound", this::OnAnimEvent_PlayerVoiceSound);
        this.addAnimEventListener("DepositInContainer", this::OnAnimEvent_DepositInContainer);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setVariable("ClimbingContainer", true);
        owner.setVariable("ClimbContainerStarted", false);
        owner.setVariable("ClimbContainerFinished", false);
        owner.setVariable("ClimbContainerOutcome", "");
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        float dirX = (Float)StateMachineParams.get(GrappledThrownIntoContainerState.ParamKeys.PARAM_DIR_X);
        float dirY = (Float)StateMachineParams.get(GrappledThrownIntoContainerState.ParamKeys.PARAM_DIR_Y);
        owner.setAnimated(true);
        if (owner.isFallOnFront()) {
            owner.setForwardDirection(dirX, dirY);
        } else {
            owner.setForwardDirection(-dirX, -dirY);
        }

        if (owner.getVariableBoolean("ClimbContainerStarted")) {
            float endX = ((Integer)StateMachineParams.get(GrappledThrownIntoContainerState.ParamKeys.PARAM_END_X)).intValue();
            float endY = ((Integer)StateMachineParams.get(GrappledThrownIntoContainerState.ParamKeys.PARAM_END_Y)).intValue();
            this.slideX(owner, endX);
            this.slideY(owner, endY);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("ClimbingContainer");
        owner.clearVariable("ClimbContainerStarted");
        owner.clearVariable("ClimbContainerFinished");
        owner.clearVariable("ClimbContainerOutcome");
        owner.clearVariable("PlayerVoiceSound");
        owner.setIgnoreMovement(false);
        owner.setForwardDirectionFromAnimAngle();
        if (GameClient.client) {
            owner.removeFromWorld();
            owner.removeFromSquare();
        }
    }

    private void OnAnimEvent_PlayerVoiceSound(IsoGameCharacter owner, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (!owner.getVariableBoolean("PlayerVoiceSound")) {
            if (player != null) {
                owner.setVariable("PlayerVoiceSound", true);
                player.playerVoiceSound(event.parameterValue);
            }
        }
    }

    private void OnAnimEvent_DepositInContainer(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        ItemContainer targetContainer = (ItemContainer)StateMachineParams.get(GrappledThrownIntoContainerState.ParamKeys.PARAM_TARGET_CONTAINER);
        if (targetContainer == null) {
            DebugType.Grapple.error("Target Container not found.");
        } else if (!targetContainer.canHumanCorpseFit()) {
            DebugType.Grapple.warn("Target container can no longer hold a corpse. %s", targetContainer);
        } else {
            if (!GameClient.client) {
                owner.becomeCorpseItem(targetContainer);
            } else {
                KahluaTable args = LuaManager.platform.newTable();
                IsoPlayer playerObj = (IsoPlayer)StateMachineParams.get(GrappledThrownIntoContainerState.ParamKeys.PARAM_GRAPPLED_BY);
                args.rawset("id", (double)playerObj.getOnlineID());
                GameClient.instance.sendClientCommand(playerObj, "deadBody", "addBody", args);
                if (playerObj.isGrappling() && StringUtils.isNullOrEmpty(playerObj.getGrappleResult())) {
                    playerObj.LetGoOfGrappled("Dropped");
                }
            }
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return true;
    }

    private void slideX(IsoGameCharacter owner, float x) {
        float dx = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    private void slideY(IsoGameCharacter owner, float y) {
        float dy = 0.05F * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    public void setParams(IsoGameCharacter owner, ItemContainer in_targetContainer) {
        int startX = owner.getSquare().getX();
        int startY = owner.getSquare().getY();
        Vector2 targetContainerWorldPos = Vector2ObjectPool.get().alloc();
        in_targetContainer.getWorldPosition(targetContainerWorldPos);
        int endX = PZMath.fastfloor(targetContainerWorldPos.x);
        int endY = PZMath.fastfloor(targetContainerWorldPos.y);
        float dirX = owner.getForwardDirectionX();
        float dirY = owner.getForwardDirectionY();
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_START_X, startX);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_START_Y, startY);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_END_X, endX);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_END_Y, endY);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_DIR_X, dirX);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_DIR_Y, dirY);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_PREV_STATE, owner.getCurrentState());
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_COLLIDABLE, Boolean.FALSE);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_TARGET_CONTAINER, in_targetContainer);
        StateMachineParams.put(GrappledThrownIntoContainerState.ParamKeys.PARAM_GRAPPLED_BY, owner.getGrappledBy());
        Vector2ObjectPool.get().release(targetContainerWorldPos);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return true;
    }

    private static enum ParamKeys {
        PARAM_START_X,
        PARAM_START_Y,
        PARAM_END_X,
        PARAM_END_Y,
        PARAM_DIR_X,
        PARAM_DIR_Y,
        PARAM_PREV_STATE,
        PARAM_COLLIDABLE,
        PARAM_TARGET_CONTAINER,
        PARAM_GRAPPLED_BY;
    }
}
