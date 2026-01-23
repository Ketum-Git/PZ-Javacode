// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.savefile;

import java.io.File;
import zombie.ZomboidFileSystem;

public final class SavefileNaming {
    public static final String SUBDIR_APOP = "apop";
    public static final String SUBDIR_CHUNKDATA = "chunkdata";
    public static final String SUBDIR_MAP = "map";
    public static final String SUBDIR_ZPOP = "zpop";
    public static final String SUBDIR_METAGRID = "metagrid";

    public static void ensureSubdirectoriesExist(String savefileDir) {
        File file = new File(savefileDir);
        ZomboidFileSystem.ensureFolderExists(new File(file, "apop"));
        ZomboidFileSystem.ensureFolderExists(new File(file, "chunkdata"));
        ZomboidFileSystem.ensureFolderExists(new File(file, "map"));
        ZomboidFileSystem.ensureFolderExists(new File(file, "zpop"));
        ZomboidFileSystem.ensureFolderExists(new File(file, "metagrid"));
    }
}
