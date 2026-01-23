// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.PersistentOutfits;
import zombie.Lua.LuaManager;
import zombie.characters.AttachedItems.AttachedWeaponDefinitions;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.skinnedmodel.population.Outfit;
import zombie.core.skinnedmodel.population.OutfitManager;
import zombie.core.skinnedmodel.population.OutfitRNG;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.util.StringUtils;
import zombie.util.Type;
import zombie.util.list.PZArrayUtil;

public final class ZombiesZoneDefinition {
    private static final ArrayList<ZombiesZoneDefinition.ZZDZone> s_zoneList = new ArrayList<>();
    private static final HashMap<String, ZombiesZoneDefinition.ZZDZone> s_zoneMap = new HashMap<>();
    public static boolean dirty = true;
    private static final ZombiesZoneDefinition.PickDefinition pickDef = new ZombiesZoneDefinition.PickDefinition();
    private static final HashMap<String, ZombiesZoneDefinition.ZZDOutfit> s_customOutfitMap = new HashMap<>();

    private static void checkDirty() {
        if (dirty) {
            dirty = false;
            init();
        }
    }

    private static void init() {
        s_zoneList.clear();
        s_zoneMap.clear();
        if (LuaManager.env.rawget("ZombiesZoneDefinition") instanceof KahluaTableImpl zombiesZoneDefinition) {
            KahluaTableIterator var4 = zombiesZoneDefinition.iterator();

            while (var4.advance()) {
                if (var4.getValue() instanceof KahluaTableImpl zoneTable) {
                    ZombiesZoneDefinition.ZZDZone zone = initZone(var4.getKey().toString(), zoneTable);
                    if (zone != null) {
                        s_zoneList.add(zone);
                        s_zoneMap.put(zone.name, zone);
                    }
                }
            }
        }
    }

    private static ZombiesZoneDefinition.ZZDZone initZone(String name, KahluaTableImpl zoneTable) {
        ZombiesZoneDefinition.ZZDZone zone = new ZombiesZoneDefinition.ZZDZone();
        zone.name = name;
        zone.femaleChance = zoneTable.rawgetInt("femaleChance");
        zone.maleChance = zoneTable.rawgetInt("maleChance");
        zone.chanceToSpawn = zoneTable.rawgetInt("chanceToSpawn");
        zone.toSpawn = zoneTable.rawgetInt("toSpawn");
        KahluaTableIterator iterator = zoneTable.iterator();

        while (iterator.advance()) {
            if (iterator.getValue() instanceof KahluaTableImpl outfitTable) {
                ZombiesZoneDefinition.ZZDOutfit outfit = initOutfit(outfitTable);
                if (outfit != null) {
                    outfit.customName = "ZZD." + zone.name + "." + outfit.name;
                    zone.outfits.add(outfit);
                }
            }
        }

        return zone;
    }

    private static ZombiesZoneDefinition.ZZDOutfit initOutfit(KahluaTableImpl outfitTable) {
        ZombiesZoneDefinition.ZZDOutfit outfit = new ZombiesZoneDefinition.ZZDOutfit();
        outfit.name = outfitTable.rawgetStr("name");
        outfit.chance = outfitTable.rawgetFloat("chance");
        outfit.gender = outfitTable.rawgetStr("gender");
        outfit.toSpawn = outfitTable.rawgetInt("toSpawn");
        outfit.mandatory = outfitTable.rawgetStr("mandatory");
        outfit.room = outfitTable.rawgetStr("room");
        outfit.femaleHairStyles = initStringChance(outfitTable.rawgetStr("femaleHairStyles"));
        outfit.maleHairStyles = initStringChance(outfitTable.rawgetStr("maleHairStyles"));
        outfit.beardStyles = initStringChance(outfitTable.rawgetStr("beardStyles"));
        return outfit;
    }

    private static ArrayList<ZombiesZoneDefinition.StringChance> initStringChance(String styles) {
        if (StringUtils.isNullOrWhitespace(styles)) {
            return null;
        } else {
            ArrayList<ZombiesZoneDefinition.StringChance> result = new ArrayList<>();
            String[] split = styles.split(";");

            for (String style : split) {
                String[] splitStyle = style.split(":");
                ZombiesZoneDefinition.StringChance stringChance = new ZombiesZoneDefinition.StringChance();
                stringChance.str = splitStyle[0];
                stringChance.chance = Float.parseFloat(splitStyle[1]);
                result.add(stringChance);
            }

            return result;
        }
    }

