// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.ecs;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import zombie.characters.component.FrameKeeperComponent;
import zombie.characters.ecs.componentmods.ECSFrameStep;
import zombie.characters.ecs.componentmods.ECSGameLoadingStateEnter;
import zombie.characters.ecs.componentmods.ECSInGameStateEnter;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.util.Type;

public interface ECSEntity {
    HashMap<Class<? extends ECSComponent>, ECSComponent> getECSComponentMap();

    default void registerECSComponents() {
        this.setECSComponent(new FrameKeeperComponent());
    }

    default void frameStep() {
        int currentFrame = IsoWorld.instance.getFrameNo();
        FrameKeeperComponent ecsFrameKeeper = this.getFrameKeeper();
        if (ecsFrameKeeper.getFrameNo() == currentFrame) {
            DebugType.General.error("Double-update call to ECSEntity.frameStep, at frame %d. ECSEntities must only be updated once per frame.", currentFrame);
            DebugType.General.printStackTrace(LogSeverity.Error, -1, null);
            throw new IllegalStateException(String.format("Double-update call at frame %d. ECSEntities must only be updated once per frame.", currentFrame));
        } else {
            ecsFrameKeeper.setFrameNo(currentFrame);
            this.visitAllComponents(ECSFrameStep.class, ECSFrameStep::frameStep);
        }
    }

    private FrameKeeperComponent getFrameKeeper() {
        return this.getECSComponent(FrameKeeperComponent.class);
    }

    default int getFrameNo() {
        return this.getFrameKeeper().getFrameNo();
    }

    default void onInGameStateEnter() {
        this.visitAllComponents(ECSInGameStateEnter.class, ECSInGameStateEnter::onInGameStateEnter);
    }

    default void onGameLoadingStateEnter() {
        this.visitAllComponents(ECSGameLoadingStateEnter.class, ECSGameLoadingStateEnter::onGameLoadingStateEnter);
    }

    default <ComponentType extends ECSComponent> ComponentType getECSComponent(Class<ComponentType> componentTypeClass) {
        checkParameterNotNull(componentTypeClass, "componentTypeClass");
        ComponentType foundComponentRaw = this.tryGetECSComponent(componentTypeClass);
        if (foundComponentRaw == null) {
            throw new IllegalStateException("Entity has no Component of type: " + componentTypeClass.getSimpleName());
        } else {
            return foundComponentRaw;
        }
    }

    default <ComponentType extends ECSComponent> void setECSComponent(ComponentType component) {
        checkParameterNotNull(component, "component");
        if (!this.hasECSComponent(component)) {
            this.removeECSComponent(component.getECSClass());
            this.getECSComponentMapInternal().put(component.getECSClass(), component);
            component.setECSOwnerEntity(this);
        }
    }

    default <ComponentType extends ECSComponent> void removeECSComponent(ComponentType component) {
        checkParameterNotNull(component, "component");
        if (this.hasECSComponent(component)) {
            this.removeECSComponent(component.getECSClass());
        }
    }

    default <ComponentType extends ECSComponent> void removeECSComponent(Class<ComponentType> componentClass) {
        checkParameterNotNull(componentClass, "componentClass");
        ComponentType component = this.tryGetECSComponent(componentClass);
        if (component != null) {
            this.getECSComponentMapInternal().remove(component.getECSClass());
            component.setECSOwnerEntity(null);
        }
    }

    default <ComponentType extends ECSComponent> ComponentType tryGetECSComponent(Class<ComponentType> componentTypeClass) {
        checkParameterNotNull(componentTypeClass, "componentTypeClass");
        return Type.tryCastTo(this.getECSComponentMapInternal().get(ECSComponent.getECSClass(componentTypeClass)), componentTypeClass);
    }

    default boolean hasECSComponent(Class<? extends ECSComponent> componentTypeClass) {
        checkParameterNotNull(componentTypeClass, "componentTypeClass");
        return this.tryGetECSComponent(componentTypeClass) != null;
    }

    default boolean hasECSComponent(ECSComponent component) {
        checkParameterNotNull(component, "component");
        return this.tryGetECSComponent(component.getECSClass()) == component;
    }

    default <ST> void visitAllComponents(Class<? extends ST> instanceOf, Consumer<ST> visitor) {
        for (ECSComponent c : this.getECSComponentMapInternal().values()) {
            ST converted = Type.tryCastTo(c, (Class<ST>)instanceOf);
            if (converted != null) {
                visitor.accept(converted);
            }
        }
    }

    default <ST, P1> void visitAllComponents(Class<? extends ST> instanceOf, BiConsumer<ST, P1> visitor, P1 param1) {
        for (ECSComponent c : this.getECSComponentMapInternal().values()) {
            ST converted = Type.tryCastTo(c, (Class<ST>)instanceOf);
            if (converted != null) {
                visitor.accept(converted, param1);
            }
        }
    }

    private HashMap<Class<? extends ECSComponent>, ECSComponent> getECSComponentMapInternal() {
        HashMap<Class<? extends ECSComponent>, ECSComponent> componentMap = this.getECSComponentMap();
        if (componentMap == null) {
            throw new IllegalStateException("Entity has no component map.");
        } else {
            return componentMap;
        }
    }

    static void checkParameterNotNull(Object parameter, String parameterName) {
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter " + parameterName + " cannot be null.");
        }
    }
}
