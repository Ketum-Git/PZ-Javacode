// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.traits.CharacterTraitDefinition;
import zombie.characters.traits.CharacterTraits;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.ItemType;
import zombie.ui.ObjectTooltip;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@UsedFromLua
public final class Literature extends InventoryItem {
    public boolean alreadyRead;
    public String requireInHandOrInventory;
    public String useOnConsume;
    private int numberOfPages = -1;
    private String bookName = "";
    private int lvlSkillTrained = -1;
    private int numLevelsTrained;
    private String skillTrained = "None";
    private int alreadyReadPages;
    private boolean canBeWrite;
    private HashMap<Integer, String> customPages;
    private String lockedBy;
    private int pageToWrite;
    private List<String> learnedRecipes;
    private final int maxTextLength = 16384;

    public Literature(String module, String name, String itemType, String texName) {
        super(module, name, itemType, texName);
        this.setBookName(name);
        this.itemType = ItemType.LITERATURE;
        if (this.staticModel == null) {
            this.staticModel = "Book";
        }
    }

    public Literature(String module, String name, String itemType, Item item) {
        super(module, name, itemType, item);
        this.setBookName(name);
        this.itemType = ItemType.LITERATURE;
        if (this.staticModel == null) {
            this.staticModel = "Book";
        }
    }

    @Override
    public boolean IsLiterature() {
        return true;
    }

    @Override
    public String getCategory() {
        return this.mainCategory != null ? this.mainCategory : "Literature";
    }

    @Override
    public void update() {
    }

    @Override
    public boolean finishupdate() {
        return true;
    }

