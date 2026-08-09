// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoZombie;
import zombie.core.ImmutableColor;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

@UsedFromLua
public class RBKateAndBaldspot extends RandomizedBuildingBase {
    public RBKateAndBaldspot() {
        this.name = "K&B story";
        this.setChance(0);
        this.setUnique(true);
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        def.setAllExplored(true);
        ArrayList<IsoZombie> zeds = this.addZombiesOnSquare(1, "Kate", 100, getSq(10746, 9412, 1));
        if (zeds != null && !zeds.isEmpty()) {
            IsoZombie kate = zeds.get(0);
            HumanVisual visu = (HumanVisual)kate.getVisual();
            visu.setHairModel("Rachel");
            visu.setHairColor(new ImmutableColor(0.83F, 0.67F, 0.27F));

            for (int i = 0; i < kate.getItemVisuals().size(); i++) {
                ItemVisual itemVisu = kate.getItemVisuals().get(i);
                if (itemVisu.getClothingItemName().equals("Skirt_Knees")) {
                    itemVisu.setTint(new ImmutableColor(0.54F, 0.54F, 0.54F));
                }
            }

            kate.getHumanVisual().setSkinTextureIndex(1);
            kate.addBlood(BloodBodyPartType.LowerLeg_L, true, true, true);
            kate.addBlood(BloodBodyPartType.LowerLeg_L, true, true, true);
            kate.addBlood(BloodBodyPartType.UpperLeg_L, true, true, true);
            kate.addBlood(BloodBodyPartType.UpperLeg_L, true, true, true);
            kate.setCrawler(true);
            kate.setCanWalk(false);
            kate.setCrawlerType(1);
            kate.resetModelNextFrame();
            zeds = this.addZombiesOnSquare(1, "Bob", 0, getSq(10747, 9412, 1));
            if (zeds != null && !zeds.isEmpty()) {
                IsoZombie bob = zeds.get(0);
                visu = (HumanVisual)bob.getVisual();
                visu.setHairModel("Baldspot");
                visu.setHairColor(new ImmutableColor(0.337F, 0.173F, 0.082F));
                visu.setBeardModel("");

                for (int ix = 0; ix < bob.getItemVisuals().size(); ix++) {
                    ItemVisual itemVisu = bob.getItemVisuals().get(ix);
                    if (itemVisu.getClothingItemName().equals("Trousers_DefaultTEXTURE_TINT")) {
                        itemVisu.setTint(new ImmutableColor(0.54F, 0.54F, 0.54F));
                    }

                    if (itemVisu.getClothingItemName().equals("Shirt_FormalTINT")) {
                        itemVisu.setTint(new ImmutableColor(0.63F, 0.71F, 0.82F));
                    }
                }

                bob.getHumanVisual().setSkinTextureIndex(1);
                bob.resetModelNextFrame();
                bob.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem("KatePic"));
                bob.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem("RippedSheets"));
                bob.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem("Pills"));
                InventoryItem hammer = InventoryItemFactory.CreateItem("Hammer");
                hammer.setCondition(1, false);
                bob.addItemToSpawnAtDeath(hammer);
                bob.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem("Nails"));
                bob.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem("Plank"));
                zeds = this.addZombiesOnSquare(1, "Raider", 0, getSq(10745, 9411, 0));
                if (zeds != null && !zeds.isEmpty()) {
                    IsoZombie raider = zeds.get(0);
                    visu = (HumanVisual)raider.getVisual();
                    visu.setHairModel("Crewcut");
                    visu.setHairColor(new ImmutableColor(0.37F, 0.27F, 0.23F));
                    visu.setBeardModel("Goatee");

                    for (int ix = 0; ix < raider.getItemVisuals().size(); ix++) {
                        ItemVisual itemVisux = raider.getItemVisuals().get(ix);
                        if (itemVisux.getClothingItemName().equals("Trousers_DefaultTEXTURE_TINT")) {
                            itemVisux.setTint(new ImmutableColor(0.54F, 0.54F, 0.54F));
                        }

                        if (itemVisux.getClothingItemName().equals("Vest_DefaultTEXTURE_TINT")) {
                            itemVisux.setTint(new ImmutableColor(0.22F, 0.25F, 0.27F));
                        }
                    }

                    raider.getHumanVisual().setSkinTextureIndex(1);
                    InventoryItem shotgun = InventoryItemFactory.CreateItem("Shotgun");
                    shotgun.setCondition(0, false);
                    raider.setAttachedItem("Rifle On Back", shotgun);
                    InventoryItem bat = InventoryItemFactory.CreateItem("BaseballBat");
                    bat.setCondition(1, false);
                    raider.addItemToSpawnAtDeath(bat);
                    raider.addItemToSpawnAtDeath(InventoryItemFactory.CreateItem("ShotgunShells"));
                    raider.resetModelNextFrame();
                    this.addItemOnGround(getSq(10747, 9412, 1), InventoryItemFactory.CreateItem("Pillow"));
                    IsoGridSquare burntSq = getSq(10745, 9410, 0);
                    burntSq.Burn();
                    burntSq = getSq(10745, 9411, 0);
                    burntSq.Burn();
                    burntSq = getSq(10746, 9411, 0);
                    burntSq.Burn();
                    burntSq = getSq(10745, 9410, 0);
                    burntSq.Burn();
                    burntSq = getSq(10745, 9412, 0);
                    burntSq.Burn();
                    burntSq = getSq(10747, 9410, 0);
                    burntSq.Burn();
                    burntSq = getSq(10746, 9409, 0);
                    burntSq.Burn();
                    burntSq = getSq(10745, 9409, 0);
                    burntSq.Burn();
                    burntSq = getSq(10744, 9410, 0);
                    burntSq.Burn();
                    burntSq = getSq(10747, 9411, 0);
                    burntSq.Burn();
                    burntSq = getSq(10746, 9412, 0);
                    burntSq.Burn();
                    IsoGridSquare ovenSq = getSq(10746, 9410, 0);

                    for (int ix = 0; ix < ovenSq.getObjects().size(); ix++) {
                        IsoObject oven = ovenSq.getObjects().get(ix);
                        if (oven.getContainer() != null) {
                            InventoryItem soup = InventoryItemFactory.CreateItem("PotOfSoup");
                            soup.setCooked(true);
                            soup.setBurnt(true);
                            oven.getContainer().AddItem(soup);
                            break;
                        }
                    }

                    this.addBarricade(getSq(10747, 9417, 0), 3);
                    this.addBarricade(getSq(10745, 9417, 0), 3);
                    this.addBarricade(getSq(10744, 9413, 0), 3);
                    this.addBarricade(getSq(10744, 9412, 0), 3);
                    this.addBarricade(getSq(10752, 9413, 0), 3);
                }
            }
        }
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (def.x == 10744 && def.y == 9409) {
            return true;
        } else {
            this.debugLine = "Need to be the K&B house";
            return false;
        }
    }
}
