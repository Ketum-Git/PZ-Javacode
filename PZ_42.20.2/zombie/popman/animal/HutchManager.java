// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman.animal;

import java.util.ArrayList;
import zombie.iso.objects.IsoHutch;

public class HutchManager {
    private static final HutchManager instance = new HutchManager();
    private final ArrayList<IsoHutch> hutchList = new ArrayList<>();

    public static HutchManager getInstance() {
        return instance;
    }

    public void clear() {
        this.hutchList.clear();
    }

    public void add(IsoHutch hutch) {
        this.hutchList.add(hutch);
    }

    public void remove(IsoHutch hutch) {
        this.hutchList.remove(hutch);
    }

    public void reforceUpdate(IsoHutch hutch) {
        if (!this.hutchList.contains(hutch)) {
            this.hutchList.add(hutch);
        }
    }

    public boolean checkHutchExistInList(IsoHutch hutch) {
        for (int i = 0; i < this.hutchList.size(); i++) {
            IsoHutch testHutch = this.hutchList.get(i);
            if (testHutch.savedX == hutch.getSquare().x && testHutch.savedY == hutch.getSquare().y && testHutch.savedZ == hutch.getSquare().z) {
                testHutch.square = hutch.square;
                return true;
            }
        }

        return false;
    }

    public void updateAll() {
        for (int i = 0; i < this.hutchList.size(); i++) {
            IsoHutch hutch = this.hutchList.get(i);
            hutch.update();
        }
    }
}
