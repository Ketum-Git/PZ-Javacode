// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.WornItems.BodyLocations;
import zombie.characters.WornItems.WornItems;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.ObservationFactory;
import zombie.core.Color;
import zombie.core.ImmutableColor;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.IHumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoWorld;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;

@UsedFromLua
public final class SurvivorDesc implements IHumanVisual {
    private static int idCount;
    public static final ArrayList<Color> TrouserCommonColors = new ArrayList<>();
    public static final ArrayList<ImmutableColor> HairCommonColors = new ArrayList<>();
    private final HumanVisual humanVisual = new HumanVisual(this);
    private final WornItems wornItems = new WornItems(BodyLocations.getGroup("Human"));
    private final SurvivorGroup group = new SurvivorGroup();
    private final HashMap<PerkFactory.Perk, Integer> xpBoostMap = new HashMap<>();
    private CharacterProfession characterProfession = CharacterProfession.UNEMPLOYED;
    private String forename = "None";
    private int id;
    private IsoGameCharacter instance;
    private boolean female = true;
    private String surname = "None";
    private String inventoryScript;
    private String torso = "Base_Torso";
    private final HashMap<Integer, Integer> metCount = new HashMap<>();
    private float bravery = 1.0F;
    private float loner;
    private float aggressiveness = 1.0F;
    private float compassion = 1.0F;
    private float temper;
    private float friendliness;
    private float favourindoors;
    private float loyalty;
    private final ArrayList<String> extra = new ArrayList<>();
    private final ArrayList<ObservationFactory.Observation> observations = new ArrayList<>(0);
    private SurvivorFactory.SurvivorType type = SurvivorFactory.SurvivorType.Neutral;
    private String voicePrefix = "VoiceFemale";
    private float voicePitch;
    private int voiceType;
    private boolean dead;
    private KahluaTable metaTable;

    @Override
    public HumanVisual getHumanVisual() {
        return this.humanVisual;
    }

    @Override
    public void getItemVisuals(ItemVisuals itemVisuals) {
        this.wornItems.getItemVisuals(itemVisuals);
    }

    @Override
    public boolean isFemale() {
        return this.female;
    }

    @Override
    public boolean isZombie() {
        return false;
    }

    @Override
    public boolean isSkeleton() {
        return false;
    }

    public WornItems getWornItems() {
        return this.wornItems;
    }

    public void setWornItem(ItemBodyLocation itemBodyLocation, InventoryItem item) {
        this.wornItems.setItem(itemBodyLocation, item);
    }

    public InventoryItem getWornItem(ItemBodyLocation itemBodyLocation) {
        return this.wornItems.getItem(itemBodyLocation);
    }

    public void dressInNamedOutfit(String outfitName) {
        ItemVisuals itemVisuals = new ItemVisuals();
        this.getHumanVisual().dressInNamedOutfit(outfitName, itemVisuals);
        this.getWornItems().setFromItemVisuals(itemVisuals);
    }

    public String getVoicePrefix() {
        return this.voicePrefix;
    }

    public void setVoicePrefix(String voicePrefix) {
        this.voicePrefix = voicePrefix;
    }

    public int getVoiceType() {
        return this.voiceType;
    }

    public void setVoiceType(int voiceType) {
        this.voiceType = voiceType;
    }

    public float getVoicePitch() {
        return this.voicePitch;
    }

    public void setVoicePitch(float voicePitch) {
        this.voicePitch = voicePitch;
    }

    public SurvivorGroup getGroup() {
        return this.group;
    }

    public boolean isLeader() {
        return this.group.getLeader() == this;
    }

    /**
     * @return the IDCount
     */
    public static int getIDCount() {
        return idCount;
    }

    public void setProfessionSkills(CharacterProfessionDefinition characterProfessionDefinition) {
        this.getXPBoostMap().clear();
        this.getXPBoostMap().putAll(characterProfessionDefinition.getXpBoosts());
    }

    public HashMap<PerkFactory.Perk, Integer> getXPBoostMap() {
        return this.xpBoostMap;
    }

    public KahluaTable getMeta() {
        if (this.metaTable == null) {
            this.metaTable = (KahluaTable)LuaManager.caller.pcall(LuaManager.thread, LuaManager.env.rawget("createMetaSurvivor"), this)[1];
        }

        return this.metaTable;
    }

