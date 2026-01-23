// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import org.joml.Vector2f;
import zombie.UsedFromLua;
import zombie.util.PooledObject;

@UsedFromLua
public final class EditStreetV1 extends PooledObject {
    private static final ClosestPoint m_closestPoint = new ClosestPoint();
    EditStreetsV1 owner;
    WorldMapStreet street;

    protected EditStreetV1 init(EditStreetsV1 owner, WorldMapStreet street) {
        this.owner = owner;
        this.street = street;
        return this;
    }

    public int getNumPoints() {
        return this.street.getNumPoints();
    }

    public float getPointX(int index) {
        return this.street.getPointX(index);
    }

    public float getPointY(int index) {
        return this.street.getPointY(index);
    }

    public void addPoint(float x, float y) {
        this.owner.getStreetData().onBeforeModifyStreet(this.street);
        this.street.addPoint(x, y);
        this.owner.getStreetData().onAfterModifyStreet(this.street);
    }

    public void insertPoint(float x, float y) {
        float uiX = this.owner.getUI().getAPI().worldToUIX(x, y);
        float uiY = this.owner.getUI().getAPI().worldToUIY(x, y);
        this.street.getClosestPointOn(this.owner.getUI(), uiX, uiY, m_closestPoint);
        this.owner.getStreetData().onBeforeModifyStreet(this.street);
        this.street.insertPoint(m_closestPoint.index + 1, x, y);
        this.owner.getStreetData().onAfterModifyStreet(this.street);
    }

    public void removePoint(int index) {
        this.owner.getStreetData().onBeforeModifyStreet(this.street);
        this.street.removePoint(index);
        this.owner.getStreetData().onAfterModifyStreet(this.street);
    }

    public void setPoint(int index, float x, float y) {
        this.owner.getStreetData().onBeforeModifyStreet(this.street);
        this.street.setPoint(index, x, y);
        this.owner.getStreetData().onAfterModifyStreet(this.street);
    }

    public String getTranslatedText() {
        return this.street.getTranslatedText();
    }

    public void setTranslatedText(String text) {
        this.owner.getStreetData().onBeforeModifyStreet(this.street);
        this.street.setTranslatedText(text);
        this.owner.getStreetData().onAfterModifyStreet(this.street);
    }

    public int getWidth() {
        return this.street.getWidth();
    }

    public void setWidth(int width) {
        this.street.setWidth(width);
    }

    public void reverseDirection() {
        this.street.reverseDirection();
    }

    public int pickPoint(float uiX, float uiY) {
        return this.street.pickPoint(this.owner.getUI(), uiX, uiY);
    }

    public Vector2f getAddPointLocation(float uiX, float uiY, Vector2f closest) {
        ClosestPoint closestPoint = this.street.getAddPointLocation(this.owner.getUI(), uiX, uiY, m_closestPoint);
        return closestPoint == null ? null : closest.set(closestPoint.x, closestPoint.y);
    }
}
