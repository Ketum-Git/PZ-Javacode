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

    public void validateSpawn(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData) {
        boolean isExterior = square.has(IsoFlagType.exterior);
        boolean hasWall = square.has(IsoObjectType.wall);
        IsoObject floor = square.getFloor();
        String sqTexName = floor != null && floor.getSprite() != null ? floor.getSprite().getName() : null;
        if (sqTexName == null) {
            sqErosionData.doNothing = true;
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
                        boolean spawned = category.replaceExistingObject(square, sqErosionData, chunkData, isExterior, hasWall);
                        if (!spawned) {
                            spawned = category.validateSpawn(square, sqErosionData, chunkData, isExterior, hasWall, false);
                        }

                        if (spawned) {
                            hasSpawned = true;
                            break;
                        }
                    }
                }
            }

            if (!hasSpawned) {
                sqErosionData.doNothing = true;
            }
        }
    }

    public void update(IsoGridSquare square, ErosionData.Square sqErosionData, ErosionData.Chunk chunkData, int eTick) {
        if (sqErosionData.regions != null) {
            for (int i = 0; i < sqErosionData.regions.size(); i++) {
                ErosionCategory.Data sqCategoryData = sqErosionData.regions.get(i);
                ErosionCategory category = ErosionRegions.getCategory(sqCategoryData.regionId, sqCategoryData.categoryId);
                int size = sqErosionData.regions.size();
                category.update(square, sqErosionData, sqCategoryData, chunkData, eTick);
                if (size > sqErosionData.regions.size()) {
                    i--;
                }
            }
        }
    }
}
