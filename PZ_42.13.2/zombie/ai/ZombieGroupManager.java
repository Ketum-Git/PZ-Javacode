// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.VirtualZombieManager;
import zombie.ai.states.PathFindState;
import zombie.ai.states.WalkTowardState;
import zombie.ai.states.ZombieIdleState;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.ZombieGroup;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class ZombieGroupManager {
    public static final ZombieGroupManager instance = new ZombieGroupManager();
    private final ArrayList<ZombieGroup> groups = new ArrayList<>();
    private final ArrayDeque<ZombieGroup> freeGroups = new ArrayDeque<>();
    private final Vector2 tempVec2 = new Vector2();
    private final Vector3 tempVec3 = new Vector3();
    private float tickCount = 30.0F;

    public void preupdate() {
        this.tickCount = this.tickCount + GameTime.getInstance().getThirtyFPSMultiplier();
        if (this.tickCount >= 30.0F) {
            this.tickCount = 0.0F;
        }

        int groupSize = SandboxOptions.instance.zombieConfig.rallyGroupSize.getValue();

        for (int i = 0; i < this.groups.size(); i++) {
            ZombieGroup group = this.groups.get(i);
            group.update();
            if (group.isEmpty()) {
                this.freeGroups.push(group);
                this.groups.remove(i--);
            }
        }
    }

    public void Reset() {
        this.freeGroups.addAll(this.groups);
        this.groups.clear();
    }

    public boolean shouldBeInGroup(IsoZombie zombie) {
        if (zombie == null) {
            return false;
        } else if (SandboxOptions.instance.zombieConfig.rallyGroupSize.getValue() <= 1) {
            return false;
        } else if (!Core.getInstance().isZombieGroupSound()) {
            return false;
        } else if (zombie.isUseless()) {
            return false;
        } else if (zombie.isDead() || zombie.isFakeDead()) {
            return false;
        } else if (zombie.isSitAgainstWall()) {
            return false;
        } else if (zombie.target != null) {
            return false;
        } else if (zombie.getCurrentBuilding() != null) {
            return false;
        } else if (VirtualZombieManager.instance.isReused(zombie)) {
            return false;
        } else if (zombie.isReanimatedForGrappleOnly()) {
            return false;
        } else {
            IsoGridSquare sq = zombie.getSquare();
            Zone zone = sq == null ? null : sq.getZone();
            return zone == null || !"Forest".equals(zone.getType()) && !"DeepForest".equals(zone.getType());
        }
    }

    public void update(IsoZombie zombie) {
        if (!GameClient.client || !zombie.isRemoteZombie()) {
            if (!this.shouldBeInGroup(zombie)) {
                if (zombie.group != null) {
                    zombie.group.remove(zombie);
                }
            } else if (this.tickCount == 0.0F) {
                if (zombie.group == null) {
                    ZombieGroup group = this.findNearestGroup(zombie.getX(), zombie.getY(), zombie.getZ());
                    if (group == null) {
                        group = this.freeGroups.isEmpty() ? new ZombieGroup() : this.freeGroups.pop().reset();
                        group.add(zombie);
                        this.groups.add(group);
                        return;
                    }

                    group.add(zombie);
                }

                if (zombie.getCurrentState() == ZombieIdleState.instance()) {
                    if (zombie == zombie.group.getLeader()) {
                        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
                        zombie.group.lastSpreadOutTime = Math.min(zombie.group.lastSpreadOutTime, worldAge);
                        if (!(zombie.group.lastSpreadOutTime + 0.083333336F > worldAge)) {
                            zombie.group.lastSpreadOutTime = worldAge;
                            int GROUP_SEPARATION_DISTANCE = SandboxOptions.instance.zombieConfig.rallyGroupSeparation.getValue();
                            Vector2 c = this.tempVec2.set(0.0F, 0.0F);

                            for (int i = 0; i < this.groups.size(); i++) {
                                ZombieGroup other = this.groups.get(i);
                                if (other.getLeader() != null
                                    && other != zombie.group
                                    && PZMath.fastfloor(other.getLeader().getZ()) == PZMath.fastfloor(zombie.getZ())) {
                                    float otherX = other.getLeader().getX();
                                    float otherY = other.getLeader().getY();
                                    float dist = IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), otherX, otherY);
                                    if (!(dist > GROUP_SEPARATION_DISTANCE * GROUP_SEPARATION_DISTANCE)) {
                                        c.x = c.x - otherX + zombie.getX();
                                        c.y = c.y - otherY + zombie.getY();
                                    }
                                }
                            }

                            int steps = this.lineClearCollideCount(
                                zombie,
                                zombie.getCell(),
                                PZMath.fastfloor(zombie.getX() + c.x),
                                PZMath.fastfloor(zombie.getY() + c.y),
                                PZMath.fastfloor(zombie.getZ()),
                                PZMath.fastfloor(zombie.getX()),
                                PZMath.fastfloor(zombie.getY()),
                                PZMath.fastfloor(zombie.getZ()),
                                10,
                                this.tempVec3
                            );
                            if (steps >= 1) {
                                if (GameClient.client || GameServer.server || !(IsoPlayer.getInstance().getHoursSurvived() < 2.0)) {
                                    if (!(this.tempVec3.x < 0.0F)
                                        && !(this.tempVec3.y < 0.0F)
                                        && IsoWorld.instance
                                            .metaGrid
                                            .isValidChunk(PZMath.fastfloor(this.tempVec3.x) / 8, PZMath.fastfloor(this.tempVec3.y) / 8)) {
                                        zombie.pathToLocation(
                                            PZMath.fastfloor(this.tempVec3.x + 0.5F),
                                            PZMath.fastfloor(this.tempVec3.y + 0.5F),
                                            PZMath.fastfloor(this.tempVec3.z)
                                        );
                                        if (zombie.getCurrentState() == PathFindState.instance() || zombie.getCurrentState() == WalkTowardState.instance()) {
                                            zombie.setLastHeardSound(zombie.getPathTargetX(), zombie.getPathTargetY(), zombie.getPathTargetZ());
                                            zombie.allowRepathDelay = 400.0F;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        float leaderX = zombie.group.getLeader().getX();
                        float leaderY = zombie.group.getLeader().getY();
                        int memberDist = SandboxOptions.instance.zombieConfig.rallyGroupRadius.getValue();
                        if (!(IsoUtils.DistanceToSquared(zombie.getX(), zombie.getY(), leaderX, leaderY) < memberDist * memberDist)) {
                            if (GameClient.client || GameServer.server || !(IsoPlayer.getInstance().getHoursSurvived() < 2.0) || Core.debug) {
                                int randX = PZMath.fastfloor(leaderX + Rand.Next(-memberDist, memberDist));
                                int randY = PZMath.fastfloor(leaderY + Rand.Next(-memberDist, memberDist));
                                if (randX >= 0 && randY >= 0 && IsoWorld.instance.metaGrid.isValidChunk(randX / 8, randY / 8)) {
                                    zombie.pathToLocation(randX, randY, PZMath.fastfloor(zombie.group.getLeader().getZ()));
                                    if (zombie.getCurrentState() == PathFindState.instance() || zombie.getCurrentState() == WalkTowardState.instance()) {
                                        zombie.setLastHeardSound(zombie.getPathTargetX(), zombie.getPathTargetY(), zombie.getPathTargetZ());
                                        zombie.allowRepathDelay = 400.0F;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public ZombieGroup findNearestGroup(float x, float y, float z) {
        ZombieGroup nearest = null;
        float minDist = Float.MAX_VALUE;
        int rallyDist = SandboxOptions.instance.zombieConfig.rallyTravelDistance.getValue();

        for (int i = 0; i < this.groups.size(); i++) {
            ZombieGroup group = this.groups.get(i);
            int idealSize = (int)(SandboxOptions.instance.zombieConfig.rallyGroupSize.getValue() * group.idealSizeFactor);
            if (idealSize < 1) {
                idealSize = 1;
            }

            if (group.isEmpty()) {
                this.groups.remove(i--);
            } else if (PZMath.fastfloor(group.getLeader().getZ()) == PZMath.fastfloor(z) && group.size() < idealSize) {
                float dist = IsoUtils.DistanceToSquared(x, y, group.getLeader().getX(), group.getLeader().getY());
                if (dist < rallyDist * rallyDist && dist < minDist) {
                    minDist = dist;
                    nearest = group;
                }
            }
        }

        return nearest;
    }

    private int lineClearCollideCount(IsoMovingObject chr, IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0, int returnMin, Vector3 out) {
        int l = 0;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5F;
        float t2 = 0.5F;
        IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
        out.set(x0, y0, z0);
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / dx;
            float m2 = (float)dz / dx;
            t += y0;
            t2 += z0;
            dx = dx < 0 ? -1 : 1;
            m *= dx;
            m2 *= dx;

            while (x0 != x1) {
                x0 += dx;
                t += m;
                t2 += m2;
                IsoGridSquare a = cell.getGridSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    boolean bTest = a.testCollideAdjacent(chr, b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
                    if (bTest) {
                        return l;
                    }
                }

                b = a;
                int var35 = PZMath.fastfloor(t);
                int var36 = PZMath.fastfloor(t2);
                out.set(x0, var35, var36);
                if (++l >= returnMin) {
                    return l;
                }
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / dy;
            float m2 = (float)dz / dy;
            t += x0;
            t2 += z0;
            dy = dy < 0 ? -1 : 1;
            m *= dy;
            m2 *= dy;

            while (y0 != y1) {
                y0 += dy;
                t += m;
                t2 += m2;
                IsoGridSquare ax = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                if (ax != null && b != null) {
                    boolean bTest = ax.testCollideAdjacent(chr, b.getX() - ax.getX(), b.getY() - ax.getY(), b.getZ() - ax.getZ());
                    if (bTest) {
                        return l;
                    }
                }

                b = ax;
                int var34 = PZMath.fastfloor(t);
                int lz = PZMath.fastfloor(t2);
                out.set(var34, y0, lz);
                if (++l >= returnMin) {
                    return l;
                }
            }
        } else {
            float m = (float)dx / dz;
            float m2 = (float)dy / dz;
            t += x0;
            t2 += y0;
            dz = dz < 0 ? -1 : 1;
            m *= dz;
            m2 *= dz;

            while (z0 != z1) {
                z0 += dz;
                t += m;
                t2 += m2;
                IsoGridSquare axx = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                if (axx != null && b != null) {
                    boolean bTest = axx.testCollideAdjacent(chr, b.getX() - axx.getX(), b.getY() - axx.getY(), b.getZ() - axx.getZ());
                    if (bTest) {
                        return l;
                    }
                }

                b = axx;
                int lx = PZMath.fastfloor(t);
                int ly = PZMath.fastfloor(t2);
                out.set(lx, ly, z0);
                if (++l >= returnMin) {
                    return l;
                }
            }
        }

        return l;
    }
}
