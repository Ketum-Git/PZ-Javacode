// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.math.interpolators;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.math.IInterpolator;
import zombie.core.math.PZMath;

@XmlRootElement
public class LerpSequence extends IInterpolator {
    @XmlElement(name = "point")
    public List<LerpSequence.LerpSequenceEntry> sequenceRaw = new ArrayList<>();
    private int numEntries = -1;
    private LerpSequence.LerpSequenceEntry[] sequenceEntries;
    private float minX;
    private float maxX;

    @Override
    public float lerp(float in_alpha) {
        int numEntries = this.numEntries;
        if (numEntries <= 0) {
            return 0.0F;
        } else if (in_alpha <= this.minX) {
            return this.sequenceEntries[0].y;
        } else if (in_alpha >= this.maxX) {
            return this.sequenceEntries[numEntries - 1].y;
        } else {
            for (int i = 1; i < numEntries; i++) {
                LerpSequence.LerpSequenceEntry entry = this.sequenceEntries[i];
                float pointX = entry.x;
                if (!(pointX < in_alpha)) {
                    LerpSequence.LerpSequenceEntry prevEntry = this.sequenceEntries[i - 1];
                    float prevPointX = prevEntry.x;
                    if (prevPointX != pointX) {
                        float lerpAlpha = (in_alpha - prevPointX) / (pointX - prevPointX);
                        return PZMath.lerp(prevEntry.y, entry.y, lerpAlpha, entry.lerpType);
                    }
                }
            }

            return this.sequenceEntries[numEntries - 1].y;
        }
    }

    private void parse() {
        this.numEntries = this.sequenceRaw.size();
        this.sequenceEntries = new LerpSequence.LerpSequenceEntry[this.numEntries];
        this.minX = Float.MAX_VALUE;
        this.maxX = Float.MIN_VALUE;

        for (int x = 0; x < this.numEntries; x++) {
            LerpSequence.LerpSequenceEntry rawEntry = this.sequenceRaw.get(x);
            this.sequenceEntries[x] = rawEntry;
        }

        for (int entryIdx = 0; entryIdx < this.numEntries; entryIdx++) {
            int smallestIdx = entryIdx;

            for (int otherIdx = entryIdx + 1; otherIdx < this.numEntries; otherIdx++) {
                if (this.sequenceEntries[otherIdx].x < this.sequenceEntries[entryIdx].x) {
                    smallestIdx = otherIdx;
                }
            }

            if (smallestIdx != entryIdx) {
                LerpSequence.LerpSequenceEntry tempEntry = this.sequenceEntries[entryIdx];
                this.sequenceEntries[entryIdx] = this.sequenceEntries[smallestIdx];
                this.sequenceEntries[smallestIdx] = tempEntry;
            }

            this.minX = PZMath.min(this.sequenceEntries[entryIdx].x, this.minX);
            this.maxX = PZMath.max(this.sequenceEntries[entryIdx].x, this.maxX);
        }
    }

    public void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
        this.parse();
    }

    @XmlType(name = "LerpSequenceEntry")
    public static final class LerpSequenceEntry {
        @XmlElement(name = "out")
        public LerpType lerpType = LerpType.Linear;
        @XmlAttribute(name = "x")
        public float x;
        @XmlAttribute(name = "y")
        public float y;
    }
}
