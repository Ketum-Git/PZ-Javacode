// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.properties;

import java.util.Map;
import zombie.UsedFromLua;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public enum IsoPropertyType {
    ISO_TYPE("IsoType"),
    IS_MOVE_ABLE("IsMoveAble"),
    OPEN_TILE_OFFSET("OpenTileOffset"),
    WINDOW_LOCKED("WindowLocked"),
    SMASHED_TILE_OFFSET("SmashedTileOffset"),
    GLASS_REMOVED_OFFSET("GlassRemovedOffset"),
    GARAGE_DOOR("GarageDoor"),
    DOUBLE_DOOR("DoubleDoor"),
    ENERGY("Energy"),
    LIGHT_RADIUS("LightRadius"),
    RED_LIGHT("lightR"),
    GREEN_LIGHT("lightG"),
    BLUE_LIGHT("lightB"),
    CONNECT_X("connectX"),
    CONNECT_Y("connectY"),
    CONTAINER("container"),
    CUSTOM_NAME("CustomName"),
    GROUP_NAME("GroupName"),
    CONTAINER_CAPACITY("ContainerCapacity"),
    CONTAINER_POSITION("ContainerPosition"),
    FACING("Facing"),
    FUEL_AMOUNT("fuelAmount"),
    WATER_AMOUNT("waterAmount"),
    MAXIMUM_WATER_AMOUNT("waterMaxAmount"),
    PROPANE_TANK("propaneTank"),
    DAMAGED_SPRITE("DamagedSprite"),
    CAN_ATTACH_ANIMAL("CanAttachAnimal"),
    CONTAINER_CLOSE_SOUND("ContainerCloseSound"),
    CONTAINER_OPEN_SOUND("ContainerOpenSound"),
    CONTAINER_PUT_SOUND("ContainerPutSound"),
    CONTAINER_TAKE_SOUND("ContainerTakeSound"),
    IS_FRIDGE("IsFridge"),
    DOOR_TRANS("doorTrans"),
    STREETLIGHT("streetlight"),
    FOOTSTEP_MATERIAL("FootstepMaterial"),
    ENTITY_SCRIPT_NAME("EntityScriptName"),
    ATTACHED_SE("attachedSE"),
    WINDOW_W("windowW"),
    WINDOW_FRAME_W("WindowW"),
    WINDOW_N("windowN"),
    WINDOW_FRAME_N("WindowN"),
    IGNORE_SURFACE_SNAP("IgnoreSurfaceSnap"),
    HOPPABLE_W("HoppableW"),
    THUMP_SOUND("ThumpSound"),
    DOOR_SOUND("DoorSound"),
    HOPPABLE_N("HoppableN"),
    TIE_SHEET_ROPE("TieSheetRope"),
    IS_PAINTABLE("IsPaintable"),
    IS_TABLE("IsTable"),
    FORCE_SINGLE_ITEM("ForceSingleItem"),
    BUSH("Bush"),
    ROOF_WALL_START("RoofWallStart"),
    SLOPED_SURFACE_HEIGHT_MAX("SlopedSurfaceHeightMax"),
    IS_GRID_EXTENSION_TILE("IsGridExtensionTile"),
    SOLID("solid"),
    TAINTED_WATER("taintedWater"),
    ATTACHED_SURFACE("attachedSurface"),
    IS_TABLE_TOP("IsTableTop"),
    ATTACHED_W("attachedW"),
    WHEELIE_BIN("WheelieBin"),
    WALL_SE("WallSE"),
    WEST_ROOF_M("WestRoofM"),
    MICROWAVE("Microwave"),
    NO_WALL_LIGHTING("NoWallLighting"),
    SURFACE("Surface"),
    AMBIENT_SOUND("AmbientSound"),
    ATTACHED_E("attachedE"),
    ITEM_HEIGHT("ItemHeight"),
    NATURE_FLOOR("natureFloor"),
    BLOCKS_PLACEMENT("BlocksPlacement"),
    GRIME_TYPE("GrimeType"),
    ATTACHED_N("attachedN"),
    ATTACHED_CEILING("attachedCeiling"),
    SCRAP_USE_TOOL("ScrapUseTool"),
    ATTACHED_S("attachedS"),
    E_OFFSET("Eoffset"),
    WEST_ROOF_T("WestRoofT"),
    CUT_N("cutN"),
    CURTAIN_W("curtainW"),
    BED_TYPE("BedType"),
    CURTAIN_S("curtainS"),
    RENDER_LAYER("RenderLayer"),
    FORCE_RENDER("forceRender"),
    ATTACHED_TO_GLASS("AttachedToGlass"),
    CORNER_NORTH_WALL("CornerNorthWall"),
    IS_WATER_COLLECTOR("IsWaterCollector"),
    WEST_ROOF_B("WestRoofB"),
    SLOPED_SURFACE_DIRECTION("SlopedSurfaceDirection"),
    VEGETATION("vegitation"),
    MINIMUM_CAR_SPEED_DMG("MinimumCarSpeedDmg"),
    CUT_W("cutW"),
    CORNER_WEST_WALL("CornerWestWall"),
    BED("bed"),
    TV("TV"),
    FREEZER_POSITION("FreezerPosition"),
    STAIRS_MW("stairsMW"),
    GENERATOR_SOUND("GeneratorSound"),
    MATERIAL_TYPE("MaterialType"),
    WALL_OVERLAY("WallOverlay"),
    CLIMB_SHEET_TOP_E("climbSheetTopE"),
    STAIRS_MN("stairsMN"),
    SIGNAL("signal"),
    ATTACHED_NW("attachedNW"),
    STACK_REPLACE_TILE_OFFSET("StackReplaceTileOffset"),
    CLIMB_SHEET_TOP_S("climbSheetTopS"),
    CUTAWAY_HINT("CutawayHint"),
    CLIMB_SHEET_TOP_W("climbSheetTopW"),
    CLIMB_SHEET_TOP_N("climbSheetTopN"),
    CANT_CLIMB("CantClimb"),
    DOOR_WALL_W_TRANS("DoorWallWTrans"),
    WALL_W_TRANS("WallWTrans"),
    DOOR_WALL_N("DoorWallN"),
    STOP_CAR("StopCar"),
    HAS_LIGHT_ON_SPRITE("HasLightOnSprite"),
    DOOR_WALL_W("DoorWallW"),
    SINK_TYPE("SinkType"),
    IS_TRASH_CAN("IsTrashCan"),
    CUSTOM_ITEM("CustomItem"),
    MOVE_WITH_WIND("MoveWithWind"),
    FLOOR_HEIGHT("FloorHeight"),
    PICK_UP_TOOL("PickUpTool"),
    SOLID_FLOOR("solidfloor"),
    TRANSPARENT_FLOOR("transparentFloor"),
    IS_FLOOR_ATTACHED("IsFloorAttached"),
    WALL_NW("WallNW"),
    BLOCK_RAIN("BlockRain"),
    WIND_TYPE("WindType"),
    FLOOR_MATERIAL("FloorMaterial"),
    W_OFFSET("Woffset"),
    FLOOR_ATTACHMENT_S("FloorAttachmentS"),
    CHAIR_W("chairW"),
    BURNT_TILE("BurntTile"),
    MATERIAL("Material"),
    FLOOR_ATTACHMENT_W("FloorAttachmentW"),
    CHAIR_E("chairE"),
    FASCIA_EDGE("FasciaEdge"),
    CHAIR_S("chairS"),
    PHYSICS_SHAPE("PhysicsShape"),
    CHAIR_N("chairN"),
    IS_HIGH("IsHigh"),
    TALL_HOPPABLE_N("TallHoppableN"),
    INVISIBLE("invisible"),
    FORCE_FADE("forceFade"),
    TALL_HOPPABLE_W("TallHoppableW"),
    FLOOR_ATTACHMENT_E("FloorAttachmentE"),
    PHYSICS_MESH("PhysicsMesh"),
    IS_STACKABLE("IsStackable"),
    FLOOR_ATTACHMENT_N("FloorAttachmentN"),
    IS_SURFACE_OFFSET("IsSurfaceOffset"),
    CAN_BE_REMOVED("canBeRemoved"),
    LIGHT_SWITCH("lightswitch"),
    SLOPED_SURFACE_HEIGHT_MIN("SlopedSurfaceHeightMin"),
    NO_FREEZER("NoFreezer"),
    PICK_UP_LEVEL("PickUpLevel"),
    SPEAR_ONLY_ATTACK_THROUGH("SpearOnlyAttackThrough"),
    IS_EAVE("isEave"),
    FENCE_TYPE_HIGH("FenceTypeHigh"),
    DOOR_WALL_N_TRANS("DoorWallNTrans"),
    SEAT_MATERIAL("SeatMaterial"),
    N_OFFSET("Noffset"),
    CURTAIN_SOUND("CurtainSound"),
    PICK_UP_WEIGHT("PickUpWeight"),
    WALL_N("WallN"),
    WALL_W("WallW"),
    PLACE_TOOL("PlaceTool"),
    FENCE_TYPE_LOW("FenceTypeLow"),
    MOVEMENT("Movement"),
    ROOF_GROUP("RoofGroup"),
    MOVE_TYPE("MoveType"),
    WALL_TYPE("WallType"),
    DIAMOND_FLOOR("diamondFloor"),
    ALWAYS_DRAW("alwaysDraw"),
    S_OFFSET("Soffset"),
    FORCE_LOCKED("forceLocked"),
    COLLIDE_N("collideN"),
    FORCE_AMBIENT("ForceAmbient"),
    INTERIOR_SIDE("InteriorSide"),
    GENERIC_CRAFTING_SURFACE("GenericCraftingSurface"),
    SOLID_TRANS("solidtrans"),
    WATER_PIPED("waterPiped"),
    PAINTING_TYPE("PaintingType"),
    COLLIDE_W("collideW"),
    IS_LOW("IsLow"),
    CURTAIN_E("curtainE"),
    WALL_N_TRANS("WallNTrans"),
    CAN_BREAK("CanBreak"),
    CURTAIN_N("curtainN"),
    FREEZER("Freezer"),
    CAN_SCRAP("CanScrap"),
    TREE("tree"),
    DOUBLE_DOOR_1("DoubleDoor1"),
    WATER("water"),
    IS_MIRROR("IsMirror"),
    DOUBLE_DOOR_2("DoubleDoor2"),
    TILE_OVERLAY("TileOverlay"),
    STAIRS_TW("stairsTW"),
    STAIRS_TN("stairsTN"),
    FREEZER_CAPACITY("FreezerCapacity"),
    MATERIAL_2("Material2"),
    FLOOR_OVERLAY("FloorOverlay"),
    HIT_BY_CAR("HitByCar"),
    EXTERIOR("exterior"),
    MATERIAL_3("Material3"),
    FIRE_REQUIREMENT("fireRequirement"),
    SCRAP_USE_SKILL("ScrapUseSkill"),
    IS_CLOSED_STATE("IsClosedState"),
    SPRITE_GRID_POS("SpriteGridPos"),
    DOOR_N("doorN"),
    NEVER_CUTAWAY("NeverCutaway"),
    CLIMB_SHEET_E("climbSheetE"),
    DOOR_FR_W("doorFrW"),
    ATTACHED_FLOOR("attachedFloor"),
    CAN_BE_CUT("canBeCut"),
    WALL_OBJECT_ALLOW_DOORFRAME("WallObjectAllowDoorframe"),
    DOOR_W("doorW"),
    DOOR_FR_N("doorFrN"),
    CLIMB_SHEET_W("climbSheetW"),
    STAIRS_BW("stairsBW"),
    CLIMB_SHEET_S("climbSheetS"),
    CLIMB_SHEET_N("climbSheetN"),
    LIVING_ROOM("livingRoom"),
    STAIRS_BN("stairsBN"),
    TREAT_AS_WALL_ORDER("TreatAsWallOrder"),
    WALL_NW_TRANS("WallNWTrans"),
    CLOSE_SNEAK_BONUS("CloseSneakBonus"),
    SCRAP_SIZE("ScrapSize"),
    GRASS_FLOOR("grassFloor"),
    SNOW_TILE("SnowTile"),
    WALL("wall"),
    MAKE_WINDOW_INVINCIBLE("makeWindowInvincible"),
    COUNTERTOP("Countertop"),
    COUNTERTOP_ATTACH("CountertopAttach"),
    FITS_BENEATH_COUNTERTOP("FitsBeneathCountertop");

    private static final Map<String, IsoPropertyType> BY_NAME = PZArrayUtil.generateNameToEnumLookUpTable(IsoPropertyType.class, IsoPropertyType::getName);
    private final String name;

    private IsoPropertyType(final String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public boolean isProperty(String inPropertyName) {
        return StringUtils.equalsIgnoreCase(this.name, inPropertyName);
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public static IsoPropertyType lookup(String name) {
        IsoPropertyType propertyType = BY_NAME.get(name);
        if (propertyType == null) {
            throw new IsoPropertyType.IsoPropertyTypeNotFoundException(name);
        } else {
            return propertyType;
        }
    }

    public static String lookupOrDefaultStr(String name) {
        try {
            return lookup(name).getName();
        } catch (IsoPropertyType.IsoPropertyTypeNotFoundException var2) {
            DebugType.WorldGen.printException(var2, var2.getMessage(), LogSeverity.Error);
            return name;
        }
    }

    public static class IsoPropertyTypeNotFoundException extends RuntimeException {
        public IsoPropertyTypeNotFoundException(String inName) {
            super("Property Name not found: " + inName);
        }
    }
}
