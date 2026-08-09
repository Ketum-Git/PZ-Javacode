// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.entity;

import java.util.Objects;
import zombie.entity.util.Array;
import zombie.entity.util.ImmutableArray;
import zombie.entity.util.ObjectMap;

public final class EntityBucketManager {
    private final ObjectMap<Family, EntityBucket> buckets = new ObjectMap<>();
    private final ObjectMap<String, EntityBucket> customBuckets = new ObjectMap<>();
    private final Array<EntityBucket> bucketsArray = new Array<>(false, 16);
    private final EntityBucket.RendererBucket rendererBucket;
    private EntityBucket.IsoObjectBucket isoObjectBucket;
    private EntityBucket.InventoryItemBucket inventoryItemBucket;
    private EntityBucket.VehiclePartBucket vehiclePartBucket;
    private int bucketIndex;
    private final ImmutableArray<GameEntity> entities;
    private boolean updating;
    private GameEntity currentUpdatingEntity;
    private final EntityBucketManager.BucketsUpdatingInformer bucketsUpdatingInformer = new EntityBucketManager.BucketsUpdatingInformer();

    protected EntityBucketManager(ImmutableArray<GameEntity> entities) {
        this.entities = entities;
        this.rendererBucket = new EntityBucket.RendererBucket(this.bucketIndex++);
        this.bucketsArray.add(this.rendererBucket);
    }

    EntityBucketManager.BucketsUpdatingInformer getBucketsUpdatingInformer() {
        return this.bucketsUpdatingInformer;
    }

    EntityBucket getRendererBucket() {
        return this.rendererBucket;
    }

    EntityBucket getIsoObjectBucket() {
        if (this.isoObjectBucket == null) {
            this.isoObjectBucket = new EntityBucket.IsoObjectBucket(this.bucketIndex++);
            this.bucketsArray.add(this.isoObjectBucket);

            for (int i = 0; i < this.entities.size(); i++) {
                this.isoObjectBucket.updateMembership(this.entities.get(i));
            }
        }

        return this.isoObjectBucket;
    }

    EntityBucket getInventoryItemBucket() {
        if (this.inventoryItemBucket == null) {
            this.inventoryItemBucket = new EntityBucket.InventoryItemBucket(this.bucketIndex++);
            this.bucketsArray.add(this.inventoryItemBucket);

            for (int i = 0; i < this.entities.size(); i++) {
                this.inventoryItemBucket.updateMembership(this.entities.get(i));
            }
        }

        return this.inventoryItemBucket;
    }

    EntityBucket getVehiclePartBucket() {
        if (this.vehiclePartBucket == null) {
            this.vehiclePartBucket = new EntityBucket.VehiclePartBucket(this.bucketIndex++);
            this.bucketsArray.add(this.vehiclePartBucket);

            for (int i = 0; i < this.entities.size(); i++) {
                this.vehiclePartBucket.updateMembership(this.entities.get(i));
            }
        }

        return this.vehiclePartBucket;
    }

    EntityBucket getBucket(Family family) {
        EntityBucket bucket = this.buckets.get(family);
        if (bucket == null) {
            bucket = new EntityBucket.FamilyBucket(this.bucketIndex++, family);
            this.buckets.put(family, bucket);
            this.bucketsArray.add(bucket);

            for (int i = 0; i < this.entities.size(); i++) {
                bucket.updateMembership(this.entities.get(i));
            }
        }

        return bucket;
    }

    EntityBucket registerCustomBucket(String identifier, EntityBucket.EntityValidator validator) {
        if (this.customBuckets.get(identifier) != null) {
            throw new IllegalArgumentException("Bucket with identifier '" + identifier + "' already exists.");
        } else {
            EntityBucket bucket = new EntityBucket.CustomBucket(this.bucketIndex++, validator);
            this.customBuckets.put(identifier, bucket);
            this.bucketsArray.add(bucket);

            for (int i = 0; i < this.entities.size(); i++) {
                bucket.updateMembership(this.entities.get(i));
            }

            return bucket;
        }
    }

    EntityBucket getCustomBucket(String identifier) {
        return this.customBuckets.get(identifier);
    }

    void updateBucketMembership(GameEntity entity) {
        this.updating = true;
        this.currentUpdatingEntity = entity;

        try {
            if (this.bucketsArray.size > 0) {
                for (int i = 0; i < this.bucketsArray.size; i++) {
                    this.bucketsArray.get(i).updateMembership(entity);
                }
            }
        } finally {
            this.updating = false;
            this.currentUpdatingEntity = null;
        }
    }

    protected class BucketsUpdatingInformer implements IBucketInformer {
        protected BucketsUpdatingInformer() {
            Objects.requireNonNull(EntityBucketManager.this);
            super();
        }

        @Override
        public boolean value() {
            return EntityBucketManager.this.updating;
        }

        @Override
        public GameEntity updatingEntity() {
            return EntityBucketManager.this.currentUpdatingEntity;
        }
    }
}
