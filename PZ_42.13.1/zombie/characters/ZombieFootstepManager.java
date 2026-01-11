// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public final class ZombieFootstepManager extends BaseZombieSoundManager {
    public static final ZombieFootstepManager instance = new ZombieFootstepManager();

    public ZombieFootstepManager() {
        super(40, 500);
    }

    @Override
    public void playSound(IsoZombie chr) {
        chr.getEmitter().playFootsteps("ZombieFootstepsCombined", chr.getFootstepVolume());
    }

    @Override
    public void postUpdate() {
    }
}
