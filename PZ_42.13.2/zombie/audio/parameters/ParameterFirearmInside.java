// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.audio.parameters;

import zombie.audio.FMODLocalParameter;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.isoregion.regions.IWorldRegion;

public final class ParameterFirearmInside extends FMODLocalParameter {
    private static final float VALUE_DIFFERENT_BUILDING = -1.0F;
    private static final float VALUE_OUTSIDE = 0.0F;
    private static final float VALUE_SAME_BUILDING = 1.0F;
    private final IsoPlayer character;

    public ParameterFirearmInside(IsoPlayer character) {
        super("FirearmInside");
        this.character = character;
    }

    @Override
    public float calculateCurrentValue() {
        Object roomShooter = this.getRoom(this.character);
        if (roomShooter == null) {
            return 0.0F;
        } else {
            IsoPlayer listener = this.getClosestListener(this.character.getX(), this.character.getY(), this.character.getZ());
            if (listener == null) {
                return -1.0F;
            } else if (listener == this.character) {
                return 1.0F;
            } else {
                Object roomListener = this.getRoom(listener);
                return roomShooter == roomListener ? 1.0F : -1.0F;
            }
        }
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

    private IsoPlayer getClosestListener(float soundX, float soundY, float soundZ) {
        if (IsoPlayer.numPlayers == 1) {
            return IsoPlayer.players[0];
        } else {
            float minDist = Float.MAX_VALUE;
            IsoPlayer closest = null;

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer chr = IsoPlayer.players[i];
                if (chr != null) {
                    if (chr == this.character) {
                        return this.character;
                    }

                    if (chr.getCurrentSquare() != null) {
                        float px = chr.getX();
                        float py = chr.getY();
                        float pz = chr.getZ();
                        float distSq = IsoUtils.DistanceToSquared(px, py, pz * 3.0F, soundX, soundY, soundZ * 3.0F);
                        distSq *= PZMath.pow(chr.getHearDistanceModifier(), 2.0F);
                        if (distSq < minDist) {
                            minDist = distSq;
                            closest = chr;
                        }
                    }
                }
            }

            return closest;
        }
    }
}
