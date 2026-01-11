// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.Position3D;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoHutch;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.ItemTag;
import zombie.util.StringUtils;

@UsedFromLua
public final class DesignationZoneAnimal extends DesignationZone {
    public static final ArrayList<DesignationZoneAnimal> designationAnimalZoneList = new ArrayList<>();
    public static final String ZONE_TYPE = "AnimalZone";
    public static final float ZONE_COLOR_R = 0.2F;
    public static final float ZONE_COLOR_G = 0.2F;
    public static final float ZONE_COLOR_B = 0.9F;
    @UsedFromLua
    public static final float ZONE_SELECTED_COLOR_R = 0.2F;
    @UsedFromLua
    public static final float ZONE_SELECTED_COLOR_G = 0.8F;
    @UsedFromLua
    public static final float ZONE_SELECTED_COLOR_B = 0.9F;
    private final ArrayList<IsoAnimal> animals = new ArrayList<>();
    private final ArrayList<IsoDeadBody> corpses = new ArrayList<>();
    public final ArrayList<IsoFeedingTrough> troughs = new ArrayList<>();
    public final ArrayList<IsoHutch> hutchs = new ArrayList<>();
    public final ArrayList<IsoWorldInventoryObject> foodOnGround = new ArrayList<>();
    public final ArrayList<IsoGridSquare> nearWaterSquares = new ArrayList<>();
    private int nbOfDung;
    private int nbOfFeather;
    private final ArrayList<Position3D> roofAreas = new ArrayList<>();
    private final ObjectPool<Position3D> position3dPool = new ObjectPool<>(Position3D::new);
    public static final String FENCE_WEST = "fencing_01_20";
    public static final String FENCE_NORTH = "fencing_01_21";
    public static final String FENCE_NORTHCORNER = "fencing_01_18";

    public DesignationZoneAnimal(String name, int x, int y, int z, int x2, int y2, boolean doSync) {
        super("AnimalZone", name, x, y, z, x2, y2, doSync);
        designationAnimalZoneList.add(this);
        this.check();
    }

    public static ArrayList<DesignationZoneAnimal> getAllDZones(
        ArrayList<DesignationZoneAnimal> currentList, DesignationZoneAnimal zoneToCheck, DesignationZoneAnimal previousZone
    ) {
        ArrayList<DesignationZoneAnimal> result = currentList;
        if (currentList == null) {
            result = new ArrayList<>();
        }

        ArrayList<DesignationZoneAnimal> newConnected = new ArrayList<>();
        DesignationZoneAnimal zone = zoneToCheck;
        if (zoneToCheck == null) {
            return result;
        } else {
            if (!result.contains(zoneToCheck)) {
                result.add(zoneToCheck);
            }

            for (int x = zoneToCheck.x; x < zone.x + zone.w; x++) {
                DesignationZoneAnimal cZone = getZone(x, zone.y - 1, zone.z);
                if (cZone != null && !result.contains(cZone) && cZone != previousZone) {
                    result.add(cZone);
                    newConnected.add(cZone);
                }

                cZone = getZone(x, zone.y + zone.h, zone.z);
                if (cZone != null && !result.contains(cZone) && cZone != previousZone) {
                    result.add(cZone);
                    newConnected.add(cZone);
                }
            }

            for (int y = zone.y; y < zone.y + zone.h; y++) {
                DesignationZoneAnimal cZonex = getZone(zone.x - 1, y, zone.z);
                if (cZonex != null && !result.contains(cZonex) && cZonex != previousZone) {
                    result.add(cZonex);
                    newConnected.add(cZonex);
                }

                cZonex = getZone(zone.x + zone.w, y, zone.z);
                if (cZonex != null && !result.contains(cZonex) && cZonex != previousZone) {
                    result.add(cZonex);
                    newConnected.add(cZonex);
                }
            }

            for (int i = 0; i < newConnected.size(); i++) {
                if (newConnected.get(i) != zoneToCheck) {
                    result = getAllDZones(result, newConnected.get(i), zone);
                }
            }

            newConnected.clear();
            return result;
        }
    }

    public void createSurroundingFence() {
    }

    public static boolean isItemFood(IsoWorldInventoryObject item) {
        InventoryItem checkItem = item.getItem();
        if (checkItem instanceof Food) {
            return true;
        } else {
            return !StringUtils.isNullOrEmpty(checkItem.getAnimalFeedType()) && checkItem instanceof DrainableComboItem
                ? true
                : checkItem.isPureWater(true) && checkItem.getFluidContainerFromSelfOrWorldItem().getRainCatcher() > 0.0F;
        }
    }

