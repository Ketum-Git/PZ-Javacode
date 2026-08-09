// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

public class GeneralAction extends Action {
    @Override
    float getDuration() {
        return 0.0F;
    }

    @Override
    void start() {
    }

    @Override
    void stop() {
    }

    @Override
    boolean isValid() {
        return false;
    }

    @Override
    boolean isUsingTimeout() {
        return true;
    }

    @Override
    void update() {
    }

    @Override
    boolean perform() {
        return false;
    }
}
