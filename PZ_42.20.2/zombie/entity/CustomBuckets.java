// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

public class CustomBuckets {
    public static final String MetaEntities = "MetaEntities";
    public static final String NonMetaRenderers = "NonMetaRenderers";
    public static final String NonMetaEntities = "NonMetaEntities";
    public static final String NonMetaCraftLogic = "NonMetaCraftLogic";
    private static final Family nonMetaCraftLogicFamily = Family.all(ComponentType.CraftLogic).get();

    static void initializeCustomBuckets(Engine engine) {
        engine.registerCustomBucket("NonMetaRenderers", new CustomBuckets.NonMetaRenderersValidator());
        engine.registerCustomBucket("NonMetaEntities", new CustomBuckets.NonMetaEntitiesValidator());
        engine.registerCustomBucket("NonMetaCraftLogic", new CustomBuckets.NonMetaCraftLogicValidator());
        engine.registerCustomBucket("MetaEntities", new CustomBuckets.MetaEntitiesValidator());
    }

    private static class MetaEntitiesValidator implements EntityBucket.EntityValidator {
        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return entity instanceof MetaEntity;
        }
    }

    private static class NonMetaCraftLogicValidator implements EntityBucket.EntityValidator {
        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return CustomBuckets.nonMetaCraftLogicFamily.matches(entity) && !(entity instanceof MetaEntity);
        }
    }

    private static class NonMetaEntitiesValidator implements EntityBucket.EntityValidator {
        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return !(entity instanceof MetaEntity);
        }
    }

    private static class NonMetaRenderersValidator implements EntityBucket.EntityValidator {
        @Override
        public boolean acceptsEntity(GameEntity entity) {
            return entity.hasRenderers() && !(entity instanceof MetaEntity);
        }
    }
}
