// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class RDSRPGNight extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<String> dice = new ArrayList<>();
    private final ArrayList<String> paper = new ArrayList<>();
    private String manual;
    private String diceBag;
    private String pencil;

    public RDSRPGNight() {
        this.name = "RPG Night";
        this.setChance(1);
        this.setUnique(true);
        this.setMaximumDays(30);
        this.items.add("Base.Calculator");
        this.items.add("Base.Eraser");
        this.items.add("Base.Crisps");
        this.items.add("Base.Crisps2");
        this.items.add("Base.Crisps3");
        this.items.add("Base.Pop");
        this.items.add("Base.Pop2");
        this.items.add("Base.Pop3");
        this.items.add("Base.Magazine_Gaming");
        this.items.add("Base.RPGmanual");
        this.items.add("Base.ComicBook");
        this.items.add("Base.Hat_Wizard");
        this.items.add("Base.Paperback_Fantasy");
        this.dice.add("Base.Dice_00");
        this.dice.add("Base.Dice_10");
        this.dice.add("Base.Dice_12");
        this.dice.add("Base.Dice_20");
        this.dice.add("Base.Dice_4");
        this.dice.add("Base.Dice_6");
        this.dice.add("Base.Dice_8");
        this.dice.add("Base.DiceBag");
        this.paper.add("Base.Notebook");
        this.paper.add("Base.SheetPaper2");
        this.paper.add("Base.GraphPaper");
        this.paper.add("Base.Journal");
        this.paper.add("Base.Note");
        this.manual = "Base.RPGmanual";
        this.diceBag = "Base.DiceBag";
        this.pencil = "Base.Pencil";
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (def.isAllExplored() && !force) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else {
            if (!force) {
                for (int i = 0; i < GameServer.Players.size(); i++) {
                    IsoPlayer player = GameServer.Players.get(i);
                    if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                        return false;
                    }
                }
            }

            if (this.getRoom(def, "livingroom") != null) {
                return true;
            } else {
                this.debugLine = "No living room";
                return false;
            }
        }
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "livingroom");
        this.addZombies(def, Rand.Next(4, 6), "Hobbyist", 10, room);
        this.addRandomItemsOnGround(room, this.items, Rand.Next(8, 13));
        this.addRandomItemsOnGround(room, this.dice, Rand.Next(8, 13));
        this.addRandomItemsOnGround(room, this.paper, Rand.Next(4, 6));
        this.addRandomItemsOnGround(room, this.manual, 1);
        this.addRandomItemsOnGround(room, this.diceBag, 1);
        this.addRandomItemsOnGround(room, this.pencil, Rand.Next(4, 6));
        def.alarmed = false;
    }
}
