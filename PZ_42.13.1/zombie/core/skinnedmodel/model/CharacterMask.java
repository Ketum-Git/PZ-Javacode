// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import zombie.characterTextures.BloodBodyPartType;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.util.Pool;

public final class CharacterMask {
    private final boolean[] visibleFlags = createFlags(CharacterMask.Part.values().length, true);

    public boolean isBloodBodyPartVisible(BloodBodyPartType bpt) {
        for (CharacterMask.Part cmp : bpt.getCharacterMaskParts()) {
            if (this.isPartVisible(cmp)) {
                return true;
            }
        }

        return false;
    }

    private static boolean[] createFlags(int length, boolean val) {
        boolean[] flags = new boolean[length];

        for (int i = 0; i < length; i++) {
            flags[i] = val;
        }

        return flags;
    }

    public void setAllVisible(boolean isVisible) {
        Arrays.fill(this.visibleFlags, isVisible);
    }

    public void copyFrom(CharacterMask rhs) {
        System.arraycopy(rhs.visibleFlags, 0, this.visibleFlags, 0, this.visibleFlags.length);
    }

    public void setPartVisible(CharacterMask.Part part, boolean isVisible) {
        if (part.hasSubdivisions()) {
            for (CharacterMask.Part sub : part.subDivisions()) {
                this.setPartVisible(sub, isVisible);
            }
        } else {
            this.visibleFlags[part.getValue()] = isVisible;
        }
    }

    public void setPartsVisible(ArrayList<Integer> parts, boolean isVisible) {
        for (int i = 0; i < parts.size(); i++) {
            int maskIndex = parts.get(i);
            CharacterMask.Part part = CharacterMask.Part.fromInt(maskIndex);
            if (part == null) {
                if (DebugLog.isEnabled(DebugType.Clothing)) {
                    DebugLog.Clothing.warn("MaskValue out of bounds: " + maskIndex);
                }
            } else {
                this.setPartVisible(part, isVisible);
            }
        }
    }

    public boolean isPartVisible(CharacterMask.Part part) {
        if (part == null) {
            return false;
        } else if (!part.hasSubdivisions()) {
            return this.visibleFlags[part.getValue()];
        } else {
            boolean allSubdivsSet = true;

            for (int i = 0; allSubdivsSet && i < part.subDivisions().length; i++) {
                CharacterMask.Part sub = part.subDivisions()[i];
                allSubdivsSet = this.visibleFlags[sub.getValue()];
            }

            return allSubdivsSet;
        }
    }

    public boolean isTorsoVisible() {
        return this.isPartVisible(CharacterMask.Part.Torso);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{VisibleFlags:(" + this.contentsToString() + ")}";
    }

    /**
     * Returns a list of all Visible components.
     */
    public String contentsToString() {
        if (this.isAllVisible()) {
            return "All Visible";
        } else if (this.isNothingVisible()) {
            return "Nothing Visible";
        } else {
            StringBuilder builder = new StringBuilder();
            int i = 0;

            for (int maskCount = 0; i < CharacterMask.Part.leaves().length; i++) {
                CharacterMask.Part part = CharacterMask.Part.leaves()[i];
                if (this.isPartVisible(part)) {
                    if (maskCount > 0) {
                        builder.append(',');
                    }

                    builder.append(part);
                    maskCount++;
                }
            }

            return builder.toString();
        }
    }

    private boolean isAll(boolean val) {
        boolean isAll = true;
        int i = 0;

        for (int count = CharacterMask.Part.leaves().length; isAll && i < count; i++) {
            CharacterMask.Part part = CharacterMask.Part.leaves()[i];
            isAll = this.isPartVisible(part) == val;
        }

        return isAll;
    }

    public boolean isNothingVisible() {
        return this.isAll(false);
    }

    public boolean isAllVisible() {
        return this.isAll(true);
    }

    public void forEachVisible(Consumer<CharacterMask.Part> action) {
        try {
            for (int i = 0; i < CharacterMask.Part.leaves().length; i++) {
                CharacterMask.Part part = CharacterMask.Part.leaves()[i];
                if (this.isPartVisible(part)) {
                    action.accept(part);
                }
            }
        } finally {
            Pool.tryRelease(action);
        }
    }

    public static enum Part {
        Head(0),
        Torso(1, true),
        Pelvis(2, true),
        LeftArm(3),
        LeftHand(4),
        RightArm(5),
        RightHand(6),
        LeftLeg(7),
        LeftFoot(8),
        RightLeg(9),
        RightFoot(10),
        Dress(11),
        Chest(12, Torso),
        Waist(13, Torso),
        Belt(14, Pelvis),
        Crotch(15, Pelvis);