    public static boolean isItemDung(IsoWorldInventoryObject item) {
        InventoryItem checkItem = item.getItem();
        return checkItem.getScriptItem().isDung;
    }

    public static boolean isItemFeather(IsoWorldInventoryObject item) {
        InventoryItem checkItem = item.getItem();
        return checkItem.hasTag(ItemTag.FEATHER);
    }

    public static void addItemOnGround(IsoWorldInventoryObject item, IsoGridSquare sq) {
        DesignationZoneAnimal zone = getZone(sq.getX(), sq.getY(), sq.getZ());
        if (zone != null) {
            if (isItemFood(item)) {
                zone.addFoodOnGround(item);
            }

            if (isItemDung(item)) {
                zone.nbOfDung++;
            }

            if (isItemFeather(item)) {
                zone.nbOfFeather++;
            }
        }
    }

    public void addFoodOnGround(IsoWorldInventoryObject item) {
        if (!this.foodOnGround.contains(item)) {
            this.foodOnGround.add(item);
        }
    }

    @Override
    public void check() {
        if (this.isFullyStreamed()) {
            if (IsoWorld.instance.currentCell == null) {
                lastUpdate = 0L;
            } else {
                for (int i = this.animals.size() - 1; i >= 0; i--) {
                    IsoAnimal animal = this.animals.get(i);
                    animal.setDZone(null);
                }

                this.nearWaterSquares.clear();
                this.animals.clear();
                this.corpses.clear();
                this.hutchs.clear();
                this.foodOnGround.clear();
                this.nbOfDung = 0;
                this.nbOfFeather = 0;
                this.position3dPool.releaseAll(this.roofAreas);
                this.roofAreas.clear();
                ArrayList<DesignationZoneAnimal> connectedDZone = new ArrayList<>();
                getAllDZones(connectedDZone, this, null);
                ArrayList<IsoAnimal> animalsOnSquare = new ArrayList<>();
                IsoCell cell = IsoWorld.instance.currentCell;

                for (int checkX = this.x; checkX < this.x + this.w; checkX++) {
                    for (int checkY = this.y; checkY < this.y + this.h; checkY++) {
                        IsoGridSquare sq = cell.getGridSquare(checkX, checkY, this.z);
                        if (sq != null) {
                            if (sq.haveRoof) {
                                this.roofAreas.add(this.position3dPool.alloc().set(checkX, checkY, this.z));
                            }

                            sq.getAnimals(animalsOnSquare);

                            for (int i = 0; i < animalsOnSquare.size(); i++) {
                                IsoAnimal animal = animalsOnSquare.get(i);
                                animal.setDZone(this);
                                animal.getConnectedDZone().clear();
                                animal.getConnectedDZone().addAll(connectedDZone);
                            }

                            for (int i = 0; i < sq.getObjects().size(); i++) {
                                IsoObject obj = sq.getObjects().get(i);
                                if (obj instanceof IsoWorldInventoryObject worldObj) {
                                    if (isItemFood(worldObj)) {
                                        this.addFoodOnGround(worldObj);
                                    }

                                    if (isItemDung(worldObj)) {
                                        this.nbOfDung++;
                                    }

                                    if (isItemFeather(worldObj)) {
                                        this.nbOfFeather++;
                                    }
                                }

                                if (obj instanceof IsoFeedingTrough trough && trough.getLinkedY() == 0 && !this.troughs.contains(trough)) {
                                    this.troughs.add(trough);
                                }

                                if (obj instanceof IsoHutch hutch && !hutch.isSlave() && !this.hutchs.contains(hutch)) {
                                    this.hutchs.add(hutch);
                                    hutch.reforceUpdate();
                                }

                                if (obj.getProperties() != null && obj.getProperties().has(IsoFlagType.water)) {
                                    for (int x2 = sq.getX() - 1; x2 < sq.getX() + 2; x2++) {
                                        for (int y2 = sq.getY() - 1; y2 < sq.getY() + 2; y2++) {
                                            IsoGridSquare sq2 = cell.getGridSquare(x2, y2, sq.z);
                                            if (sq2 != null
                                                && sq2.isFree(false)
                                                && !this.nearWaterSquares.contains(sq2)
                                                && getZone(sq2.getX(), sq2.getY(), sq2.getZ()) == this) {
                                                this.nearWaterSquares.add(sq2);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int i = 0; i < sq.getStaticMovingObjects().size(); i++) {
                                IsoMovingObject objx = sq.getStaticMovingObjects().get(i);
                                if (objx instanceof IsoDeadBody corpse && corpse.isAnimal()) {
                                    this.corpses.add(corpse);
                                }
                            }
                        }
                    }
                }

                animalsOnSquare.clear();
                this.reAttachAnimal();
            }
        }
    }

    private void reAttachAnimal() {
        if (!this.troughs.isEmpty()) {
            for (int i = 0; i < this.troughs.size(); i++) {
                this.troughs.get(i).linkedAnimals.clear();

                for (int j = 0; j < this.animals.size(); j++) {
                    this.troughs.get(i).addLinkedAnimal(this.animals.get(j));
                }
            }
        }

        if (!this.hutchs.isEmpty()) {
            for (int i = 0; i < this.hutchs.size(); i++) {
                this.hutchs.get(i).animalOutside.clear();

                for (int j = 0; j < this.animals.size(); j++) {
                    this.hutchs.get(i).animalOutside.add(this.animals.get(j));
                }
            }
        }
    }

    @Override
    public void doMeta(int hours) {
        this.check();

        for (int i = 0; i < this.animals.size(); i++) {
            IsoAnimal animal = this.animals.get(i);
            if (!animal.isBaby()) {
                this.animals.get(i).updateStatsAway(hours);
            }
        }

        for (int ix = 0; ix < this.animals.size(); ix++) {
            IsoAnimal animal = this.animals.get(ix);
            if (animal.isBaby()) {
                this.animals.get(ix).updateStatsAway(hours);
            }
        }

        for (int ixx = 0; ixx < this.hutchs.size(); ixx++) {
            this.hutchs.get(ixx).doMeta(hours);
        }

        for (int ixx = 0; ixx < this.animals.size(); ixx++) {
            IsoAnimal animal = this.animals.get(ixx);
            animal.forceWanderNow();
        }
    }

    public static String getType() {
        return "AnimalZone";
    }

    public static ArrayList<DesignationZoneAnimal> getAllZones() {
        return designationAnimalZoneList;
    }

    public static DesignationZoneAnimal getZone(int x, int y, int z) {
        for (int i = 0; i < designationAnimalZoneList.size(); i++) {
            DesignationZoneAnimal zone = designationAnimalZoneList.get(i);
            if (x >= zone.x && x < zone.x + zone.w && y >= zone.y && y < zone.y + zone.h && zone.z == z) {
                return zone;
            }
        }

        return null;
    }

    public static DesignationZoneAnimal getZoneById(double zoneID) {
        for (int i = 0; i < designationAnimalZoneList.size(); i++) {
            DesignationZoneAnimal zone = designationAnimalZoneList.get(i);
            if (zone.getId().equals(zoneID)) {
                return zone;
            }
        }

        return null;
    }

    public static DesignationZoneAnimal getZoneF(float x, float y, float z) {
        return getZone(PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
    }

    public static DesignationZoneAnimal getZone(int x, int y) {
        for (int i = 0; i < designationAnimalZoneList.size(); i++) {
            DesignationZoneAnimal zone = designationAnimalZoneList.get(i);
            if (x >= zone.x && x < zone.x + zone.w && y >= zone.y && y < zone.y + zone.h) {
                return zone;
            }
        }

        return null;
    }

    public static void removeZone(DesignationZoneAnimal zone, boolean doSync) {
        ArrayList<DesignationZoneAnimal> connectedZones = getAllDZones(null, zone, null);

        for (int i = 0; i < connectedZones.size(); i++) {
            designationAnimalZoneList.remove(connectedZones.get(i));
            allZones.remove(connectedZones.get(i));
        }

        if (doSync) {
            zone.sync();
        }
    }

    public static void removeItemFromGround(IsoWorldInventoryObject item) {
        DesignationZoneAnimal zone = getZone(item.getSquare().getX(), item.getSquare().getY(), item.getSquare().getZ());
        if (zone != null) {
            zone.foodOnGround.remove(item);
        }
    }

    public void addAnimal(IsoAnimal animal) {
        if (!this.animals.contains(animal)) {
            this.animals.add(animal);
        }
    }

    public void removeAnimal(IsoAnimal animal) {
        this.animals.remove(animal);
    }

    public void addCorpse(IsoDeadBody corpse) {
        if (corpse.isAnimal() && !this.corpses.contains(corpse)) {
            this.corpses.add(corpse);
        }
    }

    public void removeCorpse(IsoDeadBody corpse) {
        if (corpse.isAnimal()) {
            this.corpses.remove(corpse);
        }
    }

    public ArrayList<IsoAnimal> getAnimals() {
        return this.animals;
    }

    public ArrayList<IsoDeadBody> getCorpses() {
        for (int i = this.corpses.size() - 1; i >= 0; i--) {
            if (this.corpses.get(i).getStaticMovingObjectIndex() == -1) {
                this.corpses.remove(i);
            }
        }

        return this.corpses;
    }

    public ArrayList<IsoDeadBody> getCorpsesConnected() {
        ArrayList<IsoDeadBody> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZones = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZones.size(); i++) {
            DesignationZoneAnimal connectedZone = connectedZones.get(i);

            for (int j = 0; j < connectedZone.corpses.size(); j++) {
                IsoDeadBody corpse = connectedZone.corpses.get(j);
                if (!result.contains(corpse)) {
                    result.add(corpse);
                }
            }
        }

        return result;
    }

    public ArrayList<IsoFeedingTrough> getTroughs() {
        return this.troughs;
    }

    public ArrayList<IsoHutch> getHutchs() {
        return this.hutchs;
    }

    public ArrayList<IsoAnimal> getAnimalsConnected() {
        ArrayList<IsoAnimal> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZones = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZones.size(); i++) {
            DesignationZoneAnimal connectedZone = connectedZones.get(i);

            for (int j = 0; j < connectedZone.animals.size(); j++) {
                IsoAnimal animal = connectedZone.animals.get(j);
                if (!animal.isOnHook() && !result.contains(animal)) {
                    result.add(animal);
                }
            }
        }

        return result;
    }

    public ArrayList<IsoFeedingTrough> getTroughsConnected() {
        ArrayList<IsoFeedingTrough> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZone = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZone.size(); i++) {
            result.addAll(connectedZone.get(i).troughs);
        }

        return result;
    }

    public ArrayList<IsoHutch> getHutchsConnected() {
        ArrayList<IsoHutch> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZone = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZone.size(); i++) {
            result.addAll(connectedZone.get(i).hutchs);
        }

        return result;
    }

    public ArrayList<IsoWorldInventoryObject> getFoodOnGround() {
        return this.foodOnGround;
    }

    public ArrayList<IsoWorldInventoryObject> getFoodOnGroundConnected() {
        ArrayList<IsoWorldInventoryObject> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZone = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZone.size(); i++) {
            result.addAll(connectedZone.get(i).foodOnGround);
        }

        return result;
    }

