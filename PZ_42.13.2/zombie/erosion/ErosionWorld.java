// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion;

import zombie.erosion.categories.ErosionCategory;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;

public final class ErosionWorld {
    public boolean init() {
        ErosionRegions.init();
        return true;
    }

    public void validateSpawn(IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData) {
        boolean isExterior = _sq.has(IsoFlagType.exterior);
        boolean hasWall = _sq.has(IsoObjectType.wall);
        IsoObject floor = _sq.getFloor();
        String sqTexName = floor != null && floor.getSprite() != null ? floor.getSprite().getName() : null;
        if (sqTexName == null) {
            _sqErosionData.doNothing = true;
        } else {
            boolean hasSpawned = false;

            for (int i = 0; i < ErosionRegions.regions.size(); i++) {
                ErosionRegions.Region region = ErosionRegions.regions.get(i);
                String m = region.tileNameMatch;
                if ((m == null || sqTexName.startsWith(m))
                    && (!region.checkExterior || region.isExterior == isExterior)
                    && (!region.hasWall || region.hasWall == hasWall)) {
                    for (int j = 0; j < region.categories.size(); j++) {
                        ErosionCategory category = region.categories.get(j);
                        boolean spawned = category.replaceExistingObject(_sq, _sqErosionData, _chunkData, isExterior, hasWall);
                        if (!spawned) {
                            spawned = category.validateSpawn(_sq, _sqErosionData, _chunkData, isExterior, hasWall, false);
                        }

                        if (spawned) {
                            hasSpawned = true;
                            break;
                        }
                    }
                }
            }

            if (!hasSpawned) {
                _sqErosionData.doNothing = true;
            }
        }
    }

    public void update(IsoGridSquare _sq, ErosionData.Square _sqErosionData, ErosionData.Chunk _chunkData, int _eTick) {
        if (_sqErosionData.regions != null) {
            for (int i = 0; i < _sqErosionData.regions.size(); i++) {
                ErosionCategory.Data sqCategoryData = _sqErosionData.regions.get(i);
                ErosionCategory category = ErosionRegions.getCategory(sqCategoryData.regionId, sqCategoryData.categoryId);
                int size = _sqErosionData.regions.size();
                category.update(_sq, _sqErosionData, sqCategoryData, _chunkData, _eTick);
                if (size > _sqErosionData.regions.size()) {
                    i--;
                }
            }
        }
    }
}
