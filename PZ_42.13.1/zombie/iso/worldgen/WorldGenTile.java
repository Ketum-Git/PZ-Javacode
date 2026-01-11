// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import zombie.GameWindow;
import zombie.iso.CellLoader;
import zombie.iso.IsoCell;
import zombie.iso.IsoChunk;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoTree;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.worldgen.biomes.Feature;
import zombie.iso.worldgen.biomes.FeatureType;
import zombie.iso.worldgen.biomes.IBiome;
import zombie.iso.worldgen.biomes.TileGroup;
import zombie.iso.worldgen.roads.Road;
import zombie.iso.worldgen.veins.OreVein;

public class WorldGenTile {
    public static final String NO_TREE = "NO_TREE";
    public static final String NO_BUSH = "NO_BUSH";
    public static final String NO_GRASS = "NO_GRASS";

    public void setTiles(
        IBiome biome,
        IsoGridSquare square,
        IsoChunk ch,
        IsoCell cell,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> toBeDone,
        boolean isMap,
        Random rnd
    ) {
        List<String> tiles = new ArrayList<>();

        for (FeatureType type : FeatureType.values()) {
            String tile = this.getBiomeTile(biome, type, ch, tileX, tileY, tileZ, toBeDone, rnd, 16);
            if (tile != null) {
                if (isMap) {
                    IsoTree tree = square.getTree();
                    if (tree != null) {
                        square.DeleteTileObject(tree);
                    }

                    IsoObject bush = square.getBush();
                    if (bush != null) {
                        square.DeleteTileObject(bush);
                    }

                    if (square.getObjects().size() - square.getGrassLike().size() == 1) {
                        tiles.add(tile);
                    }
                } else {
                    if (tiles.size() > 1) {
                        tiles.remove(1);
                    }

                    tiles.add(tile);
                }
            }
        }

        for (String tile : tiles) {
            this.applyTile(tile, square, cell, x, y, z, rnd);
        }
    }

    public boolean setTiles(
        IBiome biome,
        FeatureType type,
        IsoGridSquare square,
        IsoChunk ch,
        IsoCell cell,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> toBeDone,
        Random rnd
    ) {
        for (int i = 0; i < 16; i++) {
            int targetSize = 8 >> i;
            if (targetSize == 0) {
                return false;
            }

            String tile = this.getBiomeTile(biome, type, ch, tileX, tileY, tileZ, toBeDone, rnd, targetSize);
            if (tile != null) {
                this.applyTile(tile, square, cell, x, y, z, rnd);
                return true;
            }
        }

        return false;
    }

    private String getBiomeTile(
        IBiome biome, FeatureType type, IsoChunk ch, int tileX, int tileY, int tileZ, EnumMap<FeatureType, String[]> toBeDone, Random rnd, int targetSize
    ) {
        String tile = toBeDone.get(type)[tileX + tileY * 8];
        if (tile == null || tile.isEmpty()) {
            Map<FeatureType, List<Feature>> features = biome.getFeatures();
            if (features == null) {
                return null;
            }

            List<Feature> featuresList = features.get(type);
            if (featuresList == null || featuresList.isEmpty()) {
                return null;
            }

            List<Feature> featuresFiltered = new ArrayList<>();

            for (Feature feature : featuresList) {
                if (feature.minSize() <= targetSize) {
                    featuresFiltered.add(feature);
                }
            }

            float prefilterProba = 0.0F;
            float postfilterProba = 0.0F;

            for (Feature featurex : featuresList) {
                prefilterProba += featurex.probability().getValue();
            }

            for (Feature featurex : featuresFiltered) {
                postfilterProba += featurex.probability().getValue();
            }

            Feature featurex = this.findFeature(featuresFiltered, prefilterProba, postfilterProba, rnd);
            if (featurex == null) {
                return null;
            }

            List<TileGroup> tileGroups = new ArrayList<>();

            for (TileGroup tileGroup : featurex.tileGroups()) {
                if (tileGroup.sx() <= targetSize && tileGroup.sy() <= targetSize) {
                    tileGroups.add(tileGroup);
                }
            }

            if (tileGroups.isEmpty()) {
                return null;
            }

            TileGroup tileGroupx = tileGroups.get(rnd.nextInt(tileGroups.size()));
            if (tileX + tileGroupx.sx() - 1 >= 8 || tileY + tileGroupx.sy() - 1 >= 8) {
                return null;
            }

            if (biome.placements() != null && !this.checkFutureSquares(tileGroupx, ch, tileX, tileY, tileZ, biome.placements().get(type))) {
                return null;
            }

            for (int ix = 0; ix < tileGroupx.sx(); ix++) {
                for (int iy = 0; iy < tileGroupx.sy(); iy++) {
                    toBeDone.get(type)[ix + tileX + (iy + tileY) * 8] = tileGroupx.tiles().get(ix + iy * tileGroupx.sx());
                }
            }

            tile = tileGroupx.tiles().get(0);
        }

        return tile;
    }

