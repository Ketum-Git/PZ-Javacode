// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.util.Type;

@UsedFromLua
public final class PlayerExtState extends State {
    private static final PlayerExtState _instance = new PlayerExtState();
    private static final Integer PARAM_EXT = 0;
    private static final Integer PARAM_EXT_PLAYING = 1;

    public static PlayerExtState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setVariable("ExtPlaying", true);
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.clearVariable("ExtPlaying");
        owner.clearVariable("PlayerVoiceSound");
        this.setParams(owner, State.Stage.Exit);
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if ("ExtFinishing".equalsIgnoreCase(event.eventName)) {
            owner.setVariable("ExtPlaying", false);
            owner.clearVariable("PlayerVoiceSound");
        }

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
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_EXT, owner.getVariableString("Ext"));
            StateMachineParams.put(PARAM_EXT_PLAYING, owner.getVariableBoolean("ExtPlaying"));
        } else {
            owner.setVariable("Ext", (String)StateMachineParams.getOrDefault(PARAM_EXT, ""));
            boolean extPlaying = (Boolean)StateMachineParams.getOrDefault(PARAM_EXT_PLAYING, false);
            owner.setVariable("ExtPlaying", extPlaying);
            if (!extPlaying) {
                owner.reportEvent("ExtFinishing");
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
