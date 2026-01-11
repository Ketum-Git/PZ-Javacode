// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.opengl;

public abstract class IOpenGLState<T extends IOpenGLState.Value> {
    protected final T currentValue = this.defaultValue();
    private boolean dirty = true;

    public void set(T value) {
        if (this.dirty || !value.equals(this.currentValue)) {
            this.setCurrentValue(value);
            this.Set(value);
        }
    }

    void setCurrentValue(T value) {
        this.dirty = false;
        this.currentValue.set(value);
    }

    public void setDirty() {
        this.dirty = true;
    }

    public void restore() {
        this.dirty = false;
        this.Set(this.getCurrentValue());
    }

    T getCurrentValue() {
        return this.currentValue;
    }

    abstract T defaultValue();

    abstract void Set(T var1);

    public interface Value {
        IOpenGLState.Value set(IOpenGLState.Value var1);
    }
}
