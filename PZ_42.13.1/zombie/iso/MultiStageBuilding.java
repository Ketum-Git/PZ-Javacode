// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Map.Entry;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.skills.PerkFactory;
import zombie.core.Translator;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.objects.IsoThumpable;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.util.StringUtils;

@UsedFromLua
public final class MultiStageBuilding {
    public static final ArrayList<MultiStageBuilding.Stage> stages = new ArrayList<>();

    public static MultiStageBuilding.Stage getStage(String stageID) {
        for (int i = 0; i < stages.size(); i++) {
            MultiStageBuilding.Stage stage = stages.get(i);
            if (stage.id.equals(stageID)) {
                return stage;
            }
        }

        return null;
    }

    public static ArrayList<MultiStageBuilding.Stage> getStages(IsoGameCharacter chr, IsoObject itemClicked, boolean cheat) {
        ArrayList<MultiStageBuilding.Stage> result = new ArrayList<>();

        for (int i = 0; i < stages.size(); i++) {
            MultiStageBuilding.Stage stage = stages.get(i);
            if (stage.canBeDone(chr, itemClicked, cheat) && !result.contains(stage)) {
                result.add(stage);
            }
        }

        return result;
    }

    public static void addStage(MultiStageBuilding.Stage stage) {
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).id.equals(stage.id)) {
                return;
            }
        }

        stages.add(stage);
    }

    @UsedFromLua
    public class Stage {
        public String name;
        public ArrayList<String> previousStage;
        public String recipeName;
        public String sprite;
        public String northSprite;
        public int timeNeeded;
        public int bonusHealth;
        public boolean bonusHealthSkill;
        public HashMap<String, Integer> xp;
        public HashMap<String, Integer> perks;
        public HashMap<String, Integer> items;
        public ArrayList<String> itemsToKeep;
        public String knownRecipe;
        public String thumpSound;
        public String wallType;
        public boolean canBePlastered;
        public String craftingSound;
        public String completionSound;
        public String id;
        public boolean canBarricade;

        public Stage() {
            Objects.requireNonNull(MultiStageBuilding.this);
            super();
            this.previousStage = new ArrayList<>();
            this.bonusHealthSkill = true;
            this.xp = new HashMap<>();
            this.perks = new HashMap<>();
            this.items = new HashMap<>();
            this.itemsToKeep = new ArrayList<>();
            this.thumpSound = "ZombieThumpGeneric";
            this.completionSound = "BuildWoodenStructureMedium";
        }

        public String getName() {
            return this.name;
        }

        public String getDisplayName() {
            return Translator.getMultiStageBuild(this.recipeName);
        }

        public String getSprite() {
            return this.sprite;
        }

        public String getNorthSprite() {
            return this.northSprite;
        }

        public String getThumpSound() {
            return this.thumpSound;
        }

        public String getRecipeName() {
            return this.recipeName;
        }

        public String getKnownRecipe() {
            return this.knownRecipe;
        }

        public int getTimeNeeded(IsoGameCharacter chr) {
            int time = this.timeNeeded;

            for (Entry<String, Integer> entry : this.xp.entrySet()) {
                time -= chr.getPerkLevel(PerkFactory.Perks.FromString(entry.getKey())) * 10;
            }

            return time;
        }

        public ArrayList<String> getItemsToKeep() {
            return this.itemsToKeep;
        }

        public ArrayList<String> getPreviousStages() {
            return this.previousStage;
        }

        public String getCraftingSound() {
            return this.craftingSound;
        }

        public KahluaTable getItemsLua() {
            KahluaTable result = LuaManager.platform.newTable();

            for (Entry<String, Integer> entry : this.items.entrySet()) {
                result.rawset(entry.getKey(), entry.getValue().toString());
            }

            return result;
        }

        public KahluaTable getPerksLua() {
            KahluaTable result = LuaManager.platform.newTable();

            for (Entry<String, Integer> entry : this.perks.entrySet()) {
                result.rawset(PerkFactory.Perks.FromString(entry.getKey()), entry.getValue().toString());
            }

            return result;
        }

        public void doStage(IsoGameCharacter chr, IsoThumpable item, boolean removeItems) {
            if (!GameClient.client) {
                int previousHealth = item.getHealth();
                int previousMaxHealth = item.getMaxHealth();
                String fSprite = this.sprite;
                if (item.north) {
                    fSprite = this.northSprite;
                }

                IsoThumpable newBuild = new IsoThumpable(IsoWorld.instance.getCell(), item.square, fSprite, item.north, item.getTable());
                newBuild.setCanBePlastered(this.canBePlastered);
                if ("doorframe".equals(this.wallType)) {
                    newBuild.setIsDoorFrame(true);
                    newBuild.setCanPassThrough(true);
                    newBuild.setIsThumpable(item.isThumpable());
                }

                int fBonusHealth = this.bonusHealth;
                switch (SandboxOptions.instance.constructionBonusPoints.getValue()) {
                    case 1:
                        fBonusHealth = (int)(fBonusHealth * 0.5);
                        break;
                    case 2:
                        fBonusHealth = (int)(fBonusHealth * 0.7);
                    case 3:
                    default:
                        break;
                    case 4:
                        fBonusHealth = (int)(fBonusHealth * 1.3);
                        break;
                    case 5:
                        fBonusHealth = (int)(fBonusHealth * 1.5);
                }

                Iterator<String> perkIT = this.perks.keySet().iterator();
                int bonus = 20;
                switch (SandboxOptions.instance.constructionBonusPoints.getValue()) {
                    case 1:
                        bonus = 5;
                        break;
                    case 2:
                        bonus = 10;
                    case 3:
                    default:
                        break;
                    case 4:
                        bonus = 35;
                        break;
                    case 5:
                        bonus = 60;
                }

                int skillBonus = 0;
                if (this.bonusHealthSkill) {
                    while (perkIT.hasNext()) {
                        String perkType = perkIT.next();
                        skillBonus += chr.getPerkLevel(PerkFactory.Perks.FromString(perkType)) * bonus;
                    }
                }

                newBuild.setMaxHealth(previousMaxHealth + fBonusHealth + skillBonus);
                newBuild.setHealth(previousHealth + fBonusHealth + skillBonus);
                newBuild.setName(this.name);
                newBuild.setThumpSound(this.getThumpSound());
                newBuild.setCanBarricade(this.canBarricade);
                newBuild.setModData(item.getModData());
                if (this.wallType != null) {
                    newBuild.getModData().rawset("wallType", this.wallType);
                }

                if (removeItems) {
                    ItemContainer inv = chr.getInventory();

                    for (String itemType : this.items.keySet()) {
                        int count = this.items.get(itemType);
                        Item scriptItem = ScriptManager.instance.getItem(itemType);
                        if (scriptItem != null) {
                            if (scriptItem.isItemType(ItemType.DRAINABLE)) {
                                InventoryItem invItem = inv.getFirstRecurse(
                                    item1 -> item1.getFullType().equals(scriptItem.getFullName()) && item1.getCurrentUses() >= count
                                );
                                if (invItem != null) {
                                    for (int i = 0; i < count; i++) {
                                        invItem.UseAndSync();
                                    }
                                }
                            } else {
                                for (int i = 0; i < count; i++) {
                                    InventoryItem invItem = inv.getFirstTypeRecurse(itemType);
                                    if (invItem != null) {
                                        invItem.UseAndSync();
                                    }
                                }
                            }
                        }
                    }
                }

                for (String perk : this.xp.keySet()) {
                    if (GameServer.server) {
                        GameServer.addXp((IsoPlayer)chr, PerkFactory.Perks.FromString(perk), this.xp.get(perk).intValue());
                    } else {
                        chr.getXp().AddXP(PerkFactory.Perks.FromString(perk), (float)this.xp.get(perk).intValue());
                    }
                }

                int index = item.getSquare().transmitRemoveItemFromSquare(item);
                newBuild.getSquare().AddSpecialObject(newBuild, index);
                newBuild.getSquare().RecalcAllWithNeighbours(true);
                newBuild.transmitCompleteItemToClients();
            }
        }

        public void playCompletionSound(IsoGameCharacter chr) {
            if (chr != null && !StringUtils.isNullOrWhitespace(this.completionSound)) {
                chr.playSound(this.completionSound);
            }
        }

        public boolean canBeDone(IsoGameCharacter chr, IsoObject itemClicked, boolean cheat) {
            ItemContainer inv = chr.getInventory();
            boolean previousStageOk = false;

            for (int i = 0; i < this.previousStage.size(); i++) {
                if (this.previousStage.get(i).equalsIgnoreCase(itemClicked.getName())) {
                    previousStageOk = true;
                    break;
                }
            }

            return previousStageOk;
        }

        public void Load(String name, String[] strArray) {
            this.recipeName = name;

            for (int i = 0; i < strArray.length; i++) {
                if (!strArray[i].trim().isEmpty() && strArray[i].contains(":")) {
                    String[] split = strArray[i].split(":");
                    String key = split[0].trim();
                    String value = split[1].trim();
                    if (key.equalsIgnoreCase("Name")) {
                        this.name = value.trim();
                    }

                    if (key.equalsIgnoreCase("TimeNeeded")) {
                        this.timeNeeded = Integer.parseInt(value.trim());
                    }

                    if (key.equalsIgnoreCase("BonusHealth")) {
                        this.bonusHealth = Integer.parseInt(value.trim());
                    }

                    if (key.equalsIgnoreCase("Sprite")) {
                        this.sprite = value.trim();
                    }

                    if (key.equalsIgnoreCase("NorthSprite")) {
                        this.northSprite = value.trim();
                    }

                    if (key.equalsIgnoreCase("KnownRecipe")) {
                        this.knownRecipe = value.trim();
                    }

                    if (key.equalsIgnoreCase("ThumpSound")) {
                        this.thumpSound = value.trim();
                    }

                    if (key.equalsIgnoreCase("WallType")) {
                        this.wallType = value.trim();
                    }

                    if (key.equalsIgnoreCase("CraftingSound")) {
                        this.craftingSound = value.trim();
                    }

                    if (key.equalsIgnoreCase("CompletionSound")) {
                        this.completionSound = value.trim();
                    }

                    if (key.equalsIgnoreCase("ID")) {
                        this.id = value.trim();
                    }

                    if (key.equalsIgnoreCase("CanBePlastered")) {
                        this.canBePlastered = Boolean.parseBoolean(value.trim());
                    }

                    if (key.equalsIgnoreCase("BonusSkill")) {
                        this.bonusHealthSkill = Boolean.parseBoolean(value.trim());
                    }

                    if (key.equalsIgnoreCase("CanBarricade")) {
                        this.canBarricade = Boolean.parseBoolean(value.trim());
                    }

                    if (key.equalsIgnoreCase("XP")) {
                        String[] split2 = value.split(";");

                        for (int j = 0; j < split2.length; j++) {
                            String[] split3 = split2[j].split("=");
                            this.xp.put(split3[0], Integer.parseInt(split3[1]));
                        }
                    }

                    if (key.equalsIgnoreCase("PreviousStage")) {
                        String[] split2 = value.split(";");

                        for (int j = 0; j < split2.length; j++) {
                            this.previousStage.add(split2[j]);
                        }
                    }

                    if (key.equalsIgnoreCase("SkillRequired")) {
                        String[] split2 = value.split(";");

                        for (int j = 0; j < split2.length; j++) {
                            String[] split3 = split2[j].split("=");
                            this.perks.put(split3[0], Integer.parseInt(split3[1]));
                        }
                    }

                    if (key.equalsIgnoreCase("ItemsRequired")) {
                        String[] split2 = value.split(";");

                        for (int j = 0; j < split2.length; j++) {
                            String[] split3 = split2[j].split("=");
                            this.items.put(split3[0], Integer.parseInt(split3[1]));
                        }
                    }

                    if (key.equalsIgnoreCase("ItemsToKeep")) {
                        String[] split2 = value.split(";");

                        for (int j = 0; j < split2.length; j++) {
                            this.itemsToKeep.add(split2[j]);
                        }
                    }
                }
            }
        }
    }
}
