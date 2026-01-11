// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import se.krka.kahlua.j2se.KahluaTableImpl;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.VehicleScript;
import zombie.util.list.PZArrayUtil;

/**
 * Contains all car model with their associated skin index
 */
@UsedFromLua
public final class VehicleType {
    public final ArrayList<VehicleType.VehicleTypeDefinition> vehiclesDefinition = new ArrayList<>();
    public int chanceToSpawnNormal = 80;
    public int chanceToSpawnBurnt;
    public int spawnRate = 16;
    public int chanceOfOverCar;
    public boolean randomAngle;
    public float baseVehicleQuality = 1.0F;
    public String name = "";
    private int chanceToSpawnKey = 70;
    public int chanceToPartDamage;
    public boolean isSpecialCar;
    public boolean isBurntCar;
    public int chanceToSpawnSpecial = 5;
    public boolean forceSpawn;
    public static final HashMap<String, VehicleType> vehicles = new HashMap<>();
    public static final ArrayList<VehicleType> specialVehicles = new ArrayList<>();

    public VehicleType(String name) {
        this.name = name;
    }

    public static void init() {
        initNormal();
        validate(vehicles.values());
        validate(specialVehicles);
    }

    private static void validate(Collection<VehicleType> types) {
    }

    private static void initNormal() {
        boolean bDebugEnabled = DebugLog.isEnabled(DebugType.Lua);
        KahluaTableImpl vehicleZoneDistribution = (KahluaTableImpl)LuaManager.env.rawget("VehicleZoneDistribution");

        for (Entry<Object, Object> zoneEntry : vehicleZoneDistribution.delegate.entrySet()) {
            String zoneType = zoneEntry.getKey().toString();
            VehicleType type = new VehicleType(zoneType);
            ArrayList<VehicleType.VehicleTypeDefinition> carList = type.vehiclesDefinition;
            KahluaTableImpl zoneDefinition = (KahluaTableImpl)zoneEntry.getValue();
            KahluaTableImpl vehiclesInZone = (KahluaTableImpl)zoneDefinition.rawget("vehicles");

            for (Entry<Object, Object> zoneEntryDefinition : vehiclesInZone.delegate.entrySet()) {
                String carType = zoneEntryDefinition.getKey().toString();
                VehicleScript vehicleScript = ScriptManager.instance.getVehicle(carType);
                if (vehicleScript == null) {
                    DebugLog.Vehicle.warn("vehicle type \"" + carType + "\" doesn't exist");
                }

                KahluaTableImpl carTypeDefinition = (KahluaTableImpl)zoneEntryDefinition.getValue();
                carList.add(new VehicleType.VehicleTypeDefinition(carType, carTypeDefinition.rawgetInt("index"), carTypeDefinition.rawgetFloat("spawnChance")));
            }

            float chanceSumm = 0.0F;

            for (int i = 0; i < carList.size(); i++) {
                chanceSumm += carList.get(i).spawnChance;
            }

            chanceSumm = 100.0F / chanceSumm;

            for (int i = 0; i < carList.size(); i++) {
                carList.get(i).spawnChance *= chanceSumm;
                if (bDebugEnabled) {
                    DebugLog.Vehicle.println(zoneType + ": " + carList.get(i).vehicleType + " " + carList.get(i).spawnChance + "%");
                }
            }

            if (zoneDefinition.delegate.containsKey("chanceToPartDamage")) {
                type.chanceToPartDamage = zoneDefinition.rawgetInt("chanceToPartDamage");
            }

            if (zoneDefinition.delegate.containsKey("chanceToSpawnNormal")) {
                type.chanceToSpawnNormal = zoneDefinition.rawgetInt("chanceToSpawnNormal");
            }

            if (zoneDefinition.delegate.containsKey("chanceToSpawnSpecial")) {
                type.chanceToSpawnSpecial = zoneDefinition.rawgetInt("chanceToSpawnSpecial");
            }

            if (zoneDefinition.delegate.containsKey("specialCar")) {
                type.isSpecialCar = zoneDefinition.rawgetBool("specialCar");
            }

            if (zoneDefinition.delegate.containsKey("burntCar")) {
                type.isBurntCar = zoneDefinition.rawgetBool("burntCar");
            }

            if (zoneDefinition.delegate.containsKey("baseVehicleQuality")) {
                type.baseVehicleQuality = zoneDefinition.rawgetFloat("baseVehicleQuality");
            }

            if (zoneDefinition.delegate.containsKey("chanceOfOverCar")) {
                type.chanceOfOverCar = zoneDefinition.rawgetInt("chanceOfOverCar");
            }

            if (zoneDefinition.delegate.containsKey("randomAngle")) {
                type.randomAngle = zoneDefinition.rawgetBool("randomAngle");
            }

            if (zoneDefinition.delegate.containsKey("spawnRate")) {
                type.spawnRate = zoneDefinition.rawgetInt("spawnRate");
            }

            if (zoneDefinition.delegate.containsKey("chanceToSpawnKey")) {
                type.chanceToSpawnKey = zoneDefinition.rawgetInt("chanceToSpawnKey");
            }

            if (zoneDefinition.delegate.containsKey("chanceToSpawnBurnt")) {
                type.chanceToSpawnBurnt = zoneDefinition.rawgetInt("chanceToSpawnBurnt");
            }

            if (zoneDefinition.delegate.containsKey("forceSpawn")) {
                type.forceSpawn = zoneDefinition.rawgetBool("forceSpawn");
            }

            vehicles.put(zoneType, type);
            if (type.isSpecialCar) {
                specialVehicles.add(type);
            }
        }

        HashSet<String> spawnedVehicles = new HashSet<>();

        for (VehicleType vehicleType : vehicles.values()) {
            for (VehicleType.VehicleTypeDefinition vehicleTypeDefinition : vehicleType.vehiclesDefinition) {
                spawnedVehicles.add(vehicleTypeDefinition.vehicleType);
            }
        }

        for (VehicleScript vehicleScript : ScriptManager.instance.getAllVehicleScripts()) {
            if (!spawnedVehicles.contains(vehicleScript.getFullName())) {
            }
        }
    }

