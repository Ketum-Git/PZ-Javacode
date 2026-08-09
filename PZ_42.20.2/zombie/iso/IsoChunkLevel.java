// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.list.array.TIntArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import zombie.audio.FMODAmbientWallLevelData;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.iso.fboRenderChunk.FBORenderCutaways;
import zombie.iso.objects.RainManager;

public final class IsoChunkLevel {
    public static final byte CLDSF_NONE = 0;
    public static final byte CLDSF_SHOULD_RENDER = 1;
    public static final byte CLDSF_RAIN_RANDOM_XY = 2;
    public IsoChunk chunk;
    public int level;
    public final IsoGridSquare[] squares = new IsoGridSquare[64];
    public final boolean[] lightCheck = new boolean[4];
    public boolean physicsCheck;
    private final TIntArrayList objectPoweredByGeneratorLocations = new TIntArrayList();
    public final byte[] rainFlags = new byte[64];
    public final float[] rainSplashFrame = new float[64];
    public boolean raining;
    public int rainSplashFrameNum = -1;
    public CorpseCount.ChunkLevelData corpseCount;
    public FMODAmbientWallLevelData fmodAmbientWallLevelData;
    public NearestWalls.ChunkLevelData nearestWalls;
    public NearestWalls.ChunkLevelData nearestExteriorWalls;
    private static final ConcurrentLinkedQueue<IsoChunkLevel> pool = new ConcurrentLinkedQueue<>();

    public IsoChunkLevel init(IsoChunk chunk, int level) {
        this.chunk = chunk;
        this.level = level;
        Arrays.fill(this.rainSplashFrame, -1.0F);
        return this;
    }

    public IsoChunk getChunk() {
        return this.chunk;
    }

    public int getLevel() {
        return this.level;
    }

    public void updateRainSplashes() {
        if (this.rainSplashFrameNum != IsoCamera.frameState.frameCount) {
            this.rainSplashFrameNum = IsoCamera.frameState.frameCount;
            boolean bRaining = IsoWorld.instance.currentCell.getRainIntensity() > 0 || RainManager.isRaining() && RainManager.rainIntensity > 0.0F;
            if (bRaining) {
                this.raining = true;
                if (!IsoCamera.frameState.paused) {
                    int intensity = IsoWorld.instance.currentCell.getRainIntensity();
                    if (intensity == 0) {
                        intensity = Math.min(PZMath.fastfloor(RainManager.rainIntensity / 0.2F) + 1, 5);
                    }

                    for (int i = 0; i < this.rainSplashFrame.length; i++) {
                        if (this.rainSplashFrame[i] < 0.0F) {
                            if (Rand.NextBool(Rand.AdjustForFramerate((int)(5.0F / intensity) * 100))) {
                                this.rainSplashFrame[i] = 0.0F;
                                this.rainFlags[i] = (byte)(this.rainFlags[i] | 2);
                            }
                        } else {
                            this.rainSplashFrame[i] = this.rainSplashFrame[i] + 0.08F * (30.0F / PerformanceSettings.getLockFPS());
                            if (this.rainSplashFrame[i] >= 1.0F) {
                                this.rainSplashFrame[i] = -1.0F;
                            }
                        }
                    }
                }
            } else {
                if (this.raining) {
                    this.raining = false;
                    Arrays.fill(this.rainSplashFrame, -1.0F);
                }
            }
        }
    }

    public void renderRainSplashes(int playerIndex) {
        if (this.raining) {
            FBORenderCutaways.ChunkLevelData cutawayLevel = this.chunk.getCutawayDataForLevel(this.level);

            for (int i = 0; i < this.rainSplashFrame.length; i++) {
                if (!(this.rainSplashFrame[i] < 0.0F)) {
                    IsoGridSquare square = this.chunk.getGridSquare(i % 8, i / 8, this.level);
                    if (cutawayLevel.shouldRenderSquare(playerIndex, square)) {
                        square.renderRainSplash(playerIndex, square.getLightInfo(playerIndex), this.rainSplashFrame[i], (this.rainFlags[i] & 2) != 0);
                        this.rainFlags[i] = (byte)(this.rainFlags[i] & -3);
                    }
                }
            }
        }
    }

    public void addObjectPoweredByGenerator(IsoObject object) {
        int location = this.getObjectLocation(object);
        if (!this.objectPoweredByGeneratorLocations.contains(location)) {
            this.objectPoweredByGeneratorLocations.add(location);
        }
    }

    public void removeObjectPoweredByGenerator(IsoObject object) {
        int location = this.getObjectLocation(object);
        if (this.countObjectsPoweredByGenerator(location, object) == 0) {
            this.objectPoweredByGeneratorLocations.remove(location);
        }
    }

    public int countObjectsPoweredByGenerator(int x, int y) {
        int location = this.getLocation(x, y);
        return this.countObjectsPoweredByGenerator(location, null);
    }

    private int countObjectsPoweredByGenerator(int location, IsoObject ignore) {
        IsoGridSquare square = this.squares[location];
        if (square == null) {
            return 0;
        } else {
            int count = 0;

            for (int i = 0; i < square.getObjects().size(); i++) {
                IsoObject object = square.getObjects().get(i);
                if (object != ignore && object.couldBePoweredByGenerator()) {
                    count++;
                }
            }

            return count;
        }
    }

    public void appendObjectsPoweredByGenerator(List<IsoObject> objects) {
        for (int i = 0; i < this.objectPoweredByGeneratorLocations.size(); i++) {
            int location = this.objectPoweredByGeneratorLocations.get(i);
            this.appendObjectsPoweredByGenerator(location, objects);
        }
    }

    private void appendObjectsPoweredByGenerator(int location, List<IsoObject> objects) {
        IsoGridSquare square = this.squares[location];
        if (square != null) {
            for (int i = 0; i < square.getObjects().size(); i++) {
                IsoObject object = square.getObjects().get(i);
                if (object.couldBePoweredByGenerator()) {
                    objects.add(object);
                }
            }
        }
    }

    private int getObjectLocation(IsoObject object) {
        return this.getLocation(object.getSquare().getX(), object.getSquare().getY());
    }

    private int getLocation(int x, int y) {
        int lx = PZMath.coordmodulo(x, 8);
        int ly = PZMath.coordmodulo(y, 8);
        return lx + ly * 8;
    }

    public void clear() {
        Arrays.fill(this.squares, null);
        Arrays.fill(this.lightCheck, true);
        this.physicsCheck = false;
        this.objectPoweredByGeneratorLocations.clear();
        Arrays.fill(this.rainFlags, (byte)0);
        Arrays.fill(this.rainSplashFrame, -1.0F);
        this.raining = false;
        this.rainSplashFrameNum = -1;
    }

    public static IsoChunkLevel alloc() {
        IsoChunkLevel obj = pool.poll();
        if (obj == null) {
            obj = new IsoChunkLevel();
        }

        return obj;
    }

    public void release() {
        if (this.fmodAmbientWallLevelData != null) {
            this.fmodAmbientWallLevelData.release();
            this.fmodAmbientWallLevelData = null;
        }

        pool.add(this);
    }

    public void checkPhysicsLaterForActiveRagdoll() {
        this.chunk.checkPhysicsLaterForActiveRagdoll(this);
    }

    public boolean containsIsoGridSquare(IsoGridSquare isoGridSquare) {
        return Arrays.stream(this.squares).anyMatch(square -> square == isoGridSquare);
    }
}
