// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalTracks;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;

@UsedFromLua
public class IsoAnimalTrack extends IsoObject {
    public boolean glow;
    private AnimalTracks track;

    public IsoAnimalTrack(IsoCell cell) {
        super(cell);
    }

    public IsoAnimalTrack(IsoGridSquare sq, String sprite, AnimalTracks track) {
        super(sq, sprite, null);
        sq.AddSpecialObject(this);
        this.track = track;
    }

    @Override
    public String getObjectName() {
        return "IsoAnimalTrack";
    }

    public void glow(IsoPlayer chr) {
        this.setOutlineHighlight(chr.playerIndex, true);
        this.setHighlighted(true, false);
        this.setOutlineHighlightCol(chr.playerIndex, 1.0F, 1.0F, 1.0F, 1.0F);
        this.setHighlightColor(1.0F, 1.0F, 1.0F, 1.0F);
        this.setOutlineHighlight(true);
        this.setOutlineHighlightCol(1.0F, 1.0F, 0.0F, 1.0F);
    }

    public void stopGlow(IsoPlayer chr) {
        this.setOutlineHighlight(chr.playerIndex, false);
    }

    public AnimalTracks getAnimalTracks() {
        return this.track;
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        this.track.save(output);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.track = new AnimalTracks();
        this.track.load(input, WorldVersion);
    }
}