    public Feature findFeature(List<Feature> features, float prefilterProba, float postfilterProba, Random rnd) {
        if (features != null && !features.isEmpty()) {
            float rndValue = rnd.nextFloat();
            float probability = 0.0F;
            Feature feature = null;

            for (Feature tmpFeature : features) {
                probability += tmpFeature.probability().getValue() / postfilterProba * prefilterProba;
                if (!(rndValue >= probability)) {
                    feature = tmpFeature;
                    break;
                }
            }

            return feature;
        } else {
            return null;
        }
    }

    private boolean checkFutureSquares(TileGroup tg, IsoChunk ch, int tileX, int tileY, int tileZ, List<String> placement) {
        if (tg.sx() == 1 && tg.sy() == 1) {
            return true;
        } else {
            for (int ix = 0; ix < tg.sx(); ix++) {
                for (int iy = 0; iy < tg.sy(); iy++) {
                    if (ix != 0 || iy != 0) {
                        IsoGridSquare square = ch.getGridSquare(tileX + ix, tileY + iy, tileZ);
                        if (square == null) {
                            return false;
                        }

                        IsoObject floor = square.getFloor();
                        if (floor == null) {
                            return false;
                        }

                        if (square.getObjects().size() - square.getGrassLike().size() - square.getBushes().size() > 1) {
                            return false;
                        }

                        if (!WorldGenUtils.INSTANCE.canPlace(placement, floor.getSprite().getName())) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    public void setTile(
        OreVein vein,
        IsoGridSquare square,
        IsoCell cell,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> toBeDone,
        Random rnd
    ) {
        List<TileGroup> tileGroups = vein.getSingleFeatures();
        TileGroup tileGroup = tileGroups.get(rnd.nextInt(tileGroups.size()));
        this.applyTile(tileGroup.tiles().get(0), square, cell, x, y, z, rnd);
    }

    public void setTile(
        Road road,
        IsoGridSquare square,
        IsoCell cell,
        int x,
        int y,
        int z,
        int tileX,
        int tileY,
        int tileZ,
        EnumMap<FeatureType, String[]> toBeDone,
        Random rnd
    ) {
        List<TileGroup> tileGroups = road.getSingleFeatures();
        TileGroup tileGroup = tileGroups.get(rnd.nextInt(tileGroups.size()));
        this.applyTile(tileGroup.tiles().get(0), square, cell, x, y, z, rnd);
    }

    public void applyTile(String tile, IsoGridSquare square, IsoCell cell, int x, int y, int z, Random rnd) {
        if (!tile.equals("NO_TREE") && !tile.equals("NO_BUSH") && !tile.equals("NO_GRASS")) {
            IsoSprite spr = this.getSprite(IsoChunk.Fix2x(tile));
            CellLoader.DoTileObjectCreation(spr, spr.getType(), square, cell, x, y, z, tile);
        }
    }

    public IsoSprite getSprite(String tile) {
        IsoSprite spr = IsoSpriteManager.instance.namedMap.get(tile);
        if (spr == null) {
            Logger.getLogger(GameWindow.class.getName()).log(Level.SEVERE, "Missing tile definition: " + tile);
            spr = IsoSprite.getSprite(IsoSpriteManager.instance, "carpentry_02_58", 0);
        }

        return spr;
    }

    public TileGroup getGround(IBiome biome, Random rnd) {
        List<Feature> features = biome.getFeatures().get(FeatureType.GROUND);
        Feature feature = features.get(rnd.nextInt(features.size()));
        List<TileGroup> tileGroups = feature.tileGroups();
        return tileGroups.get(rnd.nextInt(features.size()));
    }

    public TileGroup getPlant(IBiome biome, Random rnd) {
        List<Feature> features = biome.getFeatures().get(FeatureType.PLANT);
        if (features != null && !features.isEmpty()) {
            Feature feature = features.get(rnd.nextInt(features.size()));
            if (rnd.nextFloat() > feature.probability().getValue()) {
                return null;
            } else {
                List<TileGroup> tileGroups = feature.tileGroups();
                return tileGroups.get(rnd.nextInt(tileGroups.size()));
            }
        } else {
            return null;
        }
    }

    public void setGround(IsoSprite spr, IsoGridSquare sq) {
        spr.solidfloor = true;
        IsoObject floor = sq.getFloor();
        if (floor != null) {
            floor.clearAttachedAnimSprite();
            floor.setSprite(spr);
        }
    }

    public void deleteTiles(IsoGridSquare sq) {
        List<IsoObject> toDelete = new ArrayList<>();

        for (IsoObject element : sq.getObjects().getElements()) {
            if (element != null && !element.isFloor()) {
                toDelete.add(element);
            }
        }

        for (IsoObject elementx : toDelete) {
            sq.DeleteTileObject(elementx);
        }
    }

    public void deleteTiles(IsoGridSquare sq, List<String> toRemove) {
        List<IsoObject> toDelete = new ArrayList<>();

        for (IsoObject element : sq.getObjects().getElements()) {
            if (element != null) {
                String tmpTile = element.getSprite().name;
                if (toRemove.contains(tmpTile)) {
                    toDelete.add(element);
                }
            }
        }

        for (IsoObject elementx : toDelete) {
            sq.DeleteTileObject(elementx);
        }
    }
}