    public ArrayList<IsoGridSquare> getNearWaterSquaresConnected() {
        ArrayList<IsoGridSquare> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZone = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZone.size(); i++) {
            result.addAll(connectedZone.get(i).nearWaterSquares);
        }

        return result;
    }

    public int getFullZoneSize() {
        int result = 0;
        ArrayList<DesignationZoneAnimal> connectedZone = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZone.size(); i++) {
            DesignationZoneAnimal zone = connectedZone.get(i);
            result += zone.w * zone.h;
        }

        return result;
    }

    public static void addNewRoof(int x, int y, int z) {
        DesignationZoneAnimal zone = getZone(x, y);
        if (zone != null && z > zone.z) {
            zone.roofAreas.add(zone.position3dPool.alloc().set(x, y, z));
        }
    }

    public ArrayList<Position3D> getRoofAreas() {
        return this.roofAreas;
    }

    public ArrayList<Position3D> getRoofAreasConnected() {
        ArrayList<Position3D> result = new ArrayList<>();
        ArrayList<DesignationZoneAnimal> connectedZone = getAllDZones(null, this, null);

        for (int i = 0; i < connectedZone.size(); i++) {
            result.addAll(connectedZone.get(i).roofAreas);
        }

        return result;
    }

    public static void Reset() {
        designationAnimalZoneList.clear();
    }

    public int getNbOfDung() {
        return this.nbOfDung;
    }

    public int getNbOfFeather() {
        return this.nbOfFeather;
    }
}
