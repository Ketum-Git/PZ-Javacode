// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import zombie.ChunkMapFilenames;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.entity.Component;
import zombie.entity.GameEntityFactory;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.objects.IsoBarbecue;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFireplace;
import zombie.iso.objects.IsoJukebox;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoMannequin;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoTelevision;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWaveSignal;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;

public final class CellLoader {
    public static final ObjectCache<IsoObject> isoObjectCache = new ObjectCache<>();
    public static final ObjectCache<IsoTree> isoTreeCache = new ObjectCache<>();
    private static final HashSet<String> missingTiles = new HashSet<>();
    public static final HashMap<IsoSprite, IsoSprite> glassRemovedWindowSpriteMap = new HashMap<>();
    public static final HashMap<IsoSprite, IsoSprite> smashedWindowSpriteMap = new HashMap<>();

    public static void DoTileObjectCreation(IsoSprite spr, IsoObjectType type, IsoGridSquare sq, IsoCell cell, int x, int y, int height, String name) throws NumberFormatException {
        IsoObject obj = null;
        if (sq != null) {
            boolean bGlassRemovedWindow = false;
            boolean bSmashedWindow = false;
            if (glassRemovedWindowSpriteMap.containsKey(spr)) {
                spr = glassRemovedWindowSpriteMap.get(spr);
                type = spr.getType();
                bGlassRemovedWindow = true;
            } else if (smashedWindowSpriteMap.containsKey(spr)) {
                spr = smashedWindowSpriteMap.get(spr);
                type = spr.getType();
                bSmashedWindow = true;
            }

            PropertyContainer props = spr.getProperties();
            boolean isStove = spr.getProperties().propertyEquals("container", "stove")
                || spr.getProperties().propertyEquals("container", "toaster")
                || spr.getProperties().propertyEquals("container", "coffeemaker");
            if (spr.solidfloor && props.has(IsoFlagType.diamondFloor) && !props.has(IsoFlagType.transparentFloor)) {
                IsoObject floor = sq.getFloor();
                if (floor != null && floor.getProperties().has(IsoFlagType.diamondFloor)) {
                    if (!floor.hasComponents()) {
                        floor.clearAttachedAnimSprite();
                        floor.setSprite(spr);
                        return;
                    }

                    if (floor.isAddedToEngine()) {
                        throw new IllegalStateException("entity was added to engine");
                    }

                    for (int i = floor.componentSize() - 1; i >= 0; i--) {
                        Component component = floor.getComponentForIndex(i);
                        GameEntityFactory.RemoveComponent(floor, component);
                    }

                    floor.removeFromSquare();
                }
            }

            if (type == IsoObjectType.doorW || type == IsoObjectType.doorN) {
                IsoDoor door = new IsoDoor(cell, sq, spr, type == IsoObjectType.doorN);
                obj = door;
                AddSpecialObject(sq, door);
                if (spr.getProperties().has("DoubleDoor")) {
                    door.locked = false;
                    door.lockedByKey = false;
                }

                if (spr.getProperties().has("GarageDoor")) {
                    door.locked = !door.IsOpen();
                    door.lockedByKey = false;
                }
            } else if (type == IsoObjectType.lightswitch) {
                obj = new IsoLightSwitch(cell, sq, spr, sq.getRoomID());
                AddObject(sq, obj);
                if (obj.sprite.getProperties().has("lightR")) {
                    float r = Float.parseFloat(obj.sprite.getProperties().get("lightR")) / 255.0F;
                    float g = Float.parseFloat(obj.sprite.getProperties().get("lightG")) / 255.0F;
                    float b = Float.parseFloat(obj.sprite.getProperties().get("lightB")) / 255.0F;
                    int radius = 10;
                    if (obj.sprite.getProperties().has("LightRadius") && Integer.parseInt(obj.sprite.getProperties().get("LightRadius")) > 0) {
                        radius = Integer.parseInt(obj.sprite.getProperties().get("LightRadius"));
                    }

                    IsoLightSource l = new IsoLightSource(obj.square.getX(), obj.square.getY(), obj.square.getZ(), r, g, b, radius);
                    l.active = true;
                    l.hydroPowered = true;
                    l.switches.add((IsoLightSwitch)obj);
                    ((IsoLightSwitch)obj).lights.add(l);
                } else {
                    ((IsoLightSwitch)obj).lightRoom = true;
                }
            } else if (type != IsoObjectType.curtainN && type != IsoObjectType.curtainS && type != IsoObjectType.curtainE && type != IsoObjectType.curtainW) {
                if (spr.getProperties().has(IsoFlagType.windowW) || spr.getProperties().has(IsoFlagType.windowN)) {
                    obj = new IsoWindow(cell, sq, spr, spr.getProperties().has(IsoFlagType.windowN));
                    if (bGlassRemovedWindow) {
                        ((IsoWindow)obj).setSmashed(true);
                        ((IsoWindow)obj).setGlassRemoved(true);
                    } else if (bSmashedWindow) {
                        ((IsoWindow)obj).setSmashed(true);
                    }

                    AddSpecialObject(sq, obj);
                } else if (!spr.getProperties().has(IsoFlagType.WindowW) && !spr.getProperties().has(IsoFlagType.WindowN)) {
                    if (!spr.getProperties().has(IsoFlagType.container)
                        || !spr.getProperties().get("container").equals("barbecue") && !spr.getProperties().get("container").equals("barbecuepropane")) {
                        if (spr.getProperties().has(IsoFlagType.container) && spr.getProperties().get("container").equals("fireplace")) {
                            obj = new IsoFireplace(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if (!spr.getName().equals("camping_01_04")
                            && !spr.getName().equals("camping_01_05")
                            && !spr.getName().equals("camping_01_06")
                            && spr.getProperties().has(IsoFlagType.container)
                            && spr.getProperties().get("container").equals("campfire")) {
                            obj = new IsoFireplace(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if ("brazier".equals(spr.getProperties().get("container"))) {
                            obj = new IsoFireplace(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if ("IsoCombinationWasherDryer".equals(spr.getProperties().get("IsoType"))) {
                            obj = new IsoCombinationWasherDryer(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if (spr.getProperties().has(IsoFlagType.container) && spr.getProperties().get("container").equals("clothingdryer")) {
                            obj = new IsoClothingDryer(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if (spr.getProperties().has(IsoFlagType.container) && spr.getProperties().get("container").equals("clothingwasher")) {
                            obj = new IsoClothingWasher(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if (spr.getProperties().has(IsoFlagType.container) && spr.getProperties().get("container").equals("woodstove")) {
                            obj = new IsoFireplace(cell, sq, spr);
                            AddObject(sq, obj);
                        } else if (!spr.getProperties().has(IsoFlagType.container) || !isStove && !spr.getProperties().get("container").equals("microwave")) {
                            if (type == IsoObjectType.jukebox) {
                                obj = new IsoJukebox(cell, sq, spr);
                                obj.outlineOnMouseover = true;
                                AddObject(sq, obj);
                            } else if (type == IsoObjectType.radio) {
                                obj = new IsoRadio(cell, sq, spr);
                                AddObject(sq, obj);
                            } else if (spr.getProperties().has("signal")) {
                                String signaltype = spr.getProperties().get("signal");
                                if ("radio".equals(signaltype)) {
                                    obj = new IsoRadio(cell, sq, spr);
                                } else if ("tv".equals(signaltype)) {
                                    obj = new IsoTelevision(cell, sq, spr);
                                }

                                AddObject(sq, obj);
                            } else {
                                if (spr.getProperties().has(IsoFlagType.WallOverlay)) {
                                    IsoObject obj2 = null;
                                    if (spr.getProperties().has(IsoFlagType.attachedSE)) {
                                        obj2 = sq.getWallSE();
                                    } else if (spr.getProperties().has(IsoFlagType.attachedW)) {
                                        obj2 = sq.getWall(false);
                                        if (obj2 == null) {
                                            obj2 = sq.getWindow(false);
                                            if (obj2 != null && (obj2.getProperties() == null || !obj2.getProperties().has(IsoFlagType.WindowW))) {
                                                obj2 = null;
                                            }
                                        }

                                        if (obj2 == null) {
                                            obj2 = sq.getGarageDoor(false);
                                        }
                                    } else if (spr.getProperties().has(IsoFlagType.attachedN)) {
                                        obj2 = sq.getWall(true);
                                        if (obj2 == null) {
                                            obj2 = sq.getWindow(true);
                                            if (obj2 != null && (obj2.getProperties() == null || !obj2.getProperties().has(IsoFlagType.WindowN))) {
                                                obj2 = null;
                                            }
                                        }

                                        if (obj2 == null) {
                                            obj2 = sq.getGarageDoor(true);
                                        }
                                    } else {
                                        for (int n = sq.getObjects().size() - 1; n >= 0; n--) {
                                            IsoObject obj3 = sq.getObjects().get(n);
                                            if (obj3.sprite.getProperties().has(IsoFlagType.cutW) || obj3.sprite.getProperties().has(IsoFlagType.cutN)) {
                                                obj2 = obj3;
                                                break;
                                            }
                                        }
                                    }

                                    if (obj2 != null) {
                                        if (obj2.attachedAnimSprite == null) {
                                            obj2.attachedAnimSprite = new ArrayList<>(4);
                                        }

                                        obj2.attachedAnimSprite.add(IsoSpriteInstance.get(spr));
                                    } else {
                                        obj = IsoObject.getNew();
                                        obj.sx = 0.0F;
                                        obj.sprite = spr;
                                        obj.square = sq;
                                        AddObject(sq, obj);
                                    }

                                    return;
                                }

                                if (spr.getProperties().has(IsoFlagType.FloorOverlay)) {
                                    IsoObject obj2x = sq.getFloor();
                                    if (obj2x != null) {
                                        if (obj2x.attachedAnimSprite == null) {
                                            obj2x.attachedAnimSprite = new ArrayList<>(4);
                                        }

                                        obj2x.attachedAnimSprite.add(IsoSpriteInstance.get(spr));
                                    }
                                } else if (IsoMannequin.isMannequinSprite(spr)) {
                                    obj = new IsoMannequin(cell, sq, spr);
                                    AddObject(sq, obj);
                                } else if (type == IsoObjectType.tree) {
                                    if (spr.getName() != null && spr.getName().startsWith("vegetation_trees")) {
                                        IsoObject floor = sq.getFloor();
                                        if (floor == null
                                            || floor.getSprite() == null
                                            || floor.getSprite().getName() == null
                                            || !floor.getSprite().getName().startsWith("blends_natural")) {
                                            DebugLog.General
                                                .error("removed tree at " + sq.x + "," + sq.y + "," + sq.z + " because floor is not blends_natural");
                                            return;
                                        }
                                    }

                                    obj = IsoTree.getNew();
                                    obj.sprite = spr;
                                    obj.square = sq;
                                    obj.sx = 0.0F;
                                    ((IsoTree)obj).initTree();

                                    for (int i = 0; i < sq.getObjects().size(); i++) {
                                        IsoObject obj2x = sq.getObjects().get(i);
                                        if (obj2x instanceof IsoTree isoTree) {
                                            sq.getObjects().remove(i);
                                            obj2x.reset();
                                            isoTreeCache.push(isoTree);
                                            break;
                                        }
                                    }

                                    AddObject(sq, obj);
                                } else {
                                    if (spr.hasNoTextures() && !spr.invisible && !GameServer.server) {
                                        if (!missingTiles.contains(name)) {
                                            if (Core.debug) {
                                                DebugLog.General.error("CellLoader> missing tile " + name);
                                            }

                                            missingTiles.add(name);
                                        }

                                        spr.LoadSingleTexture(Core.debug ? "media/ui/missing-tile-debug.png" : "media/ui/missing-tile.png");
                                        if (spr.hasNoTextures()) {
                                            return;
                                        }
                                    }

                                    obj = IsoObject.getNew();
                                    obj.sx = 0.0F;
                                    obj.sprite = spr;
                                    obj.square = sq;
                                    AddObject(sq, obj);
                                }
                            }
                        } else {
                            obj = new IsoStove(cell, sq, spr);
                            AddObject(sq, obj);
                        }
                    } else {
                        obj = new IsoBarbecue(cell, sq, spr);
                        AddObject(sq, obj);
                    }
                } else {
                    obj = new IsoWindowFrame(cell, sq, spr, spr.getProperties().has(IsoFlagType.WindowN));
                    AddObject(sq, obj);
                }
            } else {
                boolean closed = Integer.parseInt(name.substring(name.lastIndexOf("_") + 1)) % 8 <= 3;
                obj = new IsoCurtain(cell, sq, spr, type == IsoObjectType.curtainN || type == IsoObjectType.curtainS, closed);
                AddSpecialObject(sq, obj);
            }

            if (obj != null) {
                obj.setTile(name);
                obj.createContainersFromSpriteProperties();
                obj.createFluidContainersFromSpriteProperties();
                if (obj.sprite.getProperties().has(IsoFlagType.vegitation)) {
                    obj.tintr = 0.7F + Rand.Next(30) / 100.0F;
                    obj.tintg = 0.7F + Rand.Next(30) / 100.0F;
                    obj.tintb = 0.7F + Rand.Next(30) / 100.0F;
                }

                if (sq.shouldNotSpawnActivatedRadiosOrTvs() && obj instanceof IsoWaveSignal isoWaveSignal && isoWaveSignal.getDeviceData().getIsTurnedOn()) {
                    isoWaveSignal.getDeviceData().setTurnedOnRaw(false);
                }
            }
        }
    }

    public static boolean LoadCellBinaryChunk(IsoCell cell, int wx, int wy, IsoChunk chunk) {
        int cellX = PZMath.fastfloor(wx / 32.0F);
        int cellY = PZMath.fastfloor(wy / 32.0F);
        String filenamepack = "world_" + cellX + "_" + cellY + ".lotpack";
        if (!IsoLot.InfoFileNames.containsKey(filenamepack)) {
            return false;
        } else {
            File fo = new File(IsoLot.InfoFileNames.get(filenamepack));
            if (fo.exists()) {
                IsoLot lot = null;

                try {
                    LotHeader lotHeader = IsoLot.InfoHeaders.get(ChunkMapFilenames.instance.getHeader(cellX, cellY));
                    lot = IsoLot.get(lotHeader.mapFiles, cellX, cellY, wx, wy, chunk);
                    if (lot.info == null) {
                        return true;
                    }

                    boolean[] bDoneSquares = new boolean[64];
                    int nonEmptySquareCount = cell.PlaceLot(lot, 0, 0, lot.minLevel, chunk, wx, wy, bDoneSquares);
                    if (nonEmptySquareCount > 0) {
                        chunk.addModded(lotHeader.mapFiles.infoFileModded.get(filenamepack));
                    }

                    if (nonEmptySquareCount == 64) {
                        return true;
                    }

                    for (int i = lot.info.mapFiles.priority + 1; i < IsoLot.MapFiles.size(); i++) {
                        MapFiles mapFiles = IsoLot.MapFiles.get(i);
                        if (mapFiles.hasCell(cellX, cellY)) {
                            IsoLot lot2 = null;

                            try {
                                lot2 = IsoLot.get(mapFiles, cellX, cellY, wx, wy, chunk);
                                nonEmptySquareCount = cell.PlaceLot(lot2, 0, 0, lot2.minLevel, chunk, wx, wy, bDoneSquares);
                                if (nonEmptySquareCount > 0) {
                                    chunk.addModded(mapFiles.infoFileModded.get(filenamepack));
                                }

                                if (nonEmptySquareCount == 64) {
                                    break;
                                }
                            } finally {
                                if (lot2 != null) {
                                    IsoLot.put(lot2);
                                }
                            }
                        }
                    }
                } finally {
                    if (lot != null) {
                        IsoLot.put(lot);
                    }
                }

                return true;
            } else {
                return false;
            }
        }
    }

    public static IsoCell LoadCellBinaryChunk(IsoSpriteManager spr, int wx, int wy) throws IOException {
        IsoCell cell = new IsoCell(256, 256);
        int nmax = IsoPlayer.numPlayers;
        int var12 = 1;
        if (!GameServer.server) {
            if (GameClient.client) {
                WorldStreamer.instance.requestLargeAreaZip(wx, wy, IsoChunkMap.chunkGridWidth / 2 + 2);
                IsoChunk.doServerRequests = false;
            }

            for (int n = 0; n < var12; n++) {
                cell.chunkMap[n].setInitialPos(wx, wy);
                IsoPlayer.assumedPlayer = n;
                int minwx = wx - IsoChunkMap.chunkGridWidth / 2;
                int minwy = wy - IsoChunkMap.chunkGridWidth / 2;
                int maxwx = wx + IsoChunkMap.chunkGridWidth / 2 + 1;
                int maxwy = wy + IsoChunkMap.chunkGridWidth / 2 + 1;

                for (int x = minwx; x < maxwx; x++) {
                    for (int y = minwy; y < maxwy; y++) {
                        if (IsoWorld.instance.getMetaGrid().isValidChunk(x, y)) {
                            cell.chunkMap[n].LoadChunk(x, y, x - minwx, y - minwy);
                        }
                    }
                }
            }
        }

        IsoPlayer.assumedPlayer = 0;
        LuaEventManager.triggerEvent("OnPostMapLoad", cell, wx, wy);
        ConnectMultitileObjects(cell);
        return cell;
    }

    private static void RecurseMultitileObjects(IsoCell cell, IsoGridSquare from, IsoGridSquare sq, ArrayList<IsoPushableObject> list) {
        IsoPushableObject foundpush = null;
        boolean bFoundOnX = false;

        for (IsoMovingObject obj : sq.getMovingObjects()) {
            if (obj instanceof IsoPushableObject push) {
                int difX = from.getX() - sq.getX();
                int difY = from.getY() - sq.getY();
                if (difY != 0 && obj.sprite.getProperties().has("connectY")) {
                    int offY = Integer.parseInt(obj.sprite.getProperties().get("connectY"));
                    if (offY == difY) {
                        push.connectList = list;
                        list.add(push);
                        foundpush = push;
                        break;
                    }
                }

                if (difX != 0 && obj.sprite.getProperties().has("connectX")) {
                    int offX = Integer.parseInt(obj.sprite.getProperties().get("connectX"));
                    if (offX == difX) {
                        push.connectList = list;
                        list.add(push);
                        foundpush = push;
                        bFoundOnX = true;
                        break;
                    }
                }
            }
        }

        if (foundpush != null) {
            if (foundpush.sprite.getProperties().has("connectY") && bFoundOnX) {
                int offY = Integer.parseInt(foundpush.sprite.getProperties().get("connectY"));
                IsoGridSquare other = cell.getGridSquare(
                    foundpush.getCurrentSquare().getX(), foundpush.getCurrentSquare().getY() + offY, foundpush.getCurrentSquare().getZ()
                );
                RecurseMultitileObjects(cell, foundpush.getCurrentSquare(), other, foundpush.connectList);
            }

            if (foundpush.sprite.getProperties().has("connectX") && !bFoundOnX) {
                int offX = Integer.parseInt(foundpush.sprite.getProperties().get("connectX"));
                IsoGridSquare other = cell.getGridSquare(
                    foundpush.getCurrentSquare().getX() + offX, foundpush.getCurrentSquare().getY(), foundpush.getCurrentSquare().getZ()
                );
                RecurseMultitileObjects(cell, foundpush.getCurrentSquare(), other, foundpush.connectList);
            }
        }
    }

    private static void ConnectMultitileObjects(IsoCell cell) {
        for (IsoMovingObject obj : cell.getObjectList()) {
            if (obj instanceof IsoPushableObject push
                && (obj.sprite.getProperties().has("connectY") || obj.sprite.getProperties().has("connectX"))
                && push.connectList == null) {
                push.connectList = new ArrayList<>();
                push.connectList.add(push);
                if (obj.sprite.getProperties().has("connectY")) {
                    int offY = Integer.parseInt(obj.sprite.getProperties().get("connectY"));
                    IsoGridSquare other = cell.getGridSquare(obj.getCurrentSquare().getX(), obj.getCurrentSquare().getY() + offY, obj.getCurrentSquare().getZ());
                    RecurseMultitileObjects(cell, push.getCurrentSquare(), other, push.connectList);
                }

                if (obj.sprite.getProperties().has("connectX")) {
                    int offX = Integer.parseInt(obj.sprite.getProperties().get("connectX"));
                    IsoGridSquare other = cell.getGridSquare(obj.getCurrentSquare().getX() + offX, obj.getCurrentSquare().getY(), obj.getCurrentSquare().getZ());
                    RecurseMultitileObjects(cell, push.getCurrentSquare(), other, push.connectList);
                }
            }
        }
    }

    private static void AddObject(IsoGridSquare square, IsoObject obj) {
        GameEntityFactory.CreateIsoEntityFromCellLoading(obj);
        int index = square.placeWallAndDoorCheck(obj, square.getObjects().size());
        if (index != square.getObjects().size() && index >= 0 && index <= square.getObjects().size()) {
            square.getObjects().add(index, obj);
        } else {
            square.getObjects().add(obj);
        }
    }

    private static void AddSpecialObject(IsoGridSquare square, IsoObject obj) {
        GameEntityFactory.CreateIsoEntityFromCellLoading(obj);
        int index = square.placeWallAndDoorCheck(obj, square.getObjects().size());
        if (index != square.getObjects().size() && index >= 0 && index <= square.getObjects().size()) {
            square.getObjects().add(index, obj);
        } else {
            square.getObjects().add(obj);
            square.getSpecialObjects().add(obj);
        }
    }
}
