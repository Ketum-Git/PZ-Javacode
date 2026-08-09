// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
    private static final GrappledThrownIntoContainerState INSTANCE = new GrappledThrownIntoContainerState();
    public static final State.Param<Integer> START_X = State.Param.ofInt("start_x", 0);
    public static final State.Param<Integer> START_Y = State.Param.ofInt("start_y", 0);
    public static final State.Param<Integer> END_X = State.Param.ofInt("end_x", 0);
    public static final State.Param<Integer> END_Y = State.Param.ofInt("end_y", 0);
    public static final State.Param<Float> DIR_X = State.Param.ofFloat("dir_x", 0.0F);
    public static final State.Param<Float> DIR_Y = State.Param.ofFloat("dir_y", 0.0F);
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);
    public static final State.Param<Boolean> COLLIDABLE = State.Param.ofBool("collidable", false);
    public static final State.Param<ItemContainer> TARGET_CONTAINER = State.Param.of("target_container", ItemContainer.class);
    public static final State.Param<IsoPlayer> GRAPPLED_BY = State.Param.of("grappled_by", IsoPlayer.class);

    public static GrappledThrownIntoContainerState instance() {
        return INSTANCE;
    }

    private GrappledThrownIntoContainerState() {
        super(true, false, true, false);
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
        float dirX = owner.get(DIR_X);
        float dirY = owner.get(DIR_Y);
        owner.setAnimated(true);
        if (owner.isFallOnFront()) {
            owner.setForwardDirection(dirX, dirY);
        } else {
            owner.setForwardDirection(-dirX, -dirY);
        }

        if (owner.getVariableBoolean("ClimbContainerStarted")) {
            float endX = owner.get(END_X).intValue();
            float endY = owner.get(END_Y).intValue();
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
        if (GameClient.client && owner.isLocal()) {
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
        ItemContainer targetContainer = owner.get(TARGET_CONTAINER);
        if (targetContainer == null) {
            DebugType.Grapple.error("Target Container not found.");
        } else if (!targetContainer.canHumanCorpseFit(GRAPPLED_BY.get(owner))) {
            DebugType.Grapple.warn("Target container can no longer hold a corpse. %s", targetContainer);
        } else {
            if (!GameClient.client) {
                owner.becomeCorpseItem(targetContainer, GRAPPLED_BY.get(owner));
            } else {
                KahluaTable args = LuaManager.platform.newTable();
                IsoPlayer playerObj = GRAPPLED_BY.get(owner);
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

    public void setParams(IsoGameCharacter owner, ItemContainer targetContainer) {
        int startX = owner.getSquare().getX();
        int startY = owner.getSquare().getY();
        Vector2 targetContainerWorldPos = Vector2ObjectPool.get().alloc();
        targetContainer.getWorldPosition(targetContainerWorldPos);
        int endX = PZMath.fastfloor(targetContainerWorldPos.x);
        int endY = PZMath.fastfloor(targetContainerWorldPos.y);
        float dirX = owner.getForwardDirectionX();
        float dirY = owner.getForwardDirectionY();
        owner.set(START_X, startX);
        owner.set(START_Y, startY);
        owner.set(END_X, endX);
        owner.set(END_Y, endY);
        owner.set(DIR_X, dirX);
        owner.set(DIR_Y, dirY);
        owner.set(PREV_STATE, owner.getCurrentState());
        owner.set(COLLIDABLE, false);
        owner.set(TARGET_CONTAINER, targetContainer);
        owner.set(GRAPPLED_BY, (IsoPlayer)owner.getGrappledBy());
        Vector2ObjectPool.get().release(targetContainerWorldPos);
    }
}
