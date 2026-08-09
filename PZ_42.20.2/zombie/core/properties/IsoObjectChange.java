// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.properties;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum IsoObjectChange {
    ENERGY("Energy"),
    LIGHT_RADIUS("LightRadius"),
    CONTAINERS("containers"),
    CONTAINER("container"),
    CONTAINER_CUSTOM_TEMPERATURE("container.customTemperature"),
    NAME("name"),
    REPLACE_WITH("replaceWith"),
    USES_EXTERNAL_WATER_SOURCE("usesExternalWaterSource"),
    EMPTY_TRASH("emptyTrash"),
    SPRITE("sprite"),
    STATE("state"),
    WASHER_STATE("washer.state"),
    DRYER_STATE("dryer.state"),
    MODE("mode"),
    BECOME_SKELETON("becomeSkeleton"),
    ANIMAL_ROT_STAGE("animalRotStage"),
    ZOMBIE_ROT_STAGE("zombieRotStage"),
    OBJECT_ID("objectID"),
    ADD_SHEET("addSheet"),
    REMOVE_SHEET("removeSheet"),
    SET_CURTAIN_OPEN("setCurtainOpen"),
    ADD_ITEM("addItem"),
    ADD_ITEM_OF_TYPE("addItemOfType"),
    ADD_RANDOM_DAMAGE_FROM_ZOMBIE("AddRandomDamageFromZombie"),
    ADD_ZOMBIE_KILL("AddZombieKill"),
    REMOVE_ITEM("removeItem"),
    REMOVE_ITEM_ID("removeItemID"),
    REMOVE_ITEM_TYPE("removeItemType"),
    REMOVE_ONE_OF("removeOneOf"),
    REANIMATED_ID("reanimatedID"),
    SHOVE("Shove"),
    WAKE_UP("wakeUp"),
    MECHANIC_ACTION_DONE("mechanicActionDone"),
    EXIT_VEHICLE("exitVehicle"),
    STOP_BURNING("StopBurning"),
    VEHICLE_NO_KEY("vehicleNoKey"),
    ROTATE("rotate"),
    LIGHT_SOURCE("lightSource"),
    PAINTABLE("paintable"),
    SWAP_ITEM("swapItem"),
    PLAY_GAIN_EXPERIENCE_LEVEL_SOUND("playGainExperienceLevelSound"),
    SET_HALO_NOTE("setHaloNote"),
    COUGH("Cough");

    private static final Map<String, IsoObjectChange> BY_NAME = PZArrayUtil.generateNameToEnumLookUpTable(IsoObjectChange.class, IsoObjectChange::getName);
    private final String name;

    private IsoObjectChange(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isProperty(String inObjectChangeName) {
        return StringUtils.equalsIgnoreCase(this.name, inObjectChangeName);
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public static IsoObjectChange lookup(String name) {
        IsoObjectChange propertyType = BY_NAME.get(name);
        if (propertyType == null) {
            throw new IsoObjectChange.IsoObjectChangeNotFoundException(name);
        } else {
            return propertyType;
        }
    }

    public static IsoObjectChange lookup(Object obj) {
        return obj instanceof IsoObjectChange change ? change : lookup(String.valueOf(obj));
    }

    public static class IsoObjectChangeNotFoundException extends RuntimeException {
        public IsoObjectChangeNotFoundException(String inName) {
            super("ObjectChange Name not found: " + inName);
        }
    }
}
