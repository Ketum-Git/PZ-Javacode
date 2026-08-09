// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.ecs;

import zombie.util.Type;

public abstract class ECSComponent {
    private final Class<? extends ECSComponent> ecsClass = getECSClass((Class<? extends ECSComponent>)this.getClass());
    private ECSEntity ecsOwner;

    public Class<? extends ECSComponent> getECSClass() {
        return this.ecsClass;
    }

    public static Class<? extends ECSComponent> getECSClass(Class<? extends ECSComponent> clazz) {
        Class<?> foundEcsClass = null;

        for (Class<?> c = clazz; c != null && c != ECSComponent.class; c = c.getSuperclass()) {
            foundEcsClass = c;
        }

        return (Class<? extends ECSComponent>)foundEcsClass;
    }

    public ECSEntity getECSOwnerEntity() {
        return this.ecsOwner;
    }

    public <EntityType extends ECSEntity> void setECSOwnerEntity(EntityType ownerEntity) {
        if (this.ecsOwner != ownerEntity) {
            ECSEntity prevOwner = this.ecsOwner;
            this.ecsOwner = ownerEntity;
            if (prevOwner != null) {
                prevOwner.removeECSComponent(this);
            }

            if (this.ecsOwner != null) {
                this.ecsOwner.setECSComponent(this);
            }
        }
    }

    public <EntityType extends ECSEntity> EntityType getECSOwnerEntity(Class<EntityType> entityTypeClass) {
        return entityTypeClass.cast(this.getECSOwnerEntity());
    }

    public <EntityType extends ECSEntity> EntityType tryGetECSOwnerEntity(Class<EntityType> entityTypeClass) {
        return Type.tryCastTo(this.getECSOwnerEntity(), entityTypeClass);
    }

    public <OwnerType> OwnerType tryGetECSOwnerEntityAs(Class<? extends OwnerType> ownerTypeClass) {
        return Type.tryCastTo(this.getECSOwnerEntity(), (Class<OwnerType>)ownerTypeClass);
    }
}
