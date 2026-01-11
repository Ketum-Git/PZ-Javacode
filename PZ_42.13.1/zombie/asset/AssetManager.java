// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import gnu.trove.map.hash.THashMap;
import java.util.ArrayList;
import zombie.debug.DebugLog;
import zombie.fileSystem.IFile;

public abstract class AssetManager implements AssetStateObserver {
    private final AssetManager.AssetTable assets = new AssetManager.AssetTable();
    private AssetManagers owner;
    private boolean isUnloadEnabled;

    public void create(AssetType type, AssetManagers owner) {
        owner.add(type, this);
        this.owner = owner;
    }

    public void destroy() {
        this.assets.forEachValue(asset -> {
            if (!asset.isEmpty()) {
                DebugLog.Asset.println("Leaking asset " + asset.getPath());
            }

            this.destroyAsset(asset);
            return true;
        });
    }

    public void removeUnreferenced() {
        if (this.isUnloadEnabled) {
            ArrayList<Asset> toRemove = new ArrayList<>();
            this.assets.forEachValue(assetx -> {
                if (assetx.getRefCount() == 0) {
                    toRemove.add(assetx);
                }

                return true;
            });

            for (Asset asset : toRemove) {
                this.assets.remove(asset.getPath());
                this.destroyAsset(asset);
            }
        }
    }

    public Asset load(AssetPath path) {
        return this.load(path, null);
    }

    public Asset load(AssetPath path, AssetManager.AssetParams params) {
        if (!path.isValid()) {
            return null;
        } else {
            Asset asset = this.get(path);
            if (asset == null) {
                asset = this.createAsset(path, params);
                this.assets.put(path.getPath(), asset);
            }

            if (asset.isEmpty() && asset.priv.desiredState == Asset.State.EMPTY) {
                this.doLoad(asset, params);
            }

            asset.addRef();
            return asset;
        }
    }

    public void load(Asset asset) {
        if (asset.isEmpty() && asset.priv.desiredState == Asset.State.EMPTY) {
            this.doLoad(asset, null);
        }

        asset.addRef();
    }

    public void unload(AssetPath path) {
        Asset asset = this.get(path);
        if (asset != null) {
            this.unload(asset);
        }
    }

    public void unload(Asset asset) {
        int newRefCount = asset.rmRef();

        assert newRefCount >= 0;

        if (newRefCount == 0 && this.isUnloadEnabled) {
            this.doUnload(asset);
        }
    }

    public void unloadWithoutDeref(Asset asset) {
        this.doUnload(asset);
    }

    public void onDataReloaded(Asset asset) {
        asset.priv.desiredState = Asset.State.READY;
        this.onLoadingSucceeded(asset);
    }

    public void reload(AssetPath path) {
        Asset asset = this.get(path);
        if (asset != null) {
            this.reload(asset);
        }
    }

    public void reload(Asset asset) {
        this.reload(asset, null);
    }

    public void reload(Asset asset, AssetManager.AssetParams params) {
        this.doUnload(asset);
        this.doLoad(asset, params);
    }

    public void enableUnload(boolean enable) {
        this.isUnloadEnabled = enable;
        if (enable) {
            this.assets.forEachValue(asset -> {
                if (asset.getRefCount() == 0) {
                    this.doUnload(asset);
                }

                return true;
            });
        }
    }

    private void doLoad(Asset asset, AssetManager.AssetParams params) {
        if (asset.priv.desiredState != Asset.State.READY) {
            asset.priv.desiredState = Asset.State.READY;
            asset.setAssetParams(params);
            this.startLoading(asset);
        }
    }

    private void doUnload(Asset asset) {
        if (asset.priv.task != null) {
            asset.priv.task.cancel();
            asset.priv.task = null;
        }

        asset.priv.desiredState = Asset.State.EMPTY;
        this.unloadData(asset);

        assert asset.priv.emptyDepCount <= 1;

        asset.priv.emptyDepCount = 1;
        asset.priv.failedDepCount = 0;
        asset.priv.checkState();
    }

    @Override
    public void onStateChanged(Asset.State old_state, Asset.State new_state, Asset asset) {
    }

    protected void startLoading(Asset asset) {
        if (asset.priv.task == null) {
            asset.priv.task = new AssetTask_LoadFromFileAsync(asset, false);
            asset.priv.task.execute();
        }
    }

    protected final void onLoadingSucceeded(Asset asset) {
        asset.priv.onLoadingSucceeded();
    }

    protected final void onLoadingFailed(Asset asset) {
        asset.priv.onLoadingFailed();
    }

    protected final void setTask(Asset asset, AssetTask task) {
        if (asset.priv.task != null) {
            if (task == null) {
                asset.priv.task = null;
            }
        } else {
            asset.priv.task = task;
        }
    }

    protected boolean loadDataFromFile(Asset asset, IFile file) {
        throw new RuntimeException("not implemented");
    }

    protected void unloadData(Asset asset) {
    }

    public AssetManager.AssetTable getAssetTable() {
        return this.assets;
    }

    public AssetManagers getOwner() {
        return this.owner;
    }

    protected abstract Asset createAsset(AssetPath arg0, AssetManager.AssetParams arg1);

    protected abstract void destroyAsset(Asset arg0);

    protected Asset get(AssetPath path) {
        return this.assets.get(path.getPath());
    }

    public static class AssetParams {
    }

    public static final class AssetTable extends THashMap<String, Asset> {
    }
}
