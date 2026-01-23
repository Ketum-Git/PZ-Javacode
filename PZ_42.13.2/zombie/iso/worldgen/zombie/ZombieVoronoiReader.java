// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.zombie;

import java.util.ArrayList;
import java.util.List;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;

public class ZombieVoronoiReader {
    public List<ZombieVoronoiEntry> getEntries(KahluaTable mainTable) {
        List<ZombieVoronoiEntry> zombieVoronoiEntries = new ArrayList<>();
        KahluaTableIterator iterZombieVoronoi = mainTable.iterator();

        while (iterZombieVoronoi.advance()) {
            KahluaTable table = (KahluaTable)iterZombieVoronoi.getValue();
            int points = this.loadInteger(table.rawget("points"), 1);
            String closest = this.loadString(table.rawget("closest"), "SECOND_MINUS_FIRST");
            double scale = this.loadDouble(table.rawget("scale"), 16.0);
            double cutoff = this.loadDouble(table.rawget("cutoff"), 0.2);
            zombieVoronoiEntries.add(new ZombieVoronoiEntry(points, closest, scale, cutoff));
        }

        return zombieVoronoiEntries;
    }

    private int loadInteger(Object object, int default_) {
        return object == null ? default_ : ((Double)object).intValue();
    }

    private Double loadDouble(Object object, Double default_) {
        return object == null ? default_ : (Double)object;
    }

    private String loadString(Object object, String default_) {
        return object == null ? default_ : (String)object;
    }
}
