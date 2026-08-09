// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
    private static final PlayerEmoteState INSTANCE = new PlayerEmoteState();
    public static final State.Param<String> EMOTE = State.Param.ofString("emote", "");
    public static final State.Param<Boolean> PLAYING = State.Param.ofBool("playing", false);
    public static final State.Param<String> LOOPING_SOUND = State.Param.ofString("looping_sound", null);

    public static PlayerEmoteState instance() {
        return INSTANCE;
    }

    private PlayerEmoteState() {
        super(true, true, true, false);
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
        String soundName = owner.get(LOOPING_SOUND);
        if (soundName != null) {
            if (owner.getEmitter().isPlaying(soundName)) {
                return;
            }

            this.stopLoopingSound(owner);
        }

        if (!StringUtils.isNullOrWhitespace(event.parameterValue)) {
            long eventInstance = owner.playSoundLocal(event.parameterValue);
            if (eventInstance != 0L) {
                owner.set(LOOPING_SOUND, event.parameterValue);
            }
        }
    }

    private void stopLoopingSound(IsoGameCharacter owner) {
        String soundName = owner.get(LOOPING_SOUND);
        if (soundName != null) {
            owner.getEmitter().stopOrTriggerSoundByName(soundName);
            owner.remove(LOOPING_SOUND);
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
        if (owner.isLocal()) {
            owner.set(EMOTE, owner.getVariableString("emote"));
            owner.set(PLAYING, owner.getVariableBoolean("EmotePlaying"));
        } else {
            owner.setVariable("emote", owner.get(EMOTE));
            owner.setVariable("EmotePlaying", owner.get(PLAYING));
        }

        super.setParams(owner, stage);
    }
}
