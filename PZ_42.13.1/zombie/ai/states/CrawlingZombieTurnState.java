// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.Vector2;

@UsedFromLua
public final class CrawlingZombieTurnState extends State {
    private static final CrawlingZombieTurnState _instance = new CrawlingZombieTurnState();
    private static final Vector2 tempVector2_1 = new Vector2();
    private static final Vector2 tempVector2_2 = new Vector2();

    public static CrawlingZombieTurnState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        ((IsoZombie)owner).allowRepathDelay = 0.0F;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        if (event.eventName.equalsIgnoreCase("TurnSome")) {
            Vector2 startDir = tempVector2_1.set(owner.dir.ToVector());
            Vector2 endDir = "left".equalsIgnoreCase(event.parameterValue)
                ? IsoDirections.fromIndex(owner.dir.index() + 1).ToVector()
                : IsoDirections.fromIndex(owner.dir.index() - 1).ToVector();
            Vector2 v = PZMath.lerp(tempVector2_2, startDir, endDir, event.timePc);
            owner.setForwardDirection(v);
        } else {
            if (event.eventName.equalsIgnoreCase("TurnComplete")) {
                if ("left".equalsIgnoreCase(event.parameterValue)) {
                    owner.dir = IsoDirections.fromIndex(owner.dir.index() + 1);
                } else {
                    owner.dir = IsoDirections.fromIndex(owner.dir.index() - 1);
                }

                owner.setForwardDirectionFromIsoDirection();
            }
        }
    }

    public static boolean calculateDir(IsoGameCharacter owner, IsoDirections targetDir) {
        return targetDir.index() > owner.dir.index() ? targetDir.index() - owner.dir.index() <= 4 : targetDir.index() - owner.dir.index() < -4;
    }

    @Override
    public boolean isSyncOnEnter() {
        return false;
    }

    @Override
    public boolean isSyncOnExit() {
        return false;
    }

    @Override
    public boolean isSyncOnSquare() {
        return false;
    }

    @Override
    public boolean isSyncInIdle() {
        return false;
    }
}
