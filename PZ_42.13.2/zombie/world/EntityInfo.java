// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

public class EntityInfo extends DictionaryInfo<EntityInfo> {
    @Override
    public String getInfoType() {
        return "entity";
    }

    public EntityInfo copy() {
        EntityInfo c = new EntityInfo();
        c.copyFrom(this);
        return c;
    }
}
