// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.characters.ZombiesStageDefinitions;
import zombie.characters.ZombiesZoneDefinition;
import zombie.characters.AttachedItems.AttachedWeaponDefinitions;
import zombie.characters.WornItems.WornItem;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.Clothing;
import zombie.iso.IsoWorld;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.randomizedWorld.randomizedVehicleStory.RandomizedVehicleStoryBase;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemType;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public class PersistentOutfits {
    public static final PersistentOutfits instance = new PersistentOutfits();
    public static final int INVALID_ID = 0;
    public static final int FEMALE_BIT = Integer.MIN_VALUE;
    public static final int NO_HAT_BIT = 32768;
    private static final int FILE_VERSION_1 = 1;
    private static final int FILE_VERSION_LATEST = 1;
    private static final byte[] FILE_MAGIC = new byte[]{80, 83, 84, 90};
    private static final int NUM_SEEDS = 500;
    private final long[] seeds = new long[500];
    private final ArrayList<String> outfitNames = new ArrayList<>();
    private final PersistentOutfits.DataList all = new PersistentOutfits.DataList();
    private final PersistentOutfits.DataList female = new PersistentOutfits.DataList();
    private final PersistentOutfits.DataList male = new PersistentOutfits.DataList();
    private final TreeMap<String, PersistentOutfits.Data> outfitToData = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeMap<String, PersistentOutfits.Data> outfitToFemale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final TreeMap<String, PersistentOutfits.Data> outfitToMale = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final ItemVisuals tempItemVisuals = new ItemVisuals();

    public void init() {
        this.all.clear();
        this.female.clear();
        this.male.clear();
        this.outfitToData.clear();
        this.outfitToFemale.clear();
        this.outfitToMale.clear();
        this.outfitNames.clear();
        if (!GameClient.client) {
            for (int i = 0; i < 500; i++) {
                this.seeds[i] = Rand.Next(Integer.MAX_VALUE);
            }
        }

        this.initOutfitList(OutfitManager.instance.femaleOutfits, true);
        this.initOutfitList(OutfitManager.instance.maleOutfits, false);
        this.registerCustomOutfits();
        if (!GameClient.client) {
            this.load();
            this.save();
        }
    }

    private void initOutfitList(ArrayList<Outfit> outfitList, boolean female) {
        ArrayList<Outfit> outfits = new ArrayList<>(outfitList);
        outfits.sort(Comparator.comparing(o -> o.name));

        for (Outfit outfit : outfits) {
            this.initOutfit(outfit.name, female, true, PersistentOutfits::ApplyOutfit);
        }
    }

    private void initOutfit(String outfitName, boolean female, boolean useSeed, PersistentOutfits.IOutfitter outfitter) {
        Map<String, PersistentOutfits.Data> outfitTo = female ? this.outfitToFemale : this.outfitToMale;
        PersistentOutfits.Data data = this.outfitToData.get(outfitName);
        if (data == null) {
            data = new PersistentOutfits.Data();
            data.index = (short)this.all.size();
            data.outfitName = outfitName;
            data.useSeed = useSeed;
            data.outfitter = outfitter;
            this.outfitNames.add(outfitName);
            this.outfitToData.put(outfitName, data);
            this.all.add(data);
        }

        PersistentOutfits.DataList mf = female ? this.female : this.male;
        mf.add(data);
        outfitTo.put(outfitName, data);
    }

    private void registerCustomOutfits() {
        ArrayList<RandomizedVehicleStoryBase> storyList = IsoWorld.instance.getRandomizedVehicleStoryList();

        for (int i = 0; i < storyList.size(); i++) {
            RandomizedVehicleStoryBase rvsb = storyList.get(i);
            rvsb.registerCustomOutfits();
        }

        ZombiesZoneDefinition.registerCustomOutfits();
        if (GameServer.server || GameClient.client) {
            this.registerOutfitter("ReanimatedPlayer", false, SharedDescriptors::ApplyReanimatedPlayerOutfit);
        }
    }

    public ArrayList<String> getOutfitNames() {
        return this.outfitNames;
    }

    public int pickRandomFemale() {
        if (this.female.isEmpty()) {
            return 0;
        } else {
            String outfitName = PZArrayUtil.pickRandom(this.female).outfitName;
            return this.pickOutfitFemale(outfitName);
        }
    }

    public int pickRandomMale() {
        if (this.male.isEmpty()) {
            return 0;
        } else {
            String outfitName = PZArrayUtil.pickRandom(this.male).outfitName;
            return this.pickOutfitMale(outfitName);
        }
    }

    public int pickOutfitFemale(String outfitName) {
        PersistentOutfits.Data data = this.outfitToFemale.get(outfitName);
        if (data == null) {
            return 0;
        } else {
            short outfitIndex = (short)data.index;
            short variant = data.useSeed ? (short)Rand.Next(500) : 0;
            return -2147483648 | outfitIndex << 16 | variant + 1;
        }
    }

    public int pickOutfitMale(String outfitName) {
        PersistentOutfits.Data data = this.outfitToMale.get(outfitName);
        if (data == null) {
            return 0;
        } else {
            short outfitIndex = (short)data.index;
            short variant = data.useSeed ? (short)Rand.Next(500) : 0;
            return outfitIndex << 16 | variant + 1;
        }
    }

    public int pickOutfit(String outfitName, boolean female) {
        String newOutfitName = ZombiesStageDefinitions.instance.getAdvancedOutfitName(outfitName);
        if (newOutfitName != null) {
            outfitName = newOutfitName;
        }

        return female ? this.pickOutfitFemale(outfitName) : this.pickOutfitMale(outfitName);
    }

    public int getOutfit(int ID) {
        if (ID == 0) {
            return 0;
        } else {
            int femaleBit = ID & -2147483648;
            ID &= Integer.MAX_VALUE;
            int noHatBit = ID & 32768;
            ID &= -32769;
            short outfitIndex = (short)(ID >> 16);
            short variant = (short)(ID & 65535);
            if (outfitIndex >= 0 && outfitIndex < this.all.size()) {
                PersistentOutfits.Data data = this.all.get(outfitIndex);
                if (data.useSeed && (variant < 1 || variant > 500)) {
                    variant = (short)(Rand.Next(500) + 1);
                }

                return femaleBit | noHatBit | outfitIndex << 16 | variant;
            } else {
                return 0;
            }
        }
    }

    public void save() {
        if (!Core.getInstance().isNoSave()) {
            File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");

            try (
                FileOutputStream fos = new FileOutputStream(outFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
            ) {
                synchronized (SliceY.SliceBufferLock) {
                    SliceY.SliceBuffer.clear();
                    ByteBuffer output = SliceY.SliceBuffer;
                    this.save(output);
                    bos.write(output.array(), 0, output.position());
                }
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
            }
        }
    }

    public void save(ByteBuffer output) {
        output.put(FILE_MAGIC);
        output.putInt(1);
        output.putShort((short)500);

        for (int i = 0; i < 500; i++) {
            output.putLong(this.seeds[i]);
        }
    }

    public void load() {
        File inFile = ZomboidFileSystem.instance.getFileInCurrentSave("z_outfits.bin");

        try (
            FileInputStream fis = new FileInputStream(inFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ) {
            synchronized (SliceY.SliceBufferLock) {
                SliceY.SliceBuffer.clear();
                ByteBuffer input = SliceY.SliceBuffer;
                int numBytes = bis.read(input.array());
                input.limit(numBytes);
                this.load(input);
            }
        } catch (FileNotFoundException var13) {
        } catch (Exception var14) {
            ExceptionLogger.logException(var14);
        }
    }

    public void load(ByteBuffer input) throws IOException {
        byte[] magic = new byte[4];
        input.get(magic);
        if (!Arrays.equals(magic, FILE_MAGIC)) {
            throw new IOException("not magic");
        } else {
            int version = input.getInt();
            if (version >= 1 && version <= 1) {
                int numVariants = input.getShort();

                for (int i = 0; i < numVariants; i++) {
                    if (i < 500) {
                        this.seeds[i] = input.getLong();
                    }
                }
            }
        }
    }

    public void registerOutfitter(String id, boolean useSeed, PersistentOutfits.IOutfitter outfitter) {
        this.initOutfit(id, true, useSeed, outfitter);
        this.initOutfit(id, false, useSeed, outfitter);
    }

    private static void ApplyOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        instance.applyOutfit(outfitID, outfitName, chr);
    }

    private void applyOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        boolean female = (outfitID & -2147483648) != 0;
        outfitID &= Integer.MAX_VALUE;
        short outfitIndex = (short)(outfitID >> 16);
        PersistentOutfits.Data data = this.all.get(outfitIndex);
        IsoZombie zombie = Type.tryCastTo(chr, IsoZombie.class);
        if (zombie != null) {
            zombie.setFemaleEtc(female);
        }

        chr.dressInNamedOutfit(data.outfitName);
        if (zombie != null && chr.doDirtBloodEtc) {
            AttachedWeaponDefinitions.instance.addRandomAttachedWeapon(zombie);
            zombie.addRandomBloodDirtHolesEtc();
        }

        this.removeFallenHat(outfitID, chr);
    }

    public boolean isHatFallen(IsoGameCharacter chr) {
        return this.isHatFallen(chr.getPersistentOutfitID());
    }

    public boolean isHatFallen(int outfitID) {
        return (outfitID & 32768) != 0;
    }

    public void setFallenHat(IsoGameCharacter chr, boolean fallen) {
        int outfitID = chr.getPersistentOutfitID();
        if (outfitID != 0) {
            if (fallen) {
                outfitID |= 32768;
            } else {
                outfitID &= -32769;
            }

            chr.setPersistentOutfitID(outfitID, chr.isPersistentOutfitInit());
        }
    }

    public boolean removeFallenHat(int outfitID, IsoGameCharacter chr) {
        if ((outfitID & 32768) == 0) {
            return false;
        } else if (chr.isUsingWornItems()) {
            return false;
        } else {
            boolean removed = false;
            chr.getItemVisuals(tempItemVisuals);

            for (int i = 0; i < tempItemVisuals.size(); i++) {
                ItemVisual itemVisual = tempItemVisuals.get(i);
                Item scriptItem = itemVisual.getScriptItem();
                if (scriptItem != null && scriptItem.getChanceToFall() > 0) {
                    chr.getItemVisuals().remove(itemVisual);
                    removed = true;
                }
            }

            return removed;
        }
    }

    public InventoryItem processFallingHat(IsoGameCharacter character, boolean hitHead) {
        if (instance.isHatFallen(character)) {
            return null;
        } else {
            InventoryItem item = null;
            IsoZombie zombie = Type.tryCastTo(character, IsoZombie.class);
            if (zombie != null && !zombie.isUsingWornItems()) {
                zombie.getItemVisuals(tempItemVisuals);

                for (int i = 0; i < tempItemVisuals.size(); i++) {
                    ItemVisual itemVisual = tempItemVisuals.get(i);
                    Item scriptItem = itemVisual.getScriptItem();
                    if (scriptItem != null && scriptItem.isItemType(ItemType.CLOTHING) && scriptItem.getChanceToFall() > 0) {
                        int chanceToFall = scriptItem.getChanceToFall();
                        if (hitHead) {
                            chanceToFall += 40;
                        }

                        if (Rand.Next(100) <= chanceToFall) {
                            item = InventoryItemFactory.CreateItem(scriptItem.getFullName());
                            if (item != null) {
                                if (item.getVisual() != null) {
                                    item.getVisual().copyFrom(itemVisual);
                                    item.synchWithVisual();
                                }
                                break;
                            }
                        }
                    }
                }
            } else if (character.getWornItems() != null && !character.getWornItems().isEmpty()) {
                for (int ix = 0; ix < character.getWornItems().size(); ix++) {
                    WornItem wornItem = character.getWornItems().get(ix);
                    InventoryItem characterItem = wornItem.getItem();
                    if (characterItem instanceof Clothing clothing) {
                        int chanceToFallx = clothing.getChanceToFall();
                        if (hitHead) {
                            chanceToFallx += 40;
                        }

                        if (clothing.getChanceToFall() > 0 && Rand.Next(100) <= chanceToFallx) {
                            item = characterItem;
                            break;
                        }
                    }
                }
            }

            if (item != null) {
                instance.setFallenHat(character, true);
            }

            return item;
        }
    }

    public void dressInOutfit(IsoGameCharacter chr, int outfitID) {
        outfitID = this.getOutfit(outfitID);
        if (outfitID != 0) {
            int outfitID2 = outfitID & 2147450879;
            short outfitIndex = (short)(outfitID2 >> 16);
            short variant = (short)(outfitID2 & 65535);
            PersistentOutfits.Data data = this.all.get(outfitIndex);
            if (data.useSeed) {
                OutfitRNG.setSeed(this.seeds[variant - 1]);
            }

            data.outfitter.accept(outfitID, data.outfitName, chr);
        }
    }

    private static final class Data {
        int index;
        String outfitName;
        boolean useSeed = true;
        PersistentOutfits.IOutfitter outfitter;
    }

    private static final class DataList extends ArrayList<PersistentOutfits.Data> {
    }

    public interface IOutfitter {
        void accept(int var1, String var2, IsoGameCharacter var3);
    }
}
