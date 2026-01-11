// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import zombie.UsedFromLua;
import zombie.ai.State;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.scripting.objects.CharacterTrait;

@UsedFromLua
public final class FakeDeadAttackState extends State {
    private static final FakeDeadAttackState _instance = new FakeDeadAttackState();

    public static FakeDeadAttackState instance() {
        return _instance;
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoZombie zombie = (IsoZombie)owner;
        zombie.DirectionFromVector(zombie.vectorToTarget);
        zombie.setFakeDead(false);
        owner.setVisibleToNPCs(true);
        owner.setCollidable(true);
        String t = owner.getDescriptor().getVoicePrefix() + "Attack";
        owner.getEmitter().playSound(t);
        if (zombie.target instanceof IsoPlayer player && !player.hasTrait(CharacterTrait.DESENSITIZED)) {
            player.getStats().add(CharacterStat.PANIC, player.getBodyDamage().getPanicIncreaseValue() * 3.0F);
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
    }

    @Override
    public void exit(IsoGameCharacter owner) {
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoZombie zombie = (IsoZombie)owner;
        if (event.eventName.equalsIgnoreCase("AttackCollisionCheck")
            && owner.isAlive()
            && zombie.isTargetInCone(1.5F, 0.9F)
            && zombie.target instanceof IsoGameCharacter targetChr
            && (targetChr.getVehicle() == null || targetChr.getVehicle().couldCrawlerAttackPassenger(targetChr))) {
            targetChr.getBodyDamage().AddRandomDamageFromZombie((IsoZombie)owner, null);
        }

        if (event.eventName.equalsIgnoreCase("FallOnFront")) {
            zombie.setFallOnFront(Boolean.parseBoolean(event.parameterValue));
        }

        if (event.eventName.equalsIgnoreCase("ActiveAnimFinishing")) {
            zombie.setCrawler(true);
        }
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
