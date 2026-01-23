// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public enum ActionSoundTime {
    ACTION_START("action_start"),
    ANIMATION_START("animation_start");

    private final String id;

    private ActionSoundTime(final String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return this.id;
    }

    public static ActionSoundTime fromValue(String value) {
        for (ActionSoundTime actionSoundTime : values()) {
            if (actionSoundTime.id.equals(value)) {
                return actionSoundTime;
            }
        }

        throw new IllegalArgumentException("No enum constant for value: " + value);
    }
}