        private final int value;
        private final CharacterMask.Part parent;
        private final boolean isSubdivided;
        private CharacterMask.Part[] subDivisions;
        private BloodBodyPartType[] bloodBodyPartTypes;
        private static final CharacterMask.Part[] s_leaves = leavesInternal();

        private Part(final int value) {
            this.value = value;
            this.parent = null;
            this.isSubdivided = false;
        }

        private Part(final int value, final CharacterMask.Part parent) {
            this.value = value;
            this.parent = parent;
            this.isSubdivided = false;
        }

        private Part(final int value, final boolean isSubdivided) {
            this.value = value;
            this.parent = null;
            this.isSubdivided = isSubdivided;
        }

        public static int count() {
            return values().length;
        }

        public static CharacterMask.Part[] leaves() {
            return s_leaves;
        }

        public static CharacterMask.Part fromInt(int index) {
            return index >= 0 && index < count() ? values()[index] : null;
        }

        public int getValue() {
            return this.value;
        }

        public CharacterMask.Part getParent() {
            return this.parent;
        }

        public boolean isSubdivision() {
            return this.parent != null;
        }

        public boolean hasSubdivisions() {
            return this.isSubdivided;
        }

        public CharacterMask.Part[] subDivisions() {
            if (this.subDivisions != null) {
                return this.subDivisions;
            } else {
                if (!this.isSubdivided) {
                    this.subDivisions = new CharacterMask.Part[0];
                }

                ArrayList<CharacterMask.Part> subDivsList = new ArrayList<>();

                for (CharacterMask.Part part : values()) {
                    if (part.parent == this) {
                        subDivsList.add(part);
                    }
                }

                this.subDivisions = subDivsList.toArray(new CharacterMask.Part[0]);
                return this.subDivisions;
            }
        }

        private static CharacterMask.Part[] leavesInternal() {
            ArrayList<CharacterMask.Part> leavesList = new ArrayList<>();

            for (CharacterMask.Part part : values()) {
                if (!part.hasSubdivisions()) {
                    leavesList.add(part);
                }
            }

            return leavesList.toArray(new CharacterMask.Part[0]);
        }

        public BloodBodyPartType[] getBloodBodyPartTypes() {
            if (this.bloodBodyPartTypes != null) {
                return this.bloodBodyPartTypes;
            } else {
                ArrayList<BloodBodyPartType> types = new ArrayList<>();
                switch (this) {
                    case Head:
                        types.add(BloodBodyPartType.Head);
                        break;
                    case Torso:
                        types.add(BloodBodyPartType.Torso_Upper);
                        types.add(BloodBodyPartType.Torso_Lower);
                        break;
                    case Pelvis:
                        types.add(BloodBodyPartType.UpperLeg_L);
                        types.add(BloodBodyPartType.UpperLeg_R);
                        types.add(BloodBodyPartType.Groin);
                        break;
                    case LeftArm:
                        types.add(BloodBodyPartType.UpperArm_L);
                        types.add(BloodBodyPartType.ForeArm_L);
                        break;
                    case LeftHand:
                        types.add(BloodBodyPartType.Hand_L);
                        break;
                    case RightArm:
                        types.add(BloodBodyPartType.UpperArm_R);
                        types.add(BloodBodyPartType.ForeArm_R);
                        break;
                    case RightHand:
                        types.add(BloodBodyPartType.Hand_R);
                        break;
                    case LeftLeg:
                        types.add(BloodBodyPartType.UpperLeg_L);
                        types.add(BloodBodyPartType.LowerLeg_L);
                        break;
                    case LeftFoot:
                        types.add(BloodBodyPartType.Foot_L);
                        break;
                    case RightLeg:
                        types.add(BloodBodyPartType.UpperLeg_R);
                        types.add(BloodBodyPartType.LowerLeg_R);
                        break;
                    case RightFoot:
                        types.add(BloodBodyPartType.Foot_R);
                    case Dress:
                    default:
                        break;
                    case Chest:
                        types.add(BloodBodyPartType.Torso_Upper);
                        break;
                    case Waist:
                        types.add(BloodBodyPartType.Torso_Lower);
                        break;
                    case Belt:
                        types.add(BloodBodyPartType.UpperLeg_L);
                        types.add(BloodBodyPartType.UpperLeg_R);
                        break;
                    case Crotch:
                        types.add(BloodBodyPartType.Groin);
                }

                this.bloodBodyPartTypes = new BloodBodyPartType[types.size()];

                for (int i = 0; i < types.size(); i++) {
                    this.bloodBodyPartTypes[i] = types.get(i);
                }

                return this.bloodBodyPartTypes;
            }
        }
    }
}
