// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.categories;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.erosion.ErosionData;
import zombie.erosion.ErosionMain;
import zombie.erosion.ErosionRegions;
import zombie.erosion.obj.ErosionObj;
import zombie.erosion.season.ErosionSeason;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;

public abstract class ErosionCategory {
    public int id;
    public ErosionRegions.Region region;
    protected ErosionCategory.SeasonDisplay[] seasonDisp = new ErosionCategory.SeasonDisplay[6];

    public ErosionCategory() {
        for (int i = 0; i < 6; i++) {
            this.seasonDisp[i] = new ErosionCategory.SeasonDisplay();
        }
    }

    protected ErosionCategory.Data getCatModData(ErosionData.Square _sqErosionData) {
        for (int i = 0; i < _sqErosionData.regions.size(); i++) {
            ErosionCategory.Data data = _sqErosionData.regions.get(i);
            if (data.regionId == this.region.id && data.categoryId == this.id) {
                return data;
            }
        }

        return null;
    }

    protected ErosionCategory.Data setCatModData(ErosionData.Square _sqErosionData) {
        ErosionCategory.Data data = this.getCatModData(_sqErosionData);
        if (data == null) {
            data = this.allocData();
            data.regionId = this.region.id;
            data.categoryId = this.id;
            _sqErosionData.regions.add(data);
            if (_sqErosionData.regions.size() > 5) {
                DebugLog.log("> 5 regions on a square");
            }
        }

        return data;
    }

    protected IsoObject validWall(IsoGridSquare _sq, boolean _north, boolean _doWindow) {
        if (_sq == null) {
            return null;
        } else {
            IsoGridSquare other = _north ? _sq.getTileInDirection(IsoDirections.N) : _sq.getTileInDirection(IsoDirections.W);
            IsoObject object = null;
            if (_sq.isWallTo(other)) {
                if (_north && _sq.has(IsoFlagType.cutN) && !_sq.has(IsoFlagType.canPathN)
                    || !_north && _sq.has(IsoFlagType.cutW) && !_sq.has(IsoFlagType.canPathW)) {
                    object = _sq.getWall(_north);
                }
            } else if (_doWindow && (_sq.isWindowBlockedTo(other) || _sq.isWindowTo(other))) {
                object = _sq.getWindowTo(other);
                if (object == null) {
                    object = _sq.getWall(_north);
                }
            }

            if (object != null) {
                if (_sq.getZ() > 0) {
                    String spname = object.getSprite().getName();
                    return spname != null && !spname.contains("roof") ? object : null;
                } else {
                    return object;
                }
            } else {
                return null;
            }
        }
    }

    protected float clerp(float _t, float _a, float _b) {
        float t2 = (float)(1.0 - Math.cos(_t * Math.PI)) / 2.0F;
        return _a * (1.0F - t2) + _b * t2;
    }

    protected int currentSeason(float _magicNum, ErosionObj _gameObj) {
        int dispSeason = 0;
        ErosionSeason Seasons = ErosionMain.getInstance().getSeasons();
        int season = Seasons.getSeason();
        float seasonDay = Seasons.getSeasonDay();
        float seasonDays = Seasons.getSeasonDays();
        float seasonDaysHalf = seasonDays / 2.0F;
        float halfMagicNum = seasonDaysHalf * _magicNum;
        ErosionCategory.SeasonDisplay curSeason = this.seasonDisp[season];
        if (curSeason.split && seasonDay >= seasonDaysHalf + halfMagicNum) {
            dispSeason = curSeason.season2;
        } else if ((!curSeason.split || !(seasonDay >= halfMagicNum)) && !(seasonDay >= seasonDays * _magicNum)) {
            ErosionCategory.SeasonDisplay prevSeason;
            if (season == 5) {
                prevSeason = this.seasonDisp[4];
            } else if (season == 1) {
                prevSeason = this.seasonDisp[5];
            } else if (season == 2) {
                prevSeason = this.seasonDisp[1];
            } else {
                prevSeason = this.seasonDisp[2];
            }

            if (prevSeason.split) {
                dispSeason = prevSeason.season2;
            } else {
                dispSeason = prevSeason.season1;
            }
        } else {
            dispSeason = curSeason.season1;
        }

        return dispSeason;
    }

    protected boolean currentBloom(float _magicNum, ErosionObj _gameObj) {
        boolean bloom = false;
        ErosionSeason Seasons = ErosionMain.getInstance().getSeasons();
        int season = Seasons.getSeason();
        if (_gameObj.hasFlower && season == 2) {
            float seasonDay = Seasons.getSeasonDay();
            float seasonDays = Seasons.getSeasonDays();
            float seasonDaysHalf = seasonDays / 2.0F;
            float halfMagicNum = seasonDaysHalf * _magicNum;
            float totalbloomdays = seasonDays - halfMagicNum;
            float curbloomday = seasonDay - halfMagicNum;
            float bdaysMax = totalbloomdays * _gameObj.bloomEnd;
            float bdaysMin = totalbloomdays * _gameObj.bloomStart;
            float totalsplit = (bdaysMax - bdaysMin) / 2.0F;
            float offset = totalsplit * _magicNum;
            bdaysMax = bdaysMin + totalsplit + offset;
            bdaysMin += offset;
            if (curbloomday >= bdaysMin && curbloomday <= bdaysMax) {
                bloom = true;
            }
        }

        return bloom;
    }

