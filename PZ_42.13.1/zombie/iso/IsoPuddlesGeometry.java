// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.popman.ObjectPool;

public final class IsoPuddlesGeometry {
    final float[] x = new float[4];
    final float[] y = new float[4];
    final float[] pdne = new float[4];
    final float[] pdnw = new float[4];
    final float[] pda = new float[4];
    final float[] pnon = new float[4];
    final int[] color = new int[4];
    IsoGridSquare square;
    boolean recalc = true;
    private boolean interiorCalc;
    public static final ObjectPool<IsoPuddlesGeometry> pool = new ObjectPool<>(IsoPuddlesGeometry::new);

    public IsoPuddlesGeometry init(IsoGridSquare square) {
        this.interiorCalc = false;
        IsoObject floor = square.getFloor();
        boolean bShore = IsoWater.getInstance().getShaderEnable() && square.getWater() != null && square.getWater().isbShore();
        if (!PerformanceSettings.fboRenderChunk
            || PerformanceSettings.puddlesQuality != 2
            || bShore
            || floor != null && floor.getProperties() != null && floor.getProperties().has(IsoFlagType.transparentFloor)) {
            this.x[0] = IsoUtils.XToScreen(square.x - square.z * 3, square.y - square.z * 3, square.z, square.z);
            this.y[0] = IsoUtils.YToScreen(square.x - square.z * 3, square.y - square.z * 3, square.z, square.z);
            this.x[1] = IsoUtils.XToScreen(square.x - square.z * 3, square.y + 1 - square.z * 3, 0.0F, 0);
            this.y[1] = IsoUtils.YToScreen(square.x - square.z * 3, square.y + 1 - square.z * 3, 0.0F, 0);
            this.x[2] = IsoUtils.XToScreen(square.x + 1 - square.z * 3, square.y + 1 - square.z * 3, 0.0F, 0);
            this.y[2] = IsoUtils.YToScreen(square.x + 1 - square.z * 3, square.y + 1 - square.z * 3, 0.0F, 0);
            this.x[3] = IsoUtils.XToScreen(square.x + 1 - square.z * 3, square.y - square.z * 3, 0.0F, 0);
            this.y[3] = IsoUtils.YToScreen(square.x + 1 - square.z * 3, square.y - square.z * 3, 0.0F, 0);
            this.x[0]--;
            this.x[1]--;
            this.x[2]--;
            this.x[3]--;
        } else {
            int CPW = 8;
            int mx = PZMath.coordmodulo(square.x, 8);
            int my = PZMath.coordmodulo(square.y, 8);
            this.x[0] = IsoUtils.XToScreen(mx - square.z * 3, my - square.z * 3, square.z, square.z);
            this.y[0] = IsoUtils.YToScreen(mx - square.z * 3, my - square.z * 3, square.z, square.z);
            this.x[1] = IsoUtils.XToScreen(mx - square.z * 3, my + 1 - square.z * 3, 0.0F, 0);
            this.y[1] = IsoUtils.YToScreen(mx - square.z * 3, my + 1 - square.z * 3, 0.0F, 0);
            this.x[2] = IsoUtils.XToScreen(mx + 1 - square.z * 3, my + 1 - square.z * 3, 0.0F, 0);
            this.y[2] = IsoUtils.YToScreen(mx + 1 - square.z * 3, my + 1 - square.z * 3, 0.0F, 0);
            this.x[3] = IsoUtils.XToScreen(mx + 1 - square.z * 3, my - square.z * 3, 0.0F, 0);
            this.y[3] = IsoUtils.YToScreen(mx + 1 - square.z * 3, my - square.z * 3, 0.0F, 0);
        }

        this.square = square;
        if (!square.getProperties().has(IsoFlagType.water) && square.getProperties().has(IsoFlagType.exterior) && !square.hasSlopedSurface()) {
            for (int i = 0; i < 4; i++) {
                this.pdne[i] = 0.0F;
                this.pdnw[i] = 0.0F;
                this.pda[i] = 1.0F;
                this.pnon[i] = 0.0F;
            }

            if (Core.getInstance().getPerfPuddles() > 1) {
                return this;
            } else {
                IsoCell cell = square.getCell();
                IsoGridSquare gs01 = cell.getGridSquare(square.x - 1, square.y, square.z);
                IsoGridSquare gs00 = cell.getGridSquare(square.x - 1, square.y - 1, square.z);
                IsoGridSquare gs10 = cell.getGridSquare(square.x, square.y - 1, square.z);
                IsoGridSquare gs02 = cell.getGridSquare(square.x - 1, square.y + 1, square.z);
                IsoGridSquare gs12 = cell.getGridSquare(square.x, square.y + 1, square.z);
                IsoGridSquare gs22 = cell.getGridSquare(square.x + 1, square.y + 1, square.z);
                IsoGridSquare gs21 = cell.getGridSquare(square.x + 1, square.y, square.z);
                IsoGridSquare gs20 = cell.getGridSquare(square.x + 1, square.y - 1, square.z);
                if (gs10 != null && gs00 != null && gs01 != null && gs02 != null && gs12 != null && gs22 != null && gs21 != null && gs20 != null) {
                    this.setFlags(0, gs01.getPuddlesDir() | gs00.getPuddlesDir() | gs10.getPuddlesDir());
                    this.setFlags(1, gs01.getPuddlesDir() | gs02.getPuddlesDir() | gs12.getPuddlesDir());
                    this.setFlags(2, gs12.getPuddlesDir() | gs22.getPuddlesDir() | gs21.getPuddlesDir());
                    this.setFlags(3, gs21.getPuddlesDir() | gs20.getPuddlesDir() | gs10.getPuddlesDir());
                    return this;
                } else {
                    return this;
                }
            }
        } else {
            for (int i = 0; i < 4; i++) {
                this.pdne[i] = 0.0F;
                this.pdnw[i] = 0.0F;
                this.pda[i] = 0.0F;
                this.pnon[i] = 0.0F;
            }

            return this;
        }
    }

