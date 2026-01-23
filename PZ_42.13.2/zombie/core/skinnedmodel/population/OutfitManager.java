// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.TreeMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.ZomboidFileSystem;
import zombie.asset.AssetPath;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.gameStates.ChooseGameInfo;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.ItemType;
import zombie.util.PZXmlParserException;
import zombie.util.PZXmlUtil;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@XmlRootElement(name = "outfitManager")
public class OutfitManager {
    @XmlElement(name = "m_MaleOutfits")
    public ArrayList<Outfit> maleOutfits = new ArrayList<>();
    @XmlElement(name = "m_FemaleOutfits")
    public ArrayList<Outfit> femaleOutfits = new ArrayList<>();
    @XmlTransient
    public static OutfitManager instance;
    @XmlTransient
    private final Hashtable<String, OutfitManager.ClothingItemEntry> cachedClothingItems = new Hashtable<>();
    @XmlTransient
    private final ArrayList<IClothingItemListener> clothingItemListeners = new ArrayList<>();
    @XmlTransient
    private final TreeMap<String, Outfit> femaleOutfitMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    @XmlTransient
    private final TreeMap<String, Outfit> maleOutfitMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("OutfitManager Already Initialized.");
        } else {
            instance = tryParse("game", "", "media/clothing/clothing.xml");
            if (instance != null) {
                instance.loaded();
            }
        }
    }

    public static void Reset() {
        if (instance != null) {
            instance.unload();
            instance = null;
        }
    }

    private void loaded() {
        for (String modID : ZomboidFileSystem.instance.getModIDs()) {
            ChooseGameInfo.Mod mod = ChooseGameInfo.getAvailableModDetails(modID);
            if (mod != null) {
                OutfitManager outfitManager = tryParse(modID, mod.getVersionDir(), "media/clothing/clothing.xml");
                if (outfitManager != null) {
                    for (Outfit outfitMod : outfitManager.maleOutfits) {
                        Outfit outfit = this.FindMaleOutfit(outfitMod.name);
                        if (outfit == null) {
                            this.maleOutfits.add(outfitMod);
                        } else {
                            if (DebugLog.isEnabled(DebugType.Clothing)) {
                                DebugLog.Clothing.println("mod \"%s\" overrides male outfit \"%s\"", modID, outfitMod.name);
                            }

                            this.maleOutfits.set(this.maleOutfits.indexOf(outfit), outfitMod);
                        }

                        this.maleOutfitMap.put(outfitMod.name, outfitMod);
                    }

                    for (Outfit outfitMod : outfitManager.femaleOutfits) {
                        Outfit outfit = this.FindFemaleOutfit(outfitMod.name);
                        if (outfit == null) {
                            this.femaleOutfits.add(outfitMod);
                        } else {
                            if (DebugLog.isEnabled(DebugType.Clothing)) {
                                DebugLog.Clothing.println("mod \"%s\" overrides female outfit \"%s\"", modID, outfitMod.name);
                            }

                            this.femaleOutfits.set(this.femaleOutfits.indexOf(outfit), outfitMod);
                        }

                        this.femaleOutfitMap.put(outfitMod.name, outfitMod);
                    }
                } else {
                    outfitManager = tryParse(modID, mod.getCommonDir(), "media/clothing/clothing.xml");
                    if (outfitManager != null) {
                        for (Outfit outfitMod : outfitManager.maleOutfits) {
                            Outfit outfit = this.FindMaleOutfit(outfitMod.name);
                            if (outfit == null) {
                                this.maleOutfits.add(outfitMod);
                            } else {
                                if (DebugLog.isEnabled(DebugType.Clothing)) {
                                    DebugLog.Clothing.println("mod \"%s\" overrides male outfit \"%s\"", modID, outfitMod.name);
                                }

                                this.maleOutfits.set(this.maleOutfits.indexOf(outfit), outfitMod);
                            }

                            this.maleOutfitMap.put(outfitMod.name, outfitMod);
                        }

                        for (Outfit outfitMod : outfitManager.femaleOutfits) {
                            Outfit outfit = this.FindFemaleOutfit(outfitMod.name);
                            if (outfit == null) {
                                this.femaleOutfits.add(outfitMod);
                            } else {
                                if (DebugLog.isEnabled(DebugType.Clothing)) {
                                    DebugLog.Clothing.println("mod \"%s\" overrides female outfit \"%s\"", modID, outfitMod.name);
                                }

                                this.femaleOutfits.set(this.femaleOutfits.indexOf(outfit), outfitMod);
                            }

                            this.femaleOutfitMap.put(outfitMod.name, outfitMod);
                        }
                    }
                }
            }
        }

        DebugFileWatcher.instance
            .add(new PredicatedFileWatcher(ZomboidFileSystem.instance.getString("media/clothing/clothing.xml"), entryKey -> onClothingXmlFileChanged()));
        this.loadAllClothingItems();

        for (Outfit outfit : this.maleOutfits) {
            outfit.immutable = true;

            for (ClothingItemReference itemRef : outfit.items) {
                itemRef.immutable = true;
            }
        }

        for (Outfit outfit : this.femaleOutfits) {
            outfit.immutable = true;

            for (ClothingItemReference itemRef : outfit.items) {
                itemRef.immutable = true;
            }
        }

        Collections.shuffle(this.maleOutfits);
        Collections.shuffle(this.femaleOutfits);
    }

    private static void onClothingXmlFileChanged() {
        DebugLog.Clothing.println("OutfitManager.onClothingXmlFileChanged> Detected change in media/clothing/clothing.xml");
        Reload();
    }

    public static void Reload() {
        DebugLog.Clothing.println("Reloading OutfitManager");
        OutfitManager oldInstance = instance;
        instance = tryParse("game", "", "media/clothing/clothing.xml");
        if (instance != null) {
            instance.loaded();
        }

        if (oldInstance != null && instance != null) {
            instance.onReloaded(oldInstance);
        }
    }

    private void onReloaded(OutfitManager oldInstance) {
        PZArrayUtil.copy(this.clothingItemListeners, oldInstance.clothingItemListeners);
        oldInstance.unload();
        this.loadAllClothingItems();
    }

    private void unload() {
        for (OutfitManager.ClothingItemEntry entry : this.cachedClothingItems.values()) {
            DebugFileWatcher.instance.remove(entry.fileWatcher);
        }

        this.cachedClothingItems.clear();
        this.clothingItemListeners.clear();
    }

    public void addClothingItemListener(IClothingItemListener listener) {
        if (listener != null) {
            if (!this.clothingItemListeners.contains(listener)) {
                this.clothingItemListeners.add(listener);
            }
        }
    }

    public void removeClothingItemListener(IClothingItemListener listener) {
        this.clothingItemListeners.remove(listener);
    }

    private void invokeClothingItemChangedEvent(String itemGuid) {
        for (IClothingItemListener listener : this.clothingItemListeners) {
            listener.clothingItemChanged(itemGuid);
        }
    }

    public Outfit GetRandomOutfit(boolean female) {
        Outfit randomOutfit;
        if (female) {
            randomOutfit = PZArrayUtil.pickRandom(this.femaleOutfits);
        } else {
            randomOutfit = PZArrayUtil.pickRandom(this.maleOutfits);
        }

        return randomOutfit;
    }

    public Outfit GetRandomNonSillyOutfit(boolean female) {
        Outfit randomOutfit;
        do {
            if (female) {
                randomOutfit = PZArrayUtil.pickRandom(this.femaleOutfits);
            } else {
                randomOutfit = PZArrayUtil.pickRandom(this.maleOutfits);
            }
        } while (randomOutfit.name.contains("Santa") || randomOutfit.name.contains("Spiffo") || randomOutfit.name.contains("Naked"));

        return randomOutfit;
    }

    public Outfit GetRandomNonProfessionalOutfit(boolean female) {
        String outfit = "Generic0" + (Rand.Next(5) + 1);
        if (Rand.NextBool(4)) {
            if (female) {
                int rand = Rand.Next(3);
                switch (rand) {
                    case 0:
                        outfit = "Mannequin1";
                        break;
                    case 1:
                        outfit = "Mannequin2";
                        break;
                    case 2:
                        outfit = "Classy";
                }
            } else {
                int rand = Rand.Next(3);
                switch (rand) {
                    case 0:
                        outfit = "Classy";
                        break;
                    case 1:
                        outfit = "Tourist";
                        break;
                    case 2:
                        outfit = "MallSecurity";
                }
            }
        }

        return this.GetSpecificOutfit(female, outfit);
    }

    public Outfit GetSpecificOutfit(boolean female, String outfitName) {
        Outfit specificOutfit;
        if (female) {
            specificOutfit = this.FindFemaleOutfit(outfitName);
        } else {
            specificOutfit = this.FindMaleOutfit(outfitName);
        }

        return specificOutfit;
    }

    private static OutfitManager tryParse(String modID, String modDir, String filename) {
        try {
            return parse(modID, modDir, filename);
        } catch (PZXmlParserException var4) {
            var4.printStackTrace();
            return null;
        }
    }

    private static OutfitManager parse(String modID, String modDir, String filename) throws PZXmlParserException {
        if ("game".equals(modID)) {
            filename = ZomboidFileSystem.instance.base.canonicalFile.getAbsolutePath()
                + File.separator
                + ZomboidFileSystem.processFilePath(filename, File.separatorChar);
        } else {
            filename = modDir + File.separator + ZomboidFileSystem.processFilePath(filename, File.separatorChar);
        }

        if (!new File(filename).exists()) {
            return null;
        } else {
            OutfitManager rootElement = PZXmlUtil.parse(OutfitManager.class, filename);
            if (rootElement != null) {
                PZArrayUtil.forEach(rootElement.maleOutfits, outfit -> outfit.setModID(modID));
                PZArrayUtil.forEach(rootElement.femaleOutfits, outfit -> outfit.setModID(modID));
                PZArrayUtil.forEach(rootElement.maleOutfits, outfit -> rootElement.maleOutfitMap.put(outfit.name, outfit));
                PZArrayUtil.forEach(rootElement.femaleOutfits, outfit -> rootElement.femaleOutfitMap.put(outfit.name, outfit));
            }

            return rootElement;
        }
    }

    private static void tryWrite(OutfitManager out, String filename) {
        try {
            write(out, filename);
        } catch (IOException | JAXBException var3) {
            var3.printStackTrace();
        }
    }

    private static void write(OutfitManager out, String filename) throws IOException, JAXBException {
        try (FileOutputStream adrFile = new FileOutputStream(filename)) {
            JAXBContext ctx = JAXBContext.newInstance(OutfitManager.class);
            Marshaller ma = ctx.createMarshaller();
            ma.setProperty("jaxb.formatted.output", Boolean.TRUE);
            ma.marshal(out, adrFile);
        }
    }

    public Outfit FindMaleOutfit(String outfitName) {
        return this.maleOutfitMap.get(outfitName);
    }

    public Outfit FindFemaleOutfit(String outfitName) {
        return this.femaleOutfitMap.get(outfitName);
    }

    private Outfit FindOutfit(ArrayList<Outfit> outfitsList, String outfitName) {
        Outfit foundOutfit = null;

        for (int i = 0; i < outfitsList.size(); i++) {
            Outfit o = outfitsList.get(i);
            if (o.name.equalsIgnoreCase(outfitName)) {
                foundOutfit = o;
                break;
            }
        }

        return foundOutfit;
    }

    public ClothingItem getClothingItem(String itemGUID) {
        String filePath = ZomboidFileSystem.instance.getFilePathFromGuid(itemGUID);
        if (filePath == null) {
            return null;
        } else {
            OutfitManager.ClothingItemEntry cachedElement = this.cachedClothingItems.get(itemGUID);
            if (cachedElement == null) {
                cachedElement = new OutfitManager.ClothingItemEntry();
                cachedElement.filePath = filePath;
                cachedElement.guid = itemGUID;
                cachedElement.item = null;
                this.cachedClothingItems.put(itemGUID, cachedElement);
            }

            if (cachedElement.item != null) {
                cachedElement.item.guid = itemGUID;
                return cachedElement.item;
            } else {
                try {
                    String absolutePath = ZomboidFileSystem.instance.resolveFileOrGUID(filePath);
                    cachedElement.item = (ClothingItem)ClothingItemAssetManager.instance.load(new AssetPath(absolutePath));
                    cachedElement.item.mame = this.extractClothingItemName(filePath);
                    cachedElement.item.guid = itemGUID;
                } catch (Exception var6) {
                    System.err.println("Failed to load ClothingItem: " + filePath);
                    ExceptionLogger.logException(var6);
                    return null;
                }

                if (cachedElement.fileWatcher == null) {
                    OutfitManager.ClothingItemEntry f_cachedElement = cachedElement;
                    String filePath1 = f_cachedElement.filePath;
                    filePath1 = ZomboidFileSystem.instance.getString(filePath1);
                    cachedElement.fileWatcher = new PredicatedFileWatcher(filePath1, entryKey -> this.onClothingItemFileChanged(f_cachedElement));
                    DebugFileWatcher.instance.add(cachedElement.fileWatcher);
                }

                return cachedElement.item;
            }
        }
    }

    private String extractClothingItemName(String filePath) {
        String itemName = StringUtils.trimPrefix(filePath, "media/clothing/clothingItems/");
        return StringUtils.trimSuffix(itemName, ".xml");
    }

    private void onClothingItemFileChanged(OutfitManager.ClothingItemEntry cachedElement) {
        ClothingItemAssetManager.instance.reload(cachedElement.item);
    }

    public void onClothingItemStateChanged(ClothingItem clothingItem) {
        if (clothingItem.isReady()) {
            this.invokeClothingItemChangedEvent(clothingItem.guid);
        }
    }

    public void loadAllClothingItems() {
        ArrayList<Item> allItems = ScriptManager.instance.getAllItems();

        for (int i = 0; i < allItems.size(); i++) {
            Item scriptItem = allItems.get(i);
            if (scriptItem.replacePrimaryHand != null) {
                String guid = ZomboidFileSystem.instance
                    .getGuidFromFilePath("media/clothing/clothingItems/" + scriptItem.replacePrimaryHand.clothingItemName + ".xml");
                if (guid != null) {
                    scriptItem.replacePrimaryHand.clothingItem = this.getClothingItem(guid);
                }
            }

            if (scriptItem.replaceSecondHand != null) {
                String guid = ZomboidFileSystem.instance
                    .getGuidFromFilePath("media/clothing/clothingItems/" + scriptItem.replaceSecondHand.clothingItemName + ".xml");
                if (guid != null) {
                    scriptItem.replaceSecondHand.clothingItem = this.getClothingItem(guid);
                }
            }

            if (!StringUtils.isNullOrWhitespace(scriptItem.getClothingItem())) {
                String guid = ZomboidFileSystem.instance.getGuidFromFilePath("media/clothing/clothingItems/" + scriptItem.getClothingItem() + ".xml");
                if (guid != null) {
                    ClothingItem clothingItem = this.getClothingItem(guid);
                    scriptItem.setClothingItemAsset(clothingItem);
                }
            }
        }
    }

    public boolean isLoadingClothingItems() {
        for (OutfitManager.ClothingItemEntry entry : this.cachedClothingItems.values()) {
            if (entry.item.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public void debugOutfits() {
        this.debugOutfits(this.femaleOutfits);
        this.debugOutfits(this.maleOutfits);
    }

    private void debugOutfits(ArrayList<Outfit> outfits) {
        for (Outfit outfit : outfits) {
            this.debugOutfit(outfit);
        }
    }

    private void debugOutfit(Outfit outfit) {
        ItemBodyLocation bagLocation = null;

        for (ClothingItemReference itemRef : outfit.items) {
            ClothingItem clothingItem = this.getClothingItem(itemRef.itemGuid);
            if (clothingItem != null && !clothingItem.isEmpty()) {
                String itemType = ScriptManager.instance.getItemTypeForClothingItem(clothingItem.mame);
                if (itemType != null) {
                    Item scriptItem = ScriptManager.instance.getItem(itemType);
                    if (scriptItem != null && scriptItem.isItemType(ItemType.CONTAINER)) {
                        ItemBodyLocation location = scriptItem.getBodyLocation() != null ? scriptItem.canBeEquipped : scriptItem.getBodyLocation();
                        if (bagLocation != null && bagLocation.equals(location)) {
                            DebugLog.Clothing.warn("outfit \"%s\" has multiple bags", outfit.name);
                        }

                        bagLocation = location;
                    }
                }
            }
        }
    }

    private static final class ClothingItemEntry {
        public ClothingItem item;
        public String guid;
        public String filePath;
        public PredicatedFileWatcher fileWatcher;
    }
}
