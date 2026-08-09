// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Predicate;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.ZomboidGlobals;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.Capability;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.input.Input;
import zombie.core.properties.PropertyContainer;
import zombie.core.properties.TilePropertyKey;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.textures.Texture;
import zombie.core.textures.TexturePackPage;
import zombie.debug.DebugOptions;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.components.contextmenuconfig.ContextMenuConfig;
import zombie.entity.components.fluids.Fluid;
import zombie.entity.components.fluids.FluidCategory;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.fluids.FluidType;
import zombie.fireFighting.FireFighting;
import zombie.globalObjects.CGlobalObjectSystem;
import zombie.globalObjects.CGlobalObjects;
import zombie.globalObjects.GlobalObject;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.inventory.types.HandWeapon;
import zombie.inventory.types.Key;
import zombie.inventory.types.Radio;
import zombie.inventory.types.WeaponType;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.DesignationZoneAnimal;
import zombie.iso.areas.SafeHouse;
import zombie.iso.objects.IsoAnimalTrack;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoBrokenGlass;
import zombie.iso.objects.IsoCarBatteryCharger;
import zombie.iso.objects.IsoClothingDryer;
import zombie.iso.objects.IsoClothingWasher;
import zombie.iso.objects.IsoCombinationWasherDryer;
import zombie.iso.objects.IsoCompost;
import zombie.iso.objects.IsoCurtain;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoFeedingTrough;
import zombie.iso.objects.IsoGenerator;
import zombie.iso.objects.IsoLightSwitch;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoStove;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoTrap;
import zombie.iso.objects.IsoTree;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.worldgen.WorldGenParams;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.network.packets.RequestMedicalCheckPacket;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.contextmenuconfig.ContextMenuConfigScript;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemKey;
import zombie.scripting.objects.ItemTag;
import zombie.scripting.objects.MoodleType;
import zombie.scripting.objects.Recipe;
import zombie.ui.TextManager;
import zombie.ui.UIFont;
import zombie.ui.ISUIWrapper.ISContextMenuWrapper;
import zombie.ui.ISUIWrapper.ISToolTipWrapper;
import zombie.ui.ISUIWrapper.LuaHelpers;
import zombie.util.AdjacentFreeTileFinder;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehiclePart;

@UsedFromLua
public class ISWorldObjectContextMenuLogic {
    private static final IsoDirections[] DIRECTIONS = IsoDirections.values();

