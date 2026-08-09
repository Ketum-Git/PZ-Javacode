// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

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
    private static final PlayerExtState INSTANCE = new PlayerExtState();
    public static final State.Param<String> EXT = State.Param.ofString("ext", "");
    public static final State.Param<Boolean> EXT_PLAYING = State.Param.ofBool("ext_playing", false);

    public static PlayerExtState instance() {
        return INSTANCE;
    }

    private PlayerExtState() {
        super(true, true, true, true);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setVariable("ExtPlaying", true);
        this.setParams(owner, State.Stage.Enter);
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
        if (owner.isLocal()) {
            owner.set(EXT, owner.getVariableString("Ext"));
            owner.set(EXT_PLAYING, owner.getVariableBoolean("ExtPlaying"));
        } else {
            owner.setVariable("Ext", owner.get(EXT));
            boolean extPlaying = owner.get(EXT_PLAYING);
            owner.setVariable("ExtPlaying", extPlaying);
            if (!extPlaying) {
                owner.reportEvent("ExtFinishing");
            }
        }

        super.setParams(owner, stage);
    }
}
