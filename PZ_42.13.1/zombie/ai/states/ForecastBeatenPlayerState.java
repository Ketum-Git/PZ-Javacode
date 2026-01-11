// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.GameTime;
import zombie.ai.State;
import zombie.characters.IsoGameCharacter;

public final class ForecastBeatenPlayerState extends State {
    private static final ForecastBeatenPlayerState _instance = new ForecastBeatenPlayerState();

    public static ForecastBeatenPlayerState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        owner.setIgnoreMovement(true);
        owner.setReanimateTimer(30.0F);
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        if (owner.getCurrentSquare() != null) {
            owner.setReanimateTimer(owner.getReanimateTimer() - GameTime.getInstance().getThirtyFPSMultiplier());
            if (owner.getReanimateTimer() <= 0.0F) {
                owner.setReanimateTimer(0.0F);
                owner.setVariable("bKnockedDown", true);
            }
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
    }
}
