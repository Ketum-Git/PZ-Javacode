// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters.component;

import zombie.characters.ecs.ECSComponent;

public class FrameKeeperComponent extends ECSComponent {
    private int frameNo = Integer.MIN_VALUE;

    public final int getFrameNo() {
        return this.frameNo;
    }

    public final void setFrameNo(int ecsFrameNo) {
        this.frameNo = ecsFrameNo;
    }
}
