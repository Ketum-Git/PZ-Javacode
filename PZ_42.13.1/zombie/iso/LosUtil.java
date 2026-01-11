// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.math.PZMath;

@UsedFromLua
public final class LosUtil {
    public static int sizeX = 200;
    public static int sizeY = 200;
    public static int sizeZ = 16;
    public static LosUtil.PerPlayerData[] cachedresults = new LosUtil.PerPlayerData[4];
    public static boolean[] cachecleared = new boolean[4];

    public static void init(int width, int height) {
        sizeX = Math.min(width, 200);
        sizeY = Math.min(height, 200);
    }

    public static LosUtil.TestResults lineClear(IsoCell cell, int x0, int y0, int z0, int x1, int y1, int z1, boolean bIgnoreDoors) {
        return lineClear(cell, x0, y0, z0, x1, y1, z1, bIgnoreDoors, 10000);
    }

    public static LosUtil.TestResults lineClear(IsoCell cell, int x0, int y0, int z0, int x1, int y1, int z1, boolean bIgnoreDoors, int RangeTillWindows) {
        if (z1 == z0 - 1) {
            IsoGridSquare sq = cell.getGridSquare(x1, y1, z1);
            if (sq != null && sq.HasElevatedFloor()) {
                z1 = z0;
            }
        }

        LosUtil.TestResults test = LosUtil.TestResults.Clear;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5F;
        float t2 = 0.5F;
        IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
        int dist = 0;
        boolean windowChange = false;
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / dx;
            float m2 = (float)dz / dx;
            t += y0;
            t2 += z0;
            dx = dx < 0 ? -1 : 1;
            m *= dx;

            for (float var47 = m2 * dx; x0 != x1; windowChange = false) {
                x0 += dx;
                t += m;
                t2 += var47;
                IsoGridSquare a = cell.getGridSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    LosUtil.TestResults newTest = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors);
                    if (newTest == LosUtil.TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }

                    if (newTest != LosUtil.TestResults.Blocked
                        && test != LosUtil.TestResults.Clear
                        && (newTest != LosUtil.TestResults.ClearThroughWindow || test != LosUtil.TestResults.ClearThroughOpenDoor)) {
                        if (newTest == LosUtil.TestResults.ClearThroughClosedDoor && test == LosUtil.TestResults.ClearThroughOpenDoor) {
                            test = newTest;
                        }
                    } else {
                        test = newTest;
                    }

                    if (test == LosUtil.TestResults.Blocked) {
                        return LosUtil.TestResults.Blocked;
                    }

                    if (windowChange) {
                        if (dist > RangeTillWindows) {
                            return LosUtil.TestResults.Blocked;
                        }

                        dist = 0;
                    }
                }

                b = a;
                int var36 = PZMath.fastfloor(t);
                int var37 = PZMath.fastfloor(t2);
                dist++;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / dy;
            float m2 = (float)dz / dy;
            t += x0;
            t2 += z0;
            dy = dy < 0 ? -1 : 1;
            m *= dy;

            for (float var45 = m2 * dy; y0 != y1; windowChange = false) {
                y0 += dy;
                t += m;
                t2 += var45;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    LosUtil.TestResults newTestx = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors);
                    if (newTestx == LosUtil.TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }

                    if (newTestx != LosUtil.TestResults.Blocked
                        && test != LosUtil.TestResults.Clear
                        && (newTestx != LosUtil.TestResults.ClearThroughWindow || test != LosUtil.TestResults.ClearThroughOpenDoor)) {
                        if (newTestx == LosUtil.TestResults.ClearThroughClosedDoor && test == LosUtil.TestResults.ClearThroughOpenDoor) {
                            test = newTestx;
                        }
                    } else {
                        test = newTestx;
                    }

                    if (test == LosUtil.TestResults.Blocked) {
                        return LosUtil.TestResults.Blocked;
                    }

