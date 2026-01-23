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
import zombie.util.StringUtils;

@UsedFromLua
public final class PlayerEmoteState extends State {
    private static final PlayerEmoteState _instance = new PlayerEmoteState();
    private static final Integer PARAM_EMOTE = 0;
    private static final Integer PARAM_PLAYING = 1;
    private static final Integer PARAM_LOOPING_SOUND = 2;

    public static PlayerEmoteState instance() {
        return _instance;
    }

    PlayerEmoteState() {
        this.addAnimEventListener("EmoteFinishing", this::OnAnimEvent_EmoteFinishing);
        this.addAnimEventListener("EmoteLooped", this::OnAnimEvent_EmoteLooped);
        this.addAnimEventListener("PlayLoopingSound", this::OnAnimEvent_PlayLoopingSound);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setVariable("EmotePlaying", true);
        owner.resetModelNextFrame();
        this.setParams(owner, State.Stage.Enter);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        IsoPlayer player = (IsoPlayer)owner;
        if (player.pressedCancelAction()) {
            owner.setVariable("EmotePlaying", false);
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        this.stopLoopingSound(owner);
        owner.clearVariable("EmotePlaying");
        owner.clearVariable("emote");
        owner.resetModelNextFrame();
        this.setParams(owner, State.Stage.Exit);
    }

    private void OnAnimEvent_EmoteFinishing(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        owner.setVariable("EmotePlaying", false);
    }

    private void OnAnimEvent_EmoteLooped(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
    }

    private void OnAnimEvent_PlayLoopingSound(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        String soundName = (String)StateMachineParams.getOrDefault(PARAM_LOOPING_SOUND, null);
        if (soundName != null) {
            if (owner.getEmitter().isPlaying(soundName)) {
                return;
            }

            this.stopLoopingSound(owner);
        }

        if (!StringUtils.isNullOrWhitespace(event.parameterValue)) {
            long eventInstance = owner.playSoundLocal(event.parameterValue);
            if (eventInstance != 0L) {
                StateMachineParams.put(PARAM_LOOPING_SOUND, event.parameterValue);
            }
        }
    }

    private void stopLoopingSound(IsoGameCharacter owner) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        String soundName = (String)StateMachineParams.getOrDefault(PARAM_LOOPING_SOUND, null);
        if (soundName != null) {
            owner.getEmitter().stopOrTriggerSoundByName(soundName);
            StateMachineParams.remove(PARAM_LOOPING_SOUND);
        }
    }

    /**
     * @return TRUE if this state handles the "Cancel Action" key or the B controller button.
     */
    @Override
    public boolean isDoingActionThatCanBeCancelled() {
        return true;
    }

    @Override
    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        HashMap<Object, Object> StateMachineParams = owner.getStateMachineParams(this);
        if (owner.isLocal()) {
            StateMachineParams.put(PARAM_EMOTE, owner.getVariableString("emote"));
            StateMachineParams.put(PARAM_PLAYING, owner.getVariableBoolean("EmotePlaying"));
        } else {
            owner.setVariable("emote", (String)StateMachineParams.getOrDefault(PARAM_EMOTE, ""));
            owner.setVariable("EmotePlaying", (Boolean)StateMachineParams.getOrDefault(PARAM_PLAYING, false));
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
        return false;
    }
}
