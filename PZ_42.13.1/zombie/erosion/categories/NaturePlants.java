// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public final class NaturePlants extends ErosionCategory {
    private final int[][] soilRef = new int[][]{
        {17, 17, 17, 17, 17, 17, 17, 17, 17, 1, 2, 8, 8},
        {11, 12, 1, 2, 8, 1, 2, 8, 1, 2, 8, 1, 2, 8, 1, 2, 8},
        {11, 12, 11, 12, 11, 12, 11, 12, 15, 16, 18, 19},
        {22, 22, 22, 22, 22, 22, 22, 22, 22, 3, 4, 14},
        {15, 16, 3, 4, 14, 3, 4, 14, 3, 4, 14, 3, 4, 14},
        {11, 12, 15, 16, 15, 16, 15, 16, 15, 16, 21},
        {13, 13, 13, 13, 13, 13, 13, 13, 13, 5, 6, 24},
        {18, 19, 5, 6, 24, 5, 6, 24, 5, 6, 24, 5, 6, 24},
        {18, 19, 18, 19, 18, 19, 18, 19, 20, 21},
        {7, 7, 7, 7, 7, 7, 7, 7, 7, 9, 10, 23},
        {19, 20, 9, 10, 23, 9, 10, 23, 9, 10, 23, 9, 10, 23},
        {15, 16, 18, 19, 20, 19, 20, 19, 20}
    };
    private final int[] spawnChance = new int[100];
    private final ArrayList<ErosionObj> objs = new ArrayList<>();
    private final NaturePlants.PlantInit[] plants = new NaturePlants.PlantInit[]{
        new NaturePlants.PlantInit("Butterfly Weed", true, 0.05F, 0.25F),
        new NaturePlants.PlantInit("Butterfly Weed", true, 0.05F, 0.25F),
        new NaturePlants.PlantInit("Swamp Sunflower", true, 0.2F, 0.45F),
        new NaturePlants.PlantInit("Swamp Sunflower", true, 0.2F, 0.45F),
        new NaturePlants.PlantInit("Purple Coneflower", true, 0.1F, 0.35F),
        new NaturePlants.PlantInit("Purple Coneflower", true, 0.1F, 0.35F),
        new NaturePlants.PlantInit("Joe-Pye Weed", true, 0.8F, 1.0F),
        new NaturePlants.PlantInit("Blazing Star", true, 0.25F, 0.65F),
        new NaturePlants.PlantInit("Wild Bergamot", true, 0.45F, 0.6F),
        new NaturePlants.PlantInit("Wild Bergamot", true, 0.45F, 0.6F),
        new NaturePlants.PlantInit("White Beard-tongue", true, 0.2F, 0.65F),
        new NaturePlants.PlantInit("White Beard-tongue", true, 0.2F, 0.65F),
        new NaturePlants.PlantInit("Ironweed", true, 0.75F, 0.85F),
        new NaturePlants.PlantInit("White Baneberry", true, 0.4F, 0.8F),
        new NaturePlants.PlantInit("Wild Columbine", true, 0.85F, 1.0F),
        new NaturePlants.PlantInit("Wild Columbine", true, 0.85F, 1.0F),
        new NaturePlants.PlantInit("Jack-in-the-pulpit", false, 0.0F, 0.0F),
        new NaturePlants.PlantInit("Wild Ginger", true, 0.1F, 0.9F),
        new NaturePlants.PlantInit("Wild Ginger", true, 0.1F, 0.9F),
        new NaturePlants.PlantInit("Wild Geranium", true, 0.65F, 0.9F),
        new NaturePlants.PlantInit("Alumroot", true, 0.35F, 0.75F),
        new NaturePlants.PlantInit("Wild Blue Phlox", true, 0.15F, 0.55F),
        new NaturePlants.PlantInit("Polemonium Reptans", true, 0.4F, 0.6F),
        new NaturePlants.PlantInit("Foamflower", true, 0.45F, 1.0F)
    };

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall
    ) {
        int objs_size = _sq.getObjects().size();

        for (int i = objs_size - 1; i >= 1; i--) {
            IsoObject obj = _sq.getObjects().get(i);
            IsoSprite spr = obj.getSprite();
            if (spr != null && spr.getName() != null) {
                if (spr.getName().startsWith("d_plants_1_")) {
                    int id = Integer.parseInt(spr.getName().replace("d_plants_1_", ""));
                    NaturePlants.CategoryData sqCategoryData = (NaturePlants.CategoryData)this.setCatModData(_sqErosionData);
                    sqCategoryData.gameObj = id < 32 ? id % 8 : (id < 48 ? id % 8 + 8 : id % 8 + 16);
                    sqCategoryData.stage = 0;
                    sqCategoryData.spawnTime = 0;
                    _sq.RemoveTileObjectErosionNoRecalc(obj);
                    return true;
                }

                if (!"vegetation_groundcover_01_16".equals(spr.getName()) && !"vegetation_groundcover_01_17".equals(spr.getName())) {
                    if (!"vegetation_groundcover_01_18".equals(spr.getName())
                        && !"vegetation_groundcover_01_19".equals(spr.getName())
                        && !"vegetation_groundcover_01_20".equals(spr.getName())
                        && !"vegetation_groundcover_01_21".equals(spr.getName())
                        && !"vegetation_groundcover_01_22".equals(spr.getName())
                        && !"vegetation_groundcover_01_23".equals(spr.getName())) {
                        continue;
                    }

                    NaturePlants.CategoryData sqCategoryData = (NaturePlants.CategoryData)this.setCatModData(_sqErosionData);
                    sqCategoryData.gameObj = _sqErosionData.rand(_sq.x, _sq.y, this.plants.length);
                    sqCategoryData.stage = 0;
                    sqCategoryData.spawnTime = 0;
                    _sq.RemoveTileObjectErosionNoRecalc(obj);

                    while (--i > 0) {
                        obj = _sq.getObjects().get(i);
                        spr = obj.getSprite();
                        if (spr != null && spr.getName() != null && spr.getName().startsWith("vegetation_groundcover_01_")) {
                            _sq.RemoveTileObjectErosionNoRecalc(obj);
                        }
                    }

                    return true;
                }

                NaturePlants.CategoryData sqCategoryData = (NaturePlants.CategoryData)this.setCatModData(_sqErosionData);
                sqCategoryData.gameObj = 21;
                sqCategoryData.stage = 0;
                sqCategoryData.spawnTime = 0;
                _sq.RemoveTileObjectErosionNoRecalc(obj);

                while (--i > 0) {
                    obj = _sq.getObjects().get(i);
                    spr = obj.getSprite();
                    if (spr != null && spr.getName() != null && spr.getName().startsWith("vegetation_groundcover_01_")) {
                        _sq.RemoveTileObjectErosionNoRecalc(obj);
                    }
                }

                return true;
            }
        }

        return false;
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
            if (_sqErosionData.rand(_sq.x, _sq.y, 101) < this.spawnChance[eValue]) {
                NaturePlants.CategoryData sqCategoryData = (NaturePlants.CategoryData)this.setCatModData(_sqErosionData);
                sqCategoryData.gameObj = soilRef[_sqErosionData.rand(_sq.x, _sq.y, soilRef.length)] - 1;
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
        NaturePlants.CategoryData sqCategoryData = (NaturePlants.CategoryData)_sqCategoryData;
        if (_eTick >= sqCategoryData.spawnTime && !sqCategoryData.doNothing) {
            if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
                ErosionObj gameObj = this.objs.get(sqCategoryData.gameObj);
                boolean bTree = false;
                int stage = 0;
                int dispSeason = this.currentSeason(_sqErosionData.magicNum, gameObj);
                boolean bloom = this.currentBloom(_sqErosionData.magicNum, gameObj);
                this.updateObj(_sqErosionData, _sqCategoryData, _sq, gameObj, false, 0, dispSeason, bloom);
            } else {
                this.clearCatModData(_sqErosionData);
            }
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; i++) {
            if (i >= 20 && i < 50) {
                this.spawnChance[i] = (int)this.clerp((i - 20) / 30.0F, 0.0F, 8.0F);
            } else if (i >= 50 && i < 80) {
                this.spawnChance[i] = (int)this.clerp((i - 50) / 30.0F, 8.0F, 0.0F);
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
        String sheet = "d_plants_1_";
        ArrayList<String> springSpr = new ArrayList<>();

        for (int ix = 0; ix <= 7; ix++) {
            springSpr.add("d_plants_1_" + ix);
        }

        ArrayList<String> autumnSpr = new ArrayList<>();

        for (int ix = 8; ix <= 15; ix++) {
            autumnSpr.add("d_plants_1_" + ix);
        }

        int offset = 16;

        for (int ix = 0; ix < this.plants.length; ix++) {
            if (ix >= 8) {
                offset = 24;
            }

            if (ix >= 16) {
                offset = 32;
            }

            NaturePlants.PlantInit plant = this.plants[ix];
            ErosionObjSprites objSpr = new ErosionObjSprites(1, plant.name, false, plant.hasFlower, false);
            objSpr.setBase(0, springSpr, 1);
            objSpr.setBase(0, autumnSpr, 4);
            objSpr.setBase(0, "d_plants_1_" + (offset + ix), 2);
            objSpr.setFlower(0, "d_plants_1_" + (offset + ix + 8));
            float bloomstart = plant.hasFlower ? plant.bloomstart : 0.0F;
            float bloomend = plant.hasFlower ? plant.bloomend : 0.0F;
            ErosionObj obj = new ErosionObj(objSpr, 30, bloomstart, bloomend, false);
            this.objs.add(obj);
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new NaturePlants.CategoryData();
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

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.putShort((short)this.spawnTime);
        }

        @Override
        public void load(ByteBuffer input, int WorldVersion) {
            super.load(input, WorldVersion);
            this.gameObj = input.get();
            this.spawnTime = input.getShort();
        }
    }

    private static final class PlantInit {
        public String name;
        public boolean hasFlower;
        public float bloomstart;
        public float bloomend;

        public PlantInit(String _name, boolean _hasFlower, float _bloomstart, float _bloomend) {
            this.name = _name;
            this.hasFlower = _hasFlower;
            this.bloomstart = _bloomstart;
            this.bloomend = _bloomend;
        }
    }
}
