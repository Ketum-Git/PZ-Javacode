// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind.nativeCode;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.ai.KnownBlockedEdges;
import zombie.ai.astar.Mover;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.iso.BentFences;
import zombie.pathfind.IPathfinder;
import zombie.pathfind.Path;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLStaircase;
import zombie.popman.ObjectPool;
import zombie.util.Type;

public final class PathFindRequest {
    IPathfinder finder;
    Mover mover;
    boolean canCrawl;
    boolean crawling;
    boolean ignoreCrawlCost;
    boolean canThump;
    boolean canClimbFences;
    boolean canClimbTallFences;
    boolean hasTarget;
    boolean canBend;
    int minLevel;
    int maxLevel;
    final ArrayList<HLChunkLevel> allowedChunkLevels = new ArrayList<>();
    final ArrayList<HLStaircase> allowedStaircases = new ArrayList<>();
    final ArrayList<KnownBlockedEdges> knownBlockedEdges = new ArrayList<>();
    float startX;
    float startY;
    float startZ;
    float targetX;
    float targetY;
    float targetZ;
    public final TFloatArrayList targetXyz = new TFloatArrayList();
    final Path path = new Path();
    boolean cancel;
    boolean doNotRelease;
    static final ObjectPool<PathFindRequest> pool = new ObjectPool<>(PathFindRequest::new);

    PathFindRequest init(IPathfinder pathfinder, Mover mover, float startX, float startY, float startZ, float targetX, float targetY, float targetZ) {
        this.finder = pathfinder;
        this.mover = mover;
        this.canCrawl = false;
        this.crawling = false;
        this.ignoreCrawlCost = false;
        this.canThump = false;
        this.canClimbFences = false;
        this.canClimbTallFences = false;
        this.hasTarget = false;
        this.canBend = false;
        if (mover instanceof IsoAnimal animal) {
            this.canThump = animal.shouldBreakObstaclesDuringPathfinding();
            this.canClimbFences = animal.canClimbFences();
        }

        if (mover instanceof IsoZombie zombie) {
            this.canCrawl = zombie.isCrawling() || zombie.isCanCrawlUnderVehicle();
            this.crawling = zombie.isCrawling();
            this.ignoreCrawlCost = zombie.isCrawling() && !zombie.isCanWalk();
            this.canThump = true;
            this.hasTarget = zombie.getTarget() != null || zombie.isMovingToPlayerSound();
            this.canBend = BentFences.getInstance().isEnabled();
        }

        IsoPlayer player = Type.tryCastTo(mover, IsoPlayer.class);
        if (player != null && !(mover instanceof IsoAnimal)) {
            this.canClimbTallFences = SandboxOptions.instance.easyClimbing.getValue() || player.getClimbingFailChanceFloat() >= 1.0F;
        }

        this.minLevel = 0;
        this.maxLevel = 63;
        this.allowedChunkLevels.clear();
        this.allowedStaircases.clear();
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetXyz.resetQuick();
        this.path.clear();
        this.cancel = false;
        return this;
    }

    static PathFindRequest alloc() {
        return pool.alloc();
    }

    public void release() {
        this.finder = null;
        this.mover = null;
        pool.release(this);
    }
}
