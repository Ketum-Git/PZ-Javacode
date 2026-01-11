// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.zombie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import se.krka.kahlua.vm.KahluaTable;
import zombie.SandboxOptions;
import zombie.Lua.LuaManager;
import zombie.iso.worldgen.utils.CellCoord;

public class ZombieVoronoi {
    private final long seed;
    private final int numberPoints;
    private final ClosestSelection closestPoint;
    private final double scale;
    private final double cutoff;
    public final Map<CellCoord, double[]> cellCache = new HashMap<>();

    public static List<ZombieVoronoi> getVoronois(long seed) {
        ZombieVoronoiReader reader = new ZombieVoronoiReader();
        KahluaTable voronoi = (KahluaTable)LuaManager.env.rawget("zombie_voronoi");
        List<ZombieVoronoiEntry> entries = reader.getEntries(voronoi);
        List<ZombieVoronoi> voronois = new ArrayList<>();

        for (int i = 0; i < entries.size(); i++) {
            voronois.add(new ZombieVoronoi(seed + i, entries.get(i)));
        }

        return voronois;
    }

    private ZombieVoronoi(long seed, ZombieVoronoiEntry entry) {
        this(seed, entry.numberPoints(), entry.closestPoint(), entry.scale(), entry.cutoff());
    }

    public ZombieVoronoi(long seed, int numberPoints, ClosestSelection closestPoint, double scale, double cutoff) {
        this.seed = seed;
        this.numberPoints = numberPoints;
        this.closestPoint = closestPoint;
        this.scale = scale;
        this.cutoff = cutoff;
    }

    private double evaluateNoise(double x, double y, Random rng, List<Coord> points) {
        points.clear();
        int iX = (int)Math.floor(x);
        int iY = (int)Math.floor(y);

        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            int secX = iX + xOffset;

            for (int yOffset = -1; yOffset <= 1; yOffset++) {
                int secY = iY + yOffset;
                int hash = HashUtil.hash2D(this.seed, secX, secY);
                rng.setSeed(hash);

                for (int i = 0; i < this.numberPoints; i++) {
                    points.add(new Coord(rng.nextDouble() + secX, rng.nextDouble() + secY));
                }
            }
        }

        return this.closestPoint.getClosest(x, y, points);
    }

    public double[] evaluateCellNoise(int wx, int wy) {
        synchronized (this.cellCache) {
            if (this.cellCache.containsKey(new CellCoord(wx, wy))) {
                return this.cellCache.get(new CellCoord(wx, wy));
            }
        }

        double[] chunkNoise = new double[1024];
        List<Coord> points = new ArrayList<>();
        Random rng = new Random();

        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                chunkNoise[x + y * 32] = this.evaluateNoise((x + wx * 32) / this.scale, (y + wy * 32) / this.scale, rng, points);
            }
        }

        synchronized (this.cellCache) {
            this.cellCache.put(new CellCoord(wx, wy), chunkNoise);
            return chunkNoise;
        }
    }

    public double[] evaluateCellCutoff(int wx, int wy) {
        double cutoff1 = SandboxOptions.instance.zombieVoronoiNoise.getValue() ? this.cutoff : 0.0;
        return this.evaluateCellCutoff(wx, wy, cutoff1);
    }

    public double[] evaluateCellCutoff(int wx, int wy, double cutoff) {
        double[] noise = this.evaluateCellNoise(wx, wy);
        return Arrays.stream(noise).map(x -> x < cutoff ? 0.0 : 1.0).toArray();
    }

    public void releaseCell(int cellX, int cellY) {
        synchronized (this.cellCache) {
            double[] var4 = this.cellCache.remove(new CellCoord(cellX, cellY));
        }
    }
}
