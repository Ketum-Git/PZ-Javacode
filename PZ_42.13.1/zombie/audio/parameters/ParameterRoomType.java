// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCamera;
import zombie.iso.IsoMetaCell;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.zones.RoomTone;

public final class ParameterRoomType extends FMODGlobalParameter {
    static ParameterRoomType instance;
    static ParameterRoomType.RoomType roomType;

    public ParameterRoomType() {
        super("RoomType");
        instance = this;
    }

    @Override
    public float calculateCurrentValue() {
        return this.getRoomType().label;
    }

    private ParameterRoomType.RoomType getRoomType() {
        if (roomType != null) {
            return roomType;
        } else {
            IsoGameCharacter character = FMODParameterUtils.getFirstListener();
            if (character == null) {
                return ParameterRoomType.RoomType.Generic;
            } else {
                BuildingDef buildingDef = character.getCurrentBuildingDef();
                if (buildingDef == null) {
                    return ParameterRoomType.RoomType.Generic;
                } else if (character.getCurrentSquare().getZ() < 0) {
                    return character.getCurrentBuildingDef().getW() > 48 && character.getCurrentBuildingDef().getH() > 48
                        ? ParameterRoomType.RoomType.Bunker
                        : ParameterRoomType.RoomType.Basement;
                } else {
                    IsoMetaGrid metaGrid = IsoWorld.instance.getMetaGrid();
                    IsoMetaCell metaCell = metaGrid.getCellData(PZMath.fastfloor(character.getX() / 256.0F), PZMath.fastfloor(character.getY() / 256.0F));
                    if (metaCell != null && !metaCell.roomTones.isEmpty()) {
                        RoomDef currentRoomDef = character.getCurrentRoomDef();
                        RoomTone roomToneForBuilding = null;

                        for (int i = 0; i < metaCell.roomTones.size(); i++) {
                            RoomTone roomTone = metaCell.roomTones.get(i);
                            RoomDef roomDef = metaGrid.getRoomAt(roomTone.x, roomTone.y, roomTone.z);
                            if (roomDef != null) {
                                if (roomDef == currentRoomDef) {
                                    return ParameterRoomType.RoomType.valueOf(roomTone.enumValue);
                                }

                                if (roomTone.entireBuilding && roomDef.building == buildingDef) {
                                    roomToneForBuilding = roomTone;
                                }
                            }
                        }

                        return roomToneForBuilding != null
                            ? ParameterRoomType.RoomType.valueOf(roomToneForBuilding.enumValue)
                            : ParameterRoomType.RoomType.Generic;
                    } else {
                        return ParameterRoomType.RoomType.Generic;
                    }
                }
            }
        }
    }

    public static void setRoomType(int roomType) {
        try {
            ParameterRoomType.roomType = ParameterRoomType.RoomType.values()[roomType];
        } catch (ArrayIndexOutOfBoundsException var2) {
            ParameterRoomType.roomType = null;
        }
    }

    public static void render(IsoPlayer player) {
        if (instance != null) {
            if (player == FMODParameterUtils.getFirstListener()) {
                if (player == IsoCamera.frameState.camCharacter) {
                    player.drawDebugTextBelow("RoomType : " + instance.getRoomType().name());
                }
            }
        }
    }

    private static enum RoomType {
        Generic(0),
        Barn(1),
        Mall(2),
        Warehouse(3),
        Prison(4),
        Church(5),
        Office(6),
        Factory(7),
        MovieTheater(8),
        Basement(9),
        Bunker(10),
        House(11),
        Commercial(12);

        final int label;

        private RoomType(final int label) {
            this.label = label;
        }
    }
}
