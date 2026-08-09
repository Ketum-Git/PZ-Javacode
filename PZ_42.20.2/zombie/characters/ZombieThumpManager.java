// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.core.math.PZMath;
import zombie.scripting.objects.SoundKey;

public final class ZombieThumpManager extends BaseZombieSoundManager {
    public static final int SOUND_RADIUS = 70;
    public static final ZombieThumpManager instance = new ZombieThumpManager();

    public ZombieThumpManager() {
        super(40, 100);
    }

    @Override
    public void playSound(IsoZombie chr) {
        long inst = chr.getEmitter().playSoundImpl(this.getThumpSound(chr), null);
        chr.getEmitter().setParameterValueByName(inst, "ObjectCondition", PZMath.ceil(chr.getThumpCondition() * 100.0F));
    }

    private String getThumpSound(IsoZombie chr) {
        if (chr.isVariable("ThumpType", "DoorClaw")) {
            switch (chr.thumpFlag) {
                case 1:
                    return SoundKey.ZOMBIE_THUMP_GENERIC.getSoundName();
                case 2:
                    return SoundKey.ZOMBIE_THUMP_WINDOW_EXTRA.getSoundName();
                case 3:
                    return SoundKey.ZOMBIE_THUMP_WINDOW.getSoundName();
                case 4:
                    return SoundKey.ZOMBIE_CLAW_METAL.getSoundName();
                case 5:
                    return SoundKey.ZOMBIE_CLAW_GARAGE_DOOR.getSoundName();
                case 6:
                    return SoundKey.ZOMBIE_CLAW_CHAINLINK_FENCE.getSoundName();
                case 7:
                    return SoundKey.ZOMBIE_CLAW_METAL.getSoundName();
                case 8:
                    return SoundKey.ZOMBIE_CLAW_WOOD.getSoundName();
            }
        } else {
            switch (chr.thumpFlag) {
                case 1:
                    return SoundKey.ZOMBIE_THUMP_GENERIC.getSoundName();
                case 2:
                    return SoundKey.ZOMBIE_THUMP_WINDOW_EXTRA.getSoundName();
                case 3:
                    return SoundKey.ZOMBIE_THUMP_WINDOW.getSoundName();
                case 4:
                    return SoundKey.ZOMBIE_THUMP_METAL.getSoundName();
                case 5:
                    return SoundKey.ZOMBIE_THUMP_GARAGE_DOOR.getSoundName();
                case 6:
                    return SoundKey.ZOMBIE_THUMP_CHAINLINK_FENCE.getSoundName();
                case 7:
                    return SoundKey.ZOMBIE_THUMP_METAL_POLE_FENCE.getSoundName();
                case 8:
                    return SoundKey.ZOMBIE_THUMP_WOOD.getSoundName();
            }
        }

        return SoundKey.ZOMBIE_THUMP_GENERIC.getSoundName();
    }

    @Override
    public void postUpdate() {
        for (int i = 0; i < this.characters.size(); i++) {
            this.characters.get(i).setThumpFlag(0);
        }
    }
}
