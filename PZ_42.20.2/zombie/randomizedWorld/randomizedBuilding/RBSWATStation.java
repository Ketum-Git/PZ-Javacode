// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.core.properties.IsoPropertyType;
import zombie.core.properties.PropertyContainer;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;
import zombie.scripting.objects.FaceType;
import zombie.scripting.objects.ItemKey;
import zombie.util.list.WeightedList;

public final class RBSWATStation extends RandomizedBuildingBase {
    private static final int PROBABILITY_RIFLE_M16 = 40;
    private static final int PROBABILITY_RIFLE_MSR7T = 60;
    private static final int PROBABILITY_SHOTGUN_JS3T = 80;
    private static final int PROBABILITY_AMMO_308 = 60;
    private static final int PROBABILITY_AMMO_556 = 20;
    private static final int PROBABILITY_AMMO_9mm = 60;
    private static final int PROBABILITY_AMMO_45 = 20;
    private static final int PROBABILITY_AMMO_SHOTGUN = 80;
    private static final int PROBABILITY_SPAWN_RIFLE = 20;
    private static final int PROBABILITY_SPAWN_ARMOR = 5;
    private static final int PROBABILITY_SPAWN_BAG = 4;
    private static final WeightedList<ItemKey> RIFLES = new WeightedList<>();
    private static final WeightedList<ItemKey> AMMO_CANS = new WeightedList<>();

