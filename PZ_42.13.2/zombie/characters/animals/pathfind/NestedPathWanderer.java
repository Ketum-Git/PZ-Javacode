// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.animals.pathfind;

import org.joml.Vector2f;
import zombie.core.SpriteRenderer;
import zombie.core.random.Rand;
import zombie.worldMap.UIWorldMap;
import zombie.worldMap.WorldMapRenderer;

public final class NestedPathWanderer {
    public NestedPaths paths;
    public NestedPath path;
    public float x;
    public float y;
    boolean moveForwardOnPath = true;
    float switchPathTimer;

    void pickAnotherPath() {
        boolean moveOut = Rand.NextBool(2);
        int index = this.paths.paths.indexOf(this.path);
        Vector2f pos = new Vector2f();
        if (moveOut) {
            for (int i = index - 1; i >= 0; i--) {
                NestedPath path = this.paths.paths.get(i);
                if (path.inset == this.path.inset - 5) {
                    float t = path.getClosestPointOn(this.x, this.y, pos);
                    if (Vector2f.distance(this.x, this.y, pos.x, pos.y) < 10.0F) {
                        this.path = path;
                        return;
                    }
                }
            }
        } else {
            for (int ix = index + 1; ix < this.paths.paths.size(); ix++) {
                NestedPath path = this.paths.paths.get(ix);
                if (path.inset == this.path.inset + 5) {
                    float t = path.getClosestPointOn(this.x, this.y, pos);
                    if (Vector2f.distance(this.x, this.y, pos.x, pos.y) < 10.0F) {
                        this.path = path;
                        return;
                    }
                }
            }
        }
    }

    void moveAlongPath(float distance) {
        Vector2f pos = new Vector2f();
        float t = this.path.getClosestPointOn(this.x, this.y, pos);
        float zoneLength = this.path.getLength();
        float t2;
        if (this.moveForwardOnPath) {
            t2 = t + distance / zoneLength;
            if (t2 >= 1.0F) {
                t2 %= 1.0F;
            }
        } else {
            t2 = t - distance / zoneLength;
            if (t2 <= 0.0F) {
                t2 = (t2 + 1.0F) % 1.0F;
            }
        }

        this.path.getPointOn(t2, pos);
        this.x = pos.x;
        this.y = pos.y;
    }

    public void render(UIWorldMap ui) {
        if (++this.switchPathTimer >= 90.0F) {
            this.pickAnotherPath();
            this.switchPathTimer = 0.0F;
        }

        this.moveAlongPath(1.0F);
        this.drawRect(ui, this.x - 1.0F, this.y - 1.0F, 2.0F, 2.0F, 0.0F, 1.0F, 0.0F, 1.0F);
    }

    public void drawLine(UIWorldMap ui, float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
        WorldMapRenderer rr = ui.getAPIv1().getRenderer();
        float _x1 = rr.worldToUIX(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _y1 = rr.worldToUIY(x1, y1, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _x2 = rr.worldToUIX(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        float _y2 = rr.worldToUIY(x2, y2, rr.getDisplayZoomF(), rr.getCenterWorldX(), rr.getCenterWorldY(), rr.getModelViewProjectionMatrix());
        SpriteRenderer.instance.renderline(null, (int)_x1, (int)_y1, (int)_x2, (int)_y2, r, g, b, a, 1.0F);
    }

    public void drawRect(UIWorldMap ui, float x1, float y1, float w, float h, float r, float g, float b, float a) {
        this.drawLine(ui, x1, y1, x1 + w, y1, r, g, b, a);
        this.drawLine(ui, x1 + w, y1, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(ui, x1, y1 + h, x1 + w, y1 + h, r, g, b, a);
        this.drawLine(ui, x1, y1, x1, y1 + h, r, g, b, a);
    }
}
