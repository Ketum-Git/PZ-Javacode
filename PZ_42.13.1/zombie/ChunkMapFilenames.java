// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;

public final class ChunkMapFilenames {
    public static ChunkMapFilenames instance = new ChunkMapFilenames();
    public final ConcurrentHashMap<Long, Object> map = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<Long, Object> headerMap = new ConcurrentHashMap<>();
    private File dirFile;
    private String cacheDir;
    private final HashSet<Integer> wxFolders = new HashSet<>();

    public ChunkMapFilenames() {
        this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        File[] directories = ZomboidFileSystem.listAllDirectories(
            this.cacheDir + File.separator + Core.gameSaveWorld + File.separator + "map", file -> true, false
        );

        for (File dir : directories) {
            try {
                this.wxFolders.add(Integer.valueOf(dir.getName()));
            } catch (Exception var7) {
            }
        }
    }

    public void clear() {
        this.dirFile = null;
        this.cacheDir = null;
        this.map.clear();
        this.headerMap.clear();
        this.wxFolders.clear();
    }

    public File getFilename(int wx, int wy) {
        long key = (long)wx << 32 | wy & 4294967295L;
        if (this.map.containsKey(key)) {
            return (File)this.map.get(key);
        } else {
            if (this.cacheDir == null) {
                this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
            }

            if (this.wxFolders.add(wx)) {
                try {
                    Files.createDirectories(Path.of(this.cacheDir + File.separator + Core.gameSaveWorld + File.separator + "map" + File.separator + wx));
                } catch (IOException var7) {
                    DebugLog.General.printException(var7, "", LogSeverity.Error);
                }
            }

            String filename = this.cacheDir + File.separator + Core.gameSaveWorld + File.separator + "map" + File.separator + wx + File.separator + wy + ".bin";
            File f = new File(filename);
            this.map.put(key, f);
            return f;
        }
    }

    public File getDir(String gameSaveWorld) {
        if (this.cacheDir == null) {
            this.cacheDir = ZomboidFileSystem.instance.getGameModeCacheDir();
        }

        if (this.dirFile == null) {
            this.dirFile = new File(this.cacheDir, "map" + File.separator + gameSaveWorld);
        }

        return this.dirFile;
    }

    public String getHeader(int wX, int wY) {
        long key = (long)wX << 32 | wY & 4294967295L;
        if (this.headerMap.containsKey(key)) {
            return this.headerMap.get(key).toString();
        } else {
            String filename = wX + "_" + wY + ".lotheader";
            this.headerMap.put(key, filename);
            return filename;
        }
    }
}
