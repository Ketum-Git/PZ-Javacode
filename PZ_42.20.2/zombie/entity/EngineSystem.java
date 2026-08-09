// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

public abstract class EngineSystem {
    private final int updatePriority;
    private final int renderLastPriority;
    private boolean updater;
    private boolean simulationUpdater;
    private boolean renderer;
    private boolean enabled = true;
    private Engine engine;
    EngineSystem.MembershipListener membershipListener;

    public EngineSystem() {
        this(false, false, Integer.MAX_VALUE, false, Integer.MAX_VALUE);
    }

    public EngineSystem(boolean updater, boolean simulationUpdater, int updatePriority) {
        this(updater, simulationUpdater, updatePriority, false, Integer.MAX_VALUE);
    }

    public EngineSystem(boolean updater, boolean simulationUpdater, int updatePriority, boolean renderer, int renderLastPriority) {
        this.updater = updater;
        this.simulationUpdater = simulationUpdater;
        this.updatePriority = updatePriority;
        this.renderer = renderer;
        this.renderLastPriority = renderLastPriority;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    public final void setEnabled(boolean b) {
        if (this.enabled != b) {
            this.enabled = b;
            if (this.membershipListener != null) {
                this.membershipListener.onMembershipPropertyChanged(this);
            }
        }
    }

    public final void setUpdater(boolean b) {
        if (this.updater != b) {
            this.updater = b;
            if (this.membershipListener != null) {
                this.membershipListener.onMembershipPropertyChanged(this);
            }
        }
    }

    public final void setSimulationUpdater(boolean b) {
        if (this.simulationUpdater != b) {
            this.simulationUpdater = b;
            if (this.membershipListener != null) {
                this.membershipListener.onMembershipPropertyChanged(this);
            }
        }
    }

    public final void setRenderer(boolean b) {
        if (this.renderer != b) {
            this.renderer = b;
            if (this.membershipListener != null) {
                this.membershipListener.onMembershipPropertyChanged(this);
            }
        }
    }

    public final Engine getEngine() {
        return this.engine;
    }

    final void addedToEngineInternal(Engine engine) {
        this.engine = engine;
        this.addedToEngine(engine);
    }

    final void removedFromEngineInternal(Engine engine) {
        this.engine = null;
        this.removedFromEngine(engine);
    }

    public void addedToEngine(Engine engine) {
    }

    public void removedFromEngine(Engine engine) {
    }

    public final int getUpdatePriority() {
        return this.updatePriority;
    }

    public final boolean isUpdater() {
        return this.updater;
    }

    public void update() {
    }

    public final int getUpdateSimulationPriority() {
        return this.updatePriority;
    }

    public final boolean isSimulationUpdater() {
        return this.simulationUpdater;
    }

    public void updateSimulation() {
    }

    public final int getRenderLastPriority() {
        return this.renderLastPriority;
    }

    public final boolean isRenderer() {
        return this.renderer;
    }

    public void renderLast() {
    }

    interface MembershipListener {
        void onMembershipPropertyChanged(EngineSystem var1);
    }
}
