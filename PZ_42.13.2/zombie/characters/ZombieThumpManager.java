// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_PARAMETER_DESCRIPTION;
import zombie.core.math.PZMath;

public final class ZombieThumpManager extends BaseZombieSoundManager {
    public static final ZombieThumpManager instance = new ZombieThumpManager();

    public ZombieThumpManager() {
        super(40, 100);
    }

    @Override
    public void playSound(IsoZombie chr) {
        long inst = chr.getEmitter().playSoundImpl(this.getThumpSound(chr), null);
        FMOD_STUDIO_PARAMETER_DESCRIPTION parameterDescription = FMODManager.instance.getParameterDescription("ObjectCondition");
        chr.getEmitter().setParameterValue(inst, parameterDescription, PZMath.ceil(chr.getThumpCondition() * 100.0F));
    }

    private String getThumpSound(IsoZombie chr) {
        if (chr.isVariable("ThumpType", "DoorClaw")) {
            switch (chr.thumpFlag) {
                case 1:
                    return "ZombieThumpGeneric";
                case 2:
                    return "ZombieThumpWindowExtra";
                case 3:
                    return "ZombieThumpWindow";
                case 4:
                    return "ZombieClawMetal";
                case 5:
                    return "ZombieClawGarageDoor";
                case 6:
                    return "ZombieClawChainlinkFence";
                case 7:
                    return "ZombieClawMetal";
                case 8:
                    return "ZombieClawWood";
            }
        } else {
            switch (chr.thumpFlag) {
                case 1:
                    return "ZombieThumpGeneric";
                case 2:
                    return "ZombieThumpWindowExtra";
                case 3:
                    return "ZombieThumpWindow";
                case 4:
                    return "ZombieThumpMetal";
                case 5:
                    return "ZombieThumpGarageDoor";
                case 6:
                    return "ZombieThumpChainlinkFence";
                case 7:
                    return "ZombieThumpMetalPoleGate";
                case 8:
                    return "ZombieThumpWood";
            }
        }

        return "ZombieThumpGeneric";
    }

    @Override
    public void postUpdate() {
        for (int i = 0; i < this.characters.size(); i++) {
            this.characters.get(i).setThumpFlag(0);
        }
    }
}
