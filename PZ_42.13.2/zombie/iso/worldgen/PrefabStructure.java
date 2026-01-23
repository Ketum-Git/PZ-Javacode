// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen;

import java.util.List;
import java.util.Map;

public class PrefabStructure {
    private final int[] dimensions;
    private final List<String> tiles;
    private final Map<String, int[][]> schematic;
    private final float zombies;
    private final List<String> categories = List.of("Floor", "FloorFurniture", "FloorOverlay", "Furniture");

    public PrefabStructure(int[] dimensions, List<String> tiles, Map<String, int[][]> schematic, float zombies) {
        this.dimensions = dimensions;
        this.tiles = tiles;
        this.schematic = schematic;
        this.zombies = zombies;
    }

    public int getX() {
        return this.dimensions[0];
    }

    public int getY() {
        return this.dimensions[1];
    }

    public List<String> getCategories() {
        return this.categories;
    }

    public boolean hasCategory(String category) {
        return this.schematic.containsKey(category);
    }

    public int getTileRef(String category, int x, int y) {
        return this.schematic.get(category)[y][x];
    }

    public String getTile(int index) {
        return this.tiles.get(index);
    }

    public float getZombies() {
        return this.zombies;
    }

    @Override
    public String toString() {
        return String.format(
            "<PrefabStructure@%s | [%s %s] | %s tiles>", Integer.toHexString(this.hashCode()), this.dimensions[0], this.dimensions[1], this.tiles.size()
        );
    }
}
