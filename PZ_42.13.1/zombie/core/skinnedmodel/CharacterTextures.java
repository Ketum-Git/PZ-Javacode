// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel;

import java.util.ArrayList;
import zombie.iso.IsoDirections;

public final class CharacterTextures {
    final ArrayList<CharacterTextures.CTAnimSet> animSets = new ArrayList<>();

    CharacterTextures.CTAnimSet getAnimSet(String animSet) {
        for (int i = 0; i < this.animSets.size(); i++) {
            CharacterTextures.CTAnimSet ctAnimSet = this.animSets.get(i);
            if (ctAnimSet.name.equals(animSet)) {
                return ctAnimSet;
            }
        }

        return null;
    }

    DeadBodyAtlas.BodyTexture getTexture(String animSet, String state, IsoDirections dir, int frame) {
        CharacterTextures.CTAnimSet ctAnimSet = this.getAnimSet(animSet);
        if (ctAnimSet == null) {
            return null;
        } else {
            CharacterTextures.CTState ctState = ctAnimSet.getState(state);
            if (ctState == null) {
                return null;
            } else {
                CharacterTextures.CTEntry ctEntry = ctState.getEntry(dir, frame);
                return ctEntry == null ? null : ctEntry.texture;
            }
        }
    }

    void addTexture(String animSet, String state, IsoDirections dir, int frame, DeadBodyAtlas.BodyTexture texture) {
        CharacterTextures.CTAnimSet ctAnimSet = this.getAnimSet(animSet);
        if (ctAnimSet == null) {
            ctAnimSet = new CharacterTextures.CTAnimSet();
            ctAnimSet.name = animSet;
            this.animSets.add(ctAnimSet);
        }

        ctAnimSet.addEntry(state, dir, frame, texture);
    }

    void clear() {
        this.animSets.clear();
    }

    private static final class CTAnimSet {
        String name;
        final ArrayList<CharacterTextures.CTState> states = new ArrayList<>();

        CharacterTextures.CTState getState(String state) {
            for (int i = 0; i < this.states.size(); i++) {
                CharacterTextures.CTState ctState = this.states.get(i);
                if (ctState.name.equals(state)) {
                    return ctState;
                }
            }

            return null;
        }

        void addEntry(String state, IsoDirections dir, int frame, DeadBodyAtlas.BodyTexture texture) {
            CharacterTextures.CTState ctState = this.getState(state);
            if (ctState == null) {
                ctState = new CharacterTextures.CTState();
                ctState.name = state;
                this.states.add(ctState);
            }

            ctState.addEntry(dir, frame, texture);
        }
    }

    private static final class CTEntry {
        int frame;
        DeadBodyAtlas.BodyTexture texture;
    }

    private static final class CTEntryList extends ArrayList<CharacterTextures.CTEntry> {
    }

    private static final class CTState {
        String name;
        final CharacterTextures.CTEntryList[] entries = new CharacterTextures.CTEntryList[IsoDirections.values().length];

        CTState() {
            for (int i = 0; i < this.entries.length; i++) {
                this.entries[i] = new CharacterTextures.CTEntryList();
            }
        }

        CharacterTextures.CTEntry getEntry(IsoDirections dir, int frame) {
            CharacterTextures.CTEntryList entries = this.entries[dir.index()];

            for (int i = 0; i < entries.size(); i++) {
                CharacterTextures.CTEntry entry = entries.get(i);
                if (entry.frame == frame) {
                    return entry;
                }
            }

            return null;
        }

        void addEntry(IsoDirections dir, int frame, DeadBodyAtlas.BodyTexture texture) {
            CharacterTextures.CTEntryList entries = this.entries[dir.index()];
            CharacterTextures.CTEntry entry = new CharacterTextures.CTEntry();
            entry.frame = frame;
            entry.texture = texture;
            entries.add(entry);
        }
    }
}