    public static void fetch(KahluaTable fetch, IsoObject v, double player, boolean doSquare) {
        KahluaTable worldObjectContextMenu = (KahluaTable)LuaManager.env.rawget("ISWorldObjectContextMenu");
        KahluaTable fetchSquares = (KahluaTable)worldObjectContextMenu.rawget("fetchSquares");
        IsoPlayer playerObj = IsoPlayer.players[(int)player];
        ItemContainer playerInv = playerObj.getInventory();
        PropertyContainer props = v.getProperties();
        if (doSquare && v.getSquare() != null) {
            ArrayList<IsoWorldInventoryObject> worldItems = v.getSquare().getWorldObjects();
            if (worldItems != null && !worldItems.isEmpty()) {
                fetch.rawset("worldItem", worldItems.getFirst());
            }

            fetch.rawset("building", v.getSquare().getBuilding());
        }

        if (v.hasFluid()) {
            KahluaTable storeWater = (KahluaTable)fetch.rawget("storeWater");
            if (v instanceof IsoFeedingTrough feedingTrough) {
                IsoFeedingTrough var23 = feedingTrough.getMasterTrough();
                if (var23 != null && !LuaHelpers.tableContainsValue(storeWater, var23)) {
                    storeWater.rawset(storeWater.size() + 1, var23);
                }
            } else if (!LuaHelpers.tableContainsValue(storeWater, v)) {
                storeWater.rawset(storeWater.size() + 1, v);
            }
        }

        Double c = (Double)fetch.rawget("c");
        fetch.rawset("c", c + 1.0);
        if (v instanceof IsoWindow) {
            fetch.rawset("window", v);
        } else if (v instanceof IsoCurtain) {
            fetch.rawset("curtain", v);
        }

        if (v instanceof IsoDoor || v instanceof IsoThumpable isoThumpable && isoThumpable.isDoor()) {
            fetch.rawset("door", v);
            if (v instanceof IsoDoor isoDoor) {
                int keyId = isoDoor.checkKeyId();
                fetch.rawset("doorKeyId", keyId);
                if (keyId == -1) {
                    fetch.rawset("doorKeyId", null);
                }
            }

            if (v instanceof IsoThumpable && v.getKeyId() != -1) {
                fetch.rawset("doorKeyId", v.getKeyId());
            }
        }

        if (v instanceof IsoAnimalTrack) {
            fetch.rawset("animaltrack", v);
        }

        if (doSquare) {
            IsoAnimalTrack animalTrack = v.getSquare().getAnimalTrack();
            if (animalTrack != null) {
                fetch.rawset("animaltrack", v.getSquare().getAnimalTrack());
            }
        }

        fetch.rawset("item", v);
        if (v instanceof IsoButcherHook) {
            fetch.rawset("butcherHook", v);
        }

        if (v.hasComponent(ComponentType.ContextMenuConfig) && !v.hasComponent(ComponentType.FluidContainer)) {
            fetch.rawset("entityContext", v);
        }

        if (v.getSprite() != null && v.getSprite().getName() != null) {
            fetch.rawset("tilename", v.getSprite().getName());
            if (v.getContainer() != null && v.getContainer().getType() != null) {
                String tilename = fetch.rawget("tilename") + " / Container Report: " + v.getContainer().getType();
                fetch.rawset("tilename", tilename);
            }

            fetch.rawset("tileObj", v);
        }

        if (v instanceof IsoCompost) {
            fetch.rawset("compost", v);
        }

        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.HoppableN)) {
            if (fetch.rawget("hoppableN") == null) {
                fetch.rawset("hoppableN", v);
            } else if (fetch.rawget("hoppableN") != v && fetch.rawget("hoppableN_2") != v) {
                fetch.rawset("hoppableN_2", v);
            }
        }

        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.HoppableW)) {
            if (fetch.rawget("hoppableW") == null) {
                fetch.rawset("hoppableW", v);
            } else if (fetch.rawget("hoppableW") != v && fetch.rawget("hoppableW_2") != v) {
                fetch.rawset("hoppableW_2", v);
            }
        }

        if (playerObj.isDraggingCorpse() && playerObj.getJoypadBind() != -1 && playerObj.getCurrentSquare() != null) {
            IsoDirections dir = playerObj.getCardinalDirection().Rot180();
            IsoGridSquare oppositeSq = playerObj.getCurrentSquare().getAdjacentSquare(dir);
            IsoObject window = playerObj.getCurrentSquare().getWindowTo(oppositeSq);
            if (window != null) {
                fetch.rawset("window", window);
            }

            IsoObject windowFrame = playerObj.getCurrentSquare().getWindowFrameTo(oppositeSq);
            if (windowFrame != null) {
                fetch.rawset("windowFrame", windowFrame);
            }

            IsoObject thumpableWindowFrame = playerObj.getCurrentSquare().getWindowThumpableTo(oppositeSq);
            if (thumpableWindowFrame != null) {
                fetch.rawset("thumpableWindow", thumpableWindowFrame);
            }
        }

        if (playerObj.isDraggingCorpse() && fetch.rawget("throwCorpseOver") == null) {
            fetchThrowCorpseOver(playerObj, fetch, "hoppableW");
            fetchThrowCorpseOver(playerObj, fetch, "hoppableN");
            if (playerObj.getJoypadBind() != -1) {
                IsoDirections dirx = playerObj.getCardinalDirection().Rot180();
                if (playerObj.canThrowCorpseOver(dirx)) {
                    IsoGridSquare oppositeSqx = playerObj.getCurrentSquare().getAdjacentSquare(dirx);
                    IsoObject hoppable = playerObj.getCurrentSquare().getHoppableTo(oppositeSqx);
                    fetch.rawset("throwCorpseOver", hoppable);
                }
            }
        }

        if (v instanceof IsoThumpable vIsoThumpable && !vIsoThumpable.isDoor()) {
            fetch.rawset("thump", v);
            if (vIsoThumpable.canBeLockByPadlock() && !vIsoThumpable.isLockedByPadlock() && vIsoThumpable.getLockedByCode() == 0) {
                fetch.rawset("padlockThump", v);
            }

            if (vIsoThumpable.isLockedByPadlock()) {
                fetch.rawset("padlockedThump", v);
            }

            if (vIsoThumpable.getLockedByCode() > 0) {
                fetch.rawset("digitalPadlockedThump", v);
            }

            if (vIsoThumpable.isWindow()) {
                fetch.rawset("thumpableWindow", v);
            }

            if (vIsoThumpable.isWindowN()) {
                if (fetch.rawget("thumpableWindowN") == null) {
                    fetch.rawset("thumpableWindowN", v);
                } else if (fetch.rawget("thumpableWindowN") != v && fetch.rawget("thumpableWindowN_2") != v) {
                    fetch.rawset("thumpableWindowN_2", v);
                }
            }

            if (vIsoThumpable.isWindowW()) {
                if (fetch.rawget("thumpableWindowW") == null) {
                    fetch.rawset("thumpableWindowW", v);
                } else if (fetch.rawget("thumpableWindowW") != v && fetch.rawget("thumpableWindowW_2") != v) {
                    fetch.rawset("thumpableWindowW_2", v);
                }
            }

            if (vIsoThumpable.propertyEquals("CustomName", "Rain Collector Barrel")) {
                fetch.rawset("rainCollectorBarrel", v);
            }
        }

        if (v instanceof IsoTree) {
            fetch.rawset("tree", v);
        }

        if (v instanceof IsoTree || v.getProperties() != null && v.getProperties().has(TilePropertyKey.CAN_ATTACH_ANIMAL.toString())) {
            fetch.rawset("attachAnimalTo", v);
        }

        if (v instanceof IsoClothingDryer) {
            fetch.rawset("clothingDryer", v);
        }

        if (v instanceof IsoClothingWasher) {
            fetch.rawset("clothingWasher", v);
        }

        if (v instanceof IsoCombinationWasherDryer) {
            fetch.rawset("comboWasherDryer", v);
        }

        if (v instanceof IsoStove && v.getContainer() != null) {
            fetch.rawset("stove", v);
        }

        if (v instanceof IsoDeadBody isoDeadBody && !isoDeadBody.isAnimal()) {
            IsoDeadBody body = (IsoDeadBody)fetch.rawget("body");
            if (body == null || body.DistToSquared(playerObj) > isoDeadBody.DistToSquared(playerObj)) {
                fetch.rawset("body", v);
            }
        }

        if (v instanceof IsoDeadBody isoDeadBodyx && isoDeadBodyx.isAnimal()) {
            fetch.rawset("animalbody", v);
        }

        if (v instanceof IsoCarBatteryCharger) {
            fetch.rawset("carBatteryCharger", v);
        }

        if (v instanceof IsoGenerator) {
            fetch.rawset("generator", v);
        }

        if (doSquare) {
            IsoDeadBody deadBody = v.getSquare() != null ? v.getSquare().getDeadBody() : null;
            if (deadBody != null && fetch.rawget("body") == null && deadBody.isAnimal()) {
                fetch.rawset("body", deadBody);
            }

            if (deadBody != null && fetch.rawget("animalbody") == null && deadBody.isAnimal()) {
                fetch.rawset("animalbody", deadBody);
            }
        }

        if (v.getSprite() != null) {
            if (v.getSprite().getProperties().has(IsoFlagType.bed)) {
                fetch.rawset("bed", v);
            }

            if (v.getSprite().getProperties().has(IsoFlagType.makeWindowInvincible)) {
                fetch.rawset("invincibleWindow", true);
            }
        }

        if (v instanceof IsoWindowFrame) {
            fetch.rawset("windowFrame", v);
        }

        if (v instanceof IsoBrokenGlass) {
            fetch.rawset("brokenGlass", v);
        }

        if (v instanceof IsoTrap) {
            fetch.rawset("trap", v);
        }

        if (v.getName() != null && v.getName().equals("EmptyGraves") && !isGraveFilledIn(v)) {
            fetch.rawset("graves", v);
        }

        if (v instanceof IsoLightSwitch isoLightSwitch && v.getSquare() != null && (v.getSquare().getRoom() != null || isoLightSwitch.getCanBeModified())) {
            fetch.rawset("lightSwitch", v);
        }

        if (doSquare
            && v.getSquare() != null
            && (v.getSquare().getProperties().has(IsoFlagType.HoppableW) || v.getSquare().getProperties().has(IsoFlagType.HoppableN))) {
            fetch.rawset("canClimbThrough", true);
        }

        if (v.getSprite() != null
            && v.getSprite().getProperties().has(IsoFlagType.water)
            && v.getSquare().DistToProper(playerObj.getSquare()) < 20.0F
            && !playerObj.isSitOnGround()) {
            fetch.rawset("canFish", true);
            if (v.getSquare().getWater() != null && v.getSquare().getWater().isShore()) {
                fetch.rawset("canFish", false);
            }

            if (v.getSquare().DistToProper(playerObj.getSquare()) < 8.0F && !playerObj.getInventory().getAllTypeRecurse("Base.Chum").isEmpty()) {
                fetch.rawset("canAddChum", true);
            }
        }

        if (doSquare) {
            KahluaTable groundType = getDirtGravelSand(v.getSquare());
            fetch.rawset("groundType", groundType);
            if (groundType != null) {
                fetch.rawset("groundSquare", v.getSquare());
            }
        }

        InventoryItem hasCuttingTool = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CUT_PLANT));
        if (v.getSprite() != null) {
            if (v.getSprite().getProperties().has(IsoFlagType.canBeCut) && hasCuttingTool != null) {
                fetch.rawset("canBeCut", v.getSquare());
            }

            if (v.getSprite().getProperties().has(IsoFlagType.canBeRemoved)) {
                fetch.rawset("canBeRemoved", v.getSquare());
            }
        }

        ArrayList<IsoSpriteInstance> attached = v.getAttachedAnimSprite();
        if (hasCuttingTool != null && attached != null) {
            for (int n = 0; n < attached.size(); n++) {
                IsoSpriteInstance sprite = attached.get(n);
                if (sprite != null
                    && sprite.getParentSprite() != null
                    && sprite.getParentSprite().getName() != null
                    && sprite.getParentSprite().getName().startsWith("f_wallvines_")) {
                    fetch.rawset("wallVine", v.getSquare());
                    break;
                }
            }
        }

        if (v.getSprite() != null && v.getSprite().getProperties().has(IsoFlagType.water) && playerInv.containsTypeRecurse("FishingNet")) {
            fetch.rawset("canTrapFish", true);
        }

        if (v.getName() != null && v.getName().equals("FishingNet") && v.getSquare() != null) {
            fetch.rawset("trapFish", v);
        }

        if (doSquare) {
            if (v.getSquare().getProperties().has(IsoFlagType.climbSheetN)
                || v.getSquare().getProperties().has(IsoFlagType.climbSheetW)
                || v.getSquare().getProperties().has(IsoFlagType.climbSheetS)
                || v.getSquare().getProperties().has(IsoFlagType.climbSheetE)) {
                fetch.rawset("sheetRopeSquare", v.getSquare());
            }

            if (FireFighting.getSquareToExtinguish(v.getSquare()) != null) {
                fetch.rawset("extinguisher", FireFighting.getExtinguisher(playerObj));
                fetch.rawset("firetile", v.getSquare());
            }

            fetch.rawset("clickedSquare", v.getSquare());
        }

        if (doSquare && playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CLEAR_ASHES)) != null && v.getSprite() != null) {
            String spriteName = v.getSprite().getName();
            if (spriteName == null) {
                spriteName = v.getSpriteName();
            }

            if (spriteName.equals("floors_burnt_01_1") || spriteName.equals("floors_burnt_01_2")) {
                IsoObject ashes = (IsoObject)fetch.rawget("ashes");
                if (ashes == null || ashes.getTargetAlpha() <= v.getTargetAlpha()) {
                    fetch.rawset("ashes", v);
                }
            }
        }

        if (doSquare) {
            InventoryItem sledgehammer = playerInv.getFirstRecurse(item -> !item.isBroken() && compareType("Sledgehammer", item));
            if (sledgehammer == null) {
                sledgehammer = playerInv.getFirstRecurse(item -> !item.isBroken() && compareType("Sledgehammer2", item));
            }

            if (sledgehammer != null
                && sledgehammer.getCondition() > 0
                && v.getSprite() != null
                && (
                    v.getSprite().getProperties().has(IsoFlagType.solidtrans)
                        || v.getSprite().getProperties().has(IsoFlagType.collideW)
                        || v.getSprite().getProperties().has(IsoFlagType.collideN)
                        || v.getSprite().getProperties().has(IsoFlagType.bed)
                        || v instanceof IsoThumpable
                        || v.getSprite().getProperties().has(IsoFlagType.windowN)
                        || v.getSprite().getProperties().has(IsoFlagType.windowW)
                        || v.getType() == IsoObjectType.stairsBN
                        || v.getType() == IsoObjectType.stairsMN
                        || v.getType() == IsoObjectType.stairsTN
                        || v.getType() == IsoObjectType.stairsBW
                        || v.getType() == IsoObjectType.stairsMW
                        || v.getType() == IsoObjectType.stairsTW
                        || (v.getProperties().has("DoorWallN") || v.getProperties().has("DoorWallW")) && !v.getSquare().haveDoor()
                        || v.getSprite().getProperties().has(IsoFlagType.waterPiped)
                )
                && (
                    v.getSprite().getName() == null
                        || !v.getSprite().getName().startsWith("blends_natural_02")
                        || !v.getSprite().getName().startsWith("floors_burnt_01_")
                )) {
                IsoObject destroy = (IsoObject)fetch.rawget("destroy");
                if (destroy == null || destroy.getTargetAlpha() <= v.getTargetAlpha()) {
                    fetch.rawset("destroy", v);
                }
            }

            if (canCleanBlood(playerObj, v.getSquare())) {
                fetch.rawset("haveBlood", v.getSquare());
            }

            if (canCleanGraffiti(playerObj, v.getSquare())) {
                fetch.rawset("haveGraffiti", v.getSquare());
            }
        }

        if (v instanceof IsoPlayer && v != playerObj) {
            fetch.rawset("clickedPlayer", v);
        }

        if (v instanceof IsoAnimal) {
            KahluaTable clickedAnimals = (KahluaTable)fetch.rawget("clickedAnimals");
            if (!LuaHelpers.tableContainsValue(clickedAnimals, v)) {
                clickedAnimals.rawset(clickedAnimals.size() + 1, v);
            }
        }

        if (v.getPipedFuelAmount() > 0) {
            if (playerInv.getFirstRecurse(ISWorldObjectContextMenuLogic::predicateStoreFuel) != null) {
                fetch.rawset("haveFuel", v);
            }

            fetch.rawset("haveFuelDebug", v);
        }

        if (v.getSprite() != null && v.getSprite().getProperties().has("fuelAmount")) {
            fetch.rawset("fuelPump", v);
        }

        if (v instanceof IsoFeedingTrough feedingTroughx) {
            IsoFeedingTrough var47 = feedingTroughx.getMasterTrough();
            if (var47 != null && var47.hasComponent(ComponentType.FluidContainer)) {
                KahluaTable fluidcontainer = (KahluaTable)fetch.rawget("fluidcontainer");
                if (!LuaHelpers.tableContainsValue(fluidcontainer, var47)) {
                    fluidcontainer.rawset(fluidcontainer.size() + 1, var47);
                }
            }
        } else if (v.hasComponent(ComponentType.FluidContainer)) {
            KahluaTable fluidcontainer = (KahluaTable)fetch.rawget("fluidcontainer");
            if (!LuaHelpers.tableContainsValue(fluidcontainer, v)) {
                fluidcontainer.rawset(fluidcontainer.size() + 1, v);
            }
        }

        if (doSquare) {
            fetch.rawset("safehouse", SafeHouse.getSafeHouse(v.getSquare()));
            fetch.rawset("safehouseAllowInteract", SafeHouse.isSafehouseAllowInteract(v.getSquare(), playerObj));
            fetch.rawset("safehouseAllowLoot", SafeHouse.isSafehouseAllowLoot(v.getSquare(), playerObj));
        }

        boolean preWaterShutoff = GameTime.instance.getWorldAgeHours() / 24.0 + (SandboxOptions.instance.getTimeSinceApo() - 1) * 30
            < SandboxOptions.instance.waterShutModifier.getValue();
        boolean vCanBeWaterPiped = LuaHelpers.castBoolean(v.getModData().rawget("canBeWaterPiped"));
        if (v.hasModData() && v.getSquare() != null && vCanBeWaterPiped && v.getSquare().isInARoom() && v.FindExternalWaterSource() != null) {
            fetch.rawset("canBeWaterPiped", v);
        }

        if (props != null
            && v.getSquare() != null
            && props.has(IsoFlagType.waterPiped)
            && !v.getUsesExternalWaterSource()
            && (v.getSquare().isInARoom() && v.FindExternalWaterSource() != null || v.getSquare().getRoom() != null && preWaterShutoff && vCanBeWaterPiped)) {
            fetch.rawset("canBeWaterPiped", v);
        }

        if (props != null) {
            fetchPickupItems(fetch, v, props, playerInv);
        }

        if (v instanceof IHasHealth) {
            fetch.rawset("health", v);
        }

        fetch.rawset("item", v);
        if (doSquare && v.getSquare() != null && fetchSquares.rawget(v.getSquare()) == null) {
            for (int i = 0; i < v.getSquare().getObjects().size(); i++) {
                fetch(fetch, v.getSquare().getObjects().get(i), player, false);
            }

            for (int i = 0; i < v.getSquare().getStaticMovingObjects().size(); i++) {
                fetch(fetch, v.getSquare().getStaticMovingObjects().get(i), player, false);
            }

            KahluaTable clickedAnimals = (KahluaTable)fetch.rawget("clickedAnimals");

            for (int x = v.getSquare().getX() - 1; x <= v.getSquare().getX() + 1; x++) {
                for (int y = v.getSquare().getY() - 1; y <= v.getSquare().getY() + 1; y++) {
                    IsoGridSquare sq = v.getSquare().getCell().getGridSquare(x, y, v.getSquare().getZ());
                    if (sq != null) {
                        for (int i = 0; i < sq.getMovingObjects().size(); i++) {
                            IsoMovingObject o = sq.getMovingObjects().get(i);
                            if (o instanceof IsoPlayer && o != playerObj) {
                                fetch.rawset("clickedPlayer", o);
                            }

                            if (o instanceof IsoAnimal && !LuaHelpers.tableContainsValue(clickedAnimals, o)) {
                                clickedAnimals.rawset(clickedAnimals.size() + 1, o);
                            }
                        }
                    }
                }
            }
        }

        if (doSquare) {
            fetch.rawset("animalZone", DesignationZoneAnimal.getZone(v.getSquare().getX(), v.getSquare().getY(), v.getSquare().getZ()));
            fetchSquares.rawset(v.getSquare(), true);
        }
    }

    private static KahluaTable getAllObjects(KahluaTable worldobjects) {
        KahluaTable copy = LuaManager.platform.newTable();
        Set<IsoGridSquare> doneSquares = new HashSet<>();
        Set<IsoObject> doneObjects = new HashSet<>();
        KahluaTableIterator it = worldobjects.iterator();

        while (it.advance()) {
            IsoObject object = (IsoObject)it.getValue();
            if (!doneObjects.contains(object)) {
                getAllObjectOnSquare(object.getSquare(), doneSquares, doneObjects, copy);
                if (doneObjects.add(object)) {
                    copy.rawset(copy.len() + 1, object);
                }
            }
        }

        doneSquares.clear();
        doneObjects.clear();
        return copy;
    }

    private static void getAllObjectOnSquare(IsoGridSquare square, Set<IsoGridSquare> doneSquares, Set<IsoObject> doneObjects, KahluaTable worldobjects) {
        if (square != null && doneSquares.add(square)) {
            for (int i = 0; i < square.getObjects().size(); i++) {
                IsoObject object = square.getObjects().get(i);
                if (doneObjects.add(object)) {
                    worldobjects.rawset(worldobjects.len() + 1, object);
                }
            }
        }
    }

    public static boolean createMenuEntries(KahluaTable fetch, KahluaTable context, double player, KahluaTable worldobjects, int x, int y, boolean test) {
        KahluaTable inventoryPaneContextMenu = (KahluaTable)LuaManager.env.rawget("ISInventoryPaneContextMenu");
        ISContextMenuWrapper contextWrapper = new ISContextMenuWrapper(context);
        IsoPlayer playerObj = IsoPlayer.players[(int)player];
        ItemContainer playerInv = playerObj.getInventory();
        IHasHealth health = (IHasHealth)fetch.rawget("health");
        if (DebugOptions.instance.uiShowContextMenuReportOptions.getValue() && health != null && Core.debug && health.getMaxHealth() > 0) {
            String text = String.format(Locale.ENGLISH, "%s: %d/%d", Translator.getText("ContextMenu_ObjectHealth"), health.getHealth(), health.getMaxHealth());
            contextWrapper.addDebugOption(text, null, null);
        }

        if (fetch.rawget("tilename") != null && (Core.debug || SandboxOptions.instance.isUnstableScriptNameSpam())) {
            addTileDebugInfo(contextWrapper, fetch);
        }

        IsoGridSquare clickedSquare = (IsoGridSquare)fetch.rawget("clickedSquare");
        if (DebugOptions.instance.uiShowContextMenuReportOptions.getValue() && (Core.debug || SandboxOptions.instance.isUnstableScriptNameSpam())) {
            if (clickedSquare != null && clickedSquare.getRoom() != null && clickedSquare.getRoom().getRoomDef() != null) {
                contextWrapper.addDebugOption(
                    String.format(
                        Locale.ENGLISH,
                        "%s: %s, x: %d, y: %d, z: %d",
                        Translator.getText("Room Report"),
                        clickedSquare.getRoom().getRoomDef().getName(),
                        clickedSquare.getX(),
                        clickedSquare.getY(),
                        clickedSquare.getZ()
                    ),
                    null,
                    null
                );
            } else if (clickedSquare != null) {
                contextWrapper.addDebugOption(
                    String.format(Locale.ENGLISH, "Coordinates Report x: %d, y: %d, z: %d", clickedSquare.getX(), clickedSquare.getY(), clickedSquare.getZ()),
                    null,
                    null
                );
            }

            contextWrapper.addDebugOption(String.format("Seed: %s", WorldGenParams.INSTANCE.getSeedString()), null, null);
        }

        KahluaTable allObjects = getAllObjects(worldobjects);
        KahluaTable worldobjectsCopy = LuaManager.copyTable(worldobjects);
        LuaHelpers.callLuaClass("DebugContextMenu", "doDebugMenu", null, player, context, worldobjectsCopy, test);
        worldobjectsCopy.wipe();
        boolean safehouseAllowInteract = LuaHelpers.castBoolean(fetch.rawget("safehouseAllowInteract"));
        if (safehouseAllowInteract) {
            IsoGridSquare firetile = (IsoGridSquare)fetch.rawget("firetile");
            InventoryItem extinguisher = (InventoryItem)fetch.rawget("extinguisher");
            if (firetile != null && extinguisher != null && doExtinguishFireMenu(worldobjects, test, contextWrapper, firetile, extinguisher, playerObj)) {
                return true;
            }

            InventoryItem heavyItem = playerObj.getPrimaryHandItem();
            if (isForceDropHeavyItem(heavyItem)) {
                KahluaTable newTable = LuaManager.platform.newTable();
                newTable.rawset(1, heavyItem);
                contextWrapper.addOption(
                    Translator.getText("ContextMenu_DropNamedItem", heavyItem.getDisplayName()), newTable, inventoryPaneContextMenu.rawget("onUnEquip"), player
                );
            }

            handleInteraction(x, y, test, contextWrapper, worldobjects, playerObj, playerInv);
            if (handleGrabWorldItem(fetch, x, y, test, contextWrapper, worldobjects, playerObj, playerInv)) {
                return true;
            }

            IsoDeadBody body = (IsoDeadBody)fetch.rawget("body");
            if (body != null
                && !body.isAnimal()
                && playerObj.getVehicle() == null
                && doBurnBodyMenu(player, worldobjects, test, playerInv, contextWrapper, body)) {
                return true;
            }

            if (LuaHelpers.getJoypadState(player) != null && playerObj.getCurrentSquare() != null) {
                IsoWindow window = (IsoWindow)fetch.rawget("window");
                IsoThumpable thumpableWindow = (IsoThumpable)fetch.rawget("thumpableWindow");
                if (window != null && thumpableWindow != null) {
                    IsoObject thumpableWindow2 = getBestWindowFrame(playerObj);
                    if (thumpableWindow2 instanceof IsoWindowFrame windowFrame) {
                        fetch.rawset("window", thumpableWindow2.getSquare().getWindow(windowFrame.getNorth()));
                        fetch.rawset("windowFrame", thumpableWindow2);
                    } else if (thumpableWindow2 instanceof IsoThumpable windowFrame) {
                        fetch.rawset("window", thumpableWindow2.getSquare().getWindow(windowFrame.getNorth()));
                        fetch.rawset("thumpableWindow", thumpableWindow2);
                    }
                }
            }

            boolean hasHammer = playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.HAMMER) && !item.isBroken()) != null;
            boolean hasRemoveBarricadeTool = playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.REMOVE_BARRICADE) && !item.isBroken()) != null;
            boolean thumpableWindowResult = doThumpableWindowFrameMenu(fetch, worldobjects, test, contextWrapper, player, hasRemoveBarricadeTool);
            if (thumpableWindowResult) {
                return true;
            }

            boolean invincibleWindow = LuaHelpers.castBoolean(fetch.rawget("invincibleWindow"));
            IsoThumpable thump = (IsoThumpable)fetch.rawget("thump");
            IsoWindow window = (IsoWindow)fetch.rawget("window");
            if (thump != null
                && !invincibleWindow
                && window == null
                && doThumpableMenu(thump, player, worldobjects, test, invincibleWindow, playerObj, hasRemoveBarricadeTool, contextWrapper)) {
                return true;
            }

            IsoObject throwOver = (IsoObject)fetch.rawget("throwCorpseOver");
            if (throwOver != null && doHoppableMenu(test, contextWrapper, throwOver, playerObj)) {
                return true;
            }

            ISContextMenuWrapper windowContextWrapper = doWindowMenu(
                fetch, worldobjects, test, contextWrapper, window, invincibleWindow, playerObj, hasRemoveBarricadeTool, hasHammer
            );
            if (windowContextWrapper == null) {
                return true;
            }

            if (windowContextWrapper.getNumOptions() <= 1.0) {
                contextWrapper.removeLastOption();
            }

            IsoBrokenGlass brokenGlass = (IsoBrokenGlass)fetch.rawget("brokenGlass");
            if (brokenGlass != null) {
                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_PickupBrokenGlass"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPickupBrokenGlass"),
                    brokenGlass,
                    playerObj.getIndex()
                );
            }

            ISContextMenuWrapper doorContextWrapper = doDoorMenu(fetch, context, worldobjects, test, contextWrapper, playerObj, hasRemoveBarricadeTool);
            if (doorContextWrapper == null) {
                return true;
            }

            if (doorContextWrapper.getNumOptions() <= 1.0) {
                contextWrapper.removeLastOption();
            }

            if (doGeneratorMenu(fetch, player, worldobjects, test, playerObj, contextWrapper, playerInv)) {
                return true;
            }

            IsoObject container = (IsoObject)fetch.rawget("container");
            if (Core.debug && container != null && ItemPickerJava.getLootDebugString(container) != null) {
                contextWrapper.addDebugOption(String.format(Locale.ENGLISH, ItemPickerJava.getLootDebugString(container)), null, null);
            }

            IsoObject haveFuelDebug = (IsoObject)fetch.rawget("haveFuelDebug");
            if (haveFuelDebug != null && Core.debug) {
                contextWrapper.addDebugOption(
                    String.format(Locale.ENGLISH, "%s: %d", Translator.getText("ContextMenu_PumpFuelAmount"), haveFuelDebug.getPipedFuelAmount()), null, null
                );
            }

            Object butcherHook = fetch.rawget("butcherHook");
            if (butcherHook != null && playerObj.getVehicle() == null) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_ButcherHook"),
                    butcherHook,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onButcherHook"),
                    playerObj
                );
            }

            if (doCarBatteryMenu(fetch, context, worldobjects, test, playerObj, playerInv)) {
                return true;
            }

            LuaHelpers.callLuaClass("ISTrapMenu", "doTrapMenu", null, player, context, worldobjects, test);
            LuaHelpers.callLuaClass("ISFarmingMenu", "doFarmingMenu", null, player, context, worldobjects, test);
            LuaHelpers.callLuaClass("ISBBQMenu", "OnFillWorldObjectContextMenu", null, player, context, worldobjects, test);
            LuaHelpers.callLuaClass("ISCampingMenu", "doCampingMenu", null, player, context, worldobjects, test);
            Object entityContext = fetch.rawget("entityContext");
            if (entityContext != null) {
                if (test) {
                    return true;
                }

                doContextConfigOptionsFromFetch(contextWrapper, fetch, playerObj);
            }

            if (safehouseAllowInteract) {
                if (test) {
                    return true;
                }

                LuaHelpers.callLuaClass("ISRadioAndTvMenu", "createMenu", allObjects, context, playerObj);
            }

            Object ashes = fetch.rawget("ashes");
            if (ashes != null) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Clear_Ashes"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClearAshes"),
                    player,
                    ashes
                );
            }

            Object ore = fetch.rawget("ore");
            if (ore != null && playerObj.getVehicle() == null) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Remove_Ore"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveGroundCoverItemHammerOrPickAxe"),
                    player,
                    ore
                );
            }

            IsoTrap trap = (IsoTrap)fetch.rawget("trap");
            if (trap != null && trap.getItem() != null && doTrapMenu(player, worldobjects, test, contextWrapper)) {
                return true;
            }

            IsoDeadBody pickedCorpse = (IsoDeadBody)IsoObjectPicker.Instance.PickCorpse(x, y);
            if (pickedCorpse != null && pickedCorpse.isAnimal()) {
                fetch.rawset("animalbody", pickedCorpse);
            }

            IsoDeadBody animalbody = (IsoDeadBody)fetch.rawget("animalbody");
            if (playerObj.getVehicle() == null && animalbody != null) {
                LuaHelpers.callLuaClass("AnimalContextMenu", "doAnimalBodyMenu", null, context, player, animalbody);
            }

            if (doPadLockMenu(fetch, player, worldobjects, test, playerInv, contextWrapper)) {
                return true;
            }

            IsoObject canBeWaterPiped = (IsoObject)fetch.rawget("canBeWaterPiped");
            if (canBeWaterPiped != null && doWaterPipeMenu(player, worldobjects, test, canBeWaterPiped, contextWrapper, playerInv)) {
                return true;
            }

            KahluaTable groundType = (KahluaTable)fetch.rawget("groundType");
            if (groundType != null && groundType.rawget("sand") != null && playerObj.isRecipeActuallyKnown("MakeChum")) {
                doCreateChumOptions(contextWrapper, playerObj, (IsoGridSquare)fetch.rawget("groundSquare"));
            }

            IsoGridSquare sheetRopeSquare = (IsoGridSquare)fetch.rawget("sheetRopeSquare");
            if (sheetRopeSquare != null && playerObj.canClimbSheetRope(sheetRopeSquare) && playerObj.getPerkLevel(PerkFactory.Perks.Strength) >= 0) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Climb_Sheet_Rope"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbSheetRope"),
                    sheetRopeSquare,
                    false,
                    player
                );
            }

            KahluaTableIterator iterator = ((KahluaTable)fetch.rawget("fluidcontainer")).iterator();

            while (iterator.advance()) {
                IsoObject fluidcontainer = (IsoObject)iterator.getValue();
                if (fluidcontainer instanceof IsoFeedingTrough feedingTrough) {
                    fluidcontainer = feedingTrough.getMasterTrough();
                }

                if (fluidcontainer != null && !(fluidcontainer instanceof IsoWorldInventoryObject)) {
                    ISContextMenuWrapper submenu = doFluidContainerMenu(contextWrapper, fluidcontainer, player, playerObj, worldobjects);
                    addFluidFromItem(fetch, test, submenu, fluidcontainer, worldobjects, playerObj, playerInv);
                    if (fluidcontainer.hasComponent(ComponentType.ContextMenuConfig)) {
                        doContextConfigOptions(submenu, fluidcontainer, playerObj);
                    }
                }
            }

            IsoClothingWasher clothingWasher = (IsoClothingWasher)fetch.rawget("clothingWasher");
            IsoCombinationWasherDryer comboWasherDryer = (IsoCombinationWasherDryer)fetch.rawget("comboWasherDryer");
            KahluaTable fluidcontainerx = (KahluaTable)fetch.rawget("fluidcontainer");
            KahluaTable fetchStoreWater = (KahluaTable)fetch.rawget("storeWater");
            if (doWashClothingOrYourselfMenu(
                fetch, player, worldobjects, fetchStoreWater, fluidcontainerx, contextWrapper, clothingWasher, comboWasherDryer, playerObj
            )) {
                return true;
            }

            if (fetchStoreWater.isEmpty() && comboWasherDryer != null && toggleComboWasherDryer(contextWrapper, playerObj, comboWasherDryer, true)) {
                return true;
            }

            LuaHelpers.callLuaClass("ISFeedingTroughMenu", "OnFillWorldObjectContextMenu", null, player, context, worldobjects, test);
            IsoObject rainCollectorBarrel = (IsoObject)fetch.rawget("rainCollectorBarrel");
            if (rainCollectorBarrel != null && !LuaHelpers.tableContainsValue(fluidcontainerx, rainCollectorBarrel)) {
                addFluidFromItem(fetch, test, contextWrapper, rainCollectorBarrel, worldobjects, playerObj, playerInv);
            }

            IsoWorldInventoryObject worldItem = (IsoWorldInventoryObject)fetch.rawget("worldItem");
            if (worldItem != null
                && worldItem.getItem() != null
                && worldItem.getFluidCapacity() > 0.0F
                && !LuaHelpers.tableContainsValue(fluidcontainerx, worldItem)) {
                addFluidFromItem(fetch, test, contextWrapper, worldItem, worldobjects, playerObj, playerInv);
            }

            IsoClothingDryer clothingDryer = (IsoClothingDryer)fetch.rawget("clothingDryer");
            if (clothingDryer != null) {
                onWashingDryer(getMoveableDisplayName(clothingDryer), contextWrapper, clothingDryer, playerObj, worldobjects);
            }

            if (doStoveOption(fetch, test, contextWrapper, player, playerObj)) {
                return true;
            }

            IsoObject bed = (IsoObject)fetch.rawget("bed");
            if ((bed != null || playerObj.getStats().get(CharacterStat.FATIGUE) > 0.9F) && doBedMenu(player, test, contextWrapper, bed, playerObj)) {
                return true;
            }

            boolean lightSwitchResult = LuaHelpers.castBoolean(
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "doLightSwitchOption", null, test, context, player)
            );
            if (lightSwitchResult) {
                return true;
            }

            if (doTreeMenu(fetch, context, worldobjects, test, playerObj, playerInv, contextWrapper)) {
                return true;
            }

            if (clickedSquare != null && doFishingMenu(fetch, worldobjects, test, contextWrapper, playerObj, clickedSquare, player, playerInv)) {
                return true;
            }

            if (doFuelMenu(fetch, context, player, test, contextWrapper)) {
                return true;
            }

            IsoPlayer clickedPlayer = (IsoPlayer)fetch.rawget("clickedPlayer");
            if (clickedPlayer != null && clickedPlayer != playerObj && doClickedPlayerMenu(worldobjects, test, playerObj, clickedPlayer, contextWrapper)) {
                return true;
            }

            if (doCleaningMenu(fetch, player, worldobjects, test, playerObj, contextWrapper)) {
                return true;
            }
        }

        SafeHouse safehouse = (SafeHouse)fetch.rawget("safehouse");
        if (safehouse != null && safehouse.playerAllowed(playerObj)) {
            if (test) {
                return true;
            }

            contextWrapper.addOption(
                Translator.getText("ContextMenu_ViewSafehouse"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onViewSafeHouse"),
                safehouse,
                playerObj
            );
        }

        boolean safehouseAllowLoot = LuaHelpers.castBoolean(fetch.rawget("safehouseAllowLoot"));
        if (!safehouseAllowLoot) {
            KahluaTable nope = contextWrapper.addOption(Translator.getText("ContextMenu_NoSafehousePermissionObjects"), null, null);
            nope.rawset("notAvailable", true);
            LuaHelpers.callLuaClass("HaloTextHelper", "addBadText", null, playerObj, Translator.getText("ContextMenu_NoSafehousePermissionObjects"));
        }

        if (safehouse == null && clickedSquare != null && clickedSquare.getBuilding() != null && clickedSquare.getBuilding().getDef() != null) {
            String reason = SafeHouse.canBeSafehouse(clickedSquare, playerObj);
            if (reason != null) {
                if (test) {
                    return true;
                }

                KahluaTable option = contextWrapper.addOption(
                    Translator.getText("ContextMenu_SafehouseClaim"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeSafeHouse"),
                    clickedSquare,
                    player
                );
                if (!reason.isEmpty()) {
                    ISToolTipWrapper toolTip = addToolTip();
                    toolTip.setVisible(false);
                    toolTip.getTable().rawset("description", reason);
                    option.rawset("notAvailable", true);
                    option.rawset("toolTip", toolTip.getTable());
                }
            }
        }

        if (safehouseAllowInteract) {
            KahluaTable clickedAnimals = (KahluaTable)fetch.rawget("clickedAnimals");
            if (clickedAnimals != null && !clickedAnimals.isEmpty()) {
                LuaEventManager.triggerEvent("OnClickedAnimalForContext", player, context, clickedAnimals, test);
            }

            Object animaltrack = fetch.rawget("animaltrack");
            if (animaltrack != null) {
                LuaHelpers.callLuaClass("ISAnimalTracksMenu", "handleIsoTracks", null, context, animaltrack, playerObj);
            }
        }

        if (SafeHouse.isSafehouseAllowClaimWar(safehouse, playerObj)) {
            contextWrapper.addOption(
                Translator.getText("ContextMenu_WarClaim"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClaimWar"),
                safehouse.getOnlineID(),
                playerObj.getUsername()
            );
        }

        DesignationZoneAnimal animalZone = (DesignationZoneAnimal)fetch.rawget("animalZone");
        if (animalZone != null) {
            LuaHelpers.callLuaClass("AnimalContextMenu", "doDesignationZoneMenu", null, context, animalZone, playerObj);
        }

        if (safehouseAllowInteract && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }

            InventoryItem rakedung = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.TAKE_DUNG));
            if (playerObj.getPrimaryHandItem() != null
                && !playerObj.getPrimaryHandItem().isBroken()
                && playerObj.getPrimaryHandItem().hasTag(ItemTag.TAKE_DUNG)) {
                rakedung = playerObj.getPrimaryHandItem();
            }

            InventoryItem shovel = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.DIG_GRAVE));
            if (playerObj.getPrimaryHandItem() != null
                && !playerObj.getPrimaryHandItem().isBroken()
                && playerObj.getPrimaryHandItem().hasTag(ItemTag.DIG_GRAVE)) {
                shovel = playerObj.getPrimaryHandItem();
            }

            IsoObject graves = (IsoObject)fetch.rawget("graves");
            if (shovel != null && doShovelAndRackMenu(player, worldobjects, test, contextWrapper, shovel, rakedung, playerObj, graves)) {
                return true;
            }

            if (graves != null
                && !isGraveFullOfCorpses(graves)
                && (playerObj.isGrappling() || playerObj.getPrimaryHandItem() != null && playerObj.getPrimaryHandItem().hasTag(ItemTag.ANIMAL_CORPSE))
                && doBuryCorpseMenu(player, test, graves, contextWrapper, playerObj)) {
                return true;
            }

            if (rakedung != null && playerObj.getVehicle() == null && rakedung != shovel) {
                KahluaTable rakeOption = contextWrapper.addOption(Translator.getText("ContextMenu_Rake"), worldobjects, null);
                ISContextMenuWrapper rakeMenu = ISContextMenuWrapper.getNew(contextWrapper);
                contextWrapper.addSubMenu(rakeOption, rakeMenu.getTable());
                if (test) {
                    return true;
                }

                rakeMenu.addGetUpOption(
                    Translator.getText("ContextMenu_RakeDung"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRakeDung"), rakedung
                );
            }

            doGardeningSubmenu(contextWrapper, worldobjects, playerObj, fetch, test);
            LuaHelpers.callLuaClass("ISDisassembleMenu", "createMenu", allObjects, context, playerObj);
            doDestroyMenu(worldobjects, playerInv, contextWrapper, playerObj);
        }

        if (playerObj.getVehicle() == null && !playerObj.isSitOnGround() && !playerObj.isSittingOnFurniture()) {
            if (test) {
                return true;
            }

            contextWrapper.addOption(
                Translator.getText("ContextMenu_SitGround"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSitOnGround")
            );
        }

        if (LuaHelpers.getJoypadState(player) == null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }

            contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_Walk_to"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWalkTo"),
                fetch.rawget("item"),
                player
            );
        }

        return false;
    }

    private static void doDestroyMenu(KahluaTable worldobjects, ItemContainer playerInv, ISContextMenuWrapper contextWrapper, IsoPlayer playerObj) {
        InventoryItem sledgehammer = playerInv.getFirstTagRecurse(ItemTag.SLEDGEHAMMER);
        if (sledgehammer == null && playerObj.isBuildCheat()) {
            sledgehammer = InventoryItemFactory.CreateItem(ItemKey.Weapon.SLEDGEHAMMER);
        }

        if (sledgehammer != null
            && !sledgehammer.isBroken()
            && (!GameClient.client || ServerOptions.getInstance().getBoolean("AllowDestructionBySledgehammer"))) {
            KahluaTable option = contextWrapper.addOption(
                Translator.getText("ContextMenu_Destroy"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onDestroy"),
                playerObj,
                sledgehammer
            );
            option.rawset("iconTexture", sledgehammer.getIcon().splitIcon());
        }
    }

    private static boolean doClickedPlayerMenu(
        KahluaTable worldobjects, boolean test, IsoPlayer playerObj, IsoPlayer clickedPlayer, ISContextMenuWrapper contextWrapper
    ) {
        boolean clickedPlayerConditionsCheck = !clickedPlayer.isAnimal()
            && (!clickedPlayer.isInvisible() || playerObj.getRole() != null && playerObj.getRole().hasCapability(Capability.SeesInvisiblePlayers));
        if (!playerObj.hasTrait(CharacterTrait.HEMOPHOBIC) && clickedPlayerConditionsCheck) {
            if (test) {
                return true;
            }

            KahluaTable option = contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_Medical_Check"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onMedicalCheck"),
                playerObj,
                clickedPlayer
            );
            if (RequestMedicalCheckPacket.isCheckForbidden(playerObj, clickedPlayer)) {
                ISToolTipWrapper tooltip = addToolTip();
                option.rawset("notAvailable", true);
                tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GetCloser", clickedPlayer.getDisplayName()));
                option.rawset("toolTip", tooltip.getTable());
            }
        }

        if (clickedPlayer.isAsleep() && Core.debug) {
            if (test) {
                return true;
            }

            KahluaTable var10 = contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_Wake_Other", clickedPlayer.getDisplayName()),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWakeOther"),
                playerObj,
                clickedPlayer
            );
        }

        if (GameClient.client && clickedPlayerConditionsCheck && GameClient.canSeePlayerStats()) {
            if (test) {
                return true;
            }

            contextWrapper.addGetUpOption(
                "Check Stats", worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCheckStats"), playerObj, clickedPlayer
            );
        }

        if (!clickedPlayer.isAsleep() && clickedPlayerConditionsCheck && GameClient.client) {
            KahluaTable tradingUi = (KahluaTable)LuaManager.env.rawget("ISTradingUI");
            KahluaTable tradingUiInstance = tradingUi != null ? (KahluaTable)LuaHelpers.callLuaClass("ISTradingUI", "GetUIForPlayer", null, playerObj) : null;
            if (tradingUiInstance == null || !LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISTradingUI", "isVisible", tradingUiInstance))) {
                KahluaTable option = contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Trade", clickedPlayer.getDisguisedDisplayName()),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTrade"),
                    playerObj,
                    clickedPlayer
                );
                if (Math.abs(playerObj.getX() - clickedPlayer.getX()) > 2.0F || Math.abs(playerObj.getY() - clickedPlayer.getY()) > 2.0F) {
                    ISToolTipWrapper tooltip = addToolTip();
                    option.rawset("notAvailable", true);
                    tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GetCloserToTrade", clickedPlayer.getDisguisedDisplayName()));
                    option.rawset("toolTip", tooltip.getTable());
                }
            }
        }

        return false;
    }

    private static boolean doBurnBodyMenu(
        double player, KahluaTable worldobjects, boolean test, ItemContainer playerInv, ISContextMenuWrapper contextWrapper, IsoDeadBody body
    ) {
        boolean hasHalfLitrePetrol = playerInv.getFirstRecurse(item -> {
            FluidContainer fc = item.getFluidContainer();
            return fc != null && fc.contains(Fluid.Petrol) && fc.getAmount() > 0.5F;
        }) != null;
        if (hasHalfLitrePetrol
            && (playerInv.containsTagRecurse(ItemTag.START_FIRE) || playerInv.containsTypeRecurse("Lighter") || playerInv.containsTypeRecurse("Matches"))) {
            if (test) {
                return true;
            }

            contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_Burn_Corpse"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onBurnCorpse"),
                player,
                body
            );
        }

        return false;
    }

    private static boolean doExtinguishFireMenu(
        KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper, IsoGridSquare firetile, InventoryItem extinguisher, IsoPlayer playerObj
    ) {
        if (test) {
            return true;
        } else {
            contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_ExtinguishFire"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveFire"),
                firetile,
                extinguisher,
                playerObj
            );
            return false;
        }
    }

    private static boolean doBuryCorpseMenu(double player, boolean test, IsoObject graves, ISContextMenuWrapper contextWrapper, IsoPlayer playerObj) {
        if (test) {
            return true;
        } else {
            Double corpseCount = LuaHelpers.castDouble(graves.getModData().rawget("corpses"));
            KahluaTable option = contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_BuryCorpse", corpseCount.intValue()),
                graves,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onBuryCorpse"),
                player,
                playerObj.getPrimaryHandItem()
            );
            if (playerObj.DistToSquared(graves.getX() + 0.5F, graves.getY() + 0.5F) > 1.5F) {
                option.rawset("notAvailable", true);
                ISToolTipWrapper toolTipWrapper = new ISToolTipWrapper();
                toolTipWrapper.initialise();
                toolTipWrapper.setVisible(false);
                toolTipWrapper.setName(Translator.getText("ContextMenu_BuryCorpse", corpseCount));
                toolTipWrapper.getTable().rawset("description", Translator.getText("Tooltip_grave_addcorpse_far"));
            }

            return false;
        }
    }

    private static boolean doWashClothingOrYourselfMenu(
        KahluaTable fetch,
        double player,
        KahluaTable worldobjects,
        KahluaTable fetchStoreWater,
        KahluaTable fluidcontainer,
        ISContextMenuWrapper contextWrapper,
        IsoClothingWasher clothingWasher,
        IsoCombinationWasherDryer comboWasherDryer,
        IsoPlayer playerObj
    ) {
        KahluaTableIterator iterator = fetchStoreWater.iterator();

        while (iterator.advance()) {
            IsoObject storeWater = (IsoObject)iterator.getValue();
            if (storeWater != null && !LuaHelpers.tableContainsValue(fluidcontainer, storeWater)) {
                String source = getMoveableDisplayName(storeWater);
                if (source == null && storeWater instanceof IsoWorldInventoryObject isoWorldInventoryObject && isoWorldInventoryObject.getItem() != null) {
                    source = storeWater.getFluidUiName();
                }

                if (source == null) {
                    source = Translator.getText("ContextMenu_NaturalWaterSource");
                }

                KahluaTable mainOption = contextWrapper.addOption(source, null, null);
                Texture var15 = Texture.trygetTexture(storeWater.getSpriteName());
                if (var15 instanceof Texture) {
                    mainOption.rawset("iconTexture", var15.splitIcon());
                }

                ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(contextWrapper);
                contextWrapper.addSubMenu(mainOption, mainSubMenu.getTable());
                if (storeWater.hasWater() && fetch.rawget("clothingDryer") == null && storeWater != clothingWasher && storeWater != comboWasherDryer) {
                    doWashClothingMenu(storeWater, player, playerObj, mainSubMenu);
                    doRecipeUsingWaterMenu(storeWater, playerObj, mainSubMenu);
                }

                if (!Core.getInstance().getGameMode().equals("LastStand")) {
                    doDrinkWaterMenu(storeWater, player, playerObj, worldobjects, mainSubMenu);
                    doFillFluidMenu(storeWater, player, playerObj, worldobjects, mainSubMenu);
                }

                if (storeWater == clothingWasher && toggleClothingWasher(mainSubMenu, worldobjects, player, playerObj, clothingWasher)) {
                    return true;
                }

                if (storeWater == comboWasherDryer && toggleComboWasherDryer(mainSubMenu, playerObj, comboWasherDryer, false)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean doShovelAndRackMenu(
        double player,
        KahluaTable worldobjects,
        boolean test,
        ISContextMenuWrapper contextWrapper,
        InventoryItem shovel,
        InventoryItem rakedung,
        IsoPlayer playerObj,
        IsoObject graves
    ) {
        KahluaTable shovelOption = contextWrapper.addOption(Translator.getText("ContextMenu_Shovel"), worldobjects, null);
        shovelOption.rawset("iconTexture", shovel.getTex());
        ISContextMenuWrapper shovelMenu = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(shovelOption, shovelMenu.getTable());
        if (rakedung != null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }

            shovelMenu.addGetUpOption(
                Translator.getText("ContextMenu_RakeDung"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRakeDung"), rakedung
            );
        }

        if ((LuaHelpers.getJoypadState(player) != null || ISEmptyGraves_canDigHere(worldobjects)) && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }

            shovelMenu.addGetUpOption(
                Translator.getText("ContextMenu_DigGraves"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onDigGraves"), player, shovel
            );
        }

        if (graves != null) {
            if (test) {
                return true;
            }

            shovelMenu.addGetUpOption(
                Translator.getText("ContextMenu_FillGrave", LuaHelpers.castDouble(graves.getModData().rawget("corpses")).intValue()),
                graves,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFillGrave"),
                player,
                shovel
            );
        }

        return false;
    }

    private static boolean doTrapMenu(double player, KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper) {
        if (test) {
            return true;
        } else {
            KahluaTable doneSquare = LuaManager.platform.newTable();
            KahluaTableIterator iterator = worldobjects.iterator();

            while (iterator.advance()) {
                IsoObject value = (IsoObject)iterator.getValue();
                if (value != null && value.getSquare() != null && !LuaHelpers.castBoolean(doneSquare.rawget(value.getSquare()))) {
                    doneSquare.rawset(value.getSquare(), true);

                    for (int n = 0; n < value.getSquare().getObjects().size(); n++) {
                        if (value.getSquare().getObjects().get(n) instanceof IsoTrap) {
                            IsoTrap trap = (IsoTrap)value.getSquare().getObjects().get(n);
                            if (trap.getItem() != null && !trap.isExploding()) {
                                contextWrapper.addGetUpOption(
                                    Translator.getText("ContextMenu_TrapTake", trap.getItem().getName()),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeTrap"),
                                    trap,
                                    player
                                );
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    private static boolean doPadLockMenu(
        KahluaTable fetch, double player, KahluaTable worldobjects, boolean test, ItemContainer playerInv, ISContextMenuWrapper contextWrapper
    ) {
        Object padlockThump = fetch.rawget("padlockThump");
        if (padlockThump != null) {
            Key padlock = (Key)playerInv.FindAndReturn("Padlock");
            if (padlock != null && padlock.getNumberOfKey() > 0) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_PutPadlock"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPutPadlock"),
                    player,
                    padlockThump,
                    padlock
                );
            }

            InventoryItem digitalPadlock = playerInv.FindAndReturn("CombinationPadlock");
            if (digitalPadlock != null) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_PutCombinationPadlock"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPutDigitalPadlock"),
                    player,
                    padlockThump,
                    digitalPadlock
                );
            }
        }

        IsoObject padlockedThump = (IsoObject)fetch.rawget("padlockedThump");
        if (padlockedThump != null && playerInv.haveThisKeyId(padlockedThump.getKeyId()) != null) {
            if (test) {
                return true;
            }

            contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_RemovePadlock"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemovePadlock"),
                player,
                padlockedThump
            );
        }

        IsoObject digitalPadlockedThump = (IsoObject)fetch.rawget("digitalPadlockedThump");
        if (digitalPadlockedThump != null) {
            if (test) {
                return true;
            }

            contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_RemoveCombinationPadlock"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveDigitalPadlock"),
                player,
                digitalPadlockedThump
            );
        }

        return false;
    }

    private static boolean doWaterPipeMenu(
        double player, KahluaTable worldobjects, boolean test, IsoObject canBeWaterPiped, ISContextMenuWrapper contextWrapper, ItemContainer playerInv
    ) {
        if (test) {
            return true;
        } else {
            String name = getMoveableDisplayName(canBeWaterPiped);
            if (name == null) {
                name = "";
            }

            KahluaTable option = contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_PlumbItem", name),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPlumbItem"),
                player,
                canBeWaterPiped
            );
            option.rawset("iconTexture", Texture.getSharedTexture("Item_PipeWrench"));
            if (playerInv.getFirstRecurse(item -> compareType("PipeWrench", item) && !item.isBroken()) == null
                && playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.PIPE_WRENCH) && !item.isBroken()) == null) {
                option.rawset("notAvailable", true);
                ISToolTipWrapper tooltip = addToolTip();
                tooltip.setName(Translator.getText("ContextMenu_PlumbItem", name));
                tooltip.getTable().rawset("description", Translator.getText("Tooltip_NeedWrench", LuaManager.GlobalObject.getItemName("Base.PipeWrench")));
                option.rawset("toolTip", tooltip.getTable());
            }

            return false;
        }
    }

    private static boolean doFishingMenu(
        KahluaTable fetch,
        KahluaTable worldobjects,
        boolean test,
        ISContextMenuWrapper contextWrapper,
        IsoPlayer playerObj,
        IsoGridSquare clickedSquare,
        double player,
        ItemContainer playerInv
    ) {
        if (LuaHelpers.getJoypadState(player) != null) {
            float px = playerObj.getX();
            float py = playerObj.getY();
            float pz = playerObj.getZ();
            InventoryItem rod = getFishingRod(playerObj);
            Object lure = getFishingLure(playerObj, rod);
            InventoryItem net = playerInv.getFirstTypeRecurse("FishingNet");
            boolean haveTarget = false;
            if (rod != null && lure != null || net != null) {
                for (int dy = -5; dy <= 5; dy++) {
                    for (int dx = -5; dx <= 5; dx++) {
                        IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)(px + dx), (double)(py + dy), (double)pz);
                        if (square != null && square.has(IsoFlagType.water) && !square.getObjects().isEmpty()) {
                            if (rod != null && lure != null) {
                                fetch.rawset("canFish", true);
                                haveTarget = true;
                            }

                            if (net != null) {
                                fetch.rawset("canTrapFish", true);
                                haveTarget = true;
                            }
                            break;
                        }
                    }

                    if (haveTarget) {
                        break;
                    }
                }
            }

            haveTarget = false;

            for (int dy = -5; dy <= 5; dy++) {
                for (int dxx = -5; dxx <= 5; dxx++) {
                    IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)(px + dxx), (double)(py + dy), (double)pz);
                    if (square != null && square.has(IsoFlagType.water) && !square.getObjects().isEmpty()) {
                        for (int i = 0; i < square.getObjects().size(); i++) {
                            IsoObject v = square.getObjects().get(i);
                            if (v.getName() != null && v.getName().equals("FishingNet")) {
                                fetch.rawset("trapFish", v);
                                haveTarget = true;
                                break;
                            }
                        }

                        if (haveTarget) {
                            break;
                        }
                    }

                    if (haveTarget) {
                        break;
                    }
                }
            }
        }

        if (LuaHelpers.castBoolean(fetch.rawget("canTrapFish"))) {
            if (test) {
                return true;
            }

            doFishNetOptions(contextWrapper, playerObj, clickedSquare);
        }

        boolean isNoFishZone = LuaHelpers.castBoolean(LuaHelpers.callLuaClass("Fishing", "isNoFishZone", null, clickedSquare.getX(), clickedSquare.getY()));
        if (!isNoFishZone) {
            IsoObject trapFish = (IsoObject)fetch.rawget("trapFish");
            if (trapFish != null) {
                if (test) {
                    return true;
                }

                doPlacedFishNetOptions(contextWrapper, playerObj, trapFish);
            }

            if (LuaHelpers.castBoolean(fetch.rawget("canFish"))) {
                KahluaTable opt = contextWrapper.addOption(
                    Translator.getText("ContextMenu_Fishing"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.openFishWindow")
                );
                opt.rawset("iconTexture", Texture.trygetTexture("Item_FishingRod").splitIcon());
            }

            if (LuaHelpers.castBoolean(fetch.rawget("canAddChum"))) {
                doChumOptions(contextWrapper, playerObj, clickedSquare);
            }
        }

        return false;
    }

    private static boolean doBedMenu(double player, boolean test, ISContextMenuWrapper contextWrapper, IsoObject bed, IsoPlayer playerObj) {
        if (bed == null) {
            doSleepOption(contextWrapper, null, player, playerObj);
            return false;
        } else {
            KahluaTable bedOption = contextWrapper.getContextFromOption(bed.getTileName());
            ISContextMenuWrapper bedSubmenu;
            if (bedOption == null) {
                bedOption = contextWrapper.addOption(bed.getTileName(), null, null);
                Texture var9 = Texture.trygetTexture(bed.getSpriteName());
                if (var9 instanceof Texture) {
                    bedOption.rawset("iconTexture", var9.splitIcon());
                }

                bedSubmenu = ISContextMenuWrapper.getNew(contextWrapper);
                contextWrapper.addSubMenu(bedOption, bedSubmenu.getTable());
            } else {
                bedSubmenu = new ISContextMenuWrapper(bedOption);
            }

            if (!isSomethingTo(bed, playerObj)
                && !bed.isFurnitureOccupied(playerObj)
                && (!playerObj.isSittingOnFurniture() || !playerObj.isSitOnFurnitureObject(bed))) {
                if (test) {
                    return true;
                }

                if (bed.getSquare().getRoom() == playerObj.getSquare().getRoom() || bed.getSquare().isCanSee((int)player)) {
                    bedSubmenu.addGetUpOption(
                        Translator.getText("ContextMenu_Rest"), bed, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRest"), player
                    );
                }
            }

            if (LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "chairCheck", null, bed))) {
                if (bedSubmenu.getNumOptions() <= 1.0) {
                    contextWrapper.removeLastOption();
                }

                return false;
            } else {
                if (!isSomethingTo(bed, playerObj) && (!GameClient.client || ServerOptions.getInstance().sleepAllowed.getValue())) {
                    if (test) {
                        return true;
                    }

                    doSleepOption(bedSubmenu, bed, player, playerObj);
                }

                if (bedSubmenu.getNumOptions() <= 1.0) {
                    contextWrapper.removeLastOption();
                }

                return false;
            }
        }
    }

    private static IsoObject getBestWindowFrame(IsoPlayer playerObj) {
        IsoGridSquare playerSq = playerObj.getCurrentSquare();
        IsoDirections dir = playerObj.getCardinalDirection();
        IsoDirections[] dirs = new IsoDirections[]{dir, dir.Rot180(), dir.RotRight(2), dir.RotLeft(2)};

        for (IsoDirections dir2 : dirs) {
            IsoGridSquare square = playerSq.getAdjacentSquare(dir2);
            IsoThumpable thumpableWindow = playerSq.getWindowThumpableTo(square);
            if (thumpableWindow != null) {
                return thumpableWindow;
            }
        }

        return null;
    }

    private static boolean doThumpableWindowFrameMenu(
        KahluaTable fetch, KahluaTable worldobjects, boolean test, ISContextMenuWrapper contextWrapper, double player, boolean hasRemoveBarricadeTool
    ) {
        IsoPlayer playerObj = IsoPlayer.players[(int)player];
        ItemContainer playerInv = playerObj.getInventory();
        IsoThumpable thumpableWindow = (IsoThumpable)fetch.rawget("thumpableWindow");
        if (thumpableWindow == null) {
            return false;
        } else {
            IsoWindow movedWindow = thumpableWindow.getSquare().getWindow(thumpableWindow.getNorth());
            if (movedWindow != null) {
                return false;
            } else {
                String windowOptionTxt = Translator.getText("ContextMenu_Window");
                KahluaTable windowOption = contextWrapper.addOption(windowOptionTxt, null, null);
                ISContextMenuWrapper windowContextWrapper = ISContextMenuWrapper.getNew(contextWrapper);
                contextWrapper.addSubMenu(windowOption, windowContextWrapper.getTable());
                IsoCurtain addCurtains = thumpableWindow.HasCurtains();
                if (addCurtains == null && playerInv.containsTypeRecurse("Sheet")) {
                    if (test) {
                        return true;
                    }

                    windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Add_sheet"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"),
                        thumpableWindow,
                        player
                    );
                }

                if (thumpableWindow.canClimbThrough(playerObj)) {
                    if (test) {
                        return true;
                    }

                    KahluaTable climboption = windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Climb_through"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbThroughWindow"),
                        thumpableWindow,
                        player
                    );
                    if (LuaHelpers.getJoypadState(player) == null) {
                        ISToolTipWrapper tooltip = addToolTip();
                        tooltip.setName(Translator.getText("ContextMenu_Info"));
                        tooltip.getTable().rawset("description", Translator.getText("Tooltip_TapKey", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                        climboption.rawset("toolTip", tooltip.getTable());
                    }

                    if (playerObj.isDraggingCorpse()) {
                        IGrappleable thrownTarget = playerObj.getGrapplingTarget();
                        if (thrownTarget instanceof IsoGameCharacter) {
                            ClimbThroughWindowPositioningParams params = ClimbThroughWindowPositioningParams.alloc();
                            ClimbThroughWindowState.getClimbThroughWindowPositioningParams(playerObj, thumpableWindow, params);
                            boolean canClimb = params.canClimb;
                            params.release();
                            if (canClimb) {
                                String optionTxt = Translator.getText("ContextMenu_ThrowCorpseThroughWindow");
                                Object functionObj = LuaManager.getFunctionObject("ISWorldObjectContextMenu.onThrowCorpseThroughWindow");
                                windowContextWrapper.addOption(optionTxt, playerObj, functionObj, thumpableWindow);
                            }
                        }
                    }
                }

                if (thumpableWindow.isBarricadeAllowed()) {
                    doBarricadeMenu(thumpableWindow, playerObj, playerInv, windowContextWrapper);
                    IsoBarricade barricade = thumpableWindow.getBarricadeForCharacter(playerObj);
                    if (barricade != null && barricade.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                        if (test) {
                            return false;
                        }

                        windowContextWrapper.addGetUpOption(
                            Translator.getText("ContextMenu_Unbarricade"),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"),
                            thumpableWindow,
                            playerObj.getIndex()
                        );
                    }

                    if (!thumpableWindow.haveSheetRope()
                        && barricade == null
                        && checkBlowTorchForBarricade(playerObj)
                        && playerInv.containsTypeRecurse("SheetMetal")
                        && test) {
                        return false;
                    }

                    if (!thumpableWindow.haveSheetRope()
                        && barricade == null
                        && checkBlowTorchForBarricade(playerObj)
                        && playerInv.getItemCountRecurse("Base.MetalBar") >= 3
                        && test) {
                        return false;
                    }

                    if (barricade != null && barricade.isMetal() && checkBlowTorchForBarricade(playerObj)) {
                        if (test) {
                            return false;
                        }

                        windowContextWrapper.addGetUpOption(
                            Translator.getText("ContextMenu_Unbarricade"),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"),
                            thumpableWindow,
                            playerObj.getIndex()
                        );
                    }

                    if (barricade != null && barricade.isMetalBar() && checkBlowTorchForBarricade(playerObj)) {
                        if (test) {
                            return false;
                        }

                        windowContextWrapper.addGetUpOption(
                            Translator.getText("ContextMenu_Unbarricade"),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"),
                            thumpableWindow,
                            playerObj.getIndex()
                        );
                    }
                }

                return false;
            }
        }
    }

    private static boolean doThumpableMenu(
        IsoThumpable thump,
        double player,
        KahluaTable worldobjects,
        boolean test,
        boolean invincibleWindow,
        IsoPlayer playerObj,
        boolean hasRemoveBarricadeTool,
        ISContextMenuWrapper contextWrapper
    ) {
        if (thump.isBarricadeAllowed()) {
            IsoBarricade barricade = thump.getBarricadeForCharacter(playerObj);
            if (barricade != null && barricade.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"),
                    thump,
                    player
                );
            }

            if (barricade != null && barricade.isMetal() && checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"),
                    thump,
                    player
                );
            }

            if (barricade != null && barricade.isMetalBar() && checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"),
                    thump,
                    player
                );
            }
        }

        return false;
    }

    private static boolean doTreeMenu(
        KahluaTable fetch,
        KahluaTable context,
        KahluaTable worldobjects,
        boolean test,
        IsoPlayer playerObj,
        ItemContainer playerInv,
        ISContextMenuWrapper contextWrapper
    ) {
        LuaHelpers.callLuaClass("AnimalContextMenu", "attachAnimalToObject", null, fetch.rawget("attachAnimalTo"), playerObj, worldobjects, context);
        IsoTree tree = (IsoTree)fetch.rawget("tree");
        if (tree != null) {
            InventoryItem axe = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CHOP_TREE));
            if (axe != null) {
                if (test) {
                    return true;
                }

                KahluaTable option = contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Chop_Tree"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onChopTree"),
                    playerObj,
                    tree
                );
                option.rawset("iconTexture", axe.getTex());
            }
        }

        return false;
    }

    private static boolean doFuelMenu(KahluaTable fetch, KahluaTable context, double player, boolean test, ISContextMenuWrapper contextWrapper) {
        IsoObject fuelPump = (IsoObject)fetch.rawget("fuelPump");
        boolean fuelPower = SandboxOptions.getInstance().allowExteriorGenerator.getValue() && fuelPump != null && fuelPump.getSquare().haveElectricity()
            || fuelPump != null && fuelPump.getSquare().hasGridPower();
        IsoObject haveFuel = (IsoObject)fetch.rawget("haveFuel");
        if (haveFuel != null && fuelPower) {
            if (test) {
                return true;
            }

            LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "doFillFuelMenu", null, haveFuel, player, context);
        } else if (fuelPump != null && !fuelPower) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_FuelPumpNoPower"), null, null);
            option.rawset("notAvailable", true);
        } else if (fuelPump != null && fuelPump.getPipedFuelAmount() <= 0) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_FuelPumpEmpty"), null, null);
            option.rawset("notAvailable", true);
        } else if (fuelPump != null && fuelPump.getPipedFuelAmount() > 0) {
            KahluaTable option = contextWrapper.addOption(Translator.getText("ContextMenu_FuelPumpNoContainer"), null, null);
            option.rawset("notAvailable", true);
        }

        return false;
    }

    private static boolean doPlayerMenu(KahluaTable fetch, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ISContextMenuWrapper contextWrapper) {
        IsoPlayer clickedPlayer = (IsoPlayer)fetch.rawget("clickedPlayer");
        if (clickedPlayer != null && clickedPlayer != playerObj) {
            boolean clickedPlayerConditionsCheck = !clickedPlayer.isAnimal()
                && (!clickedPlayer.isInvisible() || playerObj.getRole().hasCapability(Capability.SeesInvisiblePlayers));
            if (!playerObj.hasTrait(CharacterTrait.HEMOPHOBIC) && clickedPlayerConditionsCheck) {
                if (test) {
                    return true;
                }

                KahluaTable option = contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Medical_Check"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onMedicalCheck"),
                    playerObj,
                    clickedPlayer
                );
                if (RequestMedicalCheckPacket.isCheckForbidden(playerObj, clickedPlayer)) {
                    ISToolTipWrapper tooltip = addToolTip();
                    option.rawset("notAvailable", true);
                    tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GetCloser", clickedPlayer.getDisplayName()));
                    option.rawset("toolTip", tooltip.getTable());
                }
            }

            if (clickedPlayer.isAsleep() && Core.debug) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Wake_Other", clickedPlayer.getDisplayName()),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWakeOther"),
                    playerObj,
                    clickedPlayer
                );
            }

            if (GameClient.client && clickedPlayerConditionsCheck && GameClient.canSeePlayerStats()) {
                if (test) {
                    return true;
                }

                contextWrapper.addGetUpOption(
                    "Check Stats", worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCheckStats"), playerObj, clickedPlayer
                );
            }

            if (!clickedPlayer.isAsleep() && !clickedPlayerConditionsCheck && GameClient.client) {
                KahluaTable ISTradingUI = (KahluaTable)LuaManager.env.rawget("ISTradingUI");
                KahluaTable ISTradingUI_instance = ISTradingUI != null ? (KahluaTable)ISTradingUI.rawget("instance") : null;
                if (ISTradingUI_instance == null || !LuaHelpers.castBoolean(LuaHelpers.callLuaClass("ISTradingUI", "isVisible", ISTradingUI_instance))) {
                    KahluaTable option = contextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Trade", clickedPlayer.getDisplayName()),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTrade"),
                        playerObj,
                        clickedPlayer
                    );
                    if (Math.abs(playerObj.getX() - clickedPlayer.getX()) > 2.0F || Math.abs(playerObj.getY() - clickedPlayer.getY()) > 2.0F) {
                        ISToolTipWrapper tooltip = addToolTip();
                        option.rawset("notAvailable", true);
                        tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GetCloserToTrade", clickedPlayer.getDisplayName()));
                        option.rawset("toolTip", tooltip.getTable());
                    }
                }
            }
        }

        return false;
    }

    private static boolean doCarBatteryMenu(
        KahluaTable fetch, KahluaTable context, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ItemContainer playerInv
    ) {
        IsoCarBatteryCharger carBatteryCharger = (IsoCarBatteryCharger)fetch.rawget("carBatteryCharger");
        return carBatteryCharger != null
            && LuaHelpers.castBoolean(
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "handleCarBatteryCharger", null, test, context, worldobjects, playerObj, playerInv)
            );
    }

    private static boolean doCleaningMenu(
        KahluaTable fetch, double player, KahluaTable worldobjects, boolean test, IsoPlayer playerObj, ISContextMenuWrapper contextWrapper
    ) {
        IsoGridSquare haveBlood = (IsoGridSquare)fetch.rawget("haveBlood");
        if (haveBlood != null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }

            KahluaTable option = contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_CleanStains"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCleanBlood"),
                haveBlood,
                player
            );
            ISToolTipWrapper tooltip = addToolTip();
            tooltip.getTable().rawset("description", Translator.getText("Tooltip_CleanStains"));
            option.rawset("toolTip", tooltip.getTable());
        }

        IsoGridSquare haveGraffiti = (IsoGridSquare)fetch.rawget("haveGraffiti");
        if (haveGraffiti != null && playerObj.getVehicle() == null) {
            if (test) {
                return true;
            }

            KahluaTable option = contextWrapper.addGetUpOption(
                Translator.getText("ContextMenu_CleanGraffiti"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCleanGraffiti"),
                haveGraffiti,
                player
            );
            ISToolTipWrapper tooltip = addToolTip();
            tooltip.getTable().rawset("description", Translator.getText("Tooltip_CleanGraffiti"));
            option.rawset("toolTip", tooltip.getTable());
        }

        return false;
    }

    private static boolean doGeneratorMenu(
        KahluaTable fetch,
        double player,
        KahluaTable worldobjects,
        boolean test,
        IsoPlayer playerObj,
        ISContextMenuWrapper contextWrapper,
        ItemContainer playerInv
    ) {
        IsoGenerator generator = (IsoGenerator)fetch.rawget("generator");
        if (generator != null && playerObj.getVehicle() == null) {
            boolean canDo = playerObj.getPerkLevel(PerkFactory.Perks.Electricity) >= 3 || playerObj.isRecipeActuallyKnown("Generator");
            if (test) {
                return true;
            }

            KahluaTable generatorOption = contextWrapper.addOption(Translator.getText("ContextMenu_Generator"), worldobjects, null);
            generatorOption.rawset("iconTexture", Texture.getSharedTexture("Item_Generator"));
            ISContextMenuWrapper generatorSubmenu = ISContextMenuWrapper.getNew(contextWrapper);
            contextWrapper.addSubMenu(generatorOption, generatorSubmenu.getTable());
            KahluaTable option = generatorSubmenu.addGetUpOption(
                Translator.getText("ContextMenu_GeneratorInfo"),
                worldobjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onInfoGenerator"),
                generator,
                player
            );
            if (playerObj.DistToSquared(generator.getX() + 0.5F, generator.getY() + 0.5F) < 4.0F) {
                ISToolTipWrapper tooltip = addToolTip();
                tooltip.setName(Translator.getText("IGUI_Generator_TypeGas"));
                tooltip.getTable().rawset("description", LuaHelpers.callLuaClass("ISGeneratorInfoWindow", "getRichText", null, generator, true));
                option.rawset("toolTip", tooltip.getTable());
            }

            if (generator.isConnected()) {
                if (generator.isActivated()) {
                    generatorSubmenu.addGetUpOption(
                        Translator.getText("ContextMenu_Turn_Off"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onActivateGenerator"),
                        false,
                        generator,
                        player
                    );
                } else {
                    option = generatorSubmenu.addGetUpOption(
                        Translator.getText("ContextMenu_GeneratorUnplug"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPlugGenerator"),
                        generator,
                        player,
                        false
                    );
                    if (generator.getFuel() > 0.0F) {
                        option = generatorSubmenu.addGetUpOption(
                            Translator.getText("ContextMenu_Turn_On"),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onActivateGenerator"),
                            true,
                            generator,
                            player
                        );
                        boolean doStats = playerObj.DistToSquared(generator.getX() + 0.5F, generator.getY() + 0.5F) < 4.0F;
                        String description = LuaHelpers.castString(LuaHelpers.callLuaClass("ISGeneratorInfoWindow", "getRichText", null, generator, doStats));
                        if (!description.isEmpty()) {
                            ISToolTipWrapper tooltip = addToolTip();
                            tooltip.setName(Translator.getText("IGUI_Generator_TypeGas"));
                            tooltip.getTable().rawset("description", description);
                            option.rawset("toolTip", tooltip.getTable());
                        }
                    }
                }
            } else {
                option = generatorSubmenu.addGetUpOption(
                    Translator.getText("ContextMenu_GeneratorPlug"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPlugGenerator"),
                    generator,
                    player,
                    true
                );
                if (!canDo) {
                    ISToolTipWrapper tooltip = addToolTip();
                    option.rawset("notAvailable", true);
                    tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GeneratorPlugTT"));
                    option.rawset("toolTip", tooltip.getTable());
                }
            }

            Predicate<InventoryItem> predicatePetrol = item -> {
                FluidContainer fc = item.getFluidContainer();
                return fc != null && fc.contains(Fluid.Petrol) && fc.getAmount() >= 0.099F;
            };
            if (!generator.isActivated() && generator.getFuelPercentage() < 100.0F && playerInv.getFirstRecurse(predicatePetrol) != null) {
                InventoryItem petrolCan = playerInv.getFirstRecurse(predicatePetrol);
                LuaHelpers.callLuaClass(
                    "ISWorldObjectContextMenu",
                    "onAddFuelGenerator",
                    null,
                    worldobjects,
                    petrolCan,
                    generator,
                    player,
                    contextWrapper.getContextFromOption(Translator.getText("ContextMenu_Generator"))
                );
            }

            if (!generator.isActivated() && generator.getCondition() < 100.0F) {
                option = generatorSubmenu.addGetUpOption(
                    Translator.getText("ContextMenu_GeneratorFix"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFixGenerator"),
                    generator,
                    player
                );
                if (!canDo) {
                    ISToolTipWrapper tooltip = addToolTip();
                    option.rawset("notAvailable", true);
                    tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GeneratorPlugTT"));
                    option.rawset("toolTip", tooltip.getTable());
                }

                if (!playerInv.containsTypeRecurse("ElectronicsScrap")) {
                    ISToolTipWrapper tooltip = addToolTip();
                    option.rawset("notAvailable", true);
                    tooltip.getTable().rawset("description", Translator.getText("ContextMenu_GeneratorFixTT"));
                    option.rawset("toolTip", tooltip.getTable());
                }
            }

            if (!generator.isConnected()) {
                generatorSubmenu.addGetUpOption(
                    Translator.getText("ContextMenu_GeneratorTake"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeGenerator"),
                    generator,
                    player
                );
            }
        }

        return false;
    }

    private static ISContextMenuWrapper doDoorMenu(
        KahluaTable fetch,
        KahluaTable context,
        KahluaTable worldobjects,
        boolean test,
        ISContextMenuWrapper contextWrapper,
        IsoPlayer playerObj,
        boolean hasRemoveBarricadeTool
    ) {
        ItemContainer playerInv = playerObj.getInventory();
        String doorOptionTxt = Translator.getText("ContextMenu_Door_option");
        KahluaTable doorOption = contextWrapper.addOption(doorOptionTxt, null, null);
        ISContextMenuWrapper doorContextWrapper = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(doorOption, doorContextWrapper.getTable());
        KahluaTable doorContext = contextWrapper.getContextFromOption(doorOptionTxt);
        ILockableDoor door = (ILockableDoor)fetch.rawget("door");
        Object doorKeyId = fetch.rawget("doorKeyId");
        BarricadeAble barricadeAbleDoor = (BarricadeAble)fetch.rawget("door");
        if (door != null) {
            ICurtain doorCurtain = door.HasCurtains();
            if (doorCurtain != null) {
                if (test) {
                    return null;
                }

                String text = Translator.getText(doorCurtain.isCurtainOpen() ? "ContextMenu_Close_curtains" : "ContextMenu_Open_curtains");
                doorContextWrapper.addGetUpOption(
                    text, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseCurtain"), door, playerObj.getIndex()
                );
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "addRemoveCurtainOption", null, doorContext, worldobjects, door, playerObj.getIndex());
            } else if (door.canAddCurtain() && playerInv.containsTypeRecurse("Sheet")) {
                if (test) {
                    return null;
                }

                KahluaTable addSheetOption = doorContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Add_sheet"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"),
                    door,
                    playerObj.getIndex()
                );
                addSheetOption.rawset("iconTexture", Texture.getSharedTexture("Item_Sheet"));
            }

            if (test) {
                return null;
            }

            String text = Translator.getText("ContextMenu_Open_door");
            if (door.IsOpen()) {
                text = Translator.getText("ContextMenu_Close_door");
            }

            KahluaTable opendooroption = doorContextWrapper.addGetUpOption(
                text, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseDoor"), door, playerObj.getIndex()
            );
            if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                ISToolTipWrapper tooltip = addToolTip();
                tooltip.setName(Translator.getText("ContextMenu_Info"));
                tooltip.getTable().rawset("description", Translator.getText("Tooltip_OpenClose", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                opendooroption.rawset("toolTip", tooltip.getTable());
            }

            if (!door.couldBeOpen(playerObj)) {
                ISToolTipWrapper tooltip = addToolTip();
                tooltip.setName(Translator.getText("Tooltip_DoorIsLocked"));
                opendooroption.rawset("toolTip", tooltip.getTable());
                opendooroption.rawset("notAvailable", true);
            }

            IsoObject doorIsoObj = (IsoObject)door;
            if (doorIsoObj.isHoppable() && door.canClimbOver(playerObj)) {
                KahluaTable option = doorContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Climb_over"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbOverFence"),
                    door,
                    null,
                    playerObj.getIndex()
                );
                if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                    ISToolTipWrapper tooltip = addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", Translator.getText("Tooltip_Climb", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    option.rawset("toolTip", tooltip.getTable());
                }
            }

            if (!door.IsOpen() && doorKeyId != null && playerObj.getInventory().haveThisKeyId((Integer)doorKeyId) != null) {
                if (test) {
                    return null;
                }

                KahluaTable option;
                if (!door.isLockedByKey()) {
                    option = doorContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_LockDoor"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onLockDoor"),
                        playerObj.getIndex(),
                        door
                    );
                } else {
                    option = doorContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_UnlockDoor"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnLockDoor"),
                        playerObj.getIndex(),
                        door,
                        doorKeyId
                    );
                }

                if (!door.couldBeOpen(playerObj)) {
                    ISToolTipWrapper tooltip = addToolTip();
                    tooltip.setName(Translator.getText("Tooltip_DoorIsLocked"));
                    option.rawset("toolTip", tooltip.getTable());
                    option.rawset("notAvailable", true);
                }
            }

            Texture var28 = Texture.trygetTexture(doorIsoObj.getSpriteName());
            if (var28 instanceof Texture) {
                doorOption.rawset("iconTexture", var28.splitIcon());
            }

            doBarricadeMenu(barricadeAbleDoor, playerObj, playerInv, doorContextWrapper);
            IsoBarricade barricade = barricadeAbleDoor.getBarricadeForCharacter(playerObj);
            if (barricade != null && barricade.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                if (test) {
                    return null;
                }

                doorContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"),
                    door,
                    playerObj.getIndex()
                );
            }

            if (barricade != null && barricade.isMetal() && checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }

                doorContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"),
                    door,
                    playerObj.getIndex()
                );
            }

            if (barricade != null && barricade.isMetalBar() && checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }

                doorContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"),
                    door,
                    playerObj.getIndex()
                );
            }
        }

        return doorContextWrapper;
    }

    private static boolean doHoppableMenu(boolean test, ISContextMenuWrapper contextWrapper, IsoObject hoppable, IsoPlayer playerObj) {
        IsoGridSquare fromSquare;
        IsoDirections dir;
        if (hoppable.hasProperty(IsoFlagType.HoppableN)) {
            if (hoppable.getY() <= playerObj.getY()) {
                fromSquare = hoppable.getSquare();
                dir = IsoDirections.N;
            } else {
                fromSquare = hoppable.getSquare().getAdjacentSquare(IsoDirections.N);
                dir = IsoDirections.S;
            }
        } else if (hoppable.getX() <= playerObj.getX()) {
            fromSquare = hoppable.getSquare();
            dir = IsoDirections.W;
        } else {
            fromSquare = hoppable.getSquare().getAdjacentSquare(IsoDirections.W);
            dir = IsoDirections.E;
        }

        if (playerObj.isDraggingCorpse() && playerObj.canThrowCorpseOver(fromSquare, dir)) {
            if (test) {
                return true;
            }

            String optionTxt = Translator.getText("ContextMenu_ThrowCorpseOverFence");
            Object functionObj = LuaManager.getFunctionObject("ISWorldObjectContextMenu.onThrowCorpseOverFence");
            KahluaTable var8 = contextWrapper.addOption(optionTxt, playerObj, functionObj, hoppable, dir);
        }

        return false;
    }

    private static ISContextMenuWrapper doWindowMenu(
        KahluaTable fetch,
        KahluaTable worldObjects,
        boolean test,
        ISContextMenuWrapper contextWrapper,
        IsoWindow window,
        boolean invincibleWindow,
        IsoPlayer playerObj,
        boolean hasRemoveBarricadeTool,
        boolean hasHammer
    ) {
        ItemContainer playerInv = playerObj.getInventory();
        IsoWindowFrame windowFrame = (IsoWindowFrame)fetch.rawget("windowFrame");
        IsoThumpable thumpableWindow = (IsoThumpable)fetch.rawget("thumpableWindow");
        String windowOptionTxt = window == null && windowFrame == null && thumpableWindow == null
            ? Translator.getText("ContextMenu_escape_rope")
            : Translator.getText("ContextMenu_Window");
        KahluaTable windowOption = contextWrapper.addOption(windowOptionTxt, null, null);
        ISContextMenuWrapper windowContextWrapper = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(windowOption, windowContextWrapper.getTable());
        IsoCurtain curtain = (IsoCurtain)fetch.rawget("curtain");
        KahluaTable windowContext = contextWrapper.getContextFromOption(windowOptionTxt);
        if (window != null && !invincibleWindow) {
            IsoBarricade barricade = window.getBarricadeForCharacter(playerObj);
            Texture climboption = Texture.trygetTexture(window.getSpriteName());
            if (climboption instanceof Texture) {
                windowOption.rawset("iconTexture", climboption.splitIcon());
            } else {
                windowOption.rawset("iconTexture", Texture.trygetTexture("Build_WindowBlack"));
            }

            IsoCurtain curtain2 = window.HasCurtains();
            curtain = curtain != null ? curtain : curtain2;
            if (window.isSmashed() && !window.isGlassRemoved() && barricade == null) {
                if (test) {
                    return null;
                }

                KahluaTable option = windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_RemoveBrokenGlass"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveBrokenGlass"),
                    window,
                    playerObj.getIndex()
                );
                if (playerObj.getPrimaryHandItem() == null) {
                    option.rawset("notAvailable", true);
                    ISToolTipWrapper tooltip = addToolTip();
                    tooltip.getTable().rawset("description", Translator.getText("Tooltip_RemoveBrokenGlassNoItem"));
                    option.rawset("toolTip", tooltip.getTable());
                }
            }

            if (curtain2 == null && playerInv.containsTypeRecurse("Sheet")) {
                if (test) {
                    return null;
                }

                KahluaTable addSheetOption = windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Add_sheet"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"),
                    window,
                    playerObj.getIndex()
                );
                addSheetOption.rawset("iconTexture", Texture.getSharedTexture("Item_Sheet"));
            }

            if (window.canClimbThrough(playerObj)) {
                if (test) {
                    return null;
                }

                KahluaTable climboptionx = windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Climb_through"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbThroughWindow"),
                    window,
                    playerObj.getIndex()
                );
                if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                    ISToolTipWrapper tooltip = addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    if (window.isGlassRemoved()) {
                        tooltip.getTable().rawset("description", Translator.getText("Tooltip_TapKey", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    } else {
                        tooltip.getTable().rawset("description", Translator.getText("Tooltip_Climb", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    }

                    climboptionx.rawset("toolTip", tooltip.getTable());
                }

                if (playerObj.isDraggingCorpse()) {
                    IGrappleable thrownTarget = playerObj.getGrapplingTarget();
                    if (thrownTarget instanceof IsoGameCharacter) {
                        ClimbThroughWindowPositioningParams params = ClimbThroughWindowPositioningParams.alloc();
                        ClimbThroughWindowState.getClimbThroughWindowPositioningParams(playerObj, window, params);
                        boolean canClimb = params.canClimb;
                        params.release();
                        if (canClimb) {
                            String optionTxt = Translator.getText("ContextMenu_ThrowCorpseThroughWindow");
                            Object functionObj = LuaManager.getFunctionObject("ISWorldObjectContextMenu.onThrowCorpseThroughWindow");
                            windowContextWrapper.addOption(optionTxt, playerObj, functionObj, window);
                        }
                    }
                }
            }

            if (window.IsOpen() && !window.isSmashed() && barricade == null) {
                if (test) {
                    return null;
                }

                KahluaTable opencloseoption = windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Close_window"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseWindow"),
                    window,
                    playerObj.getIndex()
                );
                if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                    ISToolTipWrapper tooltip = addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", Translator.getText("Tooltip_OpenClose", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    opencloseoption.rawset("toolTip", tooltip.getTable());
                }
            }

            if (!window.IsOpen() && !window.isSmashed() && barricade == null) {
                if (test) {
                    return null;
                }

                if (window.getSprite() == null || !window.getSprite().getProperties().has("WindowLocked")) {
                    KahluaTable opencloseoption = windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Open_window"),
                        worldObjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseWindow"),
                        window,
                        playerObj.getIndex()
                    );
                    if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                        ISToolTipWrapper tooltip = addToolTip();
                        tooltip.setName(Translator.getText("ContextMenu_Info"));
                        tooltip.getTable()
                            .rawset("description", Translator.getText("Tooltip_OpenClose", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                        opencloseoption.rawset("toolTip", tooltip.getTable());
                    }
                }

                windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Smash_window"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSmashWindow"),
                    window,
                    playerObj.getIndex()
                );
            }
        }

        Object[] sheetRopeCandidates = new Object[]{
            fetch.rawget("window"),
            fetch.rawget("hoppableN"),
            fetch.rawget("hoppableN_2"),
            fetch.rawget("hoppableW"),
            fetch.rawget("hoppableW_2"),
            fetch.rawget("thumpableWindowN"),
            fetch.rawget("thumpableWindowN_2"),
            fetch.rawget("thumpableWindowW"),
            fetch.rawget("thumpableWindowW_2")
        };

        for (Object object : sheetRopeCandidates) {
            if (object != null && !(object instanceof IsoObject isoObject && isoObject.getSprite().getProperties().has(IsoFlagType.makeWindowInvincible))) {
                LuaHelpers.callLuaClass(
                    "ISWorldObjectContextMenu",
                    "doSheetRopeOptions",
                    null,
                    windowContext,
                    object,
                    worldObjects,
                    playerObj.getIndex(),
                    playerObj,
                    playerInv,
                    hasHammer,
                    test
                );
            }
        }

        if (curtain == null && windowFrame != null) {
            curtain = windowFrame.HasCurtains();
        }

        if (curtain == null && thumpableWindow != null) {
            curtain = thumpableWindow.HasCurtains();
        }

        if (curtain != null && !invincibleWindow) {
            String text = Translator.getText("ContextMenu_Open_curtains");
            if (curtain.IsOpen()) {
                text = Translator.getText("ContextMenu_Close_curtains");
            }

            if (test) {
                return null;
            }

            Texture curtainTexture = Texture.trygetTexture(curtain.getSpriteName());
            if (curtainTexture == null) {
                curtainTexture = Texture.trygetTexture("Item_Sheet");
            }

            if (window == null) {
                windowOption.rawset("name", Translator.getText("ContextMenu_Curtain"));
                windowOption.rawset("iconTexture", curtainTexture.splitIcon());
            }

            if (!curtain.getSquare().getProperties().has(IsoFlagType.exterior)) {
                if (!playerObj.getCurrentSquare().has(IsoFlagType.exterior)) {
                    KahluaTable option = windowContextWrapper.addGetUpOption(
                        text, worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseCurtain"), curtain, playerObj.getIndex()
                    );
                    option.rawset("iconTexture", curtainTexture.splitIcon());
                    if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                        ISToolTipWrapper tooltip = addToolTip();
                        tooltip.setName(Translator.getText("ContextMenu_Info"));
                        tooltip.getTable()
                            .rawset("description", Translator.getText("Tooltip_OpenCloseCurtains", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                        option.rawset("toolTip", tooltip.getTable());
                    }

                    LuaHelpers.callLuaClass(
                        "ISWorldObjectContextMenu", "addRemoveCurtainOption", null, windowContext, worldObjects, curtain, playerObj.getIndex()
                    );
                }
            } else {
                windowContextWrapper.addGetUpOption(
                    text, worldObjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onOpenCloseCurtain"), curtain, playerObj.getIndex()
                );
                LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "addRemoveCurtainOption", null, windowContext, worldObjects, curtain, playerObj.getIndex());
            }
        }

        if (window != null && !invincibleWindow) {
            IsoBarricade barricadex = window.getBarricadeForCharacter(playerObj);
            doBarricadeMenu(window, playerObj, playerInv, windowContextWrapper);
            if (barricadex != null && barricadex.getNumPlanks() > 0 & hasRemoveBarricadeTool) {
                if (test) {
                    return null;
                }

                windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"),
                    window,
                    playerObj.getIndex()
                );
            }

            if (barricadex != null && barricadex.isMetal() && checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }

                windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"),
                    window,
                    playerObj.getIndex()
                );
            }

            if (barricadex != null && barricadex.isMetalBar() && checkBlowTorchForBarricade(playerObj)) {
                if (test) {
                    return null;
                }

                windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Unbarricade"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"),
                    window,
                    playerObj.getIndex()
                );
            }
        }

        if (windowFrame != null && window == null && thumpableWindow == null) {
            if (windowFrame.getCurtain() == null && playerInv.containsTypeRecurse("Sheet")) {
                if (test) {
                    return null;
                }

                windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Add_sheet"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheet"),
                    windowFrame,
                    playerObj.getIndex()
                );
            }

            int numSheetRope = windowFrame.countAddSheetRope();
            if (windowFrame.canAddSheetRope()
                && !windowFrame.isBarricaded()
                && playerObj.getCurrentSquare().getZ() > 0
                && playerInv.containsTypeRecurse("Nails")) {
                if (playerInv.getItemCountRecurse("SheetRope") >= numSheetRope) {
                    if (test) {
                        return null;
                    }

                    windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Add_escape_rope_sheet"),
                        worldObjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheetRope"),
                        windowFrame,
                        playerObj.getIndex(),
                        true
                    );
                } else if (playerInv.getItemCountRecurse("Rope") >= numSheetRope) {
                    if (test) {
                        return null;
                    }

                    windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Add_escape_rope"),
                        worldObjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddSheetRope"),
                        windowFrame,
                        playerObj.getIndex(),
                        false
                    );
                }
            }

            if (windowFrame.haveSheetRope()) {
                if (test) {
                    return null;
                }

                windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Remove_escape_rope"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveSheetRope"),
                    windowFrame,
                    playerObj.getIndex()
                );
            }

            if (windowFrame.isBarricadeAllowed()) {
                doBarricadeMenu(windowFrame, playerObj, playerInv, windowContextWrapper);
                IsoBarricade barricadexx = windowFrame.getBarricadeForCharacter(playerObj);
                if (barricadexx != null && barricadexx.getNumPlanks() > 0 && hasRemoveBarricadeTool) {
                    if (test) {
                        return null;
                    }

                    windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Unbarricade"),
                        worldObjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricade"),
                        windowFrame,
                        playerObj.getIndex()
                    );
                }

                if (!windowFrame.haveSheetRope()
                    && barricadexx == null
                    && checkBlowTorchForBarricade(playerObj)
                    && playerInv.containsTypeRecurse("SheetMetal")
                    && test) {
                    return null;
                }

                if (!windowFrame.haveSheetRope()
                    && barricadexx == null
                    && checkBlowTorchForBarricade(playerObj)
                    && playerInv.getItemCountRecurse("Base.MetalBar") >= 3
                    && test) {
                    return null;
                }

                if (barricadexx != null && barricadexx.isMetal() && checkBlowTorchForBarricade(playerObj)) {
                    if (test) {
                        return null;
                    }

                    windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Unbarricade"),
                        worldObjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetal"),
                        windowFrame,
                        playerObj.getIndex()
                    );
                }

                if (barricadexx != null && barricadexx.isMetalBar() && checkBlowTorchForBarricade(playerObj)) {
                    if (test) {
                        return null;
                    }

                    windowContextWrapper.addGetUpOption(
                        Translator.getText("ContextMenu_Unbarricade"),
                        worldObjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onUnbarricadeMetalBar"),
                        windowFrame,
                        playerObj.getIndex()
                    );
                }
            }

            if (test) {
                return null;
            }

            if (windowFrame.canClimbThrough(playerObj) && !windowFrame.hasWindow()) {
                KahluaTable climboptionxx = windowContextWrapper.addGetUpOption(
                    Translator.getText("ContextMenu_Climb_through"),
                    worldObjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onClimbThroughWindow"),
                    windowFrame,
                    playerObj.getIndex()
                );
                if (LuaHelpers.getJoypadState(playerObj.getIndex()) == null) {
                    ISToolTipWrapper tooltip = addToolTip();
                    tooltip.setName(Translator.getText("ContextMenu_Info"));
                    tooltip.getTable().rawset("description", Translator.getText("Tooltip_TapKey", Input.getKeyName(Core.getInstance().getKey("Interact"))));
                    climboptionxx.rawset("toolTip", tooltip.getTable());
                }

                if (playerObj.isDraggingCorpse()) {
                    IGrappleable thrownTarget = playerObj.getGrapplingTarget();
                    if (thrownTarget instanceof IsoGameCharacter) {
                        ClimbThroughWindowPositioningParams params = ClimbThroughWindowPositioningParams.alloc();
                        ClimbThroughWindowState.getClimbThroughWindowPositioningParams(playerObj, windowFrame, params);
                        boolean canClimb = params.canClimb;
                        params.release();
                        if (canClimb) {
                            String optionTxt = Translator.getText("ContextMenu_ThrowCorpseThroughWindow");
                            Object functionObj = LuaManager.getFunctionObject("ISWorldObjectContextMenu.onThrowCorpseThroughWindow");
                            windowContextWrapper.addOption(optionTxt, playerObj, functionObj, windowFrame);
                        }
                    }
                }
            }
        }

        return windowContextWrapper;
    }

    private static void doBarricadeMenu(BarricadeAble barricadeAble, IsoPlayer playerObj, ItemContainer playerInv, ISContextMenuWrapper contextWrapper) {
        boolean canBarricade = barricadeAble.isBarricadeAllowed();
        boolean hasHammer = playerInv.getFirstRecurse(item -> item.hasTag(ItemTag.HAMMER) && !item.isBroken()) != null;
        IsoBarricade barricade = barricadeAble.getBarricadeForCharacter(playerObj);
        int weldingRods = playerInv.getItemCountRecurse(ItemKey.Drainable.WELDING_RODS);
        int metalWelding = playerObj.getPerkLevel(PerkFactory.Perks.MetalWelding);
        if (playerObj.isBuildCheat()
            || barricadeAble.isBarricadeAllowed()
                && (barricade == null || barricade.canAddPlank())
                && hasHammer
                && playerInv.containsTypeRecurse(ItemKey.Weapon.PLANK)
                && playerInv.getItemCountRecurse(ItemKey.Normal.NAILS) >= 2) {
            doBarricadeOption(playerObj, contextWrapper, "carpentry_01_8", "Item_Plank", "ContextMenu_Barricade");
        }

        if (playerObj.isBuildCheat()
            || canBarricade
                && barricade == null
                && checkBlowTorchForBarricade(playerObj)
                && playerInv.containsTypeRecurse(ItemKey.Normal.SHEET_METAL)
                && weldingRods >= 1
                && metalWelding >= 3) {
            doBarricadeOption(playerObj, contextWrapper, "constructedobjects_01_24", "Item_SheetMetal", "ContextMenu_MetalBarricade");
        }

        if (playerObj.isBuildCheat()
            || canBarricade
                && barricade == null
                && checkBlowTorchForBarricade(playerObj)
                && (playerInv.getItemCountRecurse(ItemKey.Weapon.METAL_BAR) >= 3 || playerInv.getItemCountRecurse(ItemKey.Weapon.IRON_BAR) >= 3)
                && weldingRods >= 3
                && metalWelding >= 3) {
            doBarricadeOption(playerObj, contextWrapper, "constructedobjects_01_55", "Item_SteelRod_Full", "ContextMenu_MetalBarBarricade");
        }
    }

    private static void doBarricadeOption(IsoPlayer playerObj, ISContextMenuWrapper contextWrapper, String tile, String itemIcon, String optionName) {
        KahluaTable option = contextWrapper.addGetUpOption(
            Translator.getText(optionName), tile, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onBarricade"), playerObj
        );
        option.rawset("iconTexture", Texture.trygetTexture(itemIcon));
        if (playerObj.tooDarkToRead()) {
            option.rawset("notAvailable", true);
            ISToolTipWrapper toolTip = addToolTip();
            option.rawset("toolTip", toolTip.getTable());
            toolTip.getTable().rawset("description", Translator.getText("IGUI_CraftingWindow_RequiresLight"));
        }
    }

    private static void doGardeningSubmenu(ISContextMenuWrapper contextWrapper, KahluaTable worldObjects, IsoPlayer playerObj, KahluaTable fetch, boolean test) {
        KahluaTable gardeningOption = contextWrapper.addOption(Translator.getText("ContextMenu_Gardening"), worldObjects, null);
        ISContextMenuWrapper gardeningSubMenu = ISContextMenuWrapper.getNew(contextWrapper);
        contextWrapper.addSubMenu(gardeningOption, gardeningSubMenu.getTable());
        ItemContainer playerInv = playerObj.getInventory();
        int playerNum = playerObj.getIndex();
        InventoryItem scythe = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.SCYTHE));
        if (scythe != null && playerObj.getVehicle() == null) {
            KahluaTable option = gardeningSubMenu.addGetUpOption(
                Translator.getText("ContextMenu_ScytheGrass"), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onScytheGrass"), scythe
            );
            option.rawset("iconTexture", scythe.getTex());
        }

        IsoGridSquare canBeRemoved = (IsoGridSquare)fetch.rawget("canBeRemoved");
        if (canBeRemoved != null && playerObj.getVehicle() == null) {
            gardeningSubMenu.addGetUpOption(
                Translator.getText("ContextMenu_RemoveGrass"),
                worldObjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveGrass"),
                canBeRemoved,
                playerNum
            );
        }

        IsoCompost compost = (IsoCompost)fetch.rawget("compost");
        if (compost != null) {
            LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "handleCompost", null, test, gardeningSubMenu.getTable(), worldObjects, playerObj, playerInv);
        }

        IsoGridSquare canBeCut = (IsoGridSquare)fetch.rawget("canBeCut");
        if (canBeCut != null && playerObj.getVehicle() == null) {
            gardeningSubMenu.addGetUpOption(
                Translator.getText("ContextMenu_RemoveBush"),
                worldObjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemovePlant"),
                canBeCut,
                false,
                playerNum
            );
        }

        IsoGridSquare wallVine = (IsoGridSquare)fetch.rawget("wallVine");
        if (wallVine != null && playerObj.getVehicle() == null) {
            gardeningSubMenu.addGetUpOption(
                Translator.getText("ContextMenu_RemoveWallVine"),
                worldObjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemovePlant"),
                wallVine,
                true,
                playerNum
            );
        }

        IsoObject stump = (IsoObject)fetch.rawget("stump");
        if (stump != null && playerObj.getVehicle() == null) {
            KahluaTable option = gardeningSubMenu.addGetUpOption(
                Translator.getText("ContextMenu_Remove_Stump"),
                worldObjects,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveGroundCoverItemPickAxe"),
                playerNum,
                stump
            );
            option.rawset("iconTexture", Texture.trygetTexture(stump.getSpriteName()).splitIcon());
        }

        IsoObject pickupItem = (IsoObject)fetch.rawget("pickupItem");
        if (pickupItem != null && playerObj.getVehicle() == null) {
            prePickupGroundCoverItem(gardeningSubMenu, worldObjects, playerNum, pickupItem);
        }

        LuaHelpers.callLuaClass("ISFarmingMenu", "doDigMenu", null, playerObj, gardeningSubMenu.getTable(), worldObjects, test);
        if (gardeningSubMenu.getNumOptions() <= 1.0) {
            contextWrapper.removeLastOption();
        }
    }

    private static boolean isGraveFilledIn(IsoObject grave) {
        if (grave == null || grave.getName() == null || !grave.getName().equals("EmptyGraves")) {
            return false;
        } else if (LuaHelpers.castBoolean(grave.getModData().rawget("filled"))) {
            return true;
        } else {
            IsoSprite sprite = grave.getSprite();
            return sprite != null && sprite.getName() != null
                ? sprite.getName().equals("location_community_cemetary_01_40")
                    || sprite.getName().equals("location_community_cemetary_01_41")
                    || sprite.getName().equals("location_community_cemetary_01_42")
                    || sprite.getName().equals("location_community_cemetary_01_43")
                : false;
        }
    }

    private static InventoryItem getFishingRod(IsoPlayer playerObj) {
        InventoryItem handItem = playerObj.getPrimaryHandItem();
        return handItem != null && predicateFishingRodOrSpear(handItem, playerObj)
            ? handItem
            : playerObj.getInventory().getFirstRecurse(item -> predicateFishingRodOrSpear(item, playerObj));
    }

    private static boolean predicateFishingRodOrSpear(InventoryItem item, IsoPlayer playerObj) {
        if (item.isBroken()) {
            return false;
        } else {
            return !item.hasTag(ItemTag.FISHING_ROD) && !item.hasTag(ItemTag.FISHING_SPEAR) ? false : getFishingLure(playerObj, item) != null;
        }
    }

    private static Object getFishingLure(IsoPlayer player, InventoryItem rod) {
        if (rod == null) {
            return null;
        } else if (rod instanceof HandWeapon handWeapon && WeaponType.getWeaponType(handWeapon) == WeaponType.SPEAR) {
            return true;
        } else {
            return player.getSecondaryHandItem() != null && player.getSecondaryHandItem().isFishingLure()
                ? player.getSecondaryHandItem()
                : player.getInventory().getFirstRecurse(InventoryItem::isFishingLure);
        }
    }

    private static KahluaTable farmingSystem_getLuaObjectOnSquare(IsoGridSquare square) {
        if (square == null) {
            return null;
        } else {
            CGlobalObjectSystem farmingSystem = getFarmingSystem();
            if (farmingSystem != null) {
                GlobalObject globalObject = farmingSystem.getObjectAt(square.getX(), square.getY(), square.getZ());
                if (globalObject != null) {
                    KahluaTable luaObject = globalObject.getModData();
                    LuaManager.call("CGlobalObject:updateFromIsoObject", luaObject);
                    return luaObject;
                }
            }

            return null;
        }
    }

    private static CGlobalObjectSystem getFarmingSystem() {
        for (int i = 0; i < CGlobalObjects.getSystemCount(); i++) {
            CGlobalObjectSystem system = CGlobalObjects.getSystemByIndex(i);
            if (system.getName() != null && system.getName().equals("CFarmingSystem")) {
                return system;
            }
        }

        return null;
    }

    private static KahluaTable getDirtGravelSand(IsoGridSquare square) {
        int index = square.getNextNonItemObjectIndex(0);

        while (index >= 0 && index < square.getObjects().size()) {
            IsoObject obj = square.getObjects().get(index);
            index = square.getNextNonItemObjectIndex(index + 1);
            if ((!obj.hasModData() || !LuaHelpers.castBoolean(obj.getModData().rawget("shovelled")))
                && (GameServer.server || farmingSystem_getLuaObjectOnSquare(square) == null)
                && obj.getSprite() != null
                && obj.getSprite().getName() != null) {
                String spriteName = obj.getSprite().getName();
                if (!spriteName.equals("floors_exterior_natural_01_13")
                    && !spriteName.equals("blends_street_01_55")
                    && !spriteName.equals("blends_street_01_54")
                    && !spriteName.equals("blends_street_01_53")
                    && !spriteName.equals("blends_street_01_48")) {
                    if (!spriteName.equals("blends_natural_01_0")
                        && !spriteName.equals("blends_natural_01_5")
                        && !spriteName.equals("blends_natural_01_6")
                        && !spriteName.equals("blends_natural_01_7")
                        && !spriteName.equals("floors_exterior_natural_01_24")) {
                        if (!spriteName.equals("blends_natural_01_96")
                            && !spriteName.equals("blends_natural_01_103")
                            && !spriteName.equals("blends_natural_01_101")
                            && !spriteName.equals("blends_natural_01_102")) {
                            if (!spriteName.startsWith("blends_natural_01_") && !spriteName.startsWith("floors_exterior_natural")) {
                                continue;
                            }

                            KahluaTable table = LuaManager.platform.newTable();
                            table.rawset("dirt", obj);
                            return table;
                        }

                        KahluaTable table = LuaManager.platform.newTable();
                        table.rawset("clay", obj);
                        return table;
                    }

                    KahluaTable table = LuaManager.platform.newTable();
                    table.rawset("sand", obj);
                    return table;
                }

                KahluaTable table = LuaManager.platform.newTable();
                table.rawset("gravel", obj);
                return table;
            }
        }

        return null;
    }

    private static boolean canCleanBlood(IsoPlayer playerObj, IsoGridSquare square) {
        ItemContainer playerInv = playerObj.getInventory();
        InventoryItem bleach = playerInv.getFirstRecurse(ISWorldObjectContextMenuLogic::predicateCleaningLiquid);
        InventoryItem mop = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CLEAN_STAINS));
        return square != null && square.haveStains() && bleach != null && mop != null;
    }

    private static boolean predicateCleaningLiquid(InventoryItem item) {
        return item == null
            ? false
            : item.hasComponent(ComponentType.FluidContainer)
                && (item.getFluidContainer().contains(Fluid.Bleach) || item.getFluidContainer().contains(Fluid.CleaningLiquid))
                && item.getFluidContainer().getAmount() >= ZomboidGlobals.cleanBloodBleachAmount;
    }

    private static boolean canCleanGraffiti(IsoPlayer playerObj, IsoGridSquare square) {
        ItemContainer playerInv = playerObj.getInventory();
        InventoryItem cleaner = playerInv.getFirstRecurse(item -> {
            FluidContainer fc = item.getFluidContainer();
            return fc != null && fc.getAmount() >= 1.25F && fc.contains(Fluid.Petrol);
        });
        InventoryItem mop = playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.CLEAN_STAINS));
        return square != null && square.haveGraffiti() && cleaner != null && mop != null;
    }

    private static boolean predicateStoreFuel(InventoryItem item) {
        FluidContainer fluidContainer = item.getFluidContainer();
        if (fluidContainer == null) {
            return false;
        } else {
            return fluidContainer.isEmpty()
                ? true
                : fluidContainer.contains(Fluid.Petrol) && fluidContainer.getAmount() < fluidContainer.getCapacity() && !item.isBroken();
        }
    }

    private static void fetchPickupItems(KahluaTable fetch, IsoObject v, PropertyContainer props, ItemContainer playerInv) {
        if (props != null) {
            if (v.getContainer() != null) {
                fetch.rawset("container", v);
            }

            String customName = props.get("CustomName");
            if (customName != null) {
                if (!props.has("IsMoveAble") && fetch.rawget("pickupItem") == null) {
                    if (ScriptManager.instance.getItem(customName) != null) {
                        fetch.rawset("pickupItem", v);
                    } else {
                        KahluaTable groundCoverItems = (KahluaTable)LuaManager.env.rawget("GroundCoverItems");
                        String itemName = (String)groundCoverItems.rawget(customName);
                        if (itemName != null && ScriptManager.instance.getItem(itemName) != null) {
                            fetch.rawset("pickupItem", v);
                        }
                    }
                }

                if (playerInv.getFirstRecurse(item -> !item.isBroken() && item.hasTag(ItemTag.REMOVE_STUMP)) != null && v.isStump()) {
                    fetch.rawset("stump", v);
                }

                if (v.getSquare().getOre() != null
                    && playerInv.getFirstRecurse(
                            item -> !item.isBroken()
                                && (
                                    item.hasTag(ItemTag.PICK_AXE)
                                        || item.hasTag(ItemTag.STONE_MAUL)
                                        || item.hasTag(ItemTag.SLEDGEHAMMER)
                                        || item.hasTag(ItemTag.CLUB_HAMMER)
                                )
                        )
                        != null) {
                    if (customName.contains("ironOre")) {
                        fetch.rawset("ore", v);
                    }

                    if (customName.contains("copperOre")) {
                        fetch.rawset("ore", v);
                    }

                    if (customName.equals("FlintBoulder")) {
                        fetch.rawset("ore", v);
                    }

                    if (customName.equals("LimestoneBoulder")) {
                        fetch.rawset("ore", v);
                    }
                }
            }
        }
    }

    private static void fetchThrowCorpseOver(IsoPlayer playerObj, KahluaTable fetch, String key) {
        if (fetch.rawget("throwCorpseOver") == null) {
            if (fetch.rawget(key) instanceof IsoObject object) {
                IsoDirections dir;
                IsoGridSquare fromSquare;
                if (object.hasProperty(IsoFlagType.HoppableN)) {
                    if (object.getY() <= playerObj.getY()) {
                        fromSquare = object.getSquare();
                        dir = IsoDirections.N;
                    } else {
                        fromSquare = object.getSquare().getAdjacentSquare(IsoDirections.N);
                        dir = IsoDirections.S;
                    }
                } else if (object.getX() <= playerObj.getX()) {
                    fromSquare = object.getSquare();
                    dir = IsoDirections.W;
                } else {
                    fromSquare = object.getSquare().getAdjacentSquare(IsoDirections.W);
                    dir = IsoDirections.E;
                }

                if (playerObj.canThrowCorpseOver(fromSquare, dir)) {
                    fetch.rawset("throwCorpseOver", object);
                }
            }
        }
    }

    private static boolean compareType(String type, InventoryItem item) {
        return type != null && type.indexOf(46) == -1
            ? compareType(type, item.getType())
            : compareType(type, item.getFullType()) || compareType(type, item.getType());
    }

    private static boolean compareType(String type1, String type2) {
        if (type1 != null && type1.contains("/")) {
            int p = type1.indexOf(type2);
            if (p == -1) {
                return false;
            } else {
                char chBefore = p > 0 ? type1.charAt(p - 1) : 0;
                char chAfter = p + type2.length() < type1.length() ? type1.charAt(p + type2.length()) : 0;
                return chBefore == 0 && chAfter == '/' || chBefore == '/' && chAfter == 0 || chBefore == '/' && chAfter == '/';
            }
        } else {
            return type1.equals(type2);
        }
    }

    private static ISToolTipWrapper addToolTip() {
        KahluaTable worldObjectContextMenu = (KahluaTable)LuaManager.env.rawget("ISWorldObjectContextMenu");
        KahluaTable pool = (KahluaTable)worldObjectContextMenu.rawget("tooltipPool");
        KahluaTable tooltipsUsed = (KahluaTable)worldObjectContextMenu.rawget("tooltipsUsed");
        KahluaTable tooltip;
        ISToolTipWrapper wrappedToolTip;
        if (!pool.isEmpty()) {
            tooltip = (KahluaTable)pool.rawget(pool.size());
            wrappedToolTip = new ISToolTipWrapper(tooltip);
            pool.rawset(pool.size(), null);
        } else {
            wrappedToolTip = new ISToolTipWrapper();
            tooltip = wrappedToolTip.getTable();
        }

        tooltipsUsed.rawset(tooltipsUsed.size() + 1, tooltip);
        wrappedToolTip.reset();
        return wrappedToolTip;
    }

    private static boolean isForceDropHeavyItem(InventoryItem item) {
        return item != null && item.isForceDropHeavyItem();
    }

    private static void getSquaresInRadius(float worldX, float worldY, float worldZ, float radius, KahluaTable doneSquares, KahluaTable squares) {
        int minX = Double.valueOf(Math.floor(worldX - radius)).intValue();
        int maxX = Double.valueOf(Math.ceil(worldX + radius)).intValue();
        int minY = Double.valueOf(Math.floor(worldY - radius)).intValue();
        int maxY = Double.valueOf(Math.ceil(worldY + radius)).intValue();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                IsoGridSquare square = IsoWorld.instance.getCell().getGridSquare((double)x, (double)y, (double)worldZ);
                if (square != null && !LuaHelpers.tableContainsKey(doneSquares, square)) {
                    doneSquares.rawset(square, true);
                    squares.rawset(squares.size() + 1, square);
                }
            }
        }
    }

    private static void getWorldObjectsInRadius(int playerNum, int screenX, int screenY, KahluaTable squares, float radius, KahluaTable worldObjects) {
        radius = 48.0F / Core.getInstance().getZoom(playerNum);
        KahluaTableIterator iterator = squares.iterator();

        while (iterator.advance()) {
            IsoGridSquare square = (IsoGridSquare)iterator.getValue();
            if (square != null) {
                ArrayList<IsoWorldInventoryObject> squareObjects = square.getWorldObjects();

                for (int i = 0; i < squareObjects.size(); i++) {
                    IsoWorldInventoryObject worldObject = squareObjects.get(i);
                    float dist = IsoUtils.DistanceToSquared(screenX, screenY, worldObject.getScreenPosX(playerNum), worldObject.getScreenPosY(playerNum));
                    if (dist <= radius * radius) {
                        worldObjects.rawset(worldObjects.size() + 1, worldObject);
                    }
                }
            }
        }
    }

    private static boolean handleInteraction(
        int x, int y, boolean test, ISContextMenuWrapper context, KahluaTable worldobjects, IsoPlayer playerObj, ItemContainer playerInv
    ) {
        if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        } else if (test) {
            return true;
        } else {
            int playerNum = playerObj.getIndex();
            KahluaTable squares = LuaManager.platform.newTable();
            KahluaTable doneSquare = LuaManager.platform.newTable();
            KahluaTableIterator iterator = worldobjects.iterator();

            while (iterator.advance()) {
                IsoObject value = (IsoObject)iterator.getValue();
                if (value != null && !LuaHelpers.tableContainsKey(doneSquare, value.getSquare())) {
                    doneSquare.rawset(value.getSquare(), true);
                    squares.rawset(squares.size() + 1, value.getSquare());
                }
            }

            if (squares.isEmpty()) {
                return false;
            } else {
                KahluaTable worldObjects = LuaManager.platform.newTable();
                if (LuaHelpers.getJoypadState(playerNum) != null) {
                    iterator = squares.iterator();

                    while (iterator.advance()) {
                        IsoGridSquare square = (IsoGridSquare)iterator.getValue();
                        if (square != null) {
                            for (int i = 0; i < square.getWorldObjects().size(); i++) {
                                IsoWorldInventoryObject worldObject = square.getWorldObjects().get(i);
                                worldObjects.rawset(worldObjects.size() + 1, worldObject);
                            }
                        }
                    }
                } else {
                    KahluaTable squares2 = LuaManager.platform.newTable();
                    iterator = squares.iterator();

                    while (iterator.advance()) {
                        if (iterator.getKey() != null) {
                            squares2.rawset(iterator.getKey(), iterator.getValue());
                        }
                    }

                    float radius = 1.0F;
                    iterator = squares2.iterator();

                    while (iterator.advance()) {
                        IsoGridSquare square = (IsoGridSquare)iterator.getValue();
                        if (square != null) {
                            float worldX = LuaManager.GlobalObject.screenToIsoX(playerNum, x, y, square.getZ());
                            float worldY = LuaManager.GlobalObject.screenToIsoY(playerNum, x, y, square.getZ());
                            getSquaresInRadius(worldX, worldY, square.getZ(), 1.0F, doneSquare, squares);
                        }
                    }

                    getWorldObjectsInRadius(playerNum, x, y, squares, 1.0F, worldObjects);
                }

                if (worldObjects.isEmpty()) {
                    return false;
                } else {
                    KahluaTable itemList = LuaManager.platform.newTable();
                    iterator = worldObjects.iterator();

                    while (iterator.advance()) {
                        IsoObject worldObject = (IsoObject)iterator.getValue();
                        if (worldObject != null) {
                            String itemName = worldObject.getName();
                            if (itemName == null && worldObject instanceof IItemProvider iItemProvider) {
                                itemName = iItemProvider.getItem().getName();
                            }

                            if (itemName == null) {
                                itemName = "???";
                            }

                            KahluaTable table = (KahluaTable)itemList.rawget(itemName);
                            if (table == null) {
                                table = LuaManager.platform.newTable();
                                itemList.rawset(itemName, table);
                            }

                            table.rawset(table.size() + 1, worldObject);
                        }
                    }

                    iterator = itemList.iterator();

                    while (iterator.advance()) {
                        KahluaTable items = (KahluaTable)iterator.getValue();
                        if (items != null) {
                            IsoObject object = (IsoObject)items.rawget(1);
                            InventoryItem item = object instanceof IItemProvider iItemProvider ? iItemProvider.getItem() : null;
                            IsoGridSquare square = object.getSquare();
                            if (item instanceof Radio) {
                                IsoObject obj = null;

                                for (int i = 0; i < square.getObjects().size(); i++) {
                                    IsoObject tObj = square.getObjects().get(i);
                                    if (tObj instanceof IsoRadio && LuaHelpers.castDouble(tObj.getModData().rawget("RadioItemID")) == item.getID()) {
                                        obj = tObj;
                                        break;
                                    }
                                }

                                if (obj != null) {
                                    KahluaTable option = context.addGetUpOption(
                                        Translator.getText("IGUI_DeviceOptions"),
                                        playerObj,
                                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.activateRadio"),
                                        obj
                                    );
                                    option.rawset("itemForTexture", item);
                                }
                            }
                        }
                    }

                    return false;
                }
            }
        }
    }

    private static boolean handleGrabWorldItem(
        KahluaTable fetch, int x, int y, boolean test, ISContextMenuWrapper context, KahluaTable worldobjects, IsoPlayer playerObj, ItemContainer playerInv
    ) {
        if (playerObj.getVehicle() != null) {
            return false;
        } else if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        } else if (test) {
            return true;
        } else {
            double player = playerObj.getIndex();
            if (playerObj.isGrappling()) {
                KahluaTable dropCorpseOption = context.addOption(
                    Translator.getText("ContextMenu_Drop_Corpse"),
                    playerObj,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onDropCorpse")
                );
                ISToolTipWrapper toolTip = addToolTip();
                toolTip.getTable().rawset("description", Translator.getText("Tooltip_GrappleCorpse"));
                dropCorpseOption.rawset("toolTip", toolTip.getTable());
            }

            KahluaTable squares = LuaManager.platform.newTable();
            KahluaTable doneSquare = LuaManager.platform.newTable();
            KahluaTableIterator iterator = worldobjects.iterator();

            while (iterator.advance()) {
                IsoObject object = (IsoObject)iterator.getValue();
                if (object != null && object.getSquare() != null && !LuaHelpers.tableContainsKey(doneSquare, object.getSquare())) {
                    doneSquare.rawset(object.getSquare(), true);
                    squares.rawset(squares.size() + 1, object.getSquare());
                }
            }

            if (squares.isEmpty()) {
                return false;
            } else {
                KahluaTable worldObjects = LuaManager.platform.newTable();
                if (LuaHelpers.getJoypadState(player) != null) {
                    iterator = squares.iterator();

                    while (iterator.advance()) {
                        IsoGridSquare square = (IsoGridSquare)iterator.getValue();
                        if (square != null) {
                            for (int i = 0; i < square.getWorldObjects().size(); i++) {
                                IsoWorldInventoryObject worldObject = square.getWorldObjects().get(i);
                                worldObjects.rawset(worldObjects.size() + 1, worldObject);
                            }
                        }
                    }
                } else {
                    KahluaTable squares2 = LuaManager.platform.newTable();
                    iterator = squares.iterator();

                    while (iterator.advance()) {
                        if (iterator.getKey() != null) {
                            squares2.rawset(iterator.getKey(), iterator.getValue());
                        }
                    }

                    float radius = 1.0F;
                    iterator = squares2.iterator();

                    while (iterator.advance()) {
                        IsoGridSquare square = (IsoGridSquare)iterator.getValue();
                        if (square != null) {
                            float worldX = LuaManager.GlobalObject.screenToIsoX((int)player, x, y, square.getZ());
                            float worldY = LuaManager.GlobalObject.screenToIsoY((int)player, x, y, square.getZ());
                            getSquaresInRadius(worldX, worldY, square.getZ(), 1.0F, doneSquare, squares);
                        }
                    }

                    getWorldObjectsInRadius((int)player, x, y, squares, 1.0F, worldObjects);
                }

                IsoDeadBody body = (IsoDeadBody)fetch.rawget("body");
                boolean bodyItem = body != null && !body.isAnimal() && playerObj.getVehicle() == null && playerInv.getItemCount("Base.CorpseMale") == 0;
                if (worldObjects.isEmpty() && !bodyItem) {
                    return false;
                } else {
                    KahluaTable itemList = LuaManager.platform.newTable();
                    iterator = worldObjects.iterator();

                    while (iterator.advance()) {
                        IsoObject worldObject = (IsoObject)iterator.getValue();
                        if (worldObject != null) {
                            String itemName = worldObject.getName();
                            if (itemName == null && worldObject instanceof IItemProvider iItemProvider) {
                                itemName = iItemProvider.getItem().getName();
                            }

                            if (itemName == null) {
                                itemName = "???";
                            }

                            if (worldObject instanceof IsoWorldInventoryObject inventoryObject) {
                                if (inventoryObject.getItem() instanceof Food food) {
                                    food.updateAge();
                                }

                                itemName = ((IItemProvider)worldObject).getItem().getName();
                            }

                            KahluaTable table = (KahluaTable)itemList.rawget(itemName);
                            if (table == null) {
                                table = LuaManager.platform.newTable();
                                itemList.rawset(itemName, table);
                            }

                            table.rawset(table.size() + 1, worldObject);
                        }
                    }

                    KahluaTable expendedPlacementItems = LuaManager.platform.newTable();
                    boolean addedExtended = false;
                    KahluaTable grabOption = context.addOption(Translator.getText("ContextMenu_Grab"), worldobjects, null);
                    ISContextMenuWrapper subMenuGrab = ISContextMenuWrapper.getNew(context);
                    context.addSubMenu(grabOption, subMenuGrab.getTable());
                    iterator = itemList.iterator();

                    while (iterator.advance()) {
                        String name = (String)iterator.getKey();
                        KahluaTable items = (KahluaTable)iterator.getValue();
                        if (name != null && items != null) {
                            IsoObject itemOne = (IsoObject)items.rawget(1);
                            if (itemOne != null && itemOne.getSquare() != null && itemOne.getSquare().isWallTo(playerObj.getSquare())) {
                                context.removeLastOption();
                                break;
                            }

                            IItemProvider itemOneItemProvider = (IItemProvider)itemOne;
                            if (itemOneItemProvider != null
                                && itemOneItemProvider.getItem() != null
                                && (
                                    itemOneItemProvider.getItem().getWorldStaticItem() != null
                                        || itemOneItemProvider.getItem().getClothingItem() != null
                                        || itemOneItemProvider.getItem().getWorldStaticItem() != null
                                        || itemOneItemProvider.getItem() instanceof HandWeapon
                                )) {
                                expendedPlacementItems.rawset(name, items);
                                addedExtended = true;
                            }

                            if (items.size() > 1) {
                                name = String.format(Locale.ENGLISH, "%s (%d)", name, items.size());
                            }

                            if (items.size() > 2) {
                                KahluaTable itemOption = subMenuGrab.addOption(name, worldobjects, null);
                                KahluaTable itemsTable = LuaManager.platform.newTable();
                                itemsTable.rawset(1, items);
                                itemOption.rawset("onHighlightParams", itemsTable);
                                itemOption.rawset(
                                    "onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlightMultiple")
                                );
                                ISContextMenuWrapper subMenuItem = ISContextMenuWrapper.getNew(subMenuGrab);
                                subMenuGrab.addSubMenu(itemOption, subMenuItem.getTable());
                                subMenuItem.addOption(
                                    Translator.getText("ContextMenu_Grab_one"),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabWItem"),
                                    itemOne,
                                    player
                                );
                                subMenuItem.addOption(
                                    Translator.getText("ContextMenu_Grab_half"),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabHalfWItems"),
                                    items,
                                    player
                                );
                                subMenuItem.addOption(
                                    Translator.getText("ContextMenu_Grab_all"),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabAllWItems"),
                                    items,
                                    player
                                );
                            } else if (items.size() > 1 && itemOneItemProvider.getItem().getActualWeight() >= 3.0F) {
                                KahluaTable itemOption = subMenuGrab.addOption(name, worldobjects, null);
                                ISContextMenuWrapper subMenuItem = ISContextMenuWrapper.getNew(subMenuGrab);
                                subMenuGrab.addSubMenu(itemOption, subMenuItem.getTable());
                                subMenuItem.addOption(
                                    Translator.getText("ContextMenu_Grab_one"),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabWItem"),
                                    itemOne,
                                    player
                                );
                                subMenuItem.addOption(
                                    Translator.getText("ContextMenu_Grab_all"),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabAllWItems"),
                                    items,
                                    player
                                );
                            } else {
                                KahluaTable option = subMenuGrab.addOption(
                                    name, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabAllWItems"), items, player
                                );
                                option.rawset("itemForTexture", itemOneItemProvider.getItem());
                                KahluaTable itemsTable = LuaManager.platform.newTable();
                                itemsTable.rawset(1, itemOneItemProvider);
                                option.rawset("onHighlightParams", itemsTable);
                                option.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlight"));
                            }
                        }
                    }

                    if (LuaHelpers.getJoypadState(player) != null) {
                        addedExtended = false;
                    }

                    if (addedExtended) {
                        KahluaTable extendedPlacementOption = context.addOption(Translator.getText("ContextMenu_ExtendedPlacement"), null, null);
                        ISContextMenuWrapper subMenuPlacement = ISContextMenuWrapper.getNew(context);
                        context.addSubMenu(extendedPlacementOption, subMenuPlacement.getTable());
                        KahluaTable namesSorted = LuaManager.platform.newTable();
                        iterator = expendedPlacementItems.iterator();

                        while (iterator.advance()) {
                            Object name = iterator.getKey();
                            if (name != null) {
                                namesSorted.rawset(namesSorted.size() + 1, name);
                            }
                        }

                        Comparator<Entry<Object, Object>> tableValueStringSorter = Comparator.comparing(a -> LuaHelpers.castString(a.getValue()));
                        LuaHelpers.tableSort(namesSorted, tableValueStringSorter);
                        iterator = namesSorted.iterator();

                        while (iterator.advance()) {
                            String name = (String)iterator.getValue();
                            if (name != null) {
                                ISContextMenuWrapper subMenu = subMenuPlacement;
                                KahluaTable items = (KahluaTable)expendedPlacementItems.rawget(name);
                                if (items != null) {
                                    IItemProvider itemOnex = (IItemProvider)items.rawget(1);
                                    if (items.size() > 2 && namesSorted.size() > 1) {
                                        KahluaTable subMenuOption = subMenuPlacement.addOption(name, worldobjects, null);
                                        subMenuOption.rawset("itemForTexture", itemOnex.getItem());
                                        KahluaTable itemsTable = LuaManager.platform.newTable();
                                        itemsTable.rawset(1, items);
                                        subMenuOption.rawset("onHighlightParams", itemsTable);
                                        subMenuOption.rawset(
                                            "onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlightMultiple")
                                        );
                                        subMenu = ISContextMenuWrapper.getNew(subMenuPlacement);
                                        subMenuPlacement.addSubMenu(subMenuOption, subMenu.getTable());
                                    }

                                    KahluaTableIterator iterator2 = items.iterator();

                                    while (iterator2.advance()) {
                                        IItemProvider item = (IItemProvider)iterator2.getValue();
                                        if (item != null) {
                                            KahluaTable option = subMenu.addOption(
                                                name, item, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onExtendedPlacement"), playerObj
                                            );
                                            option.rawset("itemForTexture", item.getItem());
                                            KahluaTable itemsTable = LuaManager.platform.newTable();
                                            itemsTable.rawset(1, item);
                                            option.rawset("onHighlightParams", itemsTable);
                                            option.rawset(
                                                "onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlight")
                                            );
                                        }
                                    }
                                }
                            }
                        }
                    }

                    return handleGrabCorpseSubmenu(fetch, playerObj, worldobjects, subMenuGrab, test);
                }
            }
        }
    }

    private static boolean handleGrabCorpseSubmenu(
        KahluaTable fetch, IsoPlayer playerObj, KahluaTable worldobjects, ISContextMenuWrapper subMenuGrab, boolean test
    ) {
        IsoDeadBody body = (IsoDeadBody)fetch.rawget("body");
        if (body == null) {
            return false;
        } else if (body.isAnimal()) {
            return false;
        } else if (playerObj.getVehicle() != null) {
            return false;
        } else {
            ItemContainer playerInv = playerObj.getInventory();
            if (playerInv.getItemCount("Base.CorpseMale") > 0) {
                return false;
            } else if (test) {
                return true;
            } else {
                double player = playerObj.getIndex();
                IsoGridSquare square = body.getSquare();
                KahluaTable corpses = LuaManager.platform.newTable();
                ArrayList<IsoMovingObject> corpses2 = square.getStaticMovingObjects();

                for (int i = 0; i < corpses2.size(); i++) {
                    corpses.rawset(corpses.size() + 1, corpses2.get(i));
                }

                for (int d = 0; d < DIRECTIONS.length; d++) {
                    IsoGridSquare square2 = square.getAdjacentSquare(DIRECTIONS[d]);
                    if (square2 != null) {
                        corpses2 = square2.getStaticMovingObjects();

                        for (int i = 0; i < corpses2.size(); i++) {
                            corpses.rawset(corpses.size() + 1, corpses2.get(i));
                        }
                    }
                }

                if (corpses.size() <= 1) {
                    addGrabCorpseSubmenuOption(worldobjects, subMenuGrab, body, player);
                    return false;
                } else {
                    Comparator<Entry<Object, Object>> tableValueIsoMovingObjectDistanceSorter = (a, b) -> {
                        float distA = ((IsoMovingObject)a.getValue()).DistToSquared(playerObj);
                        float distB = ((IsoMovingObject)b.getValue()).DistToSquared(playerObj);
                        return Float.compare(distA, distB);
                    };
                    LuaHelpers.tableSort(corpses, tableValueIsoMovingObjectDistanceSorter);
                    KahluaTableIterator iterator = corpses.iterator();

                    while (iterator.advance()) {
                        IsoDeadBody corpse = (IsoDeadBody)iterator.getValue();
                        addGrabCorpseSubmenuOption(worldobjects, subMenuGrab, corpse, player);
                    }

                    return false;
                }
            }
        }
    }

    private static void addGrabCorpseSubmenuOption(KahluaTable worldobjects, ISContextMenuWrapper subMenuGrab, IsoDeadBody corpse, double player) {
        KahluaTable opt = subMenuGrab.addGetUpOption(
            Translator.getText("IGUI_ItemCat_Corpse"), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onGrabCorpseItem"), corpse, player
        );
        KahluaTable containerButtonIcons = (KahluaTable)LuaManager.env.rawget("ContainerButtonIcons");
        if (containerButtonIcons != null) {
            opt.rawset("iconTexture", corpse.isFemale() ? containerButtonIcons.rawget("inventoryfemale") : containerButtonIcons.rawget("inventorymale"));
        }

        ISToolTipWrapper toolTip = addToolTip();
        toolTip.getTable().rawset("description", Translator.getText("Tooltip_GrappleCorpse"));
        opt.rawset("toolTip", toolTip.getTable());
        if (corpse.getSquare().haveFire()) {
            opt.rawset("notAvailable", true);
            toolTip.getTable().rawset("description", Translator.getText("Tooltip_GrappleCorpseFire"));
        }

        LuaHelpers.callLuaClass("ISWorldObjectContextMenu", "initWorldItemHighlightOption", null, opt, corpse);
    }

    private static void prePickupGroundCoverItem(ISContextMenuWrapper context, KahluaTable worldobjects, double player, IsoObject pickupItem) {
        if (!pickupItem.getSprite().getName().contains("vegetation_farming_") && !pickupItem.getSprite().getName().contains("vegetation_gardening_")) {
            if (pickupItem.getSprite().getName().contains("d_generic") || pickupItem.getSprite().getName().contains("crafting_ore")) {
                String customName = pickupItem.getProperty("CustomName");
                String itemName = null;
                if (customName != null) {
                    KahluaTable groundCoverItems = (KahluaTable)LuaManager.env.rawget("GroundCoverItems");
                    String groundCoverItemName = (String)groundCoverItems.rawget(customName);
                    if (groundCoverItemName != null) {
                        Item groundCoverItem = ScriptManager.instance.getItem(groundCoverItemName);
                        if (groundCoverItem != null) {
                            itemName = Translator.getText(groundCoverItem.getDisplayName());
                        }
                    }
                }

                if (itemName != null) {
                    String text = Translator.getText("ContextMenu_Remove_GroundItem", itemName);
                    KahluaTable option = context.addGetUpOption(
                        text, worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onPickupGroundCoverItem"), player, pickupItem
                    );
                    option.rawset("iconTexture", Texture.trygetTexture(pickupItem.getSpriteName()).splitIcon());
                }
            }
        }
    }

    private static boolean ISEmptyGraves_canDigHere(KahluaTable worldObjects) {
        KahluaTable squares = LuaManager.platform.newTable();
        KahluaTable didSquare = LuaManager.platform.newTable();
        KahluaTableIterator iterator = worldObjects.iterator();

        while (iterator.advance()) {
            IsoObject worldObj = (IsoObject)iterator.getValue();
            if (worldObj != null && !LuaHelpers.castBoolean(didSquare.rawget(worldObj.getSquare()))) {
                squares.rawset(squares.size() + 1, worldObj.getSquare());
                didSquare.rawset(worldObj.getSquare(), true);
            }
        }

        iterator = squares.iterator();

        while (iterator.advance()) {
            IsoGridSquare square = (IsoGridSquare)iterator.getValue();
            if (square != null) {
                if (square.getZ() > 0) {
                    return false;
                }

                IsoObject floor = square.getFloor();
                if (floor != null
                    && floor.getTextureName() != null
                    && (floor.getTextureName().startsWith("floors_exterior_natural") || floor.getTextureName().startsWith("blends_natural_01"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isGraveFullOfCorpses(IsoObject grave) {
        if (grave != null && grave.getName() != null && grave.getName().equals("EmptyGraves")) {
            Double currentCorpses = LuaHelpers.castDouble(grave.getModData().rawget("corpses"));
            Double maxCorpses = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISEmptyGraves", "getMaxCorpses", null, grave));
            return currentCorpses >= maxCorpses;
        } else {
            return false;
        }
    }

    private static String getMoveableDisplayName(IsoObject obj) {
        if (obj == null) {
            return null;
        } else if (obj.getSprite() == null) {
            return null;
        } else {
            PropertyContainer props = obj.getSprite().getProperties();
            if (props.has("CustomName")) {
                String name = props.get("CustomName");
                if (props.has("GroupName")) {
                    name = String.format(Locale.ENGLISH, "%s %s", props.get("GroupName"), name);
                }

                return Translator.getMoveableDisplayName(name);
            } else {
                return null;
            }
        }
    }

    private static void doFishNetOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoGridSquare square) {
        KahluaTable fishNetOption = context.addOption(Translator.getText("ContextMenu_Place_Fishing_Net"), null, null);
        fishNetOption.rawset("iconTexture", Texture.trygetTexture("Item_FishTrap").splitIcon());
        boolean isNotAvailable = false;
        if (square.DistToProper(playerObj.getCurrentSquare()) >= 5.0F) {
            isNotAvailable = true;
        }

        ISContextMenuWrapper subMenuFishNet = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(fishNetOption, subMenuFishNet.getTable());
        ArrayList<InventoryItem> nets = playerObj.getInventory().getAllRecurse(item -> compareType("Base.FishingNet", item));

        for (int k = 0; k < nets.size(); k++) {
            KahluaTable option = subMenuFishNet.addGetUpOption(
                nets.get(k).getDisplayName(), null, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFishingNet"), playerObj, nets.get(k)
            );
            option.rawset("notAvailable", isNotAvailable);
        }
    }

    private static void doPlacedFishNetOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoObject trapFish) {
        Object fishingNetTSObj = trapFish.getSquare().getModData().rawget("fishingNetTS");
        KahluaTable subOptionFishingNet = context.addOption(Translator.getText("ContextMenu_FishingNet"), null, null);
        subOptionFishingNet.rawset("iconTexture", Texture.trygetTexture(trapFish.getTextureName()).splitIcon());
        ISContextMenuWrapper subMenuFishing = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(subOptionFishingNet, subMenuFishing.getTable());
        if (fishingNetTSObj != null) {
            Double fishingNetTS = LuaHelpers.castDouble(fishingNetTSObj);
            double hourElapsed = Math.floor((GameTime.instance.getCalender().getTimeInMillis() - fishingNetTS) / 60000.0 / 60.0);
            if (hourElapsed > 0.0) {
                KahluaTable items = subMenuFishing.addGetUpOption(
                    Translator.getText("ContextMenu_Check_Trap"),
                    null,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCheckFishingNet"),
                    playerObj,
                    trapFish,
                    hourElapsed
                );
            }
        }

        KahluaTable suboption = subMenuFishing.addGetUpOption(
            Translator.getText("ContextMenu_Remove_Trap"),
            null,
            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onRemoveFishingNet"),
            playerObj,
            trapFish
        );
        if (trapFish.getSquare().getModData().rawget("fishingNetBait") == null) {
            boolean isNotAvailable = false;
            suboption = subMenuFishing.addOption(Translator.getText("ContextMenu_Add_Bait"), null, null);
            ISContextMenuWrapper subMenu = ISContextMenuWrapper.getNew(subMenuFishing);
            subMenuFishing.addSubMenu(suboption, subMenu.getTable());
            ArrayList<InventoryItem> items = playerObj.getInventory().getAllRecurse(itemx -> itemx.getFullType().equals("Base.Chum"), new ArrayList<>());

            for (int i = 0; i < items.size(); i++) {
                InventoryItem item = items.get(i);
                KahluaTable opt2 = subMenu.addGetUpOption(
                    item.getName(), null, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddBaitToFishingNet"), playerObj, trapFish, item
                );
                opt2.rawset("notAvailable", false);
            }

            if (items.isEmpty()) {
                suboption.rawset("notAvailable", true);
                ISToolTipWrapper toolTip = addToolTip();
                suboption.rawset("toolTip", toolTip.getTable());
                toolTip.getTable().rawset("description", Translator.getText("Tooltip_NeedChumForAddToFishingNet"));
            }
        }
    }

    private static void doChumOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoGridSquare square) {
        KahluaTable option = context.addOption(Translator.getText("ContextMenu_AddChum"), null, null);
        ISContextMenuWrapper submenu = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(option, submenu.getTable());
        ArrayList<InventoryItem> chumItems = playerObj.getInventory().getAllTypeRecurse("Base.Chum");

        for (int i = 0; i < chumItems.size(); i++) {
            InventoryItem item = chumItems.get(i);
            submenu.addGetUpOption(item.getDisplayName(), playerObj, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddBaitToWater"), item, square);
        }
    }

    private static void doCreateChumOptions(ISContextMenuWrapper context, IsoPlayer playerObj, IsoGridSquare square) {
        context.addGetUpOption(
            Translator.getText("ContextMenu_MakeChum"),
            playerObj,
            LuaManager.getFunctionObject("ISWorldObjectContextMenu.doCreateChumOptions_makeChum"),
            square
        );
    }

    private static boolean isSomethingTo(IsoObject item, IsoPlayer playerObj) {
        if (item != null && item.getSquare() != null) {
            IsoGridSquare playerSq = playerObj.getCurrentSquare();
            if (!AdjacentFreeTileFinder.isTileOrAdjacent(playerSq, item.getSquare())) {
                playerSq = AdjacentFreeTileFinder.Find(item.getSquare(), playerObj, null);
            }

            return playerSq != null && item.getSquare().isSomethingTo(playerSq);
        } else {
            return false;
        }
    }

    private static void doSleepOption(ISContextMenuWrapper context, IsoObject bed, double player, IsoPlayer playerObj) {
        if (playerObj.getVehicle() == null) {
            String text = bed != null ? Translator.getText("ContextMenu_Sleep") : Translator.getText("ContextMenu_SleepOnGround");
            String bedType = getBedQuality(playerObj, bed);
            if (bedType.equals("floorPillow")) {
                text = Translator.getText("ContextMenu_SleepOnGroundPillow");
            }

            if (bed == null || bed.getSquare().getRoom() == playerObj.getSquare().getRoom()) {
                boolean isOnBed = playerObj.getSitOnFurnitureObject() == bed;
                if (bed != null && !isOnBed) {
                    ArrayList<IsoObject> objects = new ArrayList<>();
                    bed.getSpriteGridObjectsIncludingSelf(objects);
                    isOnBed = objects.contains(playerObj.getSitOnFurnitureObject());
                }

                KahluaTable sleepOption;
                if (isOnBed) {
                    sleepOption = context.addOption(text, bed, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSleep"), player);
                } else {
                    sleepOption = context.addGetUpOption(text, bed, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSleep"), player);
                }

                String tooltipText = null;
                boolean sleepNeeded = !GameClient.client || ServerOptions.getInstance().sleepNeeded.getValue();
                if (sleepNeeded && playerObj.getStats().get(CharacterStat.FATIGUE) <= 0.3F) {
                    sleepOption.rawset("notAvailable", true);
                    tooltipText = Translator.getText("IGUI_Sleep_NotTiredEnough");
                }

                boolean isZombies = playerObj.getStats().getNumVisibleZombies() > 0
                    || playerObj.getStats().getNumChasingZombies() > 0
                    || playerObj.getStats().getNumVeryCloseZombies() > 0;
                if (sleepNeeded && isZombies) {
                    sleepOption.rawset("notAvailable", true);
                    tooltipText = Translator.getText("IGUI_Sleep_NotSafe");
                }

                if (sleepNeeded && playerObj.getHoursSurvived() - playerObj.getLastHourSleeped() <= 1.0) {
                    sleepOption.rawset("notAvailable", true);
                    tooltipText = Translator.getText("ContextMenu_NoSleepTooEarly");
                } else if (playerObj.getSleepingTabletEffect() < 2000.0F) {
                    if (playerObj.getMoodles().getMoodleLevel(MoodleType.PAIN) >= 2 && playerObj.getStats().get(CharacterStat.FATIGUE) <= 0.85F) {
                        sleepOption.rawset("notAvailable", true);
                        tooltipText = Translator.getText("ContextMenu_PainNoSleep");
                    } else if (playerObj.getMoodles().getMoodleLevel(MoodleType.PANIC) >= 1) {
                        sleepOption.rawset("notAvailable", true);
                        tooltipText = Translator.getText("ContextMenu_PanicNoSleep");
                    }
                }

                if (bed != null) {
                    String bedTypeXln = Translator.getTextOrNull(String.format(Locale.ENGLISH, "%s%s", "Tooltip_BedType_", bedType));
                    if (bedTypeXln != null) {
                        if (tooltipText != null) {
                            tooltipText = String.format(Locale.ENGLISH, "%s <BR> %s", tooltipText, Translator.getText("Tooltip_BedType", bedTypeXln));
                        } else {
                            tooltipText = Translator.getText("Tooltip_BedType", bedTypeXln);
                        }
                    }
                }

                if (tooltipText != null) {
                    ISToolTipWrapper sleepTooltip = addToolTip();
                    sleepTooltip.setName(Translator.getText("ContextMenu_Sleeping"));
                    sleepTooltip.getTable().rawset("description", tooltipText);
                    sleepOption.rawset("toolTip", sleepTooltip.getTable());
                }
            }
        }
    }

    private static String getBedQuality(IsoPlayer playerObj, IsoObject bed) {
        boolean playerHasPillow = playerObj.getPrimaryHandItem() != null && playerObj.getPrimaryHandItem().hasTag(ItemTag.PILLOW)
            || playerObj.getSecondaryHandItem() != null && playerObj.getSecondaryHandItem().hasTag(ItemTag.PILLOW);
        if (playerObj.getVehicle() != null) {
            String bedType = "badBed";
            if (playerHasPillow) {
                return "badBedPillow";
            } else {
                BaseVehicle vehicle = playerObj.getVehicle();
                VehiclePart seat = vehicle.getPartForSeatContainer(vehicle.getSeat(playerObj));
                ItemContainer cont = seat.getItemContainer();
                return cont.containsTag(ItemTag.PILLOW) ? "badBedPillow" : bedType;
            }
        } else if (bed == null) {
            String bedType = "floor";
            if (playerHasPillow) {
                return "floorPillow";
            } else {
                IsoGridSquare square = playerObj.getSquare();
                ArrayList<IsoWorldInventoryObject> worldObjects = square.getWorldObjects();

                for (int i = 0; i < worldObjects.size(); i++) {
                    InventoryItem item = worldObjects.get(i).getItem();
                    if (item != null && item.hasTag(ItemTag.PILLOW)) {
                        return "floorPillow";
                    }
                }

                return bedType;
            }
        } else {
            String bedType = bed.getProperty("BedType");
            if (bedType == null) {
                bedType = "averageBed";
            }

            if (!bed.propertyEquals("CustomName", "Tent") && !bed.propertyEquals("CustomName", "Shelter")) {
                if (playerHasPillow) {
                    return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
                } else {
                    if (bed.getSquare() != null) {
                        ArrayList<IsoObject> objects = new ArrayList<>();
                        bed.getSpriteGridObjectsIncludingSelf(objects);

                        for (int n = 1; n < objects.size(); n++) {
                            IsoGridSquare square = objects.get(n - 1).getSquare();
                            ArrayList<IsoWorldInventoryObject> worldObjects = square.getWorldObjects();

                            for (int ix = 0; ix < worldObjects.size(); ix++) {
                                InventoryItem item = worldObjects.get(ix).getItem();
                                if (item != null && item.hasTag(ItemTag.PILLOW)) {
                                    return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
                                }
                            }
                        }
                    }

                    return bedType;
                }
            } else {
                if (bed.getContainer() != null) {
                    ItemContainer cont = bed.getContainer();
                    if (cont.containsTag(ItemTag.TENT_BED)) {
                        bedType = "averageBed";
                    }

                    if (cont.containsTag(ItemTag.PILLOW)) {
                        return String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow");
                    }
                }

                return playerHasPillow ? String.format(Locale.ENGLISH, "%s%s", bedType, "Pillow") : bedType;
            }
        }
    }

    private static ISContextMenuWrapper doFluidContainerMenu(
        ISContextMenuWrapper context, IsoObject object, double player, IsoPlayer playerObj, KahluaTable worldObjects
    ) {
        if (object instanceof IsoWorldInventoryObject) {
            return null;
        } else {
            String containerName = getMoveableDisplayName(object);
            if (containerName == null) {
                containerName = object.getFluidUiName();
            }

            KahluaTable option = context.addOption(containerName, null, null);
            Texture isTrough = Texture.trygetTexture(object.getSpriteName());
            if (isTrough instanceof Texture) {
                option.rawset("iconTexture", isTrough.splitIcon());
            }

            if (object instanceof IsoWorldInventoryObject worldInventoryObject) {
                option.rawset("itemForTexture", worldInventoryObject.getItem());
                KahluaTable itemsTable = LuaManager.platform.newTable();
                itemsTable.rawset(1, worldInventoryObject);
                option.rawset("onHighlightParams", itemsTable);
                option.rawset("onHighlight", LuaManager.getFunctionObject("ISWorldObjectContextMenu.handleGrabWorldItem_onHighlight"));
            }

            ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(option, mainSubMenu.getTable());
            boolean isTroughx = false;
            if (object instanceof IsoFeedingTrough feedingTrough) {
                object = feedingTrough.getMasterTrough();
                context.getTable().rawset("troughSubmenu", mainSubMenu.getTable());
                context.getTable().rawset("dontShowLiquidOption", true);
                isTroughx = true;
            }

            FluidContainer fluidContainer = object.getFluidContainer();
            if (!isTroughx) {
                mainSubMenu.addOption(
                    Translator.getText("Fluid_Show_Info"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFluidInfo"), fluidContainer
                );
            }

            if (fluidContainer != null && fluidContainer.canPlayerEmpty()) {
                mainSubMenu.addOption(
                    Translator.getText("Fluid_Transfer_Fluids"),
                    player,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFluidTransfer"),
                    fluidContainer
                );
                if (Core.debug) {
                    KahluaTable addFluidOption = mainSubMenu.addDebugOption(Translator.getText("ContextMenu_AddFluid"), player, null, null, null);
                    if (addFluidOption != null) {
                        ISContextMenuWrapper addFluidSubmenu = ISContextMenuWrapper.getNew(mainSubMenu);
                        mainSubMenu.addSubMenu(addFluidOption, addFluidSubmenu.getTable());
                        List<FluidType> fluidTypes = Arrays.asList(FluidType.values());
                        fluidTypes.sort((o1, o2) -> o1.toString().compareTo(o2.toString()));

                        for (FluidType fluidType : fluidTypes) {
                            Fluid fluid = Fluid.Get(fluidType.toString());
                            if (fluid != null && fluidContainer.canAddFluid(fluid)) {
                                addFluidSubmenu.addOption(
                                    fluidType.toString(),
                                    playerObj,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.addFluidDebug"),
                                    fluidContainer,
                                    fluid
                                );
                            }
                        }
                    }
                }

                if (object.hasFluid()) {
                    doDrinkWaterMenu(object, player, playerObj, worldObjects, mainSubMenu);
                    doFillFluidMenu(object, player, playerObj, worldObjects, mainSubMenu);
                }

                if (object.hasWater()) {
                    doWashClothingMenu(object, player, playerObj, mainSubMenu);
                }

                if (object.hasFluid() && object.getFluidCapacity() < 9999.0F) {
                    mainSubMenu.addOption(
                        Translator.getText("Fluid_Empty"), player, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onFluidEmpty"), fluidContainer
                    );
                }
            }

            return mainSubMenu;
        }
    }

    private static void addFluidFromItem(
        KahluaTable fetch,
        boolean test,
        ISContextMenuWrapper context,
        IsoObject pourFluidInto,
        KahluaTable worldobjects,
        IsoPlayer playerObj,
        ItemContainer playerInv
    ) {
        if (pourFluidInto != null) {
            if (!pourFluidInto.isFluidInputLocked()) {
                KahluaTable pourOut = LuaManager.platform.newTable();

                for (int i = 0; i < playerInv.getItems().size(); i++) {
                    InventoryItem item = playerInv.getItems().get(i);
                    if (item.canStoreWater() && pourFluidInto.canTransferFluidFrom(item.getFluidContainer()) && item.getFluidContainer().canPlayerEmpty()) {
                        pourOut.rawset(pourOut.size() + 1, item);
                    }
                }

                if (!pourOut.isEmpty() && !test && pourFluidInto.getFluidAmount() < pourFluidInto.getFluidCapacity()) {
                    KahluaTable subMenuOption = context.addOption(Translator.getText("ContextMenu_AddFluidFromItem"), worldobjects, null);
                    ISContextMenuWrapper subMenu = ISContextMenuWrapper.getNew(context);
                    context.addSubMenu(subMenuOption, subMenu.getTable());
                    KahluaTableIterator iterator = pourOut.iterator();

                    while (iterator.advance()) {
                        InventoryItem item = (InventoryItem)iterator.getValue();
                        KahluaTable subOption = subMenu.addOption(
                            item.getName(),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onAddFluidFromItem"),
                            pourFluidInto,
                            item,
                            playerObj
                        );
                        subOption.rawset("itemForTexture", item);
                        if (item.IsDrainable()) {
                            DrainableComboItem drainableItem = (DrainableComboItem)item;
                            ISToolTipWrapper tooltip = addToolTip();
                            UIFont font = (UIFont)tooltip.getTable().rawget("font");
                            int tx = TextManager.instance
                                    .MeasureStringX(font, String.format(Locale.ENGLISH, "%s:", Translator.getText("ContextMenu_WaterName")))
                                + 20;
                            String descriptionText = String.format(
                                Locale.ENGLISH,
                                "%s: <SETX.%d> %d / %d",
                                Translator.getText("ContextMenu_WaterName"),
                                tx,
                                item.getCurrentUses(),
                                Double.valueOf(Math.floor(1.0F / drainableItem.getUseDelta() + 1.0E-4F)).intValue()
                            );
                            tooltip.getTable().rawset("description", descriptionText);
                            FluidContainer fc = drainableItem.getFluidContainer();
                            if (fc != null && fc.isTainted() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
                                tooltip.getTable()
                                    .rawset(
                                        "description",
                                        String.format(
                                            Locale.ENGLISH, "%s <BR> <RGB.1,0.5,0.5> %s", descriptionText, Translator.getText("Tooltip_item_TaintedWater")
                                        )
                                    );
                            }

                            subOption.rawset("toolTip", tooltip.getTable());
                        }
                    }
                }
            }
        }
    }

    private static String formatWaterAmount(IsoObject object, float amount, float max) {
        return max >= 9999.0F
            ? String.format(Locale.ENGLISH, "%s: %s", object.getFluidUiName(), Translator.getText("Tooltip_WaterUnlimited"))
            : String.format(Locale.ENGLISH, "%s: %.2fL / %.2fL", object.getFluidUiName(), amount, max);
    }

    private static void doDrinkWaterMenu(IsoObject object, double player, IsoPlayer playerObj, KahluaTable worldobjects, ISContextMenuWrapper context) {
        float thirst = playerObj.getStats().get(CharacterStat.THIRST);
        if (object.getSquare().getBuilding() == playerObj.getBuilding()) {
            if (!(object instanceof IsoClothingDryer)) {
                if (!(object instanceof IsoClothingWasher)) {
                    Fluid fluid = object.getPrimaryFluid();
                    if (fluid != null && fluid.isCategory(FluidCategory.Beverage)) {
                        KahluaTable option = context.addGetUpOption(
                            Translator.getText("ContextMenu_Drink"),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onDrink"),
                            object,
                            player
                        );
                        object.getFluidAmount();
                        ISToolTipWrapper tooltip = addToolTip();
                        float waterAmount = object.getFluidAmount();
                        float waterMax = object.getFluidCapacity();
                        String descriptionText = LuaHelpers.castString(tooltip.getTable().rawget("description"));
                        descriptionText = String.format(Locale.ENGLISH, "%s%s", descriptionText, formatWaterAmount(object, waterAmount, waterMax));
                        tooltip.getTable().rawset("description", descriptionText);
                        if (object.isTaintedWater() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
                            descriptionText = String.format(
                                Locale.ENGLISH, "%s <BR> <RGB.1,0.5,0.5> %s", descriptionText, Translator.getText("Tooltip_item_TaintedWater")
                            );
                            tooltip.getTable().rawset("description", descriptionText);
                        }

                        option.rawset("toolTip", tooltip.getTable());
                    }
                }
            }
        }
    }

    private static void doFillFluidMenu(IsoObject sink, double playerNum, IsoPlayer playerObj, KahluaTable worldobjects, ISContextMenuWrapper context) {
        if (sink.getSquare().getBuilding() == playerObj.getBuilding()) {
            ItemContainer playerInv = playerObj.getInventory();
            KahluaTable allContainers = LuaManager.platform.newTable();
            KahluaTable allContainerTypes = LuaManager.platform.newTable();
            KahluaTable allContainersOfType = LuaManager.platform.newTable();
            ArrayList<InventoryItem> pourInto = playerInv.getAllRecurse(
                item -> item.getFluidContainer() != null && !item.getFluidContainer().isFull() && item.getFluidContainer().canAddFluid(Fluid.Water),
                new ArrayList<>()
            );
            if (!pourInto.isEmpty()) {
                KahluaTable fillOption = context.addOption(Translator.getText("ContextMenu_Fill"), worldobjects, null);
                if (sink.getSquare() == null) {
                    fillOption.rawset("notAvailable", true);
                } else if (playerObj.hasFullInventory()) {
                    ISToolTipWrapper tooltip = addToolTip();
                    fillOption.rawset("notAvailable", true);
                    tooltip.getTable().rawset("description", Translator.getText("ContextMenu_FullInventory"));
                    fillOption.rawset("toolTip", tooltip.getTable());
                } else {
                    for (int i = 0; i < pourInto.size(); i++) {
                        InventoryItem container = pourInto.get(i);
                        if (sink.canTransferFluidTo(container.getFluidContainer())) {
                            allContainers.rawset(allContainers.size() + 1, container);
                        }
                    }

                    Comparator<Entry<Object, Object>> tableValueStringSorter = Comparator.comparing(a -> ((InventoryItem)a.getValue()).getName());
                    LuaHelpers.tableSort(allContainers, tableValueStringSorter);
                    InventoryItem previousContainer = null;
                    KahluaTableIterator iterator = allContainers.iterator();

                    while (iterator.advance()) {
                        InventoryItem container = (InventoryItem)iterator.getValue();
                        if (container != null) {
                            if (previousContainer != null && container.getName() != null && !container.getName().equals(previousContainer.getName())) {
                                allContainerTypes.rawset(allContainerTypes.size() + 1, allContainersOfType);
                                allContainersOfType = LuaManager.platform.newTable();
                            }

                            allContainersOfType.rawset(allContainersOfType.size() + 1, container);
                            previousContainer = container;
                        }
                    }

                    allContainerTypes.rawset(allContainerTypes.size() + 1, allContainersOfType);
                    ISContextMenuWrapper containerMenu = ISContextMenuWrapper.getNew(context);
                    context.addSubMenu(fillOption, containerMenu.getTable());
                    ISToolTipWrapper tooltip = createWaterSourceTooltip(sink);
                    KahluaTable tooltipTable = tooltip != null ? tooltip.getTable() : null;
                    if (allContainers.size() > 1) {
                        KahluaTable containerOption = containerMenu.addGetUpOption(
                            Translator.getText("ContextMenu_FillAll"),
                            worldobjects,
                            LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"),
                            sink,
                            allContainers,
                            null,
                            playerNum
                        );
                        containerOption.rawset("toolTip", tooltipTable);
                    }

                    iterator = allContainerTypes.iterator();

                    while (iterator.advance()) {
                        KahluaTable containerType = (KahluaTable)iterator.getValue();
                        if (containerType != null && !containerType.isEmpty()) {
                            InventoryItem destItem = (InventoryItem)containerType.rawget(1);
                            if (containerType.size() > 1) {
                                String name = String.format(Locale.ENGLISH, "%s (%d)", destItem.getName(), containerType.size());
                                KahluaTable containerOption = containerMenu.addOption(name, worldobjects, null);
                                containerOption.rawset("itemForTexture", destItem);
                                ISContextMenuWrapper containerTypeMenu = ISContextMenuWrapper.getNew(containerMenu);
                                containerMenu.addSubMenu(containerOption, containerTypeMenu.getTable());
                                KahluaTable containerTypeOption = null;
                                containerTypeOption = containerTypeMenu.addGetUpOption(
                                    Translator.getText("ContextMenu_FillOne"),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"),
                                    sink,
                                    LuaManager.platform.newTable(),
                                    destItem,
                                    playerNum
                                );
                                containerTypeOption.rawset("toolTip", tooltipTable);
                                if (containerType.rawget(2) != null) {
                                    containerTypeOption = containerTypeMenu.addGetUpOption(
                                        Translator.getText("ContextMenu_FillAll"),
                                        worldobjects,
                                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"),
                                        sink,
                                        containerType,
                                        null,
                                        playerNum
                                    );
                                    containerTypeOption.rawset("toolTip", tooltipTable);
                                }
                            } else {
                                KahluaTable containerOption = containerMenu.addOption(
                                    destItem.getName(),
                                    worldobjects,
                                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onTakeWater"),
                                    sink,
                                    null,
                                    destItem,
                                    playerNum
                                );
                                containerOption.rawset("itemForTexture", destItem);
                                ISToolTipWrapper t = createWaterSourceTooltip(sink);
                                if (t != null) {
                                    if (destItem instanceof DrainableComboItem) {
                                        String newDescription = LuaHelpers.castString(t.getTable().rawget("description"));
                                        newDescription = String.format(
                                            Locale.ENGLISH,
                                            "%s <LINE> %s: %d/10",
                                            newDescription,
                                            Translator.getText("ContextMenu_ItemWaterCapacity"),
                                            Double.valueOf(Math.floor(destItem.getCurrentUsesFloat() * 10.0F)).intValue()
                                        );
                                        t.getTable().rawset("description", newDescription);
                                    }

                                    containerOption.rawset("toolTip", t.getTable());
                                } else {
                                    Object functionObj = LuaManager.getFunctionObject("ISWorldObjectContextMenu.addToolTipInv");
                                    if (functionObj != null) {
                                        Object[] result = LuaManager.caller.pcall(LuaManager.thread, functionObj, destItem);
                                        if (result != null
                                            && result.length == 2
                                            && result[0] instanceof Boolean success
                                            && success
                                            && result[1] instanceof KahluaTable tooltipInv) {
                                            containerOption.rawset("toolTip", tooltipInv);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static ISToolTipWrapper createWaterSourceTooltip(IsoObject sink) {
        ISToolTipWrapper tooltip = addToolTip();
        String description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
        if (sink.isTaintedWater() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
            if (!description.isEmpty()) {
                description = String.format(Locale.ENGLISH, "%s\n<RGB:1,0.5,0.5> %s", description, Translator.getText("Tooltip_item_TaintedWater"));
                tooltip.getTable().rawset("description", description);
            } else {
                description = String.format(Locale.ENGLISH, "<RGB:1,0.5,0.5> %s", Translator.getText("Tooltip_item_TaintedWater"));
                tooltip.getTable().rawset("description", description);
            }
        }

        tooltip.getTable().rawset("maxLineWidth", 512.0);
        return description.isEmpty() ? null : tooltip;
    }

    private static void setWashClothingTooltip(
        double soapRemaining, double waterRemaining, double soapRequired, double waterRequired, KahluaTable washList, KahluaTable option
    ) {
        ISToolTipWrapper tooltip = addToolTip();
        String description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
        if (soapRemaining < soapRequired) {
            description = description + Translator.getText("IGUI_Washing_WithoutSoap") + " <LINE> ";
        } else {
            description = String.format(
                Locale.ENGLISH,
                "%s%s: %.2f / %.0f <LINE> ",
                description,
                Translator.getText("IGUI_Washing_Soap"),
                Math.min(soapRemaining, soapRequired),
                soapRequired
            );
        }

        description = String.format(
            Locale.ENGLISH,
            "%s%s: %.2f / %.0f",
            description,
            Translator.getText("ContextMenu_WaterName"),
            Math.min(waterRemaining, waterRequired),
            waterRequired
        );
        tooltip.getTable().rawset("description", description);
        option.rawset("toolTip", tooltip.getTable());
        if (waterRemaining < waterRequired) {
            option.rawset("notAvailable", true);
        }
    }

    private static void doWashClothingMenu(IsoObject sink, double player, IsoPlayer playerObj, ISContextMenuWrapper context) {
        if (sink.getSquare().getBuilding() == playerObj.getBuilding()) {
            ItemContainer playerInv = playerObj.getInventory();
            boolean washEquipment = false;
            KahluaTable washList = LuaManager.platform.newTable();
            List<InventoryItem> soapList = playerObj.getInventory().getSoapList(null, true);
            boolean washYourself = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetRequiredWater", null, playerObj)) > 0.0;
            KahluaTable washClothing = LuaManager.platform.newTable();
            ArrayList<InventoryItem> clothingInventory = playerInv.getItemsFromCategory("Clothing");

            for (int i = 0; i < clothingInventory.size(); i++) {
                InventoryItem item = clothingInventory.get(i);
                if (!item.isHidden() && (item.hasBlood() || item.hasDirt()) && !item.hasTag(ItemTag.BREAK_WHEN_WET)) {
                    if (!washEquipment) {
                        washEquipment = true;
                    }

                    washList.rawset(washList.size() + 1, item);
                    washClothing.rawset(washClothing.size() + 1, item);
                }
            }

            KahluaTable washOther = LuaManager.platform.newTable();
            ArrayList<InventoryItem> dirtyRagInventory = playerInv.getAllTag(ItemTag.CAN_BE_WASHED, new ArrayList<>());

            for (int ix = 0; ix < dirtyRagInventory.size(); ix++) {
                InventoryItem item = dirtyRagInventory.get(ix);
                if (item.getJobDelta() == 0.0F) {
                    if (!washEquipment) {
                        washEquipment = true;
                    }

                    washList.rawset(washList.size() + 1, item);
                    washOther.rawset(washOther.size() + 1, item);
                }
            }

            KahluaTable washWeapon = LuaManager.platform.newTable();
            ArrayList<InventoryItem> weaponInventory = playerInv.getItemsFromCategory("Weapon");

            for (int ixx = 0; ixx < weaponInventory.size(); ixx++) {
                InventoryItem item = weaponInventory.get(ixx);
                if (item.hasBlood()) {
                    if (!washEquipment) {
                        washEquipment = true;
                    }

                    washList.rawset(washList.size() + 1, item);
                    washWeapon.rawset(washWeapon.size() + 1, item);
                }
            }

            KahluaTable washContainer = LuaManager.platform.newTable();
            ArrayList<InventoryItem> containerInventory = playerInv.getItemsFromCategory("Container");

            for (int ixxx = 0; ixxx < containerInventory.size(); ixxx++) {
                InventoryItem item = containerInventory.get(ixxx);
                if (!item.isHidden() && (item.hasBlood() || item.hasDirt())) {
                    washEquipment = true;
                    washList.rawset(washList.size() + 1, item);
                    washContainer.rawset(washContainer.size() + 1, item);
                }
            }

            Comparator<Entry<Object, Object>> compareClothingBlood = (a, b) -> {
                Double reqSoapA = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredSoap", null, a.getValue()));
                Double reqSoapB = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredSoap", null, b.getValue()));
                return reqSoapA.compareTo(reqSoapB);
            };
            LuaHelpers.tableSort(washList, compareClothingBlood);
            LuaHelpers.tableSort(washClothing, compareClothingBlood);
            LuaHelpers.tableSort(washOther, compareClothingBlood);
            LuaHelpers.tableSort(washWeapon, compareClothingBlood);
            LuaHelpers.tableSort(washContainer, compareClothingBlood);
            if (washYourself || washEquipment) {
                KahluaTable mainOption = context.addOption(Translator.getText("ContextMenu_Wash"), null, null);
                ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(context);
                context.addSubMenu(mainOption, mainSubMenu.getTable());
                double soapRemaining = 0.0;
                if (soapList != null && !soapList.isEmpty()) {
                    soapRemaining = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetSoapRemaining", null, soapList));
                }

                float waterRemaining = sink.getFluidAmount();
                if (washYourself) {
                    soapRemaining = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetSoapRemaining", null, soapList));
                    double soapRequired = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetRequiredSoap", null, playerObj));
                    double waterRequired = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashYourself", "GetRequiredWater", null, playerObj));
                    List<InventoryItem> barSoapsList = playerObj.getInventory().getSoapList(null, false);
                    KahluaTable option = mainSubMenu.addGetUpOption(
                        Translator.getText("ContextMenu_Yourself"),
                        playerObj,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashYourself"),
                        sink,
                        barSoapsList
                    );
                    ISToolTipWrapper tooltip = addToolTip();
                    String description = LuaHelpers.castString(tooltip.getTable().rawget("description"));
                    if (soapRemaining < soapRequired) {
                        description = String.format(Locale.ENGLISH, "%s%s <LINE> ", description, Translator.getText("IGUI_Washing_WithoutSoap"));
                    } else {
                        description = String.format(
                            Locale.ENGLISH,
                            "%s%s: %.2f / %.2f <LINE> ",
                            description,
                            Translator.getText("IGUI_Washing_Soap"),
                            Math.min(soapRemaining, soapRequired),
                            soapRequired
                        );
                    }

                    description = String.format(
                        Locale.ENGLISH,
                        "%s%s: %.2f / %.2f",
                        description,
                        Translator.getText("ContextMenu_WaterName"),
                        Math.min((double)waterRemaining, waterRequired),
                        waterRequired
                    );
                    HumanVisual visual = playerObj.getHumanVisual();
                    float bodyBlood = 0.0F;
                    float bodyDirt = 0.0F;

                    for (int ixxxx = 0; ixxxx < BloodBodyPartType.MAX.index(); ixxxx++) {
                        BloodBodyPartType part = BloodBodyPartType.FromIndex(ixxxx);
                        bodyBlood += visual.getBlood(part);
                        bodyDirt += visual.getDirt(part);
                    }

                    if (bodyBlood > 0.0F) {
                        description = String.format(
                            Locale.ENGLISH,
                            "%s <LINE> %s: %.0f / 100",
                            description,
                            Translator.getText("Tooltip_clothing_bloody"),
                            Math.ceil(bodyBlood / BloodBodyPartType.MAX.index() * 100.0F)
                        );
                    }

                    if (bodyDirt > 0.0F) {
                        description = String.format(
                            Locale.ENGLISH,
                            "%s <LINE> %s: %.0f / 100",
                            description,
                            Translator.getText("Tooltip_clothing_dirty"),
                            Math.ceil(bodyDirt / BloodBodyPartType.MAX.index() * 100.0F)
                        );
                    }

                    tooltip.getTable().rawset("description", description);
                    option.rawset("toolTip", tooltip.getTable());
                    if (waterRemaining < 1.0F) {
                        option.rawset("notAvailable", true);
                    }
                }

                if (washEquipment) {
                    soapRemaining = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetSoapRemaining", null, soapList));
                    if (!washList.isEmpty()) {
                        if (!washClothing.isEmpty()) {
                            Object[] result = LuaHelpers.callLuaClassReturnMultiple(
                                "ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washClothing
                            );
                            double soapRequiredx = LuaHelpers.castDouble(result[0]);
                            double waterRequiredx = LuaHelpers.castDouble(result[1]);
                            KahluaTable optionx = mainSubMenu.addGetUpOption(
                                Translator.getText("ContextMenu_WashAllClothing"),
                                playerObj,
                                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"),
                                sink,
                                soapList,
                                washClothing,
                                null
                            );
                            setWashClothingTooltip(soapRemaining, waterRemaining, soapRequiredx, waterRequiredx, washClothing, optionx);
                        }

                        if (!washContainer.isEmpty()) {
                            Object[] result = LuaHelpers.callLuaClassReturnMultiple(
                                "ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washContainer
                            );
                            double soapRequiredx = LuaHelpers.castDouble(result[0]);
                            double waterRequiredx = LuaHelpers.castDouble(result[1]);
                            KahluaTable optionx = mainSubMenu.addGetUpOption(
                                Translator.getText("ContextMenu_WashAllContainer"),
                                playerObj,
                                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"),
                                sink,
                                soapList,
                                washContainer,
                                null
                            );
                            setWashClothingTooltip(soapRemaining, waterRemaining, soapRequiredx, waterRequiredx, washContainer, optionx);
                        }

                        if (!washWeapon.isEmpty()) {
                            Object[] result = LuaHelpers.callLuaClassReturnMultiple(
                                "ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washWeapon
                            );
                            double soapRequiredx = LuaHelpers.castDouble(result[0]);
                            double waterRequiredx = LuaHelpers.castDouble(result[1]);
                            KahluaTable optionx = mainSubMenu.addGetUpOption(
                                Translator.getText("ContextMenu_WashAllWeapon"),
                                playerObj,
                                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"),
                                sink,
                                soapList,
                                washWeapon,
                                null
                            );
                            setWashClothingTooltip(soapRemaining, waterRemaining, soapRequiredx, waterRequiredx, washWeapon, optionx);
                        }

                        if (!washOther.isEmpty()) {
                            Object[] result = LuaHelpers.callLuaClassReturnMultiple(
                                "ISWorldObjectContextMenu", "calculateSoapAndWaterRequired", null, washOther
                            );
                            double soapRequiredx = LuaHelpers.castDouble(result[0]);
                            double waterRequiredx = LuaHelpers.castDouble(result[1]);
                            KahluaTable optionx = mainSubMenu.addGetUpOption(
                                Translator.getText("ContextMenu_WashAllBandage"),
                                playerObj,
                                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"),
                                sink,
                                soapList,
                                washOther,
                                null
                            );
                            setWashClothingTooltip(soapRemaining, waterRemaining, soapRequiredx, waterRequiredx, washOther, optionx);
                        }
                    }

                    KahluaTableIterator iterator = washList.iterator();

                    while (iterator.advance()) {
                        InventoryItem item = (InventoryItem)iterator.getValue();
                        if (item != null) {
                            double soapRequiredx = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredSoap", null, item));
                            double waterRequiredx = LuaHelpers.castDouble(LuaHelpers.callLuaClass("ISWashClothing", "GetRequiredWater", null, item));
                            ISToolTipWrapper tooltipx = addToolTip();
                            String descriptionx = LuaHelpers.castString(tooltipx.getTable().rawget("description"));
                            if (soapRemaining < soapRequiredx) {
                                descriptionx = descriptionx + Translator.getText("IGUI_Washing_WithoutSoap") + " <LINE> ";
                            } else {
                                descriptionx = String.format(
                                    Locale.ENGLISH,
                                    "%s%s: %.0f / %.0f <LINE> ",
                                    descriptionx,
                                    Translator.getText("IGUI_Washing_Soap"),
                                    Math.min(soapRemaining, soapRequiredx),
                                    soapRequiredx
                                );
                            }

                            descriptionx = String.format(
                                Locale.ENGLISH,
                                "%s%s: %.2f / %.0f",
                                descriptionx,
                                Translator.getText("ContextMenu_WaterName"),
                                Math.min((double)waterRemaining, waterRequiredx),
                                waterRequiredx
                            );
                            if ((item.IsClothing() || item.IsInventoryContainer() || item.IsWeapon()) && item.getBloodLevel() > 0.0F) {
                                descriptionx = String.format(
                                    Locale.ENGLISH,
                                    "%s <LINE> %s: %.0f / 100",
                                    descriptionx,
                                    Translator.getText("Tooltip_clothing_bloody"),
                                    Math.ceil(item.getBloodLevelAdjustedHigh())
                                );
                            }

                            if (item.IsClothing() && ((Clothing)item).getDirtiness() > 0.0F) {
                                descriptionx = String.format(
                                    Locale.ENGLISH,
                                    "%s <LINE> %s: %.0f / 100",
                                    descriptionx,
                                    Translator.getText("Tooltip_clothing_dirty"),
                                    Math.ceil(((Clothing)item).getDirtiness())
                                );
                            }

                            tooltipx.getTable().rawset("description", descriptionx);
                            KahluaTable optionx = mainSubMenu.addGetUpOption(
                                Translator.getText("ContextMenu_WashClothing", item.getDisplayName()),
                                playerObj,
                                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onWashClothing"),
                                sink,
                                soapList,
                                null,
                                item
                            );
                            optionx.rawset("toolTip", tooltipx.getTable());
                            optionx.rawset("itemForTexture", item);
                            if (waterRemaining < waterRequiredx) {
                                optionx.rawset("notAvailable", true);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void CleanBandages_getAvailableItems(KahluaTable items, IsoPlayer playerObj, String recipeName, String itemType) {
        ItemContainer playerInv = playerObj.getInventory();
        int count = playerInv.getCountTypeRecurse(itemType);
        Recipe recipe = ScriptManager.instance.getRecipe(recipeName);
        if (recipe != null && count != 0) {
            KahluaTable table = LuaManager.platform.newTable();
            table.rawset("itemType", itemType);
            table.rawset("count", count);
            table.rawset("recipe", recipe);
            items.rawset(items.size() + 1, table);
        }
    }

    private static void doRecipeUsingWaterMenu(IsoObject waterObject, IsoPlayer playerObj, ISContextMenuWrapper context) {
        ItemContainer playerInv = playerObj.getInventory();
        float waterRemaining = waterObject.getFluidAmount();
        if (!(waterRemaining < 1.0F)) {
            KahluaTable items = LuaManager.platform.newTable();
            CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Bandage", "Base.BandageDirty");
            CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Denim Strips", "Base.DenimStripsDirty");
            CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Leather Strips", "Base.LeatherStripsDirty");
            CleanBandages_getAvailableItems(items, playerObj, "Base.Clean Rag", "Base.RippedSheetsDirty");
            if (!items.isEmpty()) {
                LuaHelpers.callLuaClass("ISRecipeTooltip", "releaseAll", null);
                if (items.size() == 1) {
                    LuaHelpers.callLuaClass("CleanBandages", "setSubmenu", null, context, items.rawget(1), waterObject);
                } else {
                    ISContextMenuWrapper subMenu = ISContextMenuWrapper.getNew(context);
                    KahluaTable subOption = context.addOption(Translator.getText("ContextMenu_CleanBandageEtc"), null, null);
                    context.addSubMenu(subOption, subMenu.getTable());
                    double numItems = 0.0;
                    KahluaTableIterator iterator = items.iterator();

                    while (iterator.advance()) {
                        KahluaTable item = (KahluaTable)iterator.getValue();
                        if (item != null) {
                            numItems += LuaHelpers.castDouble(item.rawget("count"));
                        }
                    }

                    KahluaTable option = subMenu.addActionsOption(
                        Translator.getText("ContextMenu_AllWithCount", Math.min(numItems, (double)waterRemaining)),
                        LuaManager.getFunctionObject("CleanBandages.onCleanAll"),
                        waterObject,
                        items
                    );
                    if (waterObject.isTaintedWater() && SandboxOptions.getInstance().enableTaintedWaterText.getValue()) {
                        ISToolTipWrapper tooltip = addToolTip();
                        tooltip.getTable().rawset("description", " <RGB.1,0.5,0.5> " + Translator.getText("Tooltip_item_TaintedWater"));
                        tooltip.getTable().rawset("maxLineWidth", 512.0);
                        option.rawset("toolTip", tooltip.getTable());
                        option.rawset("notAvailable", true);
                    }

                    iterator = items.iterator();

                    while (iterator.advance()) {
                        Object item = iterator.getValue();
                        if (item != null) {
                            LuaHelpers.callLuaClass("CleanBandages", "setSubmenu", null, subMenu, item, waterObject);
                        }
                    }
                }
            }
        }
    }

    private static boolean toggleClothingWasher(
        ISContextMenuWrapper context, KahluaTable worldobjects, double playerId, IsoPlayer playerObj, IsoClothingWasher object
    ) {
        if (object == null || object.getContainer() == null) {
            return false;
        } else if (isSomethingTo(object, playerObj)) {
            return false;
        } else if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        } else {
            KahluaTable option;
            if (object.isActivated()) {
                option = context.addGetUpOption(
                    Translator.getText("ContextMenu_Turn_Off"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingWasher"),
                    object,
                    playerId
                );
            } else {
                option = context.addGetUpOption(
                    Translator.getText("ContextMenu_Turn_On"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingWasher"),
                    object,
                    playerId
                );
            }

            if (!object.getContainer().isPowered() || object.getFluidAmount() <= 0.0F) {
                option.rawset("notAvailable", true);
                ISToolTipWrapper toolTip = addToolTip();
                option.rawset("toolTip", toolTip.getTable());
                toolTip.setVisible(false);
                toolTip.setName(getMoveableDisplayName(object));
                String description = "";
                if (!object.getContainer().isPowered()) {
                    description = Translator.getText("IGUI_RadioRequiresPowerNearby");
                }

                if (object.getFluidAmount() <= 0.0F) {
                    if (!description.isEmpty()) {
                        description = description + "\n" + Translator.getText("IGUI_RequiresWaterSupply");
                    } else {
                        description = Translator.getText("IGUI_RequiresWaterSupply");
                    }
                }

                toolTip.getTable().rawset("description", description);
            }

            return false;
        }
    }

    private static boolean toggleComboWasherDryer(
        ISContextMenuWrapper context, IsoPlayer playerObj, IsoCombinationWasherDryer object, boolean bAddObjectSubmenu
    ) {
        if (object.getContainer() != null && !isSomethingTo(object, playerObj) && !Core.getInstance().getGameMode().equals("LastStand")) {
            String objectName = object.getName();
            if (objectName == null) {
                objectName = "Combo Washer/Dryer";
            }

            PropertyContainer props = object.getProperties();
            if (props != null) {
                String groupName = props.get("GroupName");
                String customName = props.get("CustomName");
                if (groupName != null && customName != null) {
                    objectName = Translator.getMoveableDisplayName(groupName + " " + customName);
                } else if (customName != null) {
                    objectName = Translator.getMoveableDisplayName(customName);
                }
            }

            ISContextMenuWrapper subMenu = context;
            if (bAddObjectSubmenu) {
                KahluaTable subOption = context.addOption(objectName, null, null);
                subMenu = ISContextMenuWrapper.getNew(context);
                context.addSubMenu(subOption, subMenu.getTable());
            }

            KahluaTable option;
            if (object.isActivated()) {
                option = subMenu.addGetUpOption(
                    Translator.getText("ContextMenu_Turn_Off"),
                    playerObj,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleComboWasherDryer"),
                    object
                );
            } else {
                option = subMenu.addGetUpOption(
                    Translator.getText("ContextMenu_Turn_On"),
                    playerObj,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleComboWasherDryer"),
                    object
                );
            }

            String label = object.isModeWasher()
                ? Translator.getText("ContextMenu_ComboWasherDryer_SetModeDryer")
                : Translator.getText("ContextMenu_ComboWasherDryer_SetModeWasher");
            if (!object.getContainer().isPowered() || object.isModeWasher() && object.getFluidAmount() <= 0.0F) {
                option.rawset("notAvailable", true);
                ISToolTipWrapper toolTip = addToolTip();
                option.rawset("toolTip", toolTip.getTable());
                toolTip.setVisible(false);
                toolTip.setName(getMoveableDisplayName(object));
                String description = "";
                if (!object.getContainer().isPowered()) {
                    description = Translator.getText("IGUI_RadioRequiresPowerNearby");
                }

                if (object.isModeWasher() && object.getFluidAmount() <= 0.0F) {
                    if (!description.isEmpty()) {
                        description = description + "\n" + Translator.getText("IGUI_RequiresWaterSupply");
                    } else {
                        description = Translator.getText("IGUI_RequiresWaterSupply");
                    }
                }

                toolTip.getTable().rawset("description", description);
            }

            option = subMenu.addGetUpOption(
                label,
                playerObj,
                LuaManager.getFunctionObject("ISWorldObjectContextMenu.onSetComboWasherDryerMode"),
                object,
                object.isModeWasher() ? "dryer" : "washer"
            );
            return false;
        } else {
            return false;
        }
    }

    private static boolean toggleClothingDryer(
        ISContextMenuWrapper context, double playerId, IsoPlayer playerObj, KahluaTable worldobjects, IsoClothingDryer object
    ) {
        if (object == null || object.getContainer() == null) {
            return false;
        } else if (isSomethingTo(object, playerObj)) {
            return false;
        } else if (Core.getInstance().getGameMode().equals("LastStand")) {
            return false;
        } else {
            KahluaTable option;
            if (object.isActivated()) {
                option = context.addGetUpOption(
                    Translator.getText("ContextMenu_Turn_Off"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingDryer"),
                    object,
                    playerId
                );
            } else {
                option = context.addGetUpOption(
                    Translator.getText("ContextMenu_Turn_On"),
                    worldobjects,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleClothingDryer"),
                    object,
                    playerId
                );
            }

            if (!object.getContainer().isPowered()) {
                option.rawset("notAvailable", true);
                ISToolTipWrapper toolTip = addToolTip();
                option.rawset("toolTip", toolTip.getTable());
                toolTip.setVisible(false);
                toolTip.setName(getMoveableDisplayName(object));
                toolTip.getTable().rawset("description", Translator.getText("IGUI_RadioRequiresPowerNearby"));
            }

            return false;
        }
    }

    private static boolean onWashingDryer(String source, ISContextMenuWrapper context, IsoClothingDryer object, IsoPlayer player, KahluaTable worldobjects) {
        KahluaTable mainOption = context.addOption(source, null, null);
        ISContextMenuWrapper mainSubMenu = ISContextMenuWrapper.getNew(context);
        context.addSubMenu(mainOption, mainSubMenu.getTable());
        return toggleClothingDryer(mainSubMenu, player.getIndex(), player, worldobjects, object);
    }

    private static boolean doStoveOption(KahluaTable fetch, boolean test, ISContextMenuWrapper context, double player, IsoPlayer playerObj) {
        KahluaTable worldobjects = null;
        IsoStove stove = (IsoStove)fetch.rawget("stove");
        if (stove != null
            && !isSomethingTo(stove, playerObj)
            && !Core.getInstance().getGameMode().equals("LastStand")
            && stove.getContainer() != null
            && stove.getContainer().isPowered()) {
            if (test) {
                return true;
            }

            KahluaTable stoveOption = context.addOption(stove.getTileName(), null, null);
            Texture key = Texture.trygetTexture(stove.getSpriteName());
            if (key instanceof Texture) {
                stoveOption.rawset("iconTexture", key.splitIcon());
            } else {
                stoveOption.rawset(
                    "iconTexture", ((KahluaTable)LuaManager.env.rawget("ContainerButtonIcons")).rawget(stove.isMicrowave() ? "microwave" : "stove")
                );
            }

            ISContextMenuWrapper stoveMenu = ISContextMenuWrapper.getNew(context);
            context.addSubMenu(stoveOption, stoveMenu.getTable());
            String keyx = stove.Activated() ? "ContextMenu_Turn_Off" : "ContextMenu_Turn_On";
            stoveMenu.addGetUpOption(
                Translator.getText(keyx), worldobjects, LuaManager.getFunctionObject("ISWorldObjectContextMenu.onToggleStove"), stove, player
            );
            ItemContainer var12 = stove.getContainer();
            if (var12 instanceof ItemContainer) {
                if (var12.isMicrowave()) {
                    stoveMenu.addGetUpOption(
                        Translator.getText("ContextMenu_StoveSetting"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onMicrowaveSetting"),
                        stove,
                        player
                    );
                } else if (var12.isStove()) {
                    stoveMenu.addGetUpOption(
                        Translator.getText("ContextMenu_StoveSetting"),
                        worldobjects,
                        LuaManager.getFunctionObject("ISWorldObjectContextMenu.onStoveSetting"),
                        stove,
                        player
                    );
                }
            }
        }

        return false;
    }

    public static boolean checkBlowTorchForBarricade(IsoPlayer chr) {
        return chr.getInventory().getFirstRecurse(item -> item.getType().equals("BlowTorch") && item.getCurrentUses() >= 1) != null;
    }

    private static void addTileDebugInfo(ISContextMenuWrapper context, KahluaTable fetch) {
        if (DebugOptions.instance.uiShowContextMenuReportOptions.getValue()) {
            KahluaTable option = context.addDebugOption(Translator.getText("Tile Report") + ": " + LuaHelpers.castString(fetch.rawget("tilename")), null, null);
            if (option != null) {
                ISToolTipWrapper toolTipWrapper = new ISToolTipWrapper();
                option.rawset("toolTip", toolTipWrapper.getTable());
                toolTipWrapper.initialise();
                toolTipWrapper.setVisible(false);
                toolTipWrapper.setName("Tile params:");
                IsoObject tileObj = (IsoObject)fetch.rawget("tileObj");
                PropertyContainer props = tileObj.getProperties();
                ArrayList<String> names = props.getPropertyNames();
                StringBuilder params = new StringBuilder("Properties:\n");

                for (int i = 0; i < names.size(); i++) {
                    params.append(names.get(i)).append(" = ").append(props.get(names.get(i))).append("\n");
                }

                params.append("\nFlags:\n");
                ArrayList<IsoFlagType> flags = props.getFlagsList();

                for (int i = 0; i < flags.size(); i++) {
                    params.append(flags.get(i)).append("\n");
                }

                if (tileObj != null) {
                    IHasHealth health = (IHasHealth)fetch.rawget("health");
                    boolean movedThumpable = tileObj.isMovedThumpable();
                    boolean thumpable = tileObj instanceof IsoThumpable;
                    params.append("\n").append(Translator.getText("ContextMenu_IsMovedThumpable")).append(" = ").append(movedThumpable).append("\n");
                    params.append(Translator.getText("ContextMenu_IsThumpable")).append(" = ").append(thumpable).append("\n");
                    params.append(Translator.getText("ContextMenu_HasHealth")).append(" = ").append(health != null).append("\n");
                    params.append(Translator.getText("ContextMenu_IsBreakableFence"))
                        .append(" = ")
                        .append(BrokenFences.getInstance().isBreakableObject(tileObj))
                        .append("\n");
                    params.append("\n");
                    if (!movedThumpable && !thumpable && !BrokenFences.getInstance().isBreakableObject(tileObj) && health == null) {
                        params.append(Translator.getText("ContextMenu_IsNotZombieThumpable")).append("\n");
                    } else {
                        params.append(Translator.getText("ContextMenu_IsZombieThumpable")).append("\n");
                    }

                    if (!thumpable && health == null) {
                        params.append(Translator.getText("ContextMenu_IsNotPlayerThumpable")).append("\n");
                    } else {
                        params.append(Translator.getText("ContextMenu_IsPlayerThumpable")).append("\n");
                    }
                }

                toolTipWrapper.getTable().rawset("description", params.toString());
            }
        }
    }

    private static void doContextConfigOptionsFromFetch(ISContextMenuWrapper context, KahluaTable fetch, IsoPlayer playerObj) {
        GameEntity entity = (GameEntity)fetch.rawget("entityContext");
        doContextConfigOptions(context, entity, playerObj);
    }

    private static void doContextConfigOptions(ISContextMenuWrapper context, GameEntity entity, IsoPlayer playerObj) {
        ContextMenuConfig contextConfig = entity.getComponent(ComponentType.ContextMenuConfig);
        ArrayList<ContextMenuConfigScript.EntryScript> entries = contextConfig.getEntries();

        for (int n = 0; n < entries.size(); n++) {
            ContextMenuConfigScript.EntryScript entry = entries.get(n);
            StringBuilder text = new StringBuilder("ContextMenu_");
            String textRef = Translator.getText(String.valueOf(text.append(entry.getMenu())));
            String customSubmenu = entry.getCustomSubmenu();
            String icon = entry.getIcon();
            if (customSubmenu != null) {
                KahluaTable option = context.addOption(textRef, null, null);
                Object functionObject = LuaManager.getFunctionObject(customSubmenu);
                KahluaTable arguments = LuaManager.platform.newTable();
                arguments.rawset("option", option);
                arguments.rawset("entity", entity);
                arguments.rawset("playerObj", playerObj);
                arguments.rawset("extraParam", entry.getExtraParam());
                LuaManager.caller.protectedCall(LuaManager.thread, functionObject, context.getTable(), arguments);
            }

            String customFunction = entry.getCustomFunction();
            if (customFunction != null) {
                KahluaTable option = context.addGetUpOption(
                    textRef,
                    context,
                    LuaManager.getFunctionObject("ISWorldObjectContextMenu.onCustomFunction"),
                    entity,
                    playerObj,
                    customFunction,
                    entry.getExtraParam()
                );
                option.rawset("iconTexture", icon != null ? TexturePackPage.getTexture(icon) : null);
            }
        }
    }
}
