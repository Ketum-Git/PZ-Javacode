// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import gnu.trove.map.hash.TLongObjectHashMap;
import zombie.fileSystem.FileSystem;

public final class AssetManagers {
    private final AssetManagers.AssetManagerTable managers = new AssetManagers.AssetManagerTable();
    private final FileSystem fileSystem;

    public AssetManagers(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public AssetManager get(AssetType type) {
        return this.managers.get(type.type);
    }

    public void add(AssetType type, AssetManager rm) {
        this.managers.put(type.type, rm);
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public static final class AssetManagerTable extends TLongObjectHashMap<AssetManager> {
    }
}
