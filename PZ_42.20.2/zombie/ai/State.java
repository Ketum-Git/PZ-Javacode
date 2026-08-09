// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai;

import com.google.common.base.Predicates;
import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import zombie.UpdateSchedulerSimulationLevel;
import zombie.characters.IsoGameCharacter;
import zombie.characters.MoveDeltaModifiers;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.advancedanimation.events.AnimEventBroadcaster;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventListener;
import zombie.core.skinnedmodel.advancedanimation.events.IAnimEventWrappedBroadcaster;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.debug.DebugType;

public abstract class State implements IAnimEventListener, IAnimEventWrappedBroadcaster, IStateFlagsSource {
    private static final Map<Class<? extends State>, Map<String, State.Param<?>>> PARAMS_BY_STATE = new IdentityHashMap<>();
    private final AnimEventBroadcaster animEventBroadcaster = new AnimEventBroadcaster();
    private final boolean isSyncOnEnter;
    private final boolean isSyncOnExit;
    private final boolean isSyncOnSquare;
    private final boolean isSyncInIdle;

    protected State(boolean isSyncOnEnter, boolean isSyncOnExit, boolean isSyncOnSquare, boolean isSyncInIdle) {
        this.isSyncOnEnter = isSyncOnEnter;
        this.isSyncOnExit = isSyncOnExit;
        this.isSyncOnSquare = isSyncOnSquare;
        this.isSyncInIdle = isSyncInIdle;
    }

    public void enter(IsoGameCharacter owner) {
        DebugType.Multiplayer.noise(this.getName());
    }

    public void execute(IsoGameCharacter owner) {
    }

    public void exit(IsoGameCharacter owner) {
        DebugType.Multiplayer.noise(this.getName());
    }

    @Override
    public AnimEventBroadcaster getAnimEventBroadcaster() {
        return this.animEventBroadcaster;
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        this.getAnimEventBroadcaster().animEvent(owner, layer, track, event);
    }

    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
    }

    /**
     * Return TRUE if the owner should ignore collisions when passing between two squares.
     *  Defaults to FALSE
     */
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return false;
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    public void setParams(IsoGameCharacter owner, State.Stage stage) {
        DebugType.Multiplayer.trace("%s %s: %s", this.getName(), stage, this.getParams(owner));
    }

    @Override
    public final boolean isSyncOnEnter() {
        return this.isSyncOnEnter;
    }

    @Override
    public final boolean isSyncOnExit() {
        return this.isSyncOnExit;
    }

    @Override
    public final boolean isSyncOnSquare() {
        return this.isSyncOnSquare;
    }

    @Override
    public final boolean isSyncInIdle() {
        return this.isSyncInIdle;
    }

    public boolean isProcessedOnEnter() {
        return false;
    }

    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
    }

    public boolean isProcessedOnExit() {
        return false;
    }

    public void processOnExit(IsoGameCharacter owner, Map<Object, Object> delegate) {
    }

    public Map<State.Param<?>, Object> getParams(IsoGameCharacter owner) {
        return owner.getStateMachineParams(this.getClass());
    }

    public void loadFrom(Map<Object, Object> delegate, IsoGameCharacter character) {
        Map<String, State.Param<?>> params = PARAMS_BY_STATE.get(this.getClass());

        for (Entry<Object, Object> entry : delegate.entrySet()) {
            if (entry.getKey() instanceof String key && params.get(key) instanceof State.Param<?> param) {
                this.genericsHelper(character, param, entry.getValue());
            }
        }
    }

    public float awayCheckDistance() {
        return 0.2F;
    }

    private <T> void genericsHelper(IsoGameCharacter character, State.Param<T> param, Object value) {
        character.set(param, (T)value);
    }

    public UpdateSchedulerSimulationLevel getMinimumSimulationLevel() {
        return UpdateSchedulerSimulationLevel.minimum();
    }

    public static class Param<T> {
        private final Class<?> fromClazz;
        private final String name;
        private final Class<T> clazz;
        private final Supplier<T> defaultSupplier;

        private Param(String name, Class<T> clazz, Supplier<T> defaultSupplier) {
            if (defaultSupplier == null) {
                throw new IllegalArgumentException("Param cannot have a null-default-supplier");
            } else {
                Class<?> callerClass = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE)
                    .walk(frames -> frames.map(StackFrame::getDeclaringClass).filter(Predicates.not(this.getClass()::equals)).findFirst().orElse(null));
                if (State.class.isAssignableFrom(callerClass)) {
                    this.fromClazz = callerClass;
                    State.PARAMS_BY_STATE.computeIfAbsent((Class<? extends State>)this.fromClazz, var0 -> new HashMap<>()).put(name, this);
                    this.name = name;
                    this.clazz = clazz;
                    this.defaultSupplier = defaultSupplier;
                } else {
                    throw new IllegalStateException("Unable to determine caller for %s".formatted(name));
                }
            }
        }

        public static State.Param<Integer> ofInt(String name, int defaultValue) {
            return of(name, Integer.class, defaultValue);
        }

        public static State.Param<Long> ofLong(String name, long defaultValue) {
            return of(name, Long.class, defaultValue);
        }

        public static State.Param<Float> ofFloat(String name, float defaultValue) {
            return of(name, Float.class, defaultValue);
        }

        public static State.Param<String> ofString(String name, @Nullable String defaultValue) {
            return of(name, String.class, defaultValue);
        }

        public static State.Param<Boolean> ofBool(String name, boolean defaultValue) {
            return of(name, Boolean.class, defaultValue);
        }

        public static <R> State.Param<R> of(String name, Class<R> clazz) {
            return of(name, clazz, null);
        }

        public static <R> State.Param<R> of(String name, Class<R> clazz, R defaultValue) {
            return ofSupplier(name, clazz, () -> defaultValue);
        }

        public static <R> State.Param<R> ofSupplier(String name, Class<R> clazz, Supplier<R> defaultSupplier) {
            return new State.Param<>(name, clazz, defaultSupplier);
        }

        public String getName() {
            return this.name;
        }

        public T get(IsoGameCharacter owner) {
            return this.get(owner, this.defaultSupplier);
        }

        public T get(IsoGameCharacter owner, Supplier<T> defaultSupplier) {
            return (T)owner.getStateMachineParams(this.fromClazz).computeIfAbsent(this, var1x -> defaultSupplier.get());
        }

        public void set(IsoGameCharacter owner, T newT) {
            owner.getStateMachineParams(this.fromClazz).put(this, newT);
        }

        public T remove(IsoGameCharacter owner) {
            return (T)owner.getStateMachineParams(this.fromClazz).remove(this);
        }

        public Class<?> getStateClass() {
            return this.fromClazz;
        }

        public T fromDelegate(Map<Object, Object> delegate) {
            Object value = delegate.get(this.name);
            return value != null && !this.clazz.isInstance(value) ? this.defaultSupplier.get() : this.clazz.cast(value);
        }
    }

    public static enum Stage {
        Enter,
        Execute,
        Exit;
    }
}
