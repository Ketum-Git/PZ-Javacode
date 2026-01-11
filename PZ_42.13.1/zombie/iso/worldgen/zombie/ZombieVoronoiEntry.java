// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.zombie;

public record ZombieVoronoiEntry(int numberPoints, ClosestSelection closestPoint, double scale, double cutoff) {
    public ZombieVoronoiEntry(int numberPoints, String closestPoint, double scale, double cutoff) {
        this(numberPoints, ClosestSelectionType.valueOf(closestPoint), scale, cutoff);
    }
}
