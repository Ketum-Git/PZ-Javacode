// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory.types;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.logger.ExceptionLogger;
import zombie.inventory.InventoryItem;
import zombie.iso.SaveBufferMap;
import zombie.iso.SliceY;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.util.ByteBufferPooledObject;
import zombie.worldMap.symbols.WorldMapSymbols;

@UsedFromLua
public class MapItem extends InventoryItem {
    public static MapItem worldMapInstance;
    private static final byte[] FILE_MAGIC = new byte[]{87, 77, 83, 89};
    private String mapId;
    private final WorldMapSymbols symbols = new WorldMapSymbols();
    private boolean defaultAnnotationsLoaded;

    public static MapItem getSingleton() {
        if (worldMapInstance == null) {
            Item scriptItem = ScriptManager.instance.FindItem("Base.Map");
            if (scriptItem == null) {
                return null;
            }

            worldMapInstance = new MapItem("Base", "World Map", "WorldMap", scriptItem);
        }

        return worldMapInstance;
    }

    public static void SaveWorldMap() {
        if (worldMapInstance != null) {
            try {
                ByteBuffer out = SliceY.SliceBuffer;
                out.clear();
                out.put(FILE_MAGIC);
                out.putInt(241);
                worldMapInstance.getSymbols().save(out);
                File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_symbols.bin"));

                try (
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                ) {
                    bos.write(out.array(), 0, out.position());
                }
            } catch (Exception var10) {
                ExceptionLogger.logException(var10);
            }
        }
    }

    public static void SaveWorldMapToBufferMap(SaveBufferMap bufferMap) {
        if (worldMapInstance != null) {
            synchronized (SliceY.SliceBufferLock) {
                try {
                    SliceY.SliceBuffer.clear();
                    SliceY.SliceBuffer.put(FILE_MAGIC);
                    SliceY.SliceBuffer.putInt(241);
                    worldMapInstance.getSymbols().save(SliceY.SliceBuffer);
                    String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("map_symbols.bin");
                    ByteBufferPooledObject buffer = bufferMap.allocate(SliceY.SliceBuffer.position());
                    buffer.put(SliceY.SliceBuffer.array(), 0, SliceY.SliceBuffer.position());
                    bufferMap.put(fileName, buffer);
                } catch (Exception var5) {
                    ExceptionLogger.logException(var5);
                }
            }
        }
    }

    public static void LoadWorldMap() {
        if (getSingleton() != null) {
            File file = new File(ZomboidFileSystem.instance.getFileNameInCurrentSave("map_symbols.bin"));

            try (
                FileInputStream fis = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(fis);
            ) {
                ByteBuffer in = SliceY.SliceBuffer;
                in.clear();
                int numBytes = bis.read(in.array());
                in.limit(numBytes);
                byte[] magic = new byte[4];
                in.get(magic);
                if (!Arrays.equals(magic, FILE_MAGIC)) {
                    throw new IOException(file.getAbsolutePath() + " does not appear to be map_symbols.bin");
                }

                int WorldVersion = in.getInt();
                getSingleton().getSymbols().load(in, WorldVersion);
            } catch (FileNotFoundException var11) {
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
            }
        }
    }

    public static void Reset() {
        if (worldMapInstance != null) {
            worldMapInstance.getSymbols().clear();
            worldMapInstance = null;
        }
    }

    public MapItem(String module, String name, String type, String tex) {
        super(module, name, type, tex);
    }

    public MapItem(String module, String name, String type, Item item) {
        super(module, name, type, item);
    }

    @Override
    public boolean IsMap() {
        return true;
    }

    public void setMapID(String mapID) {
        this.mapId = mapID;
    }

    public String getMapID() {
        return this.mapId;
    }

    public WorldMapSymbols getSymbols() {
        return this.symbols;
    }

    @Override
    public void save(ByteBuffer output, boolean net) throws IOException {
        super.save(output, net);
        GameWindow.WriteString(output, this.mapId);
        this.symbols.save(output);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        super.load(input, WorldVersion);
        this.mapId = GameWindow.ReadString(input);
        this.symbols.load(input, WorldVersion);
    }

    public boolean checkDefaultAnnotationsLoaded() {
        return this.defaultAnnotationsLoaded ? false : (this.defaultAnnotationsLoaded = true);
    }

    public void clearDefaultAnnotations() {
        this.defaultAnnotationsLoaded = false;
        this.symbols.clearDefaultAnnotations();
    }

    public String getMediaId() {
        return this.getStashMap() != null ? this.getStashMap() : this.getMapID();
    }
}
