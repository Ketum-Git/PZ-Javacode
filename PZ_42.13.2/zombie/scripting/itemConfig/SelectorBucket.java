// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.itemConfig;

import zombie.core.Core;
import zombie.debug.DebugLog;
import zombie.entity.GameEntity;
import zombie.inventory.ItemPickInfo;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;
import zombie.scripting.itemConfig.script.SelectorBucketScript;

public class SelectorBucket {
    private final SelectorBucket[] children;
    private final SelectorType selectorType;
    private final int[] selectorIds;
    private final SituatedType selectorSituated;
    private final int selectorWorldAge;
    private final Randomizer randomizer;
    private final String origSelectorString;
    private SelectorBucket onCreate;

    public SelectorType getSelectorType() {
        return this.selectorType;
    }

    public int[] getSelectorIDs() {
        return this.selectorIds;
    }

    public SituatedType getSelectorSituated() {
        return this.selectorSituated;
    }

    public int getSelectorWorldAge() {
        return this.selectorWorldAge;
    }

    public Randomizer getRandomizer() {
        return this.randomizer;
    }

    public String getOrigSelectorString() {
        return this.origSelectorString;
    }

    public boolean containsSelectorID(int id) {
        if (id != -1 && this.selectorIds != null) {
            if (this.selectorIds.length == 1) {
                return this.selectorIds[0] == id;
            } else {
                for (int i = 0; i < this.selectorIds.length; i++) {
                    if (this.selectorIds[i] == id) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    public boolean hasSelectorIDs() {
        return this.selectorIds != null && this.selectorIds.length > 0;
    }

    public SelectorBucket(int[] selectorIds, SelectorBucketScript script, SelectorBucket[] children, Randomizer randomizer) {
        this.selectorIds = selectorIds;
        this.selectorType = script.getSelectorType();
        this.selectorSituated = script.getSelectorSituated();
        this.selectorWorldAge = script.getSelectorWorldAge();
        this.randomizer = randomizer;
        this.children = children;
        this.origSelectorString = script.getSelectorString();
    }

    public boolean Resolve(GameEntity entity, ItemPickInfo info) {
        if (this.selectorType == SelectorType.None || info.isMatch(this)) {
            if (this.children.length > 0) {
                for (int i = 0; i < this.children.length; i++) {
                    SelectorBucket child = this.children[i];
                    if (child.Resolve(entity, info)) {
                        return true;
                    }
                }
            }

            if (this.selectorType != SelectorType.None && this.randomizer != null) {
                this.randomizer.execute(entity);
                return true;
            }
        }

        return false;
    }

    public boolean ResolveOnCreate(GameEntity entity) {
        if (this.selectorType == SelectorType.OnCreate && this.randomizer != null) {
            this.randomizer.execute(entity);
            return true;
        } else {
            if (Core.debug) {
                DebugLog.General.error("Something went wrong in SelectorBucket.ResolveOnCreate.");
            }

            return false;
        }
    }
}
