// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.areas;

public final class BuildingScore {
    public float weapons;
    public float food;
    public float wood;
    public float defense;
    public IsoBuilding building;
    public int size;
    public int safety;

    public BuildingScore(IsoBuilding b) {
        this.building = b;
    }
}
