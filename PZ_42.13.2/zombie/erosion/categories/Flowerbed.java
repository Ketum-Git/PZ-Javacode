// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.obj.ErosionObjSprites;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

public final class Flowerbed extends ErosionCategory {
    private final int[] tileId = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 28, 29, 30, 31};
    private final ArrayList<ErosionObj> objs = new ArrayList<>();

    @Override
    public boolean replaceExistingObject(
        IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, boolean _isExterior, boolean _hasWall
    ) {
        int objs_size = _sq.getObjects().size();

        for (int i = objs_size - 1; i >= 0; i--) {
            IsoSprite spr = _sq.getObjects().get(i).getSprite();
            if (spr != null && spr.getName() != null) {
                if (spr.getName().startsWith("f_flowerbed_1")) {
                    int id = Integer.parseInt(spr.getName().replace("f_flowerbed_1_", ""));
                    if (id <= 23) {
                        if (id >= 12) {
                            id -= 12;
                        }

                        Flowerbed.CategoryData sqCategoryData = (Flowerbed.CategoryData)this.setCatModData(_sqErosionData);
                        sqCategoryData.hasSpawned = true;
                        sqCategoryData.gameObj = id;
                        sqCategoryData.dispSeason = -1;
                        ErosionObj obj = this.objs.get(sqCategoryData.gameObj);
                        _sq.getObjects().get(i).setName(obj.name);
                        return true;
                    }
                }

                if (spr.getName().startsWith("vegetation_ornamental_01")) {
                    int id = Integer.parseInt(spr.getName().replace("vegetation_ornamental_01_", ""));

                    for (int j = 0; j < this.tileId.length; j++) {
                        if (this.tileId[j] == id) {
                            Flowerbed.CategoryData sqCategoryData = (Flowerbed.CategoryData)this.setCatModData(_sqErosionData);
                            sqCategoryData.hasSpawned = true;
                            sqCategoryData.gameObj = j;
                            sqCategoryData.dispSeason = -1;
                            ErosionObj obj = this.objs.get(sqCategoryData.gameObj);
                            _sq.getObjects().get(i).setName(obj.name);
                            return true;
                        }
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
        return false;
    }

    @Override
    public void update(IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionCategory.Data _sqCategoryData, ErosionData.Chunk _chunkData, int _eTick) {
        Flowerbed.CategoryData sqCategoryData = (Flowerbed.CategoryData)_sqCategoryData;
        if (!sqCategoryData.doNothing) {
            if (sqCategoryData.gameObj >= 0 && sqCategoryData.gameObj < this.objs.size()) {
                ErosionObj gameObj = this.objs.get(sqCategoryData.gameObj);
                boolean bTree = false;
                int stage = 0;
                int dispSeason = ErosionMain.getInstance().getSeasons().getSeason();
                boolean bloom = false;
                if (dispSeason == 5) {
                    IsoObject isoObj = gameObj.getObject(_sq, false);
                    if (isoObj != null) {
                        isoObj.setSprite(ErosionMain.getInstance().getSpriteManager().getSprite("blends_natural_01_64"));
                        isoObj.setName(null);
                    }

                    this.clearCatModData(_sqErosionData);
                } else {
                    this.updateObj(_sqErosionData, _sqCategoryData, _sq, gameObj, false, 0, dispSeason, false);
                }
            } else {
                this.clearCatModData(_sqErosionData);
            }
        }
    }

    @Override
    public void init() {
        String sheet = "vegetation_ornamental_01_";

        for (int i = 0; i < this.tileId.length; i++) {
            ErosionObjSprites objSpr = new ErosionObjSprites(1, "Flowerbed", false, false, false);
            objSpr.setBase(0, "vegetation_ornamental_01_" + this.tileId[i], 1);
            objSpr.setBase(0, "vegetation_ornamental_01_" + this.tileId[i], 2);
            objSpr.setBase(0, "vegetation_ornamental_01_" + (this.tileId[i] + 16), 4);
            ErosionObj obj = new ErosionObj(objSpr, 30, 0.0F, 0.0F, false);
            this.objs.add(obj);
        }
    }

    @Override
    protected ErosionCategory.Data allocData() {
        return new Flowerbed.CategoryData();
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

        @Override
        public void save(ByteBuffer output) {
            super.save(output);
            output.put((byte)this.gameObj);
        }

        @Override
        public void load(ByteBuffer input, int WorldVersion) {
            super.load(input, WorldVersion);
            this.gameObj = input.get();
        }
    }
}