                    if (windowChange) {
                        if (dist > RangeTillWindows) {
                            return LosUtil.TestResults.Blocked;
                        }

                        dist = 0;
                    }
                }

                b = a;
                int var35 = PZMath.fastfloor(t);
                int lz = PZMath.fastfloor(t2);
                dist++;
            }
        } else {
            float m = (float)dx / dz;
            float m2 = (float)dy / dz;
            t += x0;
            t2 += y0;
            dz = dz < 0 ? -1 : 1;
            m *= dz;

            for (float var43 = m2 * dz; z0 != z1; windowChange = false) {
                z0 += dz;
                t += m;
                t2 += var43;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                if (a != null && b != null) {
                    LosUtil.TestResults newTestxx = a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors);
                    if (newTestxx == LosUtil.TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }

                    if (newTestxx != LosUtil.TestResults.Blocked
                        && test != LosUtil.TestResults.Clear
                        && (newTestxx != LosUtil.TestResults.ClearThroughWindow || test != LosUtil.TestResults.ClearThroughOpenDoor)) {
                        if (newTestxx == LosUtil.TestResults.ClearThroughClosedDoor && test == LosUtil.TestResults.ClearThroughOpenDoor) {
                            test = newTestxx;
                        }
                    } else {
                        test = newTestxx;
                    }

                    if (test == LosUtil.TestResults.Blocked) {
                        return LosUtil.TestResults.Blocked;
                    }

                    if (windowChange) {
                        if (dist > RangeTillWindows) {
                            return LosUtil.TestResults.Blocked;
                        }

                        dist = 0;
                    }
                }

                b = a;
                int lx = PZMath.fastfloor(t);
                int ly = PZMath.fastfloor(t2);
                dist++;
            }
        }

        return test;
    }

    public static boolean lineClearCollide(int x1, int y1, int z1, int x0, int y0, int z0, boolean bIgnoreDoors) {
        IsoCell cell = IsoWorld.instance.currentCell;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5F;
        float t2 = 0.5F;
        IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
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
                    boolean bBlocked = a.CalculateCollide(b, false, false, true, true);
                    if (!bIgnoreDoors && a.isDoorBlockedTo(b)) {
                        bBlocked = true;
                    }

                    if (bBlocked) {
                        return true;
                    }
                }

                b = a;
                int var31 = PZMath.fastfloor(t);
                int var32 = PZMath.fastfloor(t2);
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
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    boolean bBlockedx = a.CalculateCollide(b, false, false, true, true);
                    if (!bIgnoreDoors && a.isDoorBlockedTo(b)) {
                        bBlockedx = true;
                    }

                    if (bBlockedx) {
                        return true;
                    }
                }

                b = a;
                int var30 = PZMath.fastfloor(t);
                int lz = PZMath.fastfloor(t2);
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
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                if (a != null && b != null) {
                    boolean bBlockedxx = a.CalculateCollide(b, false, false, true, true);
                    if (bBlockedxx) {
                        return true;
                    }
                }

                b = a;
                int lx = PZMath.fastfloor(t);
                int ly = PZMath.fastfloor(t2);
            }
        }

        return false;
    }

    public static int lineClearCollideCount(IsoGameCharacter chr, IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0) {
        int l = 0;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5F;
        float t2 = 0.5F;
        IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
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
                    boolean bTest = b.testCollideAdjacent(chr, a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
                    if (bTest) {
                        return l;
                    }
                }

                l++;
                b = a;
                int var32 = PZMath.fastfloor(t);
                int var33 = PZMath.fastfloor(t2);
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
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    boolean bTest = b.testCollideAdjacent(chr, a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
                    if (bTest) {
                        return l;
                    }
                }

                l++;
                b = a;
                int var31 = PZMath.fastfloor(t);
                int lz = PZMath.fastfloor(t2);
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
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                if (a != null && b != null) {
                    boolean bTest = b.testCollideAdjacent(chr, a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
                    if (bTest) {
                        return l;
                    }
                }

                l++;
                b = a;
                int lx = PZMath.fastfloor(t);
                int ly = PZMath.fastfloor(t2);
            }
        }

        return l;
    }

    public static LosUtil.TestResults lineClearCached(IsoCell cell, int x1, int y1, int z1, int x0, int y0, int z0, boolean bIgnoreDoors, int playerIndex) {
        if (z1 == z0 - 1) {
            IsoGridSquare sq = cell.getGridSquare(x1, y1, z1);
            if (sq != null && sq.HasElevatedFloor()) {
                z1 = z0;
            }
        }

        int sx = x0;
        int sy = y0;
        int sz = z0;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        int cx = dx + sizeX / 2;
        int cy = dy + sizeY / 2;
        int cz = dz + sizeZ / 2;
        if (cx >= 0 && cy >= 0 && cz >= 0 && cx < sizeX && cy < sizeY && cz < sizeZ) {
            LosUtil.TestResults res = LosUtil.TestResults.Clear;
            int resultToPropagate = 1;
            LosUtil.PerPlayerData ppd = LosUtil.cachedresults[playerIndex];
            ppd.checkSize();
            byte[][][] cachedresults = ppd.cachedresults;
            if (cachedresults[cx][cy][cz] != 0) {
                if (cachedresults[cx][cy][cz] == 1) {
                    res = LosUtil.TestResults.Clear;
                }

                if (cachedresults[cx][cy][cz] == 2) {
                    res = LosUtil.TestResults.ClearThroughOpenDoor;
                }

                if (cachedresults[cx][cy][cz] == 3) {
                    res = LosUtil.TestResults.ClearThroughWindow;
                }

                if (cachedresults[cx][cy][cz] == 4) {
                    res = LosUtil.TestResults.Blocked;
                }

                if (cachedresults[cx][cy][cz] == 5) {
                    res = LosUtil.TestResults.ClearThroughClosedDoor;
                }

                return res;
            } else {
                float t = 0.5F;
                float t2 = 0.5F;
                IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
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
                            if (resultToPropagate != 4
                                && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors)
                                    == LosUtil.TestResults.Blocked) {
                                resultToPropagate = 4;
                            }

                            int cx2 = x0 - sx;
                            int cy2 = PZMath.fastfloor(t) - sy;
                            int cz2 = PZMath.fastfloor(t2) - sz;
                            cx2 += sizeX / 2;
                            cy2 += sizeY / 2;
                            cz2 += sizeZ / 2;
                            if (cachedresults[cx2][cy2][cz2] == 0) {
                                cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                            }
                        } else {
                            int cx2 = x0 - sx;
                            int cy2 = PZMath.fastfloor(t) - sy;
                            int cz2 = PZMath.fastfloor(t2) - sz;
                            cx2 += sizeX / 2;
                            cy2 += sizeY / 2;
                            cz2 += sizeZ / 2;
                            if (cachedresults[cx2][cy2][cz2] == 0) {
                                cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                            }
                        }

                        b = a;
                        int var44 = PZMath.fastfloor(t);
                        int var45 = PZMath.fastfloor(t2);
                    }
                } else {
                    t += x0;
                    if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
                        float m = (float)dx / dy;
                        float m2 = (float)dz / dy;
                        t2 += z0;
                        dy = dy < 0 ? -1 : 1;
                        m *= dy;
                        m2 *= dy;

                        while (y0 != y1) {
                            y0 += dy;
                            t += m;
                            t2 += m2;
                            IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                            if (a != null && b != null) {
                                if (resultToPropagate != 4
                                    && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors)
                                        == LosUtil.TestResults.Blocked) {
                                    resultToPropagate = 4;
                                }

                                int cx2 = PZMath.fastfloor(t) - sx;
                                int cy2 = PZMath.fastfloor((float)y0) - sy;
                                int cz2 = PZMath.fastfloor(t2) - sz;
                                cx2 += sizeX / 2;
                                cy2 += sizeY / 2;
                                cz2 += sizeZ / 2;
                                if (cachedresults[cx2][cy2][cz2] == 0) {
                                    cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                                }
                            } else {
                                int cx2 = PZMath.fastfloor(t) - sx;
                                int cy2 = PZMath.fastfloor((float)y0) - sy;
                                int cz2 = PZMath.fastfloor(t2) - sz;
                                cx2 += sizeX / 2;
                                cy2 += sizeY / 2;
                                cz2 += sizeZ / 2;
                                if (0 == cachedresults[cx2][cy2][cz2]) {
                                    cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                                }
                            }

                            b = a;
                            int var43 = PZMath.fastfloor(t);
                            int lz = PZMath.fastfloor(t2);
                        }
                    } else {
                        float m = (float)dx / dz;
                        float m2 = (float)dy / dz;
                        t2 += y0;
                        dz = dz < 0 ? -1 : 1;
                        m *= dz;
                        m2 *= dz;

                        while (z0 != z1) {
                            z0 += dz;
                            t += m;
                            t2 += m2;
                            IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                            if (a != null && b != null) {
                                if (resultToPropagate != 4
                                    && a.testVisionAdjacent(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ(), true, bIgnoreDoors)
                                        == LosUtil.TestResults.Blocked) {
                                    resultToPropagate = 4;
                                }

                                int cx2 = PZMath.fastfloor(t) - sx;
                                int cy2 = PZMath.fastfloor(t2) - sy;
                                int cz2 = PZMath.fastfloor((float)z0) - sz;
                                cx2 += sizeX / 2;
                                cy2 += sizeY / 2;
                                cz2 += sizeZ / 2;
                                if (cachedresults[cx2][cy2][cz2] == 0) {
                                    cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                                }
                            } else {
                                int cx2 = PZMath.fastfloor(t) - sx;
                                int cy2 = PZMath.fastfloor(t2) - sy;
                                int cz2 = PZMath.fastfloor((float)z0) - sz;
                                cx2 += sizeX / 2;
                                cy2 += sizeY / 2;
                                cz2 += sizeZ / 2;
                                if (cachedresults[cx2][cy2][cz2] == 0) {
                                    cachedresults[cx2][cy2][cz2] = (byte)resultToPropagate;
                                }
                            }

                            b = a;
                            int lx = PZMath.fastfloor(t);
                            int ly = PZMath.fastfloor(t2);
                        }
                    }
                }

                if (resultToPropagate == 1) {
                    cachedresults[cx][cy][cz] = (byte)resultToPropagate;
                    return LosUtil.TestResults.Clear;
                } else if (resultToPropagate == 2) {
                    cachedresults[cx][cy][cz] = (byte)resultToPropagate;
                    return LosUtil.TestResults.ClearThroughOpenDoor;
                } else if (resultToPropagate == 3) {
                    cachedresults[cx][cy][cz] = (byte)resultToPropagate;
                    return LosUtil.TestResults.ClearThroughWindow;
                } else if (resultToPropagate == 4) {
                    cachedresults[cx][cy][cz] = (byte)resultToPropagate;
                    return LosUtil.TestResults.Blocked;
                } else if (resultToPropagate == 5) {
                    cachedresults[cx][cy][cz] = (byte)resultToPropagate;
                    return LosUtil.TestResults.ClearThroughClosedDoor;
                } else {
                    return LosUtil.TestResults.Blocked;
                }
            }
        } else {
            return LosUtil.TestResults.Blocked;
        }
    }

    public static IsoGridSquareCollisionData getFirstBlockingIsoGridSquare(IsoCell cell, int x0, int y0, int z0, int x1, int y1, int z1, boolean bIgnoreDoors) {
        Vector3 midPoint = new Vector3();
        int RangeTillWindows = 10000;
        IsoGridSquareCollisionData isoGridSquareCollisionData = new IsoGridSquareCollisionData();
        if (z1 == z0 - 1) {
            IsoGridSquare sq = cell.getGridSquare(x1, y1, z1);
            if (sq != null && sq.HasElevatedFloor()) {
                z1 = z0;
            }
        }

        LosUtil.TestResults test = LosUtil.TestResults.Clear;
        int dy = y1 - y0;
        int dx = x1 - x0;
        int dz = z1 - z0;
        float t = 0.5F;
        float t2 = 0.5F;
        IsoGridSquare b = cell.getGridSquare(x0, y0, z0);
        int dist = 0;
        boolean windowChange = false;
        if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > Math.abs(dz)) {
            float m = (float)dy / dx;
            float m2 = (float)dz / dx;
            t += y0;
            t2 += z0;
            dx = dx < 0 ? -1 : 1;
            m *= dx;

            for (float var53 = m2 * dx; x0 != x1; windowChange = false) {
                x0 += dx;
                t += m;
                t2 += var53;
                IsoGridSquare a = cell.getGridSquare(x0, PZMath.fastfloor(t), PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    int x = b.getX() - a.getX();
                    int y = b.getY() - a.getY();
                    int z = b.getZ() - a.getZ();
                    midPoint.x = (a.getX() + b.getX()) * 0.5F;
                    midPoint.y = (a.getY() + b.getY()) * 0.5F;
                    midPoint.z = (a.getZ() + b.getZ()) * 0.5F;
                    a.getFirstBlocking(isoGridSquareCollisionData, x, y, z, true, bIgnoreDoors);
                    LosUtil.TestResults newTest = isoGridSquareCollisionData.testResults;
                    if (newTest == LosUtil.TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }

                    if (newTest != LosUtil.TestResults.Blocked
                        && test != LosUtil.TestResults.Clear
                        && (newTest != LosUtil.TestResults.ClearThroughWindow || test != LosUtil.TestResults.ClearThroughOpenDoor)) {
                        if (newTest == LosUtil.TestResults.ClearThroughClosedDoor && test == LosUtil.TestResults.ClearThroughOpenDoor) {
                            test = newTest;
                        }
                    } else {
                        test = newTest;
                    }

                    if (test == LosUtil.TestResults.Blocked) {
                        IsoGridSquare blockingIsoGridSquare = null;
                        if (x < 0) {
                            midPoint.x -= x;
                            midPoint.y -= y;
                            midPoint.z -= z;
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(a.getX(), a.getY(), a.getZ());
                        } else {
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(b.getX(), b.getY(), b.getZ());
                        }

                        isoGridSquareCollisionData.isoGridSquare = blockingIsoGridSquare;
                        isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }

                    if (windowChange) {
                        if (dist > 10000) {
                            isoGridSquareCollisionData.isoGridSquare = b;
                            isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }

                        dist = 0;
                    }
                }

                b = a;
                int var42 = PZMath.fastfloor(t);
                int var43 = PZMath.fastfloor(t2);
                dist++;
            }
        } else if (Math.abs(dy) >= Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            float m = (float)dx / dy;
            float m2 = (float)dz / dy;
            t += x0;
            t2 += z0;
            dy = dy < 0 ? -1 : 1;
            m *= dy;

            for (float var51 = m2 * dy; y0 != y1; windowChange = false) {
                y0 += dy;
                t += m;
                t2 += var51;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), y0, PZMath.fastfloor(t2));
                if (a != null && b != null) {
                    int xx = b.getX() - a.getX();
                    int yx = b.getY() - a.getY();
                    int zx = b.getZ() - a.getZ();
                    midPoint.x = (b.getX() + a.getX()) * 0.5F;
                    midPoint.y = (b.getY() + a.getY()) * 0.5F;
                    midPoint.z = (b.getZ() + a.getZ()) * 0.5F;
                    a.getFirstBlocking(isoGridSquareCollisionData, xx, yx, zx, true, bIgnoreDoors);
                    LosUtil.TestResults newTestx = isoGridSquareCollisionData.testResults;
                    if (newTestx == LosUtil.TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }

                    if (newTestx != LosUtil.TestResults.Blocked
                        && test != LosUtil.TestResults.Clear
                        && (newTestx != LosUtil.TestResults.ClearThroughWindow || test != LosUtil.TestResults.ClearThroughOpenDoor)) {
                        if (newTestx == LosUtil.TestResults.ClearThroughClosedDoor && test == LosUtil.TestResults.ClearThroughOpenDoor) {
                            test = newTestx;
                        }
                    } else {
                        test = newTestx;
                    }

                    if (test == LosUtil.TestResults.Blocked) {
                        IsoGridSquare blockingIsoGridSquare = null;
                        if (yx < 0) {
                            midPoint.x -= xx;
                            midPoint.y -= yx;
                            midPoint.z -= zx;
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(a.getX(), a.getY(), a.getZ());
                        } else {
                            blockingIsoGridSquare = IsoCell.getInstance().getGridSquare(b.getX(), b.getY(), b.getZ());
                        }

                        isoGridSquareCollisionData.isoGridSquare = blockingIsoGridSquare;
                        isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }

                    if (windowChange) {
                        if (dist > 10000) {
                            isoGridSquareCollisionData.isoGridSquare = b;
                            isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }

                        dist = 0;
                    }
                }

                b = a;
                int var41 = PZMath.fastfloor(t);
                int lz = PZMath.fastfloor(t2);
                dist++;
            }
        } else {
            float m = (float)dx / dz;
            float m2 = (float)dy / dz;
            t += x0;
            t2 += y0;
            dz = dz < 0 ? -1 : 1;
            m *= dz;

            for (float var49 = m2 * dz; z0 != z1; windowChange = false) {
                z0 += dz;
                t += m;
                t2 += var49;
                IsoGridSquare a = cell.getGridSquare(PZMath.fastfloor(t), PZMath.fastfloor(t2), z0);
                if (a != null && b != null) {
                    int xxx = b.getX() - a.getX();
                    int yxx = b.getY() - a.getY();
                    int zxx = b.getZ() - a.getZ();
                    midPoint.x = (b.getX() + a.getX()) * 0.5F;
                    midPoint.y = (b.getY() + a.getY()) * 0.5F;
                    midPoint.z = (b.getZ() + a.getZ()) * 0.5F;
                    a.getFirstBlocking(isoGridSquareCollisionData, xxx, yxx, zxx, true, bIgnoreDoors);
                    LosUtil.TestResults newTestxx = isoGridSquareCollisionData.testResults;
                    if (newTestxx == LosUtil.TestResults.ClearThroughWindow) {
                        windowChange = true;
                    }

                    if (newTestxx != LosUtil.TestResults.Blocked
                        && test != LosUtil.TestResults.Clear
                        && (newTestxx != LosUtil.TestResults.ClearThroughWindow || test != LosUtil.TestResults.ClearThroughOpenDoor)) {
                        if (newTestxx == LosUtil.TestResults.ClearThroughClosedDoor && test == LosUtil.TestResults.ClearThroughOpenDoor) {
                            test = newTestxx;
                        }
                    } else {
                        test = newTestxx;
                    }

                    if (test == LosUtil.TestResults.Blocked) {
                        isoGridSquareCollisionData.isoGridSquare = b;
                        isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                        isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                        return isoGridSquareCollisionData;
                    }

                    if (windowChange) {
                        if (dist > 10000) {
                            isoGridSquareCollisionData.isoGridSquare = b;
                            isoGridSquareCollisionData.hitPosition.set(midPoint.x, midPoint.y, midPoint.z);
                            isoGridSquareCollisionData.testResults = LosUtil.TestResults.Blocked;
                            return isoGridSquareCollisionData;
                        }

                        dist = 0;
                    }
                }

                b = a;
                int lx = PZMath.fastfloor(t);
                int ly = PZMath.fastfloor(t2);
                dist++;
            }
        }

        isoGridSquareCollisionData.isoGridSquare = null;
        isoGridSquareCollisionData.hitPosition.set(0.0F, 0.0F, 0.0F);
        isoGridSquareCollisionData.testResults = test;
        return isoGridSquareCollisionData;
    }

    static {
        for (int n = 0; n < 4; n++) {
            cachecleared[n] = true;
            cachedresults[n] = new LosUtil.PerPlayerData();
        }
    }

    public static final class PerPlayerData {
        public byte[][][] cachedresults;

        public void checkSize() {
            if (this.cachedresults == null
                || this.cachedresults.length != LosUtil.sizeX
                || this.cachedresults[0].length != LosUtil.sizeY
                || this.cachedresults[0][0].length != LosUtil.sizeZ) {
                this.cachedresults = new byte[LosUtil.sizeX][LosUtil.sizeY][LosUtil.sizeZ];
            }
        }
    }

    public static enum TestResults {
        Clear,
        ClearThroughOpenDoor,
        ClearThroughWindow,
        Blocked,
        ClearThroughClosedDoor;
    }
}
