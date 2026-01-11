// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedRanch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.characters.animals.AnimalDefinitions;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.animals.datas.AnimalBreed;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.zones.Zone;
import zombie.network.GameServer;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.util.StringUtils;

public class RandomizedRanchBase extends RandomizedWorldBase {
    public boolean alwaysDo;
    public static final int baseChance = 15;
    public static int totalChance;
    public static final String ranchStory = "Ranch";
    public int chance;

    public static boolean checkRanchStory(Zone zone, boolean force) {
        if ("Ranch".equals(zone.type) && zone.isFullyStreamed() && zone.hourLastSeen == 0) {
            DesignationZoneAnimal newZone = new DesignationZoneAnimal(
                "Ranch", zone.x, zone.y, zone.z, zone.x + zone.getWidth(), zone.y + zone.getHeight(), true
            );
            ArrayList<DesignationZoneAnimal> connectedZone = DesignationZoneAnimal.getAllDZones(null, newZone, null);

            for (int i = 0; i < connectedZone.size(); i++) {
                DesignationZoneAnimal checkZone = connectedZone.get(i);
                if (checkZone.hourLastSeen > 0) {
                    newZone.setName(checkZone.getName());
                    newZone.hourLastSeen = checkZone.hourLastSeen;
                }
            }

            if (newZone.hourLastSeen == 0) {
                doRandomRanch(zone, force, newZone);
            }

            newZone.hourLastSeen++;
            zone.hourLastSeen++;
            return true;
        } else {
            return false;
        }
    }

    private static boolean doRandomRanch(Zone zone, boolean force, DesignationZoneAnimal newDZone) {
        zone.hourLastSeen++;
        int chance = 6;
        switch (SandboxOptions.instance.animalRanchChance.getValue()) {
            case 1:
                return false;
            case 2:
                chance = 7;
            case 3:
            default:
                break;
            case 4:
                chance = 20;
                break;
            case 5:
                chance = 55;
                break;
            case 6:
                chance = 85;
                break;
            case 7:
                chance = 120;
        }

        if (force) {
            chance = 100;
        }

        if (Rand.Next(100) < chance) {
            randomizeRanch(zone, newDZone);
        }

        return false;
    }

    public static RanchZoneDefinitions getRandomDef(String animalType) {
        RanchZoneDefinitions choosenDef = null;
        HashMap<String, ArrayList<RanchZoneDefinitions>> defs = RanchZoneDefinitions.getDefs();
        int rand = Rand.Next(RanchZoneDefinitions.totalChance);
        int chanceIndex = 0;
        if (!StringUtils.isNullOrEmpty(animalType)) {
            int totalChance = 0;
            ArrayList<RanchZoneDefinitions> possibleDefs = defs.get(animalType);
            if (possibleDefs == null) {
                DebugLog.Animal.debugln(animalType + " wasn't found in the RanchZoneDefinitions");
                return null;
            } else {
                for (int j = 0; j < possibleDefs.size(); j++) {
                    totalChance += possibleDefs.get(j).chance;
                }

                rand = Rand.Next(totalChance);

                for (int j = 0; j < possibleDefs.size(); j++) {
                    choosenDef = possibleDefs.get(j);
                    if (choosenDef.chance + chanceIndex >= rand) {
                        break;
                    }

                    chanceIndex += choosenDef.chance;
                    choosenDef = null;
                }

                return choosenDef;
            }
        } else {
            Iterator<String> it = defs.keySet().iterator();

            while (it.hasNext() && choosenDef == null) {
                String defType = it.next();
                ArrayList<RanchZoneDefinitions> possibleDefs = defs.get(defType);

                for (int j = 0; j < possibleDefs.size(); j++) {
                    choosenDef = possibleDefs.get(j);
                    if (choosenDef.chance + chanceIndex >= rand) {
                        return choosenDef;
                    }

                    chanceIndex += choosenDef.chance;
                    choosenDef = null;
                }
            }

            return null;
        }
    }

