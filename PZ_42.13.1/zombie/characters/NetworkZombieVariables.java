// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

public class NetworkZombieVariables {
    private static final short IsFakeDead = 1;
    private static final short IsLunger = 2;
    private static final short IsRunning = 4;
    private static final short IsCrawling = 8;
    private static final short IsSitAgainstWall = 16;
    private static final short IsReanimatedPlayer = 32;
    private static final short IsOnFire = 64;
    private static final short IsUseless = 128;
    private static final short IsOnFloor = 256;
    private static final short IsReanimatedForGrappleOnly = 512;
    private static final short IsCanWalk = 1024;

    public static short getBooleanVariables(IsoZombie zombie) {
        short val = 0;
        val = (short)(val | (zombie.isFakeDead() ? 1 : 0));
        val = (short)(val | (zombie.lunger ? 2 : 0));
        val = (short)(val | (zombie.running ? 4 : 0));
        val = (short)(val | (zombie.isCrawling() ? 8 : 0));
        val = (short)(val | (zombie.isSitAgainstWall() ? 16 : 0));
        val = (short)(val | (zombie.isReanimatedPlayer() ? 32 : 0));
        val = (short)(val | (zombie.isOnFire() ? 64 : 0));
        val = (short)(val | (zombie.isUseless() ? 128 : 0));
        val = (short)(val | (zombie.isOnFloor() ? 256 : 0));
        val = (short)(val | (zombie.isReanimatedForGrappleOnly() ? 512 : 0));
        return (short)(val | (zombie.isCanWalk() ? 1024 : 0));
    }

    public static void setBooleanVariables(IsoZombie zombie, short val) {
        zombie.setFakeDead((val & 1) != 0);
        zombie.lunger = (val & 2) != 0;
        zombie.running = (val & 4) != 0;
        zombie.setCrawler((val & 8) != 0);
        zombie.setSitAgainstWall((val & 16) != 0);
        zombie.setReanimatedPlayer((val & 32) != 0);
        if ((val & 64) != 0) {
            zombie.SetOnFire();
        } else {
            zombie.StopBurning();
        }

        zombie.setUseless((val & 128) != 0);
        if (zombie.isReanimatedPlayer()) {
            zombie.setOnFloor((val & 256) != 0);
        }

        zombie.setReanimatedForGrappleOnly((val & 512) != 0);
        zombie.setCanWalk((val & 1024) != 0);
    }
}
