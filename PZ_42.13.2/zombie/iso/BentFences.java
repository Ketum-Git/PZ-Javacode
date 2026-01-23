// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.MapCollisionData;
import zombie.SandboxOptions;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameServer;
import zombie.pathfind.PolygonalMap2;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public class BentFences {
    private static final BentFences instance = new BentFences();
    private final ArrayList<BentFences.Entry> entries = new ArrayList<>();
    private final HashSet<String> collapsedTiles = new HashSet<>();
    private final HashSet<String> debrisTiles = new HashSet<>();
    private final HashMap<String, ArrayList<BentFences.Entry>> fenceMap = new HashMap<>();

    public static BentFences getInstance() {
        return instance;
    }

    private ArrayList<String> tableToTiles(KahluaTableImpl tiles, ArrayList<String> result) {
        if (tiles == null) {
            return result;
        } else {
            KahluaTableIterator it = tiles.iterator();

            while (it.advance()) {
                result.add(it.getValue().toString());
            }

            return result;
        }
    }

    private ArrayList<String> tableToTiles(KahluaTable table, String key) {
        ArrayList<String> tiles = new ArrayList<>();
        return this.tableToTiles((KahluaTableImpl)table.rawget(key), tiles);
    }

    public void addFenceTiles(int VERSION, KahluaTableImpl tiles) {
        KahluaTableIterator it = tiles.iterator();

        while (true) {
            KahluaTableImpl value;
            BentFences.Entry entry;
            KahluaTableImpl debrisTable;
            while (true) {
                if (!it.advance()) {
                    return;
                }

                value = (KahluaTableImpl)it.getValue();
                entry = new BentFences.Entry();
                entry.dir = IsoDirections.valueOf(value.rawgetStr("dir"));
                entry.health = value.rawgetInt("health") != -1 ? value.rawgetInt("health") : 100;
                entry.collapsedOffset = value.rawgetInt("collapsedOffset");
                entry.collapsedSizeX = value.rawgetInt("collapsedSizeX");
                entry.collapsedSizeY = value.rawgetInt("collapsedSizeY");
                entry.doSmash = value.rawgetBool("doSmash");
                KahluaTableImpl stageTable = (KahluaTableImpl)value.rawget("stages");
                KahluaTableImpl collapsedTable = (KahluaTableImpl)value.rawget("collapsed");
                debrisTable = (KahluaTableImpl)value.rawget("debris");
                if (stageTable != null) {
                    KahluaTableIterator stageit = stageTable.iterator();

                    while (stageit.advance()) {
                        KahluaTableImpl nextStage = (KahluaTableImpl)stageit.getValue();
                        entry.stages.put(nextStage.rawgetInt("stage"), this.tableToTiles(nextStage, "tiles"));
                    }

                    if (collapsedTable == null) {
                        break;
                    }

                    PZArrayUtil.addAll(entry.collapsed, this.tableToTiles(value, "collapsed"));
                    if (entry.collapsedSizeX * entry.collapsedSizeY == entry.collapsed.size()) {
                        this.collapsedTiles.addAll(entry.collapsed);
                        break;
                    }
                }
            }

            if (debrisTable != null) {
                PZArrayUtil.addAll(entry.debris, this.tableToTiles(value, "debris"));
                this.debrisTiles.addAll(entry.debris);
            }

            if (!entry.stages.isEmpty()) {
                entry.length = entry.stages.get(0).size();
                this.entries.add(entry);

                for (int i = 0; i < entry.stages.size(); i++) {
                    for (String spriteName : entry.stages.get(i)) {
                        ArrayList<BentFences.Entry> entries = this.fenceMap.get(spriteName);
                        if (entries == null) {
                            entries = new ArrayList<>();
                            this.fenceMap.put(spriteName, entries);
                        }

                        entries.add(entry);
                    }
                }
            }
        }
    }

    public boolean isBentObject(IsoObject obj) {
        return this.getBendStage(obj, this.getEntryForObject(obj)) > 0;
    }

    public boolean isUnbentObject(IsoObject obj) {
        BentFences.Entry entry = this.getEntryForObject(obj);
        return entry == null ? false : this.getBendStage(obj, entry) < entry.stages.size() - 1;
    }

    public boolean isUnbentObject(IsoObject obj, IsoDirections dir) {
        BentFences.Entry entry = this.getEntryForObject(obj);
        return entry != null && (!entry.isNorth() || dir == IsoDirections.N || dir == IsoDirections.S)
            ? this.getBendStage(obj, entry) < entry.stages.size() - 1
            : false;
    }

    private BentFences.Entry getEntryForObject(IsoObject obj) {
        return this.getEntryForObject(obj, IsoDirections.Max);
    }

    private BentFences.Entry getEntryForObject(IsoObject obj, IsoDirections dir) {
        if (obj != null && obj.sprite != null && obj.sprite.name != null) {
            if (!obj.getProperties().has(IsoFlagType.collideN) && !obj.getProperties().has(IsoFlagType.collideW)) {
                boolean entries = true;
            }

            ArrayList<BentFences.Entry> entries = this.fenceMap.get(obj.sprite.name);
            BentFences.Entry matched = null;
            if (entries != null) {
                for (int i = 0; i < entries.size(); i++) {
                    BentFences.Entry entry = entries.get(i);
                    if (dir == null || dir == IsoDirections.Max || entry.dir == dir) {
                        if (dir == IsoDirections.Max) {
                            if (this.isValidObject(obj, entry) && (matched == null || matched.length < entry.length)) {
                                matched = entry;
                            }
                        } else if (this.isValidObject(obj, entry)
                            && (this.getBendStage(obj, entry) < entry.stages.size() || entry.collapsed.isEmpty() || this.checkCanCollapse(obj, dir, entry))
                            && (matched == null || matched.length < entry.length)) {
                            matched = entry;
                        }
                    }
                }
            }

            return matched;
        } else {
            return null;
        }
    }

    private int getBendStage(IsoObject obj, BentFences.Entry entry) {
        if (obj != null && obj.sprite != null && obj.sprite.name != null && entry != null) {
            for (int i = 0; i < entry.stages.size(); i++) {
                if (entry.stages.get(i).contains(obj.sprite.name)) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    private int getTileIndex(ArrayList<String> tiles, IsoObject obj, BentFences.Entry entry) {
        return this.getTileIndex(tiles, obj, entry, false);
    }

    private int getTileIndex(ArrayList<String> tiles, IsoObject obj, BentFences.Entry entry, boolean forceFirst) {
        int index = -1;
        int lastIndex = -1;
        String spriteName = obj.getSpriteName();
        int centerTile = tiles.size() / 2;
        if (spriteName != null) {
            for (int i = 0; i < tiles.size(); i++) {
                if (tiles.get(i).equals(spriteName) && this.isValidSpan(tiles, obj, entry, i)) {
                    if (forceFirst || i == centerTile) {
                        return i;
                    }

                    lastIndex = index;
                    index = i;
                }
            }
        }

        return this.getBendStage(obj, entry) == 0 ? (lastIndex == -1 ? index : lastIndex) : index;
    }

    private boolean isValidSpan(ArrayList<String> tiles, IsoObject obj, BentFences.Entry entry, int index) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int i = 0; i < tiles.size(); i++) {
            int x = obj.square.x + (entry.isNorth() ? i - index : 0);
            int y = obj.square.y + (entry.isNorth() ? 0 : i - index);
            IsoGridSquare square = cell.getGridSquare(x, y, obj.square.z);
            if (square == null) {
                return false;
            }

            if (index != i && this.getObjectForEntry(square, tiles, i) == null) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidObject(IsoObject obj, BentFences.Entry entry) {
        int bendStage = this.getBendStage(obj, entry);
        if (bendStage == -1) {
            return false;
        } else {
            ArrayList<String> tiles = entry.stages.get(bendStage);
            return this.getTileIndex(tiles, obj, entry) != -1;
        }
    }

    IsoObject getObjectForEntry(IsoGridSquare square, ArrayList<String> tiles, int index) {
        for (int i = 0; i < square.getObjects().size(); i++) {
            IsoObject obj = square.getObjects().get(i);
            if (obj.sprite != null && obj.sprite.name != null && tiles.get(index).equals(obj.sprite.name)) {
                return obj;
            }
        }

        return null;
    }

    public boolean checkCanCollapse(IsoObject obj, IsoDirections dir, BentFences.Entry entry) {
        if (entry.collapsed.isEmpty()) {
            return false;
        } else {
            IsoCell cell = IsoWorld.instance.currentCell;
            int bendStage = this.getBendStage(obj, entry);
            ArrayList<String> tiles = entry.stages.get(bendStage);
            int index = this.getTileIndex(tiles, obj, entry);
            boolean north = entry.isNorth();
            int revX = dir == IsoDirections.W ? -1 : 1;
            int revY = dir == IsoDirections.N ? -1 : 1;
            int offXY = entry.collapsedOffset;
            int offsetX = obj.square.x + (north ? offXY - index : (dir == IsoDirections.W ? -1 : 0));
            int offsetY = obj.square.y + (north ? (dir == IsoDirections.N ? -1 : 0) : offXY - index);

            for (int x = 0; x < entry.collapsedSizeX; x++) {
                for (int y = 0; y < entry.collapsedSizeY; y++) {
                    IsoGridSquare square = cell.getGridSquare(offsetX + x * revX, offsetY + y * revY, obj.square.z);
                    if (square == null || !square.TreatAsSolidFloor()) {
                        return false;
                    }

                    for (int i = square.getObjects().size() - 1; i > 0; i--) {
                        if (square.getObjects().get(i) instanceof IsoObject object) {
                            if (object.isStairsObject()) {
                                return false;
                            }

                            if (object.isWall() && isWallBlockingObjectOnTile(dir, entry, x, y, object)) {
                                return false;
                            }
                        }
                    }
                }
            }

            return true;
        }
    }

    private static boolean isWallBlockingObjectOnTile(IsoDirections dir, BentFences.Entry entry, int x, int y, IsoObject object) {
        if (dir == IsoDirections.E) {
            return x != 0;
        } else if (dir == IsoDirections.S) {
            return y != 0;
        } else {
            if (dir == IsoDirections.W) {
                if (x + 1 == entry.collapsedSizeX) {
                    return !object.isWallW();
                }
            } else if (dir == IsoDirections.N && y + 1 == entry.collapsedSizeY) {
                return !object.isWallN();
            }

            return true;
        }
    }

    private void emptyContainerIfPresent(IsoObject object, IsoGridSquare square) {
        ItemContainer contents = object.getContainer();
        if (contents != null && !contents.isEmpty()) {
            for (int j = 0; j < contents.getItems().size(); j++) {
                InventoryItem item = contents.getItems().get(j);
                if (item != null) {
                    square.AddWorldInventoryItem(item, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                }
            }
        }
    }

    public void collapse(IsoObject obj, IsoDirections dir, BentFences.Entry entry, int index) {
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean north = entry.isNorth();
        int revX = dir == IsoDirections.W ? -1 : 1;
        int revY = dir == IsoDirections.N ? -1 : 1;
        int offXY = entry.collapsedOffset;
        int offsetX = obj.square.x + (north ? offXY - index : (dir == IsoDirections.W ? -1 : 0));
        int offsetY = obj.square.y + (north ? (dir == IsoDirections.N ? -1 : 0) : offXY - index);
        Iterator<String> collapsed = entry.collapsed.iterator();
        ArrayList<IsoMovingObject> tripZombies = new ArrayList<>();
        ArrayList<IsoMovingObject> crushZombies = new ArrayList<>();

        for (int x = 0; x < entry.collapsedSizeX; x++) {
            for (int y = 0; y < entry.collapsedSizeY; y++) {
                IsoGridSquare square = cell.getGridSquare(offsetX + x * revX, offsetY + y * revY, obj.square.z);
                if (square != null) {
                    if (north && y == 0 || !north && x == 0) {
                        int xOff = 0;
                        int yOff = 0;
                        switch (dir) {
                            case N:
                                yOff = 1;
                                break;
                            case S:
                                yOff = -1;
                                break;
                            case E:
                                xOff = -1;
                                break;
                            case W:
                                xOff = 1;
                        }

                        IsoGridSquare square2 = cell.getGridSquare(offsetX + xOff, offsetY + yOff, obj.square.z);
                        if (square2 != null) {
                            tripZombies.addAll(square2.getMovingObjects());
                        }
                    }

                    crushZombies.addAll(square.getMovingObjects());

                    for (int i = square.getObjects().size() - 1; i > 0; i--) {
                        if (BrokenFences.getInstance().isBreakableObject(square.getObjects().get(i))) {
                            BrokenFences.getInstance().destroyFence(square.getObjects().get(i), dir);
                        } else {
                            IsoObject object = Type.tryCastTo(square.getObjects().get(i), IsoObject.class);
                            if (object != null
                                && !object.isWall()
                                && !object.isFloor()
                                && object.sprite != null
                                && object.sprite.name != null
                                && this.fenceMap.get(object.sprite.name) == null
                                && !this.collapsedTiles.contains(object.sprite.name)) {
                                this.emptyContainerIfPresent(object, square);
                                square.transmitRemoveItemFromSquare(object);
                                BrokenFences.getInstance().addItems(object, square);
                            }
                        }
                    }

                    String spriteName = collapsed.next();
                    IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                    sprite.name = spriteName;
                    IsoObject collapsedObj = IsoObject.getNew(square, spriteName, null, false);
                    square.transmitAddObjectToSquare(collapsedObj, square.getObjects().size() + 1);
                    doSquareUpdateTasks(square);
                }
            }
        }

        for (int ix = 0; ix < tripZombies.size(); ix++) {
            if (tripZombies.get(ix) instanceof IsoZombie zombie && !zombie.isObjectBehind(obj)) {
                zombie.setOnFloor(true);
                zombie.setFallOnFront(true);
                zombie.setHitReaction("Floor");
            }
        }

        for (int ixx = 0; ixx < crushZombies.size(); ixx++) {
            if (crushZombies.get(ixx) instanceof IsoZombie zombie) {
                zombie.setOnFloor(true);
                zombie.setFallOnFront(zombie.isObjectBehind(obj));
                zombie.setHitReaction("Floor");
            } else {
                IsoPlayer player = Type.tryCastTo(crushZombies.get(ixx), IsoPlayer.class);
                if (player != null) {
                    player.setForceSprint(false);
                    player.setBumpType("left");
                    player.setVariable("BumpDone", false);
                    player.setVariable("BumpFall", true);
                    player.setBumpFallType(player.isObjectBehind(obj) ? "pushedBehind" : "pushedFront");
                    player.setVariable("TripObstacleType", 1.0F);
                }
            }
        }
    }

    public void removeCollapsedTiles(IsoObject obj, IsoDirections dir, BentFences.Entry entry, int index) {
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean north = entry.isNorth();
        int revX = dir == IsoDirections.W ? -1 : 1;
        int revY = dir == IsoDirections.N ? -1 : 1;
        int offXY = entry.collapsedOffset;
        int offsetX = obj.square.x + (north ? offXY - index : (dir == IsoDirections.W ? -1 : 0));
        int offsetY = obj.square.y + (north ? (dir == IsoDirections.N ? -1 : 0) : offXY - index);

        for (int x = 0; x < entry.collapsedSizeX; x++) {
            for (int y = 0; y < entry.collapsedSizeY; y++) {
                IsoGridSquare square = cell.getGridSquare(offsetX + x * revX, offsetY + y * revY, obj.square.z);
                if (square != null) {
                    for (int i = square.getObjects().size() - 1; i > 0; i--) {
                        IsoObject object = Type.tryCastTo(square.getObjects().get(i), IsoObject.class);
                        if (object != null && object.sprite != null && object.sprite.name != null && this.collapsedTiles.contains(object.sprite.name)) {
                            square.transmitRemoveItemFromSquare(object);
                        }

                        doSquareUpdateTasks(square);
                    }
                }
            }
        }
    }

    public void smashFence(IsoObject obj, IsoDirections dir) {
        this.smashFence(obj, dir, -1);
    }

    public void smashFence(IsoObject obj, IsoDirections dir, int index) {
        BentFences.Entry entry = this.getEntryForObject(obj);
        if (entry != null) {
            ArrayList<String> tiles = entry.stages.get(0);
            if (tiles != null) {
                int bendStage = this.getBendStage(obj, entry);
                int useIndex = index != -1 ? index : this.getTileIndex(tiles, obj, entry, true);
                if (bendStage != 0) {
                    this.resetFence(obj);
                }

                IsoCell cell = IsoWorld.instance.currentCell;

                for (int i = 1; i < tiles.size() - 1; i++) {
                    int x = obj.square.x + (entry.isNorth() ? i - useIndex : 0);
                    int y = obj.square.y + (entry.isNorth() ? 0 : i - useIndex);
                    IsoGridSquare square = cell.getGridSquare(x, y, obj.square.z);
                    if (square != null) {
                        IsoObject fenceObj = this.getObjectForEntry(square, tiles, i);
                        if (fenceObj == null) {
                            fenceObj = BrokenFences.getInstance().getBreakableObject(square, entry.isNorth());
                            if (fenceObj == null) {
                                continue;
                            }
                        }

                        String spriteName = tiles.get(i);
                        IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                        sprite.name = spriteName;
                        fenceObj.setSprite(sprite);
                        fenceObj.transmitUpdatedSprite();
                        if (spriteName != null && BrokenFences.getInstance().isBreakableSprite(spriteName)) {
                            BrokenFences.getInstance().destroyFence(fenceObj, dir);
                        } else {
                            BrokenFences.getInstance().addItems(fenceObj, square);
                            square.transmitRemoveItemFromSquare(fenceObj);
                            doSquareUpdateTasks(square);
                        }
                    }
                }
            }
        }
    }

    public void swapTiles(IsoObject obj, IsoDirections dir, boolean bending) {
        this.swapTiles(obj, dir, bending, -1);
    }

    public void swapTiles(IsoObject obj, IsoDirections dir, boolean bending, int forceStage) {
        BentFences.Entry entry = this.getEntryForObject(obj, dir);
        if (entry == null) {
            if (!bending) {
                return;
            }

            entry = this.getEntryForObject(obj);
            if (entry == null) {
                return;
            }
        }

        if (dir != null) {
            if (entry.isNorth() && dir != IsoDirections.N && dir != IsoDirections.S) {
                return;
            }

            if (!entry.isNorth() && dir != IsoDirections.W && dir != IsoDirections.E) {
                return;
            }
        }

        int bendStage = forceStage != -1 ? forceStage : this.getBendStage(obj, entry);
        if (bendStage != -1 && (bending || bendStage != 0)) {
            boolean doSmash = bending && bendStage > 0 && entry.dir != dir;
            boolean doCollapse = !doSmash && bending && bendStage + 1 >= entry.stages.size() - 1;
            boolean doSmashCollapse = doCollapse && !this.checkCanCollapse(obj, dir, entry) && entry.doSmash;
            boolean doRemoveCollapsedTiles = !bending && bendStage + 1 == entry.stages.size();
            boolean doDebris = doCollapse && !entry.debris.isEmpty();
            if (bendStage != entry.stages.size() || !bending) {
                ArrayList<String> tiles = entry.stages.get(bendStage);
                ArrayList<String> baseTiles = entry.stages.get(0);
                ArrayList<String> swapTiles = entry.stages.get(bendStage + (bending ? 1 : -1));
                if (tiles != null && swapTiles != null && baseTiles != null && tiles.size() == swapTiles.size()) {
                    IsoCell cell = IsoWorld.instance.currentCell;
                    int index = this.getTileIndex(tiles, obj, entry);
                    ArrayList<IsoGridSquare> squaresToUpdate = new ArrayList<>();

                    for (int i = 0; i < tiles.size(); i++) {
                        int x = obj.square.x + (entry.isNorth() ? i - index : 0);
                        int y = obj.square.y + (entry.isNorth() ? 0 : i - index);
                        IsoGridSquare square = cell.getGridSquare(x, y, obj.square.z);
                        if (square != null) {
                            IsoObject fenceObject = this.getObjectForEntry(square, tiles, i);
                            if (fenceObject != null) {
                                if (fenceObject.getProperties().has("CornerNorthWall")) {
                                    splitCorner(fenceObject, square, entry);
                                }

                                if (bending) {
                                    for (int j = square.getObjects().size() - 1; j > 0; j--) {
                                        IsoObject testObj = square.getObjects().get(j);
                                        if (testObj != null && testObj.sprite != null) {
                                            boolean isSign = false;
                                            if (testObj.sprite.getProperties().has(IsoFlagType.attachedN)) {
                                                isSign = true;
                                            }

                                            if (testObj.sprite.getProperties().has(IsoFlagType.attachedW)) {
                                                isSign = true;
                                            }

                                            if (testObj.sprite.getProperties().has(IsoFlagType.attachedS)) {
                                                isSign = true;
                                            }

                                            if (testObj.sprite.getProperties().has(IsoFlagType.attachedE)) {
                                                isSign = true;
                                            }

                                            if (isSign) {
                                                square.transmitRemoveItemFromSquare(testObj);
                                                BrokenFences.getInstance().addItems(testObj, square);
                                                square.invalidateRenderChunkLevel(384L);
                                            }
                                        }
                                    }
                                }

                                String spriteName = !doSmash && !doSmashCollapse ? swapTiles.get(i) : baseTiles.get(i);
                                IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                                sprite.name = spriteName;
                                fenceObject.setSprite(sprite);
                                fenceObject.transmitUpdatedSprite();
                                if (doDebris) {
                                    this.addDebrisObject(fenceObject, dir, entry);
                                }

                                squaresToUpdate.add(square);
                                fenceObject.setDamage((short)entry.health);
                            }
                        }
                    }

                    for (int ix = 0; ix < squaresToUpdate.size(); ix++) {
                        doSquareUpdateTasks(squaresToUpdate.get(ix));
                    }

                    if (dir != null) {
                        if (doSmash || doSmashCollapse) {
                            this.smashFence(obj, dir, index);
                        } else if (doCollapse) {
                            this.collapse(obj, dir, entry, index);
                        }
                    } else if (doRemoveCollapsedTiles) {
                        this.removeCollapsedTiles(obj, entry.dir, entry, index);
                    }
                }
            }
        }
    }

    private static void splitCorner(IsoObject obj, IsoGridSquare square, BentFences.Entry entry) {
        String cornerNorth = obj.getProperties().get("CornerNorthWall");
        String cornerWest = obj.getProperties().get("CornerWestWall");
        if (cornerNorth != null && cornerWest != null) {
            IsoSprite sprite = IsoSpriteManager.instance.getSprite(entry.isNorth() ? cornerNorth : cornerWest);
            sprite.name = entry.isNorth() ? cornerNorth : cornerWest;
            obj.setSprite(sprite);
            obj.transmitUpdatedSprite();
            IsoObject obj2 = IsoObject.getNew(square, entry.isNorth() ? cornerWest : cornerNorth, null, false);
            square.transmitAddObjectToSquare(obj2, square.getObjects().size() + 1);
            doSquareUpdateTasks(square);
        }
    }

    public void bendFence(IsoObject obj, IsoDirections dir) {
        BentFences.Entry entry = this.getEntryForObject(obj, dir);
        if (!this.isValidObject(obj, entry)) {
            switch (dir) {
                case N:
                    entry = this.getEntryForObject(obj, IsoDirections.W);
                    dir = IsoDirections.W;
                    break;
                case S:
                    entry = this.getEntryForObject(obj, IsoDirections.E);
                    dir = IsoDirections.E;
                    break;
                case E:
                    entry = this.getEntryForObject(obj, IsoDirections.S);
                    dir = IsoDirections.S;
                    break;
                case W:
                    entry = this.getEntryForObject(obj, IsoDirections.N);
                    dir = IsoDirections.N;
            }
        }

        if (entry != null) {
            int bendStageOld = this.getBendStage(obj, entry);
            String ThumpSound = obj.getProperties().get("ThumpSound");
            this.swapTiles(obj, dir, true);
            int bendStageNew = this.getBendStage(obj, this.getEntryForObject(obj));
            if (bendStageNew > bendStageOld) {
                String soundName = "BreakObject";
                if ("ZombieThumpChainlinkFence".equalsIgnoreCase(ThumpSound)) {
                    if (bendStageNew == 1) {
                        soundName = "ZombieThumpChainlinkFenceDamageMid";
                    } else if (bendStageNew < entry.stages.size() - 1) {
                        soundName = "ZombieThumpChainlinkFenceDamageHigh";
                    } else {
                        soundName = "ZombieThumpChainlinkFenceDamageCollapse";
                    }
                } else if ("ZombieThumpWood".equalsIgnoreCase(ThumpSound) && bendStageNew >= entry.stages.size()) {
                    soundName = "ZombieThumpWoodCollapse";
                }

                IsoGridSquare square = obj.getSquare();
                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer(soundName, false, square, 1.0F, 20.0F, 1.0F, true);
                } else {
                    SoundManager.instance.PlayWorldSound(soundName, square, 1.0F, 20.0F, 1.0F, true);
                }
            }
        }
    }

    public void unbendFence(IsoObject obj) {
        this.swapTiles(obj, null, false);
    }

    public void resetFence(IsoObject obj) {
        BentFences.Entry entry = this.getEntryForObject(obj);
        if (entry != null) {
            int bendStage = this.getBendStage(obj, entry);
            if (bendStage != -1) {
                ArrayList<String> tiles = entry.stages.get(bendStage);
                ArrayList<String> baseTiles = entry.stages.get(0);
                if (tiles != null && baseTiles != null && tiles.size() == baseTiles.size()) {
                    IsoCell cell = IsoWorld.instance.currentCell;
                    int index = this.getTileIndex(tiles, obj, entry);

                    for (int i = 0; i < tiles.size(); i++) {
                        int x = obj.square.x + (entry.isNorth() ? i - index : 0);
                        int y = obj.square.y + (entry.isNorth() ? 0 : i - index);
                        IsoGridSquare square = cell.getGridSquare(x, y, obj.square.z);
                        if (square != null) {
                            IsoObject fenceObject = this.getObjectForEntry(square, tiles, i);
                            if (fenceObject != null) {
                                if (fenceObject.getProperties().has("CornerNorthWall")) {
                                    splitCorner(fenceObject, square, entry);
                                }

                                String spriteName = baseTiles.get(i);
                                IsoSprite sprite = IsoSpriteManager.instance.getSprite(spriteName);
                                sprite.name = spriteName;
                                fenceObject.setSprite(sprite);
                                fenceObject.transmitUpdatedSprite();
                                doSquareUpdateTasks(square);
                                fenceObject.setDamage((short)entry.health);
                            }
                        }
                    }

                    if (bendStage + 1 == entry.stages.size()) {
                        this.removeCollapsedTiles(obj, entry.dir, entry, index);
                    }
                }
            }
        }
    }

    public boolean isBendableFence(IsoObject obj) {
        return this.isBentObject(obj) || this.isUnbentObject(obj);
    }

    public BentFences.ThumpData getThumpData(IsoObject obj) {
        BentFences.Entry entry = this.getEntryForObject(obj);
        return entry == null ? new BentFences.ThumpData() : this.getThumpData(obj, entry);
    }

    public BentFences.ThumpData getThumpData(IsoObject obj, BentFences.Entry entry) {
        BentFences.ThumpData thumpData = new BentFences.ThumpData();
        thumpData.health = entry.health;
        int n = 0;
        int s = 0;
        int e = 0;
        int w = 0;
        IsoCell cell = IsoWorld.instance.currentCell;
        int bendStage = this.getBendStage(obj, entry);
        ArrayList<String> tiles = entry.stages.get(bendStage);
        thumpData.bendStage = bendStage;
        thumpData.stages = entry.stages.size();
        if (!this.isEnabled()) {
            thumpData.thumpersToDamage = -1;
            thumpData.damageMultiplier = getFenceDamageMultiplier();
            return thumpData;
        } else {
            int thumpersToDamage = getThumpersRequired();
            if (bendStage > 0) {
                thumpData.directionBent = entry.dir;
                thumpersToDamage = PZMath.max(getThumpersRequired() / (bendStage + 1), 1);
            }

            thumpData.thumpersToDamage = thumpersToDamage;
            thumpData.damageMultiplier = getFenceDamageMultiplier();
            int index = this.getTileIndex(tiles, obj, entry);

            for (int i = 0; i < tiles.size(); i++) {
                int x = obj.square.x + (entry.isNorth() ? i - index : 0);
                int y = obj.square.y + (entry.isNorth() ? 0 : i - index);
                IsoGridSquare square = cell.getGridSquare(x, y, obj.square.z);
                if (square != null) {
                    if (entry.isNorth()) {
                        thumpData.totalThumpers = thumpData.totalThumpers + square.getZombieCount();
                        s += square.getZombieCount();
                        IsoGridSquare nSquare = cell.getGridSquare(x, y - 1, obj.square.z);
                        if (nSquare != null) {
                            n += nSquare.getZombieCount();
                            thumpData.totalThumpers = thumpData.totalThumpers + nSquare.getZombieCount();
                            if (nSquare.getN() != null) {
                                n += nSquare.getN().getZombieCount();
                                thumpData.totalThumpers = thumpData.totalThumpers + nSquare.getN().getZombieCount();
                            }
                        }

                        if (square.getS() != null) {
                            s += square.getS().getZombieCount();
                            thumpData.totalThumpers = thumpData.totalThumpers + square.getS().getZombieCount();
                        }

                        thumpData.directionToBend = n > s ? IsoDirections.S : IsoDirections.N;
                    } else {
                        thumpData.totalThumpers = thumpData.totalThumpers + square.getZombieCount();
                        e += square.getZombieCount();
                        IsoGridSquare wSquare = cell.getGridSquare(x - 1, y, obj.square.z);
                        if (wSquare != null) {
                            w += wSquare.getZombieCount();
                            thumpData.totalThumpers = thumpData.totalThumpers + wSquare.getZombieCount();
                            if (wSquare.getW() != null) {
                                w += wSquare.getW().getZombieCount();
                                thumpData.totalThumpers = thumpData.totalThumpers + wSquare.getW().getZombieCount();
                            }
                        }

                        if (square.getE() != null) {
                            e += square.getE().getZombieCount();
                            thumpData.totalThumpers = thumpData.totalThumpers + square.getE().getZombieCount();
                        }

                        thumpData.directionToBend = e > w ? IsoDirections.W : IsoDirections.E;
                    }
                }
            }

            return thumpData;
        }
    }

    public IsoObject getCollapsedFence(IsoGridSquare square) {
        if (square != null) {
            for (int i = 0; i < square.getObjects().size(); i++) {
                IsoObject obj = square.getObjects().get(i);
                if (obj != null && obj.sprite != null && obj.sprite.name != null && this.collapsedTiles.contains(obj.sprite.name)) {
                    return obj;
                }
            }
        }

        return null;
    }

    public void checkDamageHoppableFence(IsoMovingObject thumper, IsoGridSquare sq, IsoGridSquare oppositeSq) {
        if (this.isEnabled() && thumper instanceof IsoZombie) {
            IsoObject hoppable = sq.getHoppableTo(oppositeSq);
            if (hoppable != null && this.isBendableFence(hoppable)) {
                BentFences.ThumpData thumpData = this.getThumpData(hoppable);
                hoppable.setDamage((short)(hoppable.getDamage() - Rand.Next(0, 1) * thumpData.damageMultiplier * 0.1F));
                if (hoppable.getDamage() <= 0) {
                    getInstance().bendFence(hoppable, thumpData.directionBent);
                    hoppable.setDamage((short)thumpData.health);
                }
            }
        }
    }

    private static void doSquareUpdateTasks(IsoGridSquare square) {
        square.RecalcAllWithNeighbours(true);
        MapCollisionData.instance.squareChanged(square);
        PolygonalMap2.instance.squareChanged(square);
        IsoRegions.squareChanged(square);
        square.invalidateRenderChunkLevel(384L);
    }

    private void addDebrisObject(IsoObject obj, IsoDirections dir, BentFences.Entry entry) {
        if (entry != null && !obj.isWall() && !entry.debris.isEmpty()) {
            String spriteName = PZArrayUtil.pickRandom(entry.debris);
            IsoGridSquare square = obj.getSquare();
            if (square != null) {
                if (dir == IsoDirections.N || dir == IsoDirections.W) {
                    square = square.getAdjacentSquare(dir);
                }

                if (square != null && square.TreatAsSolidFloor()) {
                    IsoObject objNew = IsoObject.getNew(square, spriteName, null, false);
                    square.transmitAddObjectToSquare(objNew, square == obj.getSquare() ? obj.getObjectIndex() : -1);
                    BrokenFences.getInstance().addItems(obj, square);
                }
            }
        }
    }

    private static int getThumpersRequired() {
        return SandboxOptions.instance.lore.fenceThumpersRequired.getValue();
    }

    private static float getFenceDamageMultiplier() {
        return (float)SandboxOptions.instance.lore.fenceDamageMultiplier.getValue();
    }

    public boolean isEnabled() {
        return getThumpersRequired() >= 1;
    }

    public static void init() {
    }

    public void Reset() {
        this.entries.clear();
        this.fenceMap.clear();
    }

    public static final class Entry {
        IsoDirections dir = IsoDirections.Max;
        int health = 100;
        final HashMap<Integer, ArrayList<String>> stages = new HashMap<>();
        final ArrayList<String> collapsed = new ArrayList<>();
        final ArrayList<String> debris = new ArrayList<>();
        int length = -1;
        int collapsedOffset = -1;
        int collapsedSizeX = -1;
        int collapsedSizeY = -1;
        boolean doSmash = true;

        boolean isNorth() {
            return this.dir == IsoDirections.N || this.dir == IsoDirections.S;
        }
    }

    public static final class ThumpData {
        int totalThumpers;
        int bendStage;
        IsoDirections directionToBend = IsoDirections.Max;
        IsoDirections directionBent = IsoDirections.Max;
        int health = 100;
        int stages;
        int thumpersToDamage = 100;
        final float damageModifier = 0.1F;
        float damageMultiplier = 1.0F;
    }
}
