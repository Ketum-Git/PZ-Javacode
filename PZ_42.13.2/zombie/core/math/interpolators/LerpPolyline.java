// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math.interpolators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;

@XmlRootElement
public class LerpPolyline extends IInterpolator {
    @XmlElement(name = "m_Points")
    public final List<LerpPolyline.PolylinePoint> pointsRaw = new ArrayList<>();
    private int numPoints = -1;
    private float[][] points;
    private float minX;
    private float maxX;

    @Override
    public float lerp(float in_alpha) {
        int numPoints = this.numPoints;
        if (numPoints <= 0) {
            return 0.0F;
        } else if (in_alpha <= this.minX) {
            return this.points[0][1];
        } else if (in_alpha >= this.maxX) {
            return this.points[numPoints - 1][1];
        } else {
            for (int i = 1; i < numPoints; i++) {
                float pointX = this.points[i][0];
                if (!(pointX < in_alpha)) {
                    float prevPointX = this.points[i - 1][0];
                    if (prevPointX != pointX) {
                        float prevPointY = this.points[i - 1][1];
                        float pointY = this.points[i][1];
                        float lerpAlpha = (in_alpha - prevPointX) / (pointX - prevPointX);
                        return PZMath.lerp(prevPointY, pointY, lerpAlpha);
                    }
                }
            }

            return this.points[numPoints - 1][1];
        }
    }

    public void setPoints(Collection<LerpPolyline.PolylinePoint> in_points) {
        this.pointsRaw.clear();
        this.pointsRaw.addAll(in_points);
        this.parse();
    }

    public void setPoints(LerpPolyline.PolylinePoint... in_points) {
        this.setPoints(Arrays.asList(in_points));
    }

    private void parse() {
        this.numPoints = this.pointsRaw.size();
        this.points = new float[this.numPoints][2];
        this.minX = Float.MAX_VALUE;
        this.maxX = Float.MIN_VALUE;

        for (int x = 0; x < this.numPoints; x++) {
            LerpPolyline.PolylinePoint rawPoint = this.pointsRaw.get(x);
            this.points[x][0] = rawPoint.x;
            this.points[x][1] = rawPoint.y;
        }

        for (int pointIdx = 0; pointIdx < this.numPoints; pointIdx++) {
            int smallestIdx = pointIdx;

            for (int otherIdx = pointIdx + 1; otherIdx < this.numPoints; otherIdx++) {
                if (this.points[otherIdx][0] < this.points[pointIdx][0]) {
                    smallestIdx = otherIdx;
                }
            }

            if (smallestIdx != pointIdx) {
                float tempX = this.points[pointIdx][0];
                float tempY = this.points[pointIdx][1];
                this.points[pointIdx][0] = this.points[smallestIdx][0];
                this.points[pointIdx][1] = this.points[smallestIdx][1];
                this.points[smallestIdx][0] = tempX;
                this.points[smallestIdx][1] = tempY;
            }

            this.minX = PZMath.min(this.points[pointIdx][0], this.minX);
            this.maxX = PZMath.max(this.points[pointIdx][0], this.maxX);
        }
    }

    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        this.parse();
    }

    @XmlType(name = "PolyLinePoint")
    public static final class PolylinePoint {
        @XmlAttribute(name = "x")
        public float x;
        @XmlAttribute(name = "y")
        public float y;
    }
}
