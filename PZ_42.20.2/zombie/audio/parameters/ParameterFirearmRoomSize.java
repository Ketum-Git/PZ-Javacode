// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.iso.IsoGridSquare;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;

public final class ParameterFirearmRoomSize extends FMODLocalParameter {
    private final IsoPlayer character;

    public ParameterFirearmRoomSize(IsoPlayer character) {
        super("FirearmRoomSize");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        Object roomShooter = this.getRoom(this.character);
        return roomShooter == null ? 0.0F : this.getRoomSize(roomShooter);
    }

    private Object getRoom(IsoPlayer character) {
        IsoGridSquare square = character.getCurrentSquare();
        if (square == null) {
            return null;
        } else {
            IsoRoom room = square.getRoom();
            if (room != null) {
                return room;
            } else {
                IWorldRegion worldRegion = square.getIsoWorldRegion();
                return worldRegion != null && worldRegion.isPlayerRoom() ? worldRegion : null;
            }
        }
    }

    private float getRoomSize(Object room) {
        return room instanceof IsoRoom isoRoom ? isoRoom.getRoomDef().getArea() : ((IWorldRegion)room).getSquareSize();
    }
}
