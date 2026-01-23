// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import org.joml.Vector2f;
import zombie.worldMap.UIWorldMap;

public final class Intersection {
    WorldMapStreet street;
    float worldX;
    float worldY;
    int segment;
    float distanceFromStart;
    boolean renderedLabelOver;

    float getDistanceFromStart(UIWorldMap ui, WorldMapStreet owner) {
        float distanceFromStart = 0.0F;

        for (int i = 0; i < this.segment; i++) {
            float uiX1 = ui.getAPI().worldToUIX(owner.getPointX(i), owner.getPointY(i));
            float uiY1 = ui.getAPI().worldToUIY(owner.getPointX(i), owner.getPointY(i));
            float uiX2 = ui.getAPI().worldToUIX(owner.getPointX(i + 1), owner.getPointY(i + 1));
            float uiY2 = ui.getAPI().worldToUIY(owner.getPointX(i + 1), owner.getPointY(i + 1));
            distanceFromStart += Vector2f.length(uiX2 - uiX1, uiY2 - uiY1);
        }

        float uiX1 = ui.getAPI().worldToUIX(owner.getPointX(this.segment), owner.getPointY(this.segment));
        float uiY1 = ui.getAPI().worldToUIY(owner.getPointX(this.segment), owner.getPointY(this.segment));
        float uiX2 = ui.getAPI().worldToUIX(this.worldX, this.worldY);
        float uiY2 = ui.getAPI().worldToUIY(this.worldX, this.worldY);
        return distanceFromStart + Vector2f.length(uiX2 - uiX1, uiY2 - uiY1);
    }
}
