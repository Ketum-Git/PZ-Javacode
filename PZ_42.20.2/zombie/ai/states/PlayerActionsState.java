// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.Map;
import java.util.Map.Entry;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.CharacterTimedActions.BaseAction;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameClient;
import zombie.network.fields.Variables;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public final class PlayerActionsState extends State {
    private static final PlayerActionsState INSTANCE = new PlayerActionsState();
    public static final State.Param<State.Stage> STAGE = State.Param.of("stage", State.Stage.class, State.Stage.Enter);
    public static final State.Param<Variables> VARIABLES = State.Param.ofSupplier("variables", Variables.class, Variables::new);
    public static final State.Param<String> PRIMARY = State.Param.ofString("primary", "");
    public static final State.Param<String> SECONDARY = State.Param.ofString("secondary", "");
    public static final State.Param<Boolean> OVERRIDE = State.Param.ofBool("override", false);
    public static final State.Param<Float> RELOAD_SPEED = State.Param.ofFloat("reload_speed", 1.0F);
    public static final State.Param<Boolean> SITONGROUND = State.Param.ofBool("sitonground", false);

    public static PlayerActionsState instance() {
        return INSTANCE;
    }

    private PlayerActionsState() {
        super(true, true, true, true);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        InventoryItem primaryItem = owner.getPrimaryHandItem();
        InventoryItem secondaryItem = owner.getSecondaryHandItem();
        if (!(primaryItem instanceof HandWeapon) && !(secondaryItem instanceof HandWeapon)) {
            owner.setHideWeaponModel(true);
        }

        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        owner.clearVariable("PlayerVoiceSound");
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound")) {
            if (owner.getVariableBoolean("PlayerVoiceSound")) {
                return;
            }

            if (player == null) {
                return;
            }

            owner.setVariable("PlayerVoiceSound", true);
            player.stopPlayerVoiceSound(event.parameterValue);
            player.playerVoiceSound(event.parameterValue);
        }

        if (GameClient.client && event != null && owner instanceof IsoPlayer && owner.getNetworkCharacterAI().getAction() != null && !owner.isLocal()) {
            if ("changeWeaponSprite".equalsIgnoreCase(event.eventName) && !StringUtils.isNullOrEmpty(event.parameterValue)) {
                if ("original".equals(event.parameterValue)) {
                    owner.getNetworkCharacterAI().setOverride(false, null, null);
                } else {
                    owner.getNetworkCharacterAI().setOverride(true, event.parameterValue, null);
                }
            }

            if ("attachConnect".equalsIgnoreCase(event.eventName)) {
                owner.setPrimaryHandItem(null);
                owner.setSecondaryHandItem(null);
            }
        }
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        if (owner.isLocal()) {
            owner.set(STAGE, stage);
            owner.set(SITONGROUND, owner.isCurrentState(PlayerSitOnGroundState.instance()));
            if (!owner.getCharacterActions().isEmpty()) {
                BaseAction action = owner.getCharacterActions().get(0);
                if (action != null) {
                    Variables variables = new Variables();

                    for (String variable : action.animVariables) {
                        variables.get().put(variable, action.chr.getVariableString(variable));
                    }

                    if (variables.get().containsValue("DetachItem") || variables.get().containsValue("AttachItem")) {
                        variables.get().put("AttachAnim", action.chr.getVariableString("AttachAnim"));
                    }

                    if (variables.get().containsValue("Loot")) {
                        variables.get().put("LootPosition", action.chr.getVariableString("LootPosition"));
                    }

                    owner.set(VARIABLES, variables);
                    owner.set(PRIMARY, action.getPrimaryHandItem() == null ? action.getPrimaryHandMdl() : action.getPrimaryHandItem().getStaticModel());
                    owner.set(SECONDARY, action.getSecondaryHandItem() == null ? action.getSecondaryHandMdl() : action.getSecondaryHandItem().getStaticModel());
                    owner.set(OVERRIDE, action.overrideHandModels);
                }
            }

            owner.set(RELOAD_SPEED, owner.getVariableFloat("ReloadSpeed", 1.0F));
        } else {
            boolean isSitOnGround = owner.get(SITONGROUND);
            if (isSitOnGround) {
                owner.reportEvent("EventSitOnGround");
            }

            if (State.Stage.Enter == stage) {
                BaseAction baseAction = new BaseAction(owner);
                Variables variables = owner.get(VARIABLES);

                for (Entry<String, String> entry : variables.get().entrySet()) {
                    if (!"true".equals(entry.getValue()) && !"false".equals(entry.getValue())) {
                        baseAction.setAnimVariable(entry.getKey(), entry.getValue());
                    } else {
                        baseAction.setAnimVariable(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
                    }
                }

                owner.setVariable("IsPerformingAnAction", true);
                owner.getNetworkCharacterAI().setAction(baseAction);
                owner.getNetworkCharacterAI().setOverride(owner.get(OVERRIDE), owner.get(PRIMARY), owner.get(SECONDARY));
                if ("Reload".equals(variables.get().get("PerformingAction"))) {
                    owner.setVariable("ReloadSpeed", owner.get(RELOAD_SPEED));
                }

                owner.getNetworkCharacterAI().startAction();
            } else if (State.Stage.Exit == stage && owner.getNetworkCharacterAI().getAction() != null) {
                owner.getNetworkCharacterAI().stopAction();
            }
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isProcessedOnExit() {
        return true;
    }

    @Override
    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
        owner.setSitOnGround(SITONGROUND.fromDelegate(delegate));
    }
}
