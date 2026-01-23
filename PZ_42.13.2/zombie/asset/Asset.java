// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.asset;

import java.util.ArrayList;
import java.util.Objects;

public abstract class Asset {
    protected final AssetManager assetManager;
    private final AssetPath path;
    private int refCount;
    final Asset.PRIVATE priv = new Asset.PRIVATE();
    protected boolean isDefered;

    protected Asset(AssetPath path, AssetManager manager) {
        this.refCount = 0;
        this.path = path;
        this.assetManager = manager;
    }

    public abstract AssetType getType();

    public Asset.State getState() {
        return this.priv.currentState;
    }

    public boolean isEmpty() {
        return this.priv.currentState == Asset.State.EMPTY;
    }

    public boolean isReady() {
        return this.priv.currentState == Asset.State.READY || this.isDefered;
    }

    public boolean isFailure() {
        return this.priv.currentState == Asset.State.FAILURE;
    }

    public void onCreated(Asset.State state) {
        this.priv.onCreated(state);
    }

    public int getRefCount() {
        return this.refCount;
    }

    public Asset.ObserverCallback getObserverCb() {
        if (this.priv.cb == null) {
            this.priv.cb = new Asset.ObserverCallback();
        }

        return this.priv.cb;
    }

    public AssetPath getPath() {
        return this.path;
    }

    public AssetManager getAssetManager() {
        return this.assetManager;
    }

    protected void onBeforeReady() {
    }

    protected void onBeforeEmpty() {
    }

    public void addDependency(Asset dependent_asset) {
        this.priv.addDependency(dependent_asset);
    }

    public void removeDependency(Asset dependent_asset) {
        this.priv.removeDependency(dependent_asset);
    }

    int addRef() {
        return ++this.refCount;
    }

    int rmRef() {
        return --this.refCount;
    }

    public void setAssetParams(AssetManager.AssetParams params) {
    }

    public static final class ObserverCallback extends ArrayList<AssetStateObserver> {
        public void invoke(Asset.State oldState, Asset.State newState, Asset asset) {
            int n = this.size();

            for (int i = 0; i < n; i++) {
                this.get(i).onStateChanged(oldState, newState, asset);
            }
        }
    }

    final class PRIVATE implements AssetStateObserver {
        Asset.State currentState;
        Asset.State desiredState;
        int emptyDepCount;
        int failedDepCount;
        Asset.ObserverCallback cb;
        AssetTask task;

        PRIVATE() {
            Objects.requireNonNull(Asset.this);
            super();
            this.currentState = Asset.State.EMPTY;
            this.desiredState = Asset.State.EMPTY;
            this.emptyDepCount = 1;
        }

        void onCreated(Asset.State state) {
            assert this.emptyDepCount == 1;

            assert this.failedDepCount == 0;

            this.currentState = state;
            this.desiredState = Asset.State.READY;
            this.failedDepCount = state == Asset.State.FAILURE ? 1 : 0;
            this.emptyDepCount = 0;
        }

        void addDependency(Asset dependent_asset) {
            assert this.desiredState != Asset.State.EMPTY;

            dependent_asset.getObserverCb().add(this);
            if (dependent_asset.isEmpty()) {
                this.emptyDepCount++;
            }

            if (dependent_asset.isFailure()) {
                this.failedDepCount++;
            }

            this.checkState();
        }

        void removeDependency(Asset dependent_asset) {
            dependent_asset.getObserverCb().remove(this);
            if (dependent_asset.isEmpty()) {
                assert this.emptyDepCount > 0;

                this.emptyDepCount--;
            }

            if (dependent_asset.isFailure()) {
                assert this.failedDepCount > 0;

                this.failedDepCount--;
            }

            this.checkState();
        }

        @Override
        public void onStateChanged(Asset.State old_state, Asset.State new_state, Asset asset) {
            assert old_state != new_state;

            assert this.currentState != Asset.State.EMPTY || this.desiredState != Asset.State.EMPTY;

            if (old_state == Asset.State.EMPTY) {
                assert this.emptyDepCount > 0;

                this.emptyDepCount--;
            }

            if (old_state == Asset.State.FAILURE) {
                assert this.failedDepCount > 0;

                this.failedDepCount--;
            }

            if (new_state == Asset.State.EMPTY) {
                this.emptyDepCount++;
            }

            if (new_state == Asset.State.FAILURE) {
                this.failedDepCount++;
            }

            this.checkState();
        }

        void onLoadingSucceeded() {
            assert this.currentState != Asset.State.READY;

            assert this.emptyDepCount == 1;

            this.emptyDepCount--;
            this.task = null;
            this.checkState();
        }

        void onLoadingFailed() {
            assert this.currentState != Asset.State.READY;

            assert this.emptyDepCount == 1;

            this.failedDepCount++;
            this.emptyDepCount--;
            this.task = null;
            this.checkState();
        }

        void checkState() {
            Asset.State oldState = this.currentState;
            if (this.failedDepCount > 0 && this.currentState != Asset.State.FAILURE) {
                this.currentState = Asset.State.FAILURE;
                Asset.this.getAssetManager().onStateChanged(oldState, this.currentState, Asset.this);
                if (this.cb != null) {
                    this.cb.invoke(oldState, this.currentState, Asset.this);
                }
            }

            if (this.failedDepCount == 0) {
                if (this.emptyDepCount == 0 && this.currentState != Asset.State.READY && this.desiredState != Asset.State.EMPTY) {
                    Asset.this.onBeforeReady();
                    this.currentState = Asset.State.READY;
                    Asset.this.getAssetManager().onStateChanged(oldState, this.currentState, Asset.this);
                    if (this.cb != null) {
                        this.cb.invoke(oldState, this.currentState, Asset.this);
                    }
                }

                if (this.emptyDepCount > 0 && this.currentState != Asset.State.EMPTY) {
                    Asset.this.onBeforeEmpty();
                    this.currentState = Asset.State.EMPTY;
                    Asset.this.getAssetManager().onStateChanged(oldState, this.currentState, Asset.this);
                    if (this.cb != null) {
                        this.cb.invoke(oldState, this.currentState, Asset.this);
                    }
                }
            }
        }
    }

    public static enum State {
        EMPTY,
        READY,
        FAILURE;
    }
}