    public static void dressInRandomOutfit(IsoZombie chr) {
        if (!chr.isSkeleton()) {
            IsoGridSquare square = chr.getCurrentSquare();
            if (square != null) {
                ZombiesZoneDefinition.PickDefinition pickDef = pickDefinition(square.x, square.y, square.z, chr.isFemale());
                if (pickDef == null) {
                    String roomName = square.getRoom() == null ? null : square.getRoom().getName();
                    Outfit outfit = getRandomDefaultOutfit(chr.isFemale(), roomName);
                    UnderwearDefinition.addRandomUnderwear(chr);
                    chr.dressInPersistentOutfit(outfit.name);
                } else {
                    UnderwearDefinition.addRandomUnderwear(chr);
                    applyDefinition(chr, pickDef.zone, pickDef.table, pickDef.female);
                }
            }
        }
    }

    public static Zone getDefinitionZoneAt(int x, int y, int z) {
        ArrayList<Zone> zones = IsoWorld.instance.metaGrid.getZonesAt(x, y, z);
        ArrayList<Zone> zones2 = new ArrayList<>();

        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone zone = zones.get(i);
            if ("ZombiesType".equalsIgnoreCase(zone.type) && zone.name != null && s_zoneMap.get(zone.name) != null) {
                return zone;
            }

            if (s_zoneMap.containsKey(zone.type)) {
                zones2.add(zone);
            }
        }

        for (int i = zones2.size() - 1; i >= 0; i--) {
            Zone zonex = zones2.get(i);
            if (s_zoneMap.containsKey(zonex.type)) {
                return zonex;
            }
        }

