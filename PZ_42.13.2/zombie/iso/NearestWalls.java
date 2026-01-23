// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.ui.TextManager;
import zombie.ui.UIFont;

public class NearestWalls {
    private static final int CPW = 8;
    private static final int CPWx4 = 32;
    private static final int LEVELS = 64;
    private static int changeCount;
    private static int renderX;
    private static int renderY;
    private static int renderZ;

    public static void chunkLoaded(IsoChunk chunk) {
        changeCount++;
        if (changeCount < 0) {
            changeCount = 0;
        }

        for (int z = chunk.getMinLevel(); z <= chunk.getMaxLevel(); z++) {
            IsoChunkLevel chunkLevel = chunk.getLevelData(z);
            if (chunkLevel.nearestWalls != null) {
                chunkLevel.nearestWalls.changeCount = -1;
            }

            if (chunkLevel.nearestExteriorWalls != null) {
                chunkLevel.nearestExteriorWalls.changeCount = -1;
            }
        }
    }

    private static NearestWalls.ChunkLevelData getOrCreateLevelData(IsoChunk chunk, int z, boolean exterior) {
        IsoChunkLevel chunkLevel = chunk.getLevelData(z);
        if (chunkLevel == null) {
            return null;
        } else if (exterior) {
            if (chunkLevel.nearestExteriorWalls == null) {
                chunkLevel.nearestExteriorWalls = new NearestWalls.ChunkLevelData();
            }

            return chunkLevel.nearestExteriorWalls;
        } else {
            if (chunkLevel.nearestWalls == null) {
                chunkLevel.nearestWalls = new NearestWalls.ChunkLevelData();
            }

            return chunkLevel.nearestWalls;
        }
    }

    private static void calcDistanceOnThisChunkOnly(IsoChunk chunk, int z, boolean exterior) {
        int CPWx4 = 32;
        NearestWalls.ChunkLevelData levelData = getOrCreateLevelData(chunk, z, exterior);
        byte[] distance = levelData.distanceSelf;

        for (int y = 0; y < 8; y++) {
            byte wallX = -1;

            for (int x = 0; x < 8; x++) {
                levelData.closest[x + y * 8] = -1;
                int index = x * 4 + y * 32;
                distance[index + 0] = wallX == -1 ? -1 : (byte)(x - wallX);
                distance[index + 1] = -1;
                IsoGridSquare square = chunk.getGridSquare(x, y, z);
                if (square != null
                    && (square.has(IsoFlagType.WallW) || square.has(IsoFlagType.DoorWallW) || square.has(IsoFlagType.WallNW) || square.has(IsoFlagType.WindowW))
                    )
                 {
                    if (exterior) {
                        IsoGridSquare w = square.getAdjacentSquare(IsoDirections.W);
                        if (w == null || square.isInARoom() == w.isInARoom()) {
                            continue;
                        }
                    }

                    wallX = (byte)x;
                    distance[index + 0] = 0;

                    for (int x1 = x - 1; x1 >= 0; x1--) {
                        index = x1 * 4 + y * 32;
                        if (distance[index + 1] != -1) {
                            break;
                        }

                        distance[index + 1] = (byte)(wallX - x1);
                    }
                }
            }
        }

        for (int xx = 0; xx < 8; xx++) {
            byte wallY = -1;

            for (int y = 0; y < 8; y++) {
                int index = xx * 4 + y * 32;
                distance[index + 2] = wallY == -1 ? -1 : (byte)(y - wallY);
                distance[index + 3] = -1;
                IsoGridSquare square = chunk.getGridSquare(xx, y, z);
                if (square != null
                    && (square.has(IsoFlagType.WallN) || square.has(IsoFlagType.DoorWallN) || square.has(IsoFlagType.WallNW) || square.has(IsoFlagType.WindowN))
                    )
                 {
                    if (exterior) {
                        IsoGridSquare n = square.getAdjacentSquare(IsoDirections.N);
                        if (n == null || square.isInARoom() == n.isInARoom()) {
                            continue;
                        }
                    }

                    wallY = (byte)y;
                    distance[index + 2] = 0;

                    for (int y1 = y - 1; y1 >= 0; y1--) {
                        index = xx * 4 + y1 * 32;
                        if (distance[index + 3] != -1) {
                            break;
                        }

                        distance[index + 3] = (byte)(wallY - y1);
                    }
                }
            }
        }
    }

    private static int getIndex(IsoChunk chunk, int x, int y) {
        return (x - chunk.wx * 8) * 4 + (y - chunk.wy * 8) * 32;
    }

