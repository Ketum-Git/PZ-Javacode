// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

public enum ZombieStateFlag {
    Initialized(1),
    Crawling(2),
    CanWalk(4),
    FakeDead(8),
    CanCrawlUnderVehicle(16),
    ReanimatedForGrappleOnly(32);

    public final int flag;

    private ZombieStateFlag(final int in_flag) {
        this.flag = in_flag;
    }
}