        return null;
    }

    public static ZombiesZoneDefinition.PickDefinition pickDefinition(int x, int y, int z, boolean bFemale) {
        IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
        if (square == null) {
            return null;
        } else {
            String roomName = square.getRoom() == null ? null : square.getRoom().getName();
            checkDirty();
            Zone zombieZone = getDefinitionZoneAt(x, y, z);
            if (zombieZone == null) {
                return null;
            } else if (zombieZone.spawnSpecialZombies == Boolean.FALSE) {
                return null;
            } else {
                String name = StringUtils.isNullOrEmpty(zombieZone.name) ? zombieZone.type : zombieZone.name;
                ZombiesZoneDefinition.ZZDZone zedSpawnTable = s_zoneMap.get(name);
                if (zedSpawnTable == null) {
                    return null;
                } else {
                    if (zedSpawnTable.chanceToSpawn != -1) {
                        int chance = zedSpawnTable.chanceToSpawn;
                        int maxToSpawn = zedSpawnTable.toSpawn;
                        ArrayList<UUID> alreadySpawnedZone = IsoWorld.instance.getSpawnedZombieZone().get(zombieZone.getName());
                        if (alreadySpawnedZone == null) {
                            alreadySpawnedZone = new ArrayList<>();
                            IsoWorld.instance.getSpawnedZombieZone().put(zombieZone.getName(), alreadySpawnedZone);
                        }

                        if (alreadySpawnedZone.contains(zombieZone.id)) {
                            zombieZone.spawnSpecialZombies = true;
                        }

                        if (maxToSpawn == -1 || zombieZone.spawnSpecialZombies == null && alreadySpawnedZone.size() < maxToSpawn) {
                            if (Rand.Next(100) < chance) {
                                zombieZone.spawnSpecialZombies = true;
                                alreadySpawnedZone.add(zombieZone.id);
                            } else {
                                zombieZone.spawnSpecialZombies = false;
                                zombieZone = null;
                            }
                        }
                    }

                    if (zombieZone == null) {
                        return null;
                    } else {
                        ArrayList<ZombiesZoneDefinition.ZZDOutfit> mandatory = new ArrayList<>();
                        ArrayList<ZombiesZoneDefinition.ZZDOutfit> normal = new ArrayList<>();
                        int maleChance = zedSpawnTable.maleChance;
                        int femaleChance = zedSpawnTable.femaleChance;
                        if (maleChance > 0 && Rand.Next(100) < maleChance) {
                            bFemale = false;
                        }

                        if (femaleChance > 0 && Rand.Next(100) < femaleChance) {
                            bFemale = true;
                        }

                        for (int i = 0; i < zedSpawnTable.outfits.size(); i++) {
                            ZombiesZoneDefinition.ZZDOutfit zedType = zedSpawnTable.outfits.get(i);
                            String gender = zedType.gender;
                            String room = zedType.room;
                            if ((room == null || roomName != null && room.contains(roomName))
                                && (!"male".equalsIgnoreCase(gender) || !bFemale)
                                && (!"female".equalsIgnoreCase(gender) || bFemale)) {
                                String outfitName = zedType.name;
                                boolean isMandatory = Boolean.parseBoolean(zedType.mandatory);
                                if (isMandatory) {
                                    int alreadySpawnNumber = zombieZone.spawnedZombies == null ? 0 : zombieZone.spawnedZombies.getOrDefault(outfitName, 0);
                                    if (alreadySpawnNumber < zedType.toSpawn) {
                                        mandatory.add(zedType);
                                    }
                                } else {
                                    normal.add(zedType);
                                }
                            }
                        }

                        ZombiesZoneDefinition.ZZDOutfit zombieToSpawn;
                        if (!mandatory.isEmpty()) {
                            zombieToSpawn = PZArrayUtil.pickRandom(mandatory);
                        } else {
                            zombieToSpawn = getRandomOutfitInSetList(normal, true);
                        }

                        if (zombieToSpawn == null) {
                            return null;
                        } else {
                            pickDef.table = zombieToSpawn;
                            pickDef.female = bFemale;
                            pickDef.zone = zombieZone;
                            return pickDef;
                        }
                    }
                }
            }
        }
    }

    public static void applyDefinition(IsoZombie chr, Zone zombieZone, ZombiesZoneDefinition.ZZDOutfit zombieToSpawn, boolean bFemale) {
        chr.setFemaleEtc(bFemale);
        Outfit outfitSource = null;
        if (!bFemale) {
            outfitSource = OutfitManager.instance.FindMaleOutfit(zombieToSpawn.name);
        } else {
            outfitSource = OutfitManager.instance.FindFemaleOutfit(zombieToSpawn.name);
        }

        String outfitName = zombieToSpawn.customName;
        if (outfitSource == null) {
            outfitSource = OutfitManager.instance.GetRandomOutfit(bFemale);
            outfitName = outfitSource.name;
        } else if (zombieZone != null) {
            if (zombieZone.spawnedZombies == null) {
                zombieZone.spawnedZombies = new HashMap<>();
                zombieZone.spawnedZombies.put(outfitSource.name, 1);
            } else {
                int alreadySpawnNumber = zombieZone.spawnedZombies.getOrDefault(outfitSource.name, 0);
                zombieZone.spawnedZombies.put(outfitSource.name, alreadySpawnNumber + 1);
            }
        }

        if (outfitSource != null) {
            chr.dressInPersistentOutfit(outfitSource.name);
        }

        ModelManager.instance.ResetNextFrame(chr);
        chr.advancedAnimator.OnAnimDataChanged(false);
    }

    public static Outfit getRandomDefaultOutfit(boolean bFemale, String roomName) {
        ArrayList<ZombiesZoneDefinition.ZZDOutfit> list = new ArrayList<>();
        KahluaTable zombiesZoneDefinition = (KahluaTable)LuaManager.env.rawget("ZombiesZoneDefinition");
        ZombiesZoneDefinition.ZZDZone zedSpawnTable = s_zoneMap.get("Default");

        for (int i = 0; i < zedSpawnTable.outfits.size(); i++) {
            ZombiesZoneDefinition.ZZDOutfit zombieToSpawn = zedSpawnTable.outfits.get(i);
            String gender = zombieToSpawn.gender;
            String room = zombieToSpawn.room;
            if ((room == null || roomName != null && room.contains(roomName))
                && (gender == null || "male".equalsIgnoreCase(gender) && !bFemale || "female".equalsIgnoreCase(gender) && bFemale)) {
                list.add(zombieToSpawn);
            }
        }

        ZombiesZoneDefinition.ZZDOutfit zombieToSpawn = getRandomOutfitInSetList(list, false);
        Outfit result = null;
        if (zombieToSpawn != null) {
            if (bFemale) {
                result = OutfitManager.instance.FindFemaleOutfit(zombieToSpawn.name);
            } else {
                result = OutfitManager.instance.FindMaleOutfit(zombieToSpawn.name);
            }
        }

        if (result == null) {
            result = OutfitManager.instance.GetRandomOutfit(bFemale);
        }

        return result;
    }

    public static ZombiesZoneDefinition.ZZDOutfit getRandomOutfitInSetList(ArrayList<ZombiesZoneDefinition.ZZDOutfit> list, boolean doTotalChance100) {
        float totalChance = 0.0F;

        for (int i = 0; i < list.size(); i++) {
            ZombiesZoneDefinition.ZZDOutfit outfitTable = list.get(i);
            totalChance += outfitTable.chance;
        }

        float choice = Rand.Next(0.0F, 100.0F);
        if (!doTotalChance100 || totalChance > 100.0F) {
            choice = Rand.Next(0.0F, totalChance);
        }

        float subtotal = 0.0F;

        for (int i = 0; i < list.size(); i++) {
            ZombiesZoneDefinition.ZZDOutfit outfitTable = list.get(i);
            subtotal += outfitTable.chance;
            if (choice < subtotal) {
                return outfitTable;
            }
        }

        return null;
    }

    private static String getRandomHairOrBeard(ArrayList<ZombiesZoneDefinition.StringChance> styles) {
        float choice = OutfitRNG.Next(0.0F, 100.0F);
        float subtotal = 0.0F;

        for (int i = 0; i < styles.size(); i++) {
            ZombiesZoneDefinition.StringChance stringChance = styles.get(i);
            subtotal += stringChance.chance;
            if (choice < subtotal) {
                if ("null".equalsIgnoreCase(stringChance.str)) {
                    return "";
                }

                return stringChance.str;
            }
        }

        return null;
    }

    public static void registerCustomOutfits() {
        checkDirty();
        s_customOutfitMap.clear();

        for (ZombiesZoneDefinition.ZZDZone zone : s_zoneList) {
            for (ZombiesZoneDefinition.ZZDOutfit outfit : zone.outfits) {
                PersistentOutfits.instance.registerOutfitter(outfit.customName, true, ZombiesZoneDefinition::ApplyCustomOutfit);
                s_customOutfitMap.put(outfit.customName, outfit);
            }
        }
    }

    private static void ApplyCustomOutfit(int outfitID, String outfitName, IsoGameCharacter chr) {
        ZombiesZoneDefinition.ZZDOutfit zombieToSpawn = s_customOutfitMap.get(outfitName);
        boolean female = (outfitID & -2147483648) != 0;
        IsoZombie zombie = Type.tryCastTo(chr, IsoZombie.class);
        if (zombie != null) {
            zombie.setFemaleEtc(female);
        }

        chr.dressInNamedOutfit(zombieToSpawn.name);
        if (zombie == null) {
            PersistentOutfits.instance.removeFallenHat(outfitID, chr);
        } else {
            AttachedWeaponDefinitions.instance.addRandomAttachedWeapon(zombie);
            zombie.addRandomBloodDirtHolesEtc();
            boolean bFemale = chr.isFemale();
            if (bFemale && zombieToSpawn.femaleHairStyles != null) {
                zombie.getHumanVisual().setHairModel(getRandomHairOrBeard(zombieToSpawn.femaleHairStyles));
            }

            if (!bFemale && zombieToSpawn.maleHairStyles != null) {
                zombie.getHumanVisual().setHairModel(getRandomHairOrBeard(zombieToSpawn.maleHairStyles));
            }

            if (!bFemale && zombieToSpawn.beardStyles != null) {
                zombie.getHumanVisual().setBeardModel(getRandomHairOrBeard(zombieToSpawn.beardStyles));
            }

            PersistentOutfits.instance.removeFallenHat(outfitID, chr);
        }
    }

    public static int pickPersistentOutfit(IsoGridSquare square) {
        if (!GameServer.server) {
            return 0;
        } else {
            boolean bFemale = Rand.Next(2) == 0;
            ZombiesZoneDefinition.PickDefinition pickDef = pickDefinition(square.x, square.y, square.z, bFemale);
            Outfit outfit;
            if (pickDef == null) {
                String roomName = square.getRoom() == null ? null : square.getRoom().getName();
                outfit = getRandomDefaultOutfit(bFemale, roomName);
            } else {
                bFemale = pickDef.female;
                String outfitName = pickDef.table.name;
                if (bFemale) {
                    outfit = OutfitManager.instance.FindFemaleOutfit(outfitName);
                } else {
                    outfit = OutfitManager.instance.FindMaleOutfit(outfitName);
                }
            }

            if (outfit == null) {
                boolean var7 = true;
            } else {
                int outfitID = PersistentOutfits.instance.pickOutfit(outfit.name, bFemale);
                if (outfitID != 0) {
                    return outfitID;
                }

                boolean var5 = true;
            }

            return 0;
        }
    }

    public static final class PickDefinition {
        Zone zone;
        ZombiesZoneDefinition.ZZDOutfit table;
        boolean female;
    }

    private static final class StringChance {
        String str;
        float chance;
    }

    private static final class ZZDOutfit {
        String name;
        String customName;
        float chance;
        int toSpawn;
        String gender;
        String mandatory;
        String room;
        ArrayList<ZombiesZoneDefinition.StringChance> femaleHairStyles;
        ArrayList<ZombiesZoneDefinition.StringChance> maleHairStyles;
        ArrayList<ZombiesZoneDefinition.StringChance> beardStyles;
    }

    private static final class ZZDZone {
        String name;
        int femaleChance;
        int maleChance;
        int chanceToSpawn;
        int toSpawn;
        final ArrayList<ZombiesZoneDefinition.ZZDOutfit> outfits = new ArrayList<>();
    }
}
