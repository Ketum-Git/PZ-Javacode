// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import java.util.ArrayList;

/**
 * Created by ChrisWood (Tanglewood Games Limited) on 01/11/2017.
 */
public class IsoGridStack {
    public ArrayList<ArrayList<IsoGridSquare>> squares;

    public IsoGridStack(int count) {
        this.squares = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            this.squares.add(new ArrayList<>(5000));
        }
    }
}
