// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import org.joml.Vector2f;
import zombie.UsedFromLua;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.popman.ObjectPool;
import zombie.util.list.PZArrayList;

@UsedFromLua
public final class IsoWaterGeometry {
    private static final Vector2f tempVector2f = new Vector2f();
    boolean hasWater;
    boolean shore;
    final float[] x = new float[4];
    final float[] y = new float[4];
    public final float[] depth = new float[4];
    final float[] flow = new float[4];
    final float[] speed = new float[4];
    float isExternal;
    IsoGridSquare square;
    int adjacentChunkLoadedCounter;
    public static final ObjectPool<IsoWaterGeometry> pool = new ObjectPool<>(IsoWaterGeometry::new);

    public IsoWaterGeometry init(IsoGridSquare square) throws Exception {
        this.hasWater = false;
        this.shore = false;
        this.square = square;
        this.isExternal = square.getProperties().has(IsoFlagType.exterior) ? 1.0F : 0.0F;
        int shore = IsoWaterFlow.getShore(square.x, square.y);
        IsoObject floor = square.getFloor();
        String floorSpriteName = floor == null ? null : floor.getSprite().getName();
        if (square.getProperties().has(IsoFlagType.water)) {
            this.hasWater = true;

            for (int i = 0; i < 4; i++) {
                this.depth[i] = 1.0F;
            }
        } else if (shore == 1 && floorSpriteName != null && floorSpriteName.startsWith("blends_natural")) {
            for (int i = 0; i < 4; i++) {
                this.depth[i] = 0.0F;
            }

            IsoGridSquare gs01 = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare gs00 = square.getAdjacentSquare(IsoDirections.NW);
            IsoGridSquare gs10 = square.getAdjacentSquare(IsoDirections.N);
            IsoGridSquare gs02 = square.getAdjacentSquare(IsoDirections.SW);
            IsoGridSquare gs12 = square.getAdjacentSquare(IsoDirections.S);
            IsoGridSquare gs22 = square.getAdjacentSquare(IsoDirections.SE);
            IsoGridSquare gs21 = square.getAdjacentSquare(IsoDirections.E);
            IsoGridSquare gs20 = square.getAdjacentSquare(IsoDirections.NE);
            if (gs10 == null || gs00 == null || gs01 == null || gs02 == null || gs12 == null || gs22 == null || gs21 == null || gs20 == null) {
                return null;
            }

            if (gs01.getProperties().has(IsoFlagType.water) || gs00.getProperties().has(IsoFlagType.water) || gs10.getProperties().has(IsoFlagType.water)) {
                this.shore = true;
                this.depth[0] = 1.0F;
            }

            if (gs01.getProperties().has(IsoFlagType.water) || gs02.getProperties().has(IsoFlagType.water) || gs12.getProperties().has(IsoFlagType.water)) {
                this.shore = true;
                this.depth[1] = 1.0F;
            }

            if (gs12.getProperties().has(IsoFlagType.water) || gs22.getProperties().has(IsoFlagType.water) || gs21.getProperties().has(IsoFlagType.water)) {
                this.shore = true;
                this.depth[2] = 1.0F;
            }

            if (gs21.getProperties().has(IsoFlagType.water) || gs20.getProperties().has(IsoFlagType.water) || gs10.getProperties().has(IsoFlagType.water)) {
                this.shore = true;
                this.depth[3] = 1.0F;
            }
        }

        float f = 0.02F;
        float dx0 = 0.0F;
        float dy1 = 0.0F;
        if (this.shore) {
            IsoGridSquare gs01x = square.getAdjacentSquare(IsoDirections.W);
            IsoGridSquare gs12x = square.getAdjacentSquare(IsoDirections.S);
            if (gs01x != null && gs01x.has(IsoFlagType.water)) {
                dx0 = -0.02F;
            }

            if (gs12x != null && gs12x.has(IsoFlagType.water)) {
                dy1 = 0.02F;
            }
        }

        this.x[0] = IsoUtils.XToScreen(square.x + dx0, square.y, 0.0F, 0);
        this.y[0] = IsoUtils.YToScreen(square.x + dx0, square.y, 0.0F, 0);
        this.x[1] = IsoUtils.XToScreen(square.x + dx0, square.y + 1 + dy1, 0.0F, 0);
        this.y[1] = IsoUtils.YToScreen(square.x + dx0, square.y + 1 + dy1, 0.0F, 0);
        this.x[2] = IsoUtils.XToScreen(square.x + 1, square.y + 1 + dy1, 0.0F, 0);
        this.y[2] = IsoUtils.YToScreen(square.x + 1, square.y + 1 + dy1, 0.0F, 0);
        this.x[3] = IsoUtils.XToScreen(square.x + 1, square.y, 0.0F, 0);
        this.y[3] = IsoUtils.YToScreen(square.x + 1, square.y, 0.0F, 0);
        Vector2f v = IsoWaterFlow.getFlow(square, 0, 0, tempVector2f);
        this.flow[0] = v.x;
        this.speed[0] = v.y;
        v = IsoWaterFlow.getFlow(square, 0, 1, v);
        this.flow[1] = v.x;
        this.speed[1] = v.y;
        v = IsoWaterFlow.getFlow(square, 1, 1, v);
        this.flow[2] = v.x;
        this.speed[2] = v.y;
        v = IsoWaterFlow.getFlow(square, 1, 0, v);
        this.flow[3] = v.x;
        this.speed[3] = v.y;
        this.hideWaterObjects(square);
        return this;
    }

    private void hideWaterObjects(IsoGridSquare square) {
        PZArrayList<IsoObject> objs = square.getObjects();

        for (int j = 0; j < objs.size(); j++) {
            IsoObject obj = objs.get(j);
            if (obj.sprite != null && obj.sprite.name != null) {
                String spriteName = obj.sprite.name;
                if (spriteName.startsWith("blends_natural_02")
                    && (
                        spriteName.endsWith("_0")
                            || spriteName.endsWith("_1")
                            || spriteName.endsWith("_2")
                            || spriteName.endsWith("_3")
                            || spriteName.endsWith("_4")
                            || spriteName.endsWith("_5")
                            || spriteName.endsWith("_6")
                            || spriteName.endsWith("_7")
                            || spriteName.endsWith("_8")
                            || spriteName.endsWith("_9")
                            || spriteName.endsWith("_10")
                            || spriteName.endsWith("_11")
                            || spriteName.endsWith("_12")
                    )) {
                    obj.sprite.setHideForWaterRender();
                }
            }
        }
    }

    public boolean isShore() {
        return IsoWaterFlow.getShore(this.square.x, this.square.y) == 0;
    }

    public boolean isActualShore() {
        return this.shore;
    }

    public float getFlow() {
        IsoWaterFlow.getShore(this.square.x, this.square.y);
        Vector2f v = IsoWaterFlow.getFlow(this.square, 0, 0, tempVector2f);
        return v.x;
    }

    public boolean isValid() {
        return this.hasWater || this.shore;
    }

    public boolean hasWater() {
        return this.hasWater;
    }

    public boolean isbShore() {
        return this.shore;
    }
}