    @Override
    public void DoTooltip(ObjectTooltip tooltipUI, ObjectTooltip.Layout layout) {
        IsoGameCharacter character = tooltipUI.getCharacter();
        if (character != null) {
            ColorInfo highlightGood = Core.getInstance().getGoodHighlitedColor();
            ColorInfo highlightBad = Core.getInstance().getBadHighlitedColor();
            float goodR = highlightGood.getR();
            float goodG = highlightGood.getG();
            float goodB = highlightGood.getB();
            float badR = highlightBad.getR();
            float badG = highlightBad.getG();
            float badB = highlightBad.getB();
            KahluaTable modData = this.getModData();
            if (modData.rawget("literatureTitle") == null
                || modData.rawget("literatureTitle") != null && !character.isLiteratureRead((String)modData.rawget("literatureTitle"))) {
                if (this.getBoredomChange() != 0.0F) {
                    ObjectTooltip.LayoutItem item = layout.addItem();
                    float value = this.getBoredomChange() * -0.02F;
                    item.setLabel(Translator.getText("Tooltip_food_Boredom") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (value > 0.0F) {
                        item.setProgress(value, goodR, goodG, goodB, 1.0F);
                    } else {
                        item.setProgress(value * -1.0F, badR, badG, badB, 1.0F);
                    }
                }

                if (this.getStressChange() != 0.0F) {
                    ObjectTooltip.LayoutItem item = layout.addItem();
                    int stress = (int)(this.getStressChange() * 100.0F);
                    float value = stress * -0.02F;
                    item.setLabel(Translator.getText("Tooltip_literature_Stress_Reduction") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (value > 0.0F) {
                        item.setProgress(value, goodR, goodG, goodB, 1.0F);
                    } else {
                        item.setProgress(value * -1.0F, badR, badG, badB, 1.0F);
                    }
                }

                if (this.getUnhappyChange() != 0.0F) {
                    ObjectTooltip.LayoutItem item = layout.addItem();
                    float value = this.getUnhappyChange() * -0.02F;
                    item.setLabel(Translator.getText("Tooltip_food_Unhappiness") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                    if (value > 0.0F) {
                        item.setProgress(value, goodR, goodG, goodB, 1.0F);
                    } else {
                        item.setProgress(value * -1.0F, badR, badG, badB, 1.0F);
                    }
                }
            }

            if (this.getNumberOfPages() != -1) {
                ObjectTooltip.LayoutItem item = layout.addItem();
                int alreadyRead = character.getAlreadyReadPages(this.getFullType());
                item.setLabel(Translator.getText("Tooltip_literature_Number_of_Pages") + ":", 1.0F, 1.0F, 0.8F, 1.0F);
                item.setValue(alreadyRead + " / " + this.getNumberOfPages(), 1.0F, 1.0F, 1.0F, 1.0F);
            }

            boolean illiterate = character.hasTrait(CharacterTrait.ILLITERATE);
            if (this.getLvlSkillTrained() != -1) {
                ObjectTooltip.LayoutItem item = layout.addItem();
                String label = this.getLvlSkillTrained() + "";
                if (this.getLvlSkillTrained() != this.getMaxLevelTrained()) {
                    label = label + "-" + this.getMaxLevelTrained();
                }

                item.setLabel(Translator.getText("Tooltip_Literature_XpMultiplier", label), 1.0F, 1.0F, 0.8F, 1.0F);
            }

            if (this.getLearnedRecipes() != null && !illiterate) {
                for (String recipe : this.getLearnedRecipes()) {
                    ObjectTooltip.LayoutItem item = layout.addItem();
                    String label = Translator.getRecipeName(recipe);
                    item.setLabel(Translator.getText("Tooltip_Literature_LearnedRecipes", label), 1.0F, 1.0F, 0.8F, 1.0F);
                }

                ObjectTooltip.LayoutItem item = layout.addItem();
                String label = Translator.getText("Tooltip_literature_NotBeenRead");
                if (character.getKnownRecipes().containsAll(this.getLearnedRecipes())) {
                    label = Translator.getText("Tooltip_literature_HasBeenRead");
                }

                item.setLabel(label, 1.0F, 1.0F, 0.8F, 1.0F);
                if (character.getKnownRecipes().containsAll(this.getLearnedRecipes())) {
                    CharacterProfessionDefinition characterProfessionDefinition = CharacterProfessionDefinition.getCharacterProfessionDefinition(
                        character.getDescriptor().getCharacterProfession()
                    );
                    CharacterTraits characterTraits = character.getCharacterTraits();
                    List<CharacterTrait> knownTraits = characterTraits.getKnownTraits();
                    int knownProf = 0;
                    int knownTrait = 0;

                    for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
                        String recipe = this.getLearnedRecipes().get(i);
                        if (characterProfessionDefinition.isGrantedRecipe(recipe)) {
                            knownProf++;
                        }

                        for (CharacterTrait characterTrait : knownTraits) {
                            CharacterTraitDefinition trait = CharacterTraitDefinition.getCharacterTraitDefinition(characterTrait);
                            if (trait != null && trait.isGrantedRecipe(recipe)) {
                                knownTrait++;
                            }
                        }
                    }

                    if (knownProf > 0 || knownTrait > 0) {
                        item = layout.addItem();
                        item.setLabel(Translator.getText("Tooltip_literature_AlreadyKnown"), 0.0F, 1.0F, 0.8F, 1.0F);
                    }
                }
            } else if (this.getLearnedRecipes() != null && illiterate) {
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                String labelx = Translator.getText("ContextMenu_Illiterate");
                itemx.setLabel(Translator.getText("Tooltip_Literature_LearnedRecipes", labelx), 1.0F, 1.0F, 0.8F, 1.0F);
            }

            if (modData.rawget("learnedRecipe") != null && !illiterate) {
                String recipe = (String)modData.rawget("learnedRecipe");
                ObjectTooltip.LayoutItem itemx = layout.addItem();
                String labelx = Translator.getRecipeName(recipe);
                itemx.setLabel(Translator.getText("Tooltip_Literature_LearnedRecipes", labelx), 1.0F, 1.0F, 0.8F, 1.0F);
                itemx = layout.addItem();
                String label2 = Translator.getText("Tooltip_literature_NotBeenRead");
                if (tooltipUI.getCharacter().getKnownRecipes().contains(recipe)) {
                    label2 = Translator.getText("Tooltip_literature_HasBeenRead");
                }

                itemx.setLabel(label2, 1.0F, 1.0F, 0.8F, 1.0F);
                if (character.getKnownRecipes().contains(recipe)) {
                    CharacterProfessionDefinition characterProfessionDefinition = CharacterProfessionDefinition.getCharacterProfessionDefinition(
                        character.getDescriptor().getCharacterProfession()
                    );
                    CharacterTraits characterTraits = character.getCharacterTraits();
                    int knownProf = 0;
                    int knownTrait = 0;
                    if (characterProfessionDefinition.isGrantedRecipe(recipe)) {
                        knownProf++;
                    }

                    for (CharacterTrait characterTraitx : characterTraits.getKnownTraits()) {
                        CharacterTraitDefinition trait = CharacterTraitDefinition.getCharacterTraitDefinition(characterTraitx);
                        if (trait != null && trait.isGrantedRecipe(recipe)) {
                            knownTrait++;
                        }
                    }

                    if (knownProf > 0 || knownTrait > 0) {
                        itemx = layout.addItem();
                        itemx.setLabel(Translator.getText("Tooltip_literature_AlreadyKnown"), 0.0F, 1.0F, 0.8F, 1.0F);
                    }
                }
            } else if (modData.rawget("learnedRecipe") != null && illiterate) {
                ObjectTooltip.LayoutItem itemxx = layout.addItem();
                String labelxx = Translator.getText("ContextMenu_Illiterate");
                itemxx.setLabel(Translator.getText("Tooltip_Literature_LearnedRecipes", labelxx), 1.0F, 1.0F, 0.8F, 1.0F);
            }

            if (modData.rawget("literatureTitle") != null) {
                String title = (String)modData.rawget("literatureTitle");
                if (character.isLiteratureRead(title)) {
                    ObjectTooltip.LayoutItem itemxx = layout.addItem();
                    String labelxx = Translator.getText("ContextMenu_RecentlyRead");
                    if (this.hasTag(ItemTag.PICTURE)) {
                        labelxx = Translator.getText("ContextMenu_RecentlySeen");
                    }

                    itemxx.setLabel(labelxx, 1.0F, 1.0F, 0.8F, 1.0F);
                }
            }
        }
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Short, output);
        int numberPageType = 0;
        if (this.numberOfPages >= 127 && this.numberOfPages < 32767) {
            numberPageType = 1;
        } else if (this.numberOfPages >= 32767) {
            numberPageType = 2;
        }

        if (this.numberOfPages != -1) {
            bits.addFlags(1);
            if (numberPageType == 1) {
                bits.addFlags(2);
                output.putShort((short)this.numberOfPages);
            } else if (numberPageType == 2) {
                bits.addFlags(4);
                output.putInt(this.numberOfPages);
            } else {
                output.put((byte)this.numberOfPages);
            }
        }

        if (this.alreadyReadPages != 0) {
            bits.addFlags(8);
            if (numberPageType == 1) {
                output.putShort((short)this.alreadyReadPages);
            } else if (numberPageType == 2) {
                output.putInt(this.alreadyReadPages);
            } else {
                output.put((byte)this.alreadyReadPages);
            }
        }

        if (this.canBeWrite) {
            bits.addFlags(16);
        }

        if (this.customPages != null && !this.customPages.isEmpty()) {
            bits.addFlags(32);
            output.putInt(this.customPages.size());

            for (String page : this.customPages.values()) {
                GameWindow.WriteString(output, page);
            }
        }

        if (this.lockedBy != null) {
            bits.addFlags(64);
            GameWindow.WriteString(output, this.getLockedBy());
        }

        if (this.learnedRecipes != null) {
            bits.addFlags(128);
            output.putShort((short)this.learnedRecipes.size());

            for (String recipe : this.learnedRecipes) {
                GameWindow.WriteString(output, recipe);
            }
        }

        bits.write();
        bits.release();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.numberOfPages = -1;
        this.alreadyReadPages = 0;
        this.canBeWrite = false;
        this.customPages = null;
        this.lockedBy = null;
        this.learnedRecipes = null;
        BitHeaderRead bits;
        if (WorldVersion >= 226) {
            bits = BitHeader.allocRead(BitHeader.HeaderSize.Short, input);
        } else {
            bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, input);
        }

        if (!bits.equals(0)) {
            int numberPageType = 0;
            if (bits.hasFlags(1)) {
                if (bits.hasFlags(2)) {
                    numberPageType = 1;
                    this.numberOfPages = input.getShort();
                } else if (bits.hasFlags(4)) {
                    numberPageType = 2;
                    this.numberOfPages = input.getInt();
                } else {
                    this.numberOfPages = input.get();
                }
            }

            if (bits.hasFlags(8)) {
                if (numberPageType == 1) {
                    this.alreadyReadPages = input.getShort();
                } else if (numberPageType == 2) {
                    this.alreadyReadPages = input.getInt();
                } else {
                    this.alreadyReadPages = input.get();
                }
            }

            this.canBeWrite = bits.hasFlags(16);
            if (bits.hasFlags(32)) {
                int size = input.getInt();
                if (size > 0) {
                    this.customPages = new HashMap<>();

                    for (int i = 0; i < size; i++) {
                        this.customPages.put(i + 1, GameWindow.ReadString(input));
                    }
                }
            }

            if (bits.hasFlags(64)) {
                this.setLockedBy(GameWindow.ReadString(input));
            }

            if (WorldVersion >= 226 && bits.hasFlags(128)) {
                this.learnedRecipes = new ArrayList<>();
                int numRecipes = input.getShort();

                for (int i = 0; i < numRecipes; i++) {
                    this.learnedRecipes.add(GameWindow.ReadString(input));
                }
            }
        }

        bits.release();
    }