    public void updateObj(
        ErosionData.Square _sqErosionData,
        ErosionCategory.Data _sqCategoryData,
        IsoGridSquare _sq,
        ErosionObj _gameObj,
        boolean _bTree,
        int _stage,
        int _dispSeason,
        boolean _bloom
    ) {
        if (!_sqCategoryData.hasSpawned) {
            if (!_gameObj.placeObject(_sq, _stage, _bTree, _dispSeason, _bloom)) {
                this.clearCatModData(_sqErosionData);
                return;
            }

            _sqCategoryData.hasSpawned = true;
        } else if (_sqCategoryData.stage != _stage || _sqCategoryData.dispSeason != _dispSeason || _sqCategoryData.dispBloom != _bloom) {
            IsoObject obj = _gameObj.getObject(_sq, false);
            if (obj == null) {
                this.clearCatModData(_sqErosionData);
                return;
            }

            _gameObj.setStageObject(_stage, obj, _dispSeason, _bloom);
        }

        _sqCategoryData.stage = _stage;
        _sqCategoryData.dispSeason = _dispSeason;
        _sqCategoryData.dispBloom = _bloom;
    }

    protected void clearCatModData(ErosionData.Square _sqErosionData) {
        for (int i = 0; i < _sqErosionData.regions.size(); i++) {
            ErosionCategory.Data data = _sqErosionData.regions.get(i);
            if (data.regionId == this.region.id && data.categoryId == this.id) {
                _sqErosionData.regions.remove(i);
                return;
            }
        }
    }

    public abstract void init();

    public abstract boolean replaceExistingObject(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, boolean var4, boolean var5);

    public abstract boolean validateSpawn(IsoGridSquare var1, ErosionData.Square var2, ErosionData.Chunk var3, boolean var4, boolean var5, boolean var6);

    public abstract void update(IsoGridSquare var1, ErosionData.Square var2, ErosionCategory.Data var3, ErosionData.Chunk var4, int var5);

    protected abstract ErosionCategory.Data allocData();

    public static ErosionCategory.Data loadCategoryData(ByteBuffer input, int WorldVersion) {
        int regionID = input.get();
        int categoryID = input.get();
        ErosionCategory category = ErosionRegions.getCategory(regionID, categoryID);
        ErosionCategory.Data data = category.allocData();
        data.regionId = regionID;
        data.categoryId = categoryID;
        data.load(input, WorldVersion);
        return data;
    }

    public abstract void getObjectNames(ArrayList<String> var1);

    public static class Data {
        public int regionId;
        public int categoryId;
        public boolean doNothing;
        public boolean hasSpawned;
        public int stage;
        public int dispSeason;
        public boolean dispBloom;

        public void save(ByteBuffer output) {
            byte flags = 0;
            if (this.doNothing) {
                flags = (byte)(flags | 1);
            }

            if (this.hasSpawned) {
                flags = (byte)(flags | 2);
            }

            if (this.dispBloom) {
                flags = (byte)(flags | 4);
            }

            if (this.stage == 1) {
                flags = (byte)(flags | 8);
            } else if (this.stage == 2) {
                flags = (byte)(flags | 16);
            } else if (this.stage == 3) {
                flags = (byte)(flags | 32);
            } else if (this.stage == 4) {
                flags = (byte)(flags | 64);
            } else if (this.stage > 4) {
                flags = (byte)(flags | 128);
            }

            output.put((byte)this.regionId);
            output.put((byte)this.categoryId);
            output.put((byte)this.dispSeason);
            output.put(flags);
            if (this.stage > 4) {
                output.put((byte)this.stage);
            }
        }

        public void load(ByteBuffer input, int WorldVersion) {
            this.stage = 0;
            this.dispSeason = input.get();
            byte flags = input.get();
            this.doNothing = (flags & 1) != 0;
            this.hasSpawned = (flags & 2) != 0;
            this.dispBloom = (flags & 4) != 0;
            if ((flags & 8) != 0) {
                this.stage = 1;
            } else if ((flags & 16) != 0) {
                this.stage = 2;
            } else if ((flags & 32) != 0) {
                this.stage = 3;
            } else if ((flags & 64) != 0) {
                this.stage = 4;
            } else if ((flags & 128) != 0) {
                this.stage = input.get();
            }
        }
    }

    protected class SeasonDisplay {
        int season1;
        int season2;
        boolean split;

        protected SeasonDisplay() {
            Objects.requireNonNull(ErosionCategory.this);
            super();
        }
    }
}