    private void setFlags(int n, int flags) {
        this.pdne[n] = 0.0F;
        this.pdnw[n] = 0.0F;
        this.pda[n] = 0.0F;
        this.pnon[n] = 0.0F;
        if ((flags & 2) != 0) {
            this.pdne[n] = 1.0F;
        }

        if ((flags & 4) != 0) {
            this.pdnw[n] = 1.0F;
        }

        if ((flags & 8) != 0) {
            this.pda[n] = 1.0F;
        }
    }

    public void recalcIfNeeded() {
        if (this.recalc) {
            this.recalc = false;

            try {
                this.init(this.square);
            } catch (Throwable var2) {
                ExceptionLogger.logException(var2);
            }
        }
    }

    public boolean shouldRender() {
        this.recalcIfNeeded();

        for (int i = 0; i < 4; i++) {
            if (this.pdne[i] + this.pdnw[i] + this.pda[i] + this.pnon[i] > 0.0F) {
                return true;
            }
        }

        if (this.square.getProperties().has(IsoFlagType.water)) {
            return false;
        } else {
            if (IsoPuddles.leakingPuddlesInTheRoom && !this.interiorCalc && this.square != null) {
                for (int ix = 0; ix < 4; ix++) {
                    this.pdne[ix] = 0.0F;
                    this.pdnw[ix] = 0.0F;
                    this.pda[ix] = 0.0F;
                    this.pnon[ix] = 1.0F;
                }

                IsoGridSquare gsW = this.square.getAdjacentSquare(IsoDirections.W);
                IsoGridSquare gsNW = this.square.getAdjacentSquare(IsoDirections.NW);
                IsoGridSquare gsN = this.square.getAdjacentSquare(IsoDirections.N);
                IsoGridSquare gsSW = this.square.getAdjacentSquare(IsoDirections.SW);
                IsoGridSquare gsS = this.square.getAdjacentSquare(IsoDirections.S);
                IsoGridSquare gsSE = this.square.getAdjacentSquare(IsoDirections.SE);
                IsoGridSquare gsE = this.square.getAdjacentSquare(IsoDirections.E);
                IsoGridSquare gsNE = this.square.getAdjacentSquare(IsoDirections.NE);
                if (gsW == null
                    || gsN == null
                    || gsS == null
                    || gsE == null
                    || gsNW == null
                    || gsNE == null
                    || gsSW == null
                    || gsSE == null
                    || !gsW.getProperties().has(IsoFlagType.exterior)
                        && !gsN.getProperties().has(IsoFlagType.exterior)
                        && !gsS.getProperties().has(IsoFlagType.exterior)
                        && !gsE.getProperties().has(IsoFlagType.exterior)) {
                    return false;
                }

                if (!this.square.getProperties().has(IsoFlagType.collideW) && gsW.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[0] = 0.0F;
                    this.pnon[1] = 0.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (!gsS.getProperties().has(IsoFlagType.collideN) && gsS.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[1] = 0.0F;
                    this.pnon[2] = 0.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (!gsE.getProperties().has(IsoFlagType.collideW) && gsE.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[2] = 0.0F;
                    this.pnon[3] = 0.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (!this.square.getProperties().has(IsoFlagType.collideN) && gsN.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[3] = 0.0F;
                    this.pnon[0] = 0.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsN.getProperties().has(IsoFlagType.collideW) || !gsNW.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[0] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsS.getProperties().has(IsoFlagType.collideW) || !gsSW.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[1] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsSW.getProperties().has(IsoFlagType.collideN) || !gsSW.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[1] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsSE.getProperties().has(IsoFlagType.collideN) || !gsSE.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[2] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsSE.getProperties().has(IsoFlagType.collideW) || !gsSE.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[2] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsNE.getProperties().has(IsoFlagType.collideW) || !gsNE.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[3] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsE.getProperties().has(IsoFlagType.collideN) || !gsNE.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[3] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                if (gsW.getProperties().has(IsoFlagType.collideN) || !gsNW.getProperties().has(IsoFlagType.exterior)) {
                    this.pnon[0] = 1.0F;

                    for (int ix = 0; ix < 4; ix++) {
                        this.pda[ix] = 1.0F;
                    }
                }

                this.interiorCalc = true;

                for (int ix = 0; ix < 4; ix++) {
                    if (this.pdne[ix] + this.pdnw[ix] + this.pda[ix] + this.pnon[ix] > 0.0F) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public void updateLighting(int playerIndex) {
        this.setLightingAtVert(0, this.square.getVertLight(0, playerIndex));
        this.setLightingAtVert(1, this.square.getVertLight(3, playerIndex));
        this.setLightingAtVert(2, this.square.getVertLight(2, playerIndex));
        this.setLightingAtVert(3, this.square.getVertLight(1, playerIndex));
    }

    private void setLightingAtVert(int i, int vertLight) {
        this.color[i] = vertLight;
    }
}