    public static void randomizeRanch(Zone zone, DesignationZoneAnimal newDZone) {
        String animalType = zone.name;
        RanchZoneDefinitions choosenDef = getRandomDef(animalType);
        if (choosenDef == null) {
            DebugLog.Animal.debugln("No def was found for this ranch " + animalType + " was found in the RanchZoneDefinitions");
        } else {
            if (!choosenDef.possibleDef.isEmpty()) {
                choosenDef = getDefInPossibleDefList(choosenDef.possibleDef);
            }

            AnimalDefinitions femaleDef = AnimalDefinitions.getDef(choosenDef.femaleType);
            AnimalDefinitions maleDef = AnimalDefinitions.getDef(choosenDef.maleType);
            if (femaleDef == null) {
                DebugLog.Animal.debugln("No female def was found for " + choosenDef.femaleType);
            } else if (maleDef == null) {
                DebugLog.Animal.debugln("No male def was found for " + choosenDef.maleType);
            } else {
                AnimalBreed breed = null;
                boolean randomBreed = true;
                if (!StringUtils.isNullOrEmpty(choosenDef.forcedBreed)) {
                    breed = femaleDef.getBreedByName(choosenDef.forcedBreed);
                    randomBreed = false;
                    if (breed == null) {
                        DebugLog.Animal.debugln("No breed def was found for " + choosenDef.forcedBreed + " taking random one");
                        randomBreed = true;
                    }
                }

                int femaleNb = Rand.Next(choosenDef.minFemaleNb, choosenDef.maxFemaleNb + 1);
                int maleNb = Rand.Next(choosenDef.minMaleNb, choosenDef.maxMaleNb + 1);
                if (Rand.Next(100) > choosenDef.maleChance) {
                    maleNb = 0;
                }

                for (int i = 0; i < femaleNb; i++) {
                    if (randomBreed) {
                        breed = getRandomBreed(femaleDef);
                    }

                    IsoGridSquare sq = zone.getRandomFreeSquareInZone();
                    if (sq == null) {
                        DebugLog.Animal.debugln("No free square was found in the zone.");
                        return;
                    }

                    IsoAnimal animal = new IsoAnimal(IsoWorld.instance.getCell(), sq.x, sq.y, zone.z, femaleDef.animalTypeStr, breed);
                    animal.setWild(false);
                    animal.addToWorld();
                    animal.randomizeAge();
                    if (Core.getInstance().animalCheat) {
                        animal.setCustomName(Translator.getText("IGUI_AnimalType_" + animal.getAnimalType()) + " " + animal.getAnimalID());
                    }

                    IsoAnimal baby = null;
                    if (animal.getData().canHaveBaby() && Rand.Next(100) <= choosenDef.chanceForBaby) {
                        baby = animal.addBaby();
                        baby.setWild(false);
                        if (Core.getInstance().animalCheat) {
                            baby.setCustomName(Translator.getText("IGUI_AnimalType_Baby", baby.mother.getFullName()));
                        }

                        if (animal.canBeMilked()) {
                            animal.getData().setMilkQuantity(Rand.Next(5.0F, animal.getData().getMaxMilk()));
                        }

                        baby.randomizeAge();
                    }

                    if (GameTime.getInstance().getWorldAgeDaysSinceBegin() > 60.0) {
                        int randValue = Math.max(0, 190 - (int)GameTime.getInstance().getWorldAgeDaysSinceBegin());
                        if (Rand.NextBool(randValue)) {
                            animal.setHealth(0.0F);
                            if (baby != null) {
                                baby.setHealth(0.0F);
                            }
                        }
                    }

                    if (GameServer.server) {
                        AnimalInstanceManager.getInstance().add(animal, animal.getOnlineID());
                        if (baby != null) {
                            AnimalInstanceManager.getInstance().add(baby, baby.getOnlineID());
                        }
                    }
                }

                for (int i = 0; i < maleNb; i++) {
                    if (randomBreed) {
                        breed = getRandomBreed(maleDef);
                    }

                    IsoAnimal animalx = new IsoAnimal(
                        IsoWorld.instance.getCell(),
                        Rand.Next(zone.x, zone.x + zone.getWidth()),
                        Rand.Next(zone.y, zone.y + zone.getHeight()),
                        zone.z,
                        maleDef.animalTypeStr,
                        breed
                    );
                    if (Core.getInstance().animalCheat) {
                        animalx.setCustomName(Translator.getText("IGUI_AnimalType_" + animalx.getAnimalType()) + " " + animalx.getAnimalID());
                    }

                    animalx.addToWorld();
                    animalx.randomizeAge();
                    if (GameTime.getInstance().getWorldAgeDaysSinceBegin() > 60.0) {
                        int randValue = Math.max(0, 250 - (int)GameTime.getInstance().getWorldAgeDaysSinceBegin());
                        if (Rand.NextBool(randValue)) {
                            animalx.setHealth(0.0F);
                        }
                    }

                    if (GameServer.server) {
                        AnimalInstanceManager.getInstance().add(animalx, animalx.getOnlineID());
                    }
                }

                newDZone.setName(Translator.getText("UI_Ranch", Translator.getText("IGUI_AnimalType_Global_" + choosenDef.globalName), Rand.Next(10000)));
            }
        }
    }

    private static RanchZoneDefinitions getDefInPossibleDefList(ArrayList<RanchZoneDefinitions> possibleDefs) {
        int totalChance = 0;

        for (int j = 0; j < possibleDefs.size(); j++) {
            totalChance += possibleDefs.get(j).chance;
        }

        int rand = Rand.Next(totalChance);
        int chanceIndex = 0;

        for (int j = 0; j < possibleDefs.size(); j++) {
            RanchZoneDefinitions choosenDef = possibleDefs.get(j);
            if (choosenDef.chance + chanceIndex >= rand) {
                return choosenDef;
            }

            chanceIndex += choosenDef.chance;
            RanchZoneDefinitions var6 = null;
        }

        return null;
    }

    private static AnimalBreed getRandomBreed(AnimalDefinitions def) {
        return def.getBreeds().get(Rand.Next(0, def.getBreeds().size()));
    }

    public boolean isValid() {
        return true;
    }
}