    public static boolean hasTypeForZone(String zoneName) {
        if (vehicles.isEmpty()) {
            init();
        }

        zoneName = zoneName.toLowerCase();
        return vehicles.containsKey(zoneName);
    }

    public static VehicleType getRandomVehicleType(String zoneName) {
        return getRandomVehicleType(zoneName, true);
    }

    public static VehicleType getRandomVehicleType(String zoneName, Boolean doNormalWhenSpecific) {
        if (vehicles.isEmpty()) {
            init();
        }

        zoneName = zoneName.toLowerCase();
        VehicleType type = vehicles.get(zoneName);
        if (type == null) {
            DebugLog.log(zoneName + " Don't exist in VehicleZoneDistribution");
            return null;
        } else if (Rand.Next(100) < type.chanceToSpawnBurnt) {
            if (Rand.Next(100) < 80) {
                type = vehicles.get("normalburnt");
            } else {
                type = vehicles.get("specialburnt");
            }

            return type;
        } else {
            if (doNormalWhenSpecific && type.isSpecialCar && Rand.Next(100) < type.chanceToSpawnNormal) {
                type = vehicles.get("parkingstall");
            }

            if (!type.isBurntCar && !type.isSpecialCar && Rand.Next(100) < type.chanceToSpawnSpecial) {
                type = PZArrayUtil.pickRandom(specialVehicles);
            }

            if (type.isBurntCar) {
                if (Rand.Next(100) < 80) {
                    type = vehicles.get("normalburnt");
                } else {
                    type = vehicles.get("specialburnt");
                }
            }

            return type;
        }
    }

    public static VehicleType getTypeFromName(String name) {
        if (vehicles.isEmpty()) {
            init();
        }

        return vehicles.get(name);
    }

    public float getBaseVehicleQuality() {
        return this.baseVehicleQuality;
    }

    public float getRandomBaseVehicleQuality() {
        return Rand.Next(this.baseVehicleQuality - 0.1F, this.baseVehicleQuality + 0.1F);
    }

    public int getChanceToSpawnKey() {
        return this.chanceToSpawnKey;
    }

    public void setChanceToSpawnKey(int chanceToSpawnKey) {
        this.chanceToSpawnKey = chanceToSpawnKey;
    }

    public static void Reset() {
        vehicles.clear();
        specialVehicles.clear();
    }

    public static class VehicleTypeDefinition {
        public String vehicleType;
        public int index = -1;
        public float spawnChance;

        public VehicleTypeDefinition(String vehicleType, int index, float spawnChance) {
            this.vehicleType = vehicleType;
            this.index = index;
            this.spawnChance = spawnChance;
        }
    }
}
