// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.bucket;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import zombie.DebugFileWatcher;
import zombie.PredicatedFileWatcher;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.textures.Texture;

public final class Bucket {
    private String name;
    private final HashMap<Path, Texture> textures = new HashMap<>();
    private static final FileSystem m_fs = FileSystems.getDefault();
    private final PredicatedFileWatcher fileWatcher = new PredicatedFileWatcher(key -> this.HasTexture(key), path -> {
        Texture tex = this.getTexture(path);
        tex.reloadFromFile(path);
        ModelManager.instance.reloadAllOutfits();
    });

    public Bucket() {
        DebugFileWatcher.instance.add(this.fileWatcher);
    }

    public void AddTexture(Path filename, Texture texture) {
        if (texture != null) {
            this.textures.put(filename, texture);
        }
    }

    public void AddTexture(String filename, Texture texture) {
        if (texture != null) {
            this.AddTexture(m_fs.getPath(filename), texture);
        }
    }

    public void Dispose() {
        for (Texture tex : this.textures.values()) {
            tex.destroy();
        }

        this.textures.clear();
    }

    public Texture getTexture(Path filename) {
        return this.textures.get(filename);
    }

    public Texture getTexture(String filename) {
        return this.getTexture(m_fs.getPath(filename));
    }

    public boolean HasTexture(Path filename) {
        return this.textures.containsKey(filename);
    }

    public boolean HasTexture(String filename) {
        return this.HasTexture(m_fs.getPath(filename));
    }

    String getName() {
        return this.name;
    }

    void setName(String name) {
        this.name = name;
    }

    public void forgetTexture(String name) {
        this.textures.remove(name);
    }
}
