// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.pathfind;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.ai.KnownBlockedEdges;
import zombie.ai.astar.Mover;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.animals.IsoAnimal;
import zombie.pathfind.highLevel.HLChunkLevel;
import zombie.pathfind.highLevel.HLLevelTransition;

final class PathFindRequest {
    IPathfinder finder;
    Mover mover;
    boolean canCrawl;
    boolean ignoreCrawlCost;
    boolean canThump;
    int minLevel;
    int maxLevel;
    final ArrayList<HLChunkLevel> allowedChunkLevels = new ArrayList<>();
    final ArrayList<HLLevelTransition> allowedLevelTransitions = new ArrayList<>();
    final ArrayList<KnownBlockedEdges> knownBlockedEdges = new ArrayList<>();
    float startX;
    float startY;
    float startZ;
    float targetX;
    float targetY;
    float targetZ;
    final TFloatArrayList targetXyz = new TFloatArrayList();
    final Path path = new Path();
    boolean cancel;
    static final ArrayDeque<PathFindRequest> pool = new ArrayDeque<>();

    PathFindRequest init(IPathfinder pathfinder, Mover mover, float startX, float startY, float startZ, float targetX, float targetY, float targetZ) {
        this.finder = pathfinder;
        this.mover = mover;
        this.canCrawl = false;
        this.ignoreCrawlCost = false;
        this.canThump = false;
        if (mover instanceof IsoAnimal animal) {
            this.canThump = animal.shouldBreakObstaclesDuringPathfinding();
        }

        if (mover instanceof IsoZombie zombie) {
            this.canCrawl = zombie.isCrawling() || zombie.isCanCrawlUnderVehicle();
            this.ignoreCrawlCost = zombie.isCrawling() && !zombie.isCanWalk();
            this.canThump = true;
        }

        this.minLevel = 0;
        this.maxLevel = 63;
        this.allowedChunkLevels.clear();
        this.allowedLevelTransitions.clear();
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetXyz.resetQuick();
        this.path.clear();
        this.cancel = false;
        if (mover instanceof IsoGameCharacter chr) {
            ArrayList<KnownBlockedEdges> knownBlockedEdges1 = chr.getMapKnowledge().getKnownBlockedEdges();

            for (int i = 0; i < knownBlockedEdges1.size(); i++) {
                KnownBlockedEdges kbe = knownBlockedEdges1.get(i);
                this.knownBlockedEdges.add(KnownBlockedEdges.alloc().init(kbe));
            }
        }

        return this;
    }

    void addTargetXYZ(float targetX, float targetY, float targetZ) {
        this.targetXyz.add(targetX);
        this.targetXyz.add(targetY);
        this.targetXyz.add(targetZ);
    }

    static PathFindRequest alloc() {
        return pool.isEmpty() ? new PathFindRequest() : pool.pop();
    }

    public void release() {
        KnownBlockedEdges.releaseAll(this.knownBlockedEdges);
        this.knownBlockedEdges.clear();

        assert !pool.contains(this);

        pool.push(this);
    }
}
