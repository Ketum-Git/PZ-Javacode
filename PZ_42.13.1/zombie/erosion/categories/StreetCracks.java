// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjOverlay;
import zombie.erosion.obj.ErosionObjOverlaySprites;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

public final class StreetCracks extends ErosionCategory {
    private final ArrayList<ErosionObj> objs = new ArrayList<>();
    private final ArrayList<ErosionObjOverlay> crackObjs = new ArrayList<>();
    private final int[] spawnChance = new int[100];

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall
    ) {
        return false;
    }

    @Override
    public boolean validateSpawn(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall, boolean _isRespawn
    ) {
        int eValue = _sqErosionData.noiseMainInt;
        int spawnChance = this.spawnChance[eValue];
        if (spawnChance == 0) {
            return false;
        } else if (_sqErosionData.rand(_sq.x, _sq.y, 101) >= spawnChance) {
            return false;
        } else {
            StreetCracks.CategoryData sqCategoryData = (StreetCracks.CategoryData)this.setCatModData(_sqErosionData);
            sqCategoryData.gameObj = _sqErosionData.rand(_sq.x, _sq.y, this.crackObjs.size());
            sqCategoryData.maxStage = eValue > 65 ? 2 : (eValue > 55 ? 1 : 0);
            sqCategoryData.stage = 0;
            sqCategoryData.spawnTime = 150 - eValue;
            if (_sqErosionData.magicNum > 0.5F) {
                sqCategoryData.hasGrass = true;
            }

            return true;
        }
    }

    @Override
    public void update(IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionCategory.Data _sqCategoryData, ErosionData.Chunk _chunkData, int _eTick) {
        StreetCracks.CategoryData sqCategoryData = (StreetCracks.CategoryData)_sqCategoryData;
        if (_eTick >= sqCategoryData.spawnTime && !sqCategoryData.doNothing) {
            IsoObject floor = _sq.getFloor();
            if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.crackObjs.size() && floor != null) {
                ErosionObjOverlay gameObj = this.crackObjs.get(sqCategoryData.gameObj);
                int maxStage = sqCategoryData.maxStage;
                int stage = (int)Math.floor((_eTick - sqCategoryData.spawnTime) / (gameObj.cycleTime / (maxStage + 1.0F)));
                if (stage < sqCategoryData.stage) {
                    stage = sqCategoryData.stage;
                }

                if (stage >= gameObj.stages) {
                    stage = gameObj.stages - 1;
                }

                if (stage != sqCategoryData.stage) {
                    int curId = sqCategoryData.curId;
                    int id = gameObj.setOverlay(floor, curId, stage, 0, 0.0F);
                    if (id >= 0) {
                        sqCategoryData.curId = id;
                    }

                    sqCategoryData.stage = stage;
                } else if (!sqCategoryData.hasGrass && stage == gameObj.stages - 1) {
                    sqCategoryData.doNothing = true;
                }

                if (sqCategoryData.hasGrass) {
                    ErosionObj grassObj = this.objs.get(sqCategoryData.gameObj);
                    if (grassObj != null) {
                        int dispSeason = this.currentSeason(_sqErosionData.magicNum, grassObj);
                        boolean bTree = false;
                        boolean bloom = false;
                        this.updateObj(_sqErosionData, _sqCategoryData, _sq, grassObj, false, stage, dispSeason, false);
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
            this.spawnChance[i] = i >= 40 ? (int)this.clerp((i - 40) / 60.0F, 0.0F, 60.0F) : 0;
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
        String sheet = "d_streetcracks_1_";
        int[] seasons = new int[]{5, 1, 2, 4};

        for (int i = 0; i <= 7; i++) {
            ErosionObjOverlaySprites crackspr = new ErosionObjOverlaySprites(3, "StreeCracks");
            ErosionObjSprites grassspr = new ErosionObjSprites(3, "CrackGrass", false, false, false);

            for (int stage = 0; stage <= 2; stage++) {
                for (int season = 0; season <= seasons.length; season++) {
                    int id = season * 24 + stage * 8 + i;
                    if (season == 0) {
                        crackspr.setSprite(stage, "d_streetcracks_1_" + id, 0);
                    } else {
                        grassspr.setBase(stage, "d_streetcracks_1_" + id, seasons[season - 1]);
                    }
                }
            }

            this.crackObjs.add(new ErosionObjOverlay(crackspr, 60, false));
            this.objs.add(new ErosionObj(grassspr, 60, 0.0F, 0.0F, false));
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new StreetCracks.CategoryData();
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
        public boolean hasGrass;

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
            output.putInt(this.curId);
            output.put((byte)(this.hasGrass ? 1 : 0));
        }

        @Override
        public void load(ByteBuffer input, int WorldVersion) {
            super.load(input, WorldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
            this.curId = input.getInt();
            this.hasGrass = input.get() == 1;
        }
    }
}
