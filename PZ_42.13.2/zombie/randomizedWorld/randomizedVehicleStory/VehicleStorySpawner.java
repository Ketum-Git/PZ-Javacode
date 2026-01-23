// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedVehicleStory;

import java.util.ArrayList;
import java.util.HashMap;
import zombie.core.math.PZMath;
import zombie.debug.LineDrawer;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.popman.ObjectPool;
import zombie.util.Type;

public class VehicleStorySpawner {
    private static final VehicleStorySpawner instance = new VehicleStorySpawner();
    private static final Vector2 s_vector2_1 = new Vector2();
    private static final Vector2 s_vector2_2 = new Vector2();
    private static final ObjectPool<VehicleStorySpawner.Element> s_elementPool = new ObjectPool<>(VehicleStorySpawner.Element::new);
    private static final int[] s_AABB = new int[4];
    public final ArrayList<VehicleStorySpawner.Element> elements = new ArrayList<>();
    public final HashMap<String, Object> storyParams = new HashMap<>();

    public static VehicleStorySpawner getInstance() {
        return instance;
    }

    public void clear() {
        s_elementPool.release(this.elements);
        this.elements.clear();
        this.storyParams.clear();
    }

    public VehicleStorySpawner.Element addElement(String id, float x, float y, float direction, float width, float height) {
        VehicleStorySpawner.Element element = s_elementPool.alloc().init(id, x, y, direction, width, height);
        this.elements.add(element);
        return element;
    }

    public void setParameter(String key, boolean value) {
        this.storyParams.put(key, value ? Boolean.TRUE : Boolean.FALSE);
    }

    public void setParameter(String key, float value) {
        this.storyParams.put(key, value);
    }

    public void setParameter(String key, int value) {
        this.storyParams.put(key, value);
    }

    public void setParameter(String key, Object value) {
        this.storyParams.put(key, value);
    }

    public boolean getParameterBoolean(String key) {
        return this.getParameter(key, Boolean.class);
    }

    public float getParameterFloat(String key) {
        return this.getParameter(key, Float.class);
    }

    public int getParameterInteger(String key) {
        return this.getParameter(key, Integer.class);
    }

    public String getParameterString(String key) {
        return this.getParameter(key, String.class);
    }

    public <E> E getParameter(String key, Class<E> clazz) {
        return Type.tryCastTo(this.storyParams.get(key), clazz);
    }

    public void spawn(float worldX, float worldY, float worldZ, float angleRadians, VehicleStorySpawner.IElementSpawner spawner) {
        for (int i = 0; i < this.elements.size(); i++) {
            VehicleStorySpawner.Element element = this.elements.get(i);
            Vector2 v = s_vector2_1.setLengthAndDirection(element.direction, 1.0F);
            v.add(element.position);
            this.rotate(worldX, worldY, v, angleRadians);
            this.rotate(worldX, worldY, element.position, angleRadians);
            element.direction = Vector2.getDirection(v.x - element.position.x, v.y - element.position.y);
            element.z = worldZ;
            element.square = IsoWorld.instance.currentCell.getGridSquare((double)element.position.x, (double)element.position.y, (double)worldZ);
            spawner.spawn(this, element);
        }
    }

    public Vector2 rotate(float centerX, float centerY, Vector2 v, float angleRadians) {
        float vx = v.x;
        float vy = v.y;
        v.x = centerX + (float)(vx * Math.cos(angleRadians) - vy * Math.sin(angleRadians));
        v.y = centerY + (float)(vx * Math.sin(angleRadians) + vy * Math.cos(angleRadians));
        return v;
    }

    public void getAABB(float centerX, float centerY, float width, float height, float angleRadians, int[] aabb) {
        Vector2 vec = s_vector2_1.setLengthAndDirection(angleRadians, 1.0F);
        Vector2 vec2 = s_vector2_2.set(vec);
        vec2.tangent();
        vec.x *= height / 2.0F;
        vec.y *= height / 2.0F;
        vec2.x *= width / 2.0F;
        vec2.y *= width / 2.0F;
        float fx = centerX + vec.x;
        float fy = centerY + vec.y;
        float bx = centerX - vec.x;
        float by = centerY - vec.y;
        float fx1 = fx - vec2.x;
        float fy1 = fy - vec2.y;
        float fx2 = fx + vec2.x;
        float fy2 = fy + vec2.y;
        float bx1 = bx - vec2.x;
        float by1 = by - vec2.y;
        float bx2 = bx + vec2.x;
        float by2 = by + vec2.y;
        float minX = PZMath.min(fx1, PZMath.min(fx2, PZMath.min(bx1, bx2)));
        float maxX = PZMath.max(fx1, PZMath.max(fx2, PZMath.max(bx1, bx2)));
        float minY = PZMath.min(fy1, PZMath.min(fy2, PZMath.min(by1, by2)));
        float maxY = PZMath.max(fy1, PZMath.max(fy2, PZMath.max(by1, by2)));
        aabb[0] = (int)PZMath.floor(minX);
        aabb[1] = (int)PZMath.floor(minY);
        aabb[2] = (int)PZMath.ceil(maxX);
        aabb[3] = (int)PZMath.ceil(maxY);
    }

    public void render(float centerX, float centerY, float z, float width, float height, float angleRadians) {
        LineDrawer.DrawIsoRectRotated(centerX, centerY, z, width, height, angleRadians, 0.0F, 0.0F, 1.0F, 1.0F);
        float r = 1.0F;
        float g = 1.0F;
        float b = 1.0F;
        float a = 1.0F;
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (VehicleStorySpawner.Element element : this.elements) {
            Vector2 v = s_vector2_1.setLengthAndDirection(element.direction, 1.0F);
            LineDrawer.DrawIsoLine(element.position.x, element.position.y, z, element.position.x + v.x, element.position.y + v.y, z, 1.0F, 1.0F, 1.0F, 1.0F, 1);
            LineDrawer.DrawIsoRectRotated(element.position.x, element.position.y, z, element.width, element.height, element.direction, 1.0F, 1.0F, 1.0F, 1.0F);
            this.getAABB(element.position.x, element.position.y, element.width, element.height, element.direction, s_AABB);
            minX = PZMath.min(minX, (float)s_AABB[0]);
            minY = PZMath.min(minY, (float)s_AABB[1]);
            maxX = PZMath.max(maxX, (float)s_AABB[2]);
            maxY = PZMath.max(maxY, (float)s_AABB[3]);
        }
    }

    public static final class Element {
        String id;
        final Vector2 position = new Vector2();
        float direction;
        float width;
        float height;
        float z;
        IsoGridSquare square;

        VehicleStorySpawner.Element init(String id, float x, float y, float directionRadians, float width, float height) {
            this.id = id;
            this.position.set(x, y);
            this.direction = directionRadians;
            this.width = width;
            this.height = height;
            this.z = 0.0F;
            this.square = null;
            return this;
        }
    }

    public interface IElementSpawner {
        void spawn(VehicleStorySpawner spawner, VehicleStorySpawner.Element element);
    }
}
