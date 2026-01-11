// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.network.ServerMap;

public class ImportantAreaManager {
    private static final ImportantAreaManager instance = new ImportantAreaManager();
    public static final int importantAreasMaximum = 100;
    public static final int importantAreasTimeout = 10000;
    public static final LinkedList<ImportantArea> ImportantAreas = new LinkedList<>();
    public static final LinkedList<ImportantArea> ImportantAreasForDelete = new LinkedList<>();

    public static ImportantAreaManager getInstance() {
        return instance;
    }

    public final void load(ByteBuffer input, int WorldVersion) throws IOException {
        ImportantAreas.clear();
        int size = input.getInt();

        for (int i = 0; i < size; i++) {
            ImportantArea area = new ImportantArea(0, 0);
            area.load(input, WorldVersion);
            ImportantAreas.add(area);
        }
    }

    public final void save(ByteBuffer output) throws IOException {
        output.putInt(ImportantAreas.size());

        for (ImportantArea area : ImportantAreas) {
            area.save(output);
        }
    }

    public void saveDataFile() {
        if (!GameClient.client) {
            File outFile = ZomboidFileSystem.instance.getFileInCurrentSave("important_area_data.bin");

            try {
                try (
                    FileOutputStream fos = new FileOutputStream(outFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                ) {
                    synchronized (SliceY.SliceBufferLock) {
                        SliceY.SliceBuffer.clear();
                        SliceY.SliceBuffer.putInt(240);
                        this.save(SliceY.SliceBuffer);
                        bos.write(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                    }
                }
            } catch (IOException var11) {
                throw new RuntimeException(var11);
            }
        }
    }

    public ImportantArea updateOrAdd(int x, int y) {
        int sx = PZMath.coorddivision(x, 64);
        int sy = PZMath.coorddivision(y, 64);

        for (ImportantArea area : ImportantAreas) {
            if (area.sx == sx && area.sy == sy) {
                area.lastUpdate = System.currentTimeMillis();
                return area;
            }
        }

        if (ImportantAreas.size() >= 100) {
            DebugLog.Multiplayer.warn("ImportantAreas size is too big. Random map area will unload.");
            ImportantAreas.remove(Rand.Next(0, ImportantAreas.size()));
            return null;
        } else {
            ImportantArea areax = new ImportantArea(sx, sy);
            ImportantAreas.add(areax);
            return areax;
        }
    }

    public void process() {
        for (ImportantArea area : ImportantAreas) {
            if (System.currentTimeMillis() - area.lastUpdate > 10000L) {
                ImportantAreasForDelete.add(area);
            } else {
                ServerMap.instance.importantAreaIn(area.sx, area.sy);
            }
        }

        ImportantAreas.removeAll(ImportantAreasForDelete);
    }
}
