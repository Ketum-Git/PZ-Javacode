// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObjOverlay;
import zombie.erosion.obj.ErosionObjOverlaySprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

public final class WallCracks extends ErosionCategory {
    private final ArrayList<ErosionObjOverlay> objs = new ArrayList<>();
    private static final int DIRNW = 0;
    private static final int DIRN = 1;
    private static final int DIRW = 2;
    private final ArrayList<ArrayList<Integer>> objsRef = new ArrayList<>();
    private final ArrayList<ArrayList<Integer>> botRef = new ArrayList<>();
    private final ArrayList<ArrayList<Integer>> topRef = new ArrayList<>();
    private final int[] spawnChance = new int[100];

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall
    ) {
        return false;
    }

    @Override
    public boolean validateSpawn(
        IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall, boolean isRespawn
    ) {
        if (!isExterior) {
            return false;
        } else {
            int eValue = sqErosionData.noiseMainInt;
            int spawnChance = this.spawnChance[eValue];
            if (spawnChance == 0) {
                return false;
            } else if (sqErosionData.rand(square.x, square.y, 101) >= spawnChance) {
                return false;
            } else {
                IsoObject north = this.validWall(square, true, false);
                if (north != null) {
                    String spname = north.getSprite().getName();
                    if (spname != null && spname.startsWith("fencing")) {
                        north = null;
                    }
                }

                IsoObject west = this.validWall(square, false, false);
                if (west != null) {
                    String spname = west.getSprite().getName();
                    if (spname != null && spname.startsWith("fencing")) {
                        west = null;
                    }
                }

                int dir;
                if (north != null && west != null) {
                    dir = 0;
                } else if (north != null) {
                    dir = 1;
                } else {
                    if (west == null) {
                        return false;
                    }

                    dir = 2;
                }

                boolean doTop = eValue < 35 && sqErosionData.magicNum > 0.3F;
                WallCracks.CategoryData sqCategoryData = (WallCracks.CategoryData)this.setCatModData(sqErosionData);
                sqCategoryData.gameObj = this.objsRef.get(dir).get(sqErosionData.rand(square.x, square.y, this.objsRef.get(dir).size()));
                sqCategoryData.alpha = 0.0F;
                sqCategoryData.spawnTime = eValue;
                if (doTop) {
                    IsoGridSquare topsq = IsoWorld.instance.currentCell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1);
                    if (topsq != null) {
                        IsoObject obj = this.validWall(topsq, dir == 1, false);
                        if (obj != null) {
                            int refid = sqErosionData.rand(square.x, square.y, this.botRef.get(dir).size());
                            sqCategoryData.gameObj = this.botRef.get(dir).get(refid);
                            WallCracks.CategoryData topsqCategoryData = new WallCracks.CategoryData();
                            topsqCategoryData.gameObj = this.topRef.get(dir).get(refid);
                            topsqCategoryData.alpha = 0.0F;
                            topsqCategoryData.spawnTime = sqCategoryData.spawnTime;
                            sqCategoryData.hasTop = topsqCategoryData;
                        }
                    }
                }

                return true;
            }
        }
    }

    @Override
    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionCategory.Data data, ErosionData.Chunk chunkData, int eTick) {
        WallCracks.CategoryData sqCategoryData = (WallCracks.CategoryData)data;
        if (eTick >= sqCategoryData.spawnTime && !sqCategoryData.doNothing) {
            if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
                ErosionObjOverlay gameObj = this.objs.get(sqCategoryData.gameObj);
                float oldAlpha = sqCategoryData.alpha;
                float thisAlpha = (eTick - sqCategoryData.spawnTime) / 100.0F;
                if (thisAlpha > 1.0F) {
                    thisAlpha = 1.0F;
                }

                if (thisAlpha < 0.0F) {
                    thisAlpha = 0.0F;
                }

                if (thisAlpha != oldAlpha) {
                    IsoObject object = null;
                    IsoObject north = this.validWall(square, true, false);
                    IsoObject west = this.validWall(square, false, false);
                    if (north != null && west != null) {
                        object = north;
                    } else if (north != null) {
                        object = north;
                    } else if (west != null) {
                        object = west;
                    }

                    if (object != null) {
                        int curId = sqCategoryData.curId;
                        int stage = 0;
                        int id = gameObj.setOverlay(object, curId, 0, 0, thisAlpha);
                        if (id >= 0) {
                            sqCategoryData.alpha = thisAlpha;
                            sqCategoryData.curId = id;
                        }
                    } else {
                        sqCategoryData.doNothing = true;
                    }

                    if (sqCategoryData.hasTop != null) {
                        IsoGridSquare topsq = IsoWorld.instance.currentCell.getGridSquare(square.getX(), square.getY(), square.getZ() + 1);
                        if (topsq != null) {
                            this.update(topsq, sqErosionData, sqCategoryData.hasTop, chunkData, eTick);
                        }
                    }
                }
            } else {
                sqCategoryData.doNothing = true;
            }
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; i++) {
            this.spawnChance[i] = i <= 50 ? 100 : 0;
        }

        String sheet = "d_wallcracks_1_";
        int[] dir = new int[]{2, 2, 2, 1, 1, 1, 0, 0, 0};

        for (int i = 0; i < 3; i++) {
            this.objsRef.add(new ArrayList<>());
            this.topRef.add(new ArrayList<>());
            this.botRef.add(new ArrayList<>());
        }

        for (int i = 0; i < dir.length; i++) {
            for (int row = 0; row <= 7; row++) {
                int id = row * 9 + i;
                ErosionObjOverlaySprites crackspr = new ErosionObjOverlaySprites(1, "WallCracks");
                crackspr.setSprite(0, "d_wallcracks_1_" + id, 0);
                this.objs.add(new ErosionObjOverlay(crackspr, 60, true));
                this.objsRef.get(dir[i]).add(this.objs.size() - 1);
                if (row == 0) {
                    this.botRef.get(dir[i]).add(this.objs.size() - 1);
                } else if (row == 1) {
                    this.topRef.get(dir[i]).add(this.objs.size() - 1);
                }
            }
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new WallCracks.CategoryData();
    }

    @Override
    public void getObjectNames(ArrayList<String> list) {
        for (int i = 0; i < this.objs.size(); i++) {
            if (this.objs.get(i).name != null && !list.contains(this.objs.get(i).name)) {
                list.add(this.objs.get(i).name);
            }
        }
    }

    private static final class CategoryData extends ErosionCategory.Data {
        public int gameObj;
        public int spawnTime;
        public int curId = -999999;
        public float alpha;
        public WallCracks.CategoryData hasTop;

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.putShort((short)this.spawnTime);
            output.putInt(this.curId);
            output.putFloat(this.alpha);
            if (this.hasTop != null) {
                output.put((byte)1);
                output.put((byte)this.hasTop.gameObj);
                output.putShort((short)this.hasTop.spawnTime);
                output.putInt(this.hasTop.curId);
                output.putFloat(this.hasTop.alpha);
            } else {
                output.put((byte)0);
            }
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.spawnTime = input.getShort();
            this.curId = input.getInt();
            this.alpha = input.getFloat();
            boolean hasTop = input.get() != 0;
            if (hasTop) {
                this.hasTop = new WallCracks.CategoryData();
                this.hasTop.gameObj = input.get();
                this.hasTop.spawnTime = input.getShort();
                this.hasTop.curId = input.getInt();
                this.hasTop.alpha = input.getFloat();
            }
        }
    }
}