    @Override
    public void randomizeBuilding(BuildingDef def) {
        if (RIFLES.isEmpty()) {
            RIFLES.add(ItemKey.Weapon.ASSAULT_RIFLE, 40);
            RIFLES.add(ItemKey.Weapon.JS3T_SHOTGUN, 80);
            RIFLES.add(ItemKey.Weapon.MSR7T_RIFLE, 60);
        }

        if (AMMO_CANS.isEmpty()) {
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_45, 20);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_308, 60);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_9MM, 60);
            AMMO_CANS.add(ItemKey.Container.BAG_AMMO_BOX_SHOTGUN_SHELLS, 80);
        }

        for (IsoObject obj : getBuildingObjectsSimple(def)) {
            IsoGridSquare sq = obj.getSquare();
            IsoSprite sprite = obj.getSprite();
            if (sprite != null && roomValid(sq)) {
                PropertyContainer props = obj.getSprite().getProperties();
                String facing = props.get(IsoPropertyType.FACING);
                boolean facingE = FaceType.E.toString().equals(facing);
                boolean facingW = FaceType.W.toString().equals(facing);
                boolean facingN = FaceType.N.toString().equals(facing);
                boolean facingS = FaceType.S.toString().equals(facing);
                if (props.has(IsoPropertyType.CUSTOM_NAME) && props.get(IsoPropertyType.CUSTOM_NAME).contains("Gun")) {
                    doGunShelfRifles(facingE, sq, RIFLES, 20);
                } else if (validSurface(sq)) {
                    if (props.has(IsoPropertyType.GROUP_NAME) && props.get(IsoPropertyType.GROUP_NAME).contains("Oak") && Rand.NextBool(5)) {
                        doBodyArmorTable(facingS, sq);
                    } else {
                        if (props.has(IsoPropertyType.GROUP_NAME) && props.get(IsoPropertyType.GROUP_NAME).contains("Smooth")) {
                            if (Rand.NextBool(5)) {
                                doBodyArmorTable2(facingE, sq);
                                continue;
                            }

                            if (Rand.NextBool(4)) {
                                doWeaponBagTable(facingE, sq);
                                continue;
                            }
                        }

                        ItemContainer container = obj.getContainer();
                        if (container != null && obj.getContainer().getType().equals("locker")) {
                            doAmmoCans(props, facingE, facingW, facingN, sq, AMMO_CANS);
                        }
                    }
                }
            }
        }
    }

    private static void doBodyArmorTable(boolean facingS, IsoGridSquare sq) {
        int armorSlots = 2;
        int helmetSlots = 2;
        float xOffset = facingS ? 0.64F : 0.42F;
        float xOffset2 = facingS ? 0.32F : 0.48F;
        float yOffset = facingS ? 0.28F : 0.68F;
        float yOffset2 = facingS ? 0.28F : 0.4F;
        float zOffset = 0.33F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = facingS ? 270.0F : 0.0F;
        float zRotation2 = facingS ? 0.0F : 90.0F;

        for (int j = 0; j < 2; j++) {
            InventoryItem vest = InventoryItemFactory.CreateItem(ItemKey.Clothing.VEST_BULLET_SWAT);
            sq.AddWorldInventoryItem(vest, xOffset, yOffset, 0.33F, false, true);
            setWorldRotation(vest, 0.0F, 0.0F, zRotation);
            if (facingS) {
                yOffset += 0.36F;
            } else {
                xOffset += 0.36F;
            }
        }

        for (int i = 0; i < 2; i++) {
            InventoryItem helm = InventoryItemFactory.CreateItem(ItemKey.Clothing.HAT_SWAT);
            sq.AddWorldInventoryItem(helm, xOffset2, yOffset2, 0.33F, false, true);
            setWorldRotation(helm, 0.0F, 0.0F, zRotation2);
            if (facingS) {
                yOffset2 += 0.36F;
            } else {
                xOffset2 += 0.36F;
            }
        }
    }

    private static void doBodyArmorTable2(boolean facingE, IsoGridSquare sq) {
        int armorSlots = 2;
        int helmetSlots = 2;
        float xOffset = facingE ? 0.62F : 0.32F;
        float xOffset2 = facingE ? 0.32F : 0.38F;
        float yOffset = facingE ? 0.32F : 0.64F;
        float yOffset2 = facingE ? 0.38F : 0.32F;
        float zOffset = 0.25F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotation = facingE ? 270.0F : 0.0F;
        float zRotation2 = facingE ? 0.0F : 90.0F;

        for (int j = 0; j < 2; j++) {
            InventoryItem vest = InventoryItemFactory.CreateItem(ItemKey.Clothing.VEST_BULLET_SWAT);
            sq.AddWorldInventoryItem(vest, xOffset, yOffset, 0.25F, false, true);
            setWorldRotation(vest, 0.0F, 0.0F, zRotation);
            if (facingE) {
                yOffset += 0.36F;
            } else {
                xOffset += 0.36F;
            }
        }

        for (int i = 0; i < 2; i++) {
            InventoryItem helm = InventoryItemFactory.CreateItem(ItemKey.Clothing.HAT_SWAT);
            sq.AddWorldInventoryItem(helm, xOffset2, yOffset2, 0.25F, false, true);
            setWorldRotation(helm, 0.0F, 0.0F, zRotation2);
            if (facingE) {
                yOffset2 += 0.36F;
            } else {
                xOffset2 += 0.36F;
            }
        }
    }

    private static void doWeaponBagTable(boolean facingE, IsoGridSquare sq) {
        float xOffsetGun = facingE ? 0.64F : 0.54F;
        float xOffsetBag = facingE ? 0.32F : 0.52F;
        float yOffsetGun = facingE ? 0.52F : 0.72F;
        float yOffsetBag = facingE ? 0.56F : 0.4F;
        float zOffset = 0.25F;
        float xRotation = 0.0F;
        float yRotation = 0.0F;
        float zRotationBag = facingE ? 0.0F : 90.0F;
        float zRotationGun = facingE ? 270.0F : 0.0F;
        HandWeapon gun = spawnRifle(RIFLES.getRandom());
        sq.AddWorldInventoryItem(gun, xOffsetGun, yOffsetGun, 0.25F, false, true);
        setWorldRotation(gun, 0.0F, 0.0F, zRotationGun);
        InventoryContainer bag = InventoryItemFactory.CreateItem(ItemKey.Container.BAG_SWAT);
        sq.AddWorldInventoryItem(bag, xOffsetBag, yOffsetBag, 0.25F, false, true);
        ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get(bag.getType()));
        setWorldRotation(bag, 0.0F, 0.0F, zRotationBag);
    }

    private static boolean validSurface(IsoGridSquare sq) {
        for (int i = 0; i < sq.getObjects().size(); i++) {
            IsoSprite sprite = sq.getObjects().get(i).getSprite();
            PropertyContainer props1 = sprite.getProperties();
            if (props1.has(IsoPropertyType.CUSTOM_NAME)
                && (
                    props1.get(IsoPropertyType.CUSTOM_NAME).contains("Phone")
                        || props1.get(IsoPropertyType.CUSTOM_NAME).contains("Lamp")
                        || props1.get(IsoPropertyType.CUSTOM_NAME).contains("Radio")
                )) {
                return false;
            }
        }

        return true;
    }

    private static boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && sq.getRoom().getName().equals("policeswat");
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("policeswat") != null;
    }

    public RBSWATStation() {
        this.name = "SWAT Station";
        this.setAlwaysDo(true);
    }
}