    public int getCalculatedToughness() {
        this.metaTable = this.getMeta();
        KahluaTable tab = (KahluaTable)LuaManager.env.rawget("MetaSurvivor");
        Double tough = (Double)LuaManager.caller.pcall(LuaManager.thread, tab.rawget("getCalculatedToughness"), this.metaTable)[1];
        return tough.intValue();
    }

    /**
     * 
     * @param aIDCount the IDCount to set
     */
    public static void setIDCount(int aIDCount) {
        idCount = aIDCount;
    }

    public boolean isDead() {
        return this.dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public SurvivorDesc() {
        this.id = idCount++;
        IsoWorld.instance.survivorDescriptors.put(this.id, this);
        this.doStats();
    }

    public SurvivorDesc(boolean bNew) {
        this.id = idCount++;
        this.doStats();
    }

    public SurvivorDesc(SurvivorDesc other) {
        this.aggressiveness = other.aggressiveness;
        this.dead = other.dead;
        this.female = other.female;
        this.bravery = other.bravery;
        this.compassion = other.compassion;
        this.extra.addAll(other.extra);
        this.favourindoors = other.favourindoors;
        this.forename = other.forename;
        this.friendliness = other.friendliness;
        this.inventoryScript = other.inventoryScript;
        this.loner = other.loner;
        this.loyalty = other.loyalty;
        this.characterProfession = other.characterProfession;
        this.surname = other.surname;
        this.temper = other.temper;
        this.torso = other.torso;
        this.type = other.type;
        this.voicePitch = other.voicePitch;
        this.voiceType = other.voiceType;
        this.voicePrefix = other.voicePrefix;
    }

    public void meet(SurvivorDesc desc) {
        if (this.metCount.containsKey(desc.id)) {
            this.metCount.put(desc.id, this.metCount.get(desc.id) + 1);
        } else {
            this.metCount.put(desc.id, 1);
        }

        if (desc.metCount.containsKey(this.id)) {
            desc.metCount.put(this.id, desc.metCount.get(this.id) + 1);
        } else {
            desc.metCount.put(this.id, 1);
        }
    }

    public boolean hasObservation(String o) {
        for (int n = 0; n < this.observations.size(); n++) {
            if (o.equals(this.observations.get(n).getTraitID())) {
                return true;
            }
        }

        return false;
    }

    private void savePerk(ByteBuffer output, PerkFactory.Perk perk) throws IOException {
        GameWindow.WriteStringUTF(output, perk == null ? "" : perk.getId());
    }

    private PerkFactory.Perk loadPerk(ByteBuffer input, int WorldVersion) throws IOException {
        String perkName = GameWindow.ReadStringUTF(input);
        PerkFactory.Perk perk = PerkFactory.Perks.FromString(perkName);
        return perk == PerkFactory.Perks.MAX ? null : perk;
    }

    public void load(ByteBuffer input, int WorldVersion, IsoGameCharacter chr) throws IOException {
        this.id = input.getInt();
        IsoWorld.instance.survivorDescriptors.put(this.id, this);
        this.forename = GameWindow.ReadString(input);
        this.surname = GameWindow.ReadString(input);
        this.torso = GameWindow.ReadString(input);
        this.female = input.getInt() == 1;
        this.characterProfession = CharacterProfession.get(ResourceLocation.of(GameWindow.ReadString(input)));
        this.doStats();
        if (idCount < this.id) {
            idCount = this.id;
        }

        this.extra.clear();
        if (input.getInt() == 1) {
            int size = input.getInt();

            for (int i = 0; i < size; i++) {
                String extr = GameWindow.ReadString(input);
                this.extra.add(extr);
            }
        }

        int size = input.getInt();

        for (int i = 0; i < size; i++) {
            PerkFactory.Perk perk = this.loadPerk(input, WorldVersion);
            int level = input.getInt();
            if (perk != null) {
                this.getXPBoostMap().put(perk, level);
            }
        }

        if (WorldVersion >= 208) {
            this.voicePrefix = GameWindow.ReadString(input);
            this.voicePitch = input.getFloat();
            this.voiceType = input.getInt();
        } else {
            this.voicePrefix = this.female ? "VoiceFemale" : "VoiceMale";
            this.voicePitch = 0.0F;
            this.voiceType = Rand.Next(3);
        }

        this.instance = chr;
    }

    public void save(ByteBuffer output) throws IOException {
        output.putInt(this.id);
        GameWindow.WriteString(output, this.forename);
        GameWindow.WriteString(output, this.surname);
        GameWindow.WriteString(output, this.torso);
        output.putInt(this.female ? 1 : 0);
        GameWindow.WriteString(output, Registries.CHARACTER_PROFESSION.getLocation(this.characterProfession).toString());
        if (!this.extra.isEmpty()) {
            output.putInt(1);
            output.putInt(this.extra.size());

            for (int i = 0; i < this.extra.size(); i++) {
                String extr = this.extra.get(i);
                GameWindow.WriteString(output, extr);
            }
        } else {
            output.putInt(0);
        }

        output.putInt(this.getXPBoostMap().size());

        for (Entry<PerkFactory.Perk, Integer> entry : this.getXPBoostMap().entrySet()) {
            this.savePerk(output, entry.getKey());
            output.putInt(entry.getValue());
        }

        GameWindow.WriteString(output, this.voicePrefix);
        output.putFloat(this.voicePitch);
        output.putInt(this.voiceType);
    }

    public String getDescription(String newStr) {
        String s = "SurvivorDesc [" + newStr;
        s = s + "ID=" + this.id + " | " + newStr;
        s = s + "forename=" + this.forename + " | " + newStr;
        s = s + "surname=" + this.surname + " | " + newStr;
        s = s + "torso=" + this.torso + " | " + newStr;
        s = s + "bFemale=" + this.female + " | " + newStr;
        s = s + "Profession=" + this.characterProfession + " | " + newStr;
        if (!this.extra.isEmpty()) {
            s = s + "extra=";

            for (int i = 0; i < this.extra.size(); i++) {
                s = s + this.extra.get(i) + ",";
            }

            s = s + " | " + newStr;
        }

        if (!this.getXPBoostMap().isEmpty()) {
            s = s + "XPBoost=" + newStr;

            for (Entry<PerkFactory.Perk, Integer> entry : this.getXPBoostMap().entrySet()) {
                s = s + entry.getKey().getId() + "(" + entry.getKey().getName() + "):" + entry.getValue() + ", " + newStr;
            }

            s = s + " ] ";
        }

        s = s + "voicePrefix=" + this.voicePrefix + " | " + newStr;
        s = s + "voicePitch=" + this.voicePitch + " | " + newStr;
        return s + "voiceType=" + this.voiceType + " | " + newStr;
    }

    public void addObservation(String obv) {
        ObservationFactory.Observation o = ObservationFactory.getObservation(obv);
        if (o != null) {
            this.observations.add(o);
        }
    }

    private void doStats() {
        this.bravery = Rand.Next(2) == 0 ? 10.0F : 0.0F;
        this.aggressiveness = Rand.Next(2) == 0 ? 10.0F : 0.0F;
        this.compassion = 10.0F - this.aggressiveness;
        this.loner = Rand.Next(2) == 0 ? 10.0F : 0.0F;
        this.temper = Rand.Next(2) == 0 ? 10.0F : 0.0F;
        this.friendliness = 10.0F - this.loner;
        this.favourindoors = Rand.Next(2) == 0 ? 10.0F : 0.0F;
        this.loyalty = Rand.Next(2) == 0 ? 10.0F : 0.0F;
    }

    public int getMetCount(SurvivorDesc descriptor) {
        return this.metCount.containsKey(descriptor.id) ? this.metCount.get(descriptor.id) : 0;
    }

    public String getFullname() {
        return this.forename + " " + this.surname;
    }

    /**
     * @return the forename
     */
    public String getForename() {
        return this.forename;
    }

    /**
     * 
     * @param forename the forename to set
     */
    public void setForename(String forename) {
        this.forename = forename;
    }

    /**
     * @return the ID
     */
    public int getID() {
        return this.id;
    }

    /**
     * 
     * @param id the ID to set
     */
    public void setID(int id) {
        this.id = id;
    }

    /**
     * @return the Instance
     */
    public IsoGameCharacter getInstance() {
        return this.instance;
    }

    /**
     * 
     * @param Instance the Instance to set
     */
    public void setInstance(IsoGameCharacter Instance) {
        this.instance = Instance;
    }

    /**
     * @return the surname
     */
    public String getSurname() {
        return this.surname;
    }

    /**
     * 
     * @param surname the surname to set
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
     * @return the InventoryScript
     */
    public String getInventoryScript() {
        return this.inventoryScript;
    }

    /**
     * 
     * @param InventoryScript the InventoryScript to set
     */
    public void setInventoryScript(String InventoryScript) {
        this.inventoryScript = InventoryScript;
    }

    /**
     * @return the torso
     */
    public String getTorso() {
        return this.torso;
    }

    /**
     * 
     * @param torso the torso to set
     */
    public void setTorso(String torso) {
        this.torso = torso;
    }

    /**
     * @return the MetCount
     */
    public HashMap<Integer, Integer> getMetCount() {
        return this.metCount;
    }

    /**
     * @return the bravery
     */
    public float getBravery() {
        return this.bravery;
    }

    /**
     * 
     * @param bravery the bravery to set
     */
    public void setBravery(float bravery) {
        this.bravery = bravery;
    }

    /**
     * @return the loner
     */
    public float getLoner() {
        return this.loner;
    }

    /**
     * 
     * @param loner the loner to set
     */
    public void setLoner(float loner) {
        this.loner = loner;
    }

    /**
     * @return the aggressiveness
     */
    public float getAggressiveness() {
        return this.aggressiveness;
    }

    /**
     * 
     * @param aggressiveness the aggressiveness to set
     */
    public void setAggressiveness(float aggressiveness) {
        this.aggressiveness = aggressiveness;
    }

    /**
     * @return the compassion
     */
    public float getCompassion() {
        return this.compassion;
    }

    /**
     * 
     * @param compassion the compassion to set
     */
    public void setCompassion(float compassion) {
        this.compassion = compassion;
    }

    /**
     * @return the temper
     */
    public float getTemper() {
        return this.temper;
    }

    /**
     * 
     * @param temper the temper to set
     */
    public void setTemper(float temper) {
        this.temper = temper;
    }

    /**
     * @return the friendliness
     */
    public float getFriendliness() {
        return this.friendliness;
    }

    /**
     * 
     * @param friendliness the friendliness to set
     */
    public void setFriendliness(float friendliness) {
        this.friendliness = friendliness;
    }

    /**
     * @return the favourindoors
     */
    public float getFavourindoors() {
        return this.favourindoors;
    }

    /**
     * 
     * @param favourindoors the favourindoors to set
     */
    public void setFavourindoors(float favourindoors) {
        this.favourindoors = favourindoors;
    }

    /**
     * @return the loyalty
     */
    public float getLoyalty() {
        return this.loyalty;
    }

    /**
     * 
     * @param loyalty the loyalty to set
     */
    public void setLoyalty(float loyalty) {
        this.loyalty = loyalty;
    }

    public boolean isCharacterProfession(CharacterProfession characterProfession) {
        return this.characterProfession == characterProfession;
    }

    public CharacterProfession getCharacterProfession() {
        return this.characterProfession;
    }

    public void setCharacterProfession(CharacterProfession characterProfession) {
        this.characterProfession = characterProfession;
    }

    public boolean isAggressive() {
        for (ObservationFactory.Observation observation : this.observations) {
            if ("Aggressive".equals(observation.getTraitID())) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<ObservationFactory.Observation> getObservations() {
        return this.observations;
    }

    public boolean isFriendly() {
        for (ObservationFactory.Observation observation : this.observations) {
            if ("Friendly".equals(observation.getTraitID())) {
                return true;
            }
        }

        return false;
    }

    public SurvivorFactory.SurvivorType getType() {
        return this.type;
    }

    public void setType(SurvivorFactory.SurvivorType type) {
        this.type = type;
    }

    public void setFemale(boolean bFemale) {
        this.female = bFemale;
    }

    public ArrayList<String> getExtras() {
        return this.extra;
    }

    public ArrayList<ImmutableColor> getCommonHairColor() {
        return HairCommonColors;
    }

    public static void addTrouserColor(ColorInfo color) {
        TrouserCommonColors.add(color.toColor());
    }

    public static void addHairColor(ColorInfo color) {
        HairCommonColors.add(color.toImmutableColor());
    }

    public static Color getRandomSkinColor() {
        return OutfitRNG.Next(3) == 0
            ? new Color(OutfitRNG.Next(0.5F, 0.6F), OutfitRNG.Next(0.3F, 0.4F), OutfitRNG.Next(0.15F, 0.23F))
            : new Color(OutfitRNG.Next(0.9F, 1.0F), OutfitRNG.Next(0.75F, 0.88F), OutfitRNG.Next(0.45F, 0.58F));
    }
}
