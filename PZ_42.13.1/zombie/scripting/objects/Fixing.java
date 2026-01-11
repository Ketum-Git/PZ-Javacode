// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.DrainableComboItem;
import zombie.scripting.ScriptType;

@UsedFromLua
public final class Fixing extends BaseScriptObject {
    private String name;
    private ArrayList<String> require;
    private final LinkedList<Fixing.Fixer> fixers = new LinkedList<>();
    private Fixing.Fixer globalItem;
    private float conditionModifier = 1.0F;
    private static final Fixing.PredicateRequired s_PredicateRequired = new Fixing.PredicateRequired();
    private static final ArrayList<InventoryItem> s_InventoryItems = new ArrayList<>();

    public Fixing() {
        super(ScriptType.Fixing);
    }

    @Override
    public void Load(String name, String body) {
        String[] waypoint = body.split("[{}]");
        String[] coords = waypoint[1].split(",");
        this.Load(name, coords);
    }

    private void Load(String name, String[] strArray) {
        this.setName(name);

        for (int i = 0; i < strArray.length; i++) {
            if (!strArray[i].trim().isEmpty() && strArray[i].contains("=")) {
                String[] split = strArray[i].split("=", 2);
                String key = split[0].trim();
                String value = split[1].trim();
                if (key.equals("Require")) {
                    List<String> list = Arrays.asList(value.split(";"));

                    for (int j = 0; j < list.size(); j++) {
                        this.addRequiredItem(list.get(j).trim());
                    }
                } else if (!key.equals("Fixer")) {
                    if (key.equals("GlobalItem")) {
                        if (value.contains("=")) {
                            this.setGlobalItem(new Fixing.Fixer(value.split("=")[0], null, Integer.parseInt(value.split("=")[1])));
                        } else {
                            this.setGlobalItem(new Fixing.Fixer(value, null, 1));
                        }
                    } else if (key.equals("ConditionModifier")) {
                        this.setConditionModifier(Float.parseFloat(value.trim()));
                    }
                } else if (!value.contains(";")) {
                    if (value.contains("=")) {
                        this.fixers.add(new Fixing.Fixer(value.split("=")[0], null, Integer.parseInt(value.split("=")[1])));
                    } else {
                        this.fixers.add(new Fixing.Fixer(value, null, 1));
                    }
                } else {
                    LinkedList<Fixing.FixerSkill> finalList = new LinkedList<>();
                    List<String> skillList = Arrays.asList(value.split(";"));

                    for (int j = 1; j < skillList.size(); j++) {
                        String[] ss = skillList.get(j).trim().split("=");
                        finalList.add(new Fixing.FixerSkill(ss[0].trim(), Integer.parseInt(ss[1].trim())));
                    }

                    if (value.split(";")[0].trim().contains("=")) {
                        String[] ss = value.split(";")[0].trim().split("=");
                        this.fixers.add(new Fixing.Fixer(ss[0], finalList, Integer.parseInt(ss[1])));
                    } else {
                        this.fixers.add(new Fixing.Fixer(value.split(";")[0].trim(), finalList, 1));
                    }
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getRequiredItem() {
        return this.require;
    }

    public void addRequiredItem(String require) {
        if (this.require == null) {
            this.require = new ArrayList<>();
        }

        this.require.add(require);
    }

    public LinkedList<Fixing.Fixer> getFixers() {
        return this.fixers;
    }

    public Fixing.Fixer usedInFixer(InventoryItem itemType, IsoGameCharacter chr) {
        for (int j = 0; j < this.getFixers().size(); j++) {
            Fixing.Fixer fixer = this.getFixers().get(j);
            if (fixer.getFixerName().equals(itemType.getFullType())) {
                if (itemType instanceof DrainableComboItem item) {
                    if (!(item.getCurrentUsesFloat() < 1.0F)) {
                        return fixer;
                    }

                    if (item.getCurrentUses() >= fixer.getNumberOfUse()) {
                        return fixer;
                    }
                } else if (chr.getInventory().getCountTypeRecurse(fixer.getFixerName()) >= fixer.getNumberOfUse()) {
                    return fixer;
                }
            }
        }

        return null;
    }

    public InventoryItem haveGlobalItem(IsoGameCharacter chr) {
        s_InventoryItems.clear();
        ArrayList<InventoryItem> items = this.getRequiredFixerItems(chr, this.getGlobalItem(), null, s_InventoryItems);
        return items == null ? null : items.get(0);
    }

    public InventoryItem haveThisFixer(IsoGameCharacter chr, Fixing.Fixer fixer, InventoryItem brokenObject) {
        s_InventoryItems.clear();
        ArrayList<InventoryItem> items = this.getRequiredFixerItems(chr, fixer, brokenObject, s_InventoryItems);
        return items == null ? null : items.get(0);
    }

    public int countUses(IsoGameCharacter chr, Fixing.Fixer fixer, InventoryItem brokenObject) {
        s_InventoryItems.clear();
        s_PredicateRequired.uses = 0;
        this.getRequiredFixerItems(chr, fixer, brokenObject, s_InventoryItems);
        return s_PredicateRequired.uses;
    }

    private static int countUses(InventoryItem item) {
        return item instanceof DrainableComboItem drainable ? drainable.getCurrentUses() : 1;
    }

    public ArrayList<InventoryItem> getRequiredFixerItems(IsoGameCharacter chr, Fixing.Fixer fixer, InventoryItem brokenItem, ArrayList<InventoryItem> items) {
        if (fixer == null) {
            return null;
        } else {
            assert Thread.currentThread() == GameWindow.gameThread;

            Fixing.PredicateRequired predicate = s_PredicateRequired;
            predicate.fixer = fixer;
            predicate.brokenItem = brokenItem;
            predicate.uses = 0;
            chr.getInventory().getAllRecurse(predicate, items);
            return predicate.uses >= fixer.getNumberOfUse() ? items : null;
        }
    }

    public ArrayList<InventoryItem> getRequiredItems(IsoGameCharacter chr, Fixing.Fixer fixer, InventoryItem brokenItem) {
        ArrayList<InventoryItem> items = new ArrayList<>();
        if (this.getRequiredFixerItems(chr, fixer, brokenItem, items) == null) {
            items.clear();
            return null;
        } else if (this.getGlobalItem() != null && this.getRequiredFixerItems(chr, this.getGlobalItem(), brokenItem, items) == null) {
            items.clear();
            return null;
        } else {
            return items;
        }
    }

    public Fixing.Fixer getGlobalItem() {
        return this.globalItem;
    }

    public void setGlobalItem(Fixing.Fixer globalItem) {
        this.globalItem = globalItem;
    }

    public float getConditionModifier() {
        return this.conditionModifier;
    }

    public void setConditionModifier(float conditionModifier) {
        this.conditionModifier = conditionModifier;
    }

    @UsedFromLua
    public static final class Fixer {
        private String fixerName;
        private LinkedList<Fixing.FixerSkill> skills;
        private int numberOfUse = 1;

        public Fixer(String name, LinkedList<Fixing.FixerSkill> skills, int numberOfUse) {
            this.fixerName = name;
            this.skills = skills;
            this.numberOfUse = numberOfUse;
        }

        public String getFixerName() {
            return this.fixerName;
        }

        public LinkedList<Fixing.FixerSkill> getFixerSkills() {
            return this.skills;
        }

        public int getNumberOfUse() {
            return this.numberOfUse;
        }
    }

    @UsedFromLua
    public static final class FixerSkill {
        private String skillName;
        private int skillLvl;

        public FixerSkill(String skillName, int skillLvl) {
            this.skillName = skillName;
            this.skillLvl = skillLvl;
        }

        public String getSkillName() {
            return this.skillName;
        }

        public int getSkillLevel() {
            return this.skillLvl;
        }
    }

    private static final class PredicateRequired implements Predicate<InventoryItem> {
        Fixing.Fixer fixer;
        InventoryItem brokenItem;
        int uses;

        public boolean test(InventoryItem item) {
            if (this.uses >= this.fixer.getNumberOfUse()) {
                return false;
            } else if (item == this.brokenItem) {
                return false;
            } else if (!this.fixer.getFixerName().equals(item.getFullType())) {
                return false;
            } else {
                int itemUses = Fixing.countUses(item);
                if (itemUses > 0) {
                    this.uses += itemUses;
                    return true;
                } else {
                    return false;
                }
            }
        }
    }
}