    private static int getNearestWallOnSameChunk(IsoChunk chunk, int x, int y, int z, int wall, boolean exterior) {
        NearestWalls.ChunkLevelData levelData = getOrCreateLevelData(chunk, z, exterior);
        if (levelData == null) {
            return -1;
        } else {
            if (levelData.changeCount != changeCount) {
                calcDistanceOnThisChunkOnly(chunk, z, exterior);
                levelData.changeCount = changeCount;
            }

            int index = getIndex(chunk, x, y);
            return levelData.distanceSelf[index + wall];
        }
    }

    private static boolean hasWall(IsoChunk chunk, int x, int y, int z, int wall, boolean exterior) {
        return getNearestWallOnSameChunk(chunk, x, y, z, wall, exterior) == 0;
    }

    private static int getNearestWallWest(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        int wall = 0;
        int dx = -1;
        int dy = 0;
        int dist = getNearestWallOnSameChunk(chunk, x, y, z, 0, exterior);
        if (dist != -1) {
            return x - dist;
        } else {
            for (int d = 1; d <= 3; d++) {
                IsoChunk chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * -1, chunk.wy + d * 0);
                if (chunk2 == null) {
                    break;
                }

                int x2 = (chunk2.wx + 1) * 8 - 1;
                dist = getNearestWallOnSameChunk(chunk2, x2, y, z, 0, exterior);
                if (dist != -1) {
                    return x2 - dist;
                }
            }

            return -1;
        }
    }

    private static int getNearestWallEast(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        int wall = 1;
        int dx = 1;
        int dy = 0;
        int dist = getNearestWallOnSameChunk(chunk, x, y, z, 1, exterior);
        if (dist != -1) {
            return x + dist;
        } else {
            for (int d = 1; d <= 3; d++) {
                IsoChunk chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * 1, chunk.wy + d * 0);
                if (chunk2 == null) {
                    break;
                }

                int x2 = chunk2.wx * 8;
                dist = hasWall(chunk2, x2, y, z, 0, exterior) ? 0 : getNearestWallOnSameChunk(chunk2, x2, y, z, 1, exterior);
                if (dist != -1) {
                    return x2 + dist;
                }
            }

            return -1;
        }
    }

    private static int getNearestWallNorth(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        int wall = 2;
        int dx = 0;
        int dy = -1;
        int dist = getNearestWallOnSameChunk(chunk, x, y, z, 2, exterior);
        if (dist != -1) {
            return y - dist;
        } else {
            for (int d = 1; d <= 3; d++) {
                IsoChunk chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * 0, chunk.wy + d * -1);
                if (chunk2 == null) {
                    break;
                }

                int y2 = (chunk2.wy + 1) * 8 - 1;
                dist = getNearestWallOnSameChunk(chunk2, x, y2, z, 2, exterior);
                if (dist != -1) {
                    return y2 - dist;
                }
            }

            return -1;
        }
    }

    private static int getNearestWallSouth(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        int wall = 3;
        int dx = 0;
        int dy = 1;
        int dist = getNearestWallOnSameChunk(chunk, x, y, z, 3, exterior);
        if (dist != -1) {
            return y + dist;
        } else {
            for (int d = 1; d <= 3; d++) {
                IsoChunk chunk2 = IsoWorld.instance.currentCell.getChunk(chunk.wx + d * 0, chunk.wy + d * 1);
                if (chunk2 == null) {
                    break;
                }

                int y2 = chunk2.wy * 8;
                dist = hasWall(chunk2, x, y2, z, 2, exterior) ? 0 : getNearestWallOnSameChunk(chunk2, x, y2, z, 3, exterior);
                if (dist != -1) {
                    return y2 + dist;
                }
            }

            return -1;
        }
    }

    public static void render(int x, int y, int z, boolean exterior) {
        IsoChunk chunk = IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, z);
        if (chunk != null) {
            if (renderX != x || renderY != y || renderZ != z) {
                renderX = x;
                renderY = y;
                renderZ = z;
                System.out.println("ClosestWallDistance=" + ClosestWallDistance(chunk, x, y, z, exterior));
            }

            int x2 = getNearestWallWest(chunk, x, y, z, exterior);
            if (x2 != -1) {
                DrawIsoLine(x2, y + 0.5F, x + 0.5F, y + 0.5F, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                DrawIsoLine(x2, y, x2, y + 1, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
            }

            x2 = getNearestWallEast(chunk, x, y, z, exterior);
            if (x2 != -1) {
                DrawIsoLine(x2, y + 0.5F, x + 0.5F, y + 0.5F, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                DrawIsoLine(x2, y, x2, y + 1, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
            }

            int y2 = getNearestWallNorth(chunk, x, y, z, exterior);
            if (y2 != -1) {
                DrawIsoLine(x + 0.5F, y2, x + 0.5F, y + 0.5F, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                DrawIsoLine(x, y2, x + 1, y2, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
            }

            y2 = getNearestWallSouth(chunk, x, y, z, exterior);
            if (y2 != -1) {
                DrawIsoLine(x + 0.5F, y2, x + 0.5F, y + 0.5F, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
                DrawIsoLine(x, y2, x + 1, y2, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
            }

            float sx = IsoUtils.XToScreen(x, y, z, 0) - IsoCamera.frameState.offX;
            float sy = IsoUtils.YToScreen(x, y, z, 0) - IsoCamera.frameState.offY - 16 * Core.tileScale;
            TextManager.instance.DrawStringCentre(UIFont.Small, sx, sy, String.format("%d", ClosestWallDistance(chunk, x, y, z, exterior)), 1.0, 1.0, 1.0, 1.0);
        }
    }

    private static void DrawIsoLine(float x, float y, float x2, float y2, float z, float r, float g, float b, float a, int thickness) {
        float sx = IsoUtils.XToScreenExact(x, y, z, 0);
        float sy = IsoUtils.YToScreenExact(x, y, z, 0);
        float sx2 = IsoUtils.XToScreenExact(x2, y2, z, 0);
        float sy2 = IsoUtils.YToScreenExact(x2, y2, z, 0);
        LineDrawer.drawLine(sx, sy, sx2, sy2, r, g, b, a, thickness);
    }

    public static int ClosestWallDistance(IsoGridSquare square, boolean exterior) {
        return square != null && square.chunk != null ? ClosestWallDistance(square.chunk, square.x, square.y, square.z, exterior) : 127;
    }

    public static int ClosestWallDistance(IsoChunk chunk, int x, int y, int z, boolean exterior) {
        if (chunk == null) {
            return 127;
        } else {
            NearestWalls.ChunkLevelData levelData = getOrCreateLevelData(chunk, z, exterior);
            if (levelData == null) {
                return 127;
            } else {
                byte[] closest = levelData.closest;
                if (levelData.changeCount != changeCount) {
                    calcDistanceOnThisChunkOnly(chunk, z, exterior);
                    levelData.changeCount = changeCount;
                }

                int index = x - chunk.wx * 8 + (y - chunk.wy * 8) * 8;
                int dist = closest[index];
                if (dist != -1) {
                    return dist;
                } else {
                    int west = getNearestWallWest(chunk, x, y, z, exterior);
                    int east = getNearestWallEast(chunk, x, y, z, exterior);
                    int north = getNearestWallNorth(chunk, x, y, z, exterior);
                    int south = getNearestWallSouth(chunk, x, y, z, exterior);
                    if (west == -1 && east == -1 && north == -1 && south == -1) {
                        return closest[index] = (byte)127;
                    } else if (exterior) {
                        int min = 127;
                        if (west != -1) {
                            min = PZMath.min(min, x - west);
                        }

                        if (east != -1) {
                            min = PZMath.min(min, east - x - 1);
                        }

                        if (north != -1) {
                            min = PZMath.min(min, y - north);
                        }

                        if (south != -1) {
                            min = PZMath.min(min, south - y - 1);
                        }

                        return closest[index] = (byte)min;
                    } else {
                        int westEast = -1;
                        if (west != -1 && east != -1) {
                            westEast = east - west;
                        }

                        int northSouth = -1;
                        if (north != -1 && south != -1) {
                            northSouth = south - north;
                        }

                        if (westEast != -1 && northSouth != -1) {
                            return closest[index] = (byte)Math.min(westEast, northSouth);
                        } else if (westEast != -1) {
                            return closest[index] = (byte)westEast;
                        } else if (northSouth != -1) {
                            return closest[index] = (byte)northSouth;
                        } else {
                            IsoGridSquare square = chunk.getGridSquare(x - chunk.wx * 8, y - chunk.wy * 8, z);
                            if (square != null && square.isOutside()) {
                                west = west == -1 ? 127 : x - west;
                                east = east == -1 ? 127 : east - x - 1;
                                north = north == -1 ? 127 : y - north;
                                south = south == -1 ? 127 : south - y - 1;
                                return closest[index] = (byte)Math.min(west, Math.min(east, Math.min(north, south)));
                            } else {
                                return closest[index] = (byte)127;
                            }
                        }
                    }
                }
            }
        }
    }

    public static final class ChunkLevelData {
        int changeCount = -1;
        final byte[] distanceSelf = new byte[256];
        final byte[] closest = new byte[64];
    }
}
