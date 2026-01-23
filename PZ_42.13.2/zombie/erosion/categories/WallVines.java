// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.obj.ErosionObjOverlay;
import zombie.erosion.obj.ErosionObjOverlaySprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;

public final class WallVines extends ErosionCategory {
    private final ArrayList<ErosionObjOverlay> objs = new ArrayList<>();
    private static final int DIRNW = 0;
    private static final int DIRN = 1;
    private static final int DIRW = 2;
    private final int[][] objsRef = new int[3][2];
    private final HashMap<String, Integer> spriteToObj = new HashMap<>();
    private final HashMap<String, Integer> spriteToStage = new HashMap<>();
    private final int[] spawnChance = new int[100];

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall
    ) {
        int objs_size = _sq.getObjects().size();

        for (int i = objs_size - 1; i >= 1; i--) {
            IsoObject obj = _sq.getObjects().get(i);
            if (obj.attachedAnimSprite != null) {
                for (int j = 0; j < obj.attachedAnimSprite.size(); j++) {
                    IsoSprite spr = obj.attachedAnimSprite.get(j).parentSprite;
                    if (spr != null && spr.getName() != null && spr.getName().startsWith("f_wallvines_1_") && this.spriteToObj.containsKey(spr.getName())) {
                        WallVines.CategoryData sqCategoryData = (WallVines.CategoryData)this.setCatModData(_sqErosionData);
                        sqCategoryData.gameObj = this.spriteToObj.get(spr.getName());
                        int stage = this.spriteToStage.get(spr.getName());
                        sqCategoryData.stage = stage;
                        sqCategoryData.maxStage = 2;
                        sqCategoryData.spawnTime = 0;
                        obj.attachedAnimSprite.remove(j);
                        if (obj.attachedAnimSprite != null && j < obj.attachedAnimSprite.size()) {
                            obj.attachedAnimSprite.remove(j);
                        }

                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean validateSpawn(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall, boolean _isRespawn
    ) {
        if (!_isExterior) {
            return false;
        } else {
            int eValue = _sqErosionData.noiseMainInt;
            int spawnChance = this.spawnChance[eValue];
            if (spawnChance == 0) {
                return false;
            } else if (_sqErosionData.rand(_sq.x, _sq.y, 101) >= spawnChance) {
                return false;
            } else {
                int dir = -1;
                IsoObject north = this.validWall(_sq, true, true);
                IsoObject west = this.validWall(_sq, false, true);
                byte var17;
                if (north != null && west != null) {
                    var17 = 0;
                } else if (north != null) {
                    var17 = 1;
                } else {
                    if (west == null) {
                        return false;
                    }

                    var17 = 2;
                }

                WallVines.CategoryData sqCategoryData = (WallVines.CategoryData)this.setCatModData(_sqErosionData);
                sqCategoryData.gameObj = this.objsRef[var17][_sqErosionData.rand(_sq.x, _sq.y, this.objsRef[var17].length)];
                sqCategoryData.maxStage = eValue > 65 ? 3 : (eValue > 60 ? 2 : (eValue > 55 ? 1 : 0));
                sqCategoryData.stage = 0;
                sqCategoryData.spawnTime = 100 - eValue;
                if (sqCategoryData.maxStage == 3) {
                    IsoGridSquare topsq = IsoWorld.instance.currentCell.getGridSquare(_sq.getX(), _sq.getY(), _sq.getZ() + 1);
                    if (topsq != null) {
                        IsoObject obj = this.validWall(topsq, var17 == 1, true);
                        ErosionObjOverlay gameObj = this.objs.get(sqCategoryData.gameObj);
                        if (obj != null && gameObj != null) {
                            WallVines.CategoryData topsqCategoryData = new WallVines.CategoryData();
                            topsqCategoryData.gameObj = this.objsRef[var17][_sqErosionData.rand(_sq.x, _sq.y, this.objsRef[var17].length)];
                            topsqCategoryData.maxStage = eValue > 75 ? 2 : (eValue > 70 ? 1 : 0);
                            topsqCategoryData.stage = 0;
                            topsqCategoryData.spawnTime = sqCategoryData.spawnTime + (int)(gameObj.cycleTime / (sqCategoryData.maxStage + 1.0F) * 4.0F);
                            sqCategoryData.hasTop = topsqCategoryData;
                        } else {
                            sqCategoryData.maxStage = 2;
                        }
                    } else {
                        sqCategoryData.maxStage = 2;
                    }
                }

                return true;
            }
        }
    }

    @Override
    public void update(IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionCategory.Data _sqCategoryData, ErosionData.Chunk _chunkData, int _eTick) {
        WallVines.CategoryData sqCategoryData = (WallVines.CategoryData)_sqCategoryData;
        if (_eTick >= sqCategoryData.spawnTime && !sqCategoryData.doNothing) {
            if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
                ErosionObjOverlay gameObj = this.objs.get(sqCategoryData.gameObj);
                int maxStage = sqCategoryData.maxStage;
                int stage = (int)Math.floor((_eTick - sqCategoryData.spawnTime) / (gameObj.cycleTime / (maxStage + 1.0F)));
                if (stage < sqCategoryData.stage) {
                    stage = sqCategoryData.stage;
                }

                if (stage > maxStage) {
                    stage = maxStage;
                }

                if (stage > gameObj.stages) {
                    stage = gameObj.stages;
                }

                if (stage == 3 && sqCategoryData.hasTop != null && sqCategoryData.hasTop.spawnTime > _eTick) {
                    stage = 2;
                }

                int dispSeason = ErosionMain.getInstance().getSeasons().getSeason();
                if (stage != sqCategoryData.stage || sqCategoryData.dispSeason != dispSeason) {
                    IsoObject object = null;
                    IsoObject north = this.validWall(_sq, true, true);
                    IsoObject west = this.validWall(_sq, false, true);
                    if (north != null && west != null) {
                        object = north;
                    } else if (north != null) {
                        object = north;
                    } else if (west != null) {
                        object = west;
                    }

                    sqCategoryData.dispSeason = dispSeason;
                    if (object != null) {
                        int curId = sqCategoryData.curId;
                        int id = gameObj.setOverlay(object, curId, stage, dispSeason, 0.0F);
                        if (id >= 0) {
                            sqCategoryData.curId = id;
                        }
                    } else {
                        sqCategoryData.doNothing = true;
                    }

                    if (stage == 3 && sqCategoryData.hasTop != null) {
                        IsoGridSquare topsq = IsoWorld.instance.currentCell.getGridSquare(_sq.getX(), _sq.getY(), _sq.getZ() + 1);
                        if (topsq != null) {
                            this.update(topsq, _sqErosionData, sqCategoryData.hasTop, _chunkData, _eTick);
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
            this.spawnChance[i] = i >= 50 ? 100 : 0;
        }

        this.seasonDisp[5].season1 = 5;
        this.seasonDisp[5].season2 = 0;
        this.seasonDisp[5].split = false;
        this.seasonDisp[1].season1 = 1;
        this.seasonDisp[1].season2 = 0;
        this.seasonDisp[1].split = false;
        this.seasonDisp[2].season1 = 2;
        this.seasonDisp[2].season2 = 4;
        this.seasonDisp[2].split = true;
        this.seasonDisp[4].season1 = 4;
        this.seasonDisp[4].season2 = 5;
        this.seasonDisp[4].split = true;
        String sheet = "f_wallvines_1_";
        int[] seasons = new int[]{5, 2, 4, 1};
        int[] dir = new int[]{2, 2, 1, 1, 0, 0};
        int[] count = new int[3];

        for (int i = 0; i < dir.length; i++) {
            ErosionObjOverlaySprites vinespr = new ErosionObjOverlaySprites(4, "WallVines");

            for (int stage = 0; stage <= 3; stage++) {
                for (int season = 0; season <= 2; season++) {
                    int id = season * 24 + stage * 6 + i;
                    vinespr.setSprite(stage, "f_wallvines_1_" + id, seasons[season]);
                    if (season == 2) {
                        vinespr.setSprite(stage, "f_wallvines_1_" + id, seasons[season + 1]);
                    }

                    this.spriteToObj.put("f_wallvines_1_" + id, this.objs.size());
                    this.spriteToStage.put("f_wallvines_1_" + id, stage);
                }
            }

            this.objs.add(new ErosionObjOverlay(vinespr, 60, false));
            this.objsRef[dir[i]][count[dir[i]]++] = this.objs.size() - 1;
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new WallVines.CategoryData();
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
        public int maxStage;
        public int spawnTime;
        public int curId = -999999;
        public WallVines.CategoryData hasTop;

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
            output.putInt(this.curId);
            if (this.hasTop != null) {
                output.put((byte)1);
                output.put((byte)this.hasTop.gameObj);
                output.putShort((short)this.hasTop.spawnTime);
                output.putInt(this.hasTop.curId);
            } else {
                output.put((byte)0);
            }
        }

        @Override
        public void load(ByteBuffer input, int WorldVersion) {
            super.load(input, WorldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
            this.curId = input.getInt();
            boolean _hasTop = input.get() == 1;
            if (_hasTop) {
                this.hasTop = new WallVines.CategoryData();
                this.hasTop.gameObj = input.get();
                this.hasTop.spawnTime = input.getShort();
                this.hasTop.curId = input.getInt();
            }
        }
    }
}
