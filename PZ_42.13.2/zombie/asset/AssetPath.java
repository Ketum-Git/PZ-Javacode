// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import zombie.util.StringUtils;

public final class AssetPath {
    protected String path;

    public AssetPath(String path) {
        this.path = path;
    }

    public boolean isValid() {
        return !StringUtils.isNullOrEmpty(this.path);
    }

    public int getHash() {
        return this.path.hashCode();
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{ \"" + this.getPath() + "\" }";
    }
}