    /**
     * @return the boredomChange
     */
    @Override
    public float getBoredomChange() {
        return !this.alreadyRead ? this.boredomChange : 0.0F;
    }

    /**
     * @return the unhappyChange
     */
    @Override
    public float getUnhappyChange() {
        return !this.alreadyRead ? this.unhappyChange : 0.0F;
    }

    /**
     * @return the stressChange
     */
    @Override
    public float getStressChange() {
        return !this.alreadyRead ? this.stressChange : 0.0F;
    }

    public int getNumberOfPages() {
        return this.numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public String getBookName() {
        return this.bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public int getLvlSkillTrained() {
        return this.lvlSkillTrained;
    }

    public void setLvlSkillTrained(int lvlSkillTrained) {
        this.lvlSkillTrained = lvlSkillTrained;
    }

    public int getNumLevelsTrained() {
        return this.numLevelsTrained;
    }

    public void setNumLevelsTrained(int numLevelsTrained) {
        this.numLevelsTrained = numLevelsTrained;
    }

    public int getMaxLevelTrained() {
        return this.getLvlSkillTrained() + this.getNumLevelsTrained() - 1;
    }

    public String getSkillTrained() {
        return this.skillTrained;
    }

    public void setSkillTrained(String skillTrained) {
        this.skillTrained = skillTrained;
    }

    public int getAlreadyReadPages() {
        return this.alreadyReadPages;
    }

    public void setAlreadyReadPages(int alreadyReadPages) {
        this.alreadyReadPages = alreadyReadPages;
    }

    public boolean canBeWrite() {
        return this.canBeWrite;
    }

    public void setCanBeWrite(boolean canBeWrite) {
        this.canBeWrite = canBeWrite;
    }

    public HashMap<Integer, String> getCustomPages() {
        if (this.customPages == null) {
            this.customPages = new HashMap<>();
            this.customPages.put(1, "");
        }

        return this.customPages;
    }

    public void setCustomPages(HashMap<Integer, String> customPages) {
        this.customPages = customPages;
    }

    public void addPage(Integer index, String text) {
        if (text.length() > 16384) {
            text = text.substring(0, Math.min(text.length(), 16384));
        }

        if (this.customPages == null) {
            this.customPages = new HashMap<>();
        }

        this.customPages.put(index, text);
    }

    public String seePage(Integer index) {
        if (this.customPages == null) {
            this.customPages = new HashMap<>();
            this.customPages.put(1, "");
        }

        return this.customPages.get(index);
    }

    public boolean isEmptyPages() {
        if (this.customPages == null) {
            return true;
        } else {
            for (String text : this.customPages.values()) {
                if (!text.equals("")) {
                    return false;
                }
            }

            return true;
        }
    }

    public String getLockedBy() {
        return this.lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public int getPageToWrite() {
        return this.pageToWrite;
    }

    public void setPageToWrite(int pageToWrite) {
        this.pageToWrite = pageToWrite;
    }

    public List<String> getLearnedRecipes() {
        KahluaTable modData = this.getModData();
        if (modData.rawget("learnedRecipe") != null) {
            List<String> learnedRecipes = new ArrayList<>(List.of());
            String recipe = (String)modData.rawget("learnedRecipe");
            learnedRecipes.add(recipe);
            this.setLearnedRecipes(learnedRecipes);
            modData.rawset("learnedRecipe", null);
        }

        return this.learnedRecipes != null ? this.learnedRecipes : this.getScriptItem().getLearnedRecipes();
    }

    public void setLearnedRecipes(List<String> learnedRecipes) {
        this.learnedRecipes = learnedRecipes;
    }

    public String getReadType() {
        return this.getScriptItem().readType;
    }

    public boolean hasRecipe(String recipe) {
        return this.getLearnedRecipes() != null && this.getLearnedRecipes().contains(recipe)
            ? true
            : this.getModData().rawget("learnedRecipe") instanceof String learnedRecipe && learnedRecipe.equals(recipe);
    }

    public boolean containsKnownRecipe(IsoGameCharacter chr) {
        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            if (chr.getKnownRecipes().contains(recipe)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getKnownRecipes(IsoGameCharacter chr) {
        ArrayList<String> knownRecipes = new ArrayList<>();

        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            if (chr.getKnownRecipes().contains(recipe)) {
                knownRecipes.add(recipe);
            }
        }

        return knownRecipes;
    }

    public boolean containsCraftRecipe() {
        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            CraftRecipe craftRecipe = ScriptManager.instance.getCraftRecipe(recipe);
            if (craftRecipe != null) {
                return true;
            }
        }

        return false;
    }

    public boolean containsBuildRecipe() {
        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            CraftRecipe buildRecipe = ScriptManager.instance.getBuildableRecipe(recipe);
            if (buildRecipe != null) {
                return true;
            }
        }

        return false;
    }

    public boolean containsGrowingSeason() {
        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesSeasonRecipeExist"), recipe)) {
                return true;
            }
        }

        return false;
    }

    public boolean containsCraftOrBuildRecipe() {
        return this.containsCraftRecipe() || this.containsBuildRecipe();
    }

    public boolean containsMiscRecipe() {
        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesMiscRecipeExist"), recipe)) {
                return true;
            }
        }

        return false;
    }

    public List<String> getKnownMiscRecipes(IsoGameCharacter chr) {
        ArrayList<String> knownRecipes = new ArrayList<>();

        for (int i = 0; i < this.getLearnedRecipes().size(); i++) {
            String recipe = this.getLearnedRecipes().get(i);
            if (LuaManager.caller.protectedCallBoolean(LuaManager.thread, LuaManager.getFunctionObject("doesMiscRecipeExist"), recipe)
                && chr.getKnownRecipes().contains(recipe)) {
                knownRecipes.add(recipe);
            }
        }

        return knownRecipes;
    }
}
