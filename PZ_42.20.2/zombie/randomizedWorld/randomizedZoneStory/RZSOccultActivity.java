// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedZoneStory;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.animals.IsoAnimal;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;

@UsedFromLua
public class RZSOccultActivity extends RandomizedZoneStoryBase {
    public RZSOccultActivity() {
        this.name = "Occult Activity";
        this.chance = 1;
        this.minZoneHeight = 6;
        this.minZoneWidth = 6;
        this.zoneType.add(RandomizedZoneStoryBase.ZoneType.Forest.toString());
        this.setUnique(true);
    }

    @Override
    public void randomizeZoneStory(Zone zone) {
        this.cleanAreaForStory(this, zone);
        int midX = zone.pickedXForZoneStory;
        int midY = zone.pickedYForZoneStory;
        IsoGridSquare midSq = getSq(midX, midY, zone.z);
        IsoGridSquare stoneSq = IsoWorld.instance.getCell().getGridSquare(midX, midY - 1, zone.z);
        if (midSq != null) {
            if (stoneSq != null) {
                this.dirtBomb(midSq);
                if (Rand.NextBool(2)) {
                    this.dirtBomb(stoneSq);
                    this.addTileObject(stoneSq, "location_community_cemetary_01_30");
                }

                this.addCampfire(midSq);
                IsoGridSquare sq = midSq.getAdjacentSquare(IsoDirections.E);
                if (sq != null) {
                    this.addItemOnGround(sq, "Base.Book_Occult");
                    this.addItemOnGround(sq, "Base.Paperback_Occult");
                    if (Rand.NextBool(2)) {
                        if (Rand.NextBool(2)) {
                            this.addItemOnGround(sq, "Base.TarotCardDeck");
                        } else {
                            this.addItemOnGround(sq, "Base.OujaBoard");
                        }
                    }

                    if (Rand.NextBool(3)) {
                        this.addItemOnGround(sq, "Base.BlackRobe");
                    }

                    InventoryItem item = InventoryItemFactory.CreateItem("Base.Candle");
                    if (item instanceof DrainableComboItem) {
                        item.setCurrentUses(item.getMaxUses() / 2);
                        this.addItemOnGround(sq, item);
                    }
                }

                sq = midSq.getAdjacentSquare(IsoDirections.W);
                if (sq != null) {
                    ArrayList<String> knife = new ArrayList<>();
                    knife.add("Base.HuntingKnife");
                    knife.add("Base.KitchenKnife");
                    knife.add("Base.SwitchKnife");
                    knife.add("Base.Machete");
                    knife.add("Base.FightingKnife");
                    RandomizedZoneStoryBase.cleanSquareForStory(sq);
                    InventoryItem item = InventoryItemFactory.CreateItem(knife.get(Rand.Next(knife.size())));
                    if (item instanceof HandWeapon) {
                        item.setBloodLevel(1.0F);
                        item.randomizeGeneralCondition();
                        this.addItemOnGround(sq, item);
                    }
                }

                sq = midSq.getAdjacentSquare(IsoDirections.S);
                if (sq != null) {
                    RandomizedZoneStoryBase.cleanSquareForStory(sq);
                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), sq.x, sq.y, sq.z, "rabdoe", "swamp");
                    animal.randomizeAge();
                    animal.setHealth(0.0F);
                    this.addTrailOfBlood(sq.x, sq.y, sq.z, 0.0F, 1);
                    this.addBloodSplat(sq, Rand.Next(7, 12));
                }
            }
        }
    }
}
