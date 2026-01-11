// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;
import zombie.iso.IsoWorld;

public final class AnimalZoneJunction {
    public final AnimalZone zoneSelf;
    public final int pointIndexSelf;
    public final AnimalZone zoneOther;
    public final int pointIndexOther;
    public final float distanceFromStart;

    public AnimalZoneJunction(AnimalZone zoneSelf, int pointIndexSelf, AnimalZone zoneOther, int pointIndexOther) {
        this.zoneSelf = zoneSelf;
        this.pointIndexSelf = pointIndexSelf;
        this.zoneOther = zoneOther;
        this.pointIndexOther = pointIndexOther;
        this.distanceFromStart = zoneSelf.getDistanceOfPointFromStart(pointIndexSelf);
    }

    public void save(ByteBuffer output) {
        output.putInt(this.pointIndexSelf);
        output.putInt(this.pointIndexOther);
        output.putLong(this.zoneSelf.id.getMostSignificantBits());
        output.putLong(this.zoneSelf.id.getLeastSignificantBits());
        output.putLong(this.zoneOther.id.getMostSignificantBits());
        output.putLong(this.zoneOther.id.getLeastSignificantBits());
    }

    public static AnimalZoneJunction load(ByteBuffer input, int WorldVersion) {
        int pointIndexSelf = input.getInt();
        int pointIndexOther = input.getInt();
        UUID zoneSelfID = new UUID(input.getLong(), input.getLong());
        UUID zoneOtherID = new UUID(input.getLong(), input.getLong());
        AnimalZone zoneSelf = IsoWorld.instance.metaGrid.animalZoneHandler.getZone(zoneSelfID);
        AnimalZone zoneOther = IsoWorld.instance.metaGrid.animalZoneHandler.getZone(zoneOtherID);
        return new AnimalZoneJunction(zoneSelf, pointIndexSelf, zoneOther, pointIndexOther);
    }

    public int getX() {
        return this.zoneSelf.points.get(this.pointIndexSelf * 2);
    }

    public int getY() {
        return this.zoneSelf.points.get(this.pointIndexSelf * 2 + 1);
    }

    public void getJunctionsAtSamePoint(ArrayList<AnimalZoneJunction> junctions) {
        junctions.clear();

        for (int i = 0; i < this.zoneSelf.junctions.size(); i++) {
            AnimalZoneJunction junction = this.zoneSelf.junctions.get(i);
            if (junction.pointIndexSelf == this.pointIndexSelf) {
                junctions.add(junction);
            }
        }
    }

    public boolean isFirstPointOnZone1() {
        return this.pointIndexSelf == 0;
    }

    public boolean isLastPointOnZone1() {
        return this.pointIndexSelf == this.zoneSelf.points.size() / 2 - 1;
    }

    public boolean isFirstPointOnZone2() {
        return this.pointIndexOther == 0;
    }

    public boolean isLastPointOnZone2() {
        return this.pointIndexOther == this.zoneOther.points.size() / 2 - 1;
    }
}
