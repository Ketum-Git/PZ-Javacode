// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import zombie.util.flags.OrdinalShortFlag;
import zombie.util.flags.ShortFlags;

public class NetworkZombieVariables {
    public static ShortFlags getBooleanVariables(IsoZombie zombie) {
        ShortFlags flags = ShortFlags.alloc();
        flags.set(NetworkZombieVariables.Flag.IsFakeDead, zombie.isFakeDead());
        flags.set(NetworkZombieVariables.Flag.IsLunger, zombie.lunger);
        flags.set(NetworkZombieVariables.Flag.IsRunning, zombie.running);
        flags.set(NetworkZombieVariables.Flag.IsCrawling, zombie.isCrawling());
        flags.set(NetworkZombieVariables.Flag.IsSitAgainstWall, zombie.isSitAgainstWall());
        flags.set(NetworkZombieVariables.Flag.IsReanimatedPlayer, zombie.isReanimatedPlayer());
        flags.set(NetworkZombieVariables.Flag.IsOnFire, zombie.isOnFire());
        flags.set(NetworkZombieVariables.Flag.IsUseless, zombie.isUseless());
        flags.set(NetworkZombieVariables.Flag.IsOnFloor, zombie.isOnFloor());
        flags.set(NetworkZombieVariables.Flag.IsReanimatedForGrappleOnly, zombie.isReanimatedForGrappleOnly());
        flags.set(NetworkZombieVariables.Flag.IsCanWalk, zombie.isCanWalk());
        flags.set(NetworkZombieVariables.Flag.IsSkeleton, zombie.isSkeleton());
        flags.set(NetworkZombieVariables.Flag.IsFallOnFront, zombie.isFallOnFront());
        flags.set(NetworkZombieVariables.Flag.IsAnimationRecording, zombie.isAnimationRecorderActive());
        return flags;
    }

    public static void setBooleanVariables(IsoZombie zombie, short val) {
        setBooleanVariables(zombie, ShortFlags.toFlags(val));
    }

    public static void setBooleanVariables(IsoZombie zombie, ShortFlags val) {
        zombie.setFakeDead(val.has(NetworkZombieVariables.Flag.IsFakeDead));
        zombie.lunger = val.has(NetworkZombieVariables.Flag.IsLunger);
        zombie.running = val.has(NetworkZombieVariables.Flag.IsRunning);
        zombie.setCrawler(val.has(NetworkZombieVariables.Flag.IsCrawling));
        zombie.setSitAgainstWall(val.has(NetworkZombieVariables.Flag.IsSitAgainstWall));
        zombie.setReanimatedPlayer(val.has(NetworkZombieVariables.Flag.IsReanimatedPlayer));
        if (val.has(NetworkZombieVariables.Flag.IsOnFire)) {
            zombie.SetOnFire();
        } else {
            zombie.StopBurning();
        }

        zombie.setUseless(val.has(NetworkZombieVariables.Flag.IsUseless));
        if (zombie.isReanimatedPlayer()) {
            zombie.setOnFloor(val.has(NetworkZombieVariables.Flag.IsOnFloor));
        }

        zombie.setReanimatedForGrappleOnly(val.has(NetworkZombieVariables.Flag.IsReanimatedForGrappleOnly));
        zombie.setCanWalk(val.has(NetworkZombieVariables.Flag.IsCanWalk));
        zombie.setSkeleton(val.has(NetworkZombieVariables.Flag.IsSkeleton));
        zombie.setFallOnFront(val.has(NetworkZombieVariables.Flag.IsFallOnFront));
        zombie.setAnimRecorderActive(val.has(NetworkZombieVariables.Flag.IsAnimationRecording), true);
    }

    public static enum Flag implements OrdinalShortFlag {
        IsFakeDead,
        IsLunger,
        IsRunning,
        IsCrawling,
        IsSitAgainstWall,
        IsReanimatedPlayer,
        IsOnFire,
        IsUseless,
        IsOnFloor,
        IsReanimatedForGrappleOnly,
        IsCanWalk,
        IsSkeleton,
        IsFallOnFront,
        IsAnimationRecording;
    }
}
