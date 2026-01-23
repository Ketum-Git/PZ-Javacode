// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.Arrays;
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
    public final byte[] rainFlags = new byte[64];
    public final float[] rainSplashFrame = new float[64];
    public boolean raining;
    public int rainSplashFrameNum = -1;
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

    public void clear() {
        Arrays.fill(this.squares, null);
        Arrays.fill(this.lightCheck, true);
        this.physicsCheck = false;
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
