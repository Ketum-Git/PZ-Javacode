// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public final class NatureBush extends ErosionCategory {
    private final int[][] soilRef = new int[][]{
        {11, 11, 12, 13},
        {5, 5, 7, 8, 11, 11, 12, 13, 11, 11, 12, 13},
        {5, 5, 7, 8, 5, 5, 7, 8, 11, 11, 12, 13},
        {1, 1, 4, 5},
        {5, 5, 7, 8, 1, 1, 4, 5, 1, 1, 4, 5},
        {5, 5, 7, 8, 5, 5, 7, 8, 1, 1, 4, 5},
        {9, 10, 14, 15},
        {5, 5, 7, 8, 9, 10, 14, 15, 9, 10, 14, 15},
        {5, 5, 7, 8, 5, 5, 7, 8, 9, 10, 14, 15},
        {2, 3, 16, 16},
        {5, 5, 7, 8, 2, 3, 16, 16, 2, 3, 16, 16},
        {5, 5, 7, 8, 5, 5, 7, 8, 2, 3, 16, 16}
    };
    private final ArrayList<ErosionObj> objs = new ArrayList<>();
    private final int[] spawnChance = new int[100];
    private final NatureBush.BushInit[] bush = new NatureBush.BushInit[]{
        new NatureBush.BushInit("Spicebush", 0.05F, 0.35F, false),
        new NatureBush.BushInit("Ninebark", 0.65F, 0.75F, true),
        new NatureBush.BushInit("Ninebark", 0.65F, 0.75F, true),
        new NatureBush.BushInit("Blueberry", 0.4F, 0.5F, true),
        new NatureBush.BushInit("Blackberry", 0.4F, 0.5F, true),
        new NatureBush.BushInit("Piedmont azalea", 0.0F, 0.15F, true),
        new NatureBush.BushInit("Piedmont azalea", 0.0F, 0.15F, true),
        new NatureBush.BushInit("Arrowwood viburnum", 0.3F, 0.8F, true),
        new NatureBush.BushInit("Red chokeberry", 0.9F, 1.0F, true),
        new NatureBush.BushInit("Red chokeberry", 0.9F, 1.0F, true),
        new NatureBush.BushInit("Beautyberry", 0.7F, 0.85F, true),
        new NatureBush.BushInit("New jersey tea", 0.4F, 0.8F, true),
        new NatureBush.BushInit("New jersey tea", 0.4F, 0.8F, true),
        new NatureBush.BushInit("Wild hydrangea", 0.2F, 0.35F, true),
        new NatureBush.BushInit("Wild hydrangea", 0.2F, 0.35F, true),
        new NatureBush.BushInit("Shrubby St. John's wort", 0.35F, 0.75F, true)
    };

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall
    ) {
        int objs_size = _sq.getObjects().size();
        boolean replaced = false;

        for (int i = objs_size - 1; i >= 1; i--) {
            IsoObject obj = _sq.getObjects().get(i);
            IsoSprite spr = obj.getSprite();
            if (spr != null && spr.getName() != null) {
                if (spr.getName().startsWith("vegetation_foliage")) {
                    int soil = _sqErosionData.soil;
                    if (soil < 0 || soil >= this.soilRef.length) {
                        soil = _sqErosionData.rand(_sq.x, _sq.y, this.soilRef.length);
                    }

                    int[] soilRef = this.soilRef[soil];
                    int eValue = _sqErosionData.noiseMainInt;
                    NatureBush.CategoryData sqCategoryData = (NatureBush.CategoryData)this.setCatModData(_sqErosionData);
                    sqCategoryData.gameObj = soilRef[_sqErosionData.rand(_sq.x, _sq.y, soilRef.length)] - 1;
                    sqCategoryData.maxStage = (int)Math.floor(eValue / 60.0F);
                    sqCategoryData.stage = sqCategoryData.maxStage;
                    sqCategoryData.spawnTime = 0;
                    _sq.RemoveTileObject(obj, false);
                    replaced = true;
                }

                if (spr.getName().startsWith("f_bushes_1_")) {
                    int id = Integer.parseInt(spr.getName().replace("f_bushes_1_", ""));
                    NatureBush.CategoryData sqCategoryData = (NatureBush.CategoryData)this.setCatModData(_sqErosionData);
                    sqCategoryData.gameObj = id % 16;
                    sqCategoryData.maxStage = 1;
                    sqCategoryData.stage = sqCategoryData.maxStage;
                    sqCategoryData.spawnTime = 0;
                    _sq.RemoveTileObject(obj, false);
                    replaced = true;
                }
            }
        }

        return replaced;
    }

    @Override
    public boolean validateSpawn(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall, boolean _isRespawn
    ) {
        if (_sq.getObjects().size() > (_hasWall ? 2 : 1)) {
            return false;
        } else if (_sqErosionData.soil >= 0 && _sqErosionData.soil < this.soilRef.length) {
            int[] soilRef = this.soilRef[_sqErosionData.soil];
            int eValue = _sqErosionData.noiseMainInt;
            int randValue = _sqErosionData.rand(_sq.x, _sq.y, 101);
            if (randValue < this.spawnChance[eValue]) {
                NatureBush.CategoryData sqCategoryData = (NatureBush.CategoryData)this.setCatModData(_sqErosionData);
                sqCategoryData.gameObj = soilRef[_sqErosionData.rand(_sq.x, _sq.y, soilRef.length)] - 1;
                sqCategoryData.maxStage = (int)Math.floor(eValue / 60.0F);
                sqCategoryData.stage = 0;
                sqCategoryData.spawnTime = 100 - eValue;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void update(IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionCategory.Data _sqCategoryData, ErosionData.Chunk _chunkData, int _eTick) {
        NatureBush.CategoryData sqCategoryData = (NatureBush.CategoryData)_sqCategoryData;
        if (_eTick >= sqCategoryData.spawnTime && !sqCategoryData.doNothing) {
            if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
                ErosionObj gameObj = this.objs.get(sqCategoryData.gameObj);
                int maxStage = sqCategoryData.maxStage;
                int stage = (int)Math.floor((_eTick - sqCategoryData.spawnTime) / (gameObj.cycleTime / (maxStage + 1.0F)));
                if (stage < sqCategoryData.stage) {
                    stage = sqCategoryData.stage;
                }

                if (stage > maxStage) {
                    stage = maxStage;
                }

                int dispSeason = this.currentSeason(_sqErosionData.magicNum, gameObj);
                boolean bloom = this.currentBloom(_sqErosionData.magicNum, gameObj);
                boolean bTree = false;
                this.updateObj(_sqErosionData, _sqCategoryData, _sq, gameObj, false, stage, dispSeason, bloom);
            } else {
                sqCategoryData.doNothing = true;
            }
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; i++) {
            if (i >= 45 && i < 60) {
                this.spawnChance[i] = (int)this.clerp((i - 45) / 15.0F, 0.0F, 20.0F);
            }

            if (i >= 60 && i < 90) {
                this.spawnChance[i] = (int)this.clerp((i - 60) / 30.0F, 20.0F, 0.0F);
            }
        }

        this.seasonDisp[5].season1 = 0;
        this.seasonDisp[5].season2 = 0;
        this.seasonDisp[5].split = false;
        this.seasonDisp[1].season1 = 1;
        this.seasonDisp[1].season2 = 0;
        this.seasonDisp[1].split = false;
        this.seasonDisp[2].season1 = 2;
        this.seasonDisp[2].season2 = 2;
        this.seasonDisp[2].split = true;
        this.seasonDisp[4].season1 = 4;
        this.seasonDisp[4].season2 = 0;
        this.seasonDisp[4].split = true;
        ErosionIceQueen iceQueen = ErosionIceQueen.instance;
        String sheet = "f_bushes_1_";

        for (int id = 1; id <= this.bush.length; id++) {
            int i = id - 1;
            int trunk = i - (int)Math.floor(i / 8.0F) * 8;
            NatureBush.BushInit b = this.bush[i];
            ErosionObjSprites objSpr = new ErosionObjSprites(2, b.name, true, b.hasFlower, true);
            int baseId = 0 + trunk;
            int snowId = baseId + 16;
            int springId = snowId + 16;
            int autumnId = springId + 16;
            int summerId = 64 + i;
            int bloomId = summerId + 16;
            objSpr.setBase(0, "f_bushes_1_" + baseId, 0);
            objSpr.setBase(1, "f_bushes_1_" + (baseId + 8), 0);
            iceQueen.addSprite("f_bushes_1_" + baseId, "f_bushes_1_" + snowId);
            iceQueen.addSprite("f_bushes_1_" + (baseId + 8), "f_bushes_1_" + (snowId + 8));
            objSpr.setChildSprite(0, "f_bushes_1_" + springId, 1);
            objSpr.setChildSprite(1, "f_bushes_1_" + (springId + 8), 1);
            objSpr.setChildSprite(0, "f_bushes_1_" + autumnId, 4);
            objSpr.setChildSprite(1, "f_bushes_1_" + (autumnId + 8), 4);
            objSpr.setChildSprite(0, "f_bushes_1_" + summerId, 2);
            objSpr.setChildSprite(1, "f_bushes_1_" + (summerId + 32), 2);
            if (b.hasFlower) {
                objSpr.setFlower(0, "f_bushes_1_" + bloomId);
                objSpr.setFlower(1, "f_bushes_1_" + (bloomId + 32));
            }

            float bloomstart = b.hasFlower ? b.bloomstart : 0.0F;
            float bloomend = b.hasFlower ? b.bloomend : 0.0F;
            ErosionObj obj = new ErosionObj(objSpr, 60, bloomstart, bloomend, true);
            this.objs.add(obj);
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new NatureBush.CategoryData();
    }

    @Override
    public void getObjectNames(ArrayList<String> list) {
        for (int i = 0; i < this.objs.size(); i++) {
            if (this.objs.get(i).name != null && !list.contains(this.objs.get(i).name)) {
                list.add(this.objs.get(i).name);
            }
        }
    }

    private class BushInit {
        public String name;
        public float bloomstart;
        public float bloomend;
        public boolean hasFlower;

        public BushInit(final String _name, final float _bloomstart, final float _bloomend, final boolean _hasFlower) {
            Objects.requireNonNull(NatureBush.this);
            super();
            this.name = _name;
            this.bloomstart = _bloomstart;
            this.bloomend = _bloomend;
            this.hasFlower = _hasFlower;
        }
    }

    private static final class CategoryData extends ErosionCategory.Data {
        public int gameObj;
        public int maxStage;
        public int spawnTime;

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
        }

        @Override
        public void load(ByteBuffer input, int WorldVersion) {
            super.load(input, WorldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
        }
    }
}
