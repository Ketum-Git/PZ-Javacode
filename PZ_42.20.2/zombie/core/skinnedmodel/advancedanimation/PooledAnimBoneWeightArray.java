// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.advancedanimation;

import java.util.List;
import zombie.util.Pool;
import zombie.util.PooledArrayObject;
import zombie.util.list.PZArrayUtil;

public class PooledAnimBoneWeightArray extends PooledArrayObject<AnimBoneWeight> {
    private static final PooledAnimBoneWeightArray s_empty = new PooledAnimBoneWeightArray();
    private static final Pool<PooledAnimBoneWeightArray> s_pool = new Pool<>(PooledAnimBoneWeightArray::new);

    public static PooledAnimBoneWeightArray alloc(int count) {
        if (count == 0) {
            return s_empty;
        } else {
            PooledAnimBoneWeightArray newObject = s_pool.alloc();
            newObject.initCapacity(count, x$0 -> new AnimBoneWeight[x$0]);
            return newObject;
        }
    }

    public static PooledAnimBoneWeightArray toArray(List<AnimBoneWeight> list) {
        if (list == null) {
            return null;
        } else {
            PooledAnimBoneWeightArray newObject = alloc(list.size());
            PZArrayUtil.arrayCopy(newObject.array(), list);
            return newObject;
        }
    }

    public static PooledAnimBoneWeightArray toArray(PooledArrayObject<AnimBoneWeight> source) {
        if (source == null) {
            return null;
        } else {
            PooledAnimBoneWeightArray newObject = alloc(source.length());
            PZArrayUtil.arrayCopy(newObject.array(), source.array());
            return newObject;
        }
    }
}
