// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig.script;

import java.util.ArrayList;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;

public class SelectorBucketScript {
    protected final ArrayList<SelectorBucketScript> children = new ArrayList<>();
    private final SelectorType selectorType;
    protected String selectorString;
    protected SituatedType selectorSituated;
    protected int selectorWorldAge;
    protected final ArrayList<String> randomizers = new ArrayList<>();

    public SelectorType getSelectorType() {
        return this.selectorType;
    }

    public String getSelectorString() {
        return this.selectorString;
    }

    public SituatedType getSelectorSituated() {
        return this.selectorSituated;
    }

    public int getSelectorWorldAge() {
        return this.selectorWorldAge;
    }

    public ArrayList<String> getRandomizers() {
        return this.randomizers;
    }

    public ArrayList<SelectorBucketScript> getChildren() {
        return this.children;
    }

    protected SelectorBucketScript(SelectorType selectorType) {
        this.selectorType = selectorType;
    }

    protected SelectorBucketScript copy() {
        SelectorBucketScript copy = new SelectorBucketScript(this.selectorType);
        copy.selectorString = this.selectorString;
        copy.selectorSituated = this.selectorSituated;
        copy.selectorWorldAge = this.selectorWorldAge;

        for (SelectorBucketScript child : this.children) {
            copy.children.add(child.copy());
        }

        for (String s : this.randomizers) {
            copy.randomizers.add(s);
        }

        return copy;
    }
}
