// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
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
    private static final PlayerActionsState _instance = new PlayerActionsState();
    private static final Integer PARAM_STAGE = 0;
    private static final Integer PARAM_VARIABLES = 1;
    private static final Integer PARAM_PRIMARY = 2;
    private static final Integer PARAM_SECONDARY = 3;
    private static final Integer PARAM_OVERRIDE = 4;
    private static final Integer PARAM_RELOAD_SPEED = 5;
    private static final Integer PARAM_SITONGROUND = 6;

    public static PlayerActionsState instance() {
        return _instance;
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
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setHideWeaponModel(false);
        owner.clearVariable("PlayerVoiceSound");
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
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
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_STAGE, stage);
            StateMachineParams.put(PARAM_SITONGROUND, owner.isCurrentState(PlayerSitOnGroundState.instance()));
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

                    StateMachineParams.put(PARAM_VARIABLES, variables);
                    StateMachineParams.put(
                        PARAM_PRIMARY, action.getPrimaryHandItem() == null ? action.getPrimaryHandMdl() : action.getPrimaryHandItem().getStaticModel()
                    );
                    StateMachineParams.put(
                        PARAM_SECONDARY, action.getSecondaryHandItem() == null ? action.getSecondaryHandMdl() : action.getSecondaryHandItem().getStaticModel()
                    );
                    StateMachineParams.put(PARAM_OVERRIDE, action.overrideHandModels);
                }
            }

            StateMachineParams.put(PARAM_RELOAD_SPEED, owner.getVariableFloat("ReloadSpeed", 1.0F));
        } else {
            State.Stage operation = (State.Stage)StateMachineParams.getOrDefault(PARAM_STAGE, State.Stage.Enter);
            boolean isSitOnGround = (Boolean)StateMachineParams.getOrDefault(PARAM_SITONGROUND, false);
            if (isSitOnGround) {
                owner.reportEvent("EventSitOnGround");
            }

            if (State.Stage.Enter == operation) {
                BaseAction baseAction = new BaseAction(owner);
                Variables variables = (Variables)StateMachineParams.getOrDefault(PARAM_VARIABLES, new Variables());

                for (Entry<String, String> entry : variables.get().entrySet()) {
                    if (!"true".equals(entry.getValue()) && !"false".equals(entry.getValue())) {
                        baseAction.setAnimVariable(entry.getKey(), entry.getValue());
                    } else {
                        baseAction.setAnimVariable(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
                    }
                }

                owner.setVariable("IsPerformingAnAction", true);
                owner.getNetworkCharacterAI().setAction(baseAction);
                owner.getNetworkCharacterAI()
                    .setOverride(
                        (Boolean)StateMachineParams.getOrDefault(PARAM_OVERRIDE, false),
                        (String)StateMachineParams.getOrDefault(PARAM_PRIMARY, ""),
                        (String)StateMachineParams.getOrDefault(PARAM_SECONDARY, "")
                    );
                if ("Reload".equals(variables.get().get("PerformingAction"))) {
                    owner.setVariable("ReloadSpeed", (Float)StateMachineParams.getOrDefault(PARAM_RELOAD_SPEED, false));
                }

                owner.getNetworkCharacterAI().startAction();
            } else if (State.Stage.Exit == operation && owner.getNetworkCharacterAI().getAction() != null) {
                owner.getNetworkCharacterAI().stopAction();
            }
        }

        super.setParams(owner, stage);
    }

    @Override
    public boolean isSyncOnEnter() {
        return true;
    }

    @Override
    public boolean isSyncOnExit() {
        return true;
    }

    @Override
    public boolean isSyncOnSquare() {
        return true;
    }

    @Override
    public boolean isSyncInIdle() {
        return true;
    }
}
