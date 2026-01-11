// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.MapCollisionData;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class BrokenFences {
    private static final BrokenFences instance = new BrokenFences();
    private final THashMap<String, BrokenFences.Tile> unbrokenMap = new THashMap<>();
    private final THashMap<String, BrokenFences.Tile> brokenLeftMap = new THashMap<>();
    private final THashMap<String, BrokenFences.Tile> brokenRightMap = new THashMap<>();
    private final THashMap<String, BrokenFences.Tile> allMap = new THashMap<>();

    public static BrokenFences getInstance() {
        return instance;
    }

    private ArrayList<String> tableToTiles(KahluaTableImpl tiles) {
        if (tiles == null) {
            return null;
        } else {
            ArrayList<String> result = null;

            for (KahluaTableIterator it = tiles.iterator(); it.advance(); result.add(it.getValue().toString())) {
                if (result == null) {
                    result = new ArrayList<>();
                }
            }

            return result;
        }
    }

    private ArrayList<String> tableToTiles(KahluaTable table, String key) {
        return this.tableToTiles((KahluaTableImpl)table.rawget(key));
    }

    public void addBrokenTiles(KahluaTableImpl tiles) {
        KahluaTableIterator it = tiles.iterator();

        while (it.advance()) {
            String key = it.getKey().toString();
            if (!"VERSION".equalsIgnoreCase(key)) {
                KahluaTableImpl value = (KahluaTableImpl)it.getValue();
                BrokenFences.Tile tile = new BrokenFences.Tile();
                tile.self = this.tableToTiles(value, "self");
                tile.left = this.tableToTiles(value, "left");
                tile.right = this.tableToTiles(value, "right");
                this.unbrokenMap.put(key, tile);
                PZArrayUtil.forEach(tile.left, s -> this.brokenLeftMap.put(s, tile));
                PZArrayUtil.forEach(tile.right, s -> this.brokenRightMap.put(s, tile));
            }
        }

        this.allMap.putAll(this.unbrokenMap);
        this.allMap.putAll(this.brokenLeftMap);
        this.allMap.putAll(this.brokenRightMap);
    }

    public void addDebrisTiles(KahluaTableImpl tiles) {
        KahluaTableIterator it = tiles.iterator();

        while (it.advance()) {
            String key = it.getKey().toString();
            if (!"VERSION".equalsIgnoreCase(key)) {
                KahluaTableImpl value = (KahluaTableImpl)it.getValue();
                BrokenFences.Tile tile = this.unbrokenMap.get(key);
                if (tile == null) {
                    throw new IllegalArgumentException("addDebrisTiles() with unknown tile");
                }

                tile.debrisN = this.tableToTiles(value, "north");
                tile.debrisS = this.tableToTiles(value, "south");
                tile.debrisW = this.tableToTiles(value, "west");
                tile.debrisE = this.tableToTiles(value, "east");
            }
        }
    }

    public void setDestroyed(IsoObject obj) {
        obj.RemoveAttachedAnims();
        obj.getSquare().removeBlood(false, true);
        this.updateSprite(obj, true, true);
    }

    public void setDamagedLeft(IsoObject obj) {
        this.updateSprite(obj, true, false);
    }

    public void setDamagedRight(IsoObject obj) {
        this.updateSprite(obj, false, true);
    }

    public void updateSprite(IsoObject obj, boolean brokenLeft, boolean brokenRight) {
        if (this.isBreakableObject(obj)) {
            BrokenFences.Tile tile = this.allMap.get(obj.sprite.name);
            String spriteName = null;
            if (brokenLeft && brokenRight) {
                spriteName = tile.pickRandom(tile.self);
            } else if (brokenLeft) {
                spriteName = tile.pickRandom(tile.left);
            } else if (brokenRight) {
                spriteName = tile.pickRandom(tile.right);
            }

            if (spriteName != null) {
                IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                sprite.name = spriteName;
                obj.setSprite(sprite);
                obj.transmitUpdatedSprite();
                obj.getSquare().RecalcAllWithNeighbours(true);
                MapCollisionData.instance.squareChanged(obj.getSquare());
                PolygonalMap2.instance.squareChanged(obj.getSquare());
                IsoRegions.squareChanged(obj.getSquare());
                obj.invalidateRenderChunkLevel(256L);
            }
        }
    }

    private boolean isNW(IsoObject obj) {
        PropertyContainer props = obj.getProperties();
        return props.has(IsoFlagType.collideN) && props.has(IsoFlagType.collideW);
    }

    private void damageAdjacent(IsoGridSquare square, IsoDirections dirAdjacent, IsoDirections dirBreak) {
        IsoGridSquare adjacent = square.getAdjacentSquare(dirAdjacent);
        if (adjacent != null) {
            boolean north = dirAdjacent == IsoDirections.W || dirAdjacent == IsoDirections.E;
            IsoObject obj = this.getBreakableObject(adjacent, north);
            if (obj != null) {
                boolean breakLeft = dirAdjacent == IsoDirections.N || dirAdjacent == IsoDirections.E;
                boolean breakRight = dirAdjacent == IsoDirections.S || dirAdjacent == IsoDirections.W;
                if (!this.isNW(obj) || dirAdjacent != IsoDirections.S && dirAdjacent != IsoDirections.E) {
                    if (breakLeft && this.isBrokenRight(obj)) {
                        this.destroyFence(obj, dirBreak);
                    } else if (breakRight && this.isBrokenLeft(obj)) {
                        this.destroyFence(obj, dirBreak);
                    } else {
                        this.updateSprite(obj, breakLeft, breakRight);
                    }
                }
            }
        }
    }

    public void destroyFence(IsoObject obj, IsoDirections dir) {
        if (this.isBreakableObject(obj)) {
            IsoGridSquare square = obj.getSquare();
            String soundName = "BreakObject";
            if (obj instanceof IsoThumpable thumpable) {
                soundName = thumpable.getBreakSound();
            }

            String materialStr = obj.getProperties().get("ThumpSound");
            if ("ZombieThumpWood".equals(materialStr)) {
                soundName = "ZombieThumpWoodCollapse";
            }

            if (GameServer.server) {
                GameServer.PlayWorldSoundServer(soundName, false, square, 1.0F, 20.0F, 1.0F, true);
            } else {
                SoundManager.instance.PlayWorldSound(soundName, square, 1.0F, 20.0F, 1.0F, true);
            }

            boolean north = obj.getProperties().has(IsoFlagType.collideN);
            boolean west = obj.getProperties().has(IsoFlagType.collideW);
            if (obj instanceof IsoThumpable) {
                IsoObject objNew = IsoObject.getNew();
                objNew.setSquare(square);
                objNew.setSprite(obj.getSprite());
                int index = obj.getObjectIndex();
                square.transmitRemoveItemFromSquare(obj);
                square.transmitAddObjectToSquare(objNew, index);
                obj = objNew;
            }

            this.addDebrisObject(obj, dir);
            this.setDestroyed(obj);
            if (north && west) {
                this.damageAdjacent(square, IsoDirections.S, dir);
                this.damageAdjacent(square, IsoDirections.E, dir);
            } else if (north) {
                this.damageAdjacent(square, IsoDirections.W, dir);
                this.damageAdjacent(square, IsoDirections.E, dir);
            } else if (west) {
                this.damageAdjacent(square, IsoDirections.N, dir);
                this.damageAdjacent(square, IsoDirections.S, dir);
            }

            square.RecalcAllWithNeighbours(true);
            MapCollisionData.instance.squareChanged(square);
            PolygonalMap2.instance.squareChanged(square);
            IsoRegions.squareChanged(square);
            square.invalidateRenderChunkLevel(384L);
        }
    }

    private boolean isUnbroken(IsoObject obj) {
        return obj != null && obj.sprite != null && obj.sprite.name != null ? this.unbrokenMap.contains(obj.sprite.name) : false;
    }

    private boolean isBrokenLeft(IsoObject obj) {
        return obj != null && obj.sprite != null && obj.sprite.name != null ? this.brokenLeftMap.contains(obj.sprite.name) : false;
    }

    private boolean isBrokenRight(IsoObject obj) {
        return obj != null && obj.sprite != null && obj.sprite.name != null ? this.brokenRightMap.contains(obj.sprite.name) : false;
    }

    public boolean isBreakableObject(IsoObject obj) {
        return obj != null && obj.sprite != null && obj.sprite.name != null ? this.allMap.containsKey(obj.sprite.name) : false;
    }

    public boolean isBreakableSprite(String spriteName) {
        return this.allMap.containsKey(spriteName);
    }

    public IsoObject getBreakableObject(IsoGridSquare square, boolean north) {
        for (int n = 0; n < square.objects.size(); n++) {
            IsoObject obj = square.objects.get(n);
            if (this.isBreakableObject(obj)
                && (north && obj.getProperties().has(IsoFlagType.collideN) || !north && obj.getProperties().has(IsoFlagType.collideW))) {
                return obj;
            }
        }

        return null;
    }

    public void addItems(IsoObject obj, IsoGridSquare square) {
        PropertyContainer props = obj.getProperties();
        if (props != null) {
            String Material = props.get("Material");
            String Material2 = props.get("Material2");
            String Material3 = props.get("Material3");
            if ("Wood".equals(Material) || "Wood".equals(Material2) || "Wood".equals(Material3)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                if (Rand.NextBool(5)) {
                    square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.UnusableWood"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                }
            }

            if (("MetalBars".equals(Material) || "MetalBars".equals(Material2) || "MetalBars".equals(Material3)) && Rand.NextBool(2)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.MetalBar"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("MetalWire".equals(Material) || "MetalWire".equals(Material2) || "MetalWire".equals(Material3)) && Rand.NextBool(3)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Wire"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("MetalPlates".equals(Material) || "MetalPlates".equals(Material2) || "MetalPlates".equals(Material3)) && Rand.NextBool(2)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.SheetMetal"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.NutsBolts"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("MetalScrap".equals(Material) || "MetalScrap".equals(Material2) || "MetalScrap".equals(Material3)) && Rand.NextBool(2)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.SteelChunk"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.SteelPiece"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("Nails".equals(Material) || "Nails".equals(Material2) || "Nails".equals(Material3)) && Rand.NextBool(2)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Nails"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }

            if (("Screws".equals(Material) || "Screws".equals(Material2) || "Screws".equals(Material3)) && Rand.NextBool(2)) {
                square.AddWorldInventoryItem(InventoryItemFactory.CreateItem("Base.Screws"), Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
            }
        }
    }

    private void addDebrisObject(IsoObject obj, IsoDirections dir) {
        if (this.isBreakableObject(obj)) {
            BrokenFences.Tile tile = this.allMap.get(obj.sprite.name);
            IsoGridSquare square = obj.getSquare();
            String spriteName;
            switch (dir) {
                case N:
                    spriteName = tile.pickRandom(tile.debrisN);
                    square = square.getAdjacentSquare(dir);
                    break;
                case S:
                    spriteName = tile.pickRandom(tile.debrisS);
                    break;
                case W:
                    spriteName = tile.pickRandom(tile.debrisW);
                    square = square.getAdjacentSquare(dir);
                    break;
                case E:
                    spriteName = tile.pickRandom(tile.debrisE);
                    break;
                default:
                    throw new IllegalArgumentException("invalid direction");
            }

            if (spriteName != null && square != null && square.TreatAsSolidFloor()) {
                IsoObject objNew = IsoObject.getNew(square, spriteName, null, false);
                square.transmitAddObjectToSquare(objNew, square == obj.getSquare() ? obj.getObjectIndex() : -1);
                this.addItems(obj, square);
            }
        }
    }

    public void Reset() {
        this.unbrokenMap.clear();
        this.brokenLeftMap.clear();
        this.brokenRightMap.clear();
        this.allMap.clear();
    }

    private static final class Tile {
        ArrayList<String> self;
        ArrayList<String> left;
        ArrayList<String> right;
        ArrayList<String> debrisN;
        ArrayList<String> debrisS;
        ArrayList<String> debrisW;
        ArrayList<String> debrisE;

        String pickRandom(ArrayList<String> choices) {
            return choices == null ? null : PZArrayUtil.pickRandom(choices);
        }
    }
}
