// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.util.lambda;

import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ReturnValueContainerPrimitives {
    public static final class RVBoolean extends PooledObject {
        public boolean returnVal;
        private static final Pool<ReturnValueContainerPrimitives.RVBoolean> s_pool = new Pool<>(ReturnValueContainerPrimitives.RVBoolean::new);

        @Override
        public void onReleased() {
            this.returnVal = false;
        }

        public static ReturnValueContainerPrimitives.RVBoolean alloc() {
            return s_pool.alloc();
        }
    }

    public static final class RVFloat extends PooledObject {
        public float returnVal;
        private static final Pool<ReturnValueContainerPrimitives.RVFloat> s_pool = new Pool<>(ReturnValueContainerPrimitives.RVFloat::new);

        @Override
        public void onReleased() {
            this.returnVal = 0.0F;
        }

        public static ReturnValueContainerPrimitives.RVFloat alloc() {
            return s_pool.alloc();
        }
    }

    public static final class RVInt extends PooledObject {
        public int returnVal;
        private static final Pool<ReturnValueContainerPrimitives.RVInt> s_pool = new Pool<>(ReturnValueContainerPrimitives.RVInt::new);

        @Override
        public void onReleased() {
            this.returnVal = 0;
        }

        public static ReturnValueContainerPrimitives.RVInt alloc() {
            return s_pool.alloc();
        }
    }
}
