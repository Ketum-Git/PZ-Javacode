// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODGlobalParameter;
import zombie.audio.FMODParameterUtils;
import zombie.characters.IsoGameCharacter;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;

public final class ParameterRoomSize extends FMODGlobalParameter {
    public ParameterRoomSize() {
        super("RoomSize");
    }

    @Override
    public float calculateCurrentValue() {
        IsoGameCharacter character = FMODParameterUtils.getFirstListener();
        if (character == null) {
            return 0.0F;
        } else {
            RoomDef roomDef = character.getCurrentRoomDef();
            if (roomDef != null) {
                return roomDef.getArea();
            } else {
                IsoGridSquare gs = character.getCurrentSquare();
                return gs != null && gs.isInARoom() ? gs.getRoomSize() : 0.0F;
            }
        }
    }
}
