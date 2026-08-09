// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.core.math.PZMath;
import zombie.erosion.ErosionData;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.erosion.season.ErosionIceQueen;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public final class NatureTrees extends ErosionCategory {
    private final int[][] soilRef = new int[][]{
        {2, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5},
        {1, 1, 2, 2, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5},
        {2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1, 1, 3, 3, 4, 4, 4, 5},
        {1, 7, 7, 7, 9, 9, 9, 9, 9, 9, 9},
        {2, 2, 1, 1, 1, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 9, 9, 9, 9},
        {1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 7, 7, 7, 9},
        {1, 2, 8, 8, 8, 6, 6, 6, 6, 6, 6, 6, 6},
        {1, 1, 2, 2, 3, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 6, 6, 6, 6, 6},
        {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 3, 3, 8, 8, 8, 6},
        {3, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11},
        {1, 1, 3, 3, 3, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11},
        {1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 10, 10, 10, 11}
    };
    private final NatureTrees.TreeInit[] trees = new NatureTrees.TreeInit[]{
        new NatureTrees.TreeInit("American Holly", "e_americanholly_1", true),
        new NatureTrees.TreeInit("Canadian Hemlock", "e_canadianhemlock_1", true),
        new NatureTrees.TreeInit("Virginia Pine", "e_virginiapine_1", true),
        new NatureTrees.TreeInit("Riverbirch", "e_riverbirch_1", false),
        new NatureTrees.TreeInit("Cockspur Hawthorn", "e_cockspurhawthorn_1", false),
        new NatureTrees.TreeInit("Dogwood", "e_dogwood_1", false),
        new NatureTrees.TreeInit("Carolina Silverbell", "e_carolinasilverbell_1", false),
        new NatureTrees.TreeInit("Yellowwood", "e_yellowwood_1", false),
        new NatureTrees.TreeInit("Eastern Redbud", "e_easternredbud_1", false),
        new NatureTrees.TreeInit("Redmaple", "e_redmaple_1", false),
        new NatureTrees.TreeInit("American Linden", "e_americanlinden_1", false)
    };
    private final int[] spawnChance = new int[100];
    private final ArrayList<ErosionObj> objs = new ArrayList<>();

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall
    ) {
        int objsSize = square.getObjects().size();

        for (int i = objsSize - 1; i >= 1; i--) {
            IsoObject obj = square.getObjects().get(i);
            IsoSprite spr = obj.getSprite();
            if (spr != null && spr.getName() != null) {
                if (spr.getName().startsWith("jumbo_tree_01")) {
                    int soil = sqErosionData.soil;
                    if (soil < 0 || soil >= this.soilRef.length) {
                        soil = sqErosionData.rand(square.x, square.y, this.soilRef.length);
                    }

                    int[] soilRef = this.soilRef[soil];
                    int eValue = sqErosionData.noiseMainInt;
                    NatureTrees.CategoryData sqCategoryData = (NatureTrees.CategoryData)this.setCatModData(sqErosionData);
                    sqCategoryData.gameObj = soilRef[sqErosionData.rand(square.x, square.y, soilRef.length)] - 1;
                    sqCategoryData.maxStage = 5 + (int)Math.floor(eValue / 51.0F) - 1;
                    sqCategoryData.stage = sqCategoryData.maxStage;
                    sqCategoryData.spawnTime = 0;
                    sqCategoryData.dispSeason = -1;
                    ErosionObj erosionObj = this.objs.get(sqCategoryData.gameObj);
                    obj.setName(erosionObj.name);
                    sqCategoryData.hasSpawned = true;
                    return true;
                }

                if (spr.getName().startsWith("vegetation_trees")) {
                    int soil = sqErosionData.soil;
                    if (soil < 0 || soil >= this.soilRef.length) {
                        soil = sqErosionData.rand(square.x, square.y, this.soilRef.length);
                    }

                    int[] soilRef = this.soilRef[soil];
                    int eValue = sqErosionData.noiseMainInt;
                    NatureTrees.CategoryData sqCategoryData = (NatureTrees.CategoryData)this.setCatModData(sqErosionData);
                    sqCategoryData.gameObj = soilRef[sqErosionData.rand(square.x, square.y, soilRef.length)] - 1;
                    sqCategoryData.maxStage = 3 + (int)Math.floor(eValue / 51.0F) - 1;
                    sqCategoryData.stage = sqCategoryData.maxStage;
                    sqCategoryData.spawnTime = 0;
                    sqCategoryData.dispSeason = -1;
                    ErosionObj erosionObj = this.objs.get(sqCategoryData.gameObj);
                    obj.setName(erosionObj.name);
                    sqCategoryData.hasSpawned = true;
                    return true;
                }

                for (int t = 0; t < this.trees.length; t++) {
                    if (spr.getName().startsWith(this.trees[t].tilesetName)) {
                        NatureTrees.CategoryData sqCategoryData = (NatureTrees.CategoryData)this.setCatModData(sqErosionData);
                        sqCategoryData.gameObj = t;
                        sqCategoryData.maxStage = 3;
                        sqCategoryData.stage = sqCategoryData.maxStage;
                        sqCategoryData.spawnTime = 0;
                        square.RemoveTileObject(obj, false);
                        return true;
                    }

                    if (spr.getName().startsWith(this.trees[t].jumboTilesetName)) {
                        NatureTrees.CategoryData sqCategoryData = (NatureTrees.CategoryData)this.setCatModData(sqErosionData);
                        sqCategoryData.gameObj = t;
                        sqCategoryData.maxStage = 5;
                        int p = spr.getName().lastIndexOf(95);
                        int index = PZMath.tryParseInt(spr.getName().substring(p + 1), 0);
                        int columns = 2;
                        sqCategoryData.stage = index % 2 == 0 ? 4 : 5;
                        sqCategoryData.spawnTime = 0;
                        square.RemoveTileObject(obj, false);
                        return true;
                    }

                    if (spr.getName().startsWith(this.trees[t].jumboXLTilesetName) || spr.getName().startsWith(this.trees[t].jumboXXLTilesetName)) {
                        NatureTrees.CategoryData sqCategoryData = (NatureTrees.CategoryData)this.setCatModData(sqErosionData);
                        sqCategoryData.gameObj = t;
                        sqCategoryData.maxStage = spr.getName().startsWith(this.trees[t].jumboXLTilesetName) ? 6 : 7;
                        sqCategoryData.stage = spr.getName().startsWith(this.trees[t].jumboXLTilesetName) ? 6 : 7;
                        sqCategoryData.spawnTime = 0;
                        square.RemoveTileObject(obj, false);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public boolean validateSpawn(
        IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, boolean isExterior, boolean hasWall, boolean isRespawn
    ) {
        if (square.getObjects().size() > (hasWall ? 2 : 1)) {
            return false;
        } else if (sqErosionData.soil >= 0 && sqErosionData.soil < this.soilRef.length) {
            int[] soilRef = this.soilRef[sqErosionData.soil];
            int eValue = sqErosionData.noiseMainInt;
            int chance = this.spawnChance[eValue];
            if (chance > 0 && sqErosionData.rand(square.x, square.y, 101) < chance) {
                NatureTrees.CategoryData sqCategoryData = (NatureTrees.CategoryData)this.setCatModData(sqErosionData);
                sqCategoryData.gameObj = soilRef[sqErosionData.rand(square.x, square.y, soilRef.length)] - 1;
                sqCategoryData.maxStage = 2 + (int)Math.floor((eValue - 50) / 17) - 1;
                sqCategoryData.stage = 0;
                sqCategoryData.spawnTime = 130 - eValue;
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionCategory.Data sqCategoryData, ErosionData.Chunk chunkData, int eTick) {
        NatureTrees.CategoryData data = (NatureTrees.CategoryData)sqCategoryData;
        if (eTick >= data.spawnTime && !data.doNothing) {
            if (data.gameObj >= 0 && data.gameObj < this.objs.size()) {
                ErosionObj gameObj = this.objs.get(data.gameObj);
                int maxStage = data.maxStage;
                int stage = (int)Math.floor((eTick - data.spawnTime) / (gameObj.cycleTime / (maxStage + 1.0F)));
                if (stage < sqCategoryData.stage) {
                    stage = sqCategoryData.stage;
                }

                if (stage > maxStage) {
                    stage = maxStage;
                }

                boolean bTree = true;
                int dispSeason = this.currentSeason(sqErosionData.magicNum, gameObj);
                boolean bloom = false;
                this.updateObj(sqErosionData, sqCategoryData, square, gameObj, true, stage, dispSeason, false);
            } else {
                this.clearCatModData(sqErosionData);
            }
        }
    }

    @Override
    public void init() {
        for (int i = 0; i < 100; i++) {
            this.spawnChance[i] = i >= 50 ? (int)this.clerp((i - 50) / 50.0F, 0.0F, 90.0F) : 0;
        }

        int[] snames = new int[]{0, 5, 1, 2, 3, 4};
        this.seasonDisp[5].season1 = 0;
        this.seasonDisp[5].season2 = 0;
        this.seasonDisp[5].split = false;
        this.seasonDisp[1].season1 = 1;
        this.seasonDisp[1].season2 = 0;
        this.seasonDisp[1].split = false;
        this.seasonDisp[2].season1 = 2;
        this.seasonDisp[2].season2 = 3;
        this.seasonDisp[2].split = true;
        this.seasonDisp[4].season1 = 4;
        this.seasonDisp[4].season2 = 0;
        this.seasonDisp[4].split = true;
        String baseSprite = null;
        ErosionIceQueen iceQueen = ErosionIceQueen.instance;

        for (int id = 0; id < this.trees.length; id++) {
            String name = this.trees[id].name;
            String sheet = this.trees[id].tilesetName;
            boolean seasonal = !this.trees[id].evergreen;
            ErosionObjSprites objSpr = new ErosionObjSprites(8, name, true, false, seasonal);
            int TREES_NORMAL = 4;
            int TREES_JUMBO = 2;
            int TREES_JUMBOXL = 1;
            int TREES_JUMBOXXL = 1;

            for (int stage = 0; stage < 8; stage++) {
                for (int season = 0; season < snames.length; season++) {
                    int sheetid = switch (stage) {
                        case 4 -> season * 2;
                        case 5 -> season * 2 + 1;
                        case 6 -> season * 1;
                        case 7 -> season * 1;
                        default -> season * 4 + stage;
                    };

                    String repl = switch (stage) {
                        case 4, 5 -> sheet.replace("_1", "JUMBO_1");
                        case 6 -> sheet.replace("_1", "JUMBOXL_1");
                        case 7 -> sheet.replace("_1", "JUMBOXXL_1");
                        default -> sheet;
                    };
                    if (season == 0) {
                        baseSprite = repl + "_" + sheetid;
                        objSpr.setBase(stage, baseSprite, 0);
                    } else if (season == 1) {
                        iceQueen.addSprite(baseSprite, repl + "_" + sheetid);
                    } else if (seasonal) {
                        objSpr.setChildSprite(stage, repl + "_" + sheetid, snames[season]);
                    }
                }
            }

            ErosionObj obj = new ErosionObj(objSpr, 60, 0.0F, 0.0F, true);
            this.objs.add(obj);
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new NatureTrees.CategoryData();
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

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
            output.put((byte)this.maxStage);
            output.putShort((short)this.spawnTime);
        }

        @Override
        public void load(ByteBuffer input, int worldVersion) {
            super.load(input, worldVersion);
            this.gameObj = input.get();
            this.maxStage = input.get();
            this.spawnTime = input.getShort();
        }
    }

    private static final class TreeInit {
        public String name;
        public String tilesetName;
        public String jumboTilesetName;
        public String jumboXLTilesetName;
        public String jumboXXLTilesetName;
        public boolean evergreen;

        public TreeInit(String name, String tilesetName, boolean seasonal) {
            this.name = name;
            this.tilesetName = tilesetName;
            this.jumboTilesetName = tilesetName.replace("_1", "JUMBO_1");
            this.jumboXLTilesetName = tilesetName.replace("_1", "JUMBOXL_1");
            this.jumboXXLTilesetName = tilesetName.replace("_1", "JUMBOXXL_1");
            this.evergreen = seasonal;
        }
    }
}
